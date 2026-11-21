/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.engine

/** Modifier bits of a [KeyActionToken.Stroke], platform-neutral. */
@JvmInline
value class Modifiers(private val bits: Int) {
    val hasShift: Boolean get() = bits and SHIFT != 0
    val hasCtrl: Boolean get() = bits and CTRL != 0
    val hasAlt: Boolean get() = bits and ALT != 0
    val hasMeta: Boolean get() = bits and META != 0

    operator fun plus(other: Modifiers): Modifiers = Modifiers(bits or other.bits)

    fun isEmpty(): Boolean = bits == 0

    override fun toString(): String {
        val names = buildList {
            if (hasCtrl) add("Control")
            if (hasAlt) add("Alt")
            if (hasShift) add("Shift")
            if (hasMeta) add("Meta")
        }
        return if (names.isEmpty()) "None" else names.joinToString("+")
    }

    companion object {
        private const val SHIFT = 1
        private const val CTRL = 2
        private const val ALT = 4
        private const val META = 8

        val None = Modifiers(0)
        val Shift = Modifiers(SHIFT)
        val Ctrl = Modifiers(CTRL)
        val Alt = Modifiers(ALT)
        val Meta = Modifiers(META)
    }
}

/** Logical key names, aligned with the names used by the shipped layouts. */
object KeyNames {
    const val SPACE = "space"
    const val BACK_SPACE = "BackSpace"
    const val DELETE = "Delete"
    const val RETURN = "Return"
    const val SHIFT_LEFT = "Shift_L"
    const val ESCAPE = "Escape"
    const val TAB = "Tab"
    const val UP = "Up"
    const val DOWN = "Down"
    const val LEFT = "Left"
    const val RIGHT = "Right"
    const val HOME = "Home"
    const val END = "End"
    const val PAGE_UP = "Page_Up"
    const val PAGE_DOWN = "Page_Down"
    const val BACK = "BACK"
}

/** What a key (or one of its gesture slots) does. */
sealed interface KeyActionToken {
    /** Commit this literal text. */
    data class Text(val text: String) : KeyActionToken

    /** Send a logical key stroke; the platform maps [key] to its own key code. */
    data class Stroke(val key: String, val modifiers: Modifiers = Modifiers.None) : KeyActionToken

    /** Run an app-level command; the platform owns the command vocabulary. */
    data class Command(val name: String, val arg: String? = null) : KeyActionToken

    /** Switch the surface to another keyboard layout. */
    data class SwitchKeyboard(val id: KeyboardId) : KeyActionToken

    /** Open a registered panel. */
    data class OpenPanel(val id: PanelId) : KeyActionToken

    /** Set a boolean engine option (e.g. ascii_mode, ascii_punct). */
    data class SetOption(val option: String, val value: Boolean) : KeyActionToken

    /** Toggle a boolean engine option. */
    data class ToggleOption(val option: String) : KeyActionToken

    /** Move the field cursor by characters/lines. */
    data class MoveCursor(val chars: Int = 0, val lines: Int = 0) : KeyActionToken

    /** Native backspace/delete; the key spec decides repeatability. */
    data object Backspace : KeyActionToken

    /** Enter/newline. */
    data object Enter : KeyActionToken

    /** Hide the keyboard without moving focus. */
    data object Hide : KeyActionToken
}

/** The key's interaction state, used to pick a state-dependent action. */
enum class KeyState {
    Idle,
    Composing,
    Ascii,
    Aux,
    Shifted,
}

/** Second-line label: either text, an icon id, or none. */
data class KeyLabel(
    val text: String? = null,
    val icon: String? = null,
) {
    val isEmpty: Boolean get() = text.isNullOrEmpty() && icon.isNullOrEmpty()

    companion object {
        val None = KeyLabel()
        fun text(value: String) = KeyLabel(text = value)
    }
}

/** Visual class of a key; the theme maps styles to colors. */
enum class KeyStyle {
    Char,
    Symbol,
    Function,
    Accent,
    Spacer,
}

/** Swipe directions a key can carry an action for. */
enum class SwipeDir { Up, Down, Left, Right }

/**
 * One key of a layout.
 *
 * [states] carries state-dependent clicks (e.g. the comma key sends `/` while
 * composing); [action] is the idle click. [longPress] and [swipes] are the
 * other gesture slots. A key without an explicit [longPress] falls back to the
 * platform's default behaviour (uppercase letter, no-op) in M1 parity terms.
 */
data class KeySpec(
    val action: KeyActionToken,
    val label: KeyLabel = KeyLabel.None,
    val hint: KeyLabel = KeyLabel.None,
    val longPress: KeyActionToken? = null,
    val swipes: Map<SwipeDir, KeyActionToken> = emptyMap(),
    val states: Map<KeyState, KeyActionToken> = emptyMap(),
    val widthUnits: Float = 1f,
    val style: KeyStyle = KeyStyle.Char,
    val repeatable: Boolean = false,
    val slideCursor: Boolean = false,
    val sticky: Boolean = false,
) {
    val isSpacer: Boolean get() = style == KeyStyle.Spacer

    fun actionFor(state: KeyState): KeyActionToken = states[state] ?: action

    companion object {
        fun spacer(widthUnits: Float) = KeySpec(
            action = KeyActionToken.Text(""),
            style = KeyStyle.Spacer,
            widthUnits = widthUnits,
        )
    }
}

/** One row of a keyboard. */
data class RowSpec(
    val keys: List<KeySpec>,
    val heightUnits: Float = 1f,
)

/** A keyboard layout: rows of keys on a fixed unit grid. */
data class KeyboardLayout(
    val id: KeyboardId,
    val rows: List<RowSpec>,
    val displayName: String = "",
) {
    val keyCount: Int get() = rows.sumOf { it.keys.size }

    fun totalUnits(row: RowSpec): Float = row.keys.sumOf { it.widthUnits.toDouble() }.toFloat()
}

/** Resolves a layout by id. */
interface LayoutRegistry {
    fun find(id: KeyboardId): KeyboardLayout?

    fun all(): List<KeyboardLayout>
}

/** Registry backed by an immutable map; the app builds it from the layout data. */
class InMemoryLayoutRegistry(layouts: List<KeyboardLayout>) : LayoutRegistry {
    private val byId = layouts.associateBy { it.id }

    override fun find(id: KeyboardId): KeyboardLayout? = byId[id]

    override fun all(): List<KeyboardLayout> = byId.values.toList()
}
