/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.ui.settings

import kotlinx.coroutines.flow.StateFlow

/** One selectable value of a [SettingControl.Choice]. */
data class ChoiceOption(
    val value: String,
    val label: String,
)

/** How a setting is edited. */
sealed interface SettingControl {
    data class Toggle(val defaultValue: Boolean) : SettingControl

    data class Slider(
        val defaultValue: Int,
        val min: Int,
        val max: Int,
        val step: Int = 1,
        val unit: String = "",
    ) : SettingControl

    data class Choice(
        val defaultValue: String,
        val options: List<ChoiceOption>,
    ) : SettingControl

    data class TextField(
        val defaultValue: String,
        val numeric: Boolean = false,
    ) : SettingControl

    /** A row that performs an app action instead of editing a value. */
    data class Action(val id: String) : SettingControl

    /** A boolean engine option (Rime switches), not a stored preference. */
    data class ToggleOption(val option: String) : SettingControl

    /** A one-of-N engine option set (Rime switch groups). */
    data class OptionChoice(
        val optionNames: List<String>,
        val optionLabels: List<String>,
    ) : SettingControl
}

/**
 * One row of the settings screen. The app authors the list (titles resolved
 * from resources, defaults from the preference declarations); the shared
 * screen only renders and routes edits through [SettingsStore].
 */
data class SettingSpec(
    val key: String,
    val title: String,
    val summary: String? = null,
    val groupId: String,
    val groupTitle: String,
    val groupOrder: Int,
    val order: Int,
    val control: SettingControl,
    /** Hidden rows still get a spec (the no-orphan contract) but are not drawn. */
    val hidden: Boolean = false,
)

/**
 * The seam between the settings UI and the app's preference storage. Raw
 * preference types only: enums travel as their serialized names, which is how
 * the existing delegate layer stores them.
 */
interface SettingsStore {
    /** Bumped on any preference change; the screen re-reads visible values. */
    val revision: StateFlow<Int>

    fun bool(
        key: String,
        default: Boolean,
    ): Boolean

    fun setBool(
        key: String,
        value: Boolean,
    )

    fun int(
        key: String,
        default: Int,
    ): Int

    fun setInt(
        key: String,
        value: Int,
    )

    fun string(
        key: String,
        default: String,
    ): String

    fun setString(
        key: String,
        value: String,
    )
}

/**
 * The runtime-option seam: Rime switches live in the engine session, not in
 * SharedPreferences, so option rows load/set them asynchronously. Absent
 * source (no session) renders the rows as unavailable.
 */
interface RuntimeOptionSource {
    suspend fun get(option: String): Boolean?

    suspend fun set(
        option: String,
        value: Boolean,
    )
}

/** A group of rows, already ordered by the app. */
data class SettingGroup(
    val id: String,
    val title: String,
    val order: Int,
    val specs: List<SettingSpec>,
)
