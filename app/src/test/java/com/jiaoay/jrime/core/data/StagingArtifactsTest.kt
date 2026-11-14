// SPDX-FileCopyrightText: 2015 - 2026 Rime community
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.jiaoay.jrime.core.data

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class StagingArtifactsTest :
    StringSpec({

        val fullSet =
            listOf(
                "default.yaml",
                "flypy_xhfast.schema.yaml",
                "flypy_xhfast.table.bin",
                "flypy_xhfast.prism.bin",
                "flypy_xhfast.reverse.bin",
            )

        "a released staging tree with the prebuilt set is ready" {
            StagingArtifacts.isReady(stagingDir = fullSet, prebuiltDir = emptyList()) shouldBe true
        }

        "an empty tree is not ready: the missing-prebuilt edge case" {
            StagingArtifacts.isReady(stagingDir = emptyList(), prebuiltDir = emptyList()) shouldBe false
        }

        "configs without dictionary binaries are not ready" {
            val names = fullSet.filterNot { it.endsWith(".bin") }
            StagingArtifacts.isReady(stagingDir = names, prebuiltDir = emptyList()) shouldBe false
        }

        "the compiled default config alone is not ready" {
            StagingArtifacts.isReady(stagingDir = listOf("default.yaml"), prebuiltDir = emptyList()) shouldBe false
        }

        "a tree without the compiled default config is not ready" {
            val names = fullSet.filterNot { it == "default.yaml" }
            StagingArtifacts.isReady(stagingDir = names, prebuiltDir = emptyList()) shouldBe false
        }

        "a tree without any schema config is not ready" {
            val names = fullSet.filterNot { it.endsWith(".schema.yaml") }
            StagingArtifacts.isReady(stagingDir = names, prebuiltDir = emptyList()) shouldBe false
        }

        "the prebuilt fallback dir satisfies the engine on its own" {
            StagingArtifacts.isReady(stagingDir = emptyList(), prebuiltDir = fullSet) shouldBe true
        }

        "a tree split across both dirs is ready: the engine falls back per file" {
            val configs = fullSet.filterNot { it.endsWith(".bin") }
            val binaries = fullSet.filter { it.endsWith(".bin") }
            StagingArtifacts.isReady(stagingDir = configs, prebuiltDir = binaries) shouldBe true
        }
    })
