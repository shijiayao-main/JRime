# SPDX-FileCopyrightText: 2015 - 2026 Rime community
#
# SPDX-License-Identifier: GPL-3.0-or-later

# Host-side build of librime's deployment tool (rime_deployer), used at build
# time to precompile the shipped rime dictionaries and configs (startup-fastpath).
# Reuses the vendored deps and the Boost tree next to this directory.

cmake_minimum_required(VERSION 3.18.0)

project(jrime-host-tools)

set(JNI_ROOT "${CMAKE_CURRENT_SOURCE_DIR}/..")

# Apple ld rejects --build-id=none (GNU ld/lld only); it is only a
# reproducibility nicety for a host tool, so skip it on Darwin.
if(NOT CMAKE_HOST_SYSTEM_NAME MATCHES "Darwin")
  add_link_options("LINKER:--build-id=none")
endif()

set(CMAKE_MODULE_PATH "${JNI_ROOT}/cmake" ${CMAKE_MODULE_PATH})

option(BUILD_SHARED_LIBS "" OFF)
option(BUILD_TESTING "" OFF)
option(ENABLE_LOGGING "Enable logging with spdlog library" ON)

# Boost: reuse (or first-create) the tree and archive owned by the device build.
set(BOOST_VERSION 1.92.0)
if(NOT EXISTS "${JNI_ROOT}/boost-${BOOST_VERSION}.tar.xz")
  message(STATUS "Downloading Boost ${BOOST_VERSION} ......")
  file(
    DOWNLOAD
    "https://github.com/boostorg/boost/releases/download/boost-${BOOST_VERSION}/boost-${BOOST_VERSION}-cmake.tar.xz"
    "${JNI_ROOT}/boost-${BOOST_VERSION}.tar.xz"
    EXPECTED_HASH
      SHA256=9bed76128d4e46755dbe818487788c6fceb6f72b378f4daa49b7e1e600d9088d
    SHOW_PROGRESS)
  file(REMOVE_RECURSE "${JNI_ROOT}/boost")
endif()
if(NOT EXISTS "${JNI_ROOT}/boost")
  message(STATUS "Extracting Boost ${BOOST_VERSION} ......")
  file(ARCHIVE_EXTRACT INPUT "${JNI_ROOT}/boost-${BOOST_VERSION}.tar.xz" DESTINATION
       "${JNI_ROOT}")
  file(RENAME "${JNI_ROOT}/boost-${BOOST_VERSION}" "${JNI_ROOT}/boost")
endif()
set(BOOST_INCLUDE_LIBRARIES algorithm crc dll interprocess preprocessor range regex scope_exit signals2 utility uuid vmd)
add_subdirectory("${JNI_ROOT}/boost" "${CMAKE_BINARY_DIR}/boost" EXCLUDE_FROM_ALL)

# spdlog
if(ENABLE_LOGGING)
  set(SPDLOG_BUILD_SHARED OFF CACHE BOOL "" FORCE)
  set(SPDLOG_BUILD_PIC ON CACHE BOOL "" FORCE)
  set(SPDLOG_BUILD_EXAMPLE OFF CACHE BOOL "" FORCE)
  set(SPDLOG_BUILD_TESTS OFF CACHE BOOL "" FORCE)
  set(SPDLOG_INSTALL OFF CACHE BOOL "" FORCE)
  set(SPDLOG_FMT_EXTERNAL OFF CACHE BOOL "" FORCE)
  add_subdirectory("${JNI_ROOT}/librime/deps/spdlog" "${CMAKE_BINARY_DIR}/spdlog" EXCLUDE_FROM_ALL)
endif()

# yaml-cpp
option(YAML_CPP_BUILD_TOOLS "" OFF)
option(YAML_CPP_BUILD_TESTS "" OFF)
option(YAML_CPP_INSTALL "" OFF)
add_subdirectory("${JNI_ROOT}/librime/deps/yaml-cpp" "${CMAKE_BINARY_DIR}/yaml-cpp")

# snappy + leveldb
option(SNAPPY_BUILD_TESTS "" OFF)
option(SNAPPY_BUILD_BENCHMARKS "" OFF)
option(SNAPPY_INSTALL "" OFF)
add_subdirectory("${JNI_ROOT}/snappy" "${CMAKE_BINARY_DIR}/snappy")
set(HAVE_SNAPPY TRUE)
option(LEVELDB_BUILD_TESTS "" OFF)
option(LEVELDB_BUILD_BENCHMARKS "" OFF)
option(LEVELDB_INSTALL "" OFF)
add_subdirectory("${JNI_ROOT}/librime/deps/leveldb" "${CMAKE_BINARY_DIR}/leveldb")

# marisa + opencc
add_subdirectory("${JNI_ROOT}/librime/deps/marisa-trie" "${CMAKE_BINARY_DIR}/marisa-trie")
set(LIBMARISA marisa)
# NOTE: the option is deliberately declared after add_subdirectory, matching
# the device build: marisa-trie builds its target while the option is still
# undefined; find_package(Marisa) later only checks the target.
option(USE_SYSTEM_MARISA "" ON)
add_subdirectory("${JNI_ROOT}/librime/deps/opencc" "${CMAKE_BINARY_DIR}/opencc")
# OpenccWorkarounds GLOBs with paths relative to its includer, which only
# works from the jni root; replicate the copy here with absolute paths.
file(GLOB LIBOPENCC_HEADERS "${JNI_ROOT}/librime/deps/opencc/src/*.hpp")
file(COPY ${LIBOPENCC_HEADERS} DESTINATION "${CMAKE_BINARY_DIR}/include/opencc")

set(X11Keysym "${JNI_ROOT}/librime/include")
include_directories("${X11Keysym}")

# librime (plugins + tools on)
set(RIME_PLUGINS librime-lua librime-octagram librime-predict)
foreach(plugin ${RIME_PLUGINS})
  if(NOT EXISTS "${JNI_ROOT}/librime/plugins/${plugin}")
    file(CREATE_LINK "${JNI_ROOT}/${plugin}" "${JNI_ROOT}/librime/plugins/${plugin}" COPY_ON_ERROR SYMBOLIC)
  endif()
endforeach()
if(NOT EXISTS "${JNI_ROOT}/librime/plugins/librime-lua/thirdparty")
  file(CREATE_LINK "${JNI_ROOT}/librime-lua-deps" "${JNI_ROOT}/librime/plugins/librime-lua/thirdparty" COPY_ON_ERROR SYMBOLIC)
endif()

option(BUILD_TEST "" OFF)
option(BUILD_STATIC "" ON)
option(RIME_BUILD_TOOLS "Build rime command-line tools" ON)
option(RIME_HOST_TOOLS "Host tool build: skip the system-Boost REQUIRED path" ON)

# Boost headers: the device build gets per-library include dirs from
# BoostRoot.cmake's directory-scope includes; replicate that here BEFORE the
# librime subdirectory so the INCLUDE_DIRECTORIES property propagates into it.
file(GLOB BOOST_LIB_ENTRIES "${JNI_ROOT}/boost/libs/*")
foreach(_dir ${BOOST_LIB_ENTRIES})
  if(IS_DIRECTORY "${_dir}")
    include_directories("${_dir}/include")
  endif()
endforeach()

add_subdirectory("${JNI_ROOT}/librime" "${CMAKE_BINARY_DIR}/librime-build")

# boost::regex symbols come from the Boost::regex target built above (same as
# the device build, where the jni layer links it).
target_link_libraries(rime_deployer Boost::regex libopencc leveldb marisa yaml-cpp)

