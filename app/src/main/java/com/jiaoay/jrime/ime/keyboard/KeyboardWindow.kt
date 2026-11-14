// SPDX-FileCopyrightText: 2015 - 2026 Rime community
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.jiaoay.jrime.ime.keyboard

import android.text.InputType
import android.view.View
import android.view.inputmethod.EditorInfo
import com.jiaoay.jrime.core.RimeMessage
import com.jiaoay.jrime.core.SchemaItem
import com.jiaoay.jrime.daemon.RimeSession
import com.jiaoay.jrime.data.theme.Theme
import com.jiaoay.jrime.data.theme.model.TextKeyboard
import com.jiaoay.jrime.ime.broadcast.InputBroadcastReceiver
import com.jiaoay.jrime.ime.core.TrimeInputMethodService
import com.jiaoay.jrime.ime.keyboard.KeyboardPrefs.isLandscapeMode
import com.jiaoay.jrime.ime.window.BoardWindow
import com.jiaoay.jrime.ime.window.ResidentWindow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.kodein.di.DI
import org.kodein.di.instance
import timber.log.Timber

/**
 * The keyboard's data machine in the Compose era: which keyboard is active
 * (as reactive state the Compose panel renders from), the [Keyboard] objects
 * with their key data the action layer reads, and the editor/schema-driven
 * switching rules. Rendering is entirely Compose's; no view tree lives here.
 */
class KeyboardWindow(di: DI) :
    BoardWindow.NoBarBoardWindow(di),
    ResidentWindow,
    InputBroadcastReceiver {
    private val service: TrimeInputMethodService by instance()
    private val theme: Theme by instance()
    private val rime: RimeSession by instance()
    private val commonKeyboardActionListener: CommonKeyboardActionListener by instance()

    private val cursorCapsMode: Int
        get() =
            service.currentInputEditorInfo.run {
                if (inputType != InputType.TYPE_NULL) {
                    service.currentInputConnection?.getCursorCapsMode(inputType) ?: 0
                } else {
                    0
                }
            }

    companion object : ResidentWindow.Key {
        lateinit var currentKeyboard: Keyboard

        private const val MAX_CACHED_KEYBOARDS = 3
    }

    override val key: ResidentWindow.Key
        get() = KeyboardWindow

    private val presetKeyboardIds = theme.presetKeyboards.keys.toList()
    private var currentKeyboardId = ""
    private var lastKeyboardId = ""
    private var lastLockKeyboardId = ""
    private var tempAsciiMode: Boolean? = null

    /**
     * The id of the keyboard currently shown, as reactive state: the Compose
     * panel re-renders when a switch lands (plain fields leave it blind).
     */
    private val _currentKeyboardName = MutableStateFlow("")

    val currentKeyboardName: StateFlow<String> = _currentKeyboardName.asStateFlow()

    /**
     * Built [Keyboard]s carry the key data the action layer reads (labels,
     * shift/caps state, per-keyboard ascii rules). Keep a few cached so
     * switching between the common layouts stays instant.
     *
     * Access order so the least recently used entry is evicted first; the
     * current keyboard is always the most recently used one and is
     * additionally excluded from eviction.
     */
    private val cachedKeyboards = LinkedHashMap<String, Keyboard>(4, 0.75f, true)

    /** ASCII mode survives eviction, since a rebuilt keyboard would otherwise forget it. */
    private val lastAsciiModes = mutableMapOf<String, Boolean>()

    private val activeKeyboard: Keyboard? get() = cachedKeyboards[currentKeyboardId]

    override fun onCreateView(): View = View(context)

    private fun computeAllowedWidth(): Int {
        // The Compose panel spans the full window; keys lay out in its own
        // coordinate space, so a screen-width number serves the data model.
        return context.resources.displayMetrics.widthPixels
    }

    private fun selectKeyboardConfig(name: String): TextKeyboard? {
        val config = theme.presetKeyboards[name] ?: theme.presetKeyboards["default"]
        val importPreset = config?.importPreset
        if (!importPreset.isNullOrEmpty()) {
            return selectKeyboardConfig(importPreset)
        }
        return config
    }

    private fun attachKeyboard(target: String) {
        currentKeyboardId = target
        _currentKeyboardName.value = target
        lastKeyboardId = target

        val config = selectKeyboardConfig(target)
        val keyboard =
            cachedKeyboards[target]
                ?: Keyboard(context, theme, computeAllowedWidth(), config).also {
                    it.lastAsciiMode = lastAsciiModes[target] ?: it.asciiMode
                    cachedKeyboards[target] = it
                    trimKeyboardCache()
                }

        if (keyboard.isLock) lastLockKeyboardId = target
        dispatchCapsState(keyboard::setShifted)

        val currentMode = rime.run { statusCached }.isAsciiMode
        val targetMode = if (keyboard.resetAsciiMode) keyboard.asciiMode else keyboard.lastAsciiMode

        if (currentMode != targetMode) {
            service.postRimeJob {
                commitComposition()
                setRuntimeOption("ascii_mode", targetMode)
            }
        }

        currentKeyboard = keyboard
    }

    private fun smartMatchKeyboard(): String {
        // 主题的布局中包含方案id，直接采用
        val currentSchema = rime.run { statusCached }.schemaId
        if (presetKeyboardIds.contains(currentSchema)) {
            return currentSchema
        }
        val alphabet = rime.run { schemaCached }.alphabet
        val layout =
            when {
                alphabet.all { it.isLetter() } -> "qwerty"

                // 包含 26 个字母
                alphabet.all { it.isLetter() || ",./;".any(it::equals) } -> "qwerty_"

                // 包含 26 个字母和,./;
                alphabet.all { it.isLetterOrDigit() } -> "qwerty0"

                // 包含 26 个字母和数字键
                else -> "default"
            }
        return if (presetKeyboardIds.contains(layout)) layout else "default"
    }

    private fun evalKeyboard(id: String): String {
        val currentIdx = presetKeyboardIds.indexOfFirst { currentKeyboardId == it }
        val dot =
            when (id) {
                ".default" -> smartMatchKeyboard()

                ".prior" -> presetKeyboardIds.getOrNull(currentIdx - 1) ?: currentKeyboardId

                ".next" -> presetKeyboardIds.getOrNull(currentIdx + 1) ?: currentKeyboardId

                ".last" -> lastKeyboardId

                ".last_lock" -> lastLockKeyboardId

                ".ascii" -> {
                    var ascii = activeKeyboard?.asciiKeyboard
                    if (ascii.isNullOrEmpty()) {
                        ascii = lastLockKeyboardId
                    }
                    if (presetKeyboardIds.contains(ascii)) ascii else currentKeyboardId
                }

                else -> {
                    id.ifEmpty {
                        if (activeKeyboard?.isLock == true) currentKeyboardId else lastLockKeyboardId
                    }
                }
            }
        var final = dot.ifEmpty { smartMatchKeyboard() }

        // 切换到横屏布局
        if (service.isLandscapeMode()) {
            val landscape =
                theme.presetKeyboards[final]?.landscapeKeyboard ?: ""
            if (landscape.isNotEmpty() && presetKeyboardIds.contains(landscape)) final = landscape
        }
        return final
    }

    private fun trimKeyboardCache() {
        while (cachedKeyboards.size > MAX_CACHED_KEYBOARDS) {
            val eldest = cachedKeyboards.entries.firstOrNull { it.key != currentKeyboardId } ?: break
            lastAsciiModes[eldest.key] = eldest.value.lastAsciiMode
            cachedKeyboards.remove(eldest.key)
        }
    }

    /**
     * Drops every built keyboard but the current one, e.g. when the system is short on memory.
     */
    fun trimCache() {
        val current = cachedKeyboards[currentKeyboardId] ?: return
        if (cachedKeyboards.size <= 1) return
        cachedKeyboards.forEach { (id, keyboard) -> lastAsciiModes[id] = keyboard.lastAsciiMode }
        cachedKeyboards.clear()
        cachedKeyboards[currentKeyboardId] = current
    }

    fun switchKeyboard(to: String) {
        val target = evalKeyboard(to)
        if (target != currentKeyboardId) {
            attachKeyboard(target)
        }
        Timber.d("Switched to keyboard: $target")
    }

    override fun onStartInput(info: EditorInfo) {
        val targetKeyboard =
            when (info.imeOptions and EditorInfo.IME_FLAG_FORCE_ASCII) {
                EditorInfo.IME_FLAG_FORCE_ASCII -> ".ascii"

                else -> {
                    when (info.inputType and InputType.TYPE_MASK_CLASS) {
                        InputType.TYPE_CLASS_NUMBER,
                        InputType.TYPE_CLASS_PHONE,
                        InputType.TYPE_CLASS_DATETIME,
                        -> "number"

                        InputType.TYPE_CLASS_TEXT -> {
                            when (info.inputType and InputType.TYPE_MASK_VARIATION) {
                                InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
                                InputType.TYPE_TEXT_VARIATION_PASSWORD,
                                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
                                InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS,
                                InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD,
                                -> ".ascii"

                                else -> ""
                            }
                        }

                        else -> ""
                    }
                }
            }
        switchKeyboard(targetKeyboard)
        val isAsciiMode = rime.run { statusCached }.isAsciiMode
        if (targetKeyboard == ".ascii" || targetKeyboard == "number") {
            if (tempAsciiMode == null) {
                tempAsciiMode = isAsciiMode
            }
            if (!isAsciiMode) {
                service.postRimeJob { setRuntimeOption("ascii_mode", true) }
            }
        } else {
            tempAsciiMode?.let { saved ->
                if (isAsciiMode != saved) {
                    service.postRimeJob { setRuntimeOption("ascii_mode", saved) }
                }
                tempAsciiMode = null
            } ?: activeKeyboard?.let {
                if (theme.style.resetAsciiModeOnFocusChange) {
                    val targetMode = if (it.resetAsciiMode) it.asciiMode else it.lastAsciiMode
                    if (isAsciiMode != targetMode) {
                        service.postRimeJob { setRuntimeOption("ascii_mode", targetMode) }
                    }
                }
            }
        }
    }

    private fun dispatchCapsState(setShift: (Boolean, Boolean) -> Unit) {
        val status = rime.run { statusCached }
        // TODO: 启用自动首句大写后，点击方向键时，保持Shift锁定状态功能将无法生效
        if (theme.style.autoCaps == true && status.isAsciiMode) {
            setShift(false, cursorCapsMode != 0)
        }
    }

    override fun onSelectionUpdate(
        start: Int,
        end: Int,
    ) {
        dispatchCapsState { on, shifted -> activeKeyboard?.setShifted(on, shifted) }
    }

    override fun onRimeSchemaUpdated(schema: SchemaItem) {
        switchKeyboard(".default")
    }

    override fun onRimeOptionUpdated(value: RimeMessage.OptionMessage.Data) {
        val option = value.option
        when {
            option.startsWith("_keyboard_") -> {
                val target = option.removePrefix("_keyboard_")
                if (target.isNotEmpty()) {
                    switchKeyboard(target)
                }
            }

            option.startsWith("_key_") -> {
                val what = option.removePrefix("_key_")
                if (what.isNotEmpty() && value.value) {
                    commonKeyboardActionListener
                        .listener
                        .onAction(theme.resolveAction(what))
                }
            }

            option == "ascii_mode" && !value.value -> {
                // 回到中文时，释放 Shift 锁定
                activeKeyboard?.resetShift()
            }
        }
    }

    override fun onAttached() {}

    override fun onDetached() {}
}
