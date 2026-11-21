/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.ui.main.settings

import com.jiaoay.jrime.R
import com.jiaoay.jrime.ui.settings.SettingControl
import com.jiaoay.jrime.ui.settings.SettingSpec

/**
 * The 输入规则 rows: engine `switches` rather than stored preferences. They
 * live at the top of the 输入 group; each row talks to the live Rime session
 * through [RimeRuntimeOptionSource].
 *
 * The option names are the ones the shipped schema declares (see
 * `flypy_xhfast.schema.yaml` switches + the custom patch's added switches).
 */
object InputRuleSpecs {
    fun build(
        labeler: (Int) -> String,
        groupTitle: String,
    ): List<SettingSpec> {
        var order = -1000
        fun spec(
            id: String,
            title: String,
            control: SettingControl,
        ) = SettingSpec(
            key = "option:$id",
            title = title,
            groupId = "input",
            groupTitle = groupTitle,
            groupOrder = 0,
            order = order++,
            control = control,
        )
        return listOf(
            spec(
                id = "simplification",
                title = labeler(R.string.input_rule_simplification),
                control =
                SettingControl.OptionChoice(
                    optionNames = listOf("zh_simp", "zh_trad", "zh_hk", "zh_tw"),
                    optionLabels =
                    listOf(
                        labeler(R.string.zh_simp),
                        labeler(R.string.zh_trad),
                        labeler(R.string.zh_hk),
                        labeler(R.string.zh_tw),
                    ),
                ),
            ),
            spec(
                id = "full_shape",
                title = labeler(R.string.input_rule_full_shape),
                control = SettingControl.ToggleOption("full_shape"),
            ),
            spec(
                id = "ascii_punct",
                title = labeler(R.string.input_rule_ascii_punct),
                control = SettingControl.ToggleOption("ascii_punct"),
            ),
            spec(
                id = "emoji",
                title = labeler(R.string.input_rule_emoji),
                control = SettingControl.ToggleOption("emoji"),
            ),
            spec(
                id = "prediction",
                title = labeler(R.string.input_rule_prediction),
                control = SettingControl.ToggleOption("prediction"),
            ),
            spec(
                id = "char_mode",
                title = labeler(R.string.input_rule_char_mode),
                control = SettingControl.ToggleOption("char_mode"),
            ),
            spec(
                id = "preedit_format",
                title = labeler(R.string.input_rule_preedit_format),
                control = SettingControl.ToggleOption("preedit_format"),
            ),
            spec(
                id = "charset_hint",
                title = labeler(R.string.input_rule_charset_hint),
                control = SettingControl.ToggleOption("charset_hint"),
            ),
            spec(
                id = "charset",
                title = labeler(R.string.input_rule_charset),
                control = SettingControl.ToggleOption("charset"),
            ),
            spec(
                id = "comment",
                title = labeler(R.string.input_rule_comment),
                control =
                SettingControl.OptionChoice(
                    optionNames = listOf("mask_hint", "tone_hint", "comment_off"),
                    optionLabels =
                    listOf(
                        labeler(R.string.comment_mask_hint),
                        labeler(R.string.comment_tone_hint),
                        labeler(R.string.comment_off),
                    ),
                ),
            ),
        )
    }
}
