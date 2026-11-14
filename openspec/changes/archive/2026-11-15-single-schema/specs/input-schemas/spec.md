# input-schemas — ADDED

## ADDED Requirements

### Requirement: single shipped schema

The app SHALL ship exactly one input schema: 飞鹤快拼 (`flypy_xhfast`),
together with its declared dependency `flypy_radical` (component reverse
lookup). No other standalone schema SHALL ship.

#### Scenario: fresh install deploys one schema and its dependency

- **WHEN** the engine deploys on a fresh install
- **THEN** exactly `flypy_xhfast` and its dependency `flypy_radical` are
  compiled and loadable
- **AND** the deployment completes with no missing-schema or lua errors

### Requirement: embedded English input

English input SHALL be provided inside the flypy_xhfast schema via its
embedded `easy_en` translator and vocabulary chain (`easy_en.dict.yaml` and
its `en_dicts` imports), not via a standalone English schema.

#### Scenario: typing English inside the Chinese schema

- **WHEN** the user types a Latin-key sequence while flypy_xhfast is active
- **THEN** English candidates from the embedded vocabulary appear and commit
- **AND** no schema switch is involved

#### Scenario: English candidates carry no Chinese glosses

- **WHEN** embedded English candidates are listed
- **THEN** they commit as English text, without Chinese comment glosses
  (the ecdict decoration is not shipped)

### Requirement: no multi-schema management UI

The app SHALL NOT expose a schema management page; the shipped schema is the
current schema. The librime-built-in switcher menu (in-schema options such as
simplified/traditional and ASCII punctuation) remains available on its
configured hotkeys.

#### Scenario: settings contain no schema list

- **WHEN** the user opens the settings pages
- **THEN** no schema management entry exists
