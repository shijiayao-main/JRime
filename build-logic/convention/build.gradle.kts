// SPDX-FileCopyrightText: 2015 - 2024 Rime community
//
// SPDX-License-Identifier: GPL-3.0-or-later

plugins {
    `kotlin-dsl`
    kotlin("plugin.serialization") version embeddedKotlinVersion
    `java-gradle-plugin`
}

group = "com.jiaoay.jrime.build_logic"

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    implementation(libs.kotlinx.serialization.json)
}

gradlePlugin {
    plugins {
        register("androidAppConvention") {
            id = "com.jiaoay.jrime.app-convention"
            implementationClass = "AndroidAppConventionPlugin"
        }
        register("dataChecksums") {
            id = "com.jiaoay.jrime.data-checksums"
            implementationClass = "DataChecksumsPlugin"
        }
        register("nativeAppConvention") {
            id = "com.jiaoay.jrime.native-app-convention"
            implementationClass = "NativeAppConventionPlugin"
        }
        register("nativeCacheHash") {
            id = "com.jiaoay.jrime.native-cache-hash"
            implementationClass = "NativeCacheHashPlugin"
        }
        register("openccData") {
            id = "com.jiaoay.jrime.opencc-data"
            implementationClass = "OpenCCDataPlugin"
        }
        register("rimePrecompile") {
            id = "com.jiaoay.jrime.rime-precompile"
            implementationClass = "RimePrecompilePlugin"
        }
    }
}
