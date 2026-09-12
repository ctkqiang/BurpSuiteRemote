plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ktlint) apply false
}

// 产物统一收到 <仓库根>/build/mobileapp/<模块名>，与插件侧同一条约定。
val repositoryBuildDirectory = rootProject.projectDir.parentFile.parentFile.resolve("build")

subprojects {
    layout.buildDirectory.set(repositoryBuildDirectory.resolve("mobileapp/${project.name}"))
}
