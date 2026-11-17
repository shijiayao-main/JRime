#!/usr/bin/env python3
"""Harness H1 case runner (doc/harness-design.md): record / verify / diff traces.

Modes:
  record --golden DIR   replay every corpus case, write golden DIR/<id>.jsonl
  verify [--golden DIR] replay again, byte-compare each trace against golden
  diff --other DIR      byte-compare two trace directories case by case

Isolation: one engine per process; every case gets a fresh user dir, the shared
staging dir (assets/shared sources + assets/rime/build prebuilt artifacts) is
staged once per run and stays read-only for the engine.
"""
import argparse
import filecmp
import json
import shutil
import subprocess
import sys
from pathlib import Path

HARNESS = Path(__file__).resolve().parent
ROOT = HARNESS.parent
ASSETS = ROOT / "app/src/main/assets"
BUILD = HARNESS / "build"


def stage_shared(dest: Path) -> None:
    if dest.exists():
        shutil.rmtree(dest)
    shutil.copytree(ASSETS / "shared", dest)
    shutil.copytree(ASSETS / "rime/build", dest / "build")


def load_cases(corpus: Path):
    cases = []
    for line in corpus.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if line and not line.startswith("#"):
            cases.append(json.loads(line))
    return cases


def run_case(case, shared: Path, workdir: Path):
    user = workdir / "user"
    if user.exists():
        shutil.rmtree(user)
    user.mkdir(parents=True)
    trace = workdir / "trace.jsonl"
    cmd = [
        "java",
        f"-Djava.library.path={BUILD}",
        "-cp",
        str(BUILD / "classes"),
        "Replay",
        "--shared",
        str(shared),
        "--user",
        str(user),
        "--out",
        str(trace),
        "--schema",
        case.get("setup", {}).get("schema", "flypy_xhfast"),
        "--steps",
        ",".join(case["steps"]),
    ]
    r = subprocess.run(cmd, capture_output=True, text=True, timeout=180)
    if r.returncode != 0 or not trace.exists():
        print(f"[{case['id']}] REPLAY FAILED rc={r.returncode}")
        tail = (r.stdout + r.stderr)[-2500:]
        print(tail)
        return None
    return trace


def compare_dirs(dir_a: Path, dir_b: Path, cases) -> bool:
    all_ok = True
    for case in cases:
        a = dir_a / f"{case['id']}.jsonl"
        b = dir_b / f"{case['id']}.jsonl"
        if not a.exists() or not b.exists():
            print(f"[{case['id']}] DIFF MISSING ({a.exists()=}, {b.exists()=})")
            all_ok = False
        elif filecmp.cmp(a, b, shallow=False):
            print(f"[{case['id']}] DIFF PASS ({a.stat().st_size}B)")
        else:
            print(f"[{case['id']}] DIFF MISMATCH")
            all_ok = False
    return all_ok


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("mode", choices=["record", "verify", "diff"])
    ap.add_argument("--corpus", default=str(HARNESS / "corpora/baseline.jsonl"))
    ap.add_argument("--golden", default=str(HARNESS / "golden"))
    ap.add_argument("--other", help="diff mode: the second trace directory")
    args = ap.parse_args()

    cases = load_cases(Path(args.corpus))
    if args.mode == "diff":
        if not args.other:
            ap.error("diff mode needs --other DIR")
        all_ok = compare_dirs(Path(args.golden), Path(args.other), cases)
        print("diff: " + ("IDENTICAL" if all_ok else "MISMATCH"))
        sys.exit(0 if all_ok else 1)

    shared = BUILD / "staging-shared"
    print("staging shared dir ->", shared)
    stage_shared(shared)
    golden = Path(args.golden)
    golden.mkdir(parents=True, exist_ok=True)

    ok = fail = 0
    for case in cases:
        trace = run_case(case, shared, BUILD / "work" / case["id"])
        if trace is None:
            fail += 1
            continue
        dest = golden / f"{case['id']}.jsonl"
        if args.mode == "record":
            shutil.copyfile(trace, dest)
            print(f"[{case['id']}] recorded ({dest.stat().st_size}B)")
            ok += 1
        else:
            if dest.exists() and filecmp.cmp(trace, dest, shallow=False):
                print(f"[{case['id']}] VERIFY PASS ({dest.stat().st_size}B)")
                ok += 1
            else:
                print(f"[{case['id']}] VERIFY FAIL (diff against {dest})")
                fail += 1
    print(f"{args.mode}: ok={ok} fail={fail}")
    sys.exit(1 if fail else 0)


if __name__ == "__main__":
    main()
