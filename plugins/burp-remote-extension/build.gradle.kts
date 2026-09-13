// Burp Remote —— 「Burp 扩展」模块的构建脚本。
//
// 本模块产出唯一一件东西：一个可被 Burp Suite 直接加载的扩展 JAR。
// 构建脚本因此只回答四件事——用什么语言编译、依赖从哪来、质量闸门是什么、
// 以及最终产物如何组装。
//
// @author 钟智强

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
    alias(libs.plugins.shadow)
}

// group 与 version 一起决定产物文件名，也决定诊断信息里出现的那串坐标。
group = "xin.ctkqiang.burpsuite.remote"
version = "0.1.0"

kotlin {
    // 工具链固定为 JDK 17：Burp Suite 2025.x 自身运行在 17 上，
    // 用更高版本编译出的字节码会因为类文件版本过高而无法被 Burp 加载。
    //
    // 这里使用「工具链」而不是「跟随当前 JVM」，是为了让本机装了 JDK 21、25 的
    // 开发者与 CI 得到完全一致的字节码，而不是「我这能跑，你那报错」。
    jvmToolchain(17)

    compilerOptions {
        // 警告即错误。Kotlin 的警告绝大多数指向真实的逻辑缺陷
        // （未使用的参数、平台类型空安全、弃用 API），放任不管等于默许技术债累积。
        allWarningsAsErrors.set(true)
    }
}

dependencies {
    // Montoya 是 Burp 在运行期提供的扩展 API，因此只参与编译、不参与打包。
    // 一旦误用 implementation 引入，打进 JAR 的那份类会在 Burp 加载时
    // 与它自身的副本冲突，表现为难以定位的 NoSuchMethodError。
    compileOnly(libs.montoya.api)

    // 协程：结构化并发与 Flow 的实现基础，事件发布与连接管理都依赖它。
    implementation(libs.kotlinx.coroutines.core)

    // 序列化：协议报文与事件载荷唯一的序列化方案（对应 rules.md §7.6）。
    implementation(libs.kotlinx.serialization.json)

    // 传输层：本机网络被视作不可信，因此使用经过大规模验证的 HTTP 与
    // WebSocket 实现，而不是自行解析报文（对应 rules.md §12）。
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.cio)
    implementation(libs.ktor.server.websockets)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)

    // 二维码编码：配对票据必须以图形方式呈现给移动端扫描（对应 plan §55）。
    // 只取 zxing-core，它没有传递依赖，打包后不会引入与二维码无关的类。
    implementation(libs.zxing.core)

    // 测试技术栈。
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.mockk)

    // ktor-server-test-host 让传输层可以在不起真实端口的前提下被端到端测试，
    // 这正是 rules.md §13 要求的「不依赖运行中的 Burp 也能测试」。
    testImplementation(libs.ktor.server.test.host)

    // 测试代码同样需要 Montoya 类型来构造适配器的假实现。
    testCompileOnly(libs.montoya.api)
}

ktlint {
    filter {
        // 构建产物目录包含生成的源码与复制过来的资源，不属于人工维护范围。
        exclude { it.file.path.contains("/build/") }
    }
}

// 端到端夹具的源码集。
//
// 单独一个源码集而不是塞进 main：夹具带 main()，混进扩展 JAR 会往 Burp 里多塞一个入口类；
// 它复用 main 的产物与依赖，所以用的仍是真实的传输层与安全层。
val harnessSourceSet =
    sourceSets.create("harness") {
        compileClasspath += sourceSets.main.get().output
        runtimeClasspath += sourceSets.main.get().output
    }

configurations[harnessSourceSet.implementationConfigurationName].extendsFrom(configurations.implementation.get())
configurations[harnessSourceSet.runtimeOnlyConfigurationName].extendsFrom(configurations.runtimeOnly.get())
// Montoya 只参与编译：夹具要在没有 Burp 的 JVM 里跑起来，运行期不能要求它存在。
configurations[harnessSourceSet.compileOnlyConfigurationName].extendsFrom(configurations.compileOnly.get())

// 在 JVM 里起真实服务端，供移动端跑端到端链路：它不依赖 Burp，因此能独立证明「扫到的票据真能连上」。
val remoteServerHarness by tasks.registering(JavaExec::class) {
    group = "verification"
    description = "启动真实的远程控制服务端夹具，用于移动端端到端联调。"

    dependsOn(tasks.named(harnessSourceSet.classesTaskName))
    classpath = harnessSourceSet.runtimeClasspath
    mainClass.set("xin.ctkqiang.burpsuite.remote.harness.RemoteServerHarnessKt")
}

detekt {
    // 不叠加默认配置：配置以仓库内的 detekt.yml 为唯一事实来源，
    // 避免「默认配置悄悄打开某条规则」这种不可见的行为差异。
    buildUponDefaultConfig = false
    allRules = false
    parallel = true

    // basePath 让报告里的文件路径变成相对路径，本机路径不会泄露进 CI 日志。
    basePath = rootProject.projectDir.absolutePath

    config.setFrom(rootProject.layout.projectDirectory.file("detekt.yml"))
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    // 与 kotlin.jvmToolchain 保持一致，否则静态分析会按错误的语言级别解析代码。
    jvmTarget = "17"

    reports {
        html.required.set(true)
        xml.required.set(true)
        txt.required.set(false)
        sarif.required.set(false)
        md.required.set(false)
    }
}

tasks.named<io.gitlab.arturbosch.detekt.Detekt>("detekt") {
    // 静态分析的作用域被明确划在生产源码上。
    //
    // 测试函数按 rules.md §3.3 使用反引号可读句命名（`forwarding an intercept emits ...`），
    // 这与 detekt 针对生产代码的命名规则天然冲突。与其在命名、文档等规则上逐个叠加豁免
    // ——那会让规则清单逐渐失去意义——不如把边界说清楚：测试代码仍受「编译警告即错误」
    // 与 ktlint 的约束，而命名规范只对生产代码生效。
    setSource(files("src/main/kotlin"))
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()

    // 测试失败时立刻失败，不在后续测试上浪费时间。
    failFast = false

    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

tasks.named<Jar>("jar") {
    // 未打进依赖的「瘦 JAR」被显式加上 plain 后缀，
    // 以免它与下面那份可加载的完整 JAR 同名而互相覆盖。
    archiveClassifier.set("plain")
}

tasks.named<ShadowJar>("shadowJar") {
    // 产物名不带任何后缀杂质：Burp 的扩展列表里显示的就是它。
    archiveClassifier.set("")

    // Ktor 通过 META-INF/services 查找引擎实现，多个依赖各自携带同名服务文件。
    // 不合并就会丢失其中一份，表现为引擎在运行期找不到，而编译期毫无征兆。
    mergeServiceFiles()

    manifest {
        attributes(
            "Implementation-Title" to "Burp Remote Extension",
            "Implementation-Version" to project.version,
            "Implementation-Vendor" to "钟智强",
        )
    }
}

// 「产出可分发构件」的唯一入口。
//
// 之所以不让人直接调用 shadowJar，是因为那样会绕过质量闸门：闸门红着、JAR 却已经
// 生成，于是不合规的产物被拖进 Burp，问题被推迟到加载那一刻才暴露。此任务把
// 「验证」与「打包」绑成一个不可分割的动作，让「有产物」与「产物合格」同义。
val packageExtension by tasks.registering {
    group = "distribution"
    description = "运行全部质量闸门与单元测试后，产出可被 Burp Suite 直接加载的扩展 JAR。"

    dependsOn(tasks.named("ktlintCheck"))
    dependsOn(tasks.named("detekt"))
    dependsOn(tasks.named("test"))
    dependsOn(tasks.named("shadowJar"))
}

// 依赖关系只声明「都要执行」，从不声明先后。若不追加下面这行约束，Gradle 完全
// 可能先把 JAR 打出来、再让静态分析报错——产物已经落盘，失败已经无关紧要了。
tasks.named("shadowJar") {
    mustRunAfter(
        tasks.named("ktlintCheck"),
        tasks.named("detekt"),
        tasks.named("test"),
    )
}
