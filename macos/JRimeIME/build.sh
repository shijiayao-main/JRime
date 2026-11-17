#!/bin/zsh
# Builds JRimeIME.app: Swift IMK shell + jlink runtime + engine classes +
# librime_jni.dylib + the rime asset tree, ad-hoc signed.
#
# Prereqs (local machine, same as ./harness/smoke.sh): JDK (jlink) and
# app/src/main/jni/host/build-host/ populated for the dylib link.
set -e
REPO=$(cd "$(dirname "$0")/../.." && pwd)
SRC=$REPO/macos/JRimeIME
JAVA_HOME=${JAVA_HOME:-$(/usr/libexec/java_home)}
B=$REPO/macos/build
APP=$B/JRimeIME.app

echo "== [1/6] librime_jni.dylib (via harness build)"
"$REPO/harness/smoke.sh" >/dev/null 2>&1 || { echo "dylib build failed"; exit 1; }

rm -rf "$B"
mkdir -p "$APP/Contents/MacOS" "$APP/Contents/Resources/lib" \
         "$APP/Contents/Resources/engine" "$B/classes"

echo "== [2/6] engine classes"
KOTLIN_VERSION=2.4.21
KOTLINC="$REPO/harness/build/kotlinc-$KOTLIN_VERSION"
find "$REPO/desktop/engine/java" -name '*.kt' > "$B/sources.txt"
JAVA_HOME="$JAVA_HOME" "$KOTLINC/bin/kotlinc" -d "$B/classes" @"$B/sources.txt"
unzip -qo "$KOTLINC/lib/kotlin-stdlib.jar" -d "$B/classes"
cp -R "$B/classes/" "$APP/Contents/Resources/engine/"

echo "== [3/6] jlink runtime"
"$JAVA_HOME/bin/jlink" --add-modules java.base --output "$APP/Contents/Resources/runtime"

echo "== [4/6] rime assets (shared sources + prebuilt artifacts)"
mkdir -p "$APP/Contents/Resources/rime"
cp -R "$REPO/app/src/main/assets/shared/" "$APP/Contents/Resources/rime/"
cp -R "$REPO/app/src/main/assets/rime/build/" "$APP/Contents/Resources/rime/build/"
cp "$REPO/harness/build/librime_jni.dylib" "$APP/Contents/Resources/lib/"

echo "== [5/6] swift"
xcrun swiftc -swift-version 5 -O \
  -o "$APP/Contents/MacOS/JRimeIME" \
  "$SRC"/swift/*.swift

echo "== [6/6] bundle + ad-hoc sign"
cp "$SRC/Info.plist" "$APP/Contents/Info.plist"
codesign --force --deep --sign - "$APP" 2>/dev/null
codesign --verify "$APP" && echo "codesign OK"

echo "built: $APP"
