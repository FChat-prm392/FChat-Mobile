import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "fpt.edu.vn.fchat_mobile"
    compileSdk = 35

    defaultConfig {
        applicationId = "fpt.edu.vn.fchat_mobile"
        minSdk = 31
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val props = Properties()
        val localPropsFile = rootProject.file("local.properties")
        if (localPropsFile.exists()) {
            props.load(FileInputStream(localPropsFile))
            val baseUrl = props["API_BASE_URL"] as String
            buildConfigField("String", "BASE_URL", "\"$baseUrl\"")
        }
    }

    /*signingConfigs {
        create("newdebug") {
            keyAlias = "androiddebugkey"
            keyPassword = "android"
            storeFile = file("../debug-new.keystore")
            storePassword = "android"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("newdebug")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }*/
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        buildConfig = true
        viewBinding = true
    }
}

dependencies {
    implementation ("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation ("io.socket:socket.io-client:2.0.1")
    implementation("com.github.bumptech.glide:glide:4.15.1")
    implementation(libs.firebase.storage)
    annotationProcessor("com.github.bumptech.glide:compiler:4.15.1")
    
    // Firebase Authentication
    implementation("com.google.firebase:firebase-auth:22.3.1")
    implementation("com.google.android.gms:play-services-auth:21.0.0")
    
    // FlexboxLayout for reaction display
    implementation("com.google.android.flexbox:flexbox:3.0.0")

    implementation("com.google.android.material:material:1.12.0")


    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
}