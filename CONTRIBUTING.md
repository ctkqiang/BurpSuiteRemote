# Contributing / 贡献指南

## English

Thanks for wanting to help. Burp Remote is a remote control for Burp Suite: the
Burp extension is the engine, and the Android app is a thin client. Please read
this before opening a pull request.

### Before you start

Read the two binding documents in the repository:

- [`.trae/rules.md`](.trae/rules.md) — naming, comments, event sourcing, and
  layering rules. When it disagrees with personal preference, **it wins**.
- [`.trae/plan.md`](.trae/plan.md) — the responsibility and design intent of each
  module.

### Requirements

| Item | Requirement |
|------|-------------|
| JDK | **17**. Burp Suite 2025.x rejects higher bytecode, and an extension built with another JDK fails to load |
| Gradle | Use the per-project wrapper (`plugins/gradlew`, `client/mobileapp/gradlew`); there is no root wrapper |

If JDK 17 is not your default, point the build at it explicitly:

```bash
JAVA_HOME_FOR_BUILD=/path/to/jdk-17 scripts/build-burp-extension.sh
```

### Project layout

| Path | What it is |
|------|------------|
| `plugins/` | Burp Suite extension — the engine. Owns the proxy, intercept, and the HTTP listener |
| `client/mobileapp/` | Android app — the remote. Multi-module: `core/protocol`, `data`, `domain`, `ui`, `feature/*` |
| `scripts/` | Build and tooling scripts |
| `docs/` | Project site and privacy policy (GitHub Pages) |

### Build

```bash
# Burp extension JAR
scripts/build-burp-extension.sh
# → build/plugins/burp-remote-extension/libs/*.jar  (load the non-*-plain.jar)

# Android debug APK
scripts/build-mobile-app.sh
# → build/mobileapp/app/outputs/apk/debug/*.apk
```

Both scripts refuse to run unless the resolved JDK's major version is exactly 17,
so a wrong toolchain fails loudly instead of producing a broken artifact.

### Tests

JVM unit tests live under `src/test/kotlin` in each project:

```bash
plugins/gradlew --project-dir plugins test
client/mobileapp/gradlew --project-dir client/mobileapp test
```

### Static gates

Formatting is a build gate, not a review comment. `ktlint` and `detekt` are
wired into the build:

```bash
plugins/gradlew --project-dir plugins ktlintCheck detekt
client/mobileapp/gradlew --project-dir client/mobileapp ktlintCheck
```

The bundled `pre-commit` hook runs the relevant gates automatically whenever
staged `.kt` or `.kts` files are present. Install the hooks once per clone:

```bash
scripts/install-githooks.sh
```

### Commit messages

Conventional Commits, enforced by `.githooks/commit-msg`:

```
<type>(<scope>): <subject>
```

- **types**: `feat` `fix` `docs` `style` `refactor` `perf` `test` `build` `ci`
  `chore` `revert`
- **scopes**: `plugins` `client` `protocol` `docs` `build`
- **subject**: at most **100 bytes** (a Chinese character is 3 bytes), no
  trailing `.` or `。`

Keep one logical change per commit — do not mix a reformat with a behaviour
change.

### Dependencies

Check the version catalog (`libs.versions.toml`) for something reusable before
adding a new dependency.

### Never commit

Build outputs, keystores, `keystore.properties`, or any local configuration.

### Pull requests

Use the [pull request template](.github/pull_request_template.md). State what
changed and why, link the issue, and confirm the static gates pass. Do not paste
pairing codes, request bodies, or other sensitive data.

---

## 中文

感谢你愿意帮忙。Burp Remote 是 Burp Suite 的远程控制端：Burp 扩展是引擎，Android 应用是瘦
客户端。开 Pull Request 之前请先读这份指南。

### 动手之前

先读仓库里两份绑定文档：

- [`.trae/rules.md`](.trae/rules.md) —— 命名、注释、事件溯源、分层依赖规则。它与个人偏好冲突
  时，**以它为准**
- [`.trae/plan.md`](.trae/plan.md) —— 各模块的职责划分与设计意图

### 环境要求

| 项目 | 要求 |
|------|------|
| JDK | **17**。Burp Suite 2025.x 拒绝更高版本的字节码，用其他 JDK 编出的扩展会加载失败 |
| Gradle | 用各子项目自带的包装器（`plugins/gradlew`、`client/mobileapp/gradlew`），仓库根目录没有包装器 |

如果机器默认不是 JDK 17，显式指给构建脚本：

```bash
JAVA_HOME_FOR_BUILD=/path/to/jdk-17 scripts/build-burp-extension.sh
```

### 目录结构

| 路径 | 是什么 |
|------|--------|
| `plugins/` | Burp Suite 扩展 —— 引擎。代理、拦截、HTTP 监听都在这里 |
| `client/mobileapp/` | Android 应用 —— 远程端。多模块：`core/protocol`、`data`、`domain`、`ui`、`feature/*` |
| `scripts/` | 构建与工具脚本 |
| `docs/` | 项目站点与隐私政策（GitHub Pages） |

### 构建

```bash
# Burp 扩展 JAR
scripts/build-burp-extension.sh
# → build/plugins/burp-remote-extension/libs/*.jar（加载不带 -plain 的那个）

# Android debug APK
scripts/build-mobile-app.sh
# → build/mobileapp/app/outputs/apk/debug/*.apk
```

两个脚本都会校验实际解析到的 JDK 主版本号必须恰好是 17，工具链不对就直接失败，而不是产出一个
装不上的构件。

### 测试

JVM 单元测试位于各项目的 `src/test/kotlin`：

```bash
plugins/gradlew --project-dir plugins test
client/mobileapp/gradlew --project-dir client/mobileapp test
```

### 静态闸门

格式不是评审意见，而是构建闸门。`ktlint` 与 `detekt` 已接进构建：

```bash
plugins/gradlew --project-dir plugins ktlintCheck detekt
client/mobileapp/gradlew --project-dir client/mobileapp ktlintCheck
```

仓库自带的 `pre-commit` 钩子在暂存区出现 `.kt` 或 `.kts` 时会自动跑对应闸门。每个克隆装一次：

```bash
scripts/install-githooks.sh
```

### 提交信息

Conventional Commits，由 `.githooks/commit-msg` 强制执行：

```
<type>(<scope>): <subject>
```

- **type**：`feat` `fix` `docs` `style` `refactor` `perf` `test` `build` `ci`
  `chore` `revert`
- **scope**：`plugins` `client` `protocol` `docs` `build`
- **subject**：不超过 **100 字节**（一个中文字符算 3 字节），不能以 `.` 或 `。` 收尾

一个逻辑改动一个提交 —— 不要把格式化改动和行为改动混在一起。

### 依赖

新增依赖前，先看版本目录 `libs.versions.toml` 里有没有可复用的。

### 不要提交

构建产物、密钥库、`keystore.properties`，或任何本地配置。

### Pull Request

请用 [PR 模板](.github/pull_request_template.md)。写清楚改了什么、为什么改，关联对应 Issue，
并确认静态闸门通过。不要贴配对码、请求体这类敏感信息。
