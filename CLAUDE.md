# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**QuickStore** is an Android inventory management app with image-based article search. It uses a phone camera + OpenCV (ORB feature extraction + BFMatcher) to identify warehouse articles by photo. The app is published on the Play Store (`net.calvuz.quickstore`).

- **Min SDK**: 33 (Android 13)
- **Target SDK**: 35
- **Kotlin**: 2.1.0 / JVM 17
- **Current DB version**: 7 (MIGRATION_3_4: multi-location — `locations`, `article_location_thresholds`, `movements.id` Long→UUID, `from_location_uuid`/`to_location_uuid`, `inventory` PK composta articolo+ubicazione, `MovementType.ADJUSTMENT`/`TRANSFER`; MIGRATION_4_5: `movements.created_by`; MIGRATION_5_6: `article_images.is_uploaded`; MIGRATION_6_7: `is_deleted` su `article_categories`/`articles`/`locations`/`article_location_thresholds`/`article_images` + vero `updated_at` su `article_images`)

## Stato attuale — sync multi-device (HANDOFF, 2026-07-02)

QuickStore sta passando da app puramente offline a un modello opzionale multi-device: un
backend dedicato (`../quickstore-server`, Ktor + Postgres, repo separato) più due nuovi
feature module qui (`auth`, `sync`). **L'app resta comunque fruibile offline al 100% senza
mai fare login** — questo è un requisito guida, non un dettaglio.

**Verificato end-to-end** (device fisico reale + virtual device, contro
`https://quickstore.calvuz.net`): login (diretto e multi-org con select-org), messaggi di
errore puliti (niente eccezioni tecniche a schermo), push+pull manuale con dati reali,
pull "da zero" su device vergine (fresh install, ordine di dipendenza dell'upsert e
vincoli FK di Room verificati funzionanti tramite log Timber). Bug trovati e corretti in
questo giro: la schermata di login non si aggiornava subito dopo il login riuscito; Home
non si aggiornava mai tornando dalla sync (fetch one-shot senza refresh automatico); un
`Result` scartato in `ingestMovement` poteva far sparire silenziosamente un fallimento di
inventario (ora loggato e riportato in `SyncSummary.failedMovements`).

**Verificato anche `ImageTransferWorker`** (WorkManager + Hilt), accodato automaticamente
a fine `syncNow()` — carica su `/images/upload/{id}` i JPEG scattati su questo device e
non ancora sul server (`article_images.is_uploaded=false`), scarica da
`/images/download/{id}` quelli il cui metadato è arrivato via pull ma il cui file non
esiste ancora in locale. Gira come foreground service (notifica di avanzamento, canale
`image_transfer`) — richiede il permesso runtime POST_NOTIFICATIONS (richiesto in
`LoginScreen` quando si entra in `AlreadyLoggedIn`, min SDK è già 33 quindi sempre
necessario). Vincolo di rete configurabile (switch in Impostazioni > Account: solo wifi
di default, o anche dati mobili) — vedi `SyncSettingsRepository`/`SyncLocalStore`. Bug
trovato e corretto: il `<service>` interno di WorkManager che ospita il foreground di
ogni worker non dichiara un `foregroundServiceType` di default — va sovrascritto nel
manifest dell'app (`tools:node="merge"` su `SystemForegroundService`), altrimenti
`setForeground()` crasha con `IllegalArgumentException` anche a codice corretto. Testato
su device reale con foto vere: upload confermato funzionante.

**Aggiunta la propagazione delle cancellazioni locali → server** (MIGRATION_6_7):
`article_categories`/`articles`/`locations`/`article_location_thresholds`/`article_images`
hanno ora `is_deleted` — cancellare localmente diventa un soft-delete (`is_deleted=1`,
`updated_at=now`) invece di un `DELETE` fisico, raccolto dalla stessa query di push già
esistente e propagato al server (che già sapeva gestirlo in arrivo, nessuna modifica
server necessaria). `movements` resta escluso di proposito — log append-only. Cambiato
anche il comportamento di `DeleteArticleUseCase`: **non cancella più** inventario/movimenti
(il CASCADE di Room non scatta su un UPDATE), le immagini dell'articolo vengono invece
soft-eliminate esplicitamente in codice (incluso il file JPEG fisico, cancellato subito).
Non ancora testato su device reale (solo build da zero pulita).

**Non ancora fatto** (elencato per priorità presunta, nessun ordine impegnativo):
1. Canale WebSocket (`ws /sync/ws`) per il nudge near-realtime + `WorkManager` per una
   pull periodica di sicurezza (dei *metadati*, non delle foto — quello esiste già) — il
   server li supporta già, lato Android sono deliberatamente rinviati (vedi sezione
   "Sync" più sotto) per verificare push/pull manuale in isolamento prima di aggiungere
   l'automazione in background.
2. UI di gestione membership (invita/cambia ruolo/rimuovi) e lettura audit log: gli
   endpoint server esistono (`quickstore-server/CLAUDE.md` sezione 9), nessuna schermata
   Android li usa ancora.
3. `org_id` sulle entity sincronizzate: deciso di **non** aggiungerlo (i DTO di rete non lo
   portano comunque, lo inietta il server dal JWT) a meno che non emerga un vero bisogno
   di isolare dati multi-org sullo stesso device.
4. Redesign del formato di backup per il multi-magazzino (rinviato quando abbiamo fatto la
   migrazione v3→v4 — vedi sezione "Backup Format" più sotto): il backup/restore ZIP
   ignora ancora `locations`.

Dettagli architetturali del server (schema, sicurezza multi-tenant, endpoint) sono in
`../quickstore-server/CLAUDE.md` — quello resta la fonte autoritativa lato server, questo
file copre solo il client Android.

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
- `net.calvuz.qstore.<feature>.*` — self-contained feature modules: `backup`, `categories`, `export`, `settings`, `auth`, `sync`

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

Three settings stores backed by `DataStore<Preferences>`:
- `DisplaySettings` — controls `ArticleCardStyle` (compact/full) in article list
- `RecognitionSettings` — tunable OpenCV matching thresholds with presets
- `ServerSettings` — base URL of `quickstore-server` for the `auth` module, user-entered (now `https://quickstore.calvuz.net`, publicly reachable with a real TLS cert via reverse proxy — no longer just the VM's private IP)

All are exposed as `Flow<Settings>` from their repositories and observed in ViewModels.

## Auth (optional, for multi-device sync)

QuickStore remains fully usable offline with no account — `auth` is opt-in, reachable from Settings > Account, never a gate before Home.

- `AuthRepository`: `login(email, password)` → either a full `Session` or `LoginResult.OrganizationSelectionRequired` (server two-step flow for multi-org users, see `quickstore-server`); `selectOrganization(pendingToken, orgId)` completes the second step.
- JWT is stored raw (not decomposed) in `TokenStore`, an `EncryptedSharedPreferences` (Keystore-backed) — never `DataStore`, since this is a credential, not a preference. `orgName` isn't a JWT claim, so it's persisted alongside the token to survive app restarts without a fresh login.
- `JwtDecoder` reads claims (`sub`, `orgId`, `roleLevel`, `roleCode`, `exp`) client-side without verifying the signature — the server already verified it when issuing the token; the client only needs the claims to rebuild `Session` on restart and to check local expiry.
- Network client: Ktor (OkHttp engine) pinned to **3.1.3**, not the newer 3.5.0 used by `quickstore-server` — QuickStore is on Kotlin 2.1.0 and later Ktor releases ship binary metadata requiring Kotlin 2.3.x.
- Login errors: `AuthApi` checks `response.status` **before** reading the body — this Ktor client has `expectSuccess = false`, so a 401 with a plain-text body ("Invalid credentials", not JSON) doesn't throw on its own; reading it as JSON without the status check first surfaces a raw deserialization exception instead of a clean message.
- `MIGRATION_4_5` added `movements.created_by` (nullable — populated from the active session at creation time, null for offline/no-account rows). `org_id` on synced entities remains deferred: the wire DTOs never carry it anyway (the server injects it from the JWT), so it's only needed locally if a single device ever works across multiple orgs, which isn't yet a real scenario.

## Sync (manual push/pull, optional — requires a session)

First working version of the `sync` module: a **manual** "Sincronizza ora" button (Settings > Account, shown once logged in) — no WebSocket nudge or background `WorkManager` pull yet, deliberately deferred to keep this phase testable in isolation.

- `SyncRepositoryImpl.syncNow()`: push then pull, always in this order — push whatever changed locally since the last cursor, then pull whatever changed remotely.
- **Push**: queries each DAO's `getUpdatedSince(cursor)` (`getCreatedSince` for `movements`, append-only) across all 6 synced entities (`article_categories`, `locations`, `articles`, `article_location_thresholds`, `movements`, `article_images`), maps to the DTOs mirrored 1:1 from `quickstore-server`. `movements.createdBy` falls back to the current session's `userId` if a row predates any login.
- **Pull**: applies the response in server dependency order (categories → locations → articles → thresholds → movements → images) with the same last-write-wins discipline as the server (`dto.updatedAt <= existing.updatedAt` → skip). Pulled movements go through `MovementRepository.ingestPulledMovement()`, not a raw DAO insert — it also replays the inventory delta (debit/credit) so the local `inventory` cache stays consistent with movement history; skips if the id already exists (idempotent) and never re-validates availability (the server already accepted it from the other device).
- Cursor (`since`) and a stable per-install `deviceId` live in `SyncLocalStore` (its own DataStore, `sync_state`).
- Every log line in the sync path is tagged `"Sync"` via Timber (planted only in debug builds, `QuickStoreApplication.onCreate`) — `SyncRepositoryImpl`/`SyncApi`/`ImagesApi` all log push/pull payload sizes per entity, every upsert decision (insert/update/skip-stale/delete), and non-success HTTP responses with their body. Use `adb logcat -s Sync:*` when diagnosing a sync issue instead of guessing.

**Deletion propagation** (MIGRATION_6_7): `article_categories`/`articles`/`locations`/`article_location_thresholds`/`article_images` all have `is_deleted`. A local delete is now a soft-delete (`is_deleted=1`, `updated_at=now`) — picked up by the same `getUpdatedSince(cursor)` push query used for regular updates (a deletion is conceptually just another update) and pushed with `isDeleted=true` in the DTO (`toDto()` now reads the real field instead of hardcoding `false`). The server already knew how to store an incoming `isDeleted` (no server change needed). A remote deletion (`isDeleted=true` arriving via pull) is still applied as a **hard** local `DELETE` — no need to keep a local tombstone for something the server has already confirmed gone; this also still triggers Room's `FK CASCADE` for that direction (pre-existing behavior, unchanged). `movements` is deliberately excluded — it's an append-only log, "deleting" a row doesn't fit the model.
- Read-facing DAO queries (`getAll`/`observeAll`/`search*`/`count*`/`hasImages`/etc.) filter `is_deleted = 0`; sync-internal queries (`getByUuid`, `getUpdatedSince`, `getPendingUpload`) do **not** — they need to see soft-deleted rows (LWW comparison, push collection). Where the same lookup serves both a user-facing repository method and sync, the filter is applied in the repository layer instead (e.g. `ArticleRepositoryImpl.getByUuid`), not the DAO.
- Deleting an article (`DeleteArticleUseCase`) no longer wipes its inventory/movements — Room's FK `CASCADE` doesn't fire on an `UPDATE` (soft-delete), only a real `DELETE`. This is an intentional behavior change: movements stay as an append-only historical record even for a deleted article; inventory rows go stale but harmless (the article is hidden from every list/search anyway). The article's own images ARE explicitly soft-deleted in code (`ArticleImageDao.markAllDeletedByArticleUuid`), plus their physical JPEG files removed immediately from disk — there's no FK cascade to rely on anymore, so this cascade is now explicit application code, not the database.
- `locations` and `article_location_thresholds` got the `is_deleted` column for schema symmetry with the server, but have **no delete flow wired up** — neither has a UI or a `repository.delete()` today, so nothing sets it yet.
- `article_images` also gained a real `updated_at` column (previously `created_at` was reused as a stand-in, which can't represent a later deletion without corrupting the true creation timestamp).
- No WebSocket client, no periodic background sync — the metadata sync only happens when the user taps the button (the photo transfer below runs automatically after it, that part is not manual).

### Photo transfer (`ImageTransferWorker`)

The sync payload above only carries `imagePath` (a string) and `featuresData` (small OpenCV descriptors, Base64) — never the real JPEG bytes. `ImageTransferWorker` (WorkManager + Hilt, `@HiltWorker`) handles the actual files, separately from the metadata sync because it can be heavy:

- Enqueued automatically (`WorkManager.enqueueUniqueWork`, `ExistingWorkPolicy.KEEP`) at the end of every successful `syncNow()` — the worker itself no-ops immediately (`Result.success()`) if there's nothing to transfer, so it's safe to always schedule it rather than pre-checking.
- **Upload**: `ArticleImageDao.getPendingUpload()` (`is_uploaded = 0`) → `ImagesApi.uploadImage()` (multipart POST `/images/upload/{id}`) → `markUploaded()`. New `article_images.is_uploaded` column (MIGRATION_5_6): defaults false for images captured locally, forced `true` for images arriving via pull (they're on the server by definition — no need to re-upload someone else's photo).
- **Download**: for every local `article_images` row, checks `ImageStorageManager.imageExists(imagePath)`; if the file is missing, `ImagesApi.downloadImage()` (GET `/images/download/{id}`) and `ImageStorageManager.writeImageAtPath()` writes the raw bytes at that exact relative path (not a new random filename — `imagePath` is already a portable `{articleUuid}/{file}.jpg` string, reproduced identically on every device).
- Runs as a foreground service (`setForeground`, `FOREGROUND_SERVICE_TYPE_DATA_SYNC`, required by API 34+) with an ongoing progress notification ("N di M"), plus a final result notification — channel `image_transfer`, created once in `QuickStoreApplication.onCreate`. Requires runtime `POST_NOTIFICATIONS` (requested in `LoginScreen` on entering `AlreadyLoggedIn`; min SDK is already 33 so there's no legacy pre-33 fallback path to maintain).
- Network constraint is user-configurable (`SyncSettingsRepository`/`SyncLocalStore`, a `Switch` next to "Sincronizza ora"): defaults to `NetworkType.UNMETERED` (wifi-only, since transfers can be heavy), can be relaxed to `NetworkType.CONNECTED` (any network, including mobile data).
- On partial failure, `doWork()` returns `Result.retry()` — safe to retry from scratch since both upload (`is_uploaded` flag) and download (file-existence check) are idempotent; already-done items are skipped on the next attempt, only the failed ones are retried.
- Requires `Configuration.Provider` + `HiltWorkerFactory` wiring in `QuickStoreApplication`, and the default `WorkManagerInitializer` disabled in the manifest (`tools:node="remove"` on the `androidx.startup.InitializationProvider` entry) — the two can't coexist.
- WorkManager's own `SystemForegroundService` (the service that actually hosts every worker's `setForeground()` call) declares no `foregroundServiceType` in its own manifest by default — on API 34+ this crashes `setForeground(FOREGROUND_SERVICE_TYPE_DATA_SYNC)` with `IllegalArgumentException` even with correct code. Fixed by overriding that `<service>` entry in the app's manifest (`tools:node="merge"`, `android:foregroundServiceType="dataSync"`).
- Verified end-to-end on a real device with real photos: upload confirmed working.

## Key Technical Notes

- **Quantities**: `Double` throughout (inventory + movements) — supports fractional units.
- **Stock warning**: triggered when the article's TOTAL quantity across all locations (`InventoryDao.getTotalByArticle`, summed) `<= article.reorderLevel`. An optional per-location threshold also exists (`article_location_thresholds`), not yet wired into any UI warning.
- **`AddArticle` screen is dual-mode**: create vs. edit is detected from whether the `articleId` nav argument is present (the `EditArticle` route also renders `AddArticleScreen`).
- **OpenCV NDK ABI filters**: `armeabi-v7a`, `arm64-v8a`, `x86`, `x86_64` — all four are included.
- **Keystore**: signing config reads from `keystore.properties` (not committed). Release builds require this file.
