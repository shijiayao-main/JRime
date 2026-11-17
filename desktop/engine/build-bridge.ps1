# Builds librime_jni.dll (Windows, MSVC toolchain via msvc-dev-cmd) from the
# repo bridge sources against the host librime build produced by
# app/src/main/jni/host (CMake, multi-config Release).
# Env: JAVA_HOME required. Usage: build-bridge.ps1 [outDir]
$ErrorActionPreference = "Stop"
$repo = (Resolve-Path "$PSScriptRoot/../..").Path
$jni = "$repo/app/src/main/jni"
$hostBuild = if ($env:HOST_BUILD) { $env:HOST_BUILD } else { "$jni/host/build-host" }
if (-not $env:JAVA_HOME) { throw "JAVA_HOME required" }
$jdk = $env:JAVA_HOME
$out = if ($args.Count -ge 1) { $args[0] } else { "$repo/desktop/build" }
New-Item -ItemType Directory -Force -Path $out | Out-Null
Push-Location $out
try {
  $defines = @(
    "/std:c++17", "/O2", "/EHsc", "/MD",
    "-DRIME_VERSION=`"1.17.0`"",
    "`"-DRIME_EXTRA_MODULES=(lua)(octagram)(predict)`"",
    "-DOpencc_BUILT_AS_STATIC", "-DSPDLOG_COMPILED_LIB", "-DYAML_CPP_STATIC_DEFINE",
    "-I$jni/librime/include", "-I$jni/librime/src",
    "-I$hostBuild/librime-build/src", "-I$hostBuild/include"
  )
  Get-ChildItem "$jni/boost/libs" -Directory | ForEach-Object {
    $defines += "-I$($_.FullName)/include"
  }
  $defines += "-I$jdk/include", "-I$jdk/include/win32"

  foreach ($f in "rime_jni", "config", "frontend", "key", "levers", "opencc") {
    & cl @defines /c "$jni/librime_jni/$f.cc" /Fo:"$f.obj"
    if ($LASTEXITCODE -ne 0) { throw "cl failed for $f.cc" }
  }

  # The host build is multi-config (MSVC): locate the static libs under Release.
  $wanted = "librime.lib", "libopencc.lib", "libleveldb.lib", "libmarisa.lib",
            "libyaml-cpp.lib", "libsnappy.lib", "libspdlog.lib"
  $libs = foreach ($name in $wanted) {
    $found = Get-ChildItem -Path $hostBuild -Recurse -Filter $name -File |
      Select-Object -First 1 -ExpandProperty FullName
    if (-not $found) { throw "static lib not found under ${hostBuild}: $name" }
    $found
  }

  $whole = $libs | ForEach-Object { "/WHOLEARCHIVE:$_" }
  & link /DLL /OUT:librime_jni.dll rime_jni.obj config.obj frontend.obj key.obj `
      levers.obj opencc.obj @whole `
      shlwapi.lib ws2_32.lib advapi32.lib user32.lib shell32.lib
  if ($LASTEXITCODE -ne 0) { throw "link failed" }

  Write-Host "built: $out/librime_jni.dll"
} finally {
  Pop-Location
}
