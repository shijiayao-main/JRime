# Proposal: startup-fastpath

## Why

Phase-1 baselines (Android 16 x86_64 emulator): fresh-install deployment takes
~105s (was 285s before single-schema) — all of it spent compiling dictionaries
and configs **on the device** that are fully determined **at build time**: the
YAML sources are frozen in the repo, the compilers are deterministic, and
nothing on the device ever changes them. Every fresh install and every
re-install pays that cost for zero benefit.

librime's own distribution format already answers this: a shipped
`build/` directory of precompiled artifacts (prebuilt_data_dir) that the
engine loads directly and its maintenance check skips. The missing piece is
only that the compile step runs on a developer machine at build time instead
of on the phone at first run.

Doing that compile on the host requires controlling the librime build and
sources — the submodule-pointer form cannot carry patches (roadmap revision 1:
vendor librime), and with the sources vendored, the device build can also drop
the deployer/compiler machinery later.

## What Changes

- **Vendor librime and its plugin/dependency trees into the repository**:
  librime 1.17.0, librime-lua (+ deps), librime-octagram, librime-predict,
  snappy and librime's nested dependencies, plus the rime-data submodules
  still used as compile inputs (prelude). Submodule pointers are removed;
  sources are committed as a snapshot; upstream is no longer tracked.
- **Host-side precompilation**: build a host rime deployment tool from the
  vendored sources and run it at build time over the shipped asset set,
  producing `assets/shared/build/` (the engine's prebuilt layout:
  `*.table.bin`, `*.prism.bin`, `*.reverse.bin`, expanded `*.schema.yaml`,
  expanded `default.yaml`). YAML dictionary sources stop shipping in the APK.
- **Device startup becomes**: release assets (existing checksums diff) →
  engine maintenance check sees prebuilt artifacts → zero compilation.
  Fresh-install deploy target: seconds, not minutes; the 105s baseline is the
  comparison number.
- **Spike first**: host x86_64 compile → x86_64 emulator load → (device
  follow-up) arm64 real device; artifact platform-independence is verified
  before anything else lands.
- **Theme surface trim** (roadmap revision 5): flyhe.trime.yaml's 23 preset
  keyboards → the referenced closure, 18 palettes → the 1-2 the user keeps,
  215 preset keys → the keyboard-referenced closure, 15 legacy style keys
  removed; golden tests updated to the trimmed truth.
- Device-build pruning of the deployer/dict-compiler modules is **deferred**
  (they cost nothing when maintenance skips; the native-size win goes to a
  later phase) — noted so it is not lost.

## Capabilities

### Added

- `deployment-precompile`: the prebuilt-artifact contract (what ships, what
  the device loads, what "no compile on device" means).

## Impact

- First install on a device: no dictionary/config compilation; deploy wait
  collapses from ~105s to the asset-release time.
- APK drops the ~30M of YAML dictionary sources, gains the (smaller) binary
  tables — expected net smaller.
- Repo grows by the vendored native sources (~100M working tree, well below
  the old .git history we already discarded).
- Upstream librime updates become manual merges (intended: specialized fork).
