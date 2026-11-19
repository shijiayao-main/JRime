#!/usr/bin/env python3
"""End-to-end test of the desktop engine server line protocol (macOS host).

Compiles desktop/engine, stages the real rime assets, then drives
DesktopServer over stdin/stdout and asserts the full typing flow:
ready -> n i h c -> first candidate 你好 -> sel 0 -> commit 你好.

Prereq: harness/build/librime_jni.dylib exists (run ./harness/smoke.sh once).
"""
import hashlib
import json
import os
import shutil
import subprocess
import sys
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "app/src/main/assets"
DYLIB = ROOT / "harness/build/librime_jni.dylib"
JAVA = Path(subprocess.run(["/usr/libexec/java_home"], capture_output=True,
                           text=True).stdout.strip())


def _load_toolchain_properties() -> dict:
    """Parse script/toolchain.properties (toolchain single source of truth)."""
    props = {}
    for line in (ROOT / "script/toolchain.properties").read_text().splitlines():
        line = line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, _, value = line.partition("=")
        props[key.strip()] = value.strip()
    return props


_TOOLCHAIN = _load_toolchain_properties()
KOTLIN_VERSION = _TOOLCHAIN["KOTLIN_VERSION"]
KOTLIN_COMPILER_SHA256 = _TOOLCHAIN["KOTLIN_COMPILER_SHA256"]


def ensure_kotlinc() -> Path:
    build = ROOT / "desktop/build"
    dist = build / f"kotlinc-{KOTLIN_VERSION}"
    if (dist / "bin/kotlinc").is_file():
        return dist
    # reuse the copy harness/smoke.sh fetches (build.sh step [1/6] does too)
    harness_dist = ROOT / "harness/build" / f"kotlinc-{KOTLIN_VERSION}"
    if (harness_dist / "bin/kotlinc").is_file():
        return harness_dist
    build.mkdir(parents=True, exist_ok=True)
    archive = build / "kotlin-compiler.zip"
    url = (f"https://github.com/JetBrains/kotlin/releases/download/"
           f"v{KOTLIN_VERSION}/kotlin-compiler-{KOTLIN_VERSION}.zip")
    subprocess.run(["curl", "-fL", "-o", str(archive), url], check=True)
    digest = hashlib.sha256()
    with archive.open("rb") as f:
        for chunk in iter(lambda: f.read(1 << 20), b""):
            digest.update(chunk)
    if digest.hexdigest() != KOTLIN_COMPILER_SHA256:
        sys.exit(f"kotlin-compiler.zip SHA256 mismatch: expected "
                 f"{KOTLIN_COMPILER_SHA256}, got {digest.hexdigest()}")
    shutil.rmtree(build / "kotlinc", ignore_errors=True)
    with zipfile.ZipFile(archive) as z:
        z.extractall(build)
    (build / "kotlinc").rename(dist)
    # zipfile.extractall does not preserve the executable bit from the archive
    for entry in (dist / "bin").iterdir():
        if entry.is_file():
            entry.chmod(0o755)
    return dist


def compile_engine() -> Path:
    classes = ROOT / "desktop/build/classes"
    shutil.rmtree(classes, ignore_errors=True)
    classes.mkdir(parents=True)
    kotlinc = ensure_kotlinc()
    sources = [str(p) for p in (ROOT / "desktop/protocol-common").rglob("*.kt")]
    sources += [str(p) for p in (ROOT / "desktop/engine/java").rglob("*.kt")]
    env = {**os.environ, "JAVA_HOME": str(JAVA)}
    subprocess.run([str(kotlinc / "bin/kotlinc"), "-d", str(classes), *sources],
                   check=True, env=env)
    with zipfile.ZipFile(kotlinc / "lib/kotlin-stdlib.jar") as z:
        z.extractall(classes)
    return classes


def stage_shared(dest: Path) -> None:
    if dest.exists():
        shutil.rmtree(dest)
    shutil.copytree(ASSETS / "shared", dest)
    shutil.copytree(ASSETS / "rime/build", dest / "build")


def exchange(proc, line: str) -> dict:
    proc.stdin.write(line + "\n")
    proc.stdin.flush()
    while True:
        raw = proc.stdout.readline()
        if not raw:
            raise AssertionError(f"server closed; no response for {line!r}")
        if raw.strip().startswith("{"):
            return json.loads(raw)
        # tolerate any stray non-protocol line on stdout


def main() -> None:
    if not DYLIB.exists():
        sys.exit("librime_jni.dylib missing — run ./harness/smoke.sh once first")

    classes = compile_engine()
    base = Path("/tmp/jrime-desktop-test")
    shared = base / "shared"
    user = base / "user"
    stage_shared(shared)
    shutil.rmtree(user, ignore_errors=True)
    user.mkdir(parents=True)

    proc = subprocess.Popen(
        [str(JAVA / "bin/java"), f"-Djava.library.path={DYLIB.parent}",
         "-cp", str(classes), "com.jiaoay.jrime.desktop.DesktopServer",
         "--shared", str(shared), "--user", str(user)],
        stdin=subprocess.PIPE, stdout=subprocess.PIPE,
        stderr=subprocess.DEVNULL, text=True, encoding="utf-8")

    try:
        ready = exchange(proc, "ready")
        assert ready["schema"] == "flypy_xhfast", f"ready: {ready}"

        r = None
        for ch in "nihc":
            r = exchange(proc, f"k {ch}")
            assert r["handled"], f"key {ch} unhandled: {r}"
        assert r["raw"] == "nihc", f"raw input: {r}"
        assert r["cands"] and r["cands"][0][0] == "你好", \
            f"expected 你好 first, got {[c[0] for c in r['cands'][:5]]}: {r}"

        r = exchange(proc, "sel 0")
        assert r["commit"] == "你好", f"commit: {r}"

        r = exchange(proc, "k n")
        assert r["raw"] == "n", f"fresh composition after commit: {r}"

        r = exchange(proc, "exit")
        proc.wait(timeout=30)
        print("DESKTOP SERVER TEST PASS")
    finally:
        if proc.poll() is None:
            proc.kill()


if __name__ == "__main__":
    main()
