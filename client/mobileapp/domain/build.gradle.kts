plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktlint)
}

// 纯 JVM 模块，不是 Android 模块：领域层碰不到 Android 的一切，这条约束靠模块类型强制，
// 而不是靠约定——以后有人在这里 import android.* 会直接编不过。
kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.junit)
}
