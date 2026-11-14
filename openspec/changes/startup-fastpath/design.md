# Design: startup-fastpath

## Context

Roadmap revisions 1+2+5 (2026-11-15). Phase-1 baselines on the emulator:
fresh-install deploy ~105s, all device-side compilation of build-time-frozen
inputs. librime's shipped-`build/` (prebuilt) mechanism already exists; the
change moves compilation to the host and the sources into the repo.

## Goals / Non-Goals

- **Goal**: fresh install compiles nothing; startup = asset release + engine
  load; theme surface trimmed to what the user's keyboard actually references.
- **Non-Goal**: pruning deployer/dict-compiler from the device build (deferred
  — maintenance that skips costs nothing); Room/config-structuring (phase 3);
  rewriting the theme cache (the deploy-wait problem is gone once prebuilt).

## 1. Vendoring

- **Scope**: `app/src/main/jni/` — librime (1.17.0, submodule → source
  snapshot), librime-lua, librime-lua-deps, librime-octagram, librime-predict,
  snappy, and librime's nested submodules it builds against; `app/data/rime/
  prelude` (still a compile input: key_bindings/punctuation/symbols) and the
  default.yaml base is already a plain file. luna-pinyin/stroke/essay
  submodule pointers are deleted outright (their assets were removed in
  phase 1).
- **Form**: `git rm` the gitlinks, commit the checked-out trees as plain
  sources. Remove all entries from `.gitmodules`. Keep LICENSE/SPDX headers
  intact (GPL obligations unchanged by vendoring).
- **Build impact**: none for the device build (same CMake inputs); the host
  tool builds from the same vendored tree.
- **Maintenance**: no upstream tracking; fixes land as project commits.

## 2. Host precompilation

- **Tool**: build librime's own deployment tooling for the host (the tools
  target that links the deployer/config/dict compilers), invoked once over a
  staging copy of `assets/shared/` (+ the prelude inputs) with a scratch user
  dir. Output: `build/` artifacts exactly as a device deployment would
  produce.
- **Products into `assets/shared/build/`**: `*.table.bin`, `*.prism.bin`,
  `*.reverse.bin` for flypy_xhfast/flyhe_fast/flypy_radical, expanded
  `flypy_xhfast.schema.yaml`/`flyhe_fast.schema.yaml`/`flypy_radical.schema.yaml`
  and expanded `default.yaml`.
- **YAML sources stop shipping**: after the spike proves the prebuilt loads,
  dictionary/schema YAMLs are removed from assets (the host tool keeps its
  own input copy — inputs live in the repo, e.g. under `app/data/rime-src/`,
  compiled at build time; exact split settled in the spike to keep the gradle
  wiring simple).
- **Gradle wiring**: a convention plugin task `precompileRimeData` runs
  before `generateDataChecksums`; outputs are ordinary asset files, so the
  existing checksums/diff/release machinery carries them to the device
  unchanged. Host tool bootstrap (cmake build) is cached like the NDK builds.
- **Config expansion on device is fine to keep**: `default.custom.yaml` /
  user patches still apply at load time — wait, no: with `build/default.yaml`
  prebuilt, the runtime still consults the user dir's `default.custom.yaml`
  patch chain? — **Open Question 1** (below).

## 3. Device startup path (after)

```
install → DataManager.sync (checksums diff, releases shared/ incl. build/)
        → librime start_maintenance → prebuilt current, hash-unchanged → skip
        → engine loads tables/prisms from user-rime/build via prebuilt fallback
        → READY (target: seconds)
```

`ensureDefaultCustomConfig` stays as the fallback safety net. `deploy()` stays
as a no-op-ish repair path (maintenance re-checks; recompiles only if
artifacts are missing — which doubles as the corruption recovery story: see
Risks).

## 4. Theme surface trim

Mechanical closure derivation (same method as the built-in manifest): from
`flyhe.trime.yaml`'s `default` keyboard and its liquid/menu references,
collect referenced keyboards → keep those; palettes → keep `default` + the
dark-scheme link; preset_keys → union of the kept keyboards' references;
remove the 15 legacy style keys flagged by ThemeDiagnostics. Output the
keep-list for review, then apply, then update golden tests (counts change
again — third time, by design: the theme shrinks to what is used).

## Risks / Trade-offs

- [Bin platform independence] marisa/leveldb/prism formats are
  little-endian, no host-endianness shortcuts in librime's table code; still
  the spike must prove host-x86_64 artifacts load on the emulator, and a real
  arm64 device check is called out in the acceptance before this change
  archives.
- [Artifact/user-dir interplay] Open Question 1: which layer does
  `default.custom.yaml` (preset + user) patch — does the engine re-expand
  configs at load even when `build/*.yaml` is prebuilt, or is the prebuilt
  the final word and custom patches must move to the host tool? Resolve in
  the spike by observing a prebuilt install with the preset
  default.custom.yaml present (schema list must still resolve to flypy_xhfast
  and hotkeys/menu must apply); if patches are not re-applied, the host tool
  runs the full patch chain (preset custom included) and the preset
  default.custom.yaml ships only as documentation.
- [Corrupted artifacts] with no on-device compiler until the pruning phase,
  a corrupt `build/` file would brick the schema until reinstall — mitigated
  now by deployer still being present (repair = `deploy()`), long-term by the
  phase-3 Room/artifact-hash record.
- [Vendor drift] no upstream pulls; acceptable per revision 1.

## Open Questions — answered by the spike (2026-11-15)

1. **Patch-chain semantics**: with the prebuilt set staged into the user
   dir's `build/`, the engine STILL re-expands the config chain
   (default/default.custom/symbols/*.custom) at deploy time — the log shows
   `resource not loaded, compiling: <config>` for each — but performs **zero
   dictionary compilation** (no `Pass 1` lines at all). Config re-expansion
   is milliseconds; it is accepted as runtime behavior and the preset
   default.custom.yaml stays functional.
2. **Plugin artifacts**: not touched by the host tool; gram/predict.db remain
   user-dir imports. Confirmed by the spike run (host tool output contains
   only the 3 schemas' bin + expanded configs).

## Spike record (tasks 2.1–2.3)

- Host tool: `app/src/main/jni/host/CMakeLists.txt` builds vendored
  librime+deps for the host; `rime_deployer` at
  `host/build-host/librime-build/bin/rime_deployer`. Two librime CMakeLists
  changes were needed: `RIME_BUILD_TOOLS` option (tools were shared-only) and
  a `RIME_HOST_TOOLS` bypass for the Linux `find_package(Boost REQUIRED)`
  branch.
- Precompile run: `rime_deployer --build <scratch-user> <assets-shared-copy>
  <scratch-user>/build` completes in **6s** on the host; artifact inventory
  matches the device deployment byte-size-for-byte
  (flypy_xhfast.table.bin 31275480 etc.).
- **Staging pre-provisioning is the working layout**: librime's deployer does
  not treat table/reverse.bin as installable prebuilt resources (only
  schema.yaml + prism.bin are copied from prebuilt), so shipping them under
  `shared/build/` leaves the engine recompiling tables. The artifacts are
  staged instead as `assets/rime/build/` → released into the user dir's
  staging (`<externalFilesDir>/rime/build/`) by the existing checksums
  release. Verified on the emulator: deploy 3/3 success, **zero dictionary
  compilation**, first-launch deploy+startup wall clock **~16s vs the 105s
  baseline** (config re-expansion only). Typing regression pending the
  acceptance round.
- Note: `assets/rime/build/` artifacts are gitignored by the global `build/`
  pattern and intentionally stay out of git; task 3.1's
  `precompileRimeData` gradle task generates them at build time (the spike
  copies are local working-tree state).
