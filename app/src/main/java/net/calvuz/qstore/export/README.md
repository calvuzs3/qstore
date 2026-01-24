# Export Inventario - QStore

## Struttura File

```
domain/
  model/ExportModels.kt           # ExportFormat, ExportOptions, ExportResult, InventoryExportItem
  repository/ExportRepository.kt  # Interfaccia repository
  usecase/export/ExportInventoryUseCase.kt

data/
  repository/ExportRepositoryImpl.kt  # Implementazione CSV + Excel

presentation/
  ui/export/ExportViewModel.kt
  ui/export/ExportScreen.kt

di/
  ExportModule.kt  # Binding Hilt
```

## Dipendenze da aggiungere (build.gradle)

```kotlin
// Apache POI per Excel
implementation("org.apache.poi:poi:5.2.5")
implementation("org.apache.poi:poi-ooxml:5.2.5")
```

## DAO necessario (se non presente)

Aggiungi questa query in `InventoryDao`:

```kotlin
@Query("""
    SELECT * FROM inventory 
    INNER JOIN articles ON inventory.article_id = articles.id
""")
suspend fun getAllInventoryWithArticles(): List<InventoryWithArticle>
```

Con il data class:

```kotlin
data class InventoryWithArticle(
    @Embedded val inventory: InventoryEntity,
    @Relation(
        parentColumn = "article_id",
        entityColumn = "id"
    )
    val article: ArticleEntity
)
```

## FileProvider (AndroidManifest.xml)

Aggiungi nel tag `<application>`:

```xml
<provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="${applicationId}.fileprovider"
    android:exported="false"
    android:grantUriPermissions="true">
    <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/file_paths" />
</provider>
```

Crea `res/xml/file_paths.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<paths>
    <external-path name="documents" path="Documents/QStore/Export/" />
</paths>
```

## Permessi (AndroidManifest.xml)

```xml
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" 
    android:maxSdkVersion="28" />
```

Per Android 10+ usa già `Environment.DIRECTORY_DOCUMENTS` che non richiede permessi.

## Navigazione

Aggiungi la route:

```kotlin
composable("export") {
    ExportScreen(
        onNavigateBack = { navController.popBackStack() }
    )
}
```

## Output

- **CSV**: Separatore `;`, encoding UTF-8
- **Excel**: Formato `.xlsx`, colonne auto-dimensionate
- **Con foto**: Crea ZIP contenente il file dati + cartella `photos/`

File salvati in: `Documents/QStore/Export/`
