plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ktlint)
}

android {
    namespace = "xin.ctkqiang.burpsuite.remote.mobileapp.ui"

    compileSdk = 36

    defaultConfig {
        minSdk = 26
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

// rules.md §2 指定 JUnit 5：主题令牌与系统栏图标映射是纯值逻辑，必须有断言盯着。
tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

dependencies {
    // 主题要认 ThemeMode，那是领域层的类型；界面层依赖领域层是允许的方向。
    implementation(project(":domain"))
    // 技术日志要记「导航切换」，界面层据此上报每一次路由变化；它出现在导航依赖表的公开签名里，
    // 因此用 api 而不是 implementation——藏起来会让各 feature 拿到依赖表却读不到日志端口。
    api(project(":core:common"))

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.foundation)
    // 液态玻璃：底栏浮在内容之上，需要对下层做实时模糊。BurpRemoteScaffold 的公开签名里
    // 用了 HazeState 类型，所以用 api 暴露出去让 app 模块也能看到。
    api(libs.haze)
    // 底栏条目的 icon 是必填参数，核心图标集够用；扩展集太大，不进包。
    implementation(libs.compose.material.icons.core)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}
