# deployment-precompile — ADDED

## ADDED Requirements

### Requirement: prebuilt artifacts ship with the APK

The build SHALL compile all shipped rime dictionaries and configs on the host
and ship the engine's prebuilt artifacts (`build/` layout: `*.table.bin`,
`*.prism.bin`, `*.reverse.bin`, expanded `*.schema.yaml`, expanded
`default.yaml`) inside the APK. Dictionary and schema YAML sources SHALL NOT
ship in the APK.

#### Scenario: fresh install compiles nothing

- **WHEN** the app is freshly installed and the engine starts
- **THEN** the maintenance check finds the prebuilt artifacts current and
  performs no dictionary or config compilation
- **AND** the engine becomes ready from prebuilt artifacts

#### Scenario: prebuilt artifacts match a device deployment

- **WHEN** the host-precompiled artifact set is compared with a device-side
  deployment of the same inputs
- **THEN** the artifact inventory (names, per-schema set) matches

### Requirement: host-built artifacts load across architectures

Prebuilt artifacts produced on the host (x86_64) SHALL load and serve typing
on the emulator and on arm64 devices without recompilation.

#### Scenario: emulator loads host artifacts

- **WHEN** host-built `build/` artifacts are installed on an x86_64 emulator
- **THEN** schemas become ready without compilation and typing commits
  normally

### Requirement: repair path stays available

A user-triggered redeploy SHALL remain available as the repair path for
missing or corrupted prebuilt artifacts, recompiling from the shipped
sources' deployed form.

#### Scenario: corrupted artifacts recover via redeploy

- **WHEN** prebuilt artifacts on the device are missing or corrupt and the
  user triggers a redeploy
- **THEN** the engine recompiles what is needed and typing recovers

### Requirement: theme surface matches usage

The shipped keyboard theme SHALL contain the preset keyboards, palettes and
preset keys its default (and referenced) layouts actually use; unreferenced
surplus keyboards, palettes, preset keys and legacy style keys are removed.

#### Scenario: shipped theme is the referenced closure

- **WHEN** the shipped theme is parsed
- **THEN** every preset keyboard is reachable from the default keyboard's
  navigation references
- **AND** no legacy unknown style keys remain
