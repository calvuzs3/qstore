# Spec — Integrazione Ricambi QuickStore in QReport

## Contesto

Il tecnico, durante la compilazione di un checkup, può selezionare i ricambi
necessari direttamente dal catalogo di QuickStore (app magazzino). La selezione
avviene tramite un `ActivityResultLauncher` che lancia la `ArticlePickerActivity`
di QuickStore. I ricambi selezionati vengono salvati nel checkup in QReport.

La quantità indicata è a discrezione del tecnico (campo opzionale libero).
La giacenza disponibile in QuickStore non è rilevante in questa fase.

### Principio architetturale: separazione selezione / fetch dati

Il picker di QuickStore ha **una sola responsabilità**: raccogliere la scelta
dell'utente e restituire gli UUID degli articoli selezionati. I dati completi
(nome, codici, unità) vengono recuperati da QReport direttamente tramite il
`ContentProvider` di QuickStore (`QStoreArticleReader`), usando quegli UUID.

```
Picker (QStore)  →  ArrayList<UUID>  →  QStoreArticleReader.fetchByUuids()
                    (Intent extra)       (ContentProvider query)
                                              ↓
                                      List<SelectedArticle>
                                              ↓
                                      AddSparePartsUseCase → Room
```

Questo evita che il formato JSON del risultato del picker diventi un contratto
implicito tra le due app. Il ContentProvider è l'unica fonte di verità per i
dati degli articoli.

> **Nota per QStore:** il picker deve restituire `ArrayList<String>` nell'extra
> `SELECTED_UUIDS` (non più un JSON con i dati completi). Vedere §2.

---

## 1. AndroidManifest

### Permesso

```xml
<uses-permission android:name="net.calvuz.qstore.permission.READ_ARTICLES" />
```

### Dipendenza facoltativa da QuickStore

QuickStore potrebbe non essere installata. Il launcher deve gestire
`ActivityNotFoundException` mostrando un messaggio all'utente.
`QStoreArticleReader` deve restituire lista vuota (non lanciare eccezioni) se
il provider non è raggiungibile.

---

## 2. ArticleContract (costanti IPC)

File: `sync/qstore/ArticleContract.kt`

Copia locale delle costanti concordate con QuickStore. Non dipende da nessun
modulo QuickStore — è autocontenuta.

```kotlin
object ArticleContract {
    const val AUTHORITY       = "net.calvuz.qstore.provider"
    const val PERMISSION_READ = "net.calvuz.qstore.permission.READ_ARTICLES"
    const val ACTION_PICK     = "net.calvuz.qstore.action.PICK_ARTICLES"

    val ARTICLES_URI: Uri = Uri.parse("content://$AUTHORITY/articles")

    object Articles {
        const val UUID            = "uuid"
        const val NAME            = "name"
        const val DESCRIPTION     = "description"
        const val CATEGORY_ID     = "category_id"
        const val UNIT_OF_MEASURE = "unit_of_measure"
        const val CODE_OEM        = "code_oem"
        const val CODE_ERP        = "code_erp"
        const val CODE_BM         = "code_bm"
        const val NOTES           = "notes"
        const val UPDATED_AT      = "updated_at"
    }

    object PickerExtras {
        /** Inviato a QStore: UUID già presenti nel checkup (pre-spuntati nel picker). */
        const val PRESELECTED_UUIDS = "preselected_uuids"

        /**
         * Ricevuto da QStore: ArrayList<String> degli UUID selezionati dall'utente.
         * Il picker restituisce SOLO gli UUID — QReport recupera i dati dal
         * ContentProvider tramite QStoreArticleReader.fetchByUuids().
         */
        const val SELECTED_UUIDS = "selected_uuids"
    }
}
```

> **Breaking change rispetto alla versione precedente della spec:**
> `SELECTED_ARTICLES` (JSON string) è sostituito da `SELECTED_UUIDS`
> (`ArrayList<String>`). Le due app devono allineare questa modifica insieme.

---

## 3. Room — Migration DB

**Versione corrente:** v10 (o la corrente al momento dell'implementazione)
**Nuova versione:** v10 → v11

### Nuova tabella `checkup_spare_parts`

```sql
CREATE TABLE IF NOT EXISTS checkup_spare_parts (
    id           TEXT PRIMARY KEY,
    checkup_id   TEXT NOT NULL,
    article_uuid TEXT NOT NULL,
    name         TEXT NOT NULL,
    code_oem     TEXT NOT NULL DEFAULT '',
    code_erp     TEXT NOT NULL DEFAULT '',
    code_bm      TEXT NOT NULL DEFAULT '',
    unit         TEXT NOT NULL DEFAULT 'pz',
    quantity     REAL,
    notes        TEXT NOT NULL DEFAULT '',
    added_at     LONG NOT NULL
);

CREATE INDEX idx_spare_parts_checkup ON checkup_spare_parts (checkup_id);
```

`quantity` è nullable — il tecnico può lasciarlo vuoto.

I campi `name`, `code_*` e `unit` sono uno **snapshot** al momento della
selezione. Questo è intenzionale: i checkup già chiusi non devono cambiare
se il catalogo QStore viene aggiornato in seguito.

---

## 4. Entity

File: `checkup/spareparts/data/local/entity/CheckUpSparePartEntity.kt`

```kotlin
@Entity(
    tableName = "checkup_spare_parts",
    indices = [Index(value = ["checkup_id"])]
)
data class CheckUpSparePartEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "checkup_id")
    val checkupId: String,

    @ColumnInfo(name = "article_uuid")
    val articleUuid: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "code_oem")
    val codeOem: String = "",

    @ColumnInfo(name = "code_erp")
    val codeErp: String = "",

    @ColumnInfo(name = "code_bm")
    val codeBm: String = "",

    @ColumnInfo(name = "unit")
    val unit: String = "pz",

    @ColumnInfo(name = "quantity")
    val quantity: Double? = null,

    @ColumnInfo(name = "notes")
    val notes: String = "",

    @ColumnInfo(name = "added_at")
    val addedAt: Long
)
```

---

## 5. DAO

File: `checkup/spareparts/data/local/dao/CheckUpSparePartDao.kt`

```kotlin
@Dao
interface CheckUpSparePartDao {

    @Query("SELECT * FROM checkup_spare_parts WHERE checkup_id = :checkupId ORDER BY added_at ASC")
    fun observeByCheckup(checkupId: String): Flow<List<CheckUpSparePartEntity>>

    @Query("SELECT * FROM checkup_spare_parts WHERE checkup_id = :checkupId ORDER BY added_at ASC")
    suspend fun getByCheckup(checkupId: String): List<CheckUpSparePartEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(parts: List<CheckUpSparePartEntity>)

    @Query("DELETE FROM checkup_spare_parts WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM checkup_spare_parts WHERE checkup_id = :checkupId")
    suspend fun deleteAllForCheckup(checkupId: String)

    @Query("UPDATE checkup_spare_parts SET quantity = :quantity WHERE id = :id")
    suspend fun updateQuantity(id: String, quantity: Double?)

    @Query("UPDATE checkup_spare_parts SET notes = :notes WHERE id = :id")
    suspend fun updateNotes(id: String, notes: String)
}
```

---

## 6. Domain Model

File: `checkup/spareparts/domain/model/CheckUpSparePart.kt`

```kotlin
data class CheckUpSparePart(
    val id: String,
    val checkupId: String,
    val articleUuid: String,
    val name: String,
    val codeOem: String = "",
    val codeErp: String = "",
    val codeBm: String = "",
    val unit: String = "pz",
    val quantity: Double? = null,
    val notes: String = "",
    val addedAt: Long
)
```

Il codice visualizzato in UI è `codeOem` se non vuoto, altrimenti `codeErp`,
altrimenti `codeBm`, altrimenti nessun codice.

---

## 7. Repository

File: `checkup/spareparts/domain/repository/CheckUpSparePartRepository.kt`

```kotlin
interface CheckUpSparePartRepository {
    fun observeByCheckup(checkupId: String): Flow<List<CheckUpSparePart>>
    suspend fun addParts(parts: List<CheckUpSparePart>): Result<Unit>
    suspend fun removePart(id: String): Result<Unit>
    suspend fun updateQuantity(id: String, quantity: Double?): Result<Unit>
    suspend fun updateNotes(id: String, notes: String): Result<Unit>
    suspend fun clearAllForCheckup(checkupId: String): Result<Unit>
}
```

---

## 8. Use Cases

| Use Case | Input | Output |
|----------|-------|--------|
| `ObserveSparePartsUseCase` | `checkupId: String` | `Flow<List<CheckUpSparePart>>` |
| `AddSparePartsUseCase` | `checkupId: String, parts: List<SelectedArticle>` | `Result<Unit>` |
| `RemoveSparePartUseCase` | `id: String` | `Result<Unit>` |
| `UpdateSparePartQuantityUseCase` | `id: String, quantity: Double?` | `Result<Unit>` |

`SelectedArticle` è il modello di confine tra il layer IPC (ContentProvider di
QStore) e il dominio di QReport. Viene prodotto da `QStoreArticleReader` tramite
mapping del `Cursor`, non più da un parser JSON.

```kotlin
data class SelectedArticle(
    val uuid: String,
    val name: String,
    val description: String,
    val codeOem: String,
    val codeErp: String,
    val codeBm: String,
    val unit: String
)
```

`AddSparePartsUseCase` converte ogni `SelectedArticle` in `CheckUpSparePart`
assegnando un nuovo UUID e `addedAt = System.currentTimeMillis()`.
Ignora duplicati (stesso `articleUuid` già presente per quel `checkupId`).

---

## 9. QStore Content Reader

### 9a. QStoreAvailability

File: `sync/qstore/QStoreAvailability.kt`

```kotlin
object QStoreAvailability {
    fun isInstalled(context: Context): Boolean =
        context.packageManager.getLaunchIntentForPackage("net.calvuz.qstore") != null

    fun hasReadPermission(context: Context): Boolean =
        context.checkSelfPermission(ArticleContract.PERMISSION_READ) ==
            PackageManager.PERMISSION_GRANTED
}
```

### 9b. QStoreArticleReader

File: `sync/qstore/QStoreArticleReader.kt`

Unico punto di accesso al ContentProvider di QStore. Sostituisce il
`SelectedArticleParser` della versione precedente della spec.
Tutte le operazioni girano su `Dispatchers.IO`. Non lancia eccezioni verso
i chiamanti: restituisce lista vuota in caso di errore o provider non
disponibile.

```kotlin
class QStoreArticleReader(
    private val contentResolver: ContentResolver
) {

    private val projection = arrayOf(
        ArticleContract.Articles.UUID,
        ArticleContract.Articles.NAME,
        ArticleContract.Articles.DESCRIPTION,
        ArticleContract.Articles.CODE_OEM,
        ArticleContract.Articles.CODE_ERP,
        ArticleContract.Articles.CODE_BM,
        ArticleContract.Articles.UNIT_OF_MEASURE,
        ArticleContract.Articles.NOTES,
        ArticleContract.Articles.UPDATED_AT,
    )

    /**
     * Recupera articoli per lista di UUID — chiamato dopo il risultato del picker.
     */
    suspend fun fetchByUuids(uuids: List<String>): List<SelectedArticle> =
        withContext(Dispatchers.IO) {
            if (uuids.isEmpty()) return@withContext emptyList()
            val placeholders = uuids.joinToString(",") { "?" }
            runCatching {
                contentResolver.query(
                    ArticleContract.ARTICLES_URI,
                    projection,
                    "${ArticleContract.Articles.UUID} IN ($placeholders)",
                    uuids.toTypedArray(),
                    null
                )?.use { it.toSelectedArticleList() }
            }.getOrNull() ?: emptyList()
        }

    /**
     * Ricerca per nome o codice — per ricerca tipo-ahead in QReport senza
     * aprire il picker.
     */
    suspend fun search(query: String): List<SelectedArticle> =
        withContext(Dispatchers.IO) {
            runCatching {
                contentResolver.query(
                    ArticleContract.ARTICLES_URI,
                    projection,
                    "${ArticleContract.Articles.NAME} LIKE ? OR " +
                    "${ArticleContract.Articles.CODE_OEM} LIKE ? OR " +
                    "${ArticleContract.Articles.CODE_ERP} LIKE ?",
                    arrayOf("%$query%", "%$query%", "%$query%"),
                    "${ArticleContract.Articles.NAME} ASC"
                )?.use { it.toSelectedArticleList() }
            }.getOrNull() ?: emptyList()
        }

    /**
     * Emette Unit ogni volta che il ContentProvider di QStore notifica un
     * cambiamento nel catalogo. Usare con debounce — QStore può emettere
     * molte notifiche consecutive durante un import bulk.
     */
    fun observeCatalogChanges(): Flow<Unit> = callbackFlow {
        val observer = object : ContentObserver(null) {
            override fun onChange(selfChange: Boolean) { trySend(Unit) }
        }
        contentResolver.registerContentObserver(
            ArticleContract.ARTICLES_URI,
            true,
            observer
        )
        awaitClose { contentResolver.unregisterContentObserver(observer) }
    }

    // --- Cursor helpers ---

    private fun Cursor.toSelectedArticleList(): List<SelectedArticle> =
        buildList {
            while (moveToNext()) { add(toSelectedArticle()) }
        }

    private fun Cursor.toSelectedArticle() = SelectedArticle(
        uuid        = str(ArticleContract.Articles.UUID),
        name        = str(ArticleContract.Articles.NAME),
        description = strOrEmpty(ArticleContract.Articles.DESCRIPTION),
        codeOem     = strOrEmpty(ArticleContract.Articles.CODE_OEM),
        codeErp     = strOrEmpty(ArticleContract.Articles.CODE_ERP),
        codeBm      = strOrEmpty(ArticleContract.Articles.CODE_BM),
        unit        = strOrDefault(ArticleContract.Articles.UNIT_OF_MEASURE, "pz"),
    )

    private fun Cursor.str(col: String) =
        getString(getColumnIndexOrThrow(col))

    private fun Cursor.strOrEmpty(col: String) =
        getColumnIndex(col).takeIf { it >= 0 }?.let { getString(it) }.orEmpty()

    private fun Cursor.strOrDefault(col: String, default: String) =
        getColumnIndex(col).takeIf { it >= 0 }
            ?.let { getString(it) }
            ?.ifEmpty { default }
            ?: default
}
```

### 9c. DI — QStoreModule

File: `sync/qstore/di/QStoreModule.kt`

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object QStoreModule {

    @Provides
    @Singleton
    fun provideQStoreArticleReader(
        @ApplicationContext context: Context
    ): QStoreArticleReader = QStoreArticleReader(context.contentResolver)
}
```

---

## 10. ViewModel — CheckUpDetailViewModel

### Aggiunte allo stato UI

```kotlin
data class CheckUpDetailUiState(
    // ... campi esistenti ...
    val spareParts: List<CheckUpSparePart> = emptyList(),
    val sparePartsError: UiText? = null
)
```

### Nuovi metodi

```kotlin
/**
 * Chiamato dal risultato del picker con la lista di UUID selezionati.
 * Recupera i dati dal ContentProvider e salva nel checkup.
 */
fun onArticlesSelected(uuids: List<String>)

fun removeSparePart(id: String)
fun updateSparePartQuantity(id: String, quantity: Double?)
```

Flusso di `onArticlesSelected`:

```
uuids (da picker)
    → qStoreArticleReader.fetchByUuids(uuids)   // ContentProvider query
    → List<SelectedArticle>
    → addSparePartsUseCase(checkupId, articles)  // dedup + Room insert
```

Il ViewModel inietta `QStoreArticleReader` e i use cases via Hilt.
In caso di errore del reader (QStore non disponibile), mostrare
`sparePartsError` con messaggio appropriato.

---

## 11. UI — CheckUpDetailScreen

### Posizione nella LazyColumn

Inserire la sezione **dopo** `ProgressOverviewCard` e **prima** dei moduli.

### SparePartsSection (nuovo composable)

```
┌─────────────────────────────────────────┐
│ Ricambi necessari              [+ Aggiungi] │
│─────────────────────────────────────────│
│ (vuoto) Nessun ricambio aggiunto        │
│                                         │
│  ○ Filtro olio  OF-1234  pz  [qty: __] [×]│
│  ○ Cinghia K    KB-5500  m   [qty: __] [×]│
└─────────────────────────────────────────┘
```

- **Bottone "Aggiungi"** → lancia `ActivityResultLauncher`
- Ogni riga: nome, codice principale (OEM > ERP > BM), unità, campo quantità
  opzionale (numerico), pulsante rimozione
- Empty state: testo neutro (non errore)
- Se QuickStore non è installata: Snackbar
  `"QuickStore non trovata — installa l'app magazzino"`

### ActivityResultLauncher

```kotlin
val pickArticlesLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.StartActivityForResult()
) { result ->
    if (result.resultCode == Activity.RESULT_OK) {
        val uuids = result.data
            ?.getStringArrayListExtra(ArticleContract.PickerExtras.SELECTED_UUIDS)
        if (!uuids.isNullOrEmpty()) viewModel.onArticlesSelected(uuids)
    }
}

// Lancio
fun launchPicker(preselectedUuids: List<String> = emptyList()) {
    val intent = Intent(ArticleContract.ACTION_PICK).apply {
        setPackage("net.calvuz.qstore")
        putStringArrayListExtra(
            ArticleContract.PickerExtras.PRESELECTED_UUIDS,
            ArrayList(preselectedUuids)
        )
    }
    try {
        pickArticlesLauncher.launch(intent)
    } catch (e: ActivityNotFoundException) {
        // mostra Snackbar "QuickStore non trovata"
    }
}
```

`preselectedUuids` viene popolato con gli UUID già presenti in `spareParts`
così QuickStore mostra già spuntati i pezzi aggiunti in precedenza.

---

## 12. Struttura package

```
checkup/
  spareparts/
    data/
      local/
        dao/CheckUpSparePartDao.kt
        entity/CheckUpSparePartEntity.kt
        mapper/CheckUpSparePartMapper.kt
        repository/CheckUpSparePartRepositoryImpl.kt
    domain/
      model/
        CheckUpSparePart.kt
        SelectedArticle.kt
      repository/CheckUpSparePartRepository.kt
      usecase/
        ObserveSparePartsUseCase.kt
        AddSparePartsUseCase.kt
        RemoveSparePartUseCase.kt
        UpdateSparePartQuantityUseCase.kt
    presentation/
      ui/components/SparePartsSection.kt
sync/
  qstore/
    ArticleContract.kt
    QStoreArticleReader.kt
    QStoreAvailability.kt
    di/QStoreModule.kt
```

> Rimosso: `checkup/spareparts/data/mapper/SelectedArticleParser.kt`
> (sostituito da `QStoreArticleReader` + mapping Cursor interno).

---

## 13. Checklist implementazione QReport

### Fase 1 — Contract e IPC (prerequisito per tutto)
- [ ] `AndroidManifest.xml` — `uses-permission READ_ARTICLES`
- [ ] `ArticleContract.kt` — costanti IPC (con `SELECTED_UUIDS`, non più `SELECTED_ARTICLES`)
- [ ] `QStoreAvailability.kt`
- [ ] `QStoreArticleReader.kt` + `QStoreModule.kt`

### Fase 2 — Data layer
- [ ] Migration DB v→v+1 — tabella `checkup_spare_parts`
- [ ] `CheckUpSparePartEntity` + registrazione in `AppDatabase`
- [ ] `CheckUpSparePartDao`
- [ ] `CheckUpSparePartMapper` (entity ↔ domain)
- [ ] `CheckUpSparePartRepositoryImpl`

### Fase 3 — Domain layer
- [ ] `SelectedArticle` (modello di confine, prodotto dal reader)
- [ ] `CheckUpSparePart` (domain model)
- [ ] `CheckUpSparePartRepository` (interfaccia)
- [ ] `ObserveSparePartsUseCase`
- [ ] `AddSparePartsUseCase` (input: `List<SelectedArticle>`, con dedup su `articleUuid`)
- [ ] `RemoveSparePartUseCase`
- [ ] `UpdateSparePartQuantityUseCase`

### Fase 4 — DI
- [ ] Hilt module per repository e use cases spare parts

### Fase 5 — Presentation
- [ ] `CheckUpDetailViewModel` — stato + `onArticlesSelected(uuids)`, `removeSparePart`, `updateSparePartQuantity`
- [ ] `SparePartsSection` composable
- [ ] `CheckUpDetailScreen` — integrazione launcher + sezione UI

---

## 14. Fase 2 (opzionale) — Cache catalogo offline

Da implementare solo se QReport deve permettere di aggiungere ricambi anche
quando QuickStore non è installata o non è raggiungibile.

### Tabella `qstore_article_cache` (Room)

Tabella **separata** da `checkup_spare_parts`: è un mirror volatile del
catalogo QStore, può essere svuotata e ricostruita senza perdita di dati di
business.

```sql
CREATE TABLE IF NOT EXISTS qstore_article_cache (
    uuid             TEXT PRIMARY KEY,
    name             TEXT NOT NULL,
    description      TEXT NOT NULL DEFAULT '',
    code_oem         TEXT NOT NULL DEFAULT '',
    code_erp         TEXT NOT NULL DEFAULT '',
    code_bm          TEXT NOT NULL DEFAULT '',
    unit_of_measure  TEXT NOT NULL DEFAULT 'pz',
    notes            TEXT NOT NULL DEFAULT '',
    cached_at        INTEGER NOT NULL
);
```

### Sync

- `QStoreCatalogSyncWorker` (WorkManager, `CoroutineWorker`): esegue
  `QStoreArticleReader` su tutto il catalogo e popola la cache.
- `QStoreCatalogObserver` (`ContentObserver`): osserva `ARTICLES_URI` di
  QStore, schedula il worker con debounce (≥ 30 s) per evitare sync multipli
  su import bulk.
- Il worker si schedula anche al boot via `WorkManager` one-time enqueue se
  la cache è vuota o più vecchia di N ore.

```
sync/
  qstore/
    ArticleContract.kt
    QStoreArticleReader.kt
    QStoreAvailability.kt
    cache/
      QStoreArticleCacheEntity.kt
      QStoreArticleCacheDao.kt
      QStoreCatalogSyncWorker.kt
      QStoreCatalogObserver.kt
    di/QStoreModule.kt
```
