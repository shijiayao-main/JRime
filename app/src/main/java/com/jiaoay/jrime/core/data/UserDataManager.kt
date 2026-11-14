/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.core.data

import com.jiaoay.jrime.TrimeApplication
import java.io.File

/**
 * Owns the user data dir and the code-generated default.custom.yaml fallback.
 * The EXTERNAL_SYNC tree mirror that used to live here was removed with the
 * setup wizard (drop-setup-wizard): app-specific storage is the only mode, and
 * user-dictionary import/export goes through the levers-based userdict UI.
 */
object UserDataManager {
    private const val DEFAULT_CUSTOM_FILE_NAME = "default.custom.yaml"

    /**
     * The fallback the pre-specialization app wrote into the user dir. Kept verbatim
     * to recognize (and clean up) stale copies from upgraded installs.
     */
    private const val LEGACY_SCHEMA_LIST_CUSTOM_PATCH = """
      patch:
        schema_list:
          - schema: luna_pinyin
          - schema: luna_pinyin_simp
    """

    private const val SCHEMA_LIST_CUSTOM_PATCH = """
      patch:
        schema_list:
          - schema: flypy_xhfast
    """

    private val externalFilesDir get() = TrimeApplication.getInstance().externalFilesDir

    val userDataDir = File(externalFilesDir, "rime")

    /**
     * Bootstraps a usable schema list into the user data dir, but only when neither
     * the user dir nor the shared dir already provides one: the preset copy under
     * shared/ must always win over a code-generated fallback, and a user-authored
     * default.custom.yaml wins over both.
     *
     * Upgrades: the pre-specialization app wrote a luna_pinyin-only fallback into the
     * user dir, which would forever shadow the preset flypy schema list. Files
     * byte-identical to that legacy template are removed so the preset takes effect;
     * anything the user edited themselves is left untouched.
     */
    fun ensureDefaultCustomConfig() {
        val custom = File(userDataDir, DEFAULT_CUSTOM_FILE_NAME)
        if (custom.exists() && custom.readText().trim() == LEGACY_SCHEMA_LIST_CUSTOM_PATCH.trim()) {
            custom.delete()
        }
        val preset = File(DataManager.sharedDataDir, DEFAULT_CUSTOM_FILE_NAME)
        if (!custom.exists() && !preset.exists() && custom.createNewFile()) {
            custom.writeText(SCHEMA_LIST_CUSTOM_PATCH.trimIndent())
        }
    }
}
