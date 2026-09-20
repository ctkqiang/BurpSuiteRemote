import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ktlint)
}

// 发布产物的命名契约：{项目名}{版本号}.{扩展名}，与插件侧的扩展 JAR 共用同一个项目名。
val releaseProductName = "BurpRemote"

// 版本号只在这里写一次：defaultConfig 与发布产物名都读它，
// 杜绝「产物名写着 0.1.0、包里装的其实是 0.2.0」这种漂移。
val applicationVersionName = "0.1.0"
val applicationVersionCode = 1

// 签名材料从不进仓库：CI 在构建前把它落成 keystore.properties，本机开发者可自备一份。
// 文件缺失时不在这里报错——配置阶段的硬失败会让 IDE 同步、lint、debug 构建统统连坐。
// 真正的拦截放在发布任务里：只有要出「可发布的包」时才要求密钥库存在。
val releaseKeystorePropertiesFile = rootProject.file("keystore.properties")
val releaseKeystoreProperties =
    Properties().apply {
        if (releaseKeystorePropertiesFile.isFile) {
            releaseKeystorePropertiesFile.inputStream().use(::load)
        }
    }
val hasReleaseSigningMaterial = releaseKeystorePropertiesFile.isFile

android {
    namespace = "xin.ctkqiang.burpsuite.remote.mobileapp"

    compileSdk = 36

    defaultConfig {
        applicationId = "xin.ctkqiang.burpsuite.remote.mobileapp"
        minSdk = 26
        targetSdk = 36
        versionCode = applicationVersionCode
        versionName = applicationVersionName
    }

    signingConfigs {
        // 只有拿到签名材料才登记配置。空路径的 SigningConfig 会在每次 sync 时
        // 向 AGP 暴露一个并不存在的密钥库，属于凭空制造噪音。
        if (hasReleaseSigningMaterial) {
            create("release") {
                storeFile = rootProject.file(releaseKeystoreProperties.getProperty("storeFile"))
                storePassword = releaseKeystoreProperties.getProperty("storePassword")
                keyAlias = releaseKeystoreProperties.getProperty("keyAlias")
                keyPassword = releaseKeystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // 拿不到密钥库时这里是 null，产物会退化成 app-release-unsigned.apk。
            // 这种包无法上架，因此发布任务会明确拒绝它，而不是让它混进 Release 页。
            signingConfig = signingConfigs.findByName("release")
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    jvmToolchain(17)
}

// 发布产物的唯一出口：签名 APK + AAB，并按 {项目名}{版本号}.{扩展名} 归集到 outputs/dist。
//
// 为什么不直接改 AGP 的默认产物名：AGP 8.13 的公开 Variant API 已不再暴露 outputFileName
// （VariantOutput 上没有这个属性），能改名的只剩内部实现或已弃用的 applicationVariants，
// 那等于把构建脚本焊死在某个 AGP 版本上。这里改成构建后聚合：命名与执行顺序只用公开 API，
// AGP 升级不会牵连，本机跑与 CI 跑得到完全一致的产物名。
val releaseDistributionDirectory = layout.buildDirectory.dir("outputs/dist")

val packageReleaseDistribution by tasks.registering(Sync::class) {
    group = "distribution"
    description = "用公钥签名后产出 APK 与 AAB，并按 {项目名}{版本号}.{扩展名} 归集到 outputs/dist。"

    dependsOn("assembleRelease")
    dependsOn("bundleRelease")

    into(releaseDistributionDirectory)

    // 用正则整名替换而不是拼前缀：AGP 会在名字里带出 flavor、build type、AAB 的 signing 标记，
    // 逐个拼前缀迟早漏掉一种；直接锚定扩展名，产出什么就改叫什么。
    from(layout.buildDirectory.dir("outputs/apk/release")) {
        include("*.apk")
        rename(".*\\.apk", "$releaseProductName$applicationVersionName.apk")
    }
    from(layout.buildDirectory.dir("outputs/bundle/release")) {
        include("*.aab")
        rename(".*\\.aab", "$releaseProductName$applicationVersionName.aab")
    }

    doFirst {
        // 发布渠道只接受签名产物，未签名包在这里被拦下，失败信息直接指向缺失的那份配置。
        check(hasReleaseSigningMaterial) {
            "发布产物必须签名，但未找到 ${releaseKeystorePropertiesFile.absolutePath}。" +
                "请在 client/mobileapp/ 下提供 storeFile / storePassword / keyAlias / keyPassword 四项配置。"
        }
    }
}

dependencies {
    implementation(project(":ui"))
    implementation(project(":data"))
    implementation(project(":domain"))
    // 装配层要亲手建 AndroidTechnicalLog 与事件摄入的进程级作用域，日志端口因此在这里可见。
    implementation(project(":core:common"))
    // 配对入口要在本地解二维码里的票据、校验协议版本，因此协议层也在这里可见。
    implementation(project(":core:protocol"))
    implementation(project(":feature:settings"))
    implementation(project(":feature:dashboard"))
    implementation(project(":feature:history"))
    implementation(project(":feature:connection"))
    implementation(project(":feature:intercept"))
    implementation(project(":feature:repeater"))
    implementation(project(":feature:sharing"))

    implementation(platform(libs.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    // NavHost 建在装配层：只有这里同时看得见各 feature 与导航模型（rules.md §6.1）。
    implementation(libs.androidx.navigation.compose)
    // 装配层持有传输引擎的实例与生命周期（进程退出时要把它关掉），因此需要它的类型在编译期可见。
    implementation(libs.ktor.client.core)
    // 装配层要在 RestRemoteRepeaterWriter 里手动构建 JSON 请求体（requestText + tabName），
    // 因此需要 kotlinx-serialization-json 的类型在编译期可见。
    implementation(libs.kotlinx.serialization.json)
    // 装配层要亲手建 Room 库（data 把 Room 藏在实现细节里，不对外暴露），因此这里显式声明它。
    implementation(libs.androidx.room.runtime)
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
}
