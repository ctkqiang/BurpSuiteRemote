# Burp Remote

用手机控制 Burp Suite。一个 Burp 扩展加一个 Android 应用，通过局域网把代理历史、拦截决策、Repeater 条目实时推到手机上。

![扩展已加载](docs/images/plugins_screenshot/1.png)

![选择 JAR 加载](docs/images/plugins_screenshot/2.png)

![配对二维码界面](docs/images/plugins_screenshot/3.png)

![手机端主面板](docs/images/mobile_app_screenshot/Screenshot_2026-09-15-13-00-45-590_xin.ctkqiang.burpsuite.remote.mobileapp.jpg)

![手机端连接界面](docs/images/mobile_app_screenshot/Screenshot_2026-09-15-13-00-53-661_xin.ctkqiang.burpsuite.remote.mobileapp.jpg)

[English](README.en.md)

## 这是什么

Burp Remote 让你不用坐在电脑前就能操作 Burp。扩展在你的机器上开一个 WebSocket 事件流和 REST API，手机连上来之后可以浏览代理历史、放行或丢弃拦截到的请求、跑 Repeater 条目——全程不用碰笔记本。

举个场景：你在做渗透测试，Burp 开在笔记本上跑着代理。你想起身倒杯水，这时候拦截到了一个请求——手机上弹出来，你看了一眼，点「放行」，回来再看 Burp 里已经继续了。或者你在 Repeater 里调好了一个请求，在地铁上掏出手机看上一眼的执行结果。

仓库里两部分：

- **`plugins/burp-remote-extension`** —— Burp Suite 的 Java 扩展（Kotlin 写的）。通过内存事件流发布事件，通过 HTTP REST API 接受控制命令。加载后在 Burp 里加一个 "Burp Remote" 标签页，里面有配对二维码和运行参数。
- **`client/mobileapp`** —— Android 应用（Jetpack Compose，MVI 架构，Clean Architecture 分层）。连上扩展后把事件写入本地 Room 数据库，再通过状态驱动的界面渲染出来。支持离线浏览已同步的数据，重连后自动续传。

## 环境要求

- JDK 17（Burp Suite 2025.x 跑在 17 上，更高版本的字节码加载时会被拒）
- Burp Suite 社区版或专业版（2025.x 及以上）
- Android 手机，API 26（Android 8.0）及以上
- 电脑和手机在同一个局域网里（扩展默认监听 `0.0.0.0:9000`）

## 构建

两个脚本搞定一切——找 JDK 17、跑质量闸门（ktlint + detekt + 单元测试）、打印产物路径。

### 扩展 JAR

```bash
scripts/build-burp-extension.sh
```

产物：`build/plugins/burp-remote-extension/libs/burp-remote-extension-0.1.0.jar`

在 Burp 里加载：**Extensions → Installed → Add → Java → 选这个 JAR**。

脚本会用 `packageExtension` 任务，它把 ktlint、detekt、单元测试和 shadowJar 绑成一个不可分割的动作——闸门红了就产不出 JAR，不会把不合格的构件交到你手上。

### Android APK

```bash
scripts/build-mobile-app.sh
```

产物：`build/mobileapp/app/outputs/apk/debug/app-debug.apk`

安装：`adb install -r <路径>`

两个脚本都接受 `JAVA_HOME_FOR_BUILD` 环境变量指定 JDK 17 路径，附加参数透传给 Gradle。

## 用法

1. 在 Burp Suite 里加载扩展 JAR，它会在 `0.0.0.0:9000` 开始监听。
2. 在 Burp 里打开 "Burp Remote" 标签页，看到配对二维码和配对码。
3. 在手机上安装 APK，打开应用，扫二维码或手动输入配对码。
4. 应用连上之后开始同步事件，完事。

配对码有效期 5 分钟，8 位字母数字（去掉了容易混淆的 I/O/0/1/L/U）。扫一次配对码后设备身份就登记在插件端了，以后重连不用再扫——除非你把配对登记清掉了。

连上之后能看到的东西：

- **代理历史**：实时推送，每一条都有方法、URL、状态码、时间戳。点进去看请求体和响应体。
- **拦截队列**：Burp 拦截到的请求会弹到手机上，你可以放行（Forward）或丢弃（Drop）。决策回传到 Burp 执行。
- **Repeater**：从历史条目发到 Repeater，在手机上查看、编辑请求体、执行。执行结果（状态码、耗时、响应体）同步回来。

## REST API

扩展在 `http://<你的IP>:9000` 上开了一组 REST 端点，手机端用的就是这些：

| 方法 | 路径 | 用途 |
|------|------|------|
| GET | `/v1/status` | 读取运行状态（协议版本、监听端口、已配对设备） |
| POST | `/v1/pair` | 提交配对码，换取设备身份 |
| GET | `/v1/capabilities` | 读取服务端能力声明 |
| GET | `/v1/snapshot` | 取全量快照（续传失败时用） |
| GET | `/v1/history` | 读取代理历史列表 |
| GET | `/v1/history/{id}` | 读取单条历史详情（请求体 + 响应体） |
| POST | `/v1/scope/{id}` | 把历史条目发到 Repeater |
| GET | `/v1/intercepts` | 读取拦截队列 |
| GET | `/v1/intercepts/{id}` | 读取单条拦截详情 |
| POST | `/v1/intercepts/{id}/forward` | 放行拦截 |
| POST | `/v1/intercepts/{id}/drop` | 丢弃拦截 |
| POST | `/v1/intercepts/{id}/modify` | 修改并放行 |
| POST | `/v1/repeater` | 创建 Repeater 条目 |
| GET | `/v1/repeaters` | 读取 Repeater 列表 |
| GET | `/v1/repeaters/{id}` | 读取单条 Repeater 详情 |
| POST | `/v1/repeaters/{id}/execute` | 执行 Repeater 请求 |

WebSocket 事件流在 `ws://<你的IP>:9000/v1/events`。

## 事件类型

扩展通过 WebSocket 推送的事件：

| 事件类型 | 来源 | 含义 |
|---------|------|------|
| `history.item.observed` | 代理历史发布器 | Burp 代理历史里新增或更新了一条记录 |
| `intercept.created` | 拦截代理处理器 | 拦截到了一个新请求 |
| `intercept.forwarded` | 拦截代理处理器 | 请求被放行 |
| `intercept.dropped` | 拦截代理处理器 | 请求被丢弃 |
| `repeater.created` | REST API | 新建了一个 Repeater 条目 |
| `repeater.execution.started` | REST API | Repeater 请求开始执行 |
| `repeater.execution.completed` | REST API | Repeater 请求执行完成 |

每条事件带一个全局单调递增的序号，手机端用它做断点续传。

## 架构

### 事件溯源

扩展用事件溯源模型。每一条代理观测、拦截决策、Repeater 操作都会打上一个单调递增的序号，通过 WebSocket 推给已连接的客户端。序号由 `InMemoryRemoteEventStream` 集中分配——历史、拦截、Repeater 三个发布者共用一把 AtomicLong。以前各自维护计数器，会撞号，导致事件在手机端的同步协调器里被判成重复投递而静默丢弃。

事件流在内存里保留最近 1024 条，超出这个窗口的旧事件会被挤掉。手机短暂掉线（几秒到几十秒）再连上来，直接从断点的序号续传就行。如果掉线太久、窗口已经滚过去了，插件会要求手机取一次全量快照（`/v1/snapshot`），然后重新续传。

### 连接生命周期

握手顺序固定为 CONNECT → AUTHENTICATE → RESUME → 事件流。

认证靠设备身份：配对成功后插件颁发一个 `DeviceIdentifier`，以后每次连上来都要带上这个身份。不在已配对登记处里的设备一律拒绝。

连接断开是对称的：手机端发 WebSocket Close 帧（包在 `NonCancellable` 里确保发出），插件端并发读 `incoming` 帧来及时感知，立刻拆会话、释放连接名额、撤销设备关联。不用等 ping 超时（最多 30 秒）才清理。

### 安全措施

| 措施 | 做了什么 |
|------|---------|
| 设备配对 | 一次性配对码，5 分钟过期，去混淆字符集（无 I/O/0/1/L/U） |
| 身份认证 | 每次连接都要带设备身份，不在登记处里就拒 |
| 限流 | 按设备令牌桶，突发 20 条，稳态每秒 5 条 |
| 幂等 | 操作 ID 去重，重试拿回首次结果而不是重复执行 |
| 审计日志 | 每条命令的设备、类型、操作 ID、结果都记 |
| 局域网 | 不做 TLS（插件端只提供明文端点），默认不暴露到公网 |

### 手机端分层

手机端按 Clean Architecture 分四层：

- **app** —— 入口、导航、依赖注入容器、前台服务、桌面小部件
- **data** —— 仓储实现、Room 数据库、Ktor 客户端、事件摄入器
- **domain** —— 用例、实体、设置、连接状态枚举
- **ui** —— 主题系统、设计系统、共享组件

功能按屏幕拆成独立模块（`feature/` 下），每个模块只依赖 domain 和 ui，不直接碰 data。

## 技术图表

以下图表的 PlantUML 源文件在 `docs/plantuml/`，渲染输出在 `docs/images/diagrams/`。

### 架构总览

![架构总览](docs/images/diagrams/architecture-zh.png)

### 配对流程

![配对流程](docs/images/diagrams/pairing-zh.png)

### 连接生命周期

![连接生命周期](docs/images/diagrams/connection-zh.png)

### 事件同步与续传

![事件同步与续传](docs/images/diagrams/event-sync-zh.png)

## 功能模块

| 模块 | 干什么 |
|------|--------|
| `feature/connection` | 扫码配对、连接状态显示 |
| `feature/dashboard` | 主面板：连接状态、统计摘要、导航入口 |
| `feature/history` | 代理历史列表 + 详情（请求体、响应体） |
| `feature/intercept` | 拦截队列：查看、放行、丢弃、修改 |
| `feature/repeater` | Repeater 列表 + 详情 + 创建 + 执行 |
| `feature/settings` | 主题（明暗 + 8 套口味）、连接配置 |
| `feature/sharing` | 分享请求/响应 |
| `feature/screenshot` | 截图 |
| `feature/archive` | 归档 |

## 主题系统

明暗和口味是两件正交的事：明暗决定底色深浅，口味决定强调色和整体色调。

明暗三档：跟随系统、浅色、深色。

八套口味：

| 口味 | 强调色 | 调性 |
|------|--------|------|
| Burp Classic | 橙 `#FF6633` | 品牌默认 |
| Cyber Cyan | 青 `#22D3EE` | 冷调赛博 |
| Forest Emerald | 翠 `#34D399` | 自然沉稳 |
| Royal Violet | 紫 `#A78BFA` | 优雅神秘 |
| Rose Gold | 玫瑰 `#FB7185` | 温暖柔和 |
| Ocean Blue | 蓝 `#60A5FA` | 冷静通透 |
| Solar Amber | 琥珀 `#FBBF24` | 落日暖调 |
| Monochrome | 无彩色 | 最克制 |

每套口味都有浅色和深色两份配色，语义色（成功=绿、警告=黄、危险=红、信息=蓝）跨口味保持一致，代码高亮色也跨口味统一。

底部导航栏用液态玻璃效果（Haze 实时模糊 + 半透明表面 + 顶部高光渐变 + 底部内阴影 + 浮起投影），深色和浅色下参数分别调过。

## 桌面小部件

手机上有个 Android 桌面小部件，显示目标主机、实时请求数、拦截数、保存数。点一下跳进主面板。小部件从 SharedPreferences 读快照，不依赖后台服务常驻。

## 多语言

支持五种语言：英语（默认）、中文、德语、日语、蒙古语。所有功能模块的 strings.xml 都有对应翻译。

## 发版

打 tag 触发 GitHub Actions：

```bash
git tag v0.1.0
git push origin v0.1.0
```

CI 会用 JDK 17 编插件 JAR（带质量闸门）和 APK，挂到 GitHub Release 上。

## 项目结构

```
.
├── plugins/
│   └── burp-remote-extension/     # Burp 扩展（Kotlin，JDK 17）
│       ├── src/main/              # 生产代码
│       ├── src/test/              # 单元测试
│       └── src/harness/           # 端到端夹具（不依赖 Burp）
├── client/
│   └── mobileapp/                 # Android 应用（Compose，MVI）
│       ├── app/                   # 入口、导航、依赖注入容器、小部件
│       ├── data/                  # 仓储实现、Room、Ktor 客户端
│       ├── domain/                # 用例、实体、设置、连接状态
│       ├── ui/                    # 主题（8 口味 × 2 明暗）、设计系统
│       ├── core/                  # 共享：model、protocol、common
│       └── feature/              # 功能模块（9 个屏幕）
│           ├── connection/        # 扫码配对
│           ├── dashboard/         # 主面板
│           ├── history/          # 代理历史
│           ├── intercept/        # 拦截队列
│           ├── repeater/         # Repeater
│           ├── settings/         # 主题与配置
│           ├── sharing/          # 分享
│           ├── screenshot/       # 截图
│           └── archive/          # 归档
├── scripts/                       # 构建脚本（需要 JDK 17）
│   ├── build-burp-extension.sh
│   ├── build-mobile-app.sh
│   └── lib/                      # 脚本共用库（JDK 解析）
├── .github/
│   ├── workflows/release.yml     # tag 触发发版
│   └── ISSUE_TEMPLATE/           # 双语 issue 模板
└── docs/                          # 截图
```

## 技术栈

**扩展**：Kotlin、Ktor（CIO 服务器 + WebSocket + 内容协商）、kotlinx.serialization（JSON）、ZXing（二维码）、Montoya Burp API。构建用 Gradle + Shadow 插件打 fat JAR，质量闸门 ktlint + detekt。

**Android 应用**：Kotlin、Jetpack Compose、Haze（液态玻璃模糊效果）、Room（本地数据库）、Ktor 客户端（WebSocket + REST）、DataStore（偏好持久化）、CameraX + MLKit（二维码扫描）、MVI 模式单向数据流。构建用 Gradle，minSdk 26，targetSdk 36。

## 许可证

MIT

## 作者

钟智强 (Johnmelodyme)
