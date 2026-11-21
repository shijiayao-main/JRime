// The desktop IME host (D2): one JVM serving the line protocol and drawing the
// candidate window with the shared Compose surface. The Swift/ibus/TSF shells
// speak the same protocol; this process replaces the headless DesktopServer
// when a platform shell wants the Compose candidate window.
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
    implementation(project(":desktop:engine"))
    implementation(project(":desktop:protocol"))
    implementation(project(":ui:ime"))
    implementation(project(":ui:design"))
    implementation(compose.desktop.currentOs)
}

compose.desktop {
    application {
        mainClass = "com.jiaoay.jrime.desktop.app.MainKt"
    }
}

// The line protocol rides stdin: keep it attached to the shell pipe.
tasks.withType<JavaExec> {
    standardInput = System.`in`
}
