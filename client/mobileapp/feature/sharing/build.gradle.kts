plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    // 导出清单是线上格式，按 rules.md §7.6 用 kotlinx.serialization，不手拼 JSON。
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktlint)
}

android {
    namespace = "xin.ctkqiang.burpsuite.remote.mobileapp.feature.sharing"

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

// rules.md §2 指定 JUnit 5：脱敏是安全相关的纯逻辑，必须有测试盯着。
tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

dependencies {
    // 导出要读归档投影，因此依赖领域端口；导出文件写在自己的缓存目录里，不碰归档本体（rules.md §12）。
    implementation(project(":domain"))
    implementation(project(":ui"))

    // FileProvider 与文件共享都在 androidx.core 里。
    implementation(libs.androidx.core.ktx)
    // CreateDocument 契约：导出位置由用户在系统文件选择器里指定。
    implementation(libs.androidx.activity.compose)
    implementation(libs.kotlinx.serialization.json)

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

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}
