/* =============================================================================
 * Burp Remote — documentation site behaviour layer
 *
 * Responsibilities
 *   1. Locale      EN/ZH dictionary over the static DOM; data-driven blocks are re-rendered
 *   2. Theme       dark/light persistence — the sun/moon swap itself is pure CSS
 *   3. Content     tables, cards, flavour swatches, FAQ and the repository tree
 *   4. Navigation  table of contents, scroll spy, reading progress, mobile drawer
 *   5. Lightbox    zoom for every figure.shot
 *   6. Clipboard   copy buttons on figure.code blocks
 *
 * No dependency, no build step. This file must stay as the last element of
 * <body> in index.html — every lookup below runs at parse time.
 * ============================================================================= */

(function () {
  'use strict';

  /* ---------------------------------------------------------------------------
   * Constants
   * ------------------------------------------------------------------------ */

  var STORAGE_THEME = 'burp-remote-docs:theme';
  var STORAGE_LANG = 'burp-remote-docs:lang';

  /** Scroll offset at which a section counts as "current": --bar-h (60) + scroll-margin (20) + slop. */
  var SECTION_OFFSET = 88;

  /** How long a copy button stays in its "copied" state. */
  var COPY_RESET_MS = 1600;

  /** Must match the transition duration of .lightbox in index.css. */
  var LIGHTBOX_FADE_MS = 260;

  /** Breakpoint at which index.css turns the TOC into a drawer. */
  var DRAWER_QUERY = '(max-width: 1080px)';

  var REDUCED_MOTION_QUERY = '(prefers-reduced-motion: reduce)';

  /* ---------------------------------------------------------------------------
   * Locale dictionary
   *
   * Values are HTML fragments, mirrored one-to-one with the English defaults
   * baked into index.html, so `innerHTML` is the correct application method.
   * ------------------------------------------------------------------------ */

  var I18N = {
    en: {
      'nav.overview': 'Positioning',
      'nav.features': 'Features',
      'nav.protocol': 'Protocol',
      'nav.quickstart': 'Quick Start',
      'toc.title': 'Contents',
      'toc.readme': 'Full README',

      'hero.eyebrow': 'Burp Suite extension + Android client',
      'hero.tagline': 'Red Team Remote Control Framework',
      'hero.lede': 'A Burp Suite extension and an Android app that stream proxy history, intercept decisions and Repeater entries to your phone in real time — so the engagement keeps moving while you are away from the desk.',
      'hero.ctaReadme': 'Read the README',
      'hero.ctaGithub': 'View on GitHub',

      'pipeline.proxy': 'Burp proxy',
      'pipeline.sourcing': 'Event sourcing',
      'pipeline.ws': 'WebSocket push',
      'pipeline.projection': 'Mobile projection',
      'pipeline.render': 'Compose render',

      'sec.legal': 'Legal Notice',
      'sec.overview': 'Positioning',
      'sec.architecture': 'Architecture',
      'sec.screenshots': 'Screenshots',
      'sec.features': 'Core Features',
      'sec.protocol': 'Protocol Reference',
      'sec.pairing': 'Pairing',
      'sec.security': 'Security Design',
      'sec.themes': 'Theme System',
      'sec.quickstart': 'Quick Start',
      'sec.structure': 'Directory Structure',
      'sec.scenarios': 'Real-world Scenarios',
      'sec.faq': 'FAQ',

      'legal.body': 'This tool is intended solely for security researchers conducting authorised security assessments, red/blue exercises, and CTF competitions. Scanning, attacking or intercepting systems without authorisation is illegal and the user bears full legal responsibility. The developer is not responsible for any unlawful use under any circumstances.',

      'overview.title': 'Burp Suite stays the engine. The phone becomes the console.',
      'overview.body': 'Burp Remote is a remote-control framework for Burp Suite aimed at red teamers and security researchers. It carries three Burp workflows — proxy history, the intercept queue and Repeater — onto an Android phone, so the assessment does not stop when you step away from the machine. All computation stays inside Burp; the phone issues commands and renders results.',
      'overview.dataflow': 'data flow',
      'overview.pipeline': 'Burp proxy → event sourcing → WebSocket push → mobile projection → Compose render',
      'overview.diffTitle': 'How it differs',

      'arch.title': 'One event stream, one sequence, one snapshot fallback.',
      'arch.body': 'Proxy observations, intercept decisions and Repeater actions are all stamped with a monotonically increasing sequence number and pushed over WebSocket. Sequence numbers are allocated centrally by <code>InMemoryRemoteEventStream</code> — the history, intercept and Repeater publishers share a single <code>AtomicLong</code>. Independent counters previously collided, causing the mobile sync coordinator to treat live events as duplicates and drop them silently.',
      'arch.svgClient': 'Android client',
      'arch.svgServer': 'Burp extension',
      'arch.svgBurp': 'Burp Suite',
      'arch.svgLan': 'same LAN &middot; plain HTTP &middot; no TLS',
      'arch.svgNote': 'Brief disconnects resume from the last sequence number; if the 1024-event ring has scrolled past, the server asks the client for /v1/snapshot.',
      'arch.svgCaption': 'Extension owns the engine and the state; the client owns presentation and offline cache.',
      'arch.diagramArchitecture': 'Architecture overview',
      'arch.diagramPairing': 'Pairing flow',
      'arch.diagramConnection': 'Connection lifecycle',
      'arch.diagramSync': 'Event synchronisation',
      'arch.lifecycleTitle': 'Connection lifecycle',
      'arch.lifecycleBody': 'Disconnection is symmetric. The client sends a WebSocket Close frame wrapped in <code>NonCancellable</code> so delivery cannot be abandoned mid-flight, while the plugin concurrently reads <code>incoming</code> frames to notice it immediately — tearing down the session, releasing the connection slot and disassociating the device without waiting for the 30-second ping timeout.',

      'shots.extension': 'Burp extension',
      'shots.extLoaded': 'Extension loaded',
      'shots.extJar': 'Selecting the JAR',
      'shots.extPairing': 'Pairing screen',
      'shots.mobile': 'Android client',
      'shots.mobileDashboard': 'Mobile dashboard',
      'shots.mobileConnection': 'Connection screen',

      'features.title': 'Three Burp workflows, plus what only a phone can do.',
      'protocol.title': 'Sixteen REST endpoints and one WebSocket stream.',
      'protocol.body': 'The extension serves REST on <code>http://&lt;your-IP&gt;:9000</code> and pushes events on <code>ws://&lt;your-IP&gt;:9000/v1/events</code>.',
      'protocol.endpoints': 'Endpoints',
      'protocol.events': 'Event types',
      'protocol.handshake': 'handshake',
      'protocol.handshakeNote': 'The handshake order is fixed. Concurrent connections are capped; connections over the limit receive <code>TRY_AGAIN_LATER</code>.',

      'pairing.title': 'Scan once, stay registered.',
      'pairing.step1': 'Load the extension JAR — it listens on <code>0.0.0.0:9000</code>.',
      'pairing.step2': 'Open the <b>Burp Remote</b> tab in Burp to read the QR code and pairing code.',
      'pairing.step3': 'Install the APK, then scan the QR code or type the code manually.',
      'pairing.step4': 'Connected — events begin syncing immediately.',
      'pairing.note': 'The pairing code is 8 characters and valid for 5 minutes, drawn from a confusable-free alphabet with no I/O/0/1/L/U. One scan registers the device identity, so reconnecting never requires another scan.',

      'security.title': 'LAN-scoped, identity-checked, idempotent.',

      'themes.title': 'Mode sets the luminance. Flavour sets the character.',
      'themes.body': 'Light/dark mode and flavour are orthogonal: the mode controls base luminance, the flavour controls accent and tone. There are three modes — follow system, light, dark — crossed with eight flavours.',
      'themes.note': 'Semantic colours (success, warning, error, info) and syntax highlighting stay constant across every flavour. The bottom navigation bar uses Haze real-time blur with a translucent surface, a top highlight gradient, a bottom inner shadow and elevation — tuned separately for light and dark.',

      'quickstart.title': 'Two scripts, two artefacts.',
      'quickstart.requirements': 'Requirements',
      'quickstart.req1': 'JDK 17 — Burp 2025.x runs on 17, and higher bytecode is rejected at load time.',
      'quickstart.req2': 'Burp Suite Community or Professional, 2025.x or later.',
      'quickstart.req3': 'An Android device on API 26 (Android 8.0) or above.',
      'quickstart.req4': 'Both devices on the same LAN — the extension listens on <code>0.0.0.0:9000</code> by default.',
      'quickstart.jarLabel': 'extension JAR',
      'quickstart.apkLabel': 'Android APK',
      'quickstart.note': 'Both scripts accept <code>JAVA_HOME_FOR_BUILD</code> to point at JDK 17 and pass extra arguments through to Gradle. Load the JAR via <b>Extensions → Installed → Add → Java</b>.',
      'quickstart.release': 'Release',
      'quickstart.releaseNote': 'Pushing a tag triggers GitHub Actions, which builds the JAR through the quality gates and the APK on JDK 17, then attaches both to a GitHub Release.',

      'structure.title': 'Where things live.',
      'structure.label': 'repository tree',

      'footer.line': 'Built with Kotlin · Compose liquid glass UI · event-sourced design · ctkqiang',

      'meta.title': 'Burp Remote — Documentation',
      'meta.description': 'Burp Remote — a Burp Suite extension and Android client that streams proxy history, intercept decisions and Repeater entries to your phone in real time.',

      'ui.copy': 'Copy',
      'ui.copied': 'Copied'
    },

    zh: {
      'nav.overview': '项目定位',
      'nav.features': '核心功能',
      'nav.protocol': '协议参考',
      'nav.quickstart': '快速开始',
      'toc.title': '目录',
      'toc.readme': '完整 README',

      'hero.eyebrow': 'Burp Suite 扩展 + Android 客户端',
      'hero.tagline': '红队远程控制平台',
      'hero.lede': '一个 Burp Suite 扩展 + 一个 Android 应用，把代理历史、拦截决策和 Repeater 条目实时推到你的手机屏幕上——离开工位，测试进度照样推进。',
      'hero.ctaReadme': '阅读 README',
      'hero.ctaGithub': '在 GitHub 查看',

      'pipeline.proxy': 'Burp 代理',
      'pipeline.sourcing': '事件溯源',
      'pipeline.ws': 'WebSocket 推送',
      'pipeline.projection': '手机端投影',
      'pipeline.render': 'Compose 渲染',

      'sec.legal': '法律声明',
      'sec.overview': '项目定位',
      'sec.architecture': '技术架构',
      'sec.screenshots': '界面截图',
      'sec.features': '核心功能',
      'sec.protocol': '协议参考',
      'sec.pairing': '配对流程',
      'sec.security': '安全设计',
      'sec.themes': '主题系统',
      'sec.quickstart': '快速开始',
      'sec.structure': '目录结构',
      'sec.scenarios': '实战场景',
      'sec.faq': '常见问题',

      'legal.body': '本工具仅供安全研究人员在获得书面授权的情况下进行安全评估、红蓝对抗、CTF 竞赛使用。未经授权对他人系统进行扫描/攻击/拦截测试属违法行为，使用者需自行承担一切法律责任。开发者在任何情况下不对使用者的违法行为负责。',

      'overview.title': 'Burp Suite 依然是引擎，手机成为控制台。',
      'overview.body': 'Burp Remote 是一款面向红队与安全研究人员的 Burp Suite 远程控制框架。它把 Burp 的代理历史、拦截队列、Repeater 三条工作流搬到 Android 手机上，让你离开电脑时测试仍能推进。所有计算都留在 Burp 内部；手机只负责下达指令、渲染结果。',
      'overview.dataflow': '数据流向',
      'overview.pipeline': 'Burp 代理 → 事件溯源 → WebSocket 推送 → 手机端投影 → Compose 渲染',
      'overview.diffTitle': '与其他方案的区别',

      'arch.title': '一条事件流，一个序号，一份快照兜底。',
      'arch.body': '代理观测、拦截决策与 Repeater 操作都打上单调递增的序号，经 WebSocket 推送。序号由 <code>InMemoryRemoteEventStream</code> 集中分配——历史、拦截、Repeater 三个发布者共用一把 <code>AtomicLong</code>。此前各自维护计数器会撞号，导致手机端同步协调器把实时事件判为重复而静默丢弃。',
      'arch.svgClient': 'Android 客户端',
      'arch.svgServer': 'Burp 扩展',
      'arch.svgBurp': 'Burp Suite',
      'arch.svgLan': '同一局域网 &middot; 明文 HTTP &middot; 无 TLS',
      'arch.svgNote': '短暂掉线按最后一个序号续传；1024 条环形缓冲滚过之后，服务端要求客户端取 /v1/snapshot。',
      'arch.svgCaption': '扩展掌握引擎与状态，客户端掌握展示与离线缓存。',
      'arch.diagramArchitecture': '架构总览',
      'arch.diagramPairing': '配对流程',
      'arch.diagramConnection': '连接生命周期',
      'arch.diagramSync': '事件同步与续传',
      'arch.lifecycleTitle': '连接生命周期',
      'arch.lifecycleBody': '断开是对称的。手机端把 WebSocket Close 帧包在 <code>NonCancellable</code> 里发送，确保不会中途被取消；插件端同时并发读取 <code>incoming</code> 帧即时感知——立刻拆会话、释放连接名额、撤销设备关联，不必等 30 秒 ping 超时。',

      'shots.extension': 'Burp 扩展',
      'shots.extLoaded': '扩展已加载',
      'shots.extJar': '选择 JAR 加载',
      'shots.extPairing': '配对二维码界面',
      'shots.mobile': 'Android 客户端',
      'shots.mobileDashboard': '手机端主面板',
      'shots.mobileConnection': '手机端连接界面',

      'features.title': '三条 Burp 工作流，外加只有手机做得到的事。',
      'protocol.title': '十六个 REST 端点 + 一条 WebSocket 事件流。',
      'protocol.body': '扩展在 <code>http://&lt;你的IP&gt;:9000</code> 提供 REST 接口，事件流推送在 <code>ws://&lt;你的IP&gt;:9000/v1/events</code>。',
      'protocol.endpoints': '端点一览',
      'protocol.events': '事件类型',
      'protocol.handshake': '握手顺序',
      'protocol.handshakeNote': '握手顺序固定。并发连接数有上限，超限的连接会收到 <code>TRY_AGAIN_LATER</code>。',

      'pairing.title': '扫一次，长期注册。',
      'pairing.step1': '加载扩展 JAR——它监听 <code>0.0.0.0:9000</code>。',
      'pairing.step2': '打开 Burp 的 <b>Burp Remote</b> 标签页，读取二维码与配对码。',
      'pairing.step3': '手机安装 APK，然后扫码或手动输入配对码。',
      'pairing.step4': '连接成功——事件立即开始同步。',
      'pairing.note': '配对码 8 位、5 分钟有效，取自去混淆字符集（无 I/O/0/1/L/U）。扫一次即登记设备身份，之后重连不必再扫。',

      'security.title': '局域网内、校验身份、幂等执行。',

      'themes.title': '明暗决定亮度，口味决定性格。',
      'themes.body': '明暗与口味正交：明暗控制底色深浅，口味控制强调色与整体调性。明暗三档——跟随系统、浅色、深色——与八套口味交叉组合。',
      'themes.note': '语义色（成功、警告、危险、信息）与代码高亮跨口味保持一致。底部导航栏使用 Haze 实时模糊，配半透明表面、顶部高光渐变、底部内阴影与浮起投影，深浅色分别调参。',

      'quickstart.title': '两个脚本，两个产物。',
      'quickstart.requirements': '环境要求',
      'quickstart.req1': 'JDK 17——Burp 2025.x 跑在 17 上，更高版本的字节码在加载时会被拒绝。',
      'quickstart.req2': 'Burp Suite 社区版或专业版，2025.x 及以上。',
      'quickstart.req3': 'Android 手机 API 26（Android 8.0）及以上。',
      'quickstart.req4': '两端处于同一局域网——扩展默认监听 <code>0.0.0.0:9000</code>。',
      'quickstart.jarLabel': '扩展 JAR',
      'quickstart.apkLabel': 'Android APK',
      'quickstart.note': '两个脚本都接受 <code>JAVA_HOME_FOR_BUILD</code> 以指定 JDK 17，其余参数透传给 Gradle。在 Burp 中通过 <b>Extensions → Installed → Add → Java</b> 加载该 JAR。',
      'quickstart.release': '发版',
      'quickstart.releaseNote': '推送 tag 会触发 GitHub Actions：在 JDK 17 上跑过质量闸门编译 JAR、编译 APK，然后把两个产物挂到 GitHub Release。',

      'structure.title': '仓库里都有什么。',
      'structure.label': '仓库目录树',

      'footer.line': '基于 Kotlin 构建 · Compose 液态玻璃 UI · 事件溯源设计 · ctkqiang',

      'meta.title': 'Burp Remote — 文档',
      'meta.description': 'Burp Remote——一个 Burp Suite 扩展 + Android 客户端，把代理历史、拦截决策与 Repeater 条目实时推送到手机。',

      'ui.copy': '复制',
      'ui.copied': '已复制'
    }
  };

  /* ---------------------------------------------------------------------------
   * Chrome labels that live on attributes rather than on text nodes, and so
   * cannot be driven by data-i18n.
   * ------------------------------------------------------------------------ */

  var CHROME = {
    en: {
      langLabel: 'EN',
      langAria: 'Switch language to Chinese',
      themeAria: 'Switch colour theme',
      tocAria: 'Toggle contents'
    },
    zh: {
      langLabel: '中文',
      langAria: '切换语言为英文',
      themeAria: '切换明暗主题',
      tocAria: '展开或收起目录'
    }
  };

  /* ---------------------------------------------------------------------------
   * "How it differs" — Capability | Burp Remote | Remote desktop | Desktop Burp
   * ------------------------------------------------------------------------ */

  var DIFF_HEADERS = {
    en: ['Capability', 'Burp Remote', 'Remote desktop (RDP/VNC)', 'Desktop Burp'],
    zh: ['能力', 'Burp Remote', '远程桌面 (RDP/VNC)', '桌面 Burp']
  };

  var DIFF_ROWS = {
    en: [
      ['Native mobile experience', 'Yes', 'No (laggy zoom)', 'No'],
      ['Real-time history push', 'Yes', 'manual refresh', 'Yes'],
      ['Intercept forward/drop', 'Yes (one tap)', 'awkward', 'Yes'],
      ['Remote Repeater execution', 'Yes', 'awkward', 'Yes'],
      ['Offline browsing of synced data', 'Yes', 'No', 'No'],
      ['Home screen widget', 'Yes', 'No', 'No'],
      ['8 themes + liquid glass', 'Yes', 'No', 'No'],
      ['5-language localisation', 'Yes', 'partial', 'No']
    ],
    zh: [
      ['原生手机体验', '是', '否（缩放卡顿）', '否'],
      ['代理历史实时推送', '是', '需手动刷新', '是'],
      ['拦截放行/丢弃', '是（一键）', '操作困难', '是'],
      ['Repeater 远程执行', '是', '操作困难', '是'],
      ['离线浏览已同步数据', '是', '否', '否'],
      ['桌面小部件', '是', '否', '否'],
      ['8 套主题 + 液态玻璃', '是', '否', '否'],
      ['多语言（5 种）', '是', '部分', '否']
    ]
  };

  /* ---------------------------------------------------------------------------
   * Endpoints — locally independent except for the purpose column
   * ------------------------------------------------------------------------ */

  var ENDPOINT_HEADERS = {
    en: ['Method', 'Path', 'Purpose'],
    zh: ['方法', '路径', '用途']
  };

  var ENDPOINTS = [
    ['GET', '/v1/status', 'Runtime status (protocol version, port, paired devices)', '运行状态（协议版本、端口、已配对设备）'],
    ['POST', '/v1/pair', 'Submit pairing code, obtain device identity', '提交配对码，换取设备身份'],
    ['GET', '/v1/capabilities', 'Server capability declaration', '服务端能力声明'],
    ['GET', '/v1/snapshot', 'Full snapshot (fallback when resync fails)', '全量快照（续传失败时兜底）'],
    ['GET', '/v1/history', 'Proxy history list', '代理历史列表'],
    ['GET', '/v1/history/{id}', 'Single history item (request + response body)', '单条历史（请求体 + 响应体）'],
    ['POST', '/v1/scope/{id}', 'Send history item to Repeater', '历史条目发到 Repeater'],
    ['GET', '/v1/intercepts', 'Intercept queue', '拦截队列'],
    ['GET', '/v1/intercepts/{id}', 'Single intercept detail', '单条拦截详情'],
    ['POST', '/v1/intercepts/{id}/forward', 'Forward', '放行'],
    ['POST', '/v1/intercepts/{id}/drop', 'Drop', '丢弃'],
    ['POST', '/v1/intercepts/{id}/modify', 'Modify and forward', '修改并放行'],
    ['POST', '/v1/repeater', 'Create repeater entry', '创建 Repeater 条目'],
    ['GET', '/v1/repeaters', 'Repeater list', 'Repeater 列表'],
    ['GET', '/v1/repeaters/{id}', 'Single repeater detail', '单条 Repeater 详情'],
    ['POST', '/v1/repeaters/{id}/execute', 'Execute repeater request', '执行 Repeater 请求']
  ];

  /* ---------------------------------------------------------------------------
   * Event types
   * ------------------------------------------------------------------------ */

  var EVENT_HEADERS = {
    en: ['Event type', 'Source', 'Meaning'],
    zh: ['事件类型', '来源', '含义']
  };

  var EVENTS = [
    ['history.item.observed', 'Proxy history publisher', '代理历史发布器', 'History added/updated', '历史新增/更新'],
    ['intercept.created', 'Intercept proxy handler', '拦截代理处理器', 'New request intercepted', '拦截到新请求'],
    ['intercept.forwarded', 'Intercept proxy handler', '拦截代理处理器', 'Request forwarded', '请求被放行'],
    ['intercept.dropped', 'Intercept proxy handler', '拦截代理处理器', 'Request dropped', '请求被丢弃'],
    ['repeater.created', 'REST API', 'REST API', 'Repeater entry created', '新建 Repeater 条目'],
    ['repeater.execution.started', 'REST API', 'REST API', 'Execution started', '执行开始'],
    ['repeater.execution.completed', 'REST API', 'REST API', 'Execution finished', '执行完成']
  ];

  /* ---------------------------------------------------------------------------
   * Security design
   * ------------------------------------------------------------------------ */

  var SECURITY_HEADERS = {
    en: ['Measure', 'Implementation'],
    zh: ['措施', '实现']
  };

  var SECURITY_ROWS = {
    en: [
      ['Device pairing', 'One-time code, 5-minute expiry, confusable-free alphabet (no I/O/0/1/L/U)'],
      ['Authentication', 'Every connection carries a <code>DeviceIdentifier</code>; unregistered devices are rejected'],
      ['Rate limiting', 'Per-device token bucket: burst 20, steady-state 5/sec'],
      ['Idempotency', 'Operation ID dedup; retries return the original result instead of re-executing'],
      ['Audit logging', 'Every command logs device, type, operation ID, result'],
      ['LAN-only', 'Plain HTTP (no TLS); not meant for public internet exposure']
    ],
    zh: [
      ['设备配对', '一次性配对码，5 分钟过期，去混淆字符集（无 I/O/0/1/L/U）'],
      ['身份认证', '每次连接携带 <code>DeviceIdentifier</code>，未登记设备一律拒绝'],
      ['限流', '按设备令牌桶：突发 20 条，稳态 5 条/秒'],
      ['幂等', '操作 ID 去重，重试返回首次结果而非重复执行'],
      ['审计日志', '每条命令记录设备、类型、操作 ID、结果'],
      ['局域网', '明文 HTTP（无 TLS），默认不暴露公网']
    ]
  };

  /* ---------------------------------------------------------------------------
   * Core features
   * ------------------------------------------------------------------------ */

  var FEATURES = {
    en: [
      {
        title: 'Real-time Proxy History',
        points: [
          'WebSocket event stream delivers history entries to your phone in milliseconds',
          'Each record carries method, URL, status code, timestamp',
          'Tap through to full request + response bodies'
        ]
      },
      {
        title: 'Remote Intercept Decisions',
        points: [
          'Intercepted requests pop up on your phone',
          'One-tap Forward / Drop / Modify-and-forward',
          'Decisions are sent back to Burp for actual execution'
        ]
      },
      {
        title: 'Remote Repeater Execution',
        points: [
          'Send history entries to Repeater in one tap',
          'View and edit request bodies on the phone',
          'Results (status code, duration, response body) sync both ways'
        ]
      },
      {
        title: 'Home Screen Widget',
        points: [
          'Shows target host, live request count, intercept count, saved count',
          'Tap to jump back to the dashboard',
          'Reads from SharedPreferences — no persistent background service'
        ]
      },
      {
        title: '8 Themes + Liquid Glass',
        points: [
          '3 light/dark modes (follow system / light / dark) × 8 flavours, orthogonal',
          'Bottom nav bar uses Haze real-time blur + highlight gradient + inner shadow + elevation',
          'Light and dark parameters tuned separately (see “Theme System” above)'
        ]
      }
    ],
    zh: [
      {
        title: '代理历史实时推送',
        points: [
          'WebSocket 事件流，历史条目毫秒级到达手机',
          '每条记录含方法、URL、状态码、时间戳',
          '点进详情看完整请求体 + 响应体'
        ]
      },
      {
        title: '拦截队列远程决策',
        points: [
          'Burp 拦截到的请求弹到手机上',
          '一键放行（Forward）/ 丢弃（Drop）/ 修改后放行',
          '决策回传 Burp 实际执行'
        ]
      },
      {
        title: 'Repeater 远程执行',
        points: [
          '历史条目一键发到 Repeater',
          '手机端查看、编辑请求体',
          '执行结果（状态码、耗时、响应体）双向同步'
        ]
      },
      {
        title: '桌面小部件',
        points: [
          '显示目标主机、实时请求数、拦截数、保存数',
          '点一下跳回主面板',
          '从 SharedPreferences 读快照，不依赖后台服务常驻'
        ]
      },
      {
        title: '8 套主题 + 液态玻璃',
        points: [
          '明暗 3 档（跟随系统/浅色/深色）× 8 套口味，正交组合',
          '底部导航栏用 Haze 实时模糊 + 高光渐变 + 内阴影 + 浮起投影',
          '深浅色分别调参，见上方「主题系统」'
        ]
      }
    ]
  };

  /* ---------------------------------------------------------------------------
   * Real-world scenarios
   * ------------------------------------------------------------------------ */

  var SCENARIOS = {
    en: [
      { title: 'Away-from-desk intercept decisions', body: 'Burp proxies traffic on your laptop; you step away and an intercept fires — your phone buzzes, you tap “Forward”, Burp resumes.' },
      { title: 'Rechecking Repeater on the train', body: 'You tuned a Repeater request earlier; on the train, pull out your phone to check the latest execution result (status, duration, response body).' },
      { title: 'Offline history browsing', body: 'With the network down, you can still browse already-synced history; reconnect and it auto-resumes.' }
    ],
    zh: [
      { title: '离机拦截决策', body: 'Burp 在笔记本上跑代理，你起身离开时恰好拦截到请求——手机弹出，点一下「放行」，Burp 继续。' },
      { title: '地铁上复核 Repeater', body: '之前调好的 Repeater 请求，出门后掏出手机查看最新执行结果（状态码、耗时、响应体）。' },
      { title: '离线浏览历史', body: '网络断开时仍能浏览已同步的代理历史，重连后自动续传补齐。' }
    ]
  };

  /* ---------------------------------------------------------------------------
   * Theme flavours — name and hex are locale independent, the note is not
   * ------------------------------------------------------------------------ */

  var FLAVOURS = [
    { name: 'Burp Classic', hex: '#FF6633', en: 'Brand default', zh: '品牌默认' },
    { name: 'Cyber Cyan', hex: '#22D3EE', en: 'Cold cyberpunk', zh: '冷调赛博' },
    { name: 'Forest Emerald', hex: '#34D399', en: 'Natural, grounded', zh: '自然沉稳' },
    { name: 'Royal Violet', hex: '#A78BFA', en: 'Elegant, mysterious', zh: '优雅神秘' },
    { name: 'Rose Gold', hex: '#FB7185', en: 'Warm, soft', zh: '温暖柔和' },
    { name: 'Ocean Blue', hex: '#60A5FA', en: 'Calm, clear', zh: '冷静通透' },
    { name: 'Solar Amber', hex: '#FBBF24', en: 'Sunset warm', zh: '落日暖调' },
    { name: 'Monochrome', hex: null, en: 'Most restrained', zh: '最克制' }
  ];

  /* ---------------------------------------------------------------------------
   * FAQ
   * ------------------------------------------------------------------------ */

  var FAQ = {
    en: [
      { q: 'Can’t connect?', a: 'Confirm both are on the same LAN, the extension listens on <code>0.0.0.0:9000</code>, and the firewall allows port 9000.' },
      { q: 'Pairing code expired?', a: 'Codes expire after 5 minutes — regenerate.' },
      { q: 'History items not syncing to Repeater?', a: 'Make sure you are on a build with the sequence-number collision fix; older builds with three independent counters silently drop events.' },
      { q: 'IPv6 support?', a: 'IPv4 by default; <code>0.0.0.0</code> on the LAN covers typical use.' }
    ],
    zh: [
      { q: '手机连不上？', a: '确认两端处于同一局域网，插件监听 <code>0.0.0.0:9000</code>，并检查防火墙是否放行 9000 端口。' },
      { q: '配对码过期？', a: '配对码 5 分钟有效，重新生成即可。' },
      { q: '历史条目没同步到 Repeater？', a: '确认使用的是修复序号撞号之后的构建；旧版本三个发布器独立计数会静默丢事件。' },
      { q: '支持 IPv6 吗？', a: '默认 IPv4；局域网内监听 <code>0.0.0.0</code> 即可覆盖常见场景。' }
    ]
  };

  /* ---------------------------------------------------------------------------
   * Repository tree — rendered into <pre><code>, so indentation is significant
   * ------------------------------------------------------------------------ */

  var STRUCTURE_TREE = {
    en: dedent(`
      BurpsuiteRemote/
      ├── plugins/burp-remote-extension/      # Burp extension (Kotlin, JDK 17)
      │   ├── src/main/                       # production code
      │   │   ├── adapter/                    # history/intercept/repeater adapters
      │   │   ├── transport/                  # HTTP server, WS server, event stream, rate limiter
      │   │   ├── security/                   # pairing service, device registry
      │   │   └── protocol/                   # protocol contract (port, message types)
      │   ├── src/test/                       # unit tests
      │   └── src/harness/                    # end-to-end harness (no Burp dependency)
      ├── client/mobileapp/                   # Android app (Compose, MVI)
      │   ├── app/                            # entry, navigation, DI, widget
      │   ├── data/                           # repo impls, Room, Ktor client
      │   ├── domain/                         # use cases, entities, settings, connection state
      │   ├── ui/                             # theme (8 flavors × 2 modes), design system
      │   ├── core/                           # shared model / protocol / common
      │   └── feature/                        # 9 feature modules
      │       ├── connection/                 # QR pairing
      │       ├── dashboard/                  # home panel
      │       ├── history/                    # proxy history
      │       ├── intercept/                  # intercept queue
      │       ├── repeater/                   # repeater
      │       ├── settings/                   # theme & config
      │       ├── sharing/                    # share
      │       ├── screenshot/                 # screenshot
      │       └── archive/                    # archive
      ├── scripts/                            # build scripts (JDK 17 required)
      ├── .github/                            # release workflow + bilingual issue templates
      └── docs/                               # screenshots + PlantUML diagrams
    `),
    zh: dedent(`
      BurpsuiteRemote/
      ├── plugins/burp-remote-extension/      # Burp 扩展（Kotlin，JDK 17）
      │   ├── src/main/                       # 生产代码
      │   │   ├── adapter/                    # 历史/拦截/Repeater 适配器
      │   │   ├── transport/                  # HTTP 服务器、WS 服务器、事件流、限流
      │   │   ├── security/                   # 配对服务、设备登记处
      │   │   └── protocol/                   # 协议契约（端口、消息类型）
      │   ├── src/test/                       # 单元测试
      │   └── src/harness/                    # 端到端夹具（不依赖 Burp）
      ├── client/mobileapp/                   # Android 应用（Compose，MVI）
      │   ├── app/                            # 入口、导航、DI、小部件
      │   ├── data/                           # 仓储实现、Room、Ktor 客户端
      │   ├── domain/                         # 用例、实体、设置、连接状态
      │   ├── ui/                             # 主题（8 口味 × 2 明暗）、设计系统
      │   ├── core/                           # 共享 model / protocol / common
      │   └── feature/                        # 9 个功能模块
      │       ├── connection/                 # 扫码配对
      │       ├── dashboard/                  # 主面板
      │       ├── history/                    # 代理历史
      │       ├── intercept/                  # 拦截队列
      │       ├── repeater/                   # Repeater
      │       ├── settings/                   # 主题与配置
      │       ├── sharing/                    # 分享
      │       ├── screenshot/                 # 截图
      │       └── archive/                    # 归档
      ├── scripts/                            # 构建脚本（需 JDK 17）
      ├── .github/                            # 发版工作流 + 双语 issue 模板
      └── docs/                               # 截图 + PlantUML 图表
    `)
  };

  /* ---------------------------------------------------------------------------
   * DOM handles — resolved once; index.js is the last element of <body>
   * ------------------------------------------------------------------------ */

  var progressBar = document.getElementById('progress-bar');
  var langToggle = document.getElementById('lang-toggle');
  var langLabel = document.getElementById('lang-label');
  var themeToggle = document.getElementById('theme-toggle');
  var tocToggle = document.getElementById('toc-toggle');
  var tocList = document.getElementById('toc-list');
  var diffTable = document.getElementById('diff-table');
  var endpointTable = document.getElementById('endpoint-table');
  var eventTable = document.getElementById('event-table');
  var securityTable = document.getElementById('security-table');
  var featureCards = document.getElementById('feature-cards');
  var scenarioCards = document.getElementById('scenario-cards');
  var themeSwatches = document.getElementById('theme-swatches');
  var faqList = document.getElementById('faq-list');
  var structureTree = document.getElementById('structure-tree');
  var lightbox = document.getElementById('lightbox');
  var lightboxImg = document.getElementById('lightbox-img');
  var lightboxCap = document.getElementById('lightbox-cap');
  var lightboxClose = document.getElementById('lightbox-close');
  var lightboxPrev = document.getElementById('lightbox-prev');
  var lightboxNext = document.getElementById('lightbox-next');
  var metaDescription = document.querySelector('meta[name="description"]');

  var drawerQuery = window.matchMedia(DRAWER_QUERY);
  var reducedMotionQuery = window.matchMedia(REDUCED_MOTION_QUERY);

  /** Sections with a data-nav number, in document order. `key` drives both the TOC and the eyebrow. */
  var sections = [];

  /** Pending reset timers for copy buttons, keyed by button. */
  var copyTimers = new Map();

  var currentLang = 'en';
  var activeSectionId = null;
  var lightboxIndex = -1;
  var lightboxReturnFocus = null;
  var scrollTicking = false;

  /* ---------------------------------------------------------------------------
   * Helpers
   * ------------------------------------------------------------------------ */

  /**
   * Strips the indentation introduced by the source file so the tree keeps its
   * own relative layout once it lands inside <pre>.
   */
  function dedent(block) {
    var lines = block.replace(/^\n/, '').replace(/\s+$/, '').split('\n');
    var indents = lines
      .filter(function (line) { return line.trim().length > 0; })
      .map(function (line) { return line.match(/^\s*/)[0].length; });
    var pad = indents.length ? Math.min.apply(null, indents) : 0;
    return lines
      .map(function (line) { return line.slice(pad); })
      .join('\n');
  }

  /** Resolves a dictionary entry, falling back to English and then to the key itself. */
  function t(key, lang) {
    var table = I18N[lang] || I18N.en;
    if (typeof table[key] === 'string') return table[key];
    if (typeof I18N.en[key] === 'string') return I18N.en[key];
    return key;
  }

  /** Reads localStorage without throwing when storage is unavailable (file://, private mode). */
  function readStored(key) {
    try {
      return window.localStorage.getItem(key);
    } catch (error) {
      return null;
    }
  }

  function writeStored(key, value) {
    try {
      window.localStorage.setItem(key, value);
    } catch (error) {
      /* Storage disabled — the page still works, it just will not remember the choice. */
    }
  }

  function createElement(tag, className, text) {
    var element = document.createElement(tag);
    if (className) element.className = className;
    if (typeof text === 'string') element.textContent = text;
    return element;
  }

  /**
   * The diff table distinguishes a clean yes/no from a qualified answer.
   * Only a bare "Yes"/"No" earns the semantic colour.
   */
  function tallyTone(html) {
    var text = String(html).replace(/<[^>]*>/g, '').trim();
    if (/^(yes|是)$/i.test(text)) return 'yes';
    if (/^(no|否)$/i.test(text)) return 'no';
    return null;
  }

  /**
   * Rebuilds a table from scratch.
   * @param {HTMLTableElement} table
   * @param {string[]} headers   plain-text column labels
   * @param {Array<Array<string>>} rows  cell HTML per row, in locale order
   * @param {number[]} monoColumns  column indexes rendered as td.mono
   */
  function fillTable(table, headers, rows, monoColumns) {
    if (!table) return;

    var head = document.createElement('thead');
    var headRow = document.createElement('tr');
    headers.forEach(function (label) {
      var th = createElement('th', null, label);
      th.scope = 'col';
      headRow.appendChild(th);
    });
    head.appendChild(headRow);

    var body = document.createElement('tbody');
    rows.forEach(function (cells) {
      var row = document.createElement('tr');
      cells.forEach(function (cell, index) {
        var td = document.createElement('td');
        if (monoColumns && monoColumns.indexOf(index) !== -1) td.classList.add('mono');
        var tone = tallyTone(cell);
        if (tone) td.classList.add(tone);
        td.innerHTML = cell;
        row.appendChild(td);
      });
      body.appendChild(row);
    });

    table.replaceChildren(head, body);
  }

  /* ---------------------------------------------------------------------------
   * Rendering — driven entirely by the active locale
   * ------------------------------------------------------------------------ */

  function renderDiffTable(lang) {
    fillTable(diffTable, DIFF_HEADERS[lang] || DIFF_HEADERS.en, DIFF_ROWS[lang] || DIFF_ROWS.en);
  }

  /** Builds the endpoint rows for one locale; the method column becomes a protocol pill. */
  function renderEndpointTable(lang) {
    var purposeIndex = lang === 'zh' ? 3 : 2;
    var rows = ENDPOINTS.map(function (entry) {
      var method = entry[0];
      var pill = '<span class="m m-' + method + '">' + method + '</span>';
      return [pill, entry[1], entry[purposeIndex]];
    });
    fillTable(endpointTable, ENDPOINT_HEADERS[lang] || ENDPOINT_HEADERS.en, rows, [0, 1]);
  }

  function renderEventTable(lang) {
    var sourceIndex = lang === 'zh' ? 2 : 1;
    var meaningIndex = lang === 'zh' ? 4 : 3;
    var rows = EVENTS.map(function (entry) {
      return [entry[0], entry[sourceIndex], entry[meaningIndex]];
    });
    fillTable(eventTable, EVENT_HEADERS[lang] || EVENT_HEADERS.en, rows, [0]);
  }

  function renderSecurityTable(lang) {
    fillTable(securityTable, SECURITY_HEADERS[lang] || SECURITY_HEADERS.en, SECURITY_ROWS[lang] || SECURITY_ROWS.en);
  }

  function renderFeatureCards(lang) {
    if (!featureCards) return;
    var cards = (FEATURES[lang] || FEATURES.en).map(function (feature, index) {
      var card = createElement('article', 'card');
      card.dataset.idx = String(index + 1).padStart(2, '0');
      card.appendChild(createElement('h4', null, feature.title));

      var list = document.createElement('ul');
      feature.points.forEach(function (point) {
        list.appendChild(createElement('li', null, point));
      });
      card.appendChild(list);
      return card;
    });
    featureCards.replaceChildren.apply(featureCards, cards);
  }

  function renderScenarioCards(lang) {
    if (!scenarioCards) return;
    var cards = (SCENARIOS[lang] || SCENARIOS.en).map(function (scenario, index) {
      var card = createElement('article', 'card');
      card.dataset.idx = String(index + 1).padStart(2, '0');
      card.appendChild(createElement('h4', null, scenario.title));

      var list = document.createElement('ul');
      list.appendChild(createElement('li', null, scenario.body));
      card.appendChild(list);
      return card;
    });
    scenarioCards.replaceChildren.apply(scenarioCards, cards);
  }

  function renderThemeSwatches(lang) {
    if (!themeSwatches) return;
    var noteKey = lang === 'zh' ? 'zh' : 'en';

    var swatches = FLAVOURS.map(function (flavour) {
      var swatch = createElement('div', 'swatch');
      var top = createElement('div', 'swatch-top');

      var dot = createElement('span', 'swatch-dot');
      dot.style.background = flavour.hex || 'var(--fg-faint)';
      top.appendChild(dot);
      top.appendChild(createElement('span', 'swatch-name', flavour.name));
      top.appendChild(createElement('span', 'swatch-hex', flavour.hex || '—'));

      swatch.appendChild(top);
      swatch.appendChild(createElement('p', 'swatch-note', flavour[noteKey]));
      return swatch;
    });
    themeSwatches.replaceChildren.apply(themeSwatches, swatches);
  }

  function renderFaq(lang) {
    if (!faqList) return;
    var items = (FAQ[lang] || FAQ.en).map(function (entry) {
      var details = createElement('details', 'faq-item');
      details.appendChild(createElement('summary', null, entry.q));

      var answer = document.createElement('p');
      answer.innerHTML = entry.a;
      details.appendChild(answer);
      return details;
    });
    faqList.replaceChildren.apply(faqList, items);
  }

  function renderStructureTree(lang) {
    if (!structureTree) return;
    structureTree.textContent = STRUCTURE_TREE[lang] || STRUCTURE_TREE.en;
  }

  /** Rebuilds every generated block for the active locale. */
  function renderDynamic(lang) {
    renderDiffTable(lang);
    renderEndpointTable(lang);
    renderEventTable(lang);
    renderSecurityTable(lang);
    renderFeatureCards(lang);
    renderScenarioCards(lang);
    renderThemeSwatches(lang);
    renderFaq(lang);
    renderStructureTree(lang);
  }

  /** Collects the numbered sections and builds the table of contents from them. */
  function initSections() {
    sections = Array.prototype.slice
      .call(document.querySelectorAll('section.section[data-nav]'))
      .map(function (element) {
        var label = element.querySelector('[data-i18n^="sec."]');
        return {
          el: element,
          id: element.id,
          num: element.dataset.nav,
          key: label ? label.dataset.i18n : null
        };
      });
  }

  function renderToc(lang) {
    if (!tocList) return;
    var items = sections.map(function (section) {
      var link = document.createElement('a');
      link.href = '#' + section.id;
      link.dataset.target = section.id;
      link.appendChild(createElement('span', 'toc-num', section.num));
      link.appendChild(createElement('span', null, section.key ? t(section.key, lang).replace(/<[^>]*>/g, '') : section.id));

      var item = document.createElement('li');
      item.appendChild(link);
      return item;
    });
    tocList.replaceChildren.apply(tocList, items);
    activeSectionId = null;
  }

  /* ---------------------------------------------------------------------------
   * Locale application
   * ------------------------------------------------------------------------ */

  function applyI18n(lang) {
    document.querySelectorAll('[data-i18n]').forEach(function (element) {
      element.innerHTML = t(element.dataset.i18n, lang);
    });
  }

  /** Attributes cannot carry data-i18n, so they are set explicitly. */
  function applyChrome(lang) {
    var labels = CHROME[lang] || CHROME.en;
    langLabel.textContent = labels.langLabel;

    [[langToggle, labels.langAria], [themeToggle, labels.themeAria], [tocToggle, labels.tocAria]].forEach(function (pair) {
      pair[0].setAttribute('aria-label', pair[1]);
      pair[0].setAttribute('title', pair[1]);
    });
  }

  /** Swaps the four PlantUML renders between their -en and -zh variants. */
  function applyDiagramSources(lang) {
    document.querySelectorAll('[data-diagram]').forEach(function (button) {
      var image = button.querySelector('img');
      if (!image) return;
      image.src = 'images/diagrams/' + button.dataset.diagram + '-' + (lang === 'zh' ? 'zh' : 'en') + '.png';
    });
  }

  function applyMeta(lang) {
    document.title = t('meta.title', lang);
    if (metaDescription) metaDescription.setAttribute('content', t('meta.description', lang));
  }

  function applyLang(lang) {
    var next = lang === 'zh' ? 'zh' : 'en';
    currentLang = next;

    document.documentElement.dataset.lang = next;
    document.documentElement.lang = next === 'zh' ? 'zh-CN' : 'en';

    applyI18n(next);
    applyChrome(next);
    applyDiagramSources(next);
    applyMeta(next);
    renderDynamic(next);
    renderToc(next);

    if (!lightbox.hidden) closeLightbox();
    updateScrollState(true);
  }

  /* ---------------------------------------------------------------------------
   * Theme — persistence only; the sun/moon swap and palettes are CSS
   * ------------------------------------------------------------------------ */

  function applyTheme(theme) {
    document.documentElement.dataset.theme = theme === 'light' ? 'light' : 'dark';
  }

  /* ---------------------------------------------------------------------------
   * Reading progress, scroll spy, mobile drawer
   * ------------------------------------------------------------------------ */

  function updateScrollState(force) {
    var root = document.documentElement;
    var max = root.scrollHeight - window.innerHeight;
    var ratio = max > 0 ? Math.min(1, Math.max(0, window.scrollY / max)) : 0;
    progressBar.style.width = (ratio * 100).toFixed(2) + '%';

    if (!sections.length) return;

    var nextActive = sections[0].id;
    sections.forEach(function (section) {
      if (section.el.getBoundingClientRect().top <= SECTION_OFFSET) nextActive = section.id;
    });
    if (max > 0 && window.scrollY >= max - 2) nextActive = sections[sections.length - 1].id;

    if (!force && nextActive === activeSectionId) return;
    activeSectionId = nextActive;

    tocList.querySelectorAll('a').forEach(function (link) {
      var isActive = link.dataset.target === activeSectionId;
      link.classList.toggle('is-active', isActive);
      if (isActive) {
        link.setAttribute('aria-current', 'true');
      } else {
        link.removeAttribute('aria-current');
      }
    });
  }

  function onScroll() {
    if (scrollTicking) return;
    scrollTicking = true;
    window.requestAnimationFrame(function () {
      scrollTicking = false;
      updateScrollState(false);
    });
  }

  function setDrawer(open) {
    document.body.classList.toggle('toc-open', open);
    tocToggle.setAttribute('aria-expanded', String(open));
  }

  function isDrawerOpen() {
    return document.body.classList.contains('toc-open');
  }

  /* ---------------------------------------------------------------------------
   * Reveal on scroll — skipped when the visitor asked for reduced motion
   * ------------------------------------------------------------------------ */

  function initReveal() {
    var targets = Array.prototype.slice.call(
      document.querySelectorAll('.section > *, .hero > *')
    );

    if (reducedMotionQuery.matches || typeof window.IntersectionObserver !== 'function') {
      targets.forEach(function (element) {
        element.classList.add('reveal', 'in');
      });
      return;
    }

    var observer = new IntersectionObserver(function (entries) {
      entries.forEach(function (entry) {
        if (!entry.isIntersecting) return;
        entry.target.classList.add('in');
        observer.unobserve(entry.target);
      });
    }, { rootMargin: '0px 0px -8% 0px', threshold: 0.06 });

    targets.forEach(function (element) {
      element.classList.add('reveal');
      observer.observe(element);
    });
  }

  /* ---------------------------------------------------------------------------
   * Lightbox
   * ------------------------------------------------------------------------ */

  function collectShots() {
    return Array.prototype.slice.call(document.querySelectorAll('button.shot'));
  }

  function showShot(shots, index) {
    var count = shots.length;
    if (!count) return;

    lightboxIndex = ((index % count) + count) % count;
    var button = shots[lightboxIndex];
    var image = button.querySelector('img');
    var caption = button.querySelector('.shot-cap');

    lightboxImg.src = image ? image.currentSrc || image.src : '';
    lightboxImg.alt = image ? image.alt || '' : '';
    lightboxCap.textContent = caption ? caption.textContent.trim() : '';
  }

  function openLightbox(button) {
    var shots = collectShots();
    var index = shots.indexOf(button);
    if (index === -1) return;

    lightboxReturnFocus = button;
    lightbox.hidden = false;
    document.body.style.overflow = 'hidden';
    showShot(shots, index);

    /* One frame so the [hidden] → visible swap settles before the opacity transition. */
    window.requestAnimationFrame(function () {
      lightbox.classList.add('is-open');
    });
    lightboxClose.focus();
  }

  function closeLightbox() {
    if (lightbox.hidden) return;

    lightbox.classList.remove('is-open');
    document.body.style.overflow = '';
    window.setTimeout(function () {
      lightbox.hidden = true;
      lightboxImg.removeAttribute('src');
    }, LIGHTBOX_FADE_MS);

    if (lightboxReturnFocus) {
      lightboxReturnFocus.focus();
      lightboxReturnFocus = null;
    }
  }

  function stepLightbox(offset) {
    var shots = collectShots();
    showShot(shots, lightboxIndex + offset);
  }

  /* ---------------------------------------------------------------------------
   * Clipboard
   * ------------------------------------------------------------------------ */

  function copyText(text) {
    if (navigator.clipboard && window.isSecureContext) {
      return navigator.clipboard.writeText(text).then(
        function () { return true; },
        function () { return legacyCopy(text); }
      );
    }
    return Promise.resolve(legacyCopy(text));
  }

  /** Fallback for non-secure contexts, where navigator.clipboard is unavailable. */
  function legacyCopy(text) {
    var scratch = document.createElement('textarea');
    scratch.value = text;
    scratch.setAttribute('readonly', '');
    scratch.style.position = 'fixed';
    scratch.style.top = '-1000px';
    scratch.style.opacity = '0';
    document.body.appendChild(scratch);
    scratch.select();

    var copied = false;
    try {
      copied = document.execCommand('copy');
    } catch (error) {
      copied = false;
    }
    scratch.remove();
    return copied;
  }

  function flashCopy(button, copied) {
    var pending = copyTimers.get(button);
    if (pending) window.clearTimeout(pending);

    button.classList.toggle('is-done', copied);
    button.textContent = t(copied ? 'ui.copied' : 'ui.copy', currentLang);

    copyTimers.set(button, window.setTimeout(function () {
      button.classList.remove('is-done');
      button.textContent = t('ui.copy', currentLang);
      copyTimers.delete(button);
    }, COPY_RESET_MS));
  }

  function handleCopy(button) {
    var figure = button.closest('figure.code');
    var code = figure ? figure.querySelector('pre code') : null;
    if (!code) return;

    copyText(code.textContent.replace(/^\n+|\n+$/g, '')).then(function (copied) {
      flashCopy(button, copied);
    });
  }

  /* ---------------------------------------------------------------------------
   * Events — one delegated click handler plus keyboard input
   * ------------------------------------------------------------------------ */

  function onDocumentClick(event) {
    var copyButton = event.target.closest('[data-copy]');
    if (copyButton) {
      handleCopy(copyButton);
      return;
    }

    var shot = event.target.closest('button.shot');
    if (shot) {
      openLightbox(shot);
      return;
    }

    /* On narrow viewports the TOC is a drawer — collapse it once a target is chosen. */
    if (event.target.closest('.toc-list a') && drawerQuery.matches) {
      setDrawer(false);
    }
  }

  function onDocumentKeydown(event) {
    if (!lightbox.hidden) {
      if (event.key === 'Escape') {
        event.preventDefault();
        closeLightbox();
      } else if (event.key === 'ArrowLeft') {
        event.preventDefault();
        stepLightbox(-1);
      } else if (event.key === 'ArrowRight') {
        event.preventDefault();
        stepLightbox(1);
      }
      return;
    }

    if (event.key === 'Escape' && isDrawerOpen()) setDrawer(false);
  }

  function bindEvents() {
    document.addEventListener('click', onDocumentClick);
    document.addEventListener('keydown', onDocumentKeydown);

    window.addEventListener('scroll', onScroll, { passive: true });
    window.addEventListener('resize', onScroll);

    themeToggle.addEventListener('click', function () {
      var next = document.documentElement.dataset.theme === 'light' ? 'dark' : 'light';
      applyTheme(next);
      writeStored(STORAGE_THEME, next);
    });

    langToggle.addEventListener('click', function () {
      var next = currentLang === 'zh' ? 'en' : 'zh';
      applyLang(next);
      writeStored(STORAGE_LANG, next);
    });

    tocToggle.addEventListener('click', function () {
      setDrawer(!isDrawerOpen());
    });

    lightboxClose.addEventListener('click', closeLightbox);
    lightboxPrev.addEventListener('click', function () { stepLightbox(-1); });
    lightboxNext.addEventListener('click', function () { stepLightbox(1); });

    /* Clicking the backdrop closes, clicking the image itself does not. */
    lightbox.addEventListener('click', function (event) {
      if (event.target === lightbox) closeLightbox();
    });
  }

  /* ---------------------------------------------------------------------------
   * Boot
   * ------------------------------------------------------------------------ */

  function init() {
    /* index.html ships data-theme="dark"; a stored choice wins from then on. */
    var storedTheme = readStored(STORAGE_THEME);
    applyTheme(storedTheme === 'light' || storedTheme === 'dark' ? storedTheme : 'dark');

    /* index.html ships data-lang="en", which stays the first-visit default. */
    var storedLang = readStored(STORAGE_LANG);

    initSections();
    bindEvents();
    applyLang(storedLang === 'zh' ? 'zh' : 'en');
    initReveal();
    updateScrollState(true);
  }

  init();
})();
