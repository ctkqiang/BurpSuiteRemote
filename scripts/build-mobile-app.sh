#!/usr/bin/env bash
#
# Burp Remote —— 移动端 debug APK 构建脚本。
#
# 与插件侧同一个思路：先把 JDK 17 找出来显式交给 Gradle 再编。Gradle 守护进程默认用机器上的
# java，而这台机器上默认是 25；让 AGP 跑在未经支持的 JDK 上，报错往往指向无关的地方。
#
# 用法：
#   scripts/build-mobile-app.sh                 # 编 debug APK
#   scripts/build-mobile-app.sh --no-daemon     # 附加参数透传给 Gradle
#   JAVA_HOME_FOR_BUILD=/path/to/jdk-17 scripts/build-mobile-app.sh
#
# @author 钟智强

set -euo pipefail

readonly SCRIPT_FILE_NAME="$(basename "${BASH_SOURCE[0]}")"
readonly SCRIPT_DIRECTORY="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly REPOSITORY_ROOT_DIRECTORY="$(dirname "${SCRIPT_DIRECTORY}")"
readonly GRADLE_PROJECT_DIRECTORY="${REPOSITORY_ROOT_DIRECTORY}/client/mobileapp"
readonly REQUIRED_JAVA_SPECIFICATION_VERSION="17"

# 产物目录由 client/mobileapp/build.gradle.kts 重定向到 <仓库根>/build/mobileapp/<模块名>。
readonly APP_ARTIFACT_DIRECTORY="${REPOSITORY_ROOT_DIRECTORY}/build/mobileapp/app/outputs/apk/debug"

source "${SCRIPT_DIRECTORY}/lib/resolve-java-home.sh"

resolved_java_home="$(locateJavaHomeForVersion "${REQUIRED_JAVA_SPECIFICATION_VERSION}" || true)"

if [[ -z "${resolved_java_home}" ]]; then
    printf '错误：未在本机找到 JDK %s。\n' "${REQUIRED_JAVA_SPECIFICATION_VERSION}" >&2
    printf '请先安装，再显式指定它的根目录：\n' >&2
    printf '  JAVA_HOME_FOR_BUILD=/path/to/jdk-%s %s\n' \
        "${REQUIRED_JAVA_SPECIFICATION_VERSION}" "${SCRIPT_FILE_NAME}" >&2
    exit 1
fi

export JAVA_HOME="${resolved_java_home}"

resolved_java_specification_version="$(javaSpecificationVersionOf "${JAVA_HOME}")"

if [[ "${resolved_java_specification_version}" != "${REQUIRED_JAVA_SPECIFICATION_VERSION}" ]]; then
    printf '错误：%s 实际声明的主版本号是 %s，本仓库要求 %s。\n' \
        "${JAVA_HOME}" "${resolved_java_specification_version:-未知}" \
        "${REQUIRED_JAVA_SPECIFICATION_VERSION}" >&2
    exit 1
fi

printf '使用 JDK %s：%s\n' "${resolved_java_specification_version}" "${JAVA_HOME}"

if [[ ! -x "${GRADLE_PROJECT_DIRECTORY}/gradlew" ]]; then
    printf '错误：未找到 Gradle 包装器 %s/gradlew。\n' "${GRADLE_PROJECT_DIRECTORY}" >&2
    exit 1
fi

printf '开始编译移动端 debug APK……\n'

"${GRADLE_PROJECT_DIRECTORY}/gradlew" \
    --project-dir "${GRADLE_PROJECT_DIRECTORY}" \
    --console=plain \
    assembleDebug \
    "$@"

if [[ ! -d "${APP_ARTIFACT_DIRECTORY}" ]]; then
    printf '错误：编译结束但 APK 目录不存在：%s\n' "${APP_ARTIFACT_DIRECTORY}" >&2
    exit 1
fi

printf '\nAPK：\n'
find "${APP_ARTIFACT_DIRECTORY}" -maxdepth 1 -type f -name '*.apk' -print

printf '\n安装到已连接设备：adb install -r <上面的 APK 路径>\n'
