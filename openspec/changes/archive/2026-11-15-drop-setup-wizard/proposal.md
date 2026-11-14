# Proposal: drop-setup-wizard

## Why

JRime is a single-user, specialized build: the schema set, dictionaries and
keyboard theme ship inside the APK, and the user data dir only accumulates the
LevelDB user dictionaries. The 3-step setup wizard (pick storage mode → enable
IME → set default) is a generic-distribution affordance that runs on every
fresh install and re-install for exactly one person who does not need it.

Half of step 1 exists only to serve the `EXTERNAL_SYNC` storage mode: a SAF
two-way mirror of the whole rime tree (full-tree walk + per-file SHA-256,
`UserDataManager.kt:162-235`), a WorkManager periodic job, and the
StorageAccess dependency. None of this is used by the specialized build — the
upstream-config sync workflow it served is replaced by "edit the repo, build,
install" (roadmap revision 4, 2026-11-15).

## What Changes

- **Remove** the setup wizard (`ui/setup/`, the launcher redirect into it) and
  the `EXTERNAL_SYNC` storage path: the storage-mode preference UI in
  `ProfileSettingsFragment`, the SAF mirror in `UserDataManager`
  (external tree URI, sync manifest, per-file hashing), `BackgroundSyncWork`
  and its WorkManager wiring, the `SYNC_USER_DATA` broadcast action, and the
  StorageAccess dependency.
- **Hardcode** APP_STORAGE as the only storage mode; the device-protected
  direct-boot directory logic stays as-is (orthogonal to sync mode).
- **Degrade** the "enable / set default" guidance to a non-blocking banner in
  the main screen — enabling an IME is a system-forced step no app can perform
  for itself, so the wizard's step 2/3 information still has to surface, but
  without blocking entry.

## Capabilities

### Modified

- `profile-storage`: APP_STORAGE becomes the only mode.

### Removed

- `setup-wizard`: the first-run wizard flow.

## Impact

- Fresh installs start directly in the main screen.
- The userdb import/export paths (levers backup/restore, the remaining reason
  `UserDataManager` exists) are untouched.
- `RimeDataProvider` (the SAF DocumentsProvider exposing the data dir to file
  managers) is untouched — it is an independent convenience, not part of the
  sync machinery.
