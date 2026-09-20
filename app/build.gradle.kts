plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
    namespace = "tw.local.memonote"
    compileSdk = 35
    defaultConfig {
        applicationId = "tw.local.memonote"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "1.1"
        testInstrumentationRunner = "tw.local.memonote.DemoSetup"
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    testOptions { unitTests.isReturnDefaultValues = true }
    lint { disable += "OldTargetApi" }
}
dependencies { testImplementation("junit:junit:4.13.2"); androidTestImplementation("androidx.test:runner:1.6.2"); androidTestImplementation("androidx.test.ext:junit:1.2.1") }
