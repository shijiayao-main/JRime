# Design: single-schema

## Context

Roadmap revision 5 direction, refined by the user on 2026-11-15: keep only
飞鹤快拼 (`flypy_xhfast`) plus its embedded English; behave like a mainstream
IME (one schema, in-schema toggles), no multi-schema machinery.

## Removal Map

### Assets (assets/shared/)

| Remove | Reason |
|---|---|
| `easy_en.schema.yaml`, `ecdict.schema.yaml` | standalone schemas; English is embedded in flypy_xhfast |
| ~~`flyhe_fast`~~ — **kept as a hidden base schema** (see below) | initially removed as "second variant"; deployment verification exposed that `flypy_fuzzy` (简拼, enabled in the user's custom) and `cand_selector` (single-char/emoji auto-commit) are built on flyhe's table — restored via `dependencies`, never listed in schema_list |
| `flypy_reverse.schema.yaml` + `.dict.yaml` | standalone reverse lookup, unused |
| ~~`melt_eng.dict.yaml`~~ (root) | kept: mounted by the restored flyhe base schema |
| `ecdict.dict.yaml` (26M) | Chinese glosses for English candidates — decoration, dropped |
| `easy_en.custom.yaml`? — none exists | n/a |

Kept: `flypy_xhfast.*`, `flypy_radical.*` (declared dependency of the main
schema), `flyhe_fast.*` + `cn_dicts/flyhe_chars` + root `melt_eng.dict.yaml`
(hidden base schema for 简拼/cand_selector, deployed via dependencies, never
selectable), `cn_dicts/{flypy_chars,flypy_chars_ext,flypy_base,flypy_melt_eng}`,
`easy_en.dict.yaml` + `en_dicts/*` (embedded English vocabulary chain),
lua/, opencc/, global assets. The tone-hint path in `flypy_switcher.lua`
gained a nil guard; with flyhe restored it works again.

### Config adaptations

- `default.custom.yaml`: `schema_list` collapses to `- schema: flypy_xhfast`;
  the `switcher/save_options` entry for the removed `easy_en-ecdict` switch is
  dropped with its lua.
- `flypy_xhfast.custom.yaml`: remove the `Control+Shift+E → select: easy_en`
  keybinding and any patch keys that address ecdict or easy_en-as-schema;
  the in-schema `easy_en/+:` patch block (embedded English tuning) stays.
- `UserDataManager.ensureDefaultCustomConfig`: fallback patch lists only
  `flypy_xhfast`; the legacy-cleanup constant stays as-is (upgrades).

### Lua adaptation (embedded easy_en)

`lua/easy_en.lua` reads `Schema("easy_en")` (the standalone schema) to feed
ecdict comments. With the standalone schema gone, `Schema()` returns nil and
the processor would throw. Change: drop the ecdict comment path from
`lua/easy_en.lua` entirely (comments were the only thing it did with ecdict)
and read the embedded `easy_en` block from the *current* schema's config
where the comment logic still needs parameters; if nothing else in the file
depends on the standalone schema, the whole init shrinks accordingly.
`lua/flypy_switcher.lua` references the `easy_en-ecdict` option — remove that
option from its menu wiring. Verify by deployment (a throwing lua processor
fails schema load, so the emulator smoke catches it).

### App code / UI

- `ui/main/settings/schema/` (SchemaListFragment etc.) — delete; remove the
  Schemata navigation route, menu entry, and any `RimeSchema`-list UI call
  sites that exist only for management.
- The schema's runtime plumbing (`RimeSchema`, schema switching on hotkey,
  the F4 in-schema switcher) stays: librime needs a current schema, and the
  F4 menu now only lists in-schema options.

## Risks / Trade-offs

- [Lua breakage] The largest risk: any lua still referencing the dropped
  schemas or options throws at load and kills the schema. Mitigation:
  grep-driven cleanup (`easy_en-ecdict`, `Schema("easy_en")`,
  `select: easy_en`) + full redeploy smoke.
- [English glosses gone] Accepted behavior change (user-approved direction);
  English input itself is untouched (vocabulary chain kept).
- [Existing installs] A user-authored multi-schema default.custom.yaml keeps
  winning (librime semantics); dropped schemas resolve to nothing. The only
  user is the author; acceptable.
- [flypy_radical kept] It is a declared dependency of the kept schema
  (component reverse lookup while typing) at 1.3M; dropping it would require
  editing the schema's dependencies list — not worth it, revisit on demand.

## Migration Plan

Preset `default.custom.yaml` collapses in-place; checksums regeneration
handles the on-device diff (DataDiff already deletes removed files). No code
migration needed beyond the fallback constant.

## Open Questions

1. `flypy_reverse`-related lua/preset references beyond the schema file
   (grep at implementation time; clean or neutralize per finding).
