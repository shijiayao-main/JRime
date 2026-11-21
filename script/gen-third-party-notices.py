#!/usr/bin/env python3
"""Regenerate THIRD_PARTY_NOTICES.md from the AboutLibraries artifacts.

Run after changing dependencies (or after a build that refreshed
app/build/generated/aboutLibraries/*/res/raw/aboutlibraries.json):

    python3 script/gen-third-party-notices.py
"""

import glob
import json
import os
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "THIRD_PARTY_NOTICES.md")


def load_build_metadata():
    candidates = sorted(
        glob.glob(
            os.path.join(ROOT, "app/build/generated/aboutLibraries/*/res/raw/aboutlibraries.json")
        )
    )
    for path in candidates:
        try:
            with open(path, encoding="utf-8") as fh:
                return json.load(fh)
        except (OSError, json.JSONDecodeError):
            continue
    return {"libraries": []}


def load_native_configs():
    entries = []
    directory = os.path.join(ROOT, "app/licenses/libraries")
    if not os.path.isdir(directory):
        return entries
    for name in sorted(os.listdir(directory)):
        if not name.endswith(".json"):
            continue
        with open(os.path.join(directory, name), encoding="utf-8") as fh:
            entries.append(json.load(fh))
    return entries


def license_of(lib):
    licenses = lib.get("licenses") or []
    names = []
    for lic in licenses:
        if isinstance(lic, dict):
            names.append(lic.get("spdxId") or lic.get("name") or "")
        else:
            names.append(str(lic))
    return ", ".join(n for n in names if n) or "unknown"


def main():
    metadata = load_build_metadata()
    native = load_native_configs()
    libraries = metadata.get("libraries", [])
    app_libs = [l for l in libraries if l.get("tag") != "native"]
    app_libs.sort(key=lambda l: (l.get("uniqueId") or "").lower())
    native_lines = []
    for lib in sorted(native, key=lambda l: (l.get("name") or l.get("uniqueId") or "").lower()):
        native_lines.append(
            "| {name} | {version} | {lic} | {site} |".format(
                name=lib.get("name") or lib.get("uniqueId") or "",
                version=lib.get("artifactVersion") or "",
                lic=license_of(lib),
                site=lib.get("website") or "",
            )
        )
    lib_lines = []
    for lib in app_libs:
        lib_lines.append(
            "| {name} | {version} | {lic} | {site} |".format(
                name=lib.get("name") or lib.get("uniqueId") or "",
                version=lib.get("artifactVersion") or "",
                lic=license_of(lib),
                site=lib.get("website") or "",
            )
        )
    body = f"""# Third-party notices

JRime is licensed under **GPL-3.0-or-later**. It is built on the open-source Rime
ecosystem and on many application libraries. This file is generated from the
build metadata (`aboutlibraries` + `app/licenses/libraries`) so the list reflects
what is actually packaged.

Regenerate with:

```sh
python3 script/gen-third-party-notices.py
```

In-app: **设置 → 关于 → 开源说明** (or About → Open source licenses) lists every
library with its license and links.

## Rime engine and native components

| Component | Version | License | Website |
|---|---|---|---|
{os.linesep.join(native_lines) if native_lines else "| (none) | | | |"}

The Rime engine stack is credited to the [Rime community](https://rime.im/)
(librime, OpenCC and their bundled libraries). JRime's Android frontend derives
from [Trime](https://github.com/osfans/trime); thank you to its authors and
contributors.

## Application, UI and tooling libraries

| Library | Version | License | Website |
|---|---|---|---|
{os.linesep.join(lib_lines) if lib_lines else "| (none) | | | |"}

> Versions are captured from the last `assembleDebug`/`assembleRelease` metadata.
> If a table is empty, build the app once and rerun the generator.
"""
    with open(OUT, "w", encoding="utf-8") as fh:
        fh.write(body)
    print(f"wrote {OUT}: {len(native_lines)} native, {len(lib_lines)} libraries")


if __name__ == "__main__":
    main()
