/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.ui.ime

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.ints.shouldBeGreaterThan
import java.io.File

/**
 * Structural guards for the shared surface (surface-foundation spec):
 * - no platform imports (`android.*`, no Android-only Compose helpers),
 * - no app-module imports (the surface must not reach back into `:app`),
 * - no raw size/colour literals (everything through `:ui:design` tokens).
 *
 * The scan is textual and therefore imperfect; the compiler and code review
 * remain the final gates, but this catches the common regressions early.
 */
class SurfaceGuardTest :
    StringSpec({
        val sourceRoot = resolveSourceRoot()
        val sources = sourceRoot.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()

        "the surface has Kotlin sources to scan" {
            sources.size shouldBeGreaterThan 0
        }

        "the surface does not import platform APIs" {
            val offenders =
                sources.mapNotNull { file ->
                    val hit =
                        file.readLines().withIndex().firstOrNull { (_, line) ->
                            line.startsWith("import android.") ||
                                line.contains("androidx.compose.ui.platform.LocalContext")
                        }
                    hit?.let { "${file.name}:${it.index + 1}: ${it.value.trim()}" }
                }
            offenders.shouldBeEmpty()
        }

        "the surface does not import the app module" {
            val offenders =
                sources.mapNotNull { file ->
                    val hit =
                        file.readLines().withIndex().firstOrNull { (_, line) ->
                            line.startsWith("import com.jiaoay.jrime.ime.") ||
                                line.startsWith("import com.jiaoay.jrime.ui.main.")
                        }
                    hit?.let { "${file.name}:${it.index + 1}: ${it.value.trim()}" }
                }
            offenders.shouldBeEmpty()
        }

        "the surface carries no raw dp/sp/color literals" {
            val literal = Regex("""\b\d+(\.\d+)?\.(dp|sp)\b|Color\(0x""")
            val offenders =
                sources.mapNotNull { file ->
                    val hit =
                        file.readLines().withIndex().firstOrNull { (_, line) ->
                            literal.containsMatchIn(line) && !line.trimStart().startsWith("//")
                        }
                    hit?.let { "${file.name}:${it.index + 1}: ${it.value.trim()}" }
                }
            offenders.shouldBeEmpty()
        }
    })

/** Unit tests run with the module directory as the working directory. */
private fun resolveSourceRoot(): File {
    val candidates =
        listOf(
            File("src/commonMain/kotlin"),
            File("ui/ime/src/commonMain/kotlin"),
            File("src/main/java"),
            File("ui/ime/src/main/java"),
        )
    return candidates.firstOrNull { dir ->
        dir.isDirectory && dir.walkTopDown().any { it.extension == "kt" }
    } ?: error("cannot locate ui/ime sources from ${File(".").absolutePath}")
}
