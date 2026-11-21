/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.ime.phrase

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jiaoay.jrime.ui.design.ImeDimens
import com.jiaoay.jrime.ui.design.ImeShapes
import com.jiaoay.jrime.ui.design.ImeTypography
import com.jiaoay.jrime.ui.design.LocalImePalette

/**
 * The 常用语 panel: the `custom_phrase` dictionary as a tap-to-commit list,
 * plus an add strip (text + code). Tap a row to commit the phrase; the "×"
 * deletes it; both write straight back to the engine's user dictionary.
 */
@Suppress("FunctionName")
@Composable
fun PhrasePanel(
    modifier: Modifier = Modifier,
    onCommit: (String) -> Unit,
) {
    val palette = LocalImePalette.current
    val phrases by CustomPhraseStore.phrases.collectAsState()
    var text by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    val fieldStyle = ImeTypography.keySymbolStyle(palette.keyText)

    Column(
        modifier =
        modifier
            .fillMaxSize()
            .background(palette.surface)
            .padding(ImeDimens.PanelPadding),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(ImeDimens.CandidateHeight),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ImeDimens.CandidateGap),
        ) {
            PhraseField(
                value = text,
                placeholder = "常用语",
                style = fieldStyle,
                modifier = Modifier.weight(2f),
                onChange = { text = it },
            )
            PhraseField(
                value = code,
                placeholder = "编码",
                style = fieldStyle,
                modifier = Modifier.weight(1f),
                onChange = { code = it.trim() },
            )
            Box(
                modifier =
                Modifier
                    .height(ImeDimens.CandidateHeight)
                    .width(ImeDimens.MinTouchTarget)
                    .background(palette.accent, ImeShapes.Chip)
                    .clickable(enabled = text.isNotBlank() && code.isNotBlank()) {
                        CustomPhraseStore.add(CustomPhrase(text.trim(), code.trim()))
                        text = ""
                        code = ""
                    },
                contentAlignment = Alignment.Center,
            ) {
                BasicText("＋", style = ImeTypography.keySymbolStyle(palette.accentText))
            }
        }
        LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
            items(phrases, key = { "${it.text}\u0000${it.code}" }) { phrase ->
                PhraseRow(
                    phrase = phrase,
                    onCommit = { onCommit(phrase.text) },
                    onDelete = { CustomPhraseStore.delete(phrase) },
                )
            }
        }
    }
}

@Suppress("FunctionName")
@Composable
private fun PhraseField(
    value: String,
    placeholder: String,
    style: TextStyle,
    modifier: Modifier,
    onChange: (String) -> Unit,
) {
    val palette = LocalImePalette.current
    Box(
        modifier =
        modifier
            .height(ImeDimens.CandidateHeight)
            .background(palette.functionKey, ImeShapes.Chip)
            .padding(horizontal = ImeDimens.CandidateGap),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (value.isEmpty()) {
            BasicText(placeholder, style = style.copy(fontSize = ImeTypography.KeySymbol, color = palette.hintText))
        }
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = style.copy(fontSize = ImeTypography.KeySymbol),
        )
    }
}

@Suppress("FunctionName")
@Composable
private fun PhraseRow(
    phrase: CustomPhrase,
    onCommit: () -> Unit,
    onDelete: () -> Unit,
) {
    val palette = LocalImePalette.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = ImeDimens.KeyGap / 2f),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f).clickable(onClick = onCommit).padding(vertical = ImeDimens.KeyGap),
            horizontalArrangement = Arrangement.spacedBy(ImeDimens.CandidateGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicText(
                phrase.text,
                style = ImeTypography.candidateStyle(palette.candidateText),
            )
            BasicText(
                phrase.code,
                style = TextStyle(fontSize = 12.sp, color = palette.candidateComment),
            )
        }
        Box(
            modifier =
            Modifier
                .height(ImeDimens.CandidateHeight)
                .width(ImeDimens.MinTouchTarget)
                .clickable(onClick = onDelete),
            contentAlignment = Alignment.Center,
        ) {
            BasicText("×", style = ImeTypography.keySymbolStyle(palette.danger))
        }
    }
}
