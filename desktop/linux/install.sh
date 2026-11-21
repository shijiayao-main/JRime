#!/usr/bin/env bash
# Installs the JRime IBus engine for the current user (files go to
# ~/.local/share/ibus-jrime; the component XML is registered with ibus).
set -euo pipefail
HERE=$(cd "$(dirname "$0")" && pwd)
DEST="${JRIME_INSTALL_DIR:-$HOME/.local/share/ibus-jrime}"
mkdir -p "$DEST" "$HOME/.config/ibus/component"

cp "$HERE/jrime_engine_client.py" "$HERE/jrime_ibus_engine.py" "$DEST/"
cp "$HERE/ibus-engine-jrime" "$DEST/"
chmod +x "$DEST/ibus-engine-jrime"

sed -e "s|<exec>ibus-engine-jrime</exec>|<exec>$DEST/ibus-engine-jrime</exec>|" \
  "$HERE/jrime.xml" > "$HOME/.config/ibus/component/jrime.xml"

echo "installed to $DEST"
echo "engine package: export JRIME_ENGINE_CLASSPATH=/path/to/engine-classes"
echo "shared data:    export JRIME_SHARED_DIR=\$HOME/.local/share/jrime/shared"
echo "reload ibus:    ibus-daemon -drx  (or restart your session)"
