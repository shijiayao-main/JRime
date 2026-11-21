#!/usr/bin/env python3
"""A canned line-protocol engine for client tests (no librime needed)."""

import json
import sys


def main() -> None:
    for line in sys.stdin:
        line = line.strip()
        if not line:
            continue
        if line == "exit":
            return
        if line == "ready":
            print(json.dumps({"op": "ready", "handled": True, "proto": 1, "engine": "fake"}))
        elif line.startswith("k "):
            print(json.dumps({"op": line, "handled": True, "preedit": "ni", "composing": True,
                              "raw": "ni", "commit": None, "ascii": False,
                              "cands": [["你", ""], ["尼", ""]]}))
        elif line.startswith("sel "):
            # An event line first: candidate clicks are engine-initiated.
            print(json.dumps({"event": "update", "preedit": "", "composing": False, "commit": "你"}))
            print(json.dumps({"op": line, "handled": True, "preedit": "", "composing": False,
                              "raw": "", "commit": "你", "ascii": False, "cands": []}))
        elif line.startswith("rect "):
            print(json.dumps({"ok": True}))
        else:
            print(json.dumps({"op": line, "handled": True, "preedit": "", "composing": False,
                              "raw": "", "commit": None, "ascii": False, "cands": []}))
        sys.stdout.flush()


if __name__ == "__main__":
    main()
