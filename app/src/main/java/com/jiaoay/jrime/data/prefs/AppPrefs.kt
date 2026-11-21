/*
 * SPDX-FileCopyrightText: 2015 - 2025 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.data.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.Keep
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.jiaoay.jrime.R
import com.jiaoay.jrime.ime.core.InlinePreeditMode
import com.jiaoay.jrime.util.InputMethodUtils
import com.jiaoay.jrime.util.appContext
import java.lang.ref.WeakReference

/**
 * Helper class for an organized access to the shared preferences.
 */
class AppPrefs(
    private val shared: SharedPreferences,
) {
    private val applicationContext: WeakReference<Context> = WeakReference(appContext)

    private val providers = mutableListOf<PreferenceDelegateProvider>()

    fun <T : PreferenceDelegateProvider> registerProvider(providerF: (SharedPreferences) -> T): T {
        val provider = providerF(shared)
        providers.add(provider)
        return provider
    }

    private fun <T : PreferenceDelegateProvider> T.register() = this.apply {
        registerProvider { this }
    }

    val internal = Internal(shared)
    val general = General(shared).register()
    val profile = Profile(shared).register()
    val keyboard = Keyboard(shared).register()
    val clipboard = Clipboard(shared).register()
    val advanced = Advanced(shared).register()
    val appearance = Appearance(shared).register()

    @Keep
    private val onSharedPreferenceChangeListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == null) return@OnSharedPreferenceChangeListener
            providers.forEach {
                it.notifyChange(key)
            }
        }

    companion object {
        private var defaultInstance: AppPrefs? = null

        fun initDefault(sharedPreferences: SharedPreferences): AppPrefs {
            val instance = AppPrefs(sharedPreferences)
            defaultInstance = instance
            sharedPreferences.registerOnSharedPreferenceChangeListener(
                defaultInstance().onSharedPreferenceChangeListener,
            )
            return instance
        }

        fun defaultInstance(): AppPrefs = defaultInstance
            ?: throw UninitializedPropertyAccessException(
                """
                    Default preferences not initialized! Make sure to call initDefault()
                    before accessing the default preferences.
                """.trimIndent(),
            )
    }

    class Internal(
        shared: SharedPreferences,
    ) : PreferenceDelegateOwner(shared) {
        companion object {
            const val PID = "general__pid"
        }

        val pid = int(PID, 0)
    }

    class General(
        shared: SharedPreferences,
    ) : PreferenceDelegateOwner(shared, R.string.general) {
        companion object {
            const val INLINE_PREEDIT_MODE = "inline_preedit_mode"
            const val ASCII_SWITCH_TIPS = "ascii_switch_tips"
            const val INLINE_SUGGESTIONS = "inline_suggestions"
            const val PREFERRED_VOICE_INPUT = "preferred_voice_input"
            const val ASCII_SWITCH_MODE = "ascii_switch_mode"
        }

        // The composing text rides inline in the app's field by default (the
        // Gboard feel); the candidate strip never renders it, so DISABLE
        // would leave the typed code visible nowhere.
        val inlinePreeditMode = enum(R.string.inline_preedit_mode, INLINE_PREEDIT_MODE, InlinePreeditMode.COMPOSING_TEXT)
        val asciiSwitchTips = switch(R.string.ascii_switch_tips, ASCII_SWITCH_TIPS, true)

        val asciiSwitchMode = enum(R.string.ascii_switch_mode, ASCII_SWITCH_MODE, AsciiSwitchMode.BOTH)

        val preferredVoiceInput = list(
            R.string.preferred_voice_input,
            PREFERRED_VOICE_INPUT,
            "",
            { InputMethodUtils.voiceInputMethods().map { it.first.packageName } },
            { ctx ->
                InputMethodUtils.voiceInputMethods().map { it.first.loadLabel(ctx.packageManager) }
            },
        )
    }

    /**
     *  Wrapper class of keyboard settings.
     */
    class Keyboard(
        shared: SharedPreferences,
    ) : PreferenceDelegateOwner(shared, R.string.virtual_keyboard) {
        companion object {
            const val KEYBOARD_HEIGHT = "keyboard_row_height"
            const val DOUBLE_SPACE_PERIOD = "double_space_period"
            const val SPACE_LONG_PRESS_VOICE = "space_long_press_voice"

            const val USE_SOFT_CURSOR = "use_soft_cursor"

            const val SOUND_ON_KEYPRESS = "sound_on_keypress"
            const val KEY_SOUND_VOLUME = "sound_volume"
            const val VIBRATE_ON_KEY_PRESS = "vibrate_on_key_press"
            const val VIBRATE_ON_KEY_RELEASE = "vibrate_on_key_release"
            const val VIBRATE_ON_KEY_REPEAT = "vibrate_on_key_repeat"
            const val VIBRATION_DURATION = "vibration_duration"
            const val VIBRATION_AMPLITUDE = "vibration_amplitude"
            const val SPEAK_ON_KEYPRESS = "speak_on_keypress"
            const val SPEAK_ON_COMMIT = "speak_on_commit"
            const val POPUP_ON_KEY_PRESS = "show_key_popup"
            const val SHOW_KEY_HINTS = "show_key_hints"
            const val SHOW_CANDIDATE_COMMENT = "show_candidate_comment"
            const val CANDIDATE_PAGE_SIZE = "candidate_page_size"
            const val CANDIDATE_TEXT_SIZE = "candidate_text_size"

            const val HOOK_CTRL_A = "hook_ctrl_a"
            const val HOOK_CTRL_CV = "hook_ctrl_cv"
            const val HOOK_CTRL_LR = "hook_ctrl_lr"
            const val HOOK_CTRL_ZY = "hook_ctrl_zy"
        }

        val rowHeight = int(
            R.string.keyboard_row_height,
            KEYBOARD_HEIGHT,
            50,
            36,
            80,
            "dp",
        )

        val doubleSpacePeriod = switch(R.string.double_space_period, DOUBLE_SPACE_PERIOD, true)
        val spaceLongPressVoice = switch(R.string.space_long_press_voice, SPACE_LONG_PRESS_VOICE, true)

        val useSoftCursor = switch(R.string.use_soft_cursor, USE_SOFT_CURSOR, true)

        val soundOnKeyPress = switch(R.string.sound_on_keypress, SOUND_ON_KEYPRESS, false)
        val soundVolume = int(
            R.string.sound_volume,
            KEY_SOUND_VOLUME,
            10,
            0,
            100,
            "%",
            defaultLabel = R.string.system_default,
        ) { soundOnKeyPress.getValue() }

        val vibrateOnKeyPress = switch(R.string.vibrate_on_key_press, VIBRATE_ON_KEY_PRESS, false)
        val vibrationDuration = int(
            R.string.vibration_duration,
            VIBRATION_DURATION,
            0,
            0,
            100,
            "ms",
            defaultLabel = R.string.system_default,
        ) { vibrateOnKeyPress.getValue() }

        val vibrationAmplitude = int(
            R.string.vibration_amplitude,
            VIBRATION_AMPLITUDE,
            0,
            0,
            255,
            defaultLabel = R.string.system_default,
        ) { vibrateOnKeyPress.getValue() }

        val speakOnKeyPress = switch(R.string.speak_on_keypress, SPEAK_ON_KEYPRESS, false)
        val speakOnCommit = switch(R.string.speak_on_commit, SPEAK_ON_COMMIT, false)
        val popupOnKeyPress = switch(R.string.popup_on_key_press, POPUP_ON_KEY_PRESS, true)

        // The displacement (dp) past which a touch counts as a swipe; 0
        // (shown as "Disable") turns swipe detection off entirely.

        // The long-press trigger delay; 0 (the default, shown as "System
        // default") follows the system long-press timeout, the accessibility
        // setting respect included. An explicit value overrides it.

        // The backspace-style repeat cadence; 80ms is the pace the gesture
        // code always ran at before this preference took effect.

        val hookCtrlA = switch(R.string.hook_ctrl_a, HOOK_CTRL_A, false)
        val hookCtrlCV = switch(R.string.hook_ctrl_cv, HOOK_CTRL_CV, false)
        val hookCtrlLR = switch(R.string.hook_ctrl_lr, HOOK_CTRL_LR, false)
        val hookCtrlZY = switch(R.string.hook_ctrl_zy, HOOK_CTRL_ZY, false)
    }

    /**
     *  Wrapper class of profile settings.
     */
    class Profile(
        shared: SharedPreferences,
    ) : PreferenceDelegateOwner(shared)

    class Clipboard(
        shared: SharedPreferences,
    ) : PreferenceDelegateOwner(shared, R.string.clipboard) {
        companion object {
            const val CLIPBOARD_LISTENING = "clipboard_listening"
            const val CLIPBOARD_LIMIT = "clipboard_clipboard_limit"
            const val CLIPBOARD_COMPARE_RULES = "clipboard_clipboard_compare"
            const val CLIPBOARD_OUTPUT_RULES = "clipboard_clipboard_output"
            const val CLIPBOARD_SUGGESTION = "clipboard_suggestion"
            const val CLIPBOARD_SUGGESTION_TIMEOUT = "clipboard_suggestion_timeout"
            const val CLIPBOARD_RETURN_AFTER_PASTE = "clipboard_return_after_paste"
            const val CLIPBOARD_MASK_SENSITIVE = "clipboard_mask_sensitive"
        }
        val clipboardListening = switch(R.string.clipboard_listening, CLIPBOARD_LISTENING, true)
        val clipboardLimit = int(
            R.string.clipboard_limit,
            CLIPBOARD_LIMIT,
            10,
        ) { clipboardListening.getValue() }
        val clipboardCompareRules = editText(
            R.string.clipboard_compare_rules,
            CLIPBOARD_COMPARE_RULES,
            "",
            R.string.a_regular_expression_per_line,
        ) { clipboardListening.getValue() }
        val clipboardOutputRules = editText(
            R.string.clipboard_output_rules,
            CLIPBOARD_OUTPUT_RULES,
            "",
            R.string.a_regular_expression_per_line,
        ) { clipboardListening.getValue() }
        val clipboardSuggestion = switch(
            R.string.clipboard_suggestion,
            CLIPBOARD_SUGGESTION,
            true,
        ) { clipboardListening.getValue() }
        val clipboardSuggestionTimeout = int(
            R.string.clipboard_suggestion_timeout,
            CLIPBOARD_SUGGESTION_TIMEOUT,
            20,
            0,
            100,
            "s",
        ) { clipboardListening.getValue() && clipboardSuggestion.getValue() }
        val clipboardReturnAfterPaste = switch(
            R.string.clipboard_return_after_paste,
            CLIPBOARD_RETURN_AFTER_PASTE,
            true,
        ) { clipboardListening.getValue() }
    }

    class Advanced(
        shared: SharedPreferences,
    ) : PreferenceDelegateOwner(shared, R.string.advanced) {
        companion object {
            const val IGNORE_SYSTEM_GESTURE_INSETS = "ignore_system_gesture_insets"
        }

        val ignoreSystemGestureInsets = switch(
            R.string.ignore_system_gesture_insets,
            IGNORE_SYSTEM_GESTURE_INSETS,
            false,
        )
    }

    enum class AsciiSwitchMode(override val stringRes: Int) : PreferenceDelegateEnum {
        BOTH(R.string.ascii_switch_both),
        BUTTON_ONLY(R.string.ascii_switch_button),
        SHIFT_ONLY(R.string.ascii_switch_shift),
    }

    enum class PaletteModePref(override val stringRes: Int) : PreferenceDelegateEnum {
        FOLLOW_SYSTEM(R.string.automatic),
        LIGHT(R.string.light),
        DARK(R.string.dark),
    }

    enum class AccentPref(override val stringRes: Int) : PreferenceDelegateEnum {
        BLUE(R.string.accent_blue),
        GREEN(R.string.accent_green),
        PURPLE(R.string.accent_purple),
    }

    /**
     * The appearance page: palette mode, accent preset, key hints and the two
     * candidate typography knobs. The first slice of the settings-v2 IA; the
     * Compose rewrite replaces the Preference machinery in phase 2.
     */
    class Appearance(
        shared: SharedPreferences,
    ) : PreferenceDelegateOwner(shared, R.string.appearance) {
        companion object {
            const val PALETTE_MODE = "theme_palette_mode"
            const val ACCENT = "theme_accent"
            const val SHOW_KEY_HINTS = "show_key_hints"
            const val SHOW_CANDIDATE_COMMENT = "show_candidate_comment"
            const val CANDIDATE_PAGE_SIZE = "candidate_page_size"
            const val CANDIDATE_TEXT_SIZE = "candidate_text_size"
        }

        val paletteMode = enum(R.string.palette_mode, PALETTE_MODE, PaletteModePref.FOLLOW_SYSTEM)
        val accent = enum(R.string.accent_color, ACCENT, AccentPref.BLUE)
        val showKeyHints = switch(R.string.show_key_hints, SHOW_KEY_HINTS, true)
        val showCandidateComment = switch(R.string.show_candidate_comment, SHOW_CANDIDATE_COMMENT, true)
        val candidatePageSize = int(
            R.string.candidate_page_size,
            CANDIDATE_PAGE_SIZE,
            7,
            5,
            9,
        )
        val candidateTextSize = int(
            R.string.candidate_text_size,
            CANDIDATE_TEXT_SIZE,
            18,
            15,
            22,
            "sp",
        )
    }

    /** The preference groups the settings hub renders, in page order. */
    fun settingProviders(): List<PreferenceDelegateOwner> = listOf(general, keyboard, appearance, clipboard, advanced)

    fun syncToDeviceEncryptedStorage() {
        val ctx = appContext.createDeviceProtectedStorageContext()
        val sp = PreferenceManager.getDefaultSharedPreferences(ctx)
        sp.edit {
            listOf(
                keyboard,
                clipboard,
                appearance,
            ).forEach { category ->
                category.preferenceDelegates.forEach {
                    it.value.putValueTo(this@edit)
                }
            }
        }
    }
}
