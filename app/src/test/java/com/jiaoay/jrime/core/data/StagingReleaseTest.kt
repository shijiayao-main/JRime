// SPDX-FileCopyrightText: 2015 - 2026 Rime community
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.jiaoay.jrime.core.data

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import java.io.File
import java.security.MessageDigest
import kotlin.io.path.createTempDirectory

class StagingReleaseTest :
    StringSpec({

        fun sha256Of(bytes: ByteArray): String = MessageDigest
            .getInstance("SHA-256")
            .digest(bytes)
            .joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }

        fun writeFile(
            dir: File,
            name: String,
            content: String,
        ): File = File(dir, name).apply {
            parentFile?.mkdirs()
            writeText(content)
        }

        "sha256OrNull matches the one-shot digest and the manifest format" {
            val dir = createTempDirectory().toFile()
            try {
                val file = writeFile(dir, "table.bin", "payload")
                StagingRelease.sha256OrNull(file) shouldBe sha256Of("payload".toByteArray())
                // the build-time manifest (DataChecksumsPlugin) hashes with guava's
                // Hashing.sha256().toString(): lowercase hex, 64 chars
                val recorded = StagingRelease.sha256OrNull(file)!!
                recorded.length shouldBe 64
                recorded shouldBe recorded.lowercase()
            } finally {
                dir.deleteRecursively()
            }
        }

        "sha256OrNull streams files larger than the read buffer" {
            val dir = createTempDirectory().toFile()
            try {
                val big = ByteArray(512 * 1024) { (it % 251).toByte() }
                val file = File(dir, "big.table.bin").apply { writeBytes(big) }
                StagingRelease.sha256OrNull(file) shouldBe sha256Of(big)
            } finally {
                dir.deleteRecursively()
            }
        }

        "sha256OrNull is null for a missing file" {
            val dir = createTempDirectory().toFile()
            try {
                StagingRelease.sha256OrNull(File(dir, "missing.bin")) shouldBe null
            } finally {
                dir.deleteRecursively()
            }
        }

        "staleEntries keeps matching staging artifacts" {
            val dir = createTempDirectory().toFile()
            try {
                val file = writeFile(dir, "rime/build/flypy_xhfast.table.bin", "payload")
                val recorded =
                    mapOf(
                        "rime/build/flypy_xhfast.table.bin" to StagingRelease.sha256OrNull(file)!!,
                    )
                StagingRelease.staleEntries(recorded) { StagingRelease.sha256OrNull(File(dir, it)) } shouldContainExactly emptyList()
            } finally {
                dir.deleteRecursively()
            }
        }

        "staleEntries reports missing and corrupted staging artifacts" {
            val dir = createTempDirectory().toFile()
            try {
                val file = writeFile(dir, "rime/build/flypy_xhfast.table.bin", "payload")
                val recorded =
                    mapOf(
                        "rime/build/flypy_xhfast.table.bin" to StagingRelease.sha256OrNull(file)!!,
                        "rime/build/flypy_xhfast.prism.bin" to "hash-of-a-deleted-prism",
                    )
                file.writeText("corrupted")
                StagingRelease.staleEntries(recorded) { StagingRelease.sha256OrNull(File(dir, it)) }
                    .shouldContainExactlyInAnyOrder(
                        "rime/build/flypy_xhfast.table.bin",
                        "rime/build/flypy_xhfast.prism.bin",
                    )
            } finally {
                dir.deleteRecursively()
            }
        }

        "staleEntries ignores files outside the staging tree and directory markers" {
            val dir = createTempDirectory().toFile()
            try {
                val recorded =
                    mapOf(
                        "shared/default.yaml" to "missing-on-disk-but-not-staging",
                        "rime/build" to "", // directory marker
                    )
                StagingRelease.staleEntries(recorded) { StagingRelease.sha256OrNull(File(dir, it)) } shouldContainExactly emptyList()
            } finally {
                dir.deleteRecursively()
            }
        }

        "dropped staging entries are re-released by the next sync diff" {
            val manifest =
                DataChecksums(
                    sha256 = "manifest-hash",
                    files =
                    mapOf(
                        "shared/default.yaml" to "hash-default",
                        "rime/build/flypy_xhfast.table.bin" to "hash-table",
                        "rime/build" to "",
                    ),
                )
            val stale = listOf("rime/build/flypy_xhfast.table.bin")
            val record = DataChecksums.afterRelease(manifest, old = manifest, skippedFiles = stale)

            // the repair: revalidate drops the stale entry, the next sync
            // diffs it back as a fresh release from the APK
            DataDiff
                .diff(record, manifest)
                .shouldContainExactly(DataDiff.CreateFile("rime/build/flypy_xhfast.table.bin"))
        }
    })
