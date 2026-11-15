/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.data.theme

import android.content.Context
import android.graphics.Typeface
import android.graphics.fonts.Font
import android.graphics.fonts.FontFamily
import android.net.Uri
import android.os.Build
import com.jiaoay.jrime.BuildConfig
import com.jiaoay.jrime.core.data.DataManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

/**
 * The rare-glyph chain: a shipped subset font (built from HanaMinB against
 * the flyhe dictionary, covering the CJK extension blocks) leads, the
 * user-imported files follow, and the system sans face closes the chain.
 *
 * The shipped subset carries only the code points the system fonts lack,
 * so every glyph the system already draws keeps its look; the rare CJK
 * extension ideographs the candidate strip would otherwise render as tofu
 * fall through to the subset. The APK stays small -- the subset is trimmed
 * to the dictionary's own characters -- and the settings screen still
 * accepts an imported font for anything beyond the shipped coverage.
 */
object ImeFonts {
    /** The user's font directory, next to the rime data tree. */
    val fontDir: File get() = File(DataManager.userDataDir, "fonts").also { it.mkdirs() }

    private const val BUILTIN_ASSET = "fonts/jrime-ext-subset.ttf"

    private val FONT_EXTENSIONS = setOf("ttf", "otf", "ttc")

    /** Font files the user imported, in name order for a stable chain. */
    fun fontFiles(): List<File> =
        fontDir
            .listFiles { f -> f.isFile && f.extension.lowercase() in FONT_EXTENSIONS }
            ?.sortedBy { it.name }
            .orEmpty()

    /**
     * The IME-wide typeface: the shipped subset with the user imports
     * behind it and the system face closing the chain. Cheap when there
     * is nothing to chain -- the system default comes straight back.
     */
    fun buildTypeface(context: Context): Typeface {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return Typeface.DEFAULT
        val builtin = releaseBuiltin(context)
        val imports = fontFiles()
        if (builtin == null && imports.isEmpty()) return Typeface.DEFAULT
        return runCatching {
            val families =
                (listOfNotNull(builtin) + imports)
                    .distinct()
                    .map { file -> FontFamily.Builder(Font.Builder(file).build()).build() }
            Typeface.CustomFallbackBuilder(families.first())
                .apply {
                    for (i in 1 until families.size) {
                        addCustomFallback(families[i])
                    }
                }
                .setSystemFallback("sans-serif")
                .build()
        }.onFailure { Timber.e(it, "Failed to build the font chain") }
            .getOrDefault(Typeface.DEFAULT)
    }

    /**
     * Releases the shipped subset from the APK into the font dir under a
     * version-named file, so a new APK re-releases its subset and the old
     * one dies with the sweep. The copy is unconditional -- a few MB on
     * first use per version, nothing when the file already matches.
     */
    private fun releaseBuiltin(context: Context): File? =
        runCatching {
            val target = File(fontDir, "builtin-${BuildConfig.BUILD_VERSION_NAME}.ttf")
            val asset = context.assets.open(BUILTIN_ASSET)
            if (target.isFile && target.length() == asset.available().toLong()) {
                asset.close()
            } else {
                asset.use { input ->
                    val tmp = File(fontDir, "builtin.tmp")
                    tmp.outputStream().use { output -> input.copyTo(output) }
                    if (target.isFile) target.delete()
                    if (!tmp.renameTo(target)) {
                        tmp.delete()
                        error("rename failed")
                    }
                }
                Timber.d("Released the built-in subset font: $target (${target.length()} bytes)")
            }
            target
        }.onFailure {
            Timber.e(it, "Failed to release the built-in font; shipped coverage unavailable")
        }.getOrNull()

    /**
     * The cached chain, keyed on the directory signature: building reads
     * every chained file, so the palette flips that re-resolve styles must
     * not re-read them. An import changes the signature on the next IME
     * restart and rebuilds once.
     */
    @Volatile
    private var cache: Triple<List<Pair<String, Long>>, String, Typeface>? = null

    fun currentTypeface(context: Context): Typeface {
        val signature = fontFiles().map { it.name to it.length() }
        val version = BuildConfig.BUILD_VERSION_NAME
        cache?.let { (sig, ver, face) ->
            if (sig == signature && ver == version) return face
        }
        return buildTypeface(context).also { cache = Triple(signature, version, it) }
    }

    /**
     * Copies the picked document into [fontDir] under its display name.
     * Same-name re-import overwrites, so updating a font is one pick away.
     */
    suspend fun import(
        context: Context,
        uri: Uri,
    ): Result<File> =
        withContext(Dispatchers.IO) {
            runCatching {
                val name = queryDisplayName(context, uri)
                    ?.takeIf { it.contains('.') && it.substringAfterLast('.').lowercase() in FONT_EXTENSIONS }
                    ?: "imported-${System.currentTimeMillis()}.ttf"
                val target = File(fontDir, name)
                context.contentResolver.openInputStream(uri)!!.use { input ->
                    target.outputStream().use { output -> input.copyTo(output) }
                }
                Timber.d("Imported font: $target (${target.length()} bytes)")
                target
            }
        }

    private fun queryDisplayName(
        context: Context,
        uri: Uri,
    ): String? =
        context.contentResolver
            .query(uri, null, null, null, null)
            ?.use { cursor ->
                val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
            }
}
