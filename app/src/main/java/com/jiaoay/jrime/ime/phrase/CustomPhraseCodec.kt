/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.ime.phrase

/** One 常用语 entry of Rime's `custom_phrase` user dictionary. */
data class CustomPhrase(
    val text: String,
    val code: String,
    val weight: Int = 1,
)

/** A parsed `custom_phrase.txt`: the file's header comments plus the entries. */
data class CustomPhraseFile(
    val header: List<String>,
    val phrases: List<CustomPhrase>,
)

/**
 * Reads/writes Rime's `custom_phrase.txt` (tab-separated text / code /
 * weight). The header comments are preserved verbatim; malformed data lines are
 * dropped rather than corrupting the file.
 */
object CustomPhraseCodec {
    fun parse(raw: String): CustomPhraseFile {
        val header = mutableListOf<String>()
        val phrases = mutableListOf<CustomPhrase>()
        var inData = false
        raw.lineSequence().forEach { line ->
            if (line.isBlank()) {
                if (!inData) header += line
                return@forEach
            }
            if (line.startsWith("#")) {
                if (!inData) header += line
                return@forEach
            }
            inData = true
            val fields = line.split('\t')
            if (fields.size < 2) return@forEach
            val text = fields[0].trim()
            val code = fields[1].trim()
            if (text.isEmpty() || code.isEmpty()) return@forEach
            val weight = fields.getOrNull(2)?.trim()?.toIntOrNull() ?: 1
            phrases += CustomPhrase(text = text, code = code, weight = weight)
        }
        return CustomPhraseFile(header = header, phrases = phrases)
    }

    fun render(file: CustomPhraseFile): String = buildString {
        file.header.forEach {
            append(it)
            append('\n')
        }
        file.phrases.forEach { phrase ->
            append(phrase.text)
            append('\t')
            append(phrase.code)
            append('\t')
            append(phrase.weight)
            append('\n')
        }
    }
}
