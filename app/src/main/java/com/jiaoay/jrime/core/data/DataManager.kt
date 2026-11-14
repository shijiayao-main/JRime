/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.jiaoay.jrime.core.data

import android.content.res.AssetManager
import com.jiaoay.jrime.TrimeApplication
import com.jiaoay.jrime.util.FileUtils
import com.jiaoay.jrime.util.appContext
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.io.File
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/** Outcome of one [DataManager.sync] pass over the asset release chain. */
data class DataSyncResult(
    /** Entries the manifest lists but that could not be released this pass. */
    val skippedFiles: List<String>,
) {
    val isComplete: Boolean get() = skippedFiles.isEmpty()
}

object DataManager {
    private const val DATA_CHECKSUMS_NAME = "checksums.json"

    private val lock = ReentrantLock()

    private val json by lazy { Json }

    private fun deserializeDataChecksums(raw: String): DataChecksums = json.decodeFromString<DataChecksums>(raw)

    private fun serializeDataChecksums(checksums: DataChecksums): String = json.encodeToString(checksums)

    // The record describes one deployed tree, and the tree's directory changes with the
    // boot mode: the device-protected files dir before the first unlock, the external
    // files dir afterwards. Keep one record per mode in the internal files dir, or a
    // normal-mode run would overwrite the direct-boot record while deploying elsewhere,
    // and the next direct boot would trust it and skip deploying its own copy.
    private val filesDir get() = TrimeApplication.getInstance().directBootAwareContext.filesDir

    private fun AssetManager.dataChecksums(): DataChecksums = open(DATA_CHECKSUMS_NAME)
        .bufferedReader()
        .use { it.readText() }
        .let { deserializeDataChecksums(it) }

    private val externalFilesDir get() = TrimeApplication.getInstance().externalFilesDir

    val sharedDataDir = File(externalFilesDir, "shared").also { it.mkdirs() }
    val userDataDir = File(externalFilesDir, "rime").also { it.mkdirs() }

    val prebuiltDataDir = File(sharedDataDir, "build")
    val stagingDir = File(userDataDir, "build")

    /**
     * Return the absolute path of the compiled config file
     * based on given resource id.
     *
     * @param resourceId usually equals the config file name without the extension
     * @return the absolute path of the compiled config file
     */
    @JvmStatic
    fun resolveDeployedResourcePath(resourceId: String): String {
        val defaultPath = File(stagingDir, "$resourceId.yaml")
        if (!defaultPath.exists()) {
            val fallbackPath = File(prebuiltDataDir, "$resourceId.yaml")
            if (fallbackPath.exists()) return fallbackPath.absolutePath
        }
        return defaultPath.absolutePath
    }

    /**
     * Releases the asset tree into the data dirs by diffing the shipped
     * checksums against the deployed record. This is the asset release chain
     * — the only channel through which the prebuilt staging artifacts reach
     * the engine — and deliberately runs on every engine start.
     *
     * A manifest entry whose asset is missing or unwritable is logged and
     * skipped instead of crashing the caller: a partial release must never
     * take the rime thread down. Skipped entries are kept out of the deployed
     * record so the next sync retries them, and the result reports them —
     * an incomplete release is never silently reported as success.
     */
    fun sync(): DataSyncResult = lock.withLock {
        val destChecksumsFile = File(filesDir, DATA_CHECKSUMS_NAME)
        val oldChecksums =
            destChecksumsFile
                .runCatching { deserializeDataChecksums(bufferedReader().use { it.readText() }) }
                .getOrElse { DataChecksums("", emptyMap()) }

        val newChecksums =
            appContext.assets.runCatching { dataChecksums() }
                .getOrElse { e ->
                    // A manifest that cannot even be read leaves the previously
                    // released tree in place; fail loudly but do not crash.
                    Timber.e(e, "Failed to read $DATA_CHECKSUMS_NAME from assets; keeping the deployed tree as-is")
                    return@withLock DataSyncResult(listOf(DATA_CHECKSUMS_NAME))
                }

        val skippedFiles = mutableListOf<String>()
        DataDiff.diff(oldChecksums, newChecksums).sortedByDescending { it.ordinal }.forEach {
            Timber.d("Diff: $it")
            val succeeded =
                when (it) {
                    is DataDiff.CreateFile,
                    is DataDiff.UpdateFile,
                    -> appContext.assets
                        .runCatching { copyFile(it.path) }
                        .onFailure { e -> Timber.e(e, "Failed to release asset '${it.path}'") }
                        .isSuccess

                    is DataDiff.DeleteDir,
                    is DataDiff.DeleteFile,
                    -> removePath(it.path)
                        .onFailure { e -> Timber.e(e, "Failed to remove '${it.path}'") }
                        .isSuccess
                }
            if (!succeeded) skippedFiles.add(it.path)
        }

        val record = DataChecksums.afterRelease(newChecksums, oldChecksums, skippedFiles)
        destChecksumsFile.bufferedWriter().use {
            it.write(serializeDataChecksums(record))
        }
        if (skippedFiles.isNotEmpty()) {
            Timber.e("Data sync incomplete: ${skippedFiles.size} entry(ies) skipped: $skippedFiles")
        }
        Timber.d("Synced!")
        DataSyncResult(skippedFiles)
    }

    /**
     * Whether the engine can come up with usable schemas without a deployment:
     * the deployed tree must hold the compiled default config plus at least
     * one schema with its dictionary binaries, in the staging dir or in the
     * prebuilt fallback dir the engine's deployed-resource resolver checks.
     *
     * When this is false after a fresh install, the engine starts (it must
     * not soft-lock) but produces no candidates until the user repairs the
     * tree with a full deployment from the deploy menu.
     */
    fun hasStagingArtifacts(): Boolean = StagingArtifacts.isReady(
        stagingDir = stagingDir.fileNames(),
        prebuiltDir = prebuiltDataDir.fileNames(),
    )

    /**
     * The repair hook behind the deploy menu: re-verifies every recorded
     * staging artifact against the disk and drops the release-record entries
     * that are missing or corrupted, so the next [sync] re-releases them
     * from the APK before the full maintenance pass runs.
     *
     * The dictionary YAML sources stopped shipping with task 3.1, so an
     * on-device recompile cannot rebuild the table binaries on its own —
     * this re-release is what makes the deploy menu recover a deleted or
     * corrupted `build/` tree.
     */
    fun revalidateStagingRelease(): List<String> = lock.withLock {
        val destChecksumsFile = File(filesDir, DATA_CHECKSUMS_NAME)
        val oldChecksums =
            destChecksumsFile
                .runCatching { deserializeDataChecksums(bufferedReader().use { it.readText() }) }
                .getOrElse { DataChecksums("", emptyMap()) }
        val stale =
            StagingRelease.staleEntries(oldChecksums.files) { path ->
                StagingRelease.sha256OrNull(File(externalFilesDir, path))
            }
        if (stale.isNotEmpty()) {
            Timber.w("Staging artifacts missing or corrupted, dropping ${stale.size} release record entry(ies): $stale")
            destChecksumsFile.bufferedWriter().use {
                it.write(serializeDataChecksums(DataChecksums.afterRelease(oldChecksums, oldChecksums, stale)))
            }
        }
        stale
    }

    private fun File.fileNames(): List<String> = listFiles()?.map { it.name } ?: emptyList()

    private fun AssetManager.copyFile(filename: String) {
        open(filename).use { i ->
            File(externalFilesDir, filename)
                .also { it.parentFile?.mkdirs() }
                .outputStream()
                .use { o -> i.copyTo(o) }
        }
    }

    private fun removePath(path: String) = FileUtils.delete(externalFilesDir.resolve(path))
}
