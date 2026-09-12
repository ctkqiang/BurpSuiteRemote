// Burp Remote —— 插件侧构建的根工程脚本。
//
// 根工程不产出任何构件。它唯一的职责是把构建插件的版本锁定在
// gradle/libs.versions.toml 一处，并让子工程通过别名引用，
// 从而杜绝「每个模块各自写死一个版本号」这种不可审计的写法。
//
// @author 钟智强

plugins {
    // 全部以 apply false 声明：这里只负责把版本解析出来，真正应用交给子工程。
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.shadow) apply false
}

// 仓库根目录，即 plugins/ 的上一级。两种产品的构建产物都收敛到这一级下的 build/，
// 而不是散落在各自的模块目录里，这样「产物在哪」永远只有一个答案。
val repositoryBuildDirectory = rootProject.projectDir.parentFile.resolve("build")

allprojects {
    // 把每个模块的构建目录整体重定向到 <仓库根>/build/plugins/<模块名>。
    //
    // 之所以不采用「构建完再把最终构件复制过去」：复制只能搬走最后那一个文件，而
    // clean 依然会作用在旧位置、增量构建的中间产物也依然留在原地，结果是有两套
    // 目录都需要理解，反而更容易出错。
    //
    // 之所以再分一层「产品 / 模块」：Gradle 的中间产物按相对路径命名，两个模块一旦
    // 共用同一个构建目录就会互相覆盖类文件与报告，错误只在特定构建顺序下出现。
    layout.buildDirectory.set(repositoryBuildDirectory.resolve("plugins/${project.name}"))
}
