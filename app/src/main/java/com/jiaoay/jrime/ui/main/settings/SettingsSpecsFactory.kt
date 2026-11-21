/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.ui.main.settings

import android.content.Context
import com.jiaoay.jrime.R
import com.jiaoay.jrime.data.prefs.AppPrefs
import com.jiaoay.jrime.data.prefs.PreferenceDelegateUi
import com.jiaoay.jrime.ui.settings.ChoiceOption
import com.jiaoay.jrime.ui.settings.SettingControl
import com.jiaoay.jrime.ui.settings.SettingGroup
import com.jiaoay.jrime.ui.settings.SettingSpec

/**
 * Derives the settings hub's rows from the existing preference declarations.
 *
 * The delegate layer already carries the UI metadata (title, default, range,
 * enum labels), so the hub is generated from the single source of truth
 * instead of duplicating a hand-written list. A new preference type that lands
 * without a mapping fails loudly at runtime (and compile-time review) rather
 * than silently disappearing from the settings.
 */
class SettingsSpecsFactory(
    private val context: Context,
) {
    fun build(): List<SettingGroup> {
        val groups = mutableListOf<SettingGroup>()
        AppPrefs
            .defaultInstance()
            .settingProviders()
            .forEachIndexed { groupOrder, provider ->
                if (provider.title == 0) return@forEachIndexed
                val groupTitle = context.getString(provider.title)
                val specs =
                    provider.preferenceDelegatesUi.mapIndexedNotNull { order, ui ->
                        toSpec(ui, groupOrder, order, groupTitle)
                    }
                if (specs.isNotEmpty()) {
                    groups += SettingGroup(
                        id = provider::class.simpleName ?: "group-$groupOrder",
                        title = groupTitle,
                        order = groupOrder,
                        specs = specs,
                    )
                }
            }

        val dataOrder = groups.size
        val dataTitle = context.getString(R.string.data_section)
        groups += SettingGroup(
            id = "data",
            title = dataTitle,
            order = dataOrder,
            specs =
            listOf(
                SettingSpec(
                    key = ACTION_IMPORT_FONT,
                    title = context.getString(R.string.import_font_title),
                    groupId = "data",
                    groupTitle = dataTitle,
                    groupOrder = dataOrder,
                    order = 0,
                    control = SettingControl.Action(ACTION_IMPORT_FONT),
                ),
            ),
        )
        return groups.sortedBy { it.order }
    }

    private fun toSpec(
        ui: PreferenceDelegateUi<*>,
        groupOrder: Int,
        order: Int,
        groupTitle: String,
    ): SettingSpec? {
        val title =
            runCatching { context.getString(titleOf(ui)) }.getOrNull()
                ?: return null
        fun spec(
            control: SettingControl,
            hidden: Boolean = false,
        ) = SettingSpec(
            key = ui.key,
            title = title,
            groupId = groupTitle,
            groupTitle = groupTitle,
            groupOrder = groupOrder,
            order = order,
            control = control,
            hidden = hidden,
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
                spec(
                    SettingControl.Slider(
                        defaultValue = ui.defaultValue,
                        min = ui.min,
                        max = ui.max,
                        unit = ui.unit,
                    ),
                )

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
                                label = context.getString(list.entryLabels[index]),
                            )
                        },
                    ),
                )
            }

            is PreferenceDelegateUi.UniversalStringList<*> ->
                spec(
                    SettingControl.Choice(
                        defaultValue = ui.defaultValue.toString(),
                        options =
                        ui.entryValues().zip(ui.entryLabels(context)).map { (value, label) ->
                            ChoiceOption(value = value, label = label.toString())
                        },
                    ),
                )

            is PreferenceDelegateUi.StringLike ->
                if (ui.key == AppPrefs.Keyboard.CUSTOM_SOUND_EFFECT) {
                    spec(SettingControl.Action(ACTION_PICK_SOUND))
                } else {
                    // A click-only row without a generic editor: keep it
                    // visible but inert rather than dropping it silently.
                    spec(SettingControl.TextField(""), hidden = true)
                }

            else -> spec(SettingControl.TextField(""), hidden = true)
        } ?: null
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

    companion object {
        const val ACTION_IMPORT_FONT = "import_font"
        const val ACTION_PICK_SOUND = "pick_sound"
    }
}
