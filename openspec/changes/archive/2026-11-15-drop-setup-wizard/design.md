# Design: drop-setup-wizard

## Context

Roadmap revision 4 (2026-11-15). The wizard and the EXTERNAL_SYNC machinery
are generic-distribution affordances; the specialized build has exactly one
user whose data model is "APK carries everything, device accumulates userdb".

## Goals / Non-Goals

- **Goal**: fresh install lands in the main screen; APP_STORAGE is the only
  storage mode; the SAF mirror / periodic sync / StorageAccess dependency are
  gone; the enable-IME guidance remains visible but non-blocking.
- **Non-Goal**: touching `RimeDataProvider` (file-manager convenience),
  userdb import/export (levers), the direct-boot dual-directory logic, or the
  main-screen Compose migration (phase 5).

## Removal Map (verified against the current tree)

| Piece | Path | Note |
|---|---|---|
| Wizard UI | `ui/setup/` (SetupActivity + fragments) | delete; remove the launcher redirect in `MainActivity` / launcher alias |
| Storage-mode enum | `core/data/DataStorageMode.kt` | delete the enum; callers converge on APP_STORAGE semantics |
| Storage preference UI | `ui/main/settings/ProfileSettingsFragment.kt:165-276` | remove the mode picker + external-dir row; keep the userdb import/export section |
| SAF mirror | `core/data/UserDataManager.kt` (externalRimeTreeUri / externalRimeDir / `.sync-manifest.json` / `hashStream` / `localHash` / `remoteHash` / the `EXTERNAL_SYNC` branch of `sync()`) | the APP_STORAGE branch of `sync()` is a no-op — collapse `sync()` away and drop its call sites (`Rime.syncUserData`, `Rime.deploy` pre/post hooks) |
| Periodic sync | `worker/BackgroundSyncWork.kt` + `TrimeApplication.startWorkManager` | delete both; WorkManager dependency leaves `libs.versions.toml` |
| SYNC broadcast | `receiver/RimeIntentReceiver.kt` `ACTION_SYNC_USER_DATA` | delete the action; `ACTION_DEPLOY` stays (it becomes a no-op caller once phase 2 removes deployment — out of scope here) |
| StorageAccess dep | `gradle/libs.versions.toml` `storageaccess` | delete after imports are gone |
| Prefs | `data/prefs/AppPrefs.kt` `profile.dataStorageMode` / `profile.externalRimeTreeUri` | delete both properties |

**Kept**: `Rime.syncUserData()`'s inner JNI `syncRimeUserData` + userdb
dump/notification stays if the userdb snapshot flow is still reachable from
ProfileSettingsFragment's "sync/backup" entry; otherwise trim that entry too.
Decide at implementation time by reading what the fragment actually exposes
after the mode picker is gone — the userdb export/import buttons are the
keeper, the tree mirror is not.

## Enable-state banner

`MainActivity` checks `InputMethodManager` enabled + selected IME lists on
resume; a `MaterialBanner`-style row (existing view system, no Compose yet)
links to `ACTION_INPUT_METHOD_SETTINGS`. Same information the wizard carried,
zero blocking.

## Risks / Trade-offs

- [Direct-boot manifest interplay] `UserDataManager`'s device-protected
  `.sync-manifest.json` bookkeeping disappears with the SAF branch — the
  direct-boot dual-copy logic it shared a file with (`DataManager`'s per-mode
  checksums record) is **not** affected; verified they are separate records.
- [Dead JNI surface] `Rime.syncRimeUserData` may lose its last caller. JNI
  surface is trimmed in phase 4's module split; leaving one unused export for
  one phase is cheaper than a JNI churn commit inside this change.
- [Regression risk] `ProfileSettingsFragment` still owns userdb import/export;
  its tests (none today) are not added here — covered by code review + smoke.

## Migration Plan

None needed: the storage-mode preference simply stops being read; existing
installs with `EXTERNAL_SYNC` recorded fall back to APP_STORAGE semantics
(their external tree is no longer mirrored, which is the intended end state).

## Open Questions

1. The "sync/backup" entry in ProfileSettingsFragment: keep only if it maps
   to the userdb levers flow after the mode picker is removed — settle while
   editing the fragment; both alternatives satisfy the acceptance criteria.
