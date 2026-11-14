// SPDX-FileCopyrightText: 2015 - 2024 Rime community
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.jiaoay.jrime.core.data

import kotlinx.serialization.Serializable

@Serializable
data class DataChecksums(
    val sha256: String,
    val files: Map<String, String>,
) {
    companion object {
        /**
         * Builds the deployed record for a release pass so that entries which
         * could not be released are retried by the next sync:
         *
         * - a failed copy is dropped from the record, making it a Create again
         *   on the next diff;
         * - a failed deletion keeps the previous (stale) entry, so the path
         *   still diffs as a Delete;
         * - any failure clears the manifest hash, disabling the
         *   "nothing changed" fast path until a fully successful sync
         *   restores it.
         *
         * A pass without failures returns the intended record unchanged.
         */
        fun afterRelease(
            new: DataChecksums,
            old: DataChecksums,
            skippedFiles: Collection<String>,
        ): DataChecksums {
            if (skippedFiles.isEmpty()) return new
            val files = new.files.toMutableMap()
            for (path in skippedFiles) {
                val staleEntry = old.files[path]
                if (path in new.files) {
                    files.remove(path)
                } else if (staleEntry != null) {
                    files[path] = staleEntry
                }
            }
            return DataChecksums(sha256 = "", files = files)
        }
    }
}
