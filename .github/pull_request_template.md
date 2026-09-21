<!--
Thanks for contributing to Burp Remote.
Keep one logical change per PR; see CONTRIBUTING.md for the full rules.
-->

## English

### What does this PR do?

<!-- One or two sentences. What changes, and why. -->

### Related issue

<!-- e.g. Closes #123 -->

### Scope of the change

<!-- Tick the parts this PR touches. -->

- [ ] `plugins` — Burp Suite extension (engine)
- [ ] `client` — Android app (remote)
- [ ] `protocol` — wire protocol
- [ ] `docs` — documentation or the project site

### How was this tested?

- [ ] `plugins/gradlew --project-dir plugins test`
- [ ] `client/mobileapp/gradlew --project-dir client/mobileapp test`
- [ ] `scripts/build-burp-extension.sh` produces a loadable JAR
- [ ] `scripts/build-mobile-app.sh` produces an APK
- [ ] Manually verified on a device / Burp Suite (describe below)

### Checklist

- [ ] I have read [`.trae/rules.md`](.trae/rules.md) and
      [`.trae/plan.md`](.trae/plan.md)
- [ ] Static gates pass (`ktlintCheck`, `detekt`)
- [ ] Commits follow Conventional Commits with a scope of
      `plugins` | `client` | `protocol` | `docs` | `build`
- [ ] One logical change; no reformatting mixed with behaviour changes
- [ ] No build outputs, keystores, `keystore.properties`, or local config added

### Screenshots / logs (if applicable)

<!-- Don't paste pairing codes, request bodies, or other sensitive data. -->

---

## 中文

### 这个 PR 做了什么？

<!-- 一两句话：改了什么，为什么改。 -->

### 关联 Issue

<!-- 例如：Closes #123 -->

### 改动范围

<!-- 勾选本 PR 触及的部分。 -->

- [ ] `plugins` —— Burp Suite 扩展（引擎）
- [ ] `client` —— Android 应用（远程端）
- [ ] `protocol` —— 通信协议
- [ ] `docs` —— 文档或项目站点

### 如何验证的？

- [ ] `plugins/gradlew --project-dir plugins test`
- [ ] `client/mobileapp/gradlew --project-dir client/mobileapp test`
- [ ] `scripts/build-burp-extension.sh` 产出的 JAR 可加载
- [ ] `scripts/build-mobile-app.sh` 产出了 APK
- [ ] 在真机 / Burp Suite 上手动验证过（在下方说明）

### 自查清单

- [ ] 已读 [`.trae/rules.md`](.trae/rules.md) 与 [`.trae/plan.md`](.trae/plan.md)
- [ ] 静态闸门通过（`ktlintCheck`、`detekt`）
- [ ] 提交信息遵循 Conventional Commits，scope 为
      `plugins` | `client` | `protocol` | `docs` | `build`
- [ ] 一个逻辑改动；没有把格式化改动与行为改动混在一起
- [ ] 没有加入构建产物、密钥库、`keystore.properties` 或本地配置

### 截图 / 日志（如适用）

<!-- 不要贴配对码、请求体这类敏感信息。 -->
