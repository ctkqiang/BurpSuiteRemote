plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ktlint)
}

// Android library 而不是纯 JVM 模块：技术日志的落地实现要用 android.util.Log 才能进 logcat，
// 纯 JVM 模块里连 logcat 都够不着，接口就成了一张永远没有实现者的空契约。
android {
    namespace = "xin.ctkqiang.burpsuite.remote.mobileapp.core.logging"

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

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

dependencies {
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.kotlinx.coroutines.test)
    testRuntimeOnly(libs.junit.platform.launcher)
}
