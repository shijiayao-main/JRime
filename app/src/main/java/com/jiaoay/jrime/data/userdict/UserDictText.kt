/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.data.userdict

/**
 * One exported user-dictionary row. Rime's userdb text format is
 * `code<TAB>phrase<TAB>c=<commits> d=<dee> t=<tick>`; the value part is plain
 * text, which makes browsing/adding/adjusting entries possible through the
 * engine's own import path (no LevelDB surgery).
 */
data class UserDictEntry(
    val code: String,
    val phrase: String,
    val value: String,
) {
    /** Lexicon frequency; a negative value marks a deleted entry. */
    val commits: Int
        get() = COMMITS.find(value)?.groupValues?.get(1)?.toIntOrNull() ?: 0

    val deleted: Boolean get() = commits < 0

    companion object {
        private val COMMITS = Regex("""c=(-?\d+)""")
    }
}

/** Parses/rewrites the exported user-dictionary text format. */
object UserDictText {
    private val COMMITS = Regex("""c=-?\d+""")

    fun parse(raw: String): List<UserDictEntry> = raw.lineSequence()
        .map { it.trimEnd('\r') }
        .filter { it.isNotBlank() && !it.startsWith("#") }
        .mapNotNull { line ->
            val fields = line.split('\t')
            if (fields.size < 2) return@mapNotNull null
            val code = fields[0].trim()
            val phrase = fields[1]
            if (code.isEmpty() || phrase.isEmpty()) return@mapNotNull null
            UserDictEntry(
                code = code,
                phrase = phrase,
                value = fields.getOrNull(2)?.takeIf { it.isNotBlank() } ?: "c=0 d=0 t=0",
            )
        }.toList()

    fun render(entries: List<UserDictEntry>): String = buildString {
        entries.forEach { entry ->
            append(entry.code)
            append('\t')
            append(entry.phrase)
            append('\t')
            append(entry.value)
            append('\n')
        }
    }

    fun withCommits(
        entry: UserDictEntry,
        commits: Int,
    ): UserDictEntry {
        val value =
            if (COMMITS.containsMatchIn(entry.value)) {
                entry.value.replace(COMMITS, "c=$commits")
            } else {
                "c=$commits d=0 t=0"
            }
        return entry.copy(value = value)
    }

    fun bump(entry: UserDictEntry): UserDictEntry = withCommits(entry, entry.commits.coerceAtLeast(0) + 1)

    fun markDeleted(entry: UserDictEntry): UserDictEntry = withCommits(entry, -entry.commits.coerceAtLeast(1))

    /** Adds a phrase (or bumps an existing identical code+phrase). */
    fun add(
        entries: List<UserDictEntry>,
        code: String,
        phrase: String,
    ): List<UserDictEntry> {
        val index = entries.indexOfFirst { it.code == code && it.phrase == phrase }
        if (index >= 0) {
            val updated = entries.toMutableList()
            updated[index] = withCommits(entries[index], entries[index].commits.coerceAtLeast(0) + 1)
            return updated
        }
        return entries + UserDictEntry(code = code, phrase = phrase, value = "c=1 d=0 t=0")
    }
}
