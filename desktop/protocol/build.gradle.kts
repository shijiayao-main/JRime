// The desktop protocol source set: canonical JSON line codec, snapshot sampler
// and ready probe, plus the engine response DTOs they render. Engine-agnostic
// by construction (dependency inversion, doc/multiplatform-design.md §1.3).
plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
    sourceSets["main"].kotlin.srcDir("../protocol-common")
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}
