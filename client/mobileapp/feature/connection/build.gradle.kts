plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ktlint)
}

android {
    namespace = "xin.ctkqiang.burpsuite.remote.mobileapp.feature.connection"

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
    implementation(project(":domain"))
    implementation(project(":ui"))

    // 默认端口只在协议层写一次（rules.md §11），这一屏显示的就是那个值，不在这里写 9000。
    // 二维码文本到 PairingTicket 的解码也在协议层，扫码屏只负责把文本递过去。
    implementation(project(":core:protocol"))

    // rememberLauncherForActivityResult：相机是危险权限，必须在运行时申请。
    implementation(libs.androidx.activity.compose)
    // ContextCompat.getMainExecutor：CameraX 的回调要挂在主线程执行器上。
    implementation(libs.androidx.core.ktx)

    // 相机：core 是 API，camera2 是后端，lifecycle 让会话跟着界面走，view 提供 PreviewView。
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    // 只做二维码识别，不引整套 vision SDK。
    implementation(libs.mlkit.barcode.scanning)

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
