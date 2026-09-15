# Burp Remote

Control Burp Suite from your phone. A Burp extension + Android app that streams proxy history, intercept decisions, and repeater entries to a mobile device over your local network.

![Extension loaded in Burp Suite](docs/images/plugins_screenshot/1.png)

![Selecting the JAR to load](docs/images/plugins_screenshot/2.png)

![Pairing screen with QR code](docs/images/plugins_screenshot/3.png)

![Mobile dashboard](docs/images/mobile_app_screenshot/Screenshot_2026-09-15-13-00-45-590_xin.ctkqiang.burpsuite.remote.mobileapp.jpg)

![Mobile connection screen](docs/images/mobile_app_screenshot/Screenshot_2026-09-15-13-00-53-661_xin.ctkqiang.burpsuite.remote.mobileapp.jpg)

[中文](README.md)

## What it does

Burp Remote turns Burp Suite into something you can use from across the room. The extension exposes a WebSocket event stream and REST API on your machine; the Android app connects to it and lets you browse proxy history, forward or drop intercepted requests, and run repeater entries — all without touching your laptop.

Picture this: you're running a pentest, Burp is proxying traffic on your laptop. You get up to grab coffee, and an intercept fires — your phone buzzes, you glance at it, tap "Forward," and Burp resumes before you're back. Or you tuned a Repeater request earlier, and you want to check the latest execution result from the train.

Two parts, one repo:

- **`plugins/burp-remote-extension`** — a Burp Suite Java extension (built with Kotlin). It publishes events through an in-memory event stream and accepts control commands over HTTP REST. Once loaded, it adds a "Burp Remote" tab to Burp with a pairing QR code and runtime parameters.
- **`client/mobileapp`** — an Android app (Jetpack Compose, MVI architecture, Clean Architecture layering). It connects to the extension, ingests events into a local Room database, and renders them through a state-driven UI. Works offline for already-synced data and resyncs on reconnect.

## Requirements

- JDK 17 (Burp Suite 2025.x runs on 17; higher bytecode versions get rejected at load time)
- Burp Suite Community or Professional (2025.x or later)
- Android device running API 26 (Android 8.0) or above
- Both machines on the same LAN (the extension listens on `0.0.0.0:9000` by default)

## Build

Two scripts handle everything — they find JDK 17, run quality gates (ktlint + detekt + unit tests), and print artifact paths.

### Extension JAR

```bash
scripts/build-burp-extension.sh
```

Output: `build/plugins/burp-remote-extension/libs/burp-remote-extension-0.1.0.jar`

Load it in Burp: **Extensions → Installed → Add → Java → select the JAR**.

The script runs the `packageExtension` task, which bundles ktlint, detekt, unit tests, and shadowJar into one atomic action — if any gate fails, no JAR is produced. You never get a broken artifact in your hand.

### Android APK

```bash
scripts/build-mobile-app.sh
```

Output: `build/mobileapp/app/outputs/apk/debug/app-debug.apk`

Install with `adb install -r <path>`.

Both scripts accept a `JAVA_HOME_FOR_BUILD` environment variable to point at JDK 17, and pass extra arguments through to Gradle.

## Usage

1. Load the extension JAR in Burp Suite. It starts listening on `0.0.0.0:9000`.
2. Open the "Burp Remote" tab in Burp to see the pairing QR code and pairing code.
3. Install the APK on your phone, open the app, scan the QR code or enter the pairing code manually.
4. The app connects, syncs events, and you're live.

The pairing code is valid for 5 minutes, 8 alphanumeric characters (confusable characters I/O/0/1/L/U are excluded). After scanning once, the device identity is registered on the plugin side — reconnecting doesn't require re-scanning, unless you clear the pairing registry.

Once connected, you get:

- **Proxy History**: streamed in real time. Each entry has method, URL, status code, timestamp. Tap for full request and response bodies.
- **Intercept Queue**: intercepted requests show up on your phone. Forward or Drop them — the decision goes back to Burp for execution.
- **Repeater**: send history entries to Repeater, view and edit request bodies, execute. Results (status code, duration, response body) sync back.

## REST API

The extension exposes these endpoints at `http://<your-IP>:9000`:

| Method | Path | Purpose |
|--------|------|---------|
| GET | `/v1/status` | Runtime status (protocol version, listening port, paired devices) |
| POST | `/v1/pair` | Submit pairing code, get device identity |
| GET | `/v1/capabilities` | Server capability declaration |
| GET | `/v1/snapshot` | Full snapshot (used when resync fails) |
| GET | `/v1/history` | Proxy history list |
| GET | `/v1/history/{id}` | Single history item (request + response body) |
| POST | `/v1/scope/{id}` | Send history item to Repeater |
| GET | `/v1/intercepts` | Intercept queue |
| GET | `/v1/intercepts/{id}` | Single intercept detail |
| POST | `/v1/intercepts/{id}/forward` | Forward intercept |
| POST | `/v1/intercepts/{id}/drop` | Drop intercept |
| POST | `/v1/intercepts/{id}/modify` | Modify and forward |
| POST | `/v1/repeater` | Create repeater entry |
| GET | `/v1/repeaters` | Repeater list |
| GET | `/v1/repeaters/{id}` | Single repeater detail |
| POST | `/v1/repeaters/{id}/execute` | Execute repeater request |

WebSocket event stream at `ws://<your-IP>:9000/v1/events`.

## Event Types

Events pushed by the extension over WebSocket:

| Event type | Source | Meaning |
|-----------|--------|---------|
| `history.item.observed` | Proxy history publisher | New or updated entry in Burp proxy history |
| `intercept.created` | Intercept proxy handler | A new request was intercepted |
| `intercept.forwarded` | Intercept proxy handler | Intercepted request was forwarded |
| `intercept.dropped` | Intercept proxy handler | Intercepted request was dropped |
| `repeater.created` | REST API | A new repeater entry was created |
| `repeater.execution.started` | REST API | Repeater request started executing |
| `repeater.execution.completed` | REST API | Repeater request finished executing |

Each event carries a globally monotonic sequence number used by the mobile app for resumption.

## Architecture

### Event Sourcing

The extension uses an event-sourced model. Every proxy observation, intercept decision, and repeater action gets stamped with a monotonically increasing sequence number and pushed to connected clients over WebSocket. Sequence numbers are allocated centrally by `InMemoryRemoteEventStream` — history, intercept, and repeater publishers all share one AtomicLong. This used to be three independent counters, which caused collisions and silently dropped events at the mobile sync layer.

The event stream keeps the most recent 1024 events in memory; older events scroll off. If the phone disconnects briefly (seconds to tens of seconds) and reconnects, it resumes from the last sequence number. If it was gone too long and the window has scrolled past, the plugin tells the phone to take a full snapshot (`/v1/snapshot`) and then resume.

### Connection Lifecycle

The handshake is fixed: CONNECT → AUTHENTICATE → RESUME → event stream.

Authentication uses device identity: after pairing, the plugin issues a `DeviceIdentifier` that must be presented on every subsequent connection. Devices not in the paired registry are rejected.

Disconnection is symmetric: the mobile app sends a WebSocket Close frame (wrapped in `NonCancellable` to ensure delivery), and the plugin server concurrently reads `incoming` frames so it detects the close immediately, tears down the session, releases the connection slot, and disassociates the device. No waiting for ping timeout (up to 30 seconds).

### Security

| Measure | What it does |
|---------|-------------|
| Device pairing | One-time pairing code, 5-minute expiry, confusable-character-free alphabet (no I/O/0/1/L/U) |
| Authentication | Every connection carries device identity; unregistered devices are rejected |
| Rate limiting | Per-device token bucket, burst 20, steady-state 5/sec |
| Idempotency | Operation ID dedup; retries return the original result instead of re-executing |
| Audit logging | Every command logs device, type, operation ID, and result |
| LAN-only | No TLS (the plugin serves plain HTTP); not meant to be exposed to the public internet |

### Mobile App Layering

The mobile app follows Clean Architecture with four layers:

- **app** — entry point, navigation, DI container, foreground service, home screen widget
- **data** — repository implementations, Room database, Ktor client, event ingester
- **domain** — use cases, entities, settings, connection state enum
- **ui** — theme system, design system, shared components

Features are split into independent modules (under `feature/`), each depending only on domain and UI — never on data directly.

## Technical Diagrams

PlantUML sources are in `docs/plantuml/`, rendered PNGs in `docs/images/diagrams/`.

### Architecture Overview

![Architecture Overview](docs/images/diagrams/architecture-en.png)

### Pairing Flow

![Pairing Flow](docs/images/diagrams/pairing-en.png)

### Connection Lifecycle

![Connection Lifecycle](docs/images/diagrams/connection-en.png)

### Event Synchronization & Resync

![Event Synchronization](docs/images/diagrams/event-sync-en.png)

## Feature Modules

| Module | What it does |
|--------|-------------|
| `feature/connection` | QR scan pairing, connection status display |
| `feature/dashboard` | Home panel: connection state, stats summary, navigation |
| `feature/history` | Proxy history list + detail (request/response bodies) |
| `feature/intercept` | Intercept queue: view, forward, drop, modify |
| `feature/repeater` | Repeater list + detail + create + execute |
| `feature/settings` | Theme (light/dark + 8 flavors), connection config |
| `feature/sharing` | Share request/response |
| `feature/screenshot` | Screenshot |
| `feature/archive` | Archive |

## Theme System

Light/dark mode and flavor are orthogonal: light/dark controls base luminance, flavor controls accent color and overall tone.

Three light/dark modes: follow system, light, dark.

Eight flavors:

| Flavor | Accent | Personality |
|--------|--------|-------------|
| Burp Classic | Orange `#FF6633` | Brand default |
| Cyber Cyan | Cyan `#22D3EE` | Cold cyberpunk |
| Forest Emerald | Emerald `#34D399` | Natural, grounded |
| Royal Violet | Violet `#A78BFA` | Elegant, mysterious |
| Rose Gold | Rose `#FB7185` | Warm, soft |
| Ocean Blue | Blue `#60A5FA` | Calm, clear |
| Solar Amber | Amber `#FBBF24` | Sunset warm |
| Monochrome | None | Most restrained |

Each flavor has both a light and dark color scheme. Semantic colors (success=green, warning=yellow, error=red, info=blue) stay consistent across flavors, as do syntax highlighting colors.

The bottom navigation bar uses a liquid glass effect (Haze real-time blur + translucent surface + top highlight gradient + bottom inner shadow + elevation shadow), with parameters tuned separately for light and dark.

## Home Screen Widget

An Android home screen widget shows target host, live request count, intercept count, and saved count. Tapping it opens the dashboard. The widget reads from SharedPreferences — no persistent background service required.

## Localization

Five languages: English (default), Chinese, German, Japanese, Mongolian. All feature modules have translated `strings.xml` files.

## Release

Pushing a tag triggers GitHub Actions:

```bash
git tag v0.1.0
git push origin v0.1.0
```

CI builds the extension JAR (with quality gates) and APK using JDK 17, then attaches both to a GitHub Release.

## Project Layout

```
.
├── plugins/
│   └── burp-remote-extension/     # Burp extension (Kotlin, JDK 17)
│       ├── src/main/              # production code
│       ├── src/test/              # unit tests
│       └── src/harness/           # end-to-end harness (no Burp dependency)
├── client/
│   └── mobileapp/                 # Android app (Compose, MVI)
│       ├── app/                   # entry, navigation, DI, widget
│       ├── data/                  # repo impls, Room, Ktor client
│       ├── domain/                # use cases, entities, settings
│       ├── ui/                    # theme (8 flavors × 2 modes), design system
│       ├── core/                  # shared: model, protocol, common
│       └── feature/              # feature modules (9 screens)
│           ├── connection/        # QR pairing
│           ├── dashboard/         # home panel
│           ├── history/          # proxy history
│           ├── intercept/        # intercept queue
│           ├── repeater/         # repeater
│           ├── settings/         # theme & config
│           ├── sharing/          # share
│           ├── screenshot/       # screenshot
│           └── archive/          # archive
├── scripts/                       # build scripts (require JDK 17)
│   ├── build-burp-extension.sh
│   ├── build-mobile-app.sh
│   └── lib/                      # shared script library (JDK resolution)
├── .github/
│   ├── workflows/release.yml     # tag-triggered release
│   └── ISSUE_TEMPLATE/           # bilingual issue templates
└── docs/                          # screenshots
```

## Tech Stack

**Extension**: Kotlin, Ktor (CIO server + WebSockets + content negotiation), kotlinx.serialization (JSON), ZXing (QR codes), Montoya Burp API. Built with Gradle + Shadow plugin for fat JAR; quality gates via ktlint + detekt.

**Android app**: Kotlin, Jetpack Compose, Haze (liquid glass blur), Room (local database), Ktor client (WebSocket + REST), DataStore (preferences), CameraX + MLKit (QR scanning), MVI pattern with unidirectional data flow. Built with Gradle; minSdk 26, targetSdk 36.

## License

MIT

## Author

钟智强 (Johnmelodyme)
