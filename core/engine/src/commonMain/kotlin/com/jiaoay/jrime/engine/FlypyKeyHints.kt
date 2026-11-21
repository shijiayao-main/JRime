/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.engine

/**
 * The 小鹤双拼 rhyme-key hints shown on the letter faces while idle: the
 * mapping is the one the shipped `flypy_xhfast` schema derives in its
 * `preedit_convert_rules` (the second key of a syllable is its final).
 *
 * The ambiguous finals carry their common reading; the hint is a learning
 * aid, not a parser.
 */
object FlypyKeyHints {
    private val hints: Map<Char, String> =
        mapOf(
            'q' to "iu",
            'w' to "ei",
            'r' to "uan",
            't' to "ue",
            'y' to "un",
            'o' to "uo",
            'p' to "ie",
            's' to "ong",
            'd' to "ai",
            'f' to "en",
            'g' to "eng",
            'h' to "ang",
            'j' to "an",
            'k' to "ing",
            'l' to "uang",
            'z' to "ou",
            'x' to "ia",
            'c' to "ao",
            'v' to "ui/ü",
            'b' to "in",
            'n' to "iao",
            'm' to "ian",
            // The pure vowel keys are their own finals; no hint needed.
            'a' to null,
            'e' to null,
            'i' to null,
            'u' to null,
        ).mapNotNull { (key, value) -> value?.let { key to it } }.toMap()

    /** The rhyme hint for a letter key, or null when the key carries none. */
    fun hintFor(letter: Char): String? = hints[letter.lowercaseChar()]
}
