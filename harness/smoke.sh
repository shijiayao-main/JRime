#!/bin/zsh
# Desktop-JVM smoke test for the librime_jni bridge (harness H0, doc/harness-design.md).
# Compiles the repo's own bridge sources as a macOS dylib against the prebuilt
# host librime.a and dep static libs, then loads it in a plain JVM and exercises
# load -> startup -> deploy callback -> schema list -> exit.
# Prereq: app/src/main/jni/host/build-host/ populated (see harness/README.md).
set -e
REPO=$(cd "$(dirname "$0")/.." && pwd)
JNI=$REPO/app/src/main/jni
SRC=$JNI/librime_jni
HOST=${HOST_BUILD:-$JNI/host/build-host}
JDK=${JDK:-$(/usr/libexec/java_home)}
OUT=$REPO/harness/build
mkdir -p "$OUT/classes" "$OUT/data"
cd "$OUT"

COMMON=(-std=c++17 -O1 -g -fPIC
  -DRIME_VERSION='"1.17.0"'
  -DRIME_EXTRA_MODULES='(lua)(octagram)(predict)'
  -DOpencc_BUILT_AS_STATIC
  -DSPDLOG_COMPILED_LIB
  -DYAML_CPP_STATIC_DEFINE
  -I"$JNI/librime/include"
  -I"$JNI/librime/src"
  -I"$HOST/librime-build/src"
  -I"$HOST/include"
  -I"$JNI/boost")
for d in "$JNI"/boost/libs/*/include(N); do COMMON+=(-I"$d"); done
COMMON+=(-I"$JDK/include" -I"$JDK/include/darwin")

echo "== building librime_jni.dylib from repo sources"
for f in rime_jni config frontend key levers opencc; do
  clang++ "${COMMON[@]}" -c "$SRC/$f.cc" -o "$f.o"
done
clang++ -dynamiclib -o librime_jni.dylib \
  rime_jni.o config.o frontend.o key.o levers.o opencc.o \
  -force_load "$HOST/librime-build/lib/librime.a" \
  "$HOST/opencc/src/libopencc.a" \
  "$HOST/leveldb/libleveldb.a" \
  "$HOST/marisa-trie/libmarisa.a" \
  "$HOST/yaml-cpp/libyaml-cpp.a" \
  "$HOST/snappy/libsnappy.a" \
  "$HOST/spdlog/libspdlog.a" \
  -lz \
  -Wl,-undefined,error

echo "== compiling Java stubs + Smoke"
javac -d classes "$REPO"/harness/java/com/jiaoay/jrime/core/*.java "$REPO"/harness/java/Smoke.java

echo "== running smoke"
java -Djava.library.path="$OUT" -cp classes Smoke "$OUT/data"
