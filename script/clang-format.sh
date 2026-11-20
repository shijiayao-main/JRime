#!/usr/bin/env bash

# SPDX-FileCopyrightText: 2015 - 2024 Rime community
#
# SPDX-License-Identifier: GPL-3.0-or-later

# clang format options
method="-i"

# echo clang-format version for developer
clang-format --version

while getopts "in" option; do
	case "${option}" in
	i) # format code
		method="-i"
		;;
	n) # dry run and changes formatting warnings to errors
		method="--dry-run --Werror"
		;;
	\?) # invalid option
		echo "invalid option, please use -i or -n."
		exit 1
		;;
	esac
done

# array of folders to format
native_path=(
	"app/src/main/jni/librime_jni/*.h"
	"app/src/main/jni/librime_jni/*.cc"
)

# iterate over all files in current entry: scan every glob before failing,
# so one violation never blinds the report to the rest of the surface
fail=0
for entry in "${native_path[@]}"; do
	clang-format --verbose ${method} -style='file' ${entry}
	if [ "$?" -ne 0 ]; then
		fail=1
	fi
done
if [ "$fail" -ne 0 ]; then
	echo "please format the code: make style-apply"
	exit 1
fi
