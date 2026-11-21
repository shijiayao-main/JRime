package com.jiaoay.jrime.desktop.settings

import com.jiaoay.jrime.ui.settings.ChoiceOption
import com.jiaoay.jrime.ui.settings.SettingControl
import com.jiaoay.jrime.ui.settings.SettingGroup
import com.jiaoay.jrime.ui.settings.SettingSpec

/**
 * The desktop settings rows. The Android app derives its rows from AppPrefs;
 * the desktop shell has no Android preference system, so it authors the same
 * [SettingSpec] contract directly (sharing the screen, editor and tokens).
 */
object DesktopSettingSpecs {
    private const val GROUP_ID = "appearance"
    private const val GROUP_TITLE = "Appearance"

    fun build(): List<SettingGroup> {
        var order = 0
        fun spec(
            key: String,
            title: String,
            control: SettingControl,
        ) = SettingSpec(
            key = key,
            title = title,
            groupId = GROUP_ID,
            groupTitle = GROUP_TITLE,
            groupOrder = 0,
            order = order++,
            control = control,
        )
        val specs =
            listOf(
                spec(
                    DesktopSettings.PALETTE_MODE,
                    "Light / dark mode",
                    SettingControl.Choice(
                        defaultValue = DesktopSettings.defaults.getValue(DesktopSettings.PALETTE_MODE),
                        options =
                        listOf(
                            ChoiceOption("follow_system", "Follow system"),
                            ChoiceOption("light", "Light"),
                            ChoiceOption("dark", "Dark"),
                        ),
                    ),
                ),
                spec(
                    DesktopSettings.ACCENT,
                    "Accent color",
                    SettingControl.Choice(
                        defaultValue = DesktopSettings.defaults.getValue(DesktopSettings.ACCENT),
                        options =
                        listOf(
                            ChoiceOption("blue", "Blue"),
                            ChoiceOption("green", "Green"),
                            ChoiceOption("purple", "Purple"),
                        ),
                    ),
                ),
                spec(
                    DesktopSettings.CANDIDATE_TEXT_SIZE,
                    "Candidate text size",
                    SettingControl.Slider(
                        defaultValue = 18,
                        min = 15,
                        max = 22,
                        unit = "sp",
                    ),
                ),
                spec(
                    DesktopSettings.CANDIDATE_PAGE_SIZE,
                    "Candidates per page",
                    SettingControl.Slider(
                        defaultValue = 7,
                        min = 5,
                        max = 9,
                    ),
                ),
                spec(
                    DesktopSettings.SHOW_KEY_HINTS,
                    "Show key hints",
                    SettingControl.Toggle(true),
                ),
                spec(
                    DesktopSettings.SHOW_CANDIDATE_COMMENT,
                    "Show candidate comments",
                    SettingControl.Toggle(true),
                ),
            )
        return listOf(
            SettingGroup(
                id = GROUP_ID,
                title = GROUP_TITLE,
                order = 0,
                specs = specs,
                summary = "Light/dark, accent color, candidate text size and hints",
            ),
        )
    }
}
