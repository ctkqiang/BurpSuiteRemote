#!/usr/bin/env bash
#
# Burp Remote —— 扩展二进制产出脚本。
#
# 职责只有一件事：把 plugins/ 下的 Kotlin 扩展打包成一个可被 Burp Suite 直接加载
# 的 JAR，并在结束后打印它的绝对路径。
#
# 为什么不让人直接敲 ./gradlew packageExtension：
#
#   本仓库把工具链固定为 JDK 17（见规则第 2 节）。Burp Suite 2025.x 自身运行在 17
#   上，用更高版本编译出的字节码会因为类文件版本过高而被拒绝加载，而报错发生在
#   「用户点开扩展列表」的那一刻，离构建现场已经太远。开发者机器上默认的 java 往往
#   是 21 或 25，JAVA_HOME 甚至可能指向更旧的版本，于是本脚本负责把 JDK 17 找出来
#   并显式交给 Gradle，让「编译用哪个 JDK」不再取决于谁先装了什么。
#
# 质量闸门（ktlint、detekt、单元测试）由 Gradle 侧的 packageExtension 任务负责串联，
# 本脚本不重复实现，只负责环境解析与调用。
#
# 用法：
#   scripts/build-burp-extension.sh              # 走完整闸门后打包
#   scripts/build-burp-extension.sh --no-daemon  # 附加参数原样透传给 Gradle
#   JAVA_HOME_FOR_BUILD=/path/to/jdk-17 scripts/build-burp-extension.sh
#
# @author 钟智强

set -euo pipefail

readonly REQUIRED_JAVA_SPECIFICATION_VERSION="17"
readonly SCRIPT_FILE_NAME="$(basename "${BASH_SOURCE[0]}")"
readonly SCRIPTS_DIRECTORY="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly REPOSITORY_ROOT_DIRECTORY="$(dirname "${SCRIPTS_DIRECTORY}")"
readonly GRADLE_PROJECT_DIRECTORY="${REPOSITORY_ROOT_DIRECTORY}/plugins"

# 产物目录由 plugins/build.gradle.kts 统一重定向到 <仓库根>/build/plugins/<模块名>，
# 因此这里必须跟着走同一条约定；两处一旦不一致，脚本会报「产物目录不存在」，
# 而实际上只是找错了地方。
readonly EXTENSION_ARTIFACT_DIRECTORY="${REPOSITORY_ROOT_DIRECTORY}/build/plugins/burp-remote-extension/libs"

# 在常见安装位置中查找 JDK 17，找到则输出其根目录，否则返回非零退出码。
#
# 优先使用环境变量 JAVA_HOME_FOR_BUILD：CI 与装了多个 JDK 的机器可以显式指定，
# 而不必依赖本脚本的猜测顺序。猜测只是便利，永远不是权威。
locateJavaHomeForRequiredVersion() {
    if [[ -n "${JAVA_HOME_FOR_BUILD:-}" && -x "${JAVA_HOME_FOR_BUILD}/bin/java" ]]; then
        printf '%s\n' "${JAVA_HOME_FOR_BUILD}"
        return 0
    fi

    local candidate
    for candidate in \
        "/Library/Java/JavaVirtualMachines/temurin-${REQUIRED_JAVA_SPECIFICATION_VERSION}.jdk/Contents/Home" \
        "/Library/Java/JavaVirtualMachines"/*"${REQUIRED_JAVA_SPECIFICATION_VERSION}"*".jdk/Contents/Home" \
        "/usr/lib/jvm/temurin-${REQUIRED_JAVA_SPECIFICATION_VERSION}-jdk-amd64" \
        "/usr/lib/jvm/java-${REQUIRED_JAVA_SPECIFICATION_VERSION}-openjdk-amd64" \
        "/usr/lib/jvm/java-${REQUIRED_JAVA_SPECIFICATION_VERSION}-openjdk" \
        "${JAVA_HOME:-}"; do
        if [[ -n "${candidate}" && -x "${candidate}/bin/java" ]]; then
            printf '%s\n' "${candidate}"
            return 0
        fi
    done

    return 1
}

# 读取指定 JDK 根目录对外声明的主版本号，用于确认它真的是 17 而不是名字里带 17。
javaSpecificationVersionOf() {
    "${1}/bin/java" -XshowSettings:properties -version 2>&1 \
        | sed -n 's/^ *java\.specification\.version = //p' \
        | head -n 1
}

resolvedJavaHome="$(locateJavaHomeForRequiredVersion || true)"

if [[ -z "${resolvedJavaHome}" ]]; then
    printf '错误：未在本机找到 JDK %s。\n' "${REQUIRED_JAVA_SPECIFICATION_VERSION}" >&2
    printf '请先安装 JDK %s，再通过环境变量显式指定它的根目录：\n' \
        "${REQUIRED_JAVA_SPECIFICATION_VERSION}" >&2
    printf '  JAVA_HOME_FOR_BUILD=/path/to/jdk-%s %s\n' \
        "${REQUIRED_JAVA_SPECIFICATION_VERSION}" "${SCRIPT_FILE_NAME}" >&2
    exit 1
fi

export JAVA_HOME="${resolvedJavaHome}"

resolvedJavaSpecificationVersion="$(javaSpecificationVersionOf "${JAVA_HOME}")"

if [[ "${resolvedJavaSpecificationVersion}" != "${REQUIRED_JAVA_SPECIFICATION_VERSION}" ]]; then
    printf '错误：%s 实际声明的主版本号为 %s，而本仓库要求 %s。\n' \
        "${JAVA_HOME}" "${resolvedJavaSpecificationVersion:-未知}" \
        "${REQUIRED_JAVA_SPECIFICATION_VERSION}" >&2
    printf '用错误的 JDK 编译出的扩展会被 Burp Suite 拒绝加载，因此这里直接终止。\n' >&2
    exit 1
fi

printf '使用 JDK %s：%s\n' "${resolvedJavaSpecificationVersion}" "${JAVA_HOME}"

# 校验通过后目录必然存在；若不存在，说明仓库布局被改动过，应当立刻失败而不是
# 留下一个「Gradle 说找不到项目」的模糊错误。
if [[ ! -x "${GRADLE_PROJECT_DIRECTORY}/gradlew" ]]; then
    printf '错误：未找到 Gradle 包装器 %s/gradlew。\n' "${GRADLE_PROJECT_DIRECTORY}" >&2
    exit 1
fi

printf '开始构建扩展 JAR（质量闸门通过后才会产出构件）……\n'

"${GRADLE_PROJECT_DIRECTORY}/gradlew" \
    --project-dir "${GRADLE_PROJECT_DIRECTORY}" \
    --console=plain \
    packageExtension \
    "$@"

if [[ ! -d "${EXTENSION_ARTIFACT_DIRECTORY}" ]]; then
    printf '错误：构建结束但产物目录不存在：%s\n' "${EXTENSION_ARTIFACT_DIRECTORY}" >&2
    exit 1
fi

# 只列可加载的那一份：带 -plain 后缀的是不含依赖的瘦 JAR，直接加载它会因缺类而失败。
printf '\n可加载的扩展 JAR：\n'
find "${EXTENSION_ARTIFACT_DIRECTORY}" -maxdepth 1 -type f -name '*.jar' ! -name '*-plain.jar' -print

printf '\n在 Burp Suite 中通过 Extensions → Installed → Add → Java 选择上述文件即可加载。\n'
