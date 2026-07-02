# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**QuickStore** is an Android inventory management app with image-based article search. It uses a phone camera + OpenCV (ORB feature extraction + BFMatcher) to identify warehouse articles by photo. The app is published on the Play Store (`net.calvuz.quickstore`).

- **Min SDK**: 33 (Android 13)
- **Target SDK**: 35
- **Kotlin**: 2.1.0 / JVM 17
- **Current DB version**: 5 (MIGRATION_3_4: multi-location — `locations`, `article_location_thresholds`, `movements.id` Long→UUID, `from_location_uuid`/`to_location_uuid`, `inventory` PK composta articolo+ubicazione, `MovementType.ADJUSTMENT`/`TRANSFER`; MIGRATION_4_5: `movements.created_by`)

## Stato attuale — sync multi-device (HANDOFF, 2026-07-02)

QuickStore sta passando da app puramente offline a un modello opzionale multi-device: un
backend dedicato (`../quickstore-server`, Ktor + Postgres, repo separato) più due nuovi
feature module qui (`auth`, `sync`). **L'app resta comunque fruibile offline al 100% senza
mai fare login** — questo è un requisito guida, non un dettaglio.

**Verificato end-to-end** (device fisico reale, contro `https://quickstore.calvuz.net`):
login (diretto e multi-org con select-org), messaggi di errore puliti (niente eccezioni
tecniche a schermo), push+pull manuale con dati reali già presenti sul device.

**In corso di verifica dall'utente**: pull "da zero" su un device/emulatore vergine mai
sincronizzato prima (`since=0`) — è il test che stressa di più l'ordine di dipendenza
dell'upsert (categorie → ubicazioni → articoli → soglie → movimenti → immagini) e i
vincoli FK di Room.

**Non ancora fatto** (elencato per priorità presunta, nessun ordine impegnativo):
1. Canale WebSocket (`ws /sync/ws`) per il nudge near-realtime + `WorkManager` per una
   pull periodica di sicurezza — il server li supporta già, lato Android sono
   deliberatamente rinviati (vedi sezione "Sync" più sotto) per verificare push/pull
   manuale in isolamento prima di aggiungere l'automazione in background.
2. Propagazione delle cancellazioni locali → server: le entity locali non hanno un flag
   `isDeleted`, quindi oggi solo la direzione server → locale funziona (vedi sezione
   "Sync"). Richiederebbe un'altra migrazione Room.
3. Upload/download delle immagini reali (JPEG): il server ha già
   `POST/GET /images/upload|download/{id}` (vedi `quickstore-server/CLAUDE.md` sezione 9),
   ma il payload di sync qui porta solo `featuresData` (i descrittori OpenCV, piccoli,
   via Base64) — il file JPEG vero non viaggia ancora.
4. UI di gestione membership (invita/cambia ruolo/rimuovi) e lettura audit log: gli
   endpoint server esistono (`quickstore-server/CLAUDE.md` sezione 9), nessuna schermata
   Android li usa ancora.
5. `org_id` sulle entity sincronizzate: deciso di **non** aggiungerlo (i DTO di rete non lo
   portano comunque, lo inietta il server dal JWT) a meno che non emerga un vero bisogno
   di isolare dati multi-org sullo stesso device.
6. Redesign del formato di backup per il multi-magazzino (rinviato quando abbiamo fatto la
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

**Known limitations of this first version** (see comments in `SyncRepositoryImpl.kt`):
- Local entities (`article_categories`, `articles`, `locations`, `article_location_thresholds`, `article_images`) have **no `isDeleted` flag** — local deletions are hard-deletes and never propagate to the server. A remote deletion (`isDeleted=true` in a pull) *is* applied locally (row deleted). Fixing the local→remote direction needs its own migration adding soft-delete everywhere, not done here.
- `article_images` has no `updated_at` locally; `created_at` is used as a stand-in on both sides of the sync.
- No WebSocket client, no periodic background sync — sync only happens when the user taps the button.

## Key Technical Notes

- **Quantities**: `Double` throughout (inventory + movements) — supports fractional units.
- **Stock warning**: triggered when the article's TOTAL quantity across all locations (`InventoryDao.getTotalByArticle`, summed) `<= article.reorderLevel`. An optional per-location threshold also exists (`article_location_thresholds`), not yet wired into any UI warning.
- **`AddArticle` screen is dual-mode**: create vs. edit is detected from whether the `articleId` nav argument is present (the `EditArticle` route also renders `AddArticleScreen`).
- **OpenCV NDK ABI filters**: `armeabi-v7a`, `arm64-v8a`, `x86`, `x86_64` — all four are included.
- **Keystore**: signing config reads from `keystore.properties` (not committed). Release builds require this file.
