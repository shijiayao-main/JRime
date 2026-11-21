#!/usr/bin/env bash
# Build the debug APK, install it on the connected device, and make JRime the
# active input method. The one-command install the release checklist uses.
set -euo pipefail
cd "$(dirname "$0")/.."

ABI=$(adb shell getprop ro.product.cpu.abi 2>/dev/null | tr -d '\r')
echo "device abi: ${ABI:-unknown}"
./gradlew :app:assembleDebug --console=plain

APK="app/build/outputs/apk/debug/com.jiaoay.jrime-${ABI}-debug.apk"
if [ ! -f "$APK" ]; then
  APK=$(ls app/build/outputs/apk/debug/*-"$ABI"-debug.apk 2>/dev/null | head -1 || true)
fi
if [ -z "${APK:-}" ] || [ ! -f "$APK" ]; then
  APK=$(ls app/build/outputs/apk/debug/*-universal-debug.apk 2>/dev/null | head -1 || true)
fi
if [ -z "${APK:-}" ] || [ ! -f "$APK" ]; then
  echo "no debug APK found for abi=$ABI" >&2
  ls app/build/outputs/apk/debug/ >&2
  exit 1
fi
echo "installing $APK"
adb install -r "$APK"

IME=com.jiaoay.jrime.debug/com.jiaoay.jrime.ime.core.TrimeInputMethodService
adb shell ime enable "$IME" >/dev/null 2>&1 || true
adb shell ime set "$IME" >/dev/null 2>&1 || true
echo "done: $IME active; relaunch the app and open 测试输入"
