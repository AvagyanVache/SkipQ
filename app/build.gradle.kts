plugins {
    id("com.android.application")
    id("com.google.gms.google-services") // Firebase services plugin
}

android {
    compileSdk = 33

    defaultConfig {
        vectorDrawables {
            useSupportLibrary = true
        }
        applicationId = "com.example.skipq"
        minSdk = 26
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"
    }
buildFeatures{
    viewBinding = true
}
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }

    namespace = "com.example.skipq"
}

dependencies {
    implementation(platform("com.google.firebase:firebase-bom:32.1.1")) // Firebase BOM for consistent versions
    implementation("com.google.firebase:firebase-auth")                // Firebase Authentication
    implementation("com.google.firebase:firebase-analytics")           // Firebase Analytics (optional)
    implementation("androidx.appcompat:appcompat:1.2.0")
    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.appcompat:appcompat:1.4.2")
    implementation("com.google.android.material:material:1.5.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.3")
    implementation ("com.github.bumptech.glide:glide:4.15.1")
    annotationProcessor ("com.github.bumptech.glide:compiler:4.15.1")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.3")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")
}