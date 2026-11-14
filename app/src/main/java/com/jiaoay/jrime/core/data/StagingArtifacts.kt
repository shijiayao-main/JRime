// SPDX-FileCopyrightText: 2015 - 2026 Rime community
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.jiaoay.jrime.core.data

/**
 * Whether the released data tree carries the prebuilt artifacts the engine
 * loads without running a deployment: the compiled default config plus at
 * least one schema config and its dictionary binaries.
 *
 * Readiness mirrors DataManager.resolveDeployedResourcePath — per file, the
 * engine serves staging first and falls back to the prebuilt dir — so the
 * two name sets are judged as one union: a tree split across both dirs is
 * ready exactly when the engine can actually serve it.
 *
 * Startup no longer deploys, so this is the trigger for the repair banner:
 * when it is false, the engine still starts (no soft-lock) but cannot produce
 * candidates until the user runs a full deployment from the deploy menu.
 */
object StagingArtifacts {
    fun isReady(
        stagingDir: Collection<String>,
        prebuiltDir: Collection<String>,
    ): Boolean = isReady(stagingDir + prebuiltDir)

    private fun isReady(names: Collection<String>): Boolean = "default.yaml" in names &&
        names.any { it.endsWith(".schema.yaml") } &&
        names.any { it.endsWith(".table.bin") } &&
        names.any { it.endsWith(".prism.bin") }
}
