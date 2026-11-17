/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.ime.compose

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jiaoay.jrime.data.theme.model.KeyActionToken
import com.jiaoay.jrime.data.theme.model.TextKeyboard
import com.jiaoay.jrime.ime.clipboard.ClipboardPanel
import com.jiaoay.jrime.ime.keyboard.InputFeedbackManager
import com.jiaoay.jrime.ime.keyboard.KeyAction
import com.jiaoay.jrime.ime.keyboard.KeyboardWindow
import com.jiaoay.jrime.ime.symbol.LiquidWindow
import com.jiaoay.jrime.ime.window.BoardWindow
import com.jiaoay.jrime.theme.NativeTheme
import kotlinx.coroutines.delay

/**
 * The Compose IME root: the permanent action bar (menu row idle, composing
 * code echo while typing -- one constant-height band) and the active panel --
 * the whole input-view area in one composition tree. On the Gboard-structured
 * keyboards the candidates render in the panel's digit-row slot while
 * composing; the full-data panels keep them in the action bar.
 *
 * Stage-1 notes (task 1.3): liquid symbol pages still read the legacy
 * theme's model; the clipboard and switches panels render a placeholder
 * until the interop pass.
 */
@Suppress("FunctionName")
@Composable
fun ImeRoot(
    state: ImeUiState,
    keyboardWindow: KeyboardWindow,
    onDispatch: (KeyAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    CompositionLocalProvider(LocalImeColors provides rememberImeColors(state)) {
        ImeRootContent(state, keyboardWindow, onDispatch, modifier)
    }
}

@Suppress("FunctionName")
@Composable
private fun ImeRootContent(
    state: ImeUiState,
    keyboardWindow: KeyboardWindow,
    onDispatch: (KeyAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val appearance = remember(state.composing, state.hasMenu, state.paging, state.auxActive, state.isAsciiMode) {
        KeyAppearance(state.composing, state.hasMenu, state.paging, state.auxActive, state.isAsciiMode)
    }
    // The panel's board, hoisted so the action bar knows where the candidates
    // render: on the Gboard-structured keyboard boards that lead with the
    // digit row they live in the digit-row slot while composing; every other
    // panel (boards without a leading digit row, the full-data boards, the
    // liquid pages, the clipboard) keeps them in the action bar. The two
    // checks share one invariant (hasLeadingDigitRow) with the keyboard's top
    // slot, so the candidates can never vanish between the two surfaces.
    val board = rememberKeyboard(keyboardWindow, state.currentWindow)
    val qwerty = remember(board) { isQwertyBoard(board, NativeTheme.theme.presetKeys) }
    val digitRowTop = remember(board) { hasLeadingDigitRow(board, NativeTheme.theme.presetKeys) }
    val candidatesInKeyboardRow =
        qwerty && digitRowTop && (state.currentWindow == null || state.currentWindow is KeyboardWindow)
    // The IME window is fullscreen and transparent (legacy contract); the
    // visible keyboard is a bottom-anchored band, everything above it stays
    // transparent so the app shows through. The band background paints
    // through the gesture zone (otherwise the taskbar behind shows through
    // -- it must be covered, not left transparent); the keys sit clear of
    // it with breathing room above the system's ime switch / dismiss
    // buttons (the Gboard form: buttons float on the keyboard background).
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(LocalImeColors.current.keyboardBackground)
                .navigationBarsPadding()
                .padding(bottom = BandButtonBreathing)
                .onGloballyPositioned { coordinates ->
                    state.updateBandTop(coordinates.positionInWindow().y.toInt())
                },
        ) {
            // The permanent action bar: idle it carries the menu row (settings,
            // punctuation width, number, clipboard, emoji, CJK/EN, collapse);
            // composing it shows the code echo, candidates only when the panel
            // doesn't host them itself. The height never moves -- the band
            // stays put when candidates arrive (the Gboard form).
            ActionBar(state, onDispatch, candidatesInKeyboardRow)
            // The Gboard structure carries its own intrinsic height (fixed
            // 52dp rows); the legacy keyboardHeight measurement reflected the
            // old YAML renderer and clips the new taller content.
            PanelHost(
                state = state,
                board = board,
                appearance = appearance,
                onDispatch = onDispatch,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** The action bar height; the strip's height is constant across both states. */
private val ActionBarHeight = 44.dp

/**
 * How long the back slot stays blind after a "back to keyboard" tap -- a
 * quick second tap in that window is a slip, not an emoji request.
 */
private const val BACK_SLOT_SUPPRESSION_MS = 300L

/** Air between the bottom key row and the system's ime buttons below. */
private val BandButtonBreathing = 12.dp

/**
 * The action bar: idle it is the menu row -- settings, punctuation width,
 * number, clipboard, emoji on the left, the CJK/EN toggle and the collapse
 * key on the right; composing it shows the code echo alone (candidates live
 * in the keyboard's digit-row slot, so the echo never crowds them) with only
 * the CJK/EN toggle kept on the right. On the panels that don't host the
 * candidate slot themselves ([candidatesInKeyboardRow] false) the strip
 * falls back into the bar's trailing space, the old form.
 */
@Suppress("FunctionName")
@Composable
private fun ActionBar(
    state: ImeUiState,
    onDispatch: (KeyAction) -> Unit,
    candidatesInKeyboardRow: Boolean,
) {
    val actions = rememberActionBarActions()
    val colors = LocalImeColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(ActionBarHeight)
            .background(colors.stripBackground)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!state.composing) {
            // Menu state: the common entrances the keyboard used to carry as
            // corner marks and the two band buttons duplicate (the user's
            // acceptance asks: entrances live here, keys stay clean).
            ActionBarButton("⚙", actions.settings, onDispatch)
            ActionBarButton(
                if (state.isAsciiPunct) ". ," else "，。",
                actions.punctSwitch,
                onDispatch,
            )
            // The number entrance (Gboard's "?123" form): the comma key's
            // hidden long-press/swipe-up board switch is gone, so the number
            // board lives one tap away on the menu row.
            ActionBarButton("123", actions.number, onDispatch)
            ActionBarButton("📋", actions.clipboard, onDispatch)
            // The same slot is the emoji entrance on the keyboard and the way
            // back from any non-keyboard panel (the liquid pages, the
            // clipboard); the keyboard itself never shows it. A quick second
            // tap after "back" hits the slot the instant it flips back to the
            // emoji entrance, reopening a panel the user just left (the
            // "keyboard flashes then hides" report) -- so the entrance stays
            // blind for a moment after a back tap.
            var emojiSuppressedUntil by remember { mutableStateOf(0L) }
            LaunchedEffect(emojiSuppressedUntil) {
                if (emojiSuppressedUntil > 0L) {
                    delay(BACK_SLOT_SUPPRESSION_MS)
                    emojiSuppressedUntil = 0L
                }
            }
            if (state.currentWindow != null && state.currentWindow !is KeyboardWindow) {
                ActionBarButton(
                    label = "⌨",
                    action = actions.backToKeyboard,
                    onDispatch = onDispatch,
                    onTapped = { emojiSuppressedUntil = SystemClock.uptimeMillis() + BACK_SLOT_SUPPRESSION_MS },
                )
            } else {
                ActionBarButton(
                    label = "☻",
                    action = actions.emoji,
                    onDispatch = onDispatch,
                    enabled = SystemClock.uptimeMillis() >= emojiSuppressedUntil,
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            AsciiSwitchButton(state, actions.asciiSwitch, onDispatch)
            ActionBarButton("▼", actions.hide, onDispatch)
        } else {
            // The composing code echo: some hosts (notably vendor ROMs) do
            // not render the field's composing underline, so the strip shows
            // the code itself -- the way the mainstream Chinese IMEs do.
            // The engine still gets "/" as the aux separator; the face shows
            // the middle dot, matching the inline copy.
            val echo = compositionEchoText(state.composition.preedit)
            if (echo.isNotEmpty()) {
                BasicText(
                    text = echo,
                    style = gboardStyle().preeditStyle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        // tap the echoed code to cancel the composition (the
                        // Esc affordance): start the syllable over without
                        // touching what the field already holds
                        .clickable { onDispatch(actions.escape) },
                )
                CandidateDivider()
            }
            // On the Gboard boards the candidates render in the digit-row
            // slot below; the echo owns this bar. The other panels keep the
            // strip right here (the pre-candidates-slot form).
            if (!candidatesInKeyboardRow) {
                CandidateStrip(state, Modifier.weight(1f))
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
            AsciiSwitchButton(state, actions.asciiSwitch, onDispatch)
        }
    }
}

/** The action bar's preset-backed actions, one per entrance. */
private class ActionBarActions(
    val settings: KeyAction,
    val punctSwitch: KeyAction,
    val number: KeyAction,
    val clipboard: KeyAction,
    val emoji: KeyAction,
    val backToKeyboard: KeyAction,
    val asciiSwitch: KeyAction,
    val hide: KeyAction,
    val escape: KeyAction,
)

@Composable
private fun rememberActionBarActions(): ActionBarActions = remember {
    val presetKeys = NativeTheme.theme.presetKeys
    fun action(token: String) = KeyAction(KeyActionToken.Plain(token), presetKeys)
    ActionBarActions(
        settings = action("Settings"),
        punctSwitch = action("punct_switch"),
        number = action("Keyboard_number"),
        clipboard = action("clipboard_window"),
        emoji = action("liquid_keyboard_emoji"),
        backToKeyboard = action("back_to_keyboard"),
        asciiSwitch = action("ascii_switch"),
        hide = action("Hide"),
        escape = action("Escape"),
    )
}

/** A flat gray menu chip -- the Gboard toolbar button form. */
@Suppress("FunctionName")
@Composable
private fun ActionBarButton(
    label: String,
    action: KeyAction,
    onDispatch: (KeyAction) -> Unit,
    enabled: Boolean = true,
    onTapped: (() -> Unit)? = null,
) {
    val colors = LocalImeColors.current
    Box(
        modifier = Modifier
            .padding(horizontal = 3.dp)
            .height(32.dp)
            .widthIn(min = 36.dp)
            .background(colors.functionKey, RoundedCornerShape(6.dp))
            .clickable(enabled = enabled) {
                // The dispatch path no longer carries the press feedback
                // (the keyboard keys fire it at finger-down), so the action
                // bar fires its own here.
                InputFeedbackManager.run {
                    keyPressSound(action.code)
                    keyPressSpeak(action.code)
                }
                onTapped?.invoke()
                onDispatch(action)
            },
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = label,
            style =
            TextStyle(
                fontSize = 14.sp,
                color = if (enabled) colors.keySymbol else colors.stripDivider,
            ),
            modifier = Modifier.padding(horizontal = 6.dp),
        )
    }
}

/**
 * The CJK/EN toggle: the one entrance that stays in both states (switching
 * mid-composition is the common case). EN lights up in the keyboard blue so
 * the input language reads at a glance.
 */
@Suppress("FunctionName")
@Composable
private fun AsciiSwitchButton(
    state: ImeUiState,
    action: KeyAction,
    onDispatch: (KeyAction) -> Unit,
) {
    val en = state.isAsciiMode
    val colors = LocalImeColors.current
    Box(
        modifier = Modifier
            .padding(horizontal = 3.dp)
            .height(32.dp)
            .widthIn(min = 36.dp)
            .background(
                if (en) colors.activeToggle else colors.functionKey,
                RoundedCornerShape(6.dp),
            )
            .clickable {
                // Same press-feedback contract as the menu chips.
                InputFeedbackManager.run {
                    keyPressSound(action.code)
                    keyPressSpeak(action.code)
                }
                onDispatch(action)
            },
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = if (en) "EN" else "中",
            style =
            TextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = if (en) colors.activeToggleLabel else colors.keyText,
            ),
            modifier = Modifier.padding(horizontal = 6.dp),
        )
    }
}

@Suppress("FunctionName")
@Composable
private fun PanelHost(
    state: ImeUiState,
    board: TextKeyboard,
    appearance: KeyAppearance,
    onDispatch: (KeyAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (val window = state.currentWindow) {
        is LiquidWindow -> LiquidPanel(
            liquidWindow = window,
            modifier = modifier,
        )

        is com.jiaoay.jrime.ime.clipboard.ClipboardWindow -> ClipboardPanel(
            window = window,
            modifier = modifier,
        )

        // The keyboard panel renders for null (pre-first-attach) too: the
        // service attaches the keyboard window on the first schema message,
        // which races the first frame.
        is KeyboardWindow, null -> {
            GboardKeyboard(
                board = board,
                keyAppearance = appearance,
                presetKeys = NativeTheme.theme.presetKeys,
                onDispatch = onDispatch,
                modifier = modifier,
                isAsciiMode = state.isAsciiMode,
                // While composing the digit-row slot shows the candidates;
                // the gray hint covers the empty-candidate moments so the
                // row never collapses (nor reads as "tap the digits").
                composingContent = { CandidateStrip(state, Modifier.fillMaxSize(), emptyHint = true) },
            )
        }

        else -> Box(modifier = modifier)
    }
}

@Composable
private fun rememberKeyboard(
    keyboardWindow: KeyboardWindow,
    window: BoardWindow?,
): TextKeyboard {
    val name by keyboardWindow.currentKeyboardName.collectAsState()
    val boardName = name.ifEmpty { "default" }
    return remember(boardName) {
        com.jiaoay.jrime.theme.NativeTheme.theme.presetKeyboards[name]
            ?: com.jiaoay.jrime.theme.NativeTheme.theme.presetKeyboards.getValue("default")
    }
}

/**
 * The liquid symbol pages: a horizontally scrolling tab strip above the key
 * grid of the active tab. Taps dispatch through [LiquidWindow.tap] (symbol
 * pages replay through the engine, plain pages commit); the whole panel
 * lives inside the keyboard band's bound, the grid scrolling below.
 */
@Suppress("FunctionName")
@Composable
private fun LiquidPanel(
    liquidWindow: LiquidWindow,
    modifier: Modifier = Modifier,
) {
    val colors = LocalImeColors.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            // Bounded to the keyboard's height: the wrap column must stay the
            // keyboard's size (an unbounded fillMaxSize stretches it to the
            // full window and pushes the action bar off the top of the band).
            .height((4 * GboardKeyboard.ROW_HEIGHT).dp)
            .background(colors.liquidBackground),
    ) {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            itemsIndexed(liquidWindow.tabs) { index, tag ->
                LiquidTabChip(
                    label = tag.label,
                    selected = index == liquidWindow.currentTabIndex,
                    colors = colors,
                ) { liquidWindow.setDataByIndex(index) }
            }
        }
        val symbols = liquidWindow.currentKeys
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 44.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp)
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(symbols) { item ->
                Box(
                    modifier = Modifier
                        .height(44.dp)
                        .background(colors.keyBackground, RoundedCornerShape(6.dp))
                        .clickable { liquidWindow.tap(item) },
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(text = item.text, style = TextStyle(fontSize = 20.sp, color = colors.keyText))
                }
            }
        }
    }
}

/** One page tab of the liquid strip; the active one lights up. */
@Suppress("FunctionName")
@Composable
private fun LiquidTabChip(
    label: String,
    selected: Boolean,
    colors: ImeColors,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .background(
                if (selected) colors.activeToggle else colors.functionKey,
                RoundedCornerShape(6.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        BasicText(
            text = label.trim(),
            style =
            TextStyle(
                fontSize = 12.sp,
                color = if (selected) colors.activeToggleLabel else colors.keyText,
            ),
        )
    }
}
