pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

// AndroidX 与 AGP 只在 Google 仓库有，Maven Central 上找不到，两个都得列。
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "burpsuite-remote-mobileapp"

include(":app")
include(":domain")
include(":data")
include(":ui")
include(":feature:settings")
