/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** User-facing strings the shared screen needs from the host. */
data class SettingsStrings(
    val title: String,
    val searchPlaceholder: String,
    val empty: String,
    val cancel: String,
    val confirm: String,
    val notSet: String = "",
)

/**
 * The Compose settings hub: one searchable screen with grouped rows, replacing
 * the per-page Preference fragments. The app supplies the specs and the store;
 * this screen owns layout, search and the editors only.
 */
@Suppress("FunctionName")
@Composable
fun SettingsScreen(
    groups: List<SettingGroup>,
    store: SettingsStore,
    strings: SettingsStrings,
    onAction: (String) -> Unit,
    modifier: Modifier = Modifier,
    options: RuntimeOptionSource? = null,
) {
    // Reading the revision re-renders the rows with the freshly stored values.
    val revision by store.revision.collectAsState()
    var query by remember { mutableStateOf("") }
    var editingChoice by remember { mutableStateOf<SettingSpec?>(null) }
    var editingText by remember { mutableStateOf<SettingSpec?>(null) }

    val visibleGroups =
        remember(revision, query, groups) {
            val needle = query.trim()
            groups
                .map { group ->
                    group.copy(
                        specs =
                        group.specs.filter { spec ->
                            !spec.hidden &&
                                (
                                    needle.isEmpty() ||
                                        spec.title.contains(needle, ignoreCase = true) ||
                                        spec.summary?.contains(needle, ignoreCase = true) == true
                                    )
                        },
                    )
                }.filter { it.specs.isNotEmpty() }
                .sortedBy { it.order }
        }

    Column(modifier = modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text(strings.searchPlaceholder) },
            singleLine = true,
            modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )
        if (visibleGroups.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(strings.empty)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                visibleGroups.forEach { group ->
                    item(key = "header-${group.id}") {
                        Text(
                            text = group.title,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
                        )
                    }
                    items(group.specs, key = { "spec-${it.key}" }) { spec ->
                        SettingRow(
                            spec = spec,
                            store = store,
                            strings = strings,
                            options = options,
                            onAction = onAction,
                            onEditChoice = { editingChoice = spec },
                            onEditText = { editingText = spec },
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
    }

    editingChoice?.let { spec ->
        ChoiceDialog(
            spec = spec,
            store = store,
            strings = strings,
            onDismiss = { editingChoice = null },
        )
    }
    editingText?.let { spec ->
        TextDialog(
            spec = spec,
            store = store,
            strings = strings,
            onDismiss = { editingText = null },
        )
    }
}

@Suppress("FunctionName")
@Composable
private fun SettingRow(
    spec: SettingSpec,
    store: SettingsStore,
    strings: SettingsStrings,
    options: RuntimeOptionSource?,
    onAction: (String) -> Unit,
    onEditChoice: () -> Unit,
    onEditText: () -> Unit,
) {
    Row(
        modifier =
        Modifier
            .fillMaxWidth()
            .clickable(enabled = spec.control is SettingControl.Action) {
                (spec.control as? SettingControl.Action)?.let { onAction(it.id) }
            }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = spec.title, style = MaterialTheme.typography.bodyLarge)
            spec.summary?.takeIf { it.isNotEmpty() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        when (val control = spec.control) {
            is SettingControl.Toggle -> {
                val value = store.bool(spec.key, control.defaultValue)
                Switch(checked = value, onCheckedChange = { store.setBool(spec.key, it) })
            }

            is SettingControl.Slider -> {
                val value = store.int(spec.key, control.defaultValue)
                Text(
                    text = if (value == 0 && control.min == 0) "" else "$value${control.unit}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Slider(
                    value = value.toFloat(),
                    onValueChange = { raw ->
                        val stepped =
                            ((raw / control.step).roundToInt() * control.step)
                                .coerceIn(control.min, control.max)
                        store.setInt(spec.key, stepped)
                    },
                    valueRange = control.min.toFloat()..control.max.toFloat(),
                    steps = ((control.max - control.min) / control.step - 1).coerceAtLeast(0),
                    modifier = Modifier.weight(1f),
                )
            }

            is SettingControl.Choice -> {
                val stored = store.string(spec.key, control.defaultValue)
                val label = control.options.firstOrNull { it.value == stored }?.label ?: stored
                TextButton(onClick = onEditChoice) { Text(label) }
            }

            is SettingControl.TextField -> {
                val stored = store.string(spec.key, control.defaultValue)
                TextButton(onClick = onEditText) {
                    Text(if (stored.isEmpty()) spec.title else stored, maxLines = 1)
                }
            }

            is SettingControl.Action -> {
                Text(text = "›", style = MaterialTheme.typography.titleMedium)
            }

            is SettingControl.ToggleOption -> {
                OptionToggle(options = options, control = control)
            }

            is SettingControl.OptionChoice -> {
                OptionChoiceRow(
                    options = options,
                    control = control,
                    title = spec.title,
                    cancelLabel = strings.cancel,
                )
            }
        }
    }
}

@Suppress("FunctionName")
@Composable
private fun ChoiceDialog(
    spec: SettingSpec,
    store: SettingsStore,
    strings: SettingsStrings,
    onDismiss: () -> Unit,
) {
    val control = spec.control as SettingControl.Choice
    val current = store.string(spec.key, control.defaultValue)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(spec.title) },
        text = {
            Column {
                control.options.forEach { option ->
                    Row(
                        modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                store.setString(spec.key, option.value)
                                onDismiss()
                            }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (option.value == current) "◉ ${option.label}" else "○ ${option.label}",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        },
    )
}

@Suppress("FunctionName")
@Composable
private fun TextDialog(
    spec: SettingSpec,
    store: SettingsStore,
    strings: SettingsStrings,
    onDismiss: () -> Unit,
) {
    val control = spec.control as SettingControl.TextField
    var value by remember { mutableStateOf(store.string(spec.key, control.defaultValue)) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(spec.title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                singleLine = true,
                keyboardOptions =
                if (control.numeric) {
                    KeyboardOptions(keyboardType = KeyboardType.Number)
                } else {
                    KeyboardOptions.Default
                },
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    store.setString(spec.key, value)
                    onDismiss()
                },
            ) { Text(strings.confirm) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        },
    )
}

@Suppress("FunctionName")
@Composable
private fun OptionToggle(
    options: RuntimeOptionSource?,
    control: SettingControl.ToggleOption,
) {
    val scope = rememberCoroutineScope()
    var value by remember(control.option) { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(control.option) {
        value = options?.let { runCatching { it.get(control.option) }.getOrNull() }
    }
    val available = options != null && value != null
    Switch(
        checked = value == true,
        enabled = available,
        onCheckedChange = { next ->
            if (options == null) return@Switch
            scope.launch {
                runCatching { options.set(control.option, next) }
                value = next
            }
        },
    )
}

@Suppress("FunctionName")
@Composable
private fun OptionChoiceRow(
    options: RuntimeOptionSource?,
    control: SettingControl.OptionChoice,
    title: String,
    cancelLabel: String,
) {
    val scope = rememberCoroutineScope()
    var selected by remember(control.optionNames) { mutableStateOf<Int?>(null) }
    var expanded by remember { mutableStateOf(false) }
    LaunchedEffect(control.optionNames) {
        val hits =
            options?.let { source ->
                runCatching {
                    control.optionNames.mapIndexedNotNull { index, option ->
                        if (source.get(option) == true) index else null
                    }
                }.getOrNull()
            }
        // A switch group with no member enabled still means "the first
        // state"; only an absent session leaves the row unavailable.
        selected = hits?.let { it.firstOrNull() ?: 0 }
    }
    TextButton(onClick = { expanded = true }, enabled = options != null) {
        Text(
            selected?.let { control.optionLabels.getOrNull(it) } ?: "—",
            maxLines = 1,
        )
    }
    if (expanded) {
        val current = selected
        AlertDialog(
            onDismissRequest = { expanded = false },
            title = { Text(title) },
            text = {
                Column {
                    control.optionLabels.forEachIndexed { index, label ->
                        Row(
                            modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (options != null) {
                                        scope.launch {
                                            control.optionNames.forEachIndexed { nameIndex, name ->
                                                runCatching { options.set(name, nameIndex == index) }
                                            }
                                            selected = index
                                        }
                                    }
                                    expanded = false
                                }
                                .padding(vertical = 10.dp),
                        ) {
                            Text(
                                text = if (index == current) "◉ $label" else "○ $label",
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { expanded = false }) { Text(cancelLabel) } },
        )
    }
}
