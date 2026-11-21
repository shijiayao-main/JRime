// The desktop UI process (D2): candidate/status window rendered with the shared
// Compose surface, fed by the line-protocol engine client. This round proves
// the shared :ui:ime surface renders in a CMP window; wiring the real engine
// protocol and the platform shells lands next.
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
    implementation(project(":ui:ime"))
    implementation(project(":ui:design"))
    implementation(compose.desktop.currentOs)
}

compose.desktop {
    application {
        mainClass = "com.jiaoay.jrime.desktop.ui.MainKt"
    }
}
