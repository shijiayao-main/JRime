# Proposal: single-schema

## Why

JRime has exactly one user with exactly one daily schema: 飞鹤快拼
(`flypy_xhfast`, flypy shuangpin + auxiliary codes). English input is not a
separate schema — it is embedded in `flypy_xhfast` itself
(`affix_segmentor@easy_en` + `table_translator@easy_en` + the
`easy_en: dictionary: easy_en` block reading `easy_en.dict.yaml`), exactly
like a mainstream IME's built-in English. The other schemas
(`easy_en`/`ecdict`/`flyhe_fast`/`flypy_reverse`) are generic-distribution
affordances — Rime's multi-schema history — that the specialized build never
switches to.

Cutting them shrinks the APK from 46M to ~17M (ecdict alone is 26M and only
decorates English candidates with Chinese comments), halves the deploy task
list (6 units → 2: `flypy_xhfast` + its radical-reverse dependency), and
removes the multi-schema management UI that a single-schema IME (like every
mainstream one) does not have.

## What Changes

- **Ship exactly one schema**: `flypy_xhfast` (with its declared dependency
  `flypy_radical`, the component reverse lookup, kept as a typing aid).
- **Drop** the standalone schemas `easy_en`, `ecdict`, `flypy_reverse`.
  `flyhe_fast` ships on as a **hidden base schema** (deployed via the main
  schema's `dependencies`, never listed in schema_list): the 简拼
  (`flypy_fuzzy`) and single-char/emoji auto-commit (`cand_selector`)
  features are built on its tables. Schema-only dictionaries dropped: `ecdict.dict.yaml`
  (26M, Chinese comments for English candidates), `cn_dicts/flyhe_chars`,
  root `melt_eng.dict.yaml` (a flyhe_fast-only mount), `flypy_reverse.dict.yaml`.
  The embedded English vocabulary chain (`easy_en.dict.yaml` + `en_dicts/*`
  imports) stays — it is part of flypy_xhfast.
- **Adapt the embedded English plumbing** to the dropped ecdict: the
  easy-en lua comment logic and the custom patch entries referencing
  ecdict/easy_en-as-schema are removed or neutralized.
- **Collapse** `default.custom.yaml` to the single schema; the
  code-generated fallback in `UserDataManager.ensureDefaultCustomConfig`
  follows.
- **Remove** the multi-schema management UI (Schemata settings page and its
  navigation/menu entries). The F4 switcher stays as librime's built-in
  in-schema option menu (simplified/traditional, ASCII punctuation).
- Known behavior change: English candidates no longer carry Chinese glosses.

## Capabilities

### Added

- `input-schemas`: the specialized single-schema contract.

## Impact

- APK ~46M → ~17M; fresh-install deploy units 6 → 2 (the 980k-entry ecdict
  compilation disappears).
- Fresh installs and upgrades resolve exactly one schema; a user-authored
  multi-schema `default.custom.yaml` in the user dir still wins over the
  preset (librime semantics), but the non-shipped schemas it lists simply
  resolve to nothing.
