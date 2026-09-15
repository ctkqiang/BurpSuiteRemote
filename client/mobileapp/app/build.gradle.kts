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
    // 装配层要亲手建 AndroidTechnicalLog 与事件摄入的进程级作用域，日志端口因此在这里可见。
    implementation(project(":core:common"))
    // 配对入口要在本地解二维码里的票据、校验协议版本，因此协议层也在这里可见。
    implementation(project(":core:protocol"))
    implementation(project(":feature:settings"))
    implementation(project(":feature:dashboard"))
    implementation(project(":feature:history"))
    implementation(project(":feature:connection"))
    implementation(project(":feature:intercept"))
    implementation(project(":feature:repeater"))
    implementation(project(":feature:sharing"))

    implementation(platform(libs.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    // NavHost 建在装配层：只有这里同时看得见各 feature 与导航模型（rules.md §6.1）。
    implementation(libs.androidx.navigation.compose)
    // 装配层持有传输引擎的实例与生命周期（进程退出时要把它关掉），因此需要它的类型在编译期可见。
    implementation(libs.ktor.client.core)
    // 装配层要在 RestRemoteRepeaterWriter 里手动构建 JSON 请求体（requestText + tabName），
    // 因此需要 kotlinx-serialization-json 的类型在编译期可见。
    implementation(libs.kotlinx.serialization.json)
    // 装配层要亲手建 Room 库（data 把 Room 藏在实现细节里，不对外暴露），因此这里显式声明它。
    implementation(libs.androidx.room.runtime)
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
}
