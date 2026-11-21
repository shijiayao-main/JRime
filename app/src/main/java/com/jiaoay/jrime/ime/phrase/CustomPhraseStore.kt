/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.ime.phrase

import com.jiaoay.jrime.core.data.DataManager
import com.jiaoay.jrime.daemon.RimeDaemon
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File

/**
 * The 常用语 store: Rime's `custom_phrase` user dictionary, edited through the
 * app instead of a text file. Writes restart the engine session so the new
 * phrases are picked up by the `custom_phrase` translator (the stabledb is read
 * when the session builds its dictionary).
 */
object CustomPhraseStore {
    private const val FILE_NAME = "custom_phrase.txt"

    private val file: File get() = File(DataManager.userDataDir, FILE_NAME)

    /** The shipped list; librime's DB resolver falls back to the shared dir. */
    private val shippedFile: File get() = File(DataManager.sharedDataDir, FILE_NAME)

    private val _phrases = MutableStateFlow<List<CustomPhrase>>(emptyList())
    val phrases: StateFlow<List<CustomPhrase>> = _phrases.asStateFlow()

    private var header: List<String> = emptyList()
    private var scope: CoroutineScope? = null
    private var loaded = false

    fun bind(coroutineScope: CoroutineScope) {
        if (scope == null) scope = coroutineScope
        ensureLoaded()
    }

    fun ensureLoaded() {
        if (loaded) return
        loaded = true
        io { load() }
    }

    fun add(phrase: CustomPhrase) {
        if (_phrases.value.any { it.text == phrase.text && it.code == phrase.code }) return
        persist(_phrases.value + phrase)
    }

    fun update(
        old: CustomPhrase,
        new: CustomPhrase,
    ) {
        persist(_phrases.value.map { if (it == old) new else it })
    }

    fun delete(phrase: CustomPhrase) {
        persist(_phrases.value.filterNot { it == phrase })
    }

    private fun load() {
        runCatching {
            when {
                // The first edit needs the user-dir copy: the shipped file in
                // the shared dir is the read-only fallback the engine reads,
                // so seed it before the panel can edit the list.
                file.exists() -> file.readText()

                shippedFile.exists() ->
                    shippedFile.readText().also { raw ->
                        runCatching {
                            file.parentFile?.mkdirs()
                            file.writeText(raw)
                        }
                    }

                else -> ""
            }
        }
            .onSuccess { raw ->
                val parsed = CustomPhraseCodec.parse(raw)
                header = parsed.header
                _phrases.value = parsed.phrases
            }.onFailure { Timber.w(it, "custom_phrase read failed") }
    }

    private fun persist(phrases: List<CustomPhrase>) {
        _phrases.value = phrases
        io {
            runCatching {
                file.parentFile?.mkdirs()
                file.writeText(CustomPhraseCodec.render(CustomPhraseFile(header, phrases)))
                // The custom_phrase translator reads its stabledb at session
                // build time; a lightweight restart applies the edit.
                RimeDaemon.restartRime(fullCheck = false)
            }.onFailure { Timber.w(it, "custom_phrase write failed") }
        }
    }

    private fun io(block: () -> Unit) {
        val target = scope
        if (target == null) {
            block()
        } else {
            target.launch(Dispatchers.IO) { block() }
        }
    }
}
