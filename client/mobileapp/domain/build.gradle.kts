plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktlint)
}

// 纯 JVM 模块，不是 Android 模块：领域层碰不到 Android 的一切，这条约束靠模块类型强制，
// 而不是靠约定——以后有人在这里 import android.* 会直接编不过。
kotlin {
    jvmToolchain(17)
}

// rules.md §2 指定 JUnit 5，不用 JUnit 4。
tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

dependencies {
    // 用 api：领域模型公开的属性带着身份值类型，藏起来会让使用方拿到模型却读不到身份值。
    api(project(":core:model"))

    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.mockk)
    testRuntimeOnly(libs.junit.platform.launcher)
}
