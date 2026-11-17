#!/usr/bin/env bash
# Builds librime_jni.so (Linux) from the repo bridge sources against the host
# librime build produced by app/src/main/jni/host (CMake). macOS uses
# ./harness/smoke.sh instead; Windows uses build-bridge.ps1.
# Env: JAVA_HOME required. Usage: build-bridge.sh [outDir]
set -e
REPO=$(cd "$(dirname "$0")/../.." && pwd)
JNI=$REPO/app/src/main/jni
HOST=${HOST_BUILD:-$JNI/host/build-host}
JDK=${JAVA_HOME:?JAVA_HOME required}
OUT=${1:-$REPO/desktop/build}
mkdir -p "$OUT"
CXX=${CXX:-g++}

COMMON=(-std=c++17 -O2 -fPIC
  -DRIME_VERSION='"1.17.0"'
  -DRIME_EXTRA_MODULES='(lua)(octagram)(predict)'
  -DOpencc_BUILT_AS_STATIC
  -DSPDLOG_COMPILED_LIB
  -DYAML_CPP_STATIC_DEFINE
  -I"$JNI/librime/include"
  -I"$JNI/librime/src"
  -I"$HOST/librime-build/src"
  -I"$HOST/include")
for d in "$JNI"/boost/libs/*/include; do COMMON+=(-I"$d"); done
COMMON+=(-I"$JDK/include" -I"$JDK/include/linux")

cd "$OUT"
for f in rime_jni config frontend key levers opencc; do
  "$CXX" "${COMMON[@]}" -c "$JNI/librime_jni/$f.cc" -o "$f.o"
done

"$CXX" -shared -o librime_jni.so \
  rime_jni.o config.o frontend.o key.o levers.o opencc.o \
  -L"$HOST/librime-build/lib" \
  -Wl,--whole-archive -lrime -Wl,--no-whole-archive \
  "$HOST/opencc/src/libopencc.a" \
  "$HOST/leveldb/libleveldb.a" \
  "$HOST/marisa-trie/libmarisa.a" \
  "$HOST/yaml-cpp/libyaml-cpp.a" \
  "$HOST/snappy/libsnappy.a" \
  "$HOST/spdlog/libspdlog.a" \
  -lz -lpthread -ldl

echo "built: $OUT/librime_jni.so"
