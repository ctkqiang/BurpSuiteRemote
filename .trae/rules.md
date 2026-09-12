# Burp Remote — Workspace Rules

**Document:** `.trae/rules.md`
**Scope:** the entire repository (both `plugins/` and `client/`)
**Author:** 钟智强
**License:** free and open source
**Status:** binding. These rules override personal preference.

> If a rule in this document conflicts with the illustrative pseudo-code in
> `.trae/plan.md`, **this document wins**. `plan.md` describes *what* we build;
> `rules.md` describes *how* we are allowed to write it.

---

## 0. How to read this document

The words below are used with a strict, technical meaning throughout the project.

| Term | Meaning |
| --- | --- |
| **Must** | Non-negotiable. A violation fails review. |
| **Must not** | Forbidden. A violation fails review. |
| **Should** | Strong default. Deviating requires a written justification in the pull request. |
| **Command** | An imperative request for the system to do something. It is *intent*, not *result*. |
| **Event** | An immutable record of a fact that already happened. It is *result*, not *intent*. |
| **Projection** | A read model derived by replaying events. It is disposable and rebuildable. |
| **Aggregate** | A consistency boundary that protects invariants for one command at a time. |

---

## 1. Project identity

| Item | Value |
| --- | --- |
| Project name | Burp Remote |
| Application identifier | `xin.ctkqiang.burpsuite` |
| Author | 钟智强 |
| Distribution | free of charge, open source (`isFree = true`) |
| Default remote port | `9000` |
| Default locale | `en` |
| Supported locales | `en`, `zh`, `de` |
| Theme modes | `AUTOMATIC`, `LIGHT`, `DARK` |
| Brand colour | Burp Suite orange |

### 1.1 The two products in one repository

```text
BurpsuiteRemote/
├── plugins/     # Project 1 — the Burp Suite extension (remote server)
├── client/      # Project 2 — the mobile application (remote client)
├── protocol/    # Language-neutral wire contracts (JSON schemas, versioned)
├── docs/        # Architecture, security and protocol documentation
├── scripts/     # Developer tooling and automation
└── .trae/       # plan.md and rules.md
```

### 1.2 Responsibility split — this is the single most important boundary

```text
Burp Suite            executes HTTP, owns proxy history, owns intercept queue
      │ Montoya
plugins/              observes Burp, exposes the remote protocol, owns the
                      authoritative remote event sequence
      │ TLS over LAN, port 9000
client/               observes events, stores a local event journal, rebuilds
                      projections, renders state
```

**The client must never behave as a second Burp.** It does not implement a proxy.
It does not invent facts about Burp. It only projects facts that Burp produced.

---

## 2. Language and toolchain

Both projects are written in **Kotlin**. There is exactly one language and one
convention set in this repository.

| Concern | Choice |
| --- | --- |
| Language | Kotlin (JVM target for `plugins/`, Android target for `client/`) |
| JDK | 17 |
| Build | Gradle with the Kotlin DSL (`build.gradle.kts` only) |
| Serialisation | `kotlinx.serialization` |
| Async | `kotlinx.coroutines`, `Flow` |
| Dependency injection | constructor injection; a DI container only at the composition root |
| Android UI | Jetpack Compose, Material 3 |
| Android persistence | Room on SQLite, DataStore for preferences |
| Android paging | Paging 3 |
| Static analysis | `ktlint` + `detekt`, wired into the build |
| Tests | JUnit 5, `kotlinx-coroutines-test`, Turbine, MockK, Room test helpers |

**`plugins/` uses Kotlin to talk to the Montoya API.** Montoya is a Java API, and
Kotlin on the JVM is fully interoperable with it. Use Kotlin, not Java, so that
naming, comment and event-sourcing rules apply uniformly everywhere.

**Must not** introduce Java sources. **Must not** mix Groovy `build.gradle` with
Kotlin DSL.

### 2.1 Build output directory

Every Gradle module of either product writes its output to a single `build/`
directory at the repository root. No module keeps a `build/` directory of its own.

```text
build/
├── plugins/<module>/     # Project 1 — the Burp Suite extension
└── mobileapp/<module>/   # Project 2 — the Android application
```

The redirection covers **all** of Gradle's output, including intermediate
directories (`classes/`, `kotlin/`, `tmp/`, `reports/`), not just the final
artefact. This is why it is implemented with `layout.buildDirectory.set(...)`
inside an `allprojects {}` block: copying the final artefact after a build would
leave `clean` and incremental state pointing at the old location, producing two
directories that must both be understood.

The product segment (`plugins/` or `mobileapp/`) is mandatory. Gradle names its
intermediate files by their relative path, so two modules sharing one build
directory would silently overwrite each other's class files and reports, with
the failure appearing only under certain build orders.

A build script **must not** set `buildDir` or `layout.buildDirectory` anywhere
else. New modules inherit the layout through `allprojects {}` and need no
per-module configuration.

`build/` is ignored by `.gitignore`, so it never enters version control.

---

## 3. Naming rules

### 3.1 The core principle

Every identifier **must** read as an English sentence fragment that a reviewer
understands without opening the declaration. Length is not a defect;
ambiguity is.

```kotlin
// Must not
val respTxt: String
val intcptId: String
fun fwd(i: String): Boolean

// Must
val responseBodyText: String
val interceptIdentifier: InterceptIdentifier
fun forwardIntercept(interceptIdentifier: InterceptIdentifier): CommandResult
```

### 3.2 Abbreviations and acronyms are prohibited

Short forms are banned. Every abbreviation **must** be expanded into full words.
An abbreviation is any token that is not a complete English word.

This applies to **every** identifier kind: variables, parameters, functions,
classes, files, packages, database columns, protocol field names and resource
names.

#### 3.2.1 Expansion table (non-exhaustive — extend it, never break it)

| Prohibited | Required |
| --- | --- |
| `id`, `Id` | `identifier`, `Identifier` |
| `seq` | `sequenceNumber` |
| `ts`, `t` | `occurredAt`, `recordedAt` |
| `msg` | `message` |
| `req` | `request` |
| `res`, `resp` | `response` |
| `cfg`, `config` | `configuration` |
| `auth` | `authentication` **or** `authorization` (never a single ambiguous word) |
| `db` | `database` |
| `tx`, `txn` | `transaction` |
| `ctx` | `context` |
| `err` | `error` |
| `num` | `number` |
| `str` | `text` |
| `tmp`, `temp` | `temporary` |
| `btn` | `button` |
| `img` | `image` |
| `vm` | `viewModel` |
| `svc` | `service` |
| `dto` | `dataTransferObject` |
| `ws` | `webSocket` |
| `os` | `operatingSystem` |
| `ip` | `internetProtocolAddress` |
| `ui` | `userInterface` |
| `sdk` | `softwareDevelopmentKit` |
| `api` | `applicationProgrammingInterface` — or, better, the concrete noun |
| `impl` (suffix) | the concrete technology, e.g. `RoomHistoryRepository` |
| `misc`, `utils`, `helpers`, `common` | a precise, purpose-named package |

#### 3.2.2 The narrow allow-list of initialisms

A small set of initialisms is permitted **only** because they name a protocol or
format by its universally recognised name and have no natural long synonym.
They **must** appear as a complete, Kotlin-cased token, never as a fragment.

`Http`, `Https`, `Json`, `Xml`, `Html`, `Css`, `Tcp`, `Udp`, `Tls`, `Uri`,
`Url`, `Uuid`, `Mime`, `Sql`, `Rest`

```kotlin
// Permitted — complete tokens, Kotlin-cased
data class HttpMethod(val value: String)
val connectionUrl: Url
val requestJsonPayload: JsonElement

// Must not — fragments or shouting capitalisation
val httpURL: String
val JSONPayload: String
val TlsCfg: TlsConfiguration
```

Any initialism outside this list is a violation. When in doubt, spell it out.

### 3.3 Case rules by declaration kind

| Declaration | Convention | Example |
| --- | --- | --- |
| Package | lower case, no underscores | `xin.ctkqiang.burpsuite.domain.event` |
| Class, interface, object, enum | `PascalCase` | `HistoryItemObserved` |
| Function, property, parameter, local | `camelCase` | `observeHistoryItems` |
| Constant (`const val`, top-level immutable) | `SCREAMING_SNAKE_CASE` | `DEFAULT_REMOTE_PORT` |
| Type parameter | single capital letter only if truly generic | `T`, `State`, `Event` |
| Value class for an identifier | `<Noun>Identifier` | `HistoryIdentifier` |
| Composable | `PascalCase` noun, no verb | `HistoryListScreen` |
| Test function | backticked descriptive sentence | `` `forwarding an intercept emits InterceptForwarded` `` |
| Database column | `snake_case`, full words | `event_identifier`, `sequence_number` |
| Protocol field | `camelCase`, full words | `eventIdentifier`, `sequenceNumber` |
| Resource file | lower case with underscores | `values-zh/strings.xml` |

### 3.4 Suffixes that carry meaning

These suffixes are reserved; do not use them for anything else.

| Suffix | Applies to | Meaning |
| --- | --- | --- |
| `Identifier` | every identifier type and property | strongly typed identity |
| `Flow` | `Flow` properties | an observable stream, not a snapshot |
| `Repository` | data access abstractions | persistence port |
| `Gateway` | outbound remote abstraction | remote port |
| `Projection` | read-model builders | event reducer |
| `Reducer` | pure reduction functions | `(State, Event) -> State` |
| `Handler` | command handlers | one command, one responsibility |
| `Effect` | Compose side effects | one-shot, not state |

### 3.5 Names we never use

`Manager`, `Helper`, `Utility`, `Util`, `Processor`, `Data`, `Object`,
`Thing`, `Misc`, `Common`, `Impl`.

A class named `ConnectionManager` tells a reviewer nothing. A class named
`RemoteSessionRegistry` tells them exactly what invariant it protects.

### 3.6 Boolean naming

Booleans **must** read as a yes/no question.

```kotlin
val isConnected: Boolean
val hasBeenArchived: Boolean
val shouldRetryCommand: Boolean
val canForwardIntercept: Boolean
```

### 3.7 Strongly typed identifiers

Raw `String` must not be used to carry identity across a boundary. Use
`@JvmInline value class` so the compiler prevents mixing kinds of identity.

```kotlin
/**
 * Identifies one event inside the append-only event journal.
 *
 * The value is stable for the lifetime of the event and is used together with
 * [sequenceNumber] to deduplicate at-least-once delivery.
 */
@JvmInline
value class EventIdentifier(val value: String)

/** Identifies one command execution, enabling idempotent retries. */
@JvmInline
value class OperationIdentifier(val value: String)

/** Identifies one HTTP history entry as observed by Burp. */
@JvmInline
value class HistoryIdentifier(val value: String)

/** Identifies one intercept item owned by the Burp runtime. */
@JvmInline
value class InterceptIdentifier(val value: String)

/** Identifies one paired device. */
@JvmInline
value class DeviceIdentifier(val value: String)
```

---

## 4. Comment and documentation rules

Comments are **comprehensive and mandatory**. They are not optional decoration;
they are part of the deliverable.

### 4.0 Language of comments

**All comments, KDoc blocks and file headers must be written in Chinese
(简体中文).** This applies to `plugins/`, `client/` and any Kotlin file in the
repository.

Rationale: the author and the primary reviewers operate in Chinese, and a
comment that the reader cannot skim at full speed is a comment that will not be
read. Identifier names stay in English (see §3) so the code remains greppable
and interoperable; the *prose around* the code is Chinese.

- KDoc `@param` / `@return` / `@throws` / `@property` tags keep their English
  tag keywords, because those are parsed by tooling. The text after the tag is
  Chinese.
- Wire identifiers, type strings, file names and identifiers are never
  translated. `intercept.forwarded` stays `intercept.forwarded`.
- Chinese comments must not smuggle in abbreviations that §3.2 bans from code.
  Write 「拦截项标识符」, not 「拦截ID」.

### 4.1 Every public declaration has KDoc

Public classes, interfaces, functions, properties and value classes **must**
carry KDoc. KDoc **must** document:

1. **What** the declaration is, in one sentence.
2. **Why** it exists and which architectural role it plays.
3. **Contract**: valid inputs, invariants, units, nullability, thread-safety.
4. **Failure mode**: when it throws or returns a failure.

```kotlin
/**
 * 向本地只追加事件日志批量写入事件。
 *
 * 实现必须保证「整批写入」与「一条不写」二者之一成立，绝不出现写一半的中间态。
 * 事件一经写入不可修改、不可删除：日志是唯一事实来源，所有投影都由它重建。
 *
 * 本函数可从任意协程调度器调用，实现自行负责切换调度器，绝不阻塞调用方线程。
 *
 * @param events 待写入的事件列表，必须按 sequenceNumber 升序排列。
 * @throws EventStoreFailure 底层事务提交失败时抛出。
 */
suspend fun appendEvents(events: List<StoredEvent>)
```

### 4.2 File header

Every non-trivial Kotlin file **must** start with a header block stating the
module, the architectural layer and the reason the file exists.

```kotlin
/**
 * Burp Remote —— 协议层 / 事件
 *
 * 声明与历史记录相关的事实类型全集。这些类型是 Burp 插件与移动端之间的线上契约，
 * 因此除非提升协议版本号，任何 @SerialName 都不允许改动。
 *
 * @author 钟智强
 */
```

### 4.3 Comments explain *why*, never *what*

```kotlin
// 禁止——只是复述代码，且会随代码腐化
// 计数器加一
counter += 1

// 正确——记录后来者无法从代码推断出的隐含约束
// 检查点必须与投影更新放在同一个事务里推进。若进程在两者之间崩溃，
// 检查点会声称投影已推进而实际没有，事件将被静默丢弃。
projectionCheckpointDao.updateCheckpoint(projectionName, sequenceNumber)
```

### 4.4 Domain events are documented as facts

Every event type **must** document the fact it records and the moment it
occurred, so that a reviewer can order it on the timeline without reading code.

```kotlin
/**
 * 记录 Burp 已将某个被拦截的请求放行至目标服务器这一事实。
 *
 * 这是「事实」而非「请求」：客户端绝不能乐观地自行产生该事件。只有当 Montoya
 * 运行时确认放行成功后，插件才发布它。
 *
 * @property interceptIdentifier 本次被放行的拦截项。
 * @property occurredAt Burp 实际执行放行的时刻。
 */
data class InterceptForwarded(
    val interceptIdentifier: InterceptIdentifier,
    val occurredAt: Instant,
) : InterceptEvent
```

### 4.5 Security-sensitive code must say so

Any code that touches credentials, tokens, request bodies or the local database
**must** carry a comment naming the threat it mitigates.

```kotlin
// 脱敏只作用于「导出副本」。已归档的原始报文永不改写——即使用户已执行分享，
// 其证据链仍须保持逐字节精确。见 SECURITY.md「敏感数据处理」。
val redactedExportPayload = sensitiveDataRedactor.redact(exportPayload)
```

### 4.6 Forbidden comments

- Commented-out code. Delete it; version control remembers.
- Redundant restatements of the declaration name.
- `TODO` without an owner and a tracking reference. Use
  `// TODO(钟智强): <待办动作> —— <issue 链接>`.
- Banner art, ASCII dividers, emoji decorations.
- Comments that describe a plan instead of the code that exists.
- Comments written in any language other than Chinese (see §4.0).

---

## 5. Event sourcing rules

Event sourcing is the backbone of both products. The rules below are not
stylistic; breaking them breaks correctness.

### 5.1 Commands are intent; events are facts

| | Command | Event |
| --- | --- | --- |
| Tense | imperative, present | past |
| Meaning | "perform this" | "this happened" |
| Naming | `ForwardIntercept` | `InterceptForwarded` |
| Produced by | the client or a user action | the authoritative runtime |
| Idempotency key | `OperationIdentifier` | `EventIdentifier` + `sequenceNumber` |

**Must not** name an event after a command. **Must not** let the client fabricate
an event. The client may only store events it received, or events describing its
own local actions (for example `HistoryBookmarked`).

### 5.2 Events are immutable

Every event **must** be an immutable `data class` implementing a `sealed`
interface. Events use `val` exclusively, expose no mutable collections and hold
no behaviour beyond derived read-only helpers.

```kotlin
sealed interface DomainEvent {
    val eventIdentifier: EventIdentifier
    val sequenceNumber: Long
    val occurredAt: Instant
}
```

### 5.3 The event envelope

Every transported event **must** carry this envelope. Field names are part of
the protocol contract and follow §3.2 (full words, no abbreviations).

```json
{
  "protocolVersion": 1,
  "eventIdentifier": "event_01J",
  "sequenceNumber": 5003,
  "occurredAt": 1757660000000,
  "eventType": "intercept.forwarded",
  "aggregateType": "intercept",
  "aggregateIdentifier": "intercept_123",
  "payload": {}
}
```

### 5.4 The event store is append-only

- **Must** provide only `appendEvents` and read operations. There is no update
  and no delete.
- `eventIdentifier` **must** be unique.
- `sequenceNumber` **must** be unique and monotonic per source.
- Appending an event and advancing its projection checkpoint **must** happen in
  a single transaction, so an event can never be stored without its projection,
  and a projection can never claim progress it did not make.

### 5.5 At-least-once delivery and idempotency

The network gives at-least-once delivery. Design for it; never assume
exactly-once.

- The client **must** deduplicate incoming events by `eventIdentifier` and
  `sequenceNumber`.
- A gap in `sequenceNumber` **must** trigger a resume request, never a silent
  continue.
- Every command **must** carry an `OperationIdentifier`. The plugin **must**
  record command outcomes in an operation log and **must** return the recorded
  result instead of re-executing a destructive action.

### 5.6 Reducers and projections

- A reducer **must** be a pure function: `(State, Event) -> State`. No I/O, no
  clock reads, no randomness.
- A projection **must** be fully rebuildable by replaying the event journal from
  sequence zero.
- A projection **must not** be treated as a source of truth. If it disagrees
  with the journal, the journal wins and the projection is rebuilt.
- Each projection **must** persist its own checkpoint
  (`projectionName`, `lastSequenceNumber`, `updatedAt`).

### 5.7 Replay and snapshots

- Replay **must** be a first-class, tested operation.
- Snapshots are an optimisation, not a design requirement. **Must not** add
  snapshotting before measurement proves it is needed.

### 5.8 What must never be event-sourced

**Must not** attempt to reconstruct Burp's HTTP engine, proxy internals, complete
history database or project state. Burp owns that state. Event-source only the
**remote control and synchronisation layer**, plus the client's own local
actions.

---

## 6. Architecture and dependency rules

### 6.1 Direction of dependency

```text
userInterface / feature  ──▶  domain  ◀──  data
```

- `domain` is the centre. It **must not** depend on Android, Compose, Room,
  OkHttp, Montoya or `kotlinx.serialization`'s runtime annotations.
- `data` implements interfaces declared by `domain`.
- `feature` depends on `domain` and `userInterface`.
- The application module is the only place allowed to assemble everything.

### 6.2 Ports and adapters

`domain` declares ports (interfaces). `data` and `plugins/` provide adapters.

```kotlin
/**
 * Port: the append-only event journal.
 *
 * The domain depends on this abstraction only. Persistence technology, storage
 * format and transaction semantics are adapter concerns.
 */
interface EventStore {
    suspend fun appendEvents(events: List<StoredEvent>)
    suspend fun readEventsAfter(sequenceNumber: Long): List<StoredEvent>
    suspend fun latestSequenceNumber(): Long
}
```

```kotlin
/**
 * Port: the remote Burp extension.
 *
 * Implementations transport commands and events. The domain never learns
 * whether that transport is REST, WebSocket or an in-process fake.
 */
interface BurpRemoteGateway {
    suspend fun executeCommand(command: RemoteCommand): CommandResult
    fun observeEvents(): Flow<RemoteEvent>
}
```

### 6.3 Forbidden couplings

- `domain` → `data`
- `domain` → `android` / `compose` / `room` / `okhttp` / `montoya`
- a Composable → a Room `@Dao`
- a Montoya type → a JSON serializer → the client (this creates catastrophic
  protocol coupling; always map Montoya objects through an adapter first)

---

## 7. Kotlin code structure

### 7.1 Files and packages

- One top-level public declaration per file; the filename **must** match the
  declaration (`ForwardIntercept.kt` contains `ForwardIntercept`).
- Related small declarations may share a file only when they form one closed
  hierarchy (for example `HistoryEvents.kt`).
- Packages are lowercase, singular-domain-named and map one-to-one onto
  directories.
- **Must not** create `utils`, `helpers`, `misc` or `common` packages.
- **Must not** use wildcard imports.

### 7.2 Immutability

- `val` by default; `var` only for a locally scoped, provably confined value.
- Collections crossing a boundary **must** be immutable.
- `data class` for values, `sealed interface` for closed hierarchies, `enum
  class` for closed scalar sets, `value class` for identity and units.
- **Must not** expose mutable collections in a public API.

### 7.3 Nullability and errors

- `!!` is forbidden outside a documented, immediately-preceding null check.
- **Must not** swallow exceptions. Either handle them meaningfully or let them
  propagate.
- Domain failures **must** be modelled explicitly with a sealed result type.

```kotlin
/**
 * The outcome of executing a command against the remote runtime.
 *
 * Modelled as a closed type rather than an exception because a failed command
 * is an expected domain outcome, not a defect.
 */
sealed interface CommandResult {
    data class Succeeded(val operationIdentifier: OperationIdentifier) : CommandResult
    data class Rejected(val reason: RejectionReason) : CommandResult
    data class Failed(val error: RemoteError) : CommandResult
}
```

### 7.4 Functions

- Public functions **must** declare explicit return types.
- Functions **must** do one thing; a function that needs "and" in its summary
  must be split.
- Prefer expression bodies for pure one-liners.
- Side-effecting functions **must** be named with a verb that reveals the effect
  (`persistEvent`, `sendCommand`).
- **Must not** use a boolean parameter to switch behaviour; prefer two clearly
  named functions or a sealed type.

### 7.5 Coroutines

- `suspend` functions **must** be main-safe: if they do blocking work, they move
  it to an injected dispatcher themselves.
- Dispatchers **must** be injected, never referenced directly, except in test
  code.
- `GlobalScope` is forbidden.
- Structured concurrency is mandatory; a launched coroutine **must** belong to a
  scope whose lifetime is owned by the caller.
- `Flow` exposed across a boundary **must** be cold and must not leak upstream
  failures; map failures into the domain's error model.

### 7.6 Serialisation

- Use `kotlinx.serialization`.
- Serial names are protocol contracts. Changing a `@SerialName` **must** be
  accompanied by a protocol version bump in `protocol/versions/`.
- **Must not** expose Montoya class names on the wire. Map to protocol DTOs
  through an adapter.

---

## 8. Compose and MVI rules (`client/`)

### 8.1 Unidirectional data flow

```text
composable → userInterfaceIntent → viewModel → command → domain
domain → event → projection → userInterfaceState → composable
```

- State **must** be immutable and exposed as a single
  `StateFlow<...UserInterfaceState>`.
- One-shot actions (navigation, toasts) **must** be modelled as a sealed
  `...UserInterfaceEffect`, never as state.

### 8.2 Naming note

Because abbreviations are prohibited, Compose types are named
`HistoryUserInterfaceState` and `HistoryUserInterfaceIntent` rather than the
widespread `HistoryUiState` / `HistoryUiIntent`. This is intentional and
enforced by §3.2.

### 8.3 Rules

- Composables **must** be stateless wherever possible; hoist state to the
  ViewModel.
- **Must not** place business logic, I/O or command construction inside a
  composable.
- **Must not** hardcode a colour. Read it from the theme.
- **Must not** hardcode user-visible text. Read it from string resources.
- Every screen **must** ship a `@Preview` for the light and dark colour schemes.
- `LazyColumn` items **must** provide a stable `key`.

---

## 9. Localisation rules

- Default locale is `en`. Supported locales are `en`, `zh`, `de`.
- Every user-visible string **must** live in `strings.xml`.
  `values/`, `values-zh/`, `values-de/`.

```kotlin
// Must not
Text("Connected")

// Must
Text(text = stringResource(R.string.connection_state_connected))
```

- Formatting and plurals **must** use `<plurals>` and positional format
  arguments, never string concatenation.
- `domain` and `plugins/` **must not** put user-interface language **on the
  wire**. They emit stable machine codes; the client maps codes to localised
  strings. This constrains the protocol, not the operator-facing tab — see §9.1.
- New strings **must** be added to all three locales in the same change.

### 9.1 Plugin-side resource bundles

The Burp tab is operator-facing UI and therefore **is** localised, through the
JVM resource bundle rather than `strings.xml`:

```text
plugins/burp-remote-extension/src/main/resources/
├── messages.properties         # en — base bundle, and the fallback for every other locale
├── messages_zh.properties
└── messages_de.properties
```

- The locale comes from the operating system (`Locale.getDefault()`), resolved
  once at the composition root and injected. A widget **must not** read the
  ambient locale itself.
- A widget **must not** contain a user-visible string literal. Every one of them
  goes through the localisation lookup.
- Every key **must** exist in all three bundles. A test compares the key sets, so
  a missing translation fails the build instead of showing a raw key in the UI.
- Resource keys are `snake_case`; formatting uses positional arguments (`{0}`,
  `{1}`), never string concatenation.
- Bundles are read as UTF-8 (JDK 9 and later). The same files **must not** be read
  through `Properties.load`, which still decodes ISO-8859-1.

---

## 10. Theme rules

- Theme modes are exactly `AUTOMATIC`, `LIGHT`, `DARK`. `AUTOMATIC` follows the
  operating-system setting.
- The brand colour is Burp Suite orange. It **must** be defined once as a colour
  token and referenced through the Material 3 colour scheme.
- **Must not** hardcode orange in a composable.
- Typography, shapes, spacing and colour **must** all be provided by the theme.
- The theme preference is user data and lives in the settings layer (DataStore),
  never in a composable.

---

## 11. Protocol and networking rules

- The default remote port is **`9000`**. The port **must** be configurable and
  **must not** be duplicated as a magic number; expose it as
  `DEFAULT_REMOTE_PORT` in the shared protocol module.
- Transport is REST for commands, queries and snapshots, and WebSocket for the
  live event stream.
- Every message **must** carry `protocolVersion`.
- Protocol type strings **must** be stable, dot-separated and
  implementation-independent: `history.item.observed`, `intercept.forwarded`.
- **Must not** expose implementation names (`ProxyHttpRequestResponse`).
- Live events **must** carry metadata only. Large request and response bodies
  **must** be fetched on demand by identifier.
- The client **must** request a snapshot when the extension can no longer serve
  the requested sequence range.

---

## 12. Security rules

This system remotely controls Burp Suite. Treat the local network as untrusted.

- **Must not** commit secrets, keys, tokens or credentials. **Must not** log
  request or response bodies, cookies, `Authorization` headers or tokens by
  default.
- **Must not** include credentials in crash reports or analytics.
- Pairing, authentication and authorisation are mandatory before any control
  command is accepted.
- Control actions require an explicit `OperationIdentifier` and rate limiting.
- Redaction applies to exported copies only. The archived original **must**
  remain byte-exact.
- The local database is sensitive. Keep it in application-private storage and
  route all access through the repository port so encryption can be introduced
  later without touching the domain.

---

## 13. Testing rules

- Unit tests cover: command handlers, reducers, aggregates, serialisers,
  projection logic, filters, redaction and event ordering.
- Integration tests cover: Room, the event store, projection transactions,
  WebSocket and REST transport, and protocol compatibility.
- Every event-store and projection test **must** include an idempotency case:
  delivering the same event twice must not change the projection.
- Every projection **must** be tested by rebuilding it from an empty database
  and comparing against the incrementally built projection.
- Tests **must** be deterministic. A test **must not** depend on wall-clock time;
  inject a `TimeSource`.
- Test names are backticked sentences describing the behaviour.
- `plugins/` **must** be testable without a running Burp instance; the Montoya
  boundary is an adapter and **must** be faked behind a port.

---

## 14. Git and workflow rules

- Commit messages follow Conventional Commits:
  `feat(protocol): add resume-from handshake`.
- Scopes: `plugins`, `client`, `protocol`, `docs`, `build`.
- One logical change per commit. **Must not** mix formatting churn with behaviour
  changes.
- **Must not** commit generated files, build output, keystores, or local
  configuration.
- Attribution stays with the author: 钟智强.

---

## 15. Formatting and enforcement

- `.editorconfig` defines indentation (4 spaces for Kotlin, 2 for XML/JSON) and
  line endings (LF).
- `ktlint` enforces formatting and import ordering.
- `detekt` enforces complexity, naming and forbidden-pattern rules. Custom
  `detekt` rules **must** encode §3.2 (no abbreviations) and §4 (KDoc presence)
  once the codebase exists.
- The build **must** fail on a violation. Formatting is never a review comment;
  it is a build gate.

Suggested gates:

```bash
./gradlew ktlintCheck detekt
./gradlew test
```

---

## 16. Forbidden pattern checklist

Use this list in review. Any "no" is a blocker.

- [ ] No abbreviations or acronyms outside the §3.2.2 allow-list.
- [ ] No single-letter identifiers except true generic type parameters.
- [ ] No raw `String` carrying identity across a boundary.
- [ ] No comments that restate the code.
- [ ] No comment, KDoc block or file header written in a language other than Chinese (§4.0).
- [ ] No public declaration without KDoc.
- [ ] No `!!`, no swallowed `catch`, no `GlobalScope`.
- [ ] No mutable collections in a public API.
- [ ] No command named like an event, or event named like a command.
- [ ] No event store update or delete path.
- [ ] No projection treated as a source of truth.
- [ ] No Montoya type crossing the protocol boundary.
- [ ] No hardcoded colour, string or port.
- [ ] No `utils`, `helpers`, `misc`, `common` package.
- [ ] No `Impl`-suffixed class name.
- [ ] No secrets, tokens or bodies in logs.
- [ ] No test that depends on wall-clock time or ordering of unrelated work.
