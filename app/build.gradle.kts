plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
    id("kotlin-kapt")
}

android {
    namespace = "com.example.urbanfix"
    compileSdk {
        version = release(36)


    }
    packaging {
        resources {
            excludes.add("/META-INF/INDEX.LIST")
            excludes.add("/META-INF/DEPENDENCIES")
        }
    }

    defaultConfig {
        applicationId = "com.example.urbanfix"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        viewBinding =true
    }


    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)



    //firebase
    implementation(platform("com.google.firebase:firebase-bom:34.9.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")

    implementation("com.google.firebase:firebase-database")

    //FCM
    implementation("com.google.firebase:firebase-messaging:23.4.1")
    implementation("com.google.auth:google-auth-library-oauth2-http:1.23.0")
    implementation("com.google.api-client:google-api-client:2.3.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    //Pie chart
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    //Image
    implementation("com.google.firebase:firebase-storage")

    //Appwrite
    implementation("io.appwrite:sdk-for-android:12.0.0")
    // glide
    implementation("com.github.bumptech.glide:glide:4.16.0")
    kapt("com.github.bumptech.glide:compiler:4.16.0")
}