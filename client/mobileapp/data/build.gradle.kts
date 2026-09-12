plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    // 远程层要按线上契约声明报文 DTO，序列化器由编译期生成，因此需要这个插件。
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.ktlint)
}

android {
    namespace = "xin.ctkqiang.burpsuite.remote.mobileapp.data"

    compileSdk = 36

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    jvmToolchain(17)
}

// rules.md §2 指定 JUnit 5。安卓单元测试任务同样是 Test，因此这一条对它们一并生效。
tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

dependencies {
    implementation(project(":domain"))

    // 线上事件信封与机器码住在协议层，适配层只做「线上类型 → 领域类型」的翻译。
    implementation(project(":core:protocol"))
    // 传输、事件通道与事件摄入都要按 rules.md §12 记结构化技术日志，端口由 core:common 提供。
    implementation(project(":core:common"))

    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.datastore.preferences)

    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.websockets)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.json)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.mockk)

    // REST 的用例用 MockEngine 驱动，不必起真插件；WebSocket 那部分没有替身，改测抽出来的纯判定。
    testImplementation(libs.ktor.client.mock)
    testRuntimeOnly(libs.junit.platform.launcher)
}
