# profile-storage — ADDED

## ADDED Requirements

### Requirement: single storage mode

The app SHALL store all rime data (shared mirror, user data dir, user
dictionaries) under app-specific external storage only. No external-tree
synchronization mode SHALL exist.

#### Scenario: data lives under app-specific storage

- **WHEN** the engine starts in any boot mode
- **THEN** shared data is resolved under `<externalFilesDir>/shared` and user
  data under `<externalFilesDir>/rime` (device-protected variant before first
  unlock, unchanged)
- **AND** no SAF tree URI is recorded, read, or required

#### Scenario: former EXTERNAL_SYNC installs

- **WHEN** an existing installation carries a recorded external sync tree
- **THEN** the app operates in app-specific storage semantics
- **AND** no mirror of the external tree is performed

### Requirement: enable-state guidance is non-blocking

The main screen SHALL surface, without blocking entry, whether the IME is
enabled and selected, linking to the system input-method settings when either
is not satisfied.

#### Scenario: banner for unenabled IME

- **WHEN** the main screen is resumed and the IME is not enabled or not
  selected
- **THEN** a non-blocking notice with a link to the system input-method
  settings is visible
- **AND** the rest of the main screen remains usable

#### Scenario: no banner once active

- **WHEN** the IME is enabled and selected
- **THEN** no enable-state notice is shown
