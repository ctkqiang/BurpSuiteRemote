plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ktlint)
}

android {
    namespace = "xin.ctkqiang.burpsuite.remote.mobileapp"

    compileSdk = 36

    defaultConfig {
        applicationId = "xin.ctkqiang.burpsuite.remote.mobileapp"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":ui"))
    implementation(project(":data"))
    implementation(project(":domain"))
    implementation(project(":feature:settings"))
    implementation(project(":feature:dashboard"))
    implementation(project(":feature:history"))
    implementation(project(":feature:connection"))
    implementation(project(":feature:intercept"))
    implementation(project(":feature:repeater"))
    implementation(project(":feature:archive"))
    implementation(project(":feature:screenshot"))
    implementation(project(":feature:sharing"))

    implementation(platform(libs.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    // NavHost 建在装配层：只有这里同时看得见各 feature 与导航模型（rules.md §6.1）。
    implementation(libs.androidx.navigation.compose)
    // 装配层要亲手建 Room 库（data 把 Room 藏在实现细节里，不对外暴露），因此这里显式声明它。
    implementation(libs.androidx.room.runtime)
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
}
