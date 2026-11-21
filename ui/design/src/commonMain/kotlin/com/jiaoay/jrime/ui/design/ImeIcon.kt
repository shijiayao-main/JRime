/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.ui.design

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SentimentSatisfied
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SettingsBackupRestore
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * JRime's semantic icon set.
 *
 * Names are product semantics, never scheme/marketing names: an icon says what
 * the control does, so a future input scheme (an AI-tuned descendant of the
 * shipped flypy default, or any other Rime schema) never forces an icon rename.
 * Vectors are outlined Material Symbols (Apache-2.0, attributed in the license
 * page); the mapping lives here so a custom brand set can replace it later
 * without touching the surfaces.
 */
enum class ImeIcon(
    val vector: ImageVector,
    /** Stable identifier used by models/extensions/telemetry. */
    val key: String,
) {
    Settings(Icons.Outlined.Settings, "settings"),
    InputRules(Icons.Outlined.Tune, "input_rules"),
    Keyboard(Icons.Outlined.Keyboard, "keyboard"),
    NumberPad(Icons.Outlined.Apps, "number_pad"),
    Candidates(Icons.Outlined.FormatListBulleted, "candidates"),
    Appearance(Icons.Outlined.Palette, "appearance"),
    Clipboard(Icons.Outlined.ContentPaste, "clipboard"),
    Emoji(Icons.Outlined.SentimentSatisfied, "emoji"),
    Phrase(Icons.Outlined.Bookmarks, "phrase"),
    Language(Icons.Outlined.Translate, "language"),
    Hide(Icons.Outlined.KeyboardArrowDown, "hide"),
    Back(Icons.Outlined.ArrowBack, "back"),
    Close(Icons.Outlined.Close, "close"),
    Search(Icons.Outlined.Search, "search"),
    ChevronLeft(Icons.Outlined.ChevronLeft, "chevron_left"),
    ChevronRight(Icons.Outlined.ChevronRight, "chevron_right"),
    More(Icons.Outlined.MoreHoriz, "more"),
    Pin(Icons.Outlined.PushPin, "pin"),
    Delete(Icons.Outlined.DeleteOutline, "delete"),
    Copy(Icons.Outlined.ContentCopy, "copy"),
    Code(Icons.Outlined.Code, "code"),
    Export(Icons.Outlined.SaveAlt, "export"),
    Restore(Icons.Outlined.SettingsBackupRestore, "restore"),
    Diagnostics(Icons.Outlined.Description, "diagnostics"),
    Add(Icons.Outlined.Add, "add"),
    TextFields(Icons.Outlined.TextFields, "text_fields"),
}
