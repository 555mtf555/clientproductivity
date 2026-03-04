# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug

# Run unit tests
./gradlew test

# Run instrumented tests (requires device/emulator)
./gradlew connectedAndroidTest

# Run a single test class
./gradlew test --tests "com.example.clientproductivity.ExampleUnitTest"

# Clean build
./gradlew clean assembleDebug
```

## Architecture

**MVVM + Clean Architecture** with a single `:app` module.

```
data/
  dao/          → Room DAOs (TaskDao, ClientDao, ProjectDao, ActivityLogDao)
  entity/       → Room entities + TaskWithContext (joined query result)
  di/           → Hilt DataModule (DB, DAO, Repository singletons)
  backup/       → JSON export/import via kotlinx.serialization
  prefs/        → DataStore preferences (PreferenceManager)
  worker/       → WorkManager TaskReminderWorker (hourly check)
  TaskRepository / ClientRepository → business logic, wrap DAOs

viewmodel/
  TaskViewModel   → drives most app state; combines multiple Flows
  ClientViewModel → client/project CRUD
  BackupViewModel → export/import orchestration

ui/
  navigation/AppScaffold.kt → NavHost, bottom bar, FAB wiring
  screens/      → one file per screen composable
  components/   → CommonUi.kt, TaskComponents.kt (shared composables)
  theme/        → Material3 theming; CustomAppThemes has Blue/Green/Red presets
```

**Key patterns:**
- **StateFlow + combine**: ViewModels expose `StateFlow` derived via `combine()` and `stateIn(WhileSubscribed(5_000))`.
- **Soft delete**: Tasks use `isRemoved`/`removedAt` — never hard-deleted from the UI.
- **Joined queries**: `TaskWithContext` flattens task + client + project fields in a single DAO query.
- **Activity log**: Every task mutation inserts an `ActivityLogEntity` (cascade-deleted with its task).

## Tech Stack

| Layer | Library |
|---|---|
| UI | Jetpack Compose + Material3 |
| DI | Hilt (KSP) |
| DB | Room (KSP) |
| Nav | Navigation Compose |
| Async | Kotlin Coroutines + Flow |
| Prefs | DataStore |
| Background | WorkManager |
| Serialization | kotlinx.serialization (backup JSON) |

- `compileSdk 35`, `minSdk 24`, `jvmTarget 17`, core library desugaring enabled.
- KSP is used for both Room and Hilt annotation processing (not KAPT).

## Navigation

Defined in `AppScaffold.kt`. Routes:
- `dashboard`, `projects`, `clients`, `settings` — bottom nav tabs
- `new_task?projectId={projectId}` — optional Long arg
- `edit_task/{taskId}` — required Long arg
- `client_detail/{clientId}` — required Long arg

## Database

`AppDatabase` uses destructive migration fallback. When adding new entities or columns, increment `version` in `AppDatabase.kt` and add a `Migration` object, or accept data loss during development with `fallbackToDestructiveMigration()`.

`DateConverters.kt` serializes `java.time.Instant` to/from `Long` (epoch millis).

## Theming

`PreferenceManager` stores the selected theme key. `CustomAppThemes.kt` maps keys to `CustomAppTheme` objects. `ClientProductivityTheme.kt` reads the preference and applies the correct `ColorScheme`.
