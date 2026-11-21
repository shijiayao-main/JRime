/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.ui.main.settings

import android.content.Context
import com.jiaoay.jrime.R
import com.jiaoay.jrime.data.prefs.AppPrefs
import com.jiaoay.jrime.data.prefs.PreferenceDelegateOwner
import com.jiaoay.jrime.data.prefs.PreferenceDelegateUi
import com.jiaoay.jrime.ui.design.ImeIcon
import com.jiaoay.jrime.ui.settings.ChoiceOption
import com.jiaoay.jrime.ui.settings.SettingControl
import com.jiaoay.jrime.ui.settings.SettingGroup
import com.jiaoay.jrime.ui.settings.SettingSpec

/**
 * Derives the settings hub's rows from the existing preference declarations.
 *
 * The delegate layer already carries the UI metadata (title, default, range,
 * enum labels), so the hub is generated from the single source of truth
 * instead of duplicating a hand-written list: a new preference shows up here
 * automatically, and a preference type without a mapping is emitted as a
 * hidden row (kept for the no-orphan contract) instead of vanishing.
 *
 * The rows are re-grouped into the six product sections (input / keyboard /
 * candidates / appearance / clipboard / system+data) rather than the old
 * per-page provider buckets.
 */
class SettingsSpecsFactory(
    private val labeler: (Int) -> String,
    private val dynamicChoices: (PreferenceDelegateUi.UniversalStringList<*>) -> List<ChoiceOption> = { emptyList() },
    private val providers: List<PreferenceDelegateOwner>,
) {
    fun build(): List<SettingGroup> {
        val rows = LinkedHashMap<String, MutableList<SettingSpec>>()
        var order = 0
        providers.forEach { provider ->
            provider.preferenceDelegatesUi.forEach { ui ->
                val group = groupFor(ui.key)
                val spec = toSpec(ui, group, order)
                order += 1
                spec?.let { rows.getOrPut(group.id) { mutableListOf() } += it }
            }
        }
        val groups =
            GROUP_DEFS.mapNotNull { def ->
                rows[def.id]?.let { specs ->
                    SettingGroup(
                        id = def.id,
                        title = labeler(def.titleRes),
                        order = def.order,
                        specs = specs,
                        summary = labeler(def.summaryRes),
                        icon = def.icon,
                    )
                }
            }.toMutableList()
        fun actionSpec(
            action: String,
            titleRes: Int,
            order: Int,
        ) = SettingSpec(
            key = action,
            title = labeler(titleRes),
            groupId = GROUP_SYSTEM.id,
            groupTitle = labeler(GROUP_SYSTEM.titleRes),
            groupOrder = GROUP_SYSTEM.order,
            order = order,
            control = SettingControl.Action(action),
        )
        val dataGroup =
            SettingGroup(
                id = GROUP_SYSTEM.id,
                title = labeler(GROUP_SYSTEM.titleRes),
                order = GROUP_SYSTEM.order,
                summary = labeler(GROUP_SYSTEM.summaryRes),
                icon = GROUP_SYSTEM.icon,
                specs =
                listOf(
                    actionSpec(ACTION_IMPORT_FONT, R.string.import_font_title, 0),
                    actionSpec(ACTION_OPEN_USERDICT_BROWSER, R.string.user_dict_browser_title, 1),
                    actionSpec(ACTION_EXPORT_BACKUP, R.string.backup_export_title, 2),
                    actionSpec(ACTION_IMPORT_BACKUP, R.string.backup_import_title, 3),
                    actionSpec(ACTION_COPY_DIAGNOSTICS, R.string.copy_diagnostics_title, 4),
                ),
            )
        val systemIndex = groups.indexOfFirst { it.id == GROUP_SYSTEM.id }
        if (systemIndex >= 0) {
            val existing = groups[systemIndex]
            groups[systemIndex] = existing.copy(specs = existing.specs + dataGroup.specs)
        } else {
            groups += dataGroup
        }
        return groups.sortedBy { it.order }
    }

    private fun toSpec(
        ui: PreferenceDelegateUi<*>,
        group: GroupDef,
        order: Int,
    ): SettingSpec? {
        val title = runCatching { labeler(titleOf(ui)) }.getOrNull() ?: return null
        fun spec(
            control: SettingControl,
            hidden: Boolean = false,
        ) = SettingSpec(
            key = ui.key,
            title = title,
            groupId = group.id,
            groupTitle = labeler(group.titleRes),
            groupOrder = group.order,
            order = order,
            control = control,
            // The legacy-only knobs (landscape split, hidden toolbar/symbols,
            // expanded hit area) are still declared for the old Keyboard model
            // and are hidden here until that model retires; the no-orphan
            // contract keeps their specs in the list.
            hidden = hidden || ui.key in LEGACY_HIDDEN_KEYS,
        )
        return when (ui) {
            is PreferenceDelegateUi.Switch -> spec(SettingControl.Toggle(ui.defaultValue))

            is PreferenceDelegateUi.SeekBarInt ->
                spec(
                    SettingControl.Slider(
                        defaultValue = ui.defaultValue,
                        min = ui.min,
                        max = ui.max,
                        step = ui.step,
                        unit = ui.unit,
                    ),
                )

            is PreferenceDelegateUi.EditTextInt ->
                // Large/unbounded ranges are edited as text, never as a slider:
                // the see-saw between Int.MAX_VALUE bounds overflows Material3's
                // tick computation (NegativeArraySizeException).
                spec(SettingControl.TextField(ui.defaultValue.toString()))

            is PreferenceDelegateUi.EditText -> spec(SettingControl.TextField(ui.defaultValue))

            is PreferenceDelegateUi.StringList<*> -> {
                @Suppress("UNCHECKED_CAST")
                val list = ui as PreferenceDelegateUi.StringList<Any>
                spec(
                    SettingControl.Choice(
                        defaultValue = list.serializer.serialize(list.defaultValue),
                        options =
                        list.entryValues.mapIndexed { index, value ->
                            ChoiceOption(
                                value = list.serializer.serialize(value),
                                label = labeler(list.entryLabels[index]),
                            )
                        },
                    ),
                )
            }

            is PreferenceDelegateUi.UniversalStringList<*> ->
                spec(
                    SettingControl.Choice(
                        defaultValue = ui.defaultValue.toString(),
                        options = dynamicChoices(ui),
                    ),
                )

            is PreferenceDelegateUi.StringLike -> spec(SettingControl.TextField(""), hidden = true)

            else -> spec(SettingControl.TextField(""), hidden = true)
        }
    }

    private fun titleOf(ui: PreferenceDelegateUi<*>): Int = when (ui) {
        is PreferenceDelegateUi.Switch -> ui.title
        is PreferenceDelegateUi.SeekBarInt -> ui.title
        is PreferenceDelegateUi.EditTextInt -> ui.title
        is PreferenceDelegateUi.EditText -> ui.title
        is PreferenceDelegateUi.StringList<*> -> ui.title
        is PreferenceDelegateUi.UniversalStringList<*> -> ui.title
        is PreferenceDelegateUi.StringLike -> ui.title
        else -> 0
    }

    private fun groupFor(key: String): GroupDef = when {
        key in INPUT_KEYS -> GROUP_INPUT
        key in CANDIDATE_KEYS -> GROUP_CANDIDATES
        key in APPEARANCE_KEYS -> GROUP_APPEARANCE
        key.startsWith("clipboard") -> GROUP_CLIPBOARD
        key in SYSTEM_KEYS -> GROUP_SYSTEM
        else -> GROUP_KEYBOARD
    }

    private data class GroupDef(
        val id: String,
        val titleRes: Int,
        val order: Int,
        val summaryRes: Int,
        val icon: ImeIcon,
    )

    companion object {
        const val ACTION_IMPORT_FONT = "__action_import_font"
        const val ACTION_EXPORT_BACKUP = "__action_export_backup"
        const val ACTION_IMPORT_BACKUP = "__action_import_backup"
        const val ACTION_COPY_DIAGNOSTICS = "__action_copy_diagnostics"
        const val ACTION_OPEN_USERDICT_BROWSER = "__action_userdict_browser"

        private val GROUP_INPUT = GroupDef("input", R.string.group_input, 0, R.string.group_input_summary, ImeIcon.InputRules)
        private val GROUP_KEYBOARD = GroupDef("keyboard", R.string.group_keyboard, 1, R.string.group_keyboard_summary, ImeIcon.Keyboard)
        private val GROUP_CANDIDATES = GroupDef("candidates", R.string.group_candidates, 2, R.string.group_candidates_summary, ImeIcon.Candidates)
        private val GROUP_APPEARANCE = GroupDef("appearance", R.string.group_appearance, 3, R.string.group_appearance_summary, ImeIcon.Appearance)
        private val GROUP_CLIPBOARD = GroupDef("clipboard", R.string.group_clipboard, 4, R.string.group_clipboard_summary, ImeIcon.Clipboard)
        private val GROUP_SYSTEM = GroupDef("system", R.string.group_system, 5, R.string.group_system_summary, ImeIcon.Settings)

        private val GROUP_DEFS =
            listOf(GROUP_INPUT, GROUP_KEYBOARD, GROUP_CANDIDATES, GROUP_APPEARANCE, GROUP_CLIPBOARD, GROUP_SYSTEM)

        private val INPUT_KEYS =
            setOf(
                AppPrefs.General.INLINE_PREEDIT_MODE,
                AppPrefs.General.ASCII_SWITCH_TIPS,
                AppPrefs.General.PREFERRED_VOICE_INPUT,
                AppPrefs.General.ASCII_SWITCH_MODE,
                AppPrefs.Keyboard.DOUBLE_SPACE_PERIOD,
                AppPrefs.Keyboard.USE_SOFT_CURSOR,
                AppPrefs.Keyboard.SPEAK_ON_COMMIT,
                AppPrefs.Keyboard.HOOK_CTRL_A,
                AppPrefs.Keyboard.HOOK_CTRL_CV,
                AppPrefs.Keyboard.HOOK_CTRL_LR,
                AppPrefs.Keyboard.HOOK_CTRL_ZY,
                AppPrefs.Keyboard.HOOK_SHIFT_SPACE,
                AppPrefs.Keyboard.HOOK_SHIFT_NUM,
                AppPrefs.Keyboard.HOOK_SHIFT_SYMBOL,
            )

        private val CANDIDATE_KEYS =
            setOf(
                AppPrefs.Appearance.CANDIDATE_PAGE_SIZE,
                AppPrefs.Appearance.CANDIDATE_TEXT_SIZE,
            )

        private val APPEARANCE_KEYS =
            setOf(
                AppPrefs.Appearance.PALETTE_MODE,
                AppPrefs.Appearance.ACCENT,
                AppPrefs.Appearance.SHOW_KEY_HINTS,
                AppPrefs.Appearance.SHOW_CANDIDATE_COMMENT,
                AppPrefs.Advanced.SHOW_APP_ICON,
            )

        private val SYSTEM_KEYS = setOf(AppPrefs.Advanced.IGNORE_SYSTEM_GESTURE_INSETS)

        /** Declared but only consumed by the retiring legacy Keyboard model. */
        private val LEGACY_HIDDEN_KEYS =
            setOf(
                AppPrefs.Keyboard.LANDSCAPE_MODE,
                AppPrefs.Keyboard.SPLIT_SPACE_PERCENT,
                AppPrefs.Keyboard.HIDE_INPUT_BAR,
                AppPrefs.Keyboard.HIDE_KEY_SYMBOL,
                AppPrefs.Keyboard.HIDE_KEY_HINT,
                AppPrefs.Keyboard.EXPAND_KEYPRESS_AREA,
            )

        /** Convenience for the app: resolves labels from resources. */
        fun forContext(context: Context): SettingsSpecsFactory = SettingsSpecsFactory(
            labeler = context::getString,
            dynamicChoices = { ui -> ui.entryValues().map { ChoiceOption(it, it) } },
            providers = AppPrefs.defaultInstance().settingProviders(),
        )
    }
}
