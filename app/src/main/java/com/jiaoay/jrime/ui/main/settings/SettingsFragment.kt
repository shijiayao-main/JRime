/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.ui.main.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import com.jiaoay.jrime.BuildConfig
import com.jiaoay.jrime.R
import com.jiaoay.jrime.data.backup.UserDataBackup
import com.jiaoay.jrime.data.prefs.AppPrefs
import com.jiaoay.jrime.data.theme.ImeFonts
import com.jiaoay.jrime.ui.main.NavigationRoute
import com.jiaoay.jrime.ui.settings.SettingsScreen
import com.jiaoay.jrime.ui.settings.SettingsStrings
import com.jiaoay.jrime.util.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The settings hub host: a Fragment only because the app's navigation is
 * fragment-based; the screen itself is the Compose `SettingsScreen` fed by
 * [AppSettingsStore] + [SettingsSpecsFactory].
 */
class SettingsFragment : Fragment() {
    private var store: AppSettingsStore? = null

    private val importFont =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) importFont(uri)
        }

    private val exportBackup =
        registerForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
            if (uri != null) exportBackup(uri)
        }

    private val importBackup =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) confirmImportBackup(uri)
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val appStore = AppSettingsStore(PreferenceManager.getDefaultSharedPreferences(requireContext()))
        store = appStore
        val baseGroups = SettingsSpecsFactory.forContext(requireContext()).build()
        val inputTitle = baseGroups.firstOrNull { it.id == "input" }?.title.orEmpty()
        val ruleRows = InputRuleSpecs.build(labeler = { getString(it) }, groupTitle = inputTitle)
        val groups =
            baseGroups.map { group ->
                if (group.id == "input") {
                    group.copy(specs = ruleRows + group.specs)
                } else {
                    group
                }
            }
        val strings =
            SettingsStrings(
                title = getString(R.string.settings),
                searchPlaceholder = getString(R.string.search_settings),
                empty = getString(R.string.no_matching_settings),
                cancel = getString(R.string.cancel),
                confirm = getString(R.string.ok),
                back = getString(R.string.settings_back),
                itemsCountFormat = getString(R.string.settings_items_count),
                categories = getString(R.string.settings_categories),
            )
        return ComposeView(requireContext()).apply {
            setContent {
                // The hub wears the app's brand blue (the keyboard's accent
                // default) instead of Material's baseline purple.
                val dark = isSystemInDarkTheme()
                MaterialTheme(
                    colorScheme =
                    if (dark) {
                        darkColorScheme(primary = Color(0xFF8AB4F8))
                    } else {
                        lightColorScheme(primary = Color(0xFF1A73E8))
                    },
                ) {
                    SettingsScreen(
                        groups = groups,
                        store = appStore,
                        strings = strings,
                        onAction = ::handleAction,
                        options = RimeRuntimeOptionSource(),
                    )
                }
            }
        }
    }

    private fun handleAction(id: String) {
        when (id) {
            SettingsSpecsFactory.ACTION_IMPORT_FONT ->
                importFont.launch(arrayOf("font/*", "application/octet-stream"))

            SettingsSpecsFactory.ACTION_EXPORT_BACKUP ->
                exportBackup.launch("jrime-backup-${backupStamp()}.zip")

            SettingsSpecsFactory.ACTION_IMPORT_BACKUP ->
                importBackup.launch(arrayOf("application/zip", "application/octet-stream"))

            SettingsSpecsFactory.ACTION_COPY_DIAGNOSTICS -> copyDiagnostics()

            SettingsSpecsFactory.ACTION_OPEN_USERDICT_BROWSER ->
                findNavController().navigate(NavigationRoute.UserDictBrowser)
        }
    }

    private fun backupStamp(): String = SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date())

    private fun exportBackup(uri: Uri) {
        val resolver = requireContext().contentResolver
        lifecycleScope.launch {
            val result =
                withContext(Dispatchers.IO) {
                    runCatching {
                        resolver.openOutputStream(uri)?.use { out ->
                            UserDataBackup.exportTo(out).getOrThrow()
                        } ?: error("cannot open the target file")
                    }
                }
            result
                .onSuccess { summary ->
                    requireContext().toast(getString(R.string.backup_export_done, summary.dictionaries))
                }.onFailure {
                    requireContext().toast(getString(R.string.backup_failed, it.message.orEmpty()))
                }
        }
    }

    private fun confirmImportBackup(uri: Uri) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.backup_import_title)
            .setMessage(R.string.backup_import_confirm)
            .setPositiveButton(R.string.ok) { _, _ -> importBackup(uri) }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun importBackup(uri: Uri) {
        val resolver = requireContext().contentResolver
        lifecycleScope.launch {
            val result =
                withContext(Dispatchers.IO) {
                    runCatching {
                        resolver.openInputStream(uri)?.use { input ->
                            UserDataBackup.importFrom(input).getOrThrow()
                        } ?: error("cannot open the archive")
                    }
                }
            result
                .onSuccess { summary ->
                    requireContext().toast(getString(R.string.backup_import_done, summary.dictionaries))
                }.onFailure {
                    requireContext().toast(getString(R.string.backup_failed, it.message.orEmpty()))
                }
        }
    }

    /** A copyable snapshot for bug reports: build ids, device, engine state. */
    private fun copyDiagnostics() {
        val api = context?.getSystemService(ClipboardManager::class.java)
        val text =
            buildString {
                appendLine("JRime ${BuildConfig.BUILD_VERSION_NAME} (${BuildConfig.BUILD_COMMIT_HASH})")
                appendLine("Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT}) / ${Build.MANUFACTURER} ${Build.MODEL}")
                appendLine("Engine error: ${com.jiaoay.jrime.daemon.RimeDaemon.startupErrorOrNull()?.message ?: "none"}")
                appendLine(
                    "IME active: " +
                        android.provider.Settings.Secure.getString(
                            requireContext().contentResolver,
                            android.provider.Settings.Secure.DEFAULT_INPUT_METHOD,
                        ),
                )
                append("Preferences: ${AppPrefs.defaultInstance().settingProviders().size} groups")
            }
        api?.setPrimaryClip(ClipData.newPlainText("JRime diagnostics", text))
        requireContext().toast(R.string.diagnostics_copied)
    }

    private fun importFont(uri: Uri) {
        viewLifecycleOwner.lifecycleScope.launch {
            ImeFonts
                .import(requireContext(), uri)
                .onSuccess {
                    requireContext().toast(R.string.import_font_done)
                }.onFailure {
                    requireContext().toast(getString(R.string.import_font_failed, it.message))
                }
        }
    }

    override fun onStop() {
        AppPrefs.defaultInstance().syncToDeviceEncryptedStorage()
        super.onStop()
    }

    override fun onDestroyView() {
        store?.dispose()
        store = null
        super.onDestroyView()
    }
}
