// Top-level build file — plugin declarations only.
// All dependency versions are in gradle/libs.versions.toml (created in S-02).
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android)      apply false
    alias(libs.plugins.kotlin.compose)      apply false
    alias(libs.plugins.hilt.android)        apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp)                 apply false
}

