/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.data.backup

import com.jiaoay.jrime.core.data.DataManager
import com.jiaoay.jrime.core.data.UserDataManager
import com.jiaoay.jrime.daemon.RimeDaemon
import com.jiaoay.jrime.data.userdict.UserDictManager
import timber.log.Timber
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Whole-user-data backup: every Rime user dictionary exported as text plus the
 * user-dir config the app owns (custom phrases, the minimal custom patch).
 *
 * The archive is a plain zip so it stays inspectable/repairable; restoring
 * feeds each dictionary back through the engine's own import path (no
 * LevelDB file surgery) and restarts the session.
 */
object UserDataBackup {
    data class Summary(
        val dictionaries: Int,
        val phrases: Boolean,
        val hasCustomPatch: Boolean,
    )

    private const val PHRASE_ENTRY = "custom_phrase.txt"
    private const val CUSTOM_PATCH_ENTRY = "default.custom.yaml"
    private const val USERDICT_PREFIX = "userdict/"

    fun exportTo(out: OutputStream): Result<Summary> = runCatching {
        var dictionaries = 0
        var phrases = false
        var customPatch = false
        ZipOutputStream(out).use { zip ->
            val phraseFile = File(DataManager.userDataDir, PHRASE_ENTRY)
            if (phraseFile.isFile) {
                zip.putNextEntry(ZipEntry(PHRASE_ENTRY))
                phraseFile.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
                phrases = true
            }
            val customFile = File(UserDataManager.userDataDir, CUSTOM_PATCH_ENTRY)
            if (customFile.isFile) {
                zip.putNextEntry(ZipEntry(CUSTOM_PATCH_ENTRY))
                customFile.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
                customPatch = true
            }
            UserDictManager.getUserDictList().forEach { dictName ->
                zip.putNextEntry(ZipEntry("$USERDICT_PREFIX$dictName.txt"))
                val result = UserDictManager.exportUserDict(zip, dictName, "$dictName.txt")
                zip.closeEntry()
                if (result.isSuccess) dictionaries++
            }
        }
        Summary(dictionaries = dictionaries, phrases = phrases, hasCustomPatch = customPatch)
    }.onFailure { Timber.e(it, "user data backup failed") }

    fun importFrom(ins: InputStream): Result<Summary> = runCatching {
        var dictionaries = 0
        var phrases = false
        var customPatch = false
        ZipInputStream(ins).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val name = entry.name
                when {
                    name == PHRASE_ENTRY -> {
                        val target = File(DataManager.userDataDir, PHRASE_ENTRY)
                        target.parentFile?.mkdirs()
                        target.outputStream().use { zip.copyTo(it) }
                        phrases = true
                    }

                    name == CUSTOM_PATCH_ENTRY -> {
                        val target = File(UserDataManager.userDataDir, CUSTOM_PATCH_ENTRY)
                        target.parentFile?.mkdirs()
                        target.outputStream().use { zip.copyTo(it) }
                        customPatch = true
                    }

                    name.startsWith(USERDICT_PREFIX) && name.endsWith(".txt") -> {
                        val dictName = name.removePrefix(USERDICT_PREFIX).removeSuffix(".txt")
                        if (UserDictManager.importUserDict(zip, dictName, "$dictName.txt").isSuccess) {
                            dictionaries++
                        }
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        if (dictionaries > 0 || phrases) {
            RimeDaemon.restartRime(fullCheck = false)
        }
        Summary(dictionaries = dictionaries, phrases = phrases, hasCustomPatch = customPatch)
    }.onFailure { Timber.e(it, "user data restore failed") }
}
