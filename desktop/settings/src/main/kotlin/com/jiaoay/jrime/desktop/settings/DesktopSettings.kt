package com.jiaoay.jrime.desktop.settings

import java.io.File
import java.util.Properties

/**
 * The desktop settings store: one human-readable properties file shared by the
 * settings window and the candidate-window app. (The Android app keeps its own
 * AppPrefs; the *settings UI* and spec contract are shared.)
 */
object DesktopSettings {
    const val PALETTE_MODE = "palette_mode"
    const val ACCENT = "accent"
    const val CANDIDATE_TEXT_SIZE = "candidate_text_size"
    const val CANDIDATE_PAGE_SIZE = "candidate_page_size"
    const val SHOW_KEY_HINTS = "show_key_hints"
    const val SHOW_CANDIDATE_COMMENT = "show_candidate_comment"

    val defaults: Map<String, String> =
        mapOf(
            PALETTE_MODE to "follow_system",
            ACCENT to "blue",
            CANDIDATE_TEXT_SIZE to "18",
            CANDIDATE_PAGE_SIZE to "7",
            SHOW_KEY_HINTS to "true",
            SHOW_CANDIDATE_COMMENT to "true",
        )

    val file: File
        get() {
            val override = System.getenv("JRIME_SETTINGS_FILE")
            if (!override.isNullOrEmpty()) return File(override)
            val home = System.getProperty("user.home")
            return File(home, ".config/jrime/settings.properties")
        }

    fun read(): Map<String, String> {
        val properties = Properties()
        val target = file
        if (target.isFile) {
            runCatching { target.inputStream().use { properties.load(it) } }
        }
        return defaults.keys.associateWith { key -> properties.getProperty(key) ?: defaults.getValue(key) }
    }

    fun write(values: Map<String, String>) {
        val properties = Properties()
        defaults.keys.forEach { key -> properties.setProperty(key, values[key] ?: defaults.getValue(key)) }
        file.parentFile?.mkdirs()
        file.outputStream().use { properties.store(it, "JRime desktop settings") }
    }
}
