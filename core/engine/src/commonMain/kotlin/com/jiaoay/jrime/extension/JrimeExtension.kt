/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.extension

import com.jiaoay.jrime.action.ImeAction
import com.jiaoay.jrime.engine.KeyboardId
import com.jiaoay.jrime.engine.PanelId

/**
 * The model-first extension contract.
 *
 * JRime has no user-facing YAML/theme/keyboard configuration layer. To add a
 * feature, implement this interface in a module and register it at startup:
 * every contribution is a typed model (actions, keyboards, panels, input
 * rules), so the host can render/dispatch it without parsing strings.
 *
 * The interface is intentionally small and stable; the shipped app itself is
 * the first extension (see `JrimeCoreExtension`).
 */
interface JrimeExtension {
    /** Stable, unique identifier (lowercase, no spaces). */
    val id: String

    /** Extra key actions addressable by layout slots or toolbar buttons. */
    fun actions(): Map<String, ImeAction> = emptyMap()

    /** Additional keyboard layouts. */
    fun keyboards(): List<KeyboardExtension> = emptyList()

    /** Additional surface panels. The host decides the content renderer. */
    fun panels(): List<PanelDescriptor> = emptyList()

    /** Additional runtime input rules (Rime switches) exposed in settings. */
    fun inputRules(): List<InputRuleDescriptor> = emptyList()

    /** Extra toolbar entries, merged after the shipped actions. */
    fun toolbarActions(): List<ExtensionToolbarAction> = emptyList()
}

/** A toolbar button contributed by an extension (typed action, semantic icon name). */
data class ExtensionToolbarAction(
    val id: String,
    val label: String,
    val action: ImeAction,
    /** A semantic icon name from the design system (`ImeIcon`); null = text only. */
    val icon: String? = null,
)

/** A keyboard contribution in the typed model (geometry + typed slots). */
data class KeyboardExtension(
    val id: KeyboardId,
    val keys: List<KeySlot>,
    val columns: Int = 30,
)

/**
 * One key slot: geometry plus typed actions per gesture. Width is measured in
 * layout units (the board's [KeyboardExtension.columns] grid); 0 means one unit.
 * The host adapts slots to the surface renderer, no legacy token is written.
 */
data class KeySlot(
    val label: String = "",
    val width: Float = 0f,
    val click: ImeAction? = null,
    val longPress: ImeAction? = null,
    val swipeUp: ImeAction? = null,
    val swipeDown: ImeAction? = null,
    val swipeLeft: ImeAction? = null,
    val swipeRight: ImeAction? = null,
)

data class PanelDescriptor(
    val id: PanelId,
    val title: String,
    val icon: String,
)

enum class InputRuleKind {
    Toggle,
    MultiState,
}

data class InputRuleDescriptor(
    val option: String,
    val title: String,
    val kind: InputRuleKind = InputRuleKind.Toggle,
    val labels: List<String> = emptyList(),
)
