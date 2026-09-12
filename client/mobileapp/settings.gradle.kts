pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

// AndroidX 与 AGP 只在 Google 仓库有，Maven Central 上找不到，两个都得列。
// ML Kit 的条码识别也只发在 Google 仓库。
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "burpsuite-remote-mobileapp"

include(":app")
include(":core:common")
include(":core:model")
include(":core:protocol")
include(":domain")
include(":data")
include(":ui")
include(":feature:settings")
include(":feature:dashboard")
include(":feature:history")
include(":feature:connection")
include(":feature:intercept")
include(":feature:repeater")
include(":feature:archive")
include(":feature:screenshot")
include(":feature:sharing")
