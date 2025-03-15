plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
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
    implementation(platform("com.google.firebase:firebase-bom:32.1.1"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-analytics")
    implementation("androidx.appcompat:appcompat:1.2.0")
    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.appcompat:appcompat:1.4.2")
    implementation("com.google.android.material:material:1.5.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.3")
    implementation ("com.github.bumptech.glide:glide:4.15.1")
    implementation ("com.google.android.gms:play-services-base:18.0.0")
    implementation ("com.google.firebase:firebase-auth:21.0.1")
    implementation ("com.google.firebase:firebase-firestore:24.4.0")
    implementation ("com.google.android.gms:play-services-auth:20.2.0")
    implementation ("com.google.firebase:firebase-auth:21.2.0")
    implementation ("androidx.navigation:navigation-fragment-ktx:2.5.0")
    implementation ("androidx.navigation:navigation-ui-ktx:2.5.0")
    implementation ("com.google.mlkit:translate:17.0.1")
    implementation ("com.google.code.gson:gson:2.8.8")
    implementation("com.google.android.gms:play-services-maps:19.1.0")
    implementation ("com.google.android.gms:play-services-location:21.0.1")
    implementation ("com.google.android.gms:play-services-maps:18.2.0")




    annotationProcessor ("com.github.bumptech.glide:compiler:4.15.1")


    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.3")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")
}