// Desktop settings shell: the shared :ui:settings Compose screen hosted in a
// normal desktop window, backed by a small properties file (the Android app
// uses AppPrefs instead; the screen and specs are the same contract).
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
    implementation(project(":ui:settings"))
    implementation(compose.desktop.currentOs)
    implementation(compose.runtime)
    implementation(compose.foundation)
    implementation(compose.material3)
    implementation(libs.kotlinx.coroutines.core)
}

compose.desktop {
    application {
        mainClass = "com.jiaoay.jrime.desktop.settings.MainKt"
    }
}
