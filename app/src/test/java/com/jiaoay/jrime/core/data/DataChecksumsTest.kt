// SPDX-FileCopyrightText: 2015 - 2026 Rime community
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.jiaoay.jrime.core.data

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

class DataChecksumsTest :
    StringSpec({

        val manifest =
            DataChecksums(
                sha256 = "manifest-hash",
                files =
                mapOf(
                    "shared/default.yaml" to "hash-default",
                    "rime/build/default.yaml" to "hash-compiled-default",
                    "rime/build/flypy_xhfast.table.bin" to "hash-table",
                    "rime/build" to "", // directory marker
                ),
            )

        "a pass without failures records the intended manifest unchanged" {
            val record = DataChecksums.afterRelease(manifest, old = DataChecksums("", emptyMap()), skippedFiles = emptyList())
            record shouldBe manifest
        }

        "a failed copy is dropped from the record so the next sync retries it" {
            val record =
                DataChecksums.afterRelease(
                    new = manifest,
                    old = DataChecksums("", emptyMap()),
                    skippedFiles = listOf("rime/build/flypy_xhfast.table.bin"),
                )

            record.files.containsKey("rime/build/flypy_xhfast.table.bin") shouldBe false
            // the failed entry must not disable retries for the released ones
            record.sha256 shouldBe ""
            DataDiff
                .diff(record, manifest)
                .shouldContainExactly(DataDiff.CreateFile("rime/build/flypy_xhfast.table.bin"))
        }

        "a failed deletion keeps the stale entry so the next sync retries it" {
            val new =
                DataChecksums(
                    sha256 = "manifest-hash-2",
                    files = mapOf("shared/default.yaml" to "hash-default"),
                )
            val record =
                DataChecksums.afterRelease(
                    new = new,
                    old = manifest,
                    skippedFiles = listOf("rime/build"),
                )

            // blank-checksum entries mark directories in the diff
            record.files["rime/build"] shouldBe ""
            DataDiff
                .diff(record, new)
                .shouldContainExactly(DataDiff.DeleteDir("rime/build"))
        }

        "any failure disables the nothing-changed fast path" {
            val record =
                DataChecksums.afterRelease(
                    new = manifest,
                    old = DataChecksums("", emptyMap()),
                    skippedFiles = listOf("rime/build/default.yaml"),
                )

            // a partial release must never compare equal to the manifest:
            // the next sync re-diffs and retries the skipped entry
            record.sha256 shouldBe ""
            DataDiff
                .diff(record, manifest)
                .shouldContainExactly(DataDiff.CreateFile("rime/build/default.yaml"))
        }
    })
