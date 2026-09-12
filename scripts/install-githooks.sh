#!/usr/bin/env bash
#
# Burp Remote —— Git 钩子安装脚本。
#
# 把仓库内的 .githooks/ 复制到 .git/hooks/ 并赋予可执行权限。
#
# 为什么不改用 core.hooksPath 指向 .githooks/：那需要改写本机的 git 配置，而钩子本身
# 并不需要配置就能生效。复制只有一个来源（.githooks/ 受版本控制），安装动作幂等，
# 也不会在用户不知情时改动他的 git 环境。代价是钩子更新后需要重新执行一次本脚本，
# 这一点写在下面的提示里。
#
# 用法：
#   scripts/install-githooks.sh
#
# @author 钟智强

set -euo pipefail

readonly SCRIPT_DIRECTORY="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly REPOSITORY_ROOT_DIRECTORY="$(dirname "${SCRIPT_DIRECTORY}")"
readonly HOOK_SOURCE_DIRECTORY="${REPOSITORY_ROOT_DIRECTORY}/.githooks"
readonly HOOK_TARGET_DIRECTORY="${REPOSITORY_ROOT_DIRECTORY}/.git/hooks"

if [[ ! -d "${HOOK_SOURCE_DIRECTORY}" ]]; then
    printf '错误：未找到钩子源目录 %s。\n' "${HOOK_SOURCE_DIRECTORY}" >&2
    exit 1
fi

if [[ ! -d "${HOOK_TARGET_DIRECTORY}" ]]; then
    printf '错误：%s 不存在，这个目录不是 Git 仓库。\n' "${HOOK_TARGET_DIRECTORY}" >&2
    exit 1
fi

installed_hook_count=0

for hook_source_path in "${HOOK_SOURCE_DIRECTORY}"/*; do
    if [[ ! -f "${hook_source_path}" ]]; then
        continue
    fi

    hook_file_name="$(basename "${hook_source_path}")"
    install -m 0755 "${hook_source_path}" "${HOOK_TARGET_DIRECTORY}/${hook_file_name}"
    printf '已安装钩子：%s\n' "${hook_file_name}"
    installed_hook_count=$((installed_hook_count + 1))
done

if (( installed_hook_count == 0 )); then
    printf '错误：%s 下没有任何可安装的钩子。\n' "${HOOK_SOURCE_DIRECTORY}" >&2
    exit 1
fi

printf '共安装 %d 个钩子。钩子源文件改动后，请重新执行本脚本。\n' "${installed_hook_count}"
