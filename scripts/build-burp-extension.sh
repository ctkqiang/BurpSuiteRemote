#!/usr/bin/env bash
#
# 把 plugins/ 下的 Kotlin 扩展打成 Burp Suite 能直接加载的 JAR，结束时打印绝对路径。
# 必须用 JDK 17：Burp Suite 2025.x 跑在 17 上，更高版本的字节码会被拒加载，而报错要等
# 用户点开扩展列表时才冒出；JAVA_HOME_FOR_BUILD 可指定 JDK 根目录，附加参数透传给 Gradle。

set -euo pipefail

readonly REQUIRED_JAVA_SPECIFICATION_VERSION="17"
readonly SCRIPT_FILE_NAME="$(basename "${BASH_SOURCE[0]}")"
readonly SCRIPTS_DIRECTORY="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly REPOSITORY_ROOT_DIRECTORY="$(dirname "${SCRIPTS_DIRECTORY}")"
readonly GRADLE_PROJECT_DIRECTORY="${REPOSITORY_ROOT_DIRECTORY}/plugins"

# 产物目录被 plugins/build.gradle.kts 重定向过，路径得跟着它走，不然只会报「找不到产物」
readonly EXTENSION_ARTIFACT_DIRECTORY="${REPOSITORY_ROOT_DIRECTORY}/build/plugins/burp-remote-extension/libs"

# 优先认 JAVA_HOME_FOR_BUILD，CI 或多 JDK 机器能显式指定；下面那串路径只是兜底猜测
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

# 看 JDK 自报的主版本号，免得目录名带 17、实际却不是 17
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

# 布局被改过就直接失败，别留给 Gradle 去报「找不到项目」这种模糊错误
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

# 带 -plain 的是不含依赖的瘦 JAR，加载会缺类，过滤掉
printf '\n可加载的扩展 JAR：\n'
find "${EXTENSION_ARTIFACT_DIRECTORY}" -maxdepth 1 -type f -name '*.jar' ! -name '*-plain.jar' -print

printf '\n在 Burp Suite 中通过 Extensions → Installed → Add → Java 选择上述文件即可加载。\n'
