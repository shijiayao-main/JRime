/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.ui.main.settings.userdict

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import com.jiaoay.jrime.R
import com.jiaoay.jrime.daemon.RimeDaemon
import com.jiaoay.jrime.data.userdict.UserDictEntry
import com.jiaoay.jrime.data.userdict.UserDictManager
import com.jiaoay.jrime.data.userdict.UserDictText
import com.jiaoay.jrime.util.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/**
 * Browses/edits one Rime user dictionary through the engine's own text
 * export/import path: Rime keeps userdb values as plain text
 * (`code<TAB>phrase<TAB>c=...`), so adding a word is a positive `c=`, deleting
 * is a negative one (Rime treats negative commits as "deleted"), and the
 * import merges the delta.
 */
class UserDictBrowserFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        setContent { BrowserScreen() }
    }

    @Suppress("FunctionName")
    @Composable
    private fun BrowserScreen() {
        val scope = rememberCoroutineScope()
        var dicts by remember { mutableStateOf<List<String>>(emptyList()) }
        var selected by remember { mutableStateOf<String?>(null) }
        var entries by remember { mutableStateOf<List<UserDictEntry>>(emptyList()) }
        var loading by remember { mutableStateOf(true) }
        var query by remember { mutableStateOf("") }
        var phrase by remember { mutableStateOf("") }
        var code by remember { mutableStateOf("") }

        fun showError(error: Throwable) {
            requireContext().toast(
                requireContext().getString(R.string.user_dict_browser_failed, error.message ?: ""),
            )
        }

        fun reload(dict: String) {
            scope.launch {
                loading = true
                runCatching { withContext(Dispatchers.IO) { exportEntries(dict) } }
                    .onSuccess { entries = it }
                    .onFailure { showError(it) }
                loading = false
            }
        }

        fun persist(
            dict: String,
            next: List<UserDictEntry>,
        ) {
            scope.launch {
                runCatching {
                    withContext(Dispatchers.IO) {
                        importEntries(dict, next)
                        exportEntries(dict).also {
                            // The engine caches user dictionaries in the live session;
                            // a restart is what makes imported/deleted entries visible.
                            RimeDaemon.restartRime(fullCheck = false)
                        }
                    }
                }.onSuccess {
                    entries = it
                    requireContext().toast(requireContext().getString(R.string.user_dict_browser_done, it.size))
                }.onFailure { showError(it) }
            }
        }

        LaunchedEffect(Unit) {
            runCatching { withContext(Dispatchers.IO) { UserDictManager.getUserDictList().toList() } }
                .onSuccess { list ->
                    dicts = list
                    // Prefer the schema this build is specialized for.
                    selected = list.firstOrNull { it == "flypy_xhfast" } ?: list.firstOrNull()
                }.onFailure { error ->
                    requireContext().toast(
                        requireContext().getString(
                            R.string.user_dict_browser_load_failed,
                            error.message ?: "",
                        ),
                    )
                }
            loading = false
        }
        LaunchedEffect(selected) { selected?.let { reload(it) } }

        Surface(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Text(
                    text = getString(R.string.user_dict_browser_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                if (dicts.size > 1) {
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        dicts.forEach { dict ->
                            FilterChip(
                                selected = dict == selected,
                                onClick = { selected = dict },
                                label = { Text(dict) },
                            )
                        }
                    }
                }
                if (loading) {
                    CircularProgressIndicator(modifier = Modifier.padding(vertical = 24.dp))
                } else if (dicts.isEmpty()) {
                    Text(
                        text = getString(R.string.user_dict_browser_empty),
                        modifier = Modifier.padding(vertical = 16.dp),
                    )
                } else {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        label = { Text(getString(R.string.user_dict_browser_search)) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = phrase,
                            onValueChange = { phrase = it },
                            singleLine = true,
                            label = { Text(getString(R.string.user_dict_browser_phrase_hint)) },
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = code,
                            onValueChange = { code = it },
                            singleLine = true,
                            label = { Text(getString(R.string.user_dict_browser_code_hint)) },
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedButton(
                            enabled = phrase.isNotBlank() && code.isNotBlank() && selected != null,
                            onClick = {
                                val dict = selected ?: return@OutlinedButton
                                if (phrase.isBlank() || code.isBlank()) return@OutlinedButton
                                persist(dict, UserDictText.add(entries, code.trim(), phrase.trim()))
                                phrase = ""
                                code = ""
                            },
                        ) {
                            Text(getString(R.string.user_dict_browser_add))
                        }
                    }
                    Text(
                        text = getString(R.string.user_dict_browser_entries, entries.size),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                    val shown =
                        entries.filter { entry ->
                            query.isBlank() ||
                                entry.phrase.contains(query, ignoreCase = true) ||
                                entry.code.contains(query, ignoreCase = true)
                        }
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 4.dp),
                    ) {
                        items(shown, key = { it.code + "\t" + it.phrase }) { entry ->
                            UserDictEntryRow(
                                entry = entry,
                                onBump = {
                                    val dict = selected ?: return@UserDictEntryRow
                                    persist(dict, entries.map { if (it === entry) UserDictText.bump(it) else it })
                                },
                                onDelete = {
                                    val dict = selected ?: return@UserDictEntryRow
                                    persist(dict, entries.map { if (it === entry) UserDictText.markDeleted(it) else it })
                                },
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }

    @Suppress("FunctionName")
    @Composable
    private fun UserDictEntryRow(
        entry: UserDictEntry,
        onBump: () -> Unit,
        onDelete: () -> Unit,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.phrase,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text =
                    "${entry.code} · c=${entry.commits}" +
                        if (entry.deleted) " (${getString(R.string.user_dict_browser_deleted)})" else "",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (entry.deleted) {
                TextButton(onClick = onBump) { Text(getString(R.string.user_dict_browser_restore)) }
            } else {
                TextButton(onClick = onBump) { Text("+1") }
                TextButton(onClick = onDelete) { Text(getString(R.string.user_dict_browser_delete)) }
            }
        }
    }
}

private fun exportEntries(dict: String): List<UserDictEntry> {
    val out = ByteArrayOutputStream()
    UserDictManager.exportUserDict(out, dict, "userdict-browser.txt").getOrThrow()
    return UserDictText.parse(out.toString(Charsets.UTF_8.name()))
}

private fun importEntries(
    dict: String,
    entries: List<UserDictEntry>,
) {
    val bytes = UserDictText.render(entries).toByteArray(Charsets.UTF_8)
    UserDictManager.importUserDict(
        ByteArrayInputStream(bytes),
        dict,
        "userdict-browser-delta.txt",
    ).getOrThrow()
}
