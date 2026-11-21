// D0 feasibility spike: can a Compose Multiplatform window serve as the shared
// desktop IME surface (candidate/status window)? The window must stay on top
// without stealing focus. This module only proves the build and window
// behaviour; the real desktop UI lands in :ui:* with a desktop target in D2.
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
}

compose.desktop {
    application {
        mainClass = "com.jiaoay.jrime.desktop.uispike.MainKt"
    }
}
