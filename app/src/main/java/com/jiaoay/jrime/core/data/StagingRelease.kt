// SPDX-FileCopyrightText: 2015 - 2026 Rime community
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.jiaoay.jrime.core.data

import java.io.File
import java.security.MessageDigest

/**
 * The repair-path half of the asset release chain.
 *
 * The dictionary YAML sources no longer ship in the APK (task 3.1), so an
 * on-device full recompile cannot rebuild the prebuilt table binaries —
 * re-releasing them from the APK is the only repair for a deleted or
 * corrupted staging tree. The release record would skip them ("already
 * deployed"), so the deploy menu re-verifies the recorded staging artifacts
 * against the disk and drops the entries that no longer match; the next
 * [DataManager.sync] then re-releases exactly those from the APK.
 */
object StagingRelease {
    /** Release-record key prefix of the prebuilt staging artifacts. */
    const val STAGING_DIR_KEY = "rime/build/"

    /**
     * Recorded staging entries whose released file is missing or no longer
     * matches its checksum. Blank-checksum entries are directory markers and
     * are skipped: re-released files recreate their parent directories.
     */
    fun staleEntries(
        recordedFiles: Map<String, String>,
        releasedSha256: (path: String) -> String?,
    ): List<String> = recordedFiles
        .filterKeys { it.startsWith(STAGING_DIR_KEY) }
        .filterValues { it.isNotBlank() }
        .filter { (path, sha256) -> releasedSha256(path) != sha256 }
        .keys
        .toList()

    /**
     * Streaming SHA-256 of a released file as lowercase hex, matching the
     * format the build-time checksums manifest uses; null when the file is
     * missing or unreadable.
     */
    fun sha256OrNull(file: File): String? = runCatching {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        digest
            .digest()
            .joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }
    }.getOrNull()
}
