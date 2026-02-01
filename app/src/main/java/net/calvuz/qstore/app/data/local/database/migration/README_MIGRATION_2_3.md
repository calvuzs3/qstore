# Migrazione ArticleImage: da Long id a String uuid

## Panoramica

Questa migrazione converte la primary key di `article_images` da `Long` (auto-generated) a `String` (UUID).

**Motivo**: Supportare il merge intelligente tra dispositivi diversi senza rischio di collisioni ID.

## File da sostituire

Sostituisci questi file nel tuo progetto con le versioni in questa cartella:

### Layer Data
| File | Path nel progetto |
|------|-------------------|
| `ArticleImageEntity.kt` | `app/data/local/entity/` |
| `ArticleImageDao.kt` | `app/data/local/database/` |
| `ArticleImageMapper.kt` | `app/data/mapper/` |
| `ImageRecognitionRepositoryImpl.kt` | `app/data/repository/` |
| `BackupSerializer.kt` | `backup/data/serializer/` |

### Layer Domain
| File | Path nel progetto |
|------|-------------------|
| `ArticleImage.kt` | `app/domain/model/` |
| `ImageRecognitionRepository.kt` | `app/domain/repository/` |
| `DeleteArticleImageUseCase.kt` | `app/domain/usecase/recognition/` |
| `GetArticleImagesUseCase.kt` | `app/domain/usecase/recognition/` |

### Backup
| File | Path nel progetto |
|------|-------------------|
| `BackupData.kt` | `backup/domain/model/` |

### Migration
| File | Path nel progetto |
|------|-------------------|
| `Migration_ArticleImage_UUID.kt` | `app/data/local/database/` |

## Passi per applicare la migrazione

### 1. Aggiorna la versione del database

In `QuickStoreDatabase.kt`, incrementa `DATABASE_VERSION`:

```kotlin
@Database(
    entities = [...],
    version = 7,  // Era 6, ora 7
    exportSchema = true
)
abstract class QuickStoreDatabase : RoomDatabase() {
    // ...
}
```

### 2. Aggiungi la migration al database builder

In `QuickStoreDatabase.kt` o nel tuo `DatabaseModule.kt`:

```kotlin
Room.databaseBuilder(context, QuickStoreDatabase::class.java, "quickstore_db")
    .addMigrations(
        // ... altre migration esistenti ...
        MIGRATION_ARTICLE_IMAGE_UUID  // <-- Aggiungi questa
    )
    .build()
```

### 3. Aggiorna i numeri di versione nella migration

In `Migration_ArticleImage_UUID.kt`, modifica i numeri:

```kotlin
val MIGRATION_ARTICLE_IMAGE_UUID = object : Migration(6, 7) { // <-- I tuoi numeri
```

### 4. Cerca e sostituisci negli altri file

Cerca nel progetto questi pattern e aggiornali:

```kotlin
// PRIMA (vecchio)
fun deleteById(imageId: Long)
fun getById(imageId: Long)
imageId: Long
image.id

// DOPO (nuovo)
fun deleteByUuid(uuid: String)
fun getByUuid(uuid: String)
imageUuid: String
image.uuid
```

### 5. Aggiorna i ViewModel (se necessario)

Se hai ViewModel che usano `imageId: Long`, aggiornali a `imageUuid: String`.

Esempio tipico in un ArticleDetailViewModel:

```kotlin
// PRIMA
fun deleteImage(imageId: Long) {
    viewModelScope.launch {
        deleteArticleImageUseCase(imageId)
    }
}

// DOPO
fun deleteImage(imageUuid: String) {
    viewModelScope.launch {
        deleteArticleImageUseCase(imageUuid)
    }
}
```

### 6. Aggiorna le UI Compose (se necessario)

Se passi `imageId` come parametro, aggiornalo:

```kotlin
// PRIMA
onClick = { onDeleteImage(image.id) }

// DOPO
onClick = { onDeleteImage(image.uuid) }
```

## Test della migrazione

1. **Installa la versione precedente** dell'app con alcuni dati di test
2. **Aggiorna all'app con la migration** 
3. **Verifica**:
   - Le immagini esistenti sono ancora visibili
   - Puoi aggiungere nuove immagini
   - Puoi eliminare immagini
   - Il riconoscimento visuale funziona
   - Backup e restore funzionano

## Retrocompatibilità Backup

I backup creati con la vecchia versione (con `id: Long`) **NON saranno compatibili** con la nuova versione.

Opzioni:
1. **Ignora**: Gli utenti dovranno creare nuovi backup dopo l'aggiornamento
2. **Migration nel restore**: Aggiungi logica per convertire vecchi backup (più complesso)

Per la soluzione 2, in `BackupSerializer.deserializeArticleImages()`:

```kotlin
fun deserializeArticleImages(jsonString: String): List<ArticleImageBackup> {
    return try {
        // Prova nuovo formato (uuid)
        json.decodeFromString<List<ArticleImageBackup>>(jsonString)
    } catch (e: Exception) {
        // Fallback: vecchio formato (id: Long)
        val oldFormat = json.decodeFromString<List<ArticleImageBackupLegacy>>(jsonString)
        oldFormat.map { old ->
            ArticleImageBackup(
                uuid = UUID.randomUUID().toString(),
                articleUuid = old.articleUuid,
                imagePath = old.imagePath,
                featuresDataBase64 = old.featuresDataBase64,
                createdAt = old.createdAt
            )
        }
    }
}

// Classe legacy per deserializzare vecchi backup
@Serializable
private data class ArticleImageBackupLegacy(
    val id: Long,
    val articleUuid: String,
    val imagePath: String,
    val featuresDataBase64: String,
    val createdAt: Long
)
```

## Checklist finale

- [ ] Sostituiti tutti i file
- [ ] Aggiornato DATABASE_VERSION
- [ ] Aggiunta migration al database builder
- [ ] Aggiornati numeri versione nella migration
- [ ] Cercato e sostituito `imageId: Long` → `imageUuid: String`
- [ ] Aggiornati ViewModel
- [ ] Aggiornate UI Compose
- [ ] Testato su dispositivo con dati esistenti
- [ ] Testato backup/restore
- [ ] Testato riconoscimento immagini
