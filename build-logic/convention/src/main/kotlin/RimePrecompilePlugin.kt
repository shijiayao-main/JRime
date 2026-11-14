/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.gradle.process.ExecOperations
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.FileTime
import javax.inject.Inject

/**
 * Add task precompileRimeData: run librime's own deployment tool (built for the
 * host, see app/src/main/jni/host/CMakeLists.txt) over a scratch copy of the
 * shipped rime config set and stage the compiled artifacts into
 * assets/rime/build/, where the existing checksums release machinery deploys
 * them straight into the user dir's staging (startup-fastpath).
 *
 * Compile-only inputs (dictionary YAML sources) live outside the APK in
 * app/data/rime/dicts/ and are assembled into the scratch copy at build time;
 * the engine reuses the staged table/prism/reverse artifacts without ever
 * reading the YAML sources (DictCompiler "reuse existing table" path), so they
 * no longer ship. Config-chain sources (default/schema/custom/symbols/...) do
 * ship: maintenance re-expands them on every deploy.
 */
class RimePrecompilePlugin : Plugin<Project> {
    companion object {
        const val TASK = "precompileRimeData"
        const val BUILD_TOOL_TASK = "buildRimeDeployer"
        const val CLEAN_TASK = "cleanPrecompileRimeData"
    }

    private val Project.hostCMakeLists
        get() = file("src/main/jni/host/CMakeLists.txt")

    private val Project.hostBuildDir
        get() = file("src/main/jni/host/build-host")

    private val Project.deployerFile
        get() = hostBuildDir.resolve("librime-build/bin/rime_deployer")

    private val Project.dictSourcesDir
        get() = file("data/rime/dicts")

    private val Project.preludeDir
        get() = file("data/rime/prelude")

    private val Project.sharedDataDir
        get() = assetsDir.resolve("shared")

    private val Project.stagingAssetsDir
        get() = assetsDir.resolve("rime/build")

    override fun apply(target: Project) {
        val buildTool =
            target.tasks.register<BuildRimeDeployerTask>(BUILD_TOOL_TASK) {
                cmakeListsFile.set(target.hostCMakeLists)
                // Everything the host tool compiles from, except the Boost
                // tree: it is pinned by the archived tarball hash and cached
                // next to the jni root, so fingerprinting it would be waste.
                sourceDirs.setFrom(
                    listOf(
                        target.file("src/main/jni/cmake"),
                        target.file("src/main/jni/librime"),
                        target.file("src/main/jni/librime-lua"),
                        target.file("src/main/jni/librime-lua-deps"),
                        target.file("src/main/jni/librime-octagram"),
                        target.file("src/main/jni/librime-predict"),
                        target.file("src/main/jni/snappy"),
                    ),
                )
                buildDir.set(target.hostBuildDir)
                deployerFile.set(target.deployerFile)
            }

        val precompile =
            target.tasks.register<PrecompileRimeDataTask>(TASK) {
                dependsOn(buildTool)
                deployerFile.set(buildTool.flatMap { it.deployerFile })
                sharedDataDir.set(target.sharedDataDir)
                dictSourcesDir.set(target.dictSourcesDir)
                preludeDir.set(target.preludeDir)
                scratchDir.set(target.layout.buildDirectory.dir("rime-precompile"))
                outputDir.set(target.stagingAssetsDir)
            }
        // assets/shared is also installOpenCCData's output dir: the scratch
        // copy must see the generated opencc data, and gradle's
        // implicit-dependency validation insists on the explicit edge.
        precompile.configure { dependsOn(target.tasks.withType<OpenCCDataPlugin.InstallOpenCCDataTask>()) }
        // the artifacts are ordinary asset files: cover them with checksums
        target.tasks.getByName(DataChecksumsPlugin.TASK).dependsOn(precompile)

        target.tasks
            .register<Delete>(CLEAN_TASK) {
                delete(target.stagingAssetsDir)
            }.also {
                target.cleanTask.dependsOn(it)
            }
    }

    /** Build the host-side rime_deployer; cached on the vendored sources. */
    abstract class BuildRimeDeployerTask : DefaultTask() {
        @get:Inject
        abstract val execOperations: ExecOperations

        @get:InputFile
        abstract val cmakeListsFile: RegularFileProperty

        @get:PathSensitive(PathSensitivity.RELATIVE)
        @get:InputFiles
        abstract val sourceDirs: ConfigurableFileCollection

        @get:Internal
        abstract val buildDir: DirectoryProperty

        @get:OutputFile
        abstract val deployerFile: RegularFileProperty

        @TaskAction
        fun execute() {
            val build = buildDir.get().asFile.apply { mkdirs() }
            val source = cmakeListsFile.get().asFile
            logger.lifecycle("configuring host rime_deployer build in {}", build)
            execOperations.exec {
                commandLine("cmake", "-S", source.parentFile, "-B", build, "-DCMAKE_BUILD_TYPE=Release")
            }
            logger.lifecycle("building rime_deployer")
            execOperations.exec {
                commandLine(
                    "cmake",
                    "--build",
                    build,
                    "--target",
                    "rime_deployer",
                    "--parallel",
                    Runtime.getRuntime().availableProcessors().toString(),
                )
            }
            val deployer = deployerFile.get().asFile
            check(deployer.isFile && deployer.canExecute()) {
                "rime_deployer was not produced at $deployer"
            }
        }
    }

    /** Run the host deployer over a scratch copy and stage assets/rime/build/. */
    abstract class PrecompileRimeDataTask : DefaultTask() {
        @get:Inject
        abstract val execOperations: ExecOperations

        @get:InputFile
        abstract val deployerFile: RegularFileProperty

        @get:PathSensitive(PathSensitivity.RELATIVE)
        @get:InputDirectory
        abstract val sharedDataDir: DirectoryProperty

        @get:PathSensitive(PathSensitivity.RELATIVE)
        @get:InputDirectory
        abstract val dictSourcesDir: DirectoryProperty

        @get:PathSensitive(PathSensitivity.RELATIVE)
        @get:InputDirectory
        abstract val preludeDir: DirectoryProperty

        @get:Internal
        abstract val scratchDir: DirectoryProperty

        @get:OutputDirectory
        abstract val outputDir: DirectoryProperty

        private val shared by lazy { sharedDataDir.get().asFile }
        private val dictSources by lazy { dictSourcesDir.get().asFile }
        private val prelude by lazy { preludeDir.get().asFile }
        private val output by lazy { outputDir.get().asFile }
        private val scratch by lazy { scratchDir.get().asFile }
        private val scratchShared by lazy { scratch.resolve("shared") }
        private val scratchUser by lazy { scratch.resolve("user") }
        private val scratchStaging by lazy { scratchUser.resolve("build") }

        companion object {
            // Mirrors the shipped schema set: the schema_list patch in
            // assets/shared/default.custom.yaml plus flypy_xhfast's dependencies
            // (hidden base schemas that exist only to get their dictionaries
            // compiled, e.g. flyhe_fast and easy_en). The inventory checks below
            // fail the build when this drifts, so a newly shipped schema cannot
            // silently miss its prebuilt data.
            private val SCHEMAS = listOf("flypy_xhfast", "flyhe_fast", "flypy_radical", "easy_en")

            // The deployer records source file mtimes in the compiled configs'
            // __build_info/timestamps; pinning them makes the artifacts
            // byte-stable across rebuilds, so checksums and the device diff
            // stay quiet unless the inputs really change.
            private val SOURCE_EPOCH = FileTime.fromMillis(946684800000L) // 2000-01-01T00:00:00Z
        }

        @TaskAction
        fun execute() {
            prepareScratch()
            runDeployer()
            stageArtifacts()
        }

        /** Wipe and reassemble the scratch shared dir, dereferencing symlinks. */
        private fun prepareScratch() {
            check(shared.isDirectory) { "missing shared data dir: $shared" }
            check(dictSources.isDirectory) { "missing dictionary sources dir: $dictSources" }
            check(prelude.isDirectory) { "missing prelude dir: $prelude" }
            scratch.deleteRecursively()
            scratchShared.mkdirs()
            scratchUser.mkdirs()
            copyTree(shared, scratchShared)
            copyTree(dictSources, scratchShared)
            // The prelude files reach the deployer through the symlinks inside
            // the shared dir (cp -rL semantics above resolve them), so any
            // surviving link means the copy went wrong.
            assertNoSymlinks(scratch)
            normalizeTimestamps(scratchShared)
        }

        private fun copyTree(
            source: File,
            target: File,
        ) {
            Files.walk(source.toPath()).use { stream ->
                stream.forEach { path ->
                    val dest = target.toPath().resolve(source.toPath().relativize(path).toString())
                    if (Files.isDirectory(path)) {
                        Files.createDirectories(dest)
                    } else {
                        Files.createDirectories(dest.parent)
                        // without NOFOLLOW_LINKS a symlink is copied from its
                        // target: shared/key_bindings.yaml & friends resolve to
                        // data/rime/prelude, and a dead link would break the
                        // deployment.
                        Files.copy(path, dest, StandardCopyOption.REPLACE_EXISTING)
                    }
                }
            }
        }

        private fun assertNoSymlinks(root: File) {
            Files.walk(root.toPath()).use { stream ->
                val links = stream.filter { Files.isSymbolicLink(it) }.toList()
                check(links.isEmpty()) { "symlink(s) survived the scratch copy under $root: $links" }
            }
        }

        private fun normalizeTimestamps(root: File) {
            Files.walk(root.toPath()).use { stream ->
                stream.filter { Files.isRegularFile(it) }.forEach {
                    Files.setLastModifiedTime(it, SOURCE_EPOCH)
                }
            }
        }

        private fun runDeployer() {
            val log = scratch.resolve("deployer.log")
            val result =
                log.outputStream().buffered().use { out ->
                    execOperations.exec {
                        commandLine(
                            deployerFile.get().asFile.absolutePath,
                            "--build",
                            scratchUser.absolutePath,
                            scratchShared.absolutePath,
                            scratchStaging.absolutePath,
                        )
                        standardOutput = out
                        errorOutput = out
                        isIgnoreExitValue = true
                    }
                }
            if (result.exitValue != 0) {
                logger.error(log.readLines().takeLast(50).joinToString("\n"))
                error("rime_deployer failed with exit code ${result.exitValue}; see the log tail above and $log")
            }
            logger.lifecycle("rime_deployer staged {} file(s)", scratchStaging.listFiles()?.size ?: 0)
        }

        /** Publish the staged artifacts, verifying the shipped schema set. */
        private fun stageArtifacts() {
            check(scratchStaging.isDirectory) { "rime_deployer produced no staging dir at $scratchStaging" }
            val expected =
                listOf("default.yaml") +
                    SCHEMAS.flatMap { schema ->
                        listOf(
                            "$schema.schema.yaml",
                            "$schema.table.bin",
                            "$schema.prism.bin",
                            "$schema.reverse.bin",
                        )
                    }
            val missing = expected.filterNot { scratchStaging.resolve(it).isFile }
            check(missing.isEmpty()) {
                "rime_deployer output is missing expected artifact(s): $missing; " +
                    "if the shipped schema set changed, update SCHEMAS in RimePrecompilePlugin.kt"
            }
            // The staging copy below ships everything verbatim, so anything the
            // deployer produced beyond the expected set would silently ride the
            // APK into users' data dir; fail fast instead.
            val unexpected =
                scratchStaging.listFiles()?.map { it.name }?.filterNot { it in expected } ?: emptyList()
            check(unexpected.isEmpty()) {
                "rime_deployer output has unexpected artifact(s): $unexpected; " +
                    "if the shipped schema set changed, update SCHEMAS in RimePrecompilePlugin.kt"
            }
            output.deleteRecursively()
            output.mkdirs()
            scratchStaging.listFiles()?.forEach { file ->
                file.copyTo(output.resolve(file.name))
                logger.lifecycle("staged {}", file.name)
            }
        }
    }
}
