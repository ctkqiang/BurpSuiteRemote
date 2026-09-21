# Security Policy / 安全策略

## English

### Reporting a vulnerability

Please **do not open a public Issue** for security problems. Report privately via
either channel:

- Email: **johnmelodymel@qq.com**
- GitHub: **Security → Report a vulnerability** (private advisory)

Include the reproduction steps, the affected component (extension or Android
app), the version or commit, and the impact you believe it has. We aim to
acknowledge a report within a few days and will credit you in the advisory unless
you prefer otherwise.

### Supported versions

The project is pre-1.0. Security fixes target the latest release (currently
`0.1.0`) and the `main` branch. Older builds are not maintained.

### Threat model — read this before deploying

This tool gives a phone remote control over a running Burp Suite. **It is built
on the assumption that the local network is trusted.** Understand the following
before you file a report or expose a deployment:

- **Transport is plain HTTP with no TLS.** Traffic on the wire is readable by
  anyone who can observe the segment. This is a deliberate design choice for a
  trusted LAN, not an oversight.
- **Never expose port `9000` to the internet.** Do not port-forward it, do not
  put it behind a public reverse proxy, and do not use it on an untrusted
  network (public Wi-Fi, hotel networks, shared office VLANs).
- **Pairing is the authentication boundary.** Pairing codes are single-use and
  expire after **5 minutes**. Every connection carries a device identity, and
  devices that are not registered are refused.
- **Control operations are rate-limited and idempotent**, so a replayed or
  duplicated command does not apply twice.

### Out of scope

The following are properties of the design, not vulnerabilities:

- Anyone with LAN access to the plain-HTTP port being able to observe traffic
- Any consequence of port-forwarding `9000` or otherwise exposing the listener
- Use against systems you are not authorised to test

### Authorised use

This project is for **authorised** security testing only. Scanning, attacking,
or intercepting traffic on systems you do not have written permission to test is
illegal, and the user bears full legal responsibility — see the legal notice at
the top of [README.md](README.md).

### Data handling

Archived originals are kept byte-exact; redaction is applied only to exported
copies, so an export can never silently rewrite your evidence. Pairing codes and
request bodies are sensitive — do not paste them into public Issues or pull
requests.

---

## 中文

### 报告安全漏洞

发现安全问题请**不要开公开 Issue**。通过以下任一渠道私下报告：

- 邮件：**johnmelodymel@qq.com**
- GitHub：**Security → Report a vulnerability**（私密安全公告）

请附上复现步骤、受影响的组件（插件或 Android 应用）、版本号或 commit，以及你认为的影响范围。
我们会在几天内确认收到，并在公告中致谢（除非你希望匿名）。

### 支持版本

项目尚未发布 1.0。安全修复只针对最新版本（当前为 `0.1.0`）与 `main` 分支，历史构建不再维护。

### 威胁模型 —— 部署前请先读完

本工具让手机可以远程控制正在运行的 Burp Suite。**它的成立前提是局域网可信。** 在提单或暴露
部署之前，请先理解以下几点：

- **传输是明文 HTTP，没有 TLS。** 网线上能观察到该网段的人可以读到全部流量。这是面向可信
  局域网的**有意设计**，不是疏漏。
- **绝不要把 `9000` 端口暴露到公网。** 不要做端口映射，不要挂在公网反向代理后面，也不要在
  不可信网络（公共 Wi-Fi、酒店网络、共享办公 VLAN）里使用。
- **配对就是鉴权边界。** 配对码一次性有效、**5 分钟**过期；每个连接都携带设备身份，未登记的
  设备一律拒绝。
- **控制类操作带限流与幂等去重**，重放或重复的命令不会被执行两次。

### 不属于漏洞的项

以下都是**设计使然**，不算漏洞：

- 能访问该局域网的明文 HTTP 端口的人可以观察流量
- 对 `9000` 做端口映射或以其他方式暴露监听端所导致的任何后果
- 用在本**未获授权**的系统上

### 授权使用

本项目仅供**获得授权**的安全测试使用。未经书面许可对他人系统进行扫描、攻击或流量拦截属于
违法行为，使用者自行承担全部法律责任 —— 见 [README.md](README.md) 文首的法律声明。

### 数据处理

归档的原始数据保持字节精确，脱敏只作用于导出副本，因此导出不会悄悄改写你的证据。配对码与请求
体属于敏感信息，请勿贴进公开的 Issue 或 Pull Request。
