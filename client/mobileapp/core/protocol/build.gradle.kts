plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktlint)
}

// 纯 JVM 模块，不是 Android 模块：协议层要能在不启模拟器的前提下被直接测试，
// 一旦这里能 import android.*，这种可测性就会慢慢消失。
kotlin {
    jvmToolchain(17)
}

// rules.md §2 指定 JUnit 5，不用 JUnit 4。
tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

dependencies {
    // 用 api 而不是 implementation：DTO 的公开属性带着 model 的值类型，
    // 藏起来会让使用方拿到 DTO 却读不到身份值。
    api(project(":core:model"))

    // 线上编解码唯一的实现（rules.md §7.6）。
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.mockk)
    testRuntimeOnly(libs.junit.platform.launcher)
}
