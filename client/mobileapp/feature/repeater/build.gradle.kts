plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ktlint)
}

android {
    namespace = "xin.ctkqiang.burpsuite.remote.mobileapp.feature.repeater"

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

dependencies {
    // 重放屏要拿远程客户端判断命令通路存不存在，因此它也依赖领域端口而不是传输实现。
    implementation(project(":domain"))
    implementation(project(":ui"))

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    // 各屏的排版（Column/Row/下拉刷新等等）全部来自 foundation，而版本目录里还没有它的别名，
    // 因此这里写字面量坐标，版本仍由 BOM 定。补上别名后应换成 libs.compose.foundation。
    implementation("androidx.compose.foundation:foundation")
    // @Preview 注解来自 tooling-preview，渲染器只在 debug 变体里进包（rules.md §8.3 要求每屏两套预览）。
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
}
