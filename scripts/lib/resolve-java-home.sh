# Burp Remote —— 构建脚本共用的 JDK 解析。
#
# 抽出来是因为插件与客户端都要求 JDK 17，两处各写一份「在常见路径里找」的逻辑，迟早有一处
# 忘了更新，而症状是「另一个产品的构建悄悄用了错的 JDK」。
#
# 本文件只放函数，不放顶层变量：调用方各自定义需要的主版本号，避免 readonly 变量互相覆盖。

# 按需要的主版本号找 JDK 根目录，找到就输出路径，找不到返回非零。
locateJavaHomeForVersion() {
    local required_version="$1"

    # 环境变量优先：CI 与装了多个 JDK 的机器可以显式指定，不必依赖这里的猜测顺序。
    if [[ -n "${JAVA_HOME_FOR_BUILD:-}" && -x "${JAVA_HOME_FOR_BUILD}/bin/java" ]]; then
        printf '%s\n' "${JAVA_HOME_FOR_BUILD}"
        return 0
    fi

    local candidate
    for candidate in \
        "/Library/Java/JavaVirtualMachines/temurin-${required_version}.jdk/Contents/Home" \
        "/Library/Java/JavaVirtualMachines"/*"${required_version}"*".jdk/Contents/Home" \
        "/usr/lib/jvm/temurin-${required_version}-jdk-amd64" \
        "/usr/lib/jvm/java-${required_version}-openjdk-amd64" \
        "/usr/lib/jvm/java-${required_version}-openjdk" \
        "${JAVA_HOME:-}"; do
        if [[ -n "${candidate}" && -x "${candidate}/bin/java" ]]; then
            printf '%s\n' "${candidate}"
            return 0
        fi
    done

    return 1
}

# 读指定 JDK 对外声明的主版本号，用来确认它真的是 17，而不是名字里带 17。
javaSpecificationVersionOf() {
    "${1}/bin/java" -XshowSettings:properties -version 2>&1 \
        | sed -n 's/^ *java\.specification\.version = //p' \
        | head -n 1
}
