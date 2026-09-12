// Burp Remote —— 插件侧 Gradle 构建的根设置文件。
//
// 本文件回答三个问题：插件侧包含哪些子工程、构建插件与运行期依赖从哪里下载、
// 以及单个子工程是否允许私自声明依赖仓库。最后一个问题的答案是不允许：
// 依赖来源必须集中声明，否则构建结果无法复现，也无法审计。
//
// @author 钟智强

pluginManagement {
    // 构建插件统一从 Gradle 官方插件门户解析，缺失时回落到 Maven 中央仓库。
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    // 子工程私自声明仓库会让同一份代码在不同机器上解析出不同产物，因此直接禁止。
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        mavenCentral()
    }
}

// 构建名称同时出现在命令行提示与诊断日志中，因此不包含空格。
rootProject.name = "burp-remote-plugins"

// Burp Suite 扩展模块：产物是一个可被 Burp 直接加载的扩展 JAR。
include("burp-remote-extension")
