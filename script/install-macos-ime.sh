#!/usr/bin/env bash
# Build JRimeIME.app and install it as a macOS input method for the current
# user. Re-run after code changes; the system picks up the new bundle when the
# app is relaunched.
set -euo pipefail
cd "$(dirname "$0")/.."

./macos/JRimeIME/build.sh

DEST="$HOME/Library/Input Methods"
mkdir -p "$DEST"
rm -rf "$DEST/JRimeIME.app"
cp -R macos/build/JRimeIME.app "$DEST/JRimeIME.app"
# Kill any old instance; the system loads the bundle when the input source is
# next activated. (Launching an LSUIElement input method with `open` is not a
# valid activation path, so the script only refreshes the bundle.)
pkill -f JRimeIME 2>/dev/null || true

echo "installed: $DEST/JRimeIME.app"
echo "next: System Settings > Keyboard > Input Sources > + > Chinese, Simplified > JRimeIME"
echo "then switch to it with the input menu (or Ctrl+Space)."
