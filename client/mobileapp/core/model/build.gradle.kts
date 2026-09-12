plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktlint)
}

// 纯 JVM 且不启用 kotlinx.serialization 插件：身份类型一旦贴上 @Serializable，
// 领域层就通过注解依赖了序列化运行库（rules.md §6.1），所以序列化器全部住在 :core:protocol。
kotlin {
    jvmToolchain(17)
}

// rules.md §2 指定 JUnit 5，不用 JUnit 4。
tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

dependencies {
    // 本模块只有值类型，没有任何运行期依赖：这里一旦出现非测试依赖，就说明身份类型开始兼管别的事了。
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.mockk)
    testRuntimeOnly(libs.junit.platform.launcher)
}
