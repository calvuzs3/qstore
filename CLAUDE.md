# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**QuickStore** is an Android inventory management app with image-based article search. It uses a phone camera + OpenCV (ORB feature extraction + BFMatcher) to identify warehouse articles by photo. The app is published on the Play Store (`net.calvuz.quickstore`).

- **Min SDK**: 33 (Android 13)
- **Target SDK**: 35
- **Kotlin**: 2.1.0 / JVM 17
- **Current DB version**: 4 (MIGRATION_3_4: multi-location — `locations`, `article_location_thresholds`, `movements.id` Long→UUID, `from_location_uuid`/`to_location_uuid`, `inventory` PK composta articolo+ubicazione, `MovementType.ADJUSTMENT`/`TRANSFER`)

## Build Commands

```bash
# Debug build and install on connected device/emulator
./gradlew installDebug

# Release build (requires keystore.properties)
./gradlew assembleRelease

# Run unit tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest

# Run a single test class
./gradlew test --tests "net.calvuz.qstore.ExampleUnitTest"

# Clean build
./gradlew clean build
```

Room schema is exported to `app/schemas/` (configured via KSP arg `room.schemaLocation`).

## Architecture

Strict Clean Architecture in three layers. The package root splits into two namespaces:

- `net.calvuz.qstore.app.*` — core app (articles, inventory, movements, camera/recognition)
- `net.calvuz.qstore.<feature>.*` — self-contained feature modules: `backup`, `categories`, `export`, `settings`

Each feature module and the core app follow the same internal layout:

```
<feature>/
  domain/model/          # Pure Kotlin data classes (no Android/Room imports)
  domain/repository/     # Interfaces only
  domain/usecase/        # One use case per file, return Result<T>
  data/local/entity/     # Room @Entity classes
  data/local/database/   # DAOs
  data/mapper/           # Entity ↔ Domain mappers
  data/repository/       # Repository implementations
  di/                    # Hilt @Module objects
  presentation/          # Composable screens + ViewModels
```

**Key rules enforced across the codebase:**
- Domain layer has zero Android/Room imports — pure Kotlin only.
- All suspend functions return `Result<T>` for type-safe error handling.
- Database updates are reactive: DAOs return `Flow<List<T>>` for queries that drive UI; `suspend fun` for one-shot writes.
- All FK relationships use `onDelete = CASCADE`.
- `@Transaction` is used on Room operations that update multiple tables atomically (e.g., `AddMovementUseCase` updates both `movements` and `inventory` tables).

## Dependency Injection (Hilt)

Hilt modules are all `@InstallIn(SingletonComponent::class)`. Key singletons:

- `DatabaseModule` — provides `QuickStoreDatabase` + all DAOs, with explicit migrations (`MIGRATION_1_2`, `MIGRATION_2_3`)
- `RepositoryModule` — binds repository interfaces to implementations
- `OpenCVModule` — provides `OpenCVManager`, `FeatureExtractor`, `ImageMatcher`, `ImageStorageManager`
- Feature DI modules: `BackupModule`, `CategoryUseCaseModule`, `ExportModule`, `SettingsModule`

ViewModels receive use cases via constructor injection (`@HiltViewModel`).

## Navigation

All routes are defined as `sealed class Screen(val route: String)` in `AppNavigation.kt`. Routes use string path parameters (e.g., `"article/{articleId}"`). Article/category IDs are UUIDs passed as strings. The `SearchResults` screen receives a comma-separated list of UUIDs encoded in the route.

## OpenCV Image Recognition

OpenCV is initialized asynchronously at application startup in `QuickStoreApplication`. The pipeline:

1. `OpenCVManager.initialize()` — tries `initDebug()` first, falls back to async `initAsync()`
2. `FeatureExtractor` — extracts ORB keypoints/descriptors from `Bitmap`
3. `ImageMatcher` — BFMatcher comparison; returns similarity score
4. Features are stored as `BLOB` in `article_images.features_data` to avoid re-extraction on search
5. On backup/restore, features are serialized as Base64 JSON so they survive without recompute

**Critical**: Every OpenCV `Mat` must be `.release()`d. Use `finally` blocks. The repository handles cleanup automatically, but new code touching `Mat` objects must follow the same pattern.

Image files are stored at: `/data/data/net.calvuz.quickstore/files/article_images/{articleUuid}/`

## Database Schema (v4)

Tables: `articles`, `article_categories`, `inventory`, `movements`, `article_images`, `locations`, `article_location_thresholds`

- All primary keys are UUIDs (TEXT), not auto-increment integers (`movements.id` was the one exception, migrated to UUID in v4).
- `inventory` is a separate table from `articles`, now with a **composite PK** `(article_uuid, location_uuid)` — an article has one giacenza row per magazzino/ubicazione, not 1:1 with `articles` anymore. Quantity is `Double` to support fractional units (kg, litres, metres). Total quantity across locations is a SUM, not a stored value (see `InventoryDao.getTotalByArticle`).
- `movements` is **append-only** (no update/delete in normal flow) and carries `from_location_uuid`/`to_location_uuid` (nullable): `IN` sets only `to`, `OUT` only `from`, `ADJUSTMENT` exactly one of the two, `TRANSFER` both (different locations) — see `MovementRepositoryImpl` for the unified debit/credit algorithm based on which fields are set, not on `type`.
- `locations` = magazzini/ubicazioni (es. "Sede", "Furgone"). `article_location_thresholds` = soglia di riordino opzionale per coppia articolo/ubicazione (se assente, vale `articles.reorderLevel` sul totale).
- Timestamps are UTC epoch milliseconds (`Long`). Display conversion to `LocalDateTime` happens only in the Presentation layer.
- When adding a new DB version, add a `Migration` object in `Migrations.kt` and register it in `DatabaseModule`.

## Backup Format

Backups are ZIP archives containing:
- `metadata.json` — version info + SHA-256 checksums per component
- `data/{categories,articles,inventory,movements,article_images}.json` — serialized with `kotlinx.serialization`
- `images/{articleUuid}/*.jpg` — raw image files

**Known limitation (as of v4/multi-location):** the backup format does not yet carry `locations` or the `from_location_uuid`/`to_location_uuid` on movements — `BackupSerializer`/`BackupRepositoryImpl` reassign everything to a freshly recreated "Magazzino principale" default location on restore, and any additional locations created before the backup are silently lost. Redesigning the backup format for multi-location was explicitly deferred to a dedicated future change (see comments in `BackupSerializer.kt`).
- `settings/{display_settings,recognition_settings}.json`

Before any restore, a safety backup is created automatically.

## Export

Inventory exports to CSV (`;` separator, UTF-8) or Excel (`.xlsx` via Apache POI). Files are saved to `Documents/QStore/Export/`. When "with photos" is selected, output is a ZIP. POI logging is excluded via `configurations.all { exclude(group = "org.apache.logging.log4j") }`.

## Settings

Two settings stores backed by `DataStore<Preferences>`:
- `DisplaySettings` — controls `ArticleCardStyle` (compact/full) in article list
- `RecognitionSettings` — tunable OpenCV matching thresholds with presets

Both are exposed as `Flow<Settings>` from their repositories and observed in ViewModels.

## Key Technical Notes

- **Quantities**: `Double` throughout (inventory + movements) — supports fractional units.
- **Stock warning**: triggered when the article's TOTAL quantity across all locations (`InventoryDao.getTotalByArticle`, summed) `<= article.reorderLevel`. An optional per-location threshold also exists (`article_location_thresholds`), not yet wired into any UI warning.
- **`AddArticle` screen is dual-mode**: create vs. edit is detected from whether the `articleId` nav argument is present (the `EditArticle` route also renders `AddArticleScreen`).
- **OpenCV NDK ABI filters**: `armeabi-v7a`, `arm64-v8a`, `x86`, `x86_64` — all four are included.
- **Keystore**: signing config reads from `keystore.properties` (not committed). Release builds require this file.
