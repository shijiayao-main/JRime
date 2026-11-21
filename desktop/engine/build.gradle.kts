/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// The desktop JVM engine: the line-protocol server plus the shared protocol
// source set. D1a keeps the historical source layout (the kotlinc scripts and
// the Swift bundle still compile these exact directories) while giving the
// engine a real Gradle module for the module/protocol convergence in D1b.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
    sourceSets["main"].kotlin.srcDirs("java", "../protocol-common")
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}
