# QStore Backup Feature - Guida di Integrazione

## 📦 Struttura dei File

```
backup/
├── domain/                          # Layer Domain
│   ├── model/
│   │   ├── BackupMetadata.kt       # Metadati del backup
│   │   ├── BackupData.kt           # Modelli dati serializzabili
│   │   └── BackupResult.kt         # Risultati e stati operazioni
│   ├── repository/
│   │   └── BackupRepository.kt     # Interfaccia repository
│   └── usecase/
│       ├── CreateBackupUseCase.kt  # Use case creazione backup
│       └── RestoreBackupUseCase.kt # Use case ripristino
│
├── data/                            # Layer Data
│   ├── repository/
│   │   └── BackupRepositoryImpl.kt # Implementazione repository
│   ├── serializer/
│   │   └── BackupSerializer.kt     # Serializzazione JSON
│   └── zip/
│       └── BackupZipManager.kt     # Gestione file ZIP
│
├── di/
│   └── BackupModule.kt             # Modulo Hilt
│
└── presentation/
    ├── BackupViewModel.kt          # ViewModel
    └── BackupRestoreScreen.kt      # Schermata Compose
```

## 🔧 Dipendenze da Aggiungere

### build.gradle.kts (app)

```kotlin
dependencies {
    // Serializzazione JSON (se non già presente)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
}

// Nel blocco plugins
plugins {
    kotlin("plugin.serialization") version "1.9.0"
}
```

## 📋 Checklist Integrazione

### 1. Copia i file
Estrai lo ZIP e copia la cartella `backup/` in:
```
app/src/main/java/net/calvuz/qstore/
```

### 2. Aggiungi il FileProvider (se non presente)

In `AndroidManifest.xml`:
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
    <external-path name="downloads" path="Download/QStore/" />
    <cache-path name="cache" path="." />
    <files-path name="files" path="." />
</paths>
```

### 3. Permessi (AndroidManifest.xml)

```xml
<!-- Per salvare in Downloads -->
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
    android:maxSdkVersion="28" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"
    android:maxSdkVersion="32" />
```

### 4. Aggiungi la Navigation

Nella tua Navigation:
```kotlin
composable("backup") {
    BackupRestoreScreen(
        onNavigateBack = { navController.popBackStack() }
    )
}
```

### 5. Verifica Repository Interfaces

Assicurati che esistano queste interfacce nel tuo progetto:
- `DisplaySettingsRepository` con metodo `getSettings(): Flow<DisplaySettings>` e `updateSettings(settings: DisplaySettings)`
- `RecognitionSettingsRepository` con metodi simili

### 6. Fix Import se necessario

Potrebbero essere necessari aggiustamenti agli import in base alla struttura esatta del tuo progetto:
- `BuildConfig.VERSION_NAME` e `BuildConfig.VERSION_CODE`
- `ArticleCardStyle.fromName()` - verifica che esista questo metodo companion

## 🗂️ Struttura ZIP di Backup Generato

```
qstore_backup_2025-01-25_143052.zip
│
├── metadata.json                    # Info + checksums
├── data/
│   ├── categories.json
│   ├── articles.json
│   ├── inventory.json
│   ├── movements.json
│   └── article_images.json          # Include features Base64
├── images/
│   └── {articleUuid}/
│       └── {filename}.jpg
└── settings/
    ├── display_settings.json
    └── recognition_settings.json
```

## ⚠️ Note Importanti

1. **Checksums**: Ogni componente ha il proprio checksum SHA-256 nei metadata
2. **Features OpenCV**: Sono incluse come Base64 nel JSON, quindi il restore è immediato senza ricalcolo
3. **Backup di sicurezza**: Prima del restore viene creato automaticamente un backup
4. **ID autoincrement**: Gli ID dei movimenti e article_images vengono rigenerati al restore

## 🧪 Test Consigliati

1. Crea un backup con dati esistenti
2. Verifica che il file ZIP sia valido
3. Prova il restore su un'installazione pulita
4. Verifica integrità dati dopo restore
5. Testa il flow con Document Picker

## 📱 UI Features

- Progress indicator durante backup/restore
- Lista backup disponibili con metadata
- Validazione backup prima del restore
- Dialog di conferma con warning
- Condivisione backup via share sheet
- Eliminazione backup con conferma
