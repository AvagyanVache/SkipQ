buildscript {
    dependencies {
        classpath("com.android.tools.build:gradle:8.1.0")           // Update Gradle version if needed
        classpath("com.google.gms:google-services:4.4.0")           // Google Services plugin
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.google.gms.google.services) apply false
}
