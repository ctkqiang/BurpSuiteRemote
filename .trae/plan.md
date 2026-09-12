# Burp Remote

## Architecture & Implementation Plan

**Project name:** Burpsuite Remote

**Application ID:** `xin.ctkqiang.burpsuite`

**Author:** 钟智强

**License:** Free and open source (`isFree = true`, `isOpenSource = true`)

**Repository layout (single repository, two products):**

```text
BurpsuiteRemote/
├── .trae/                       # This directory — plan, rules, source of truth
│   ├── plan.md                  # Architecture and implementation plan
│   └── rules.md                 # Binding repository-wide rules
├── plugins/                     # Product 1 — Burp Suite remote server (Burp extension)
│   └── burp-remote-extension/   # Kotlin extension loaded by Burp Suite
├── client/                      # Product 2 — Android mobile application
│   └── mobileapp/               # Kotlin + Jetpack Compose application
├── protocol/                    # Shared wire contract (versioned)
├── docs/                        # User and operator documentation
└── scripts/                     # Build, release, and developer tooling
```

**Product 1 — Burp Suite remote server (the Burp plugin):** `plugins/burp-remote-extension`

**Product 2 — mobile application (the client):** `client/mobileapp`

**Platform:** Android

**UI:** Jetpack Compose

**Default port:** `9000` (single canonical value, `DEFAULT_REMOTE_PORT`)

**Default language:** English (`en`)

**Supported languages:**

- `en`
- `zh`
- `de`

**Theme:**

- Light
- Dark
- Orange brand identity (Burp Suite orange)
- Follow-system option

**Architecture:**

- Event Sourcing
- CQRS
- Clean Architecture
- Hexagonal / Ports & Adapters
- MVI / Unidirectional Data Flow
- Offline-first
- Local event journal
- SQLite via Room
- REST + WebSocket
- Burp Montoya extension
- Explicit command/event protocol

---

# 1. Product Definition

Burp Remote is a mobile remote-control and observation application for Burp Suite Community.

It is **not a Burp Suite replacement**.

It does not implement its own HTTP proxy.

It does not attempt to replace Burp's HTTP engine.

It does not become an independent interception engine.

Instead:

```text
                  BURP SUITE
              ┌─────────────────┐
              │ Proxy           │
              │ HTTP History    │
              │ Intercept       │
              │ Repeater        │
              │ HTTP Engine     │
              └────────┬────────┘
                       │
                 Montoya API
                       │
              ┌────────▼────────┐
              │ Burp Remote     │
              │ Extension       │
              └────────┬────────┘
                       │
                Remote Protocol
                       │
                 LAN / TLS
                       │
              ┌────────▼────────┐
              │ Android App     │
              │                 │
              │ Compose         │
              │ CQRS            │
              │ Event Store     │
              │ SQLite / Room   │
              └─────────────────┘
```

Burp is the authoritative runtime.

Android is the remote operator console.

---

# 2. Core Architectural Principle

The system has three different kinds of state.

## 2.1 Burp Runtime State

Owned by Burp.

Examples:

```text
Proxy
HTTP history
Intercept queue
Repeater
Request execution
Response execution
Burp project state
```

Android must not pretend to own this state.

---

## 2.2 Remote Control State

Owned by the Burp Remote extension.

Examples:

```text
connected devices
paired devices
remote sessions
event sequence
remote command status
capabilities
event journal
```

---

## 2.3 Mobile State

Owned by Android.

Examples:

```text
saved history
local search indexes
bookmarks
annotations
screenshots
beautified screenshots
local preferences
UI state
connection configuration
```

```text
BURP
  │
  │ authoritative runtime state
  ▼
REMOTE EXTENSION
  │
  │ events / commands
  ▼
ANDROID
  │
  │ local projections
  ▼
ROOM / SQLITE
```

---

# 3. Architectural Pattern

The project uses multiple patterns together.

```text
                    ┌───────────────────────┐
                    │     Jetpack Compose   │
                    │        UI Layer       │
                    └───────────┬───────────┘
                                │
                           UI Events
                                │
                    ┌───────────▼───────────┐
                    │      ViewModel        │
                    │        MVI            │
                    └───────────┬───────────┘
                                │
                             Command
                                │
                    ┌───────────▼───────────┐
                    │    Application Layer  │
                    │       CQRS Bus         │
                    └───────────┬───────────┘
                                │
                  ┌─────────────┴─────────────┐
                  │                           │
             Command Side                Query Side
                  │                           │
                  ▼                           ▼
          Command Handlers             Query Handlers
                  │                           │
                  ▼                           ▼
         Remote / Local Ports        Projection / Room
                  │                           │
                  ▼                           ▼
          Burp Extension              SQLite Database
                  │
                  ▼
               Events
                  │
                  ▼
            Event Store
                  │
                  ▼
              Reducers
                  │
                  ▼
          Materialized Views
                  │
                  ▼
               Compose
```

This is effectively:

```text
CQRS
+
Event Sourcing
+
MVI
+
Clean Architecture
+
Hexagonal Architecture
+
Offline-first
```

---

# 4. Why Event Sourcing?

Event sourcing fits here: Burp Remote needs **what happened**, not just current state.

Example:

```text
REQUEST_CREATED
       ↓
REQUEST_INTERCEPTED
       ↓
REQUEST_MODIFIED
       ↓
REQUEST_FORWARDED
       ↓
RESPONSE_RECEIVED
       ↓
HISTORY_SAVED
       ↓
ANNOTATION_ADDED
       ↓
SCREENSHOT_CREATED
```

The application can reconstruct a timeline.

This gives us:

```text
history
timeline
audit
reconnect
synchronization
debugging
offline projections
```

However:

## Do NOT event-source Burp itself.

Do not attempt to reconstruct:

```text
Burp HTTP engine
Burp proxy internals
Burp's complete HTTP history
Burp's internal project database
```

Burp already owns those.

Instead, event-source the **remote control and synchronization layer**.

---

# 5. CQRS

CQRS separates:

```text
COMMANDS
```

from:

```text
QUERIES
```

A command changes something.

A query reads something.

---

## 5.1 Commands

Examples:

```text
ConnectToBurp
PairDevice
ForwardIntercept
DropIntercept
ModifyIntercept
SendToRepeater
ExecuteRepeater
SaveHistory
BookmarkHistory
AnnotateHistory
CreateScreenshot
BeautifyScreenshot
ShareHistory
```

Command:

```text
ForwardIntercept(
    interceptIdentifier = "intercept_123",
    operationIdentifier = "operation_123"
)
```

The command means:

> Please perform this operation.

It does not mean:

> This operation succeeded.

---

# 6. Events

Events represent facts.

Examples:

```text
DevicePaired
DeviceConnected

HistoryItemObserved
HistoryItemSaved

InterceptCreated
InterceptModified
InterceptForwarded
InterceptDropped

RepeaterRequestCreated
RepeaterExecutionStarted
RepeaterExecutionCompleted

HistoryBookmarked
HistoryAnnotated

ScreenshotImported
ScreenshotBeautified
```

The distinction is:

```text
COMMAND

"Forward this request."

EVENT

"The request was forwarded."
```

Never confuse these.

---

# 7. Command Flow

Example:

```text
Android
  │
  │ ForwardInterceptCommand
  ▼
CommandBus
  │
  ▼
ForwardInterceptHandler
  │
  ▼
BurpRemoteClient
  │
  │ HTTP/WebSocket
  ▼
Burp Remote Extension
  │
  ▼
Montoya
  │
  ▼
Burp
  │
  │ actual execution
  ▼
Burp Remote Extension
  │
  │ InterceptForwardedEvent
  ▼
Android Event Stream
  │
  ▼
EventStore
  │
  ▼
Projection
  │
  ▼
Compose UI
```

The Android application does not invent:

```text
InterceptForwarded
```

It receives the fact from the authoritative side.

---

# 8. Event Sequence

Every event has a monotonic sequence number.

Example:

```text
5000 HistoryItemObserved
5001 InterceptCreated
5002 InterceptModified
5003 InterceptForwarded
5004 ResponseReceived
5005 HistoryItemSaved
```

Event envelope:

```json
{
  "protocolVersion": 1,
  "eventIdentifier": "event_01J...",
  "sequenceNumber": 5003,
  "occurredAt": 1757660000000,
  "eventType": "intercept.forwarded",
  "aggregateType": "intercept",
  "aggregateIdentifier": "intercept_123",
  "payload": {}
}
```

Synchronization depends on the sequence number.

---

# 9. Reconnection

Suppose Android received:

```text
5000
5001
5002
5003
```

Then Wi-Fi disappears.

Burp continues:

```text
5004
5005
5006
5007
```

Android reconnects with:

```text
RESUME_FROM=5003
```

The extension responds:

```text
5004
5005
5006
5007
```

Android applies them.

---

# 10. Resynchronization

If the extension no longer has the requested events:

```text
Android

lastSequence = 5003

        │
        ▼

RESUME_FROM 5003

        │
        ▼

Extension:

EVENTS_NO_LONGER_AVAILABLE
```

Then:

```text
Android
   │
   ▼
REQUEST_SNAPSHOT
   │
   ▼
Extension
   │
   ▼
CURRENT_STATE_SNAPSHOT
   │
   ▼
Android
   │
   ▼
Replace materialized projection
   │
   ▼
Resume events
```

This prevents the mobile database from drifting indefinitely.

---

# 11. Event Store

For the Android app:

```text
Room / SQLite
```

should contain a local event journal.

Example:

```text
remote_event_journal
```

Schema:

```text
event_identifier
sequence_number
aggregate_type
aggregate_identifier
event_type
event_version
timestamp
payload
metadata
received_at
```

Important:

```text
sequence_number UNIQUE
event_identifier UNIQUE
```

---

# 12. Event Store Data Structure

Conceptually:

```kotlin
data class StoredEvent(
    val eventIdentifier: EventIdentifier,
    val sequenceNumber: Long,
    val aggregate: AggregateReference,
    val eventType: EventType,
    val eventVersion: Int,
    val timestamp: Instant,
    val payload: ByteArray,
    val metadata: EventMetadata
)
```

Identifiers should be strongly typed.

Prefer:

```kotlin
@JvmInline
value class EventIdentifier(val value: String)

@JvmInline
value class OperationIdentifier(val value: String)

@JvmInline
value class HistoryIdentifier(val value: String)

@JvmInline
value class InterceptIdentifier(val value: String)

@JvmInline
value class DeviceIdentifier(val value: String)
```

Avoid:

```kotlin
String
```

everywhere.

Strong identifiers prevent accidental mixing of:

```text
historyIdentifier
eventIdentifier
operationIdentifier
deviceIdentifier
```

---

# 13. Aggregates

Do not make everything an aggregate.

Recommended aggregates:

```text
RemoteSession
Device
Intercept
RepeaterRequest
SavedHistory
```

HTTP history observed from Burp should primarily be treated as an external fact stream and materialized into local projections.

---

# 14. Event Types

Use sealed types.

Conceptually:

```kotlin
sealed interface DomainEvent

sealed interface ConnectionEvent : DomainEvent

data class DeviceConnected(...)
data class DeviceDisconnected(...)
data class DevicePaired(...)
data class DeviceUnpaired(...)

sealed interface HistoryEvent : DomainEvent

data class HistoryItemObserved(...)
data class HistoryItemSaved(...)
data class HistoryAnnotationAdded(...)

sealed interface InterceptEvent : DomainEvent

data class InterceptCreated(...)
data class InterceptModified(...)
data class InterceptForwarded(...)
data class InterceptDropped(...)

sealed interface RepeaterEvent : DomainEvent

data class RepeaterCreated(...)
data class RepeaterExecutionStarted(...)
data class RepeaterExecutionCompleted(...)

sealed interface ScreenshotEvent : DomainEvent

data class ScreenshotImported(...)
data class ScreenshotBeautified(...)
```

---

# 15. CQRS Command Structure

```kotlin
sealed interface Command

data class ConnectToBurp(...)
data class PairDevice(...)
data class ForwardIntercept(...)
data class DropIntercept(...)
data class ModifyIntercept(...)
data class SaveHistory(...)
data class BookmarkHistory(...)
data class AnnotateHistory(...)
data class SendToRepeater(...)
data class ExecuteRepeater(...)
data class ImportScreenshot(...)
data class BeautifyScreenshot(...)
data class ShareHistory(...)
```

Each command receives:

```text
operationIdentifier
```

Example:

```text
operationIdentifier = operation_01JXYZ
```

This enables idempotency.

---

# 16. Idempotency

Suppose Android sends:

```text
ForwardIntercept(op_123)
```

Network timeout occurs.

Android does not know whether Burp executed it.

It retries.

The extension receives:

```text
ForwardIntercept(op_123)
```

again.

The extension must recognize:

```text
op_123
```

and not execute the destructive action twice.

Therefore maintain:

```text
operation_log
```

with:

```text
operation_id
command_type
status
created_at
completed_at
result
```

States:

```text
RECEIVED
PROCESSING
SUCCEEDED
FAILED
```

---

# 17. Local SQLite Architecture

Use:

```text
Room
  ↓
SQLite
```

Do not write raw SQLite infrastructure unless a specific performance requirement justifies it.

Room provides:

```text
entities
DAOs
transactions
migration support
compile-time SQL verification
Flow integration
```

---

# 18. Database

Recommended database:

```text
BurpRemoteDatabase
```

Tables:

```text
remote_event_journal
operation_log

history
history_message

history_annotation
history_bookmark

intercept
intercept_message

repeater
repeater_execution

screenshot
screenshot_region

device
remote_session

projection_checkpoint

app_settings
```

---

# 19. History Model

Separate metadata from large message bodies.

Do not put everything into one giant row.

Use:

```text
history
```

for metadata:

```text
history_identifier
sequence_number
timestamp
host
method
scheme
path
status_code
mime_type
response_length
tls
destination_internet_protocol_address
listener_port
duration
edited
title
created_at
updated_at
```

Then:

```text
history_message
```

contains:

```text
history_identifier
request_headers
request_body
response_headers
response_body
encoding
```

This allows the list UI to load only metadata.

---

# 20. Lazy Message Loading

History list:

```text
GET /api/user
200
api.example.com
1.2 KB
```

should not load a 5 MB response body for every row.

Flow:

```text
History screen
      │
      ▼
History metadata query
      │
      ▼
LazyColumn
      │
      ▼
User taps item
      │
      ▼
Load full message
```

---

# 21. Local Saved History

The user can save Burp traffic locally:

```text
Burp History
     │
     ▼
Android
     │
     ▼
Save
     │
     ▼
SQLite / Room
     │
     ▼
Permanent local history
```

Once saved:

```text
Burp can disconnect.
Wi-Fi can disappear.
Burp can close.
```

The saved history remains accessible.

---

# 22. Read-Only Historical Mode

Saved history should have a clear semantic distinction.

```text
LIVE
```

versus:

```text
ARCHIVED
```

Archived history is read-only.

Example:

```text
Archive
 ├── Request
 ├── Response
 ├── Headers
 ├── Body
 ├── Timeline
 ├── Notes
 └── Screenshot
```

The mobile application must not accidentally send an archived request to Burp merely because the user opened it.

Actions should explicitly say:

```text
Send to Repeater
```

or:

```text
Replay in Burp
```

rather than treating archived data as executable.

---

# 23. History Lifecycle

```text
BURP OBSERVED
      │
      ▼
REMOTE EVENT
      │
      ▼
ANDROID PROJECTION
      │
      ├───────────────┐
      │               │
      ▼               ▼
Live History       Save
                      │
                      ▼
                Archived History
                      │
                      ▼
                 SQLite/Room
```

---

# 24. History State Machine

```text
OBSERVED
   │
   ▼
LIVE
   │
   ├── SAVE ───────► ARCHIVED
   │
   └── DELETE LIVE PROJECTION

ARCHIVED
   │
   ├── ANNOTATE
   ├── BOOKMARK
   ├── SHARE
   ├── SCREENSHOT
   └── SEND TO REPEATER
```

Archived data should never mutate because Burp changed.

It represents what the user explicitly saved.

---

# 25. Sharing

Saved history can be shared.

Do not share raw database rows.

Create an export format.

Recommended:

```text
.burpremote
```

Example:

```text
burpremote-export/
├── manifest.json
├── history/
│   ├── request.bin
│   └── response.bin
├── metadata.json
├── screenshots/
│   └── screenshot-001.webp
└── annotations.json
```

Compress:

```text
ZIP
```

and give it the extension:

```text
.burpremote
```

---

# 26. Export Manifest

Example:

```json
{
  "format": "burpremote",
  "exportFormatVersion": 1,
  "createdAt": "2026-09-12T10:00:00Z",
  "items": [
    {
      "historyIdentifier": "history_123",
      "method": "GET",
      "host": "example.com",
      "path": "/api/user"
    }
  ]
}
```

The importer validates:

```text
format
version
checksum
file size
payload structure
```

before importing.

---

# 27. Sharing Security

HTTP history can contain:

```text
cookies
Authorization headers
JWTs
API keys
CSRF tokens
session identifiers
personal data
```

Therefore the share workflow should include:

```text
Share
  ↓
Security Review
  ↓
Sensitive Data Detection
  ↓
Redaction Preview
  ↓
User Confirmation
  ↓
Export
```

Potential redaction:

```text
Authorization: Bearer ********
Cookie: session=********
X-API-Key: ********
```

Never silently modify the user's original archived copy.

Redaction applies only to the exported copy.

---

# 28. Screenshot System

Screenshot handling should be a separate subsystem.

There are two different cases.

## Case A — Imported screenshot

User selects:

```text
Screenshot
```

from Android Photos / Files.

System:

```text
Image
 ↓
Decode
 ↓
Screenshot Detector
 ↓
OCR / Structure Analysis
 ↓
Beautifier
 ↓
Preview
 ↓
Save
```

---

## Case B — Screenshot generated from Burp history

User selects:

```text
Share / Screenshot
```

The app generates a clean security-research presentation image.

Example:

```text
┌─────────────────────────────────────┐
│ GET /api/user                       │
│                                     │
│ Host: api.example.com              │
│ Status: 200                         │
│                                     │
│ ┌───────────────────────────────┐   │
│ │ Request                       │   │
│ │ Authorization: Bearer ...     │   │
│ │                                │   │
│ │ historyIdentifier=123          │   │
│ └───────────────────────────────┘   │
│                                     │
│ ┌───────────────────────────────┐   │
│ │ Response                      │   │
│ │ {"status":"ok"}               │   │
│ └───────────────────────────────┘   │
└─────────────────────────────────────┘
```

This should be generated as a dedicated Compose screenshot/export surface rather than taking a screenshot of whatever happens to be visible.

---

# 29. Screenshot Detection

The detector should identify common screenshot structures:

```text
status bar
navigation bar
browser chrome
Burp UI chrome
phone frame
black borders
scroll indicators
```

Pipeline:

```text
Bitmap
   │
   ▼
Image Normalization
   │
   ▼
Edge / Region Detection
   │
   ├── top system UI
   ├── bottom navigation UI
   ├── browser chrome
   └── content region
   │
   ▼
Content Crop
   │
   ▼
OCR / Layout Detection
   │
   ▼
Beautification
```

Do not destroy the original.

Store:

```text
original_image
processed_image
processing_metadata
```

---

# 30. Screenshot Data Model

```text
screenshot
-------------------------
screenshot_identifier
history_identifier nullable
original_uri
processed_uri
width
height
format
created_at
processed_at
processing_version
status
```

Processing states:

```text
IMPORTED
ANALYZING
DETECTED
BEAUTIFYING
READY
FAILED
```

---

# 31. Beautification Engine

Create an abstraction:

```kotlin
interface ScreenshotBeautifier {
    suspend fun beautify(
        image: ScreenshotInput
    ): BeautifiedScreenshot
}
```

Do not tightly couple the app to a particular OCR/vision implementation.

Possible implementations:

```text
Android native image processing
ML Kit OCR
future local vision model
future remote vision service
```

The domain layer should not know which engine is being used.

---

# 32. Android Project Structure

Recommended:

```text
BurpsuiteRemote/
│
├── .trae/
│   ├── plan.md
│   └── rules.md
│
├── client/
│   └── mobileapp/
│       ├── app/
│       │
│       ├── core/
│       │   ├── common/
│       │   ├── model/
│       │   ├── protocol/
│       │   ├── crypto/
│       │   ├── serialization/
│       │   └── testing/
│       │
│       ├── domain/
│       │   ├── command/
│       │   ├── event/
│       │   ├── aggregate/
│       │   ├── query/
│       │   ├── projection/
│       │   ├── repository/
│       │   └── service/
│       │
│       ├── data/
│       │   ├── database/
│       │   ├── eventstore/
│       │   ├── repository/
│       │   ├── remote/
│       │   ├── export/
│       │   └── screenshot/
│       │
│       ├── feature/
│       │   ├── dashboard/
│       │   ├── connection/
│       │   ├── history/
│       │   ├── intercept/
│       │   ├── repeater/
│       │   ├── archive/
│       │   ├── screenshot/
│       │   ├── sharing/
│       │   └── settings/
│       │
│       └── ui/
│           ├── theme/
│           ├── components/
│           ├── navigation/
│           └── icons/
│
├── plugins/
│   └── burp-remote-extension/
│       ├── src/
│       └── build.gradle.kts
│
├── protocol/
│   ├── README.md
│   ├── commands/
│   ├── events/
│   ├── schemas/
│   └── versions/
│
├── scripts/
│
└── docs/
    ├── architecture/
    ├── security/
    ├── protocol/
    └── development/
```

---

# 33. Android Package Structure

Base package:

```text
xin.ctkqiang.burpsuite
```

Recommended:

```text
xin.ctkqiang.burpsuite
├── app
├── core
├── domain
├── data
├── feature
└── ui
```

Avoid:

```text
xin.ctkqiang.burpsuite.utils
xin.ctkqiang.burpsuite.helpers
xin.ctkqiang.burpsuite.misc
```

as dumping grounds.

---

# 34. Domain Layer

The domain layer should contain the business model.

Example:

```text
domain/
├── command/
│   ├── Command.kt
│   ├── CommandBus.kt
│   ├── ForwardIntercept.kt
│   ├── DropIntercept.kt
│   ├── SaveHistory.kt
│   └── ShareHistory.kt
│
├── event/
│   ├── DomainEvent.kt
│   ├── EventEnvelope.kt
│   ├── HistoryEvents.kt
│   ├── InterceptEvents.kt
│   ├── RepeaterEvents.kt
│   └── ScreenshotEvents.kt
│
├── query/
│   ├── Query.kt
│   ├── HistoryQuery.kt
│   ├── InterceptQuery.kt
│   └── ScreenshotQuery.kt
│
├── projection/
│   ├── HistoryProjection.kt
│   ├── InterceptProjection.kt
│   └── TimelineProjection.kt
│
└── repository/
    ├── EventStore.kt
    ├── HistoryRepository.kt
    └── RemoteRepository.kt
```

---

# 35. Repository Interfaces

Domain should depend on interfaces.

Example:

```kotlin
interface EventStore {
    suspend fun appendEvents(events: List<StoredEvent>)
    suspend fun readEventsAfter(sequenceNumber: Long): List<StoredEvent>
    suspend fun latestSequenceNumber(): Long
}
```

Remote:

```kotlin
interface BurpRemoteGateway {
    suspend fun executeCommand(command: RemoteCommand): CommandResult
    fun observeEvents(): Flow<RemoteEvent>
}
```

History:

```kotlin
interface HistoryRepository {
    fun observeHistory(
        query: HistoryQuery
    ): Flow<PagingData<HistoryItem>>

    suspend fun get(historyIdentifier: HistoryIdentifier): HistoryItem?

    suspend fun save(historyIdentifier: HistoryIdentifier)
}
```

---

# 36. Projection Architecture

Events are reduced into read models.

Example:

```text
Event:

HistoryItemObserved
```

becomes:

```text
history table
```

Then:

```text
HistoryQuery
```

reads:

```text
history table
```

rather than replaying the entire event stream every time.

This is the CQRS read side.

---

# 37. Projection Pipeline

```text
             EVENT
               │
               ▼
        Event Dispatcher
               │
        ┌──────┼───────┐
        │      │       │
        ▼      ▼       ▼
     History Intercept Timeline
     Project  Project  Project
        │      │       │
        ▼      ▼       ▼
      SQLite SQLite  SQLite
```

Each projection should be independently rebuildable.

---

# 38. Projection Checkpoint

Every projection stores:

```text
projection_name
last_sequence
updated_at
```

Example:

```text
history_projection = 5003
timeline_projection = 5003
intercept_projection = 5001
```

If the application crashes:

```text
restart
 ↓
read checkpoint
 ↓
replay remaining events
 ↓
projection becomes current
```

---

# 39. Event Replay

This should be a first-class operation.

```text
EventStore
    │
    ▼
Replay
    │
    ▼
Projection
```

Useful for:

```text
database migrations
bug recovery
projection rebuild
testing
debugging
future features
```

---

# 40. Snapshotting

Do not replay millions of events every startup.

Use snapshots for aggregates where appropriate.

Example:

```text
aggregate_snapshot
-------------------------
aggregate_type
aggregate_identifier
sequence_number
state
created_at
```

But do not over-engineer this in MVP.

Start with:

```text
event log
+
projection checkpoints
```

Add snapshots when measurements show they are needed.

---

# 41. MVI UI Architecture

Compose should observe immutable state.

```text
ViewModel
    │
    ▼
HistoryUserInterfaceState
    │
    ▼
Compose
```

User action:

```text
Compose
   │
   ▼
HistoryUserInterfaceIntent
   │
   ▼
ViewModel
   │
   ▼
Command
```

Example:

```kotlin
sealed interface HistoryUserInterfaceIntent {
    data class OpenHistoryItem(
        val historyIdentifier: HistoryIdentifier
    ) : HistoryUserInterfaceIntent

    data class SaveHistoryItem(
        val historyIdentifier: HistoryIdentifier
    ) : HistoryUserInterfaceIntent

    data class BookmarkHistoryItem(
        val historyIdentifier: HistoryIdentifier
    ) : HistoryUserInterfaceIntent

    data class ShareHistoryItem(
        val historyIdentifier: HistoryIdentifier
    ) : HistoryUserInterfaceIntent
}
```

---

# 42. UI State

Example:

```kotlin
data class HistoryUserInterfaceState(
    val items: List<HistoryRow>,
    val selectedHistoryIdentifier: HistoryIdentifier?,
    val filter: HistoryFilter,
    val connectionState: ConnectionState,
    val loading: Boolean,
    val error: UserInterfaceError?
)
```

Never expose database entities directly to Compose.

Use UI models.

---

# 43. UI Navigation

Recommended navigation:

```text
Dashboard
│
├── Live
│   ├── History
│   ├── Intercept
│   └── Repeater
│
├── Archive
│   ├── Saved History
│   ├── Bookmarks
│   └── Screenshots
│
└── Settings
    ├── Burp Connection
    ├── Appearance
    ├── Language
    ├── Security
    └── Storage
```

---

# 44. Main Dashboard

The dashboard should communicate connection state immediately.

Example:

```text
┌──────────────────────────────────────┐
│ Burp Remote                          │
│ ● Connected                          │
│ api.example.com                      │
│                                      │
│ 1,284 Live Requests                  │
│ 23 Intercepted                       │
│ 87 Saved                             │
│                                      │
│ [History] [Intercept] [Repeater]     │
│                                      │
│ Recent                               │
│ GET  /api/login                 200  │
│ POST /api/user                  403  │
│ GET  /api/admin                 200  │
└──────────────────────────────────────┘
```

---

# 45. Theme

Brand identity:

```text
Orange
```

Do not hardcode orange into individual composables.

Use:

```text
Theme
 ├── ColorScheme
 ├── Typography
 ├── Shapes
 └── Dimensions
```

Example conceptual palette:

```text
Primary = Burp-inspired orange
```

Theme mode is exactly one of these three canonical values:

```text
AUTOMATIC
LIGHT
DARK
```

`AUTOMATIC` follows the operating-system setting.

The three values above are the only allowed theme modes; they match `rules.md`
§1 and must not be renamed (for example, do not call `AUTOMATIC` "system").

Theme preference belongs in the data/settings layer.

---

# 46. Localization

Default:

```text
en
```

Supported:

```text
en
zh
de
```

Do not put strings directly into Kotlin.

Bad:

```kotlin
Text("Connected")
```

Prefer:

```kotlin
Text(stringResource(R.string.connection_connected))
```

Resources:

```text
res/
├── values/
│   └── strings.xml
├── values-zh/
│   └── strings.xml
└── values-de/
    └── strings.xml
```

The domain layer must never contain UI-language strings.

---

# 47. Settings

Use DataStore for lightweight application preferences.

Examples:

```text
theme
language
server address
server port
device name
auto-save
screenshot processing
redaction defaults
```

Room remains for structured application data.

DataStore remains for preferences/configuration. Android recommends keeping DataStore operations in the data layer and exposing them through ViewModels/repositories.

---

# 48. Communication Protocol

Use:

```text
REST
+
WebSocket
```

REST:

```text
queries
commands
snapshots
large resource retrieval
```

WebSocket:

```text
live events
connection state
event synchronization
```

---

# 49. REST Endpoints

```text
GET  /v1/status

POST /v1/pair

GET  /v1/capabilities

GET  /v1/history
GET  /v1/history/{historyIdentifier}

GET  /v1/intercepts
GET  /v1/intercepts/{interceptIdentifier}

POST /v1/intercepts/{interceptIdentifier}/modify
POST /v1/intercepts/{interceptIdentifier}/forward
POST /v1/intercepts/{interceptIdentifier}/drop

POST /v1/repeater
POST /v1/repeater/{repeaterRequestIdentifier}/execute

GET  /v1/snapshot
```

---

# 50. WebSocket

```text
/v1/events
```

Handshake:

```text
Android
   │
   │ CONNECT
   ▼
Extension
   │
   │ AUTHENTICATE
   ▼
Extension
   │
   │ RESUME sequenceNumber=5003
   ▼
Extension
   │
   │ events
   ▼
Android
```

---

# 51. Protocol Versioning

Every message contains:

```json
{
  "protocolVersion": 1,
  "messageType": "event"
}
```

Never expose internal Montoya class names.

Bad:

```json
{
  "messageType": "ProxyHttpRequestResponse"
}
```

Good:

```json
{
  "messageType": "history.item.observed"
}
```

The protocol must remain independent of the Burp implementation.

---

# 52. Burp Extension Structure

The Burp side should use the Montoya API.

Recommended:

```text
plugins/
└── burp-remote-extension/
    ├── build.gradle.kts
    └── src/main/kotlin/
        └── xin/ctkqiang/burpsuite/remote/
            ├── BurpRemoteExtension.kt
            │
            ├── adapter/
            │   ├── BurpHistoryAdapter.kt
            │   ├── BurpProxyAdapter.kt
            │   ├── BurpInterceptAdapter.kt
            │   └── BurpRepeaterAdapter.kt
            │
            ├── protocol/
            │   ├── RemoteCommand.kt
            │   ├── RemoteEvent.kt
            │   ├── RemoteEnvelope.kt
            │   └── RemoteProtocolVersion.kt
            │
            ├── transport/
            │   ├── RemoteHttpServer.kt
            │   ├── RemoteWebSocketServer.kt
            │   └── RemoteConnectionRegistry.kt
            │
            ├── command/
            │   ├── RemoteCommandDispatcher.kt
            │   ├── ForwardInterceptCommandHandler.kt
            │   ├── DropInterceptCommandHandler.kt
            │   └── SendToRepeaterCommandHandler.kt
            │
            ├── event/
            │   ├── RemoteEventPublisher.kt
            │   ├── RemoteEventJournalStore.kt
            │   └── EventSequenceGenerator.kt
            │
            └── security/
                ├── DevicePairingService.kt
                ├── PairedDeviceRegistry.kt
                ├── DeviceAuthenticationService.kt
                └── DeviceAuthorizationService.kt
```

The extension is written in Kotlin on the JVM and compiled against the Montoya API. Montoya is a Java API, so Kotlin/JVM interoperates with it directly; the result is packaged as a JAR and loaded by Burp Suite. No Java source files are written by hand in this repository.

Identifier and declaration names in the extension follow the same long-form, acronym-free conventions as the client, as defined in `rules.md`.

---

# 53. Burp Adapter Layer

```text
Montoya
   │
   ▼
Adapter
   │
   ▼
Remote DTO
   │
   ▼
Protocol
```

Never:

```text
Montoya object
   ↓
JSON serializer
   ↓
Android
```

That couples the wire format to Burp internals.

---

# 54. Security Model

The remote-control server is dangerous by definition.

It controls Burp.

Therefore:

```text
LAN != trusted
```

Security requirements:

```text
device pairing
authentication
authorization
TLS
operation IDs
replay protection
rate limiting
message size limits
connection limits
audit logging
```

---

# 55. Pairing

Initial connection:

```text
Burp Extension
     │
     ▼
Generate pairing challenge
     │
     ▼
User enters/scans pairing code
     │
     ▼
Android establishes device identity
     │
     ▼
Credential stored securely
```

After pairing:

```text
device identity
+
authentication credential
```

is used.

Never expose:

```text
http://192.168.x.x:9000
```

with unauthenticated remote control.

---

# 56. Authorization

Initial model:

```text
READ
CONTROL
```

READ allows:

```text
status
history
request
response
events
```

CONTROL allows:

```text
modify intercept
forward
drop
repeater execution
proxy controls
```

Future:

```text
READ_ONLY
OPERATOR
ADMIN
```

---

# 57. Sensitive Data

The application handles sensitive material.

Potential data:

```text
Cookie
JWT
Authorization
API keys
session IDs
passwords
CSRF tokens
private data
```

Therefore:

```text
never log request bodies by default
never log credentials
never include tokens in analytics
never expose credentials in crash reports
```

---

# 58. Local Database Security

The local database should be treated as sensitive.

Recommended architecture:

```text
Room
 ↓
SQLite
 ↓
Application-private storage
```

Future enhancement:

```text
SQLCipher / encrypted database
```

or encrypted sensitive payload storage.

Do not make encryption a prerequisite for the first architecture milestone if it slows development, but design the repository boundary so it can be introduced later.

---

# 59. Offline-first

The app must remain useful without Burp being connected.

Offline features:

```text
saved history
search
filters
bookmarks
annotations
screenshots
exports
```

Online features:

```text
live history
intercept
repeater
Burp controls
live events
```

UI should clearly communicate:

```text
LIVE
```

versus:

```text
OFFLINE
```

---

# 60. Connection State Machine

```text
DISCONNECTED
      │
      ▼
DISCOVERING
      │
      ▼
CONNECTING
      │
      ▼
AUTHENTICATING
      │
      ▼
SYNCING
      │
      ▼
CONNECTED
      │
      ├──────────────┐
      │              │
      ▼              ▼
RECONNECTING      DISCONNECT
      │
      ▼
SYNCING
      │
      ▼
CONNECTED
```

Failure:

```text
AUTHENTICATION_FAILED
PROTOCOL_ERROR
TIMEOUT
SERVER_UNAVAILABLE
RESYNC_REQUIRED
```

---

# 61. Event Processing Pipeline

```text
WebSocket
   │
   ▼
Frame Decoder
   │
   ▼
Protocol Validator
   │
   ▼
Event Envelope
   │
   ▼
Deduplication
   │
   ▼
Event Store
   │
   ▼
Projection Dispatcher
   │
   ├── History
   ├── Intercept
   ├── Timeline
   ├── Connection
   └── Screenshot
   │
   ▼
Room
   │
   ▼
Flow
   │
   ▼
ViewModel
   │
   ▼
Compose
```

---

# 62. Exactly-once vs At-least-once

Do not pretend the network gives exactly-once delivery.

Design for:

```text
at-least-once event delivery
```

and implement deduplication using:

```text
eventIdentifier
sequenceNumber
```

For commands:

```text
operationIdentifier
```

This gives us practical idempotency.

---

# 63. Event Ordering

Events from the same remote source must be ordered using:

```text
sequenceNumber
```

Android should reject or quarantine impossible sequences.

Example:

```text
received 100
received 101
received 103
```

Missing:

```text
102
```

Android should not blindly continue.

Request:

```text
RESUME_FROM 101
```

---

# 64. Database Transaction

When processing an event:

```text
BEGIN TRANSACTION

insert remote_event_journal

update projection

update projection_checkpoint

COMMIT
```

This prevents:

```text
event stored
projection missing
```

or:

```text
projection updated
event missing
```

from causing inconsistent state.

---

# 65. Example Transaction

Receive:

```text
HistoryItemSaved
sequenceNumber=5021
```

Transaction:

```text
remote_event_journal
   INSERT sequence_number 5021

history
   UPDATE archived = true

projection_checkpoint
   UPDATE sequence_number = 5021

COMMIT
```

All three succeed or all three fail.

---

# 66. Paging

Saved history can become huge.

Use database paging.

Do not:

```text
SELECT * FROM history
```

into memory.

Use:

```text
Paging 3
+
Room
+
Flow
```

Conceptually:

```text
SQLite
  │
  ▼
PagingSource
  │
  ▼
Pager
  │
  ▼
ViewModel
  │
  ▼
LazyColumn
```

---

# 67. Search

Search should happen in SQLite.

Potential indexed fields:

```text
host
method
path
status_code
mime_type
timestamp
title
```

For body search, consider:

```text
SQLite FTS
```

later.

Do not load the entire archive into Kotlin memory to search it.

---

# 68. Filtering

History filtering:

```text
method
host
status
MIME
TLS
edited
saved
bookmarked
time range
```

Example:

```text
method = POST
status >= 400
host = api.example.com
saved = true
```

Build query objects:

```kotlin
data class HistoryFilter(
    val host: String?,
    val methods: Set<HttpMethod>,
    val statusCodes: Set<Int>,
    val savedOnly: Boolean,
    val bookmarkedOnly: Boolean,
    val from: Instant?,
    val until: Instant?
)
```

---

# 69. Timeline

One special feature should be a unified timeline.

Example:

```text
15:41:02
HTTP request observed

15:41:03
Request intercepted

15:41:05
Request modified

15:41:06
Request forwarded

15:41:08
Response received

15:41:10
History saved

15:42:11
Bookmark added

15:43:02
Screenshot created
```

---

# 70. Timeline Projection

```text
Event Store
     │
     ▼
TimelineProjection
     │
     ▼
timeline table
```

Schema:

```text
timeline
-------------------------
timeline_identifier
timestamp
event_type
aggregate_type
aggregate_identifier
summary
metadata
sequence_number
```

The UI can then display a security-testing session timeline.

---

# 71. "Investigation" Concept

Future feature:

```text
Investigation
```

An investigation groups:

```text
HTTP requests
responses
annotations
screenshots
bookmarks
notes
repeater executions
timeline events
```

Example:

```text
Investigation: API Authorization Test

├── GET /api/user
├── GET /api/user/123
├── GET /api/user/124
├── Repeater #1
├── Screenshot #1
├── Screenshot #2
└── Notes
```

---

# 72. Data Model for Investigation

```text
investigation
----------------
investigation_identifier
name
description
created_at
updated_at

investigation_item
----------------
investigation_identifier
item_type
item_identifier
created_at
```

Item types:

```text
HISTORY
REPEATER
SCREENSHOT
ANNOTATION
TIMELINE_EVENT
```

---

# 73. Share Investigation

A complete investigation can become:

```text
.burpremote
```

containing:

```text
manifest
investigation
history
requests
responses
screenshots
annotations
timeline
```

This creates a portable security-research artifact.

---

# 74. Screenshot + Investigation

Example workflow:

```text
History
   │
   ▼
Select request
   │
   ▼
Annotate
   │
   ▼
Capture
   │
   ▼
Beautify
   │
   ▼
Attach to Investigation
   │
   ▼
Share
```

---

# 75. Export Pipeline

```text
User
 │
 ▼
Share
 │
 ▼
ExportCommand
 │
 ▼
Load Investigation
 │
 ▼
Load History
 │
 ▼
Load Screenshots
 │
 ▼
Sensitive Data Scanner
 │
 ▼
Redaction
 │
 ▼
Manifest
 │
 ▼
ZIP
 │
 ▼
Android Share Sheet
```

---

# 76. Architecture Boundaries

The following dependency direction must be enforced:

```text
UI
 ↓
Domain
 ↓
Interfaces
 ↓
Data
```

Never:

```text
Compose
 ↓
Room DAO
```

and never:

```text
Domain
 ↓
Room
```

and never:

```text
Domain
 ↓
Retrofit
```

---

# 77. Dependency Rules

Allowed:

```text
feature → domain
feature → ui
data → domain
core → domain
app → everything
```

Not allowed:

```text
domain → data
domain → android
domain → compose
domain → room
domain → okhttp
```

This keeps the architecture replaceable.

---

# 78. Testing Strategy

Testing must exist at multiple levels.

## Unit

```text
Command handlers
Reducers
Aggregates
Event serializers
Projection logic
Filters
Redaction
Screenshot metadata
```

## Integration

```text
Room
Event store
Projection transactions
WebSocket
REST
Protocol compatibility
```

## End-to-end

```text
Android
    ↓
Burp Extension
    ↓
Burp
```

Test:

```text
history
intercept
forward
drop
repeater
reconnect
resume
resync
archive
export
import
```

---

# 79. Event Tests

Example:

```text
Given:
    sequenceNumber = 100

When:
    HistoryItemObserved

Then:
    history projection contains item

And:
    checkpoint = 100
```

Then:

```text
When:
    same event arrives again

Then:
    projection is unchanged
```

This validates idempotency.

---

# 80. Failure Testing

Simulate:

```text
Wi-Fi disappears
WebSocket closes
Burp crashes
Burp restarts
Android process dies
event duplicated
event arrives out of order
command times out
command response lost
database transaction fails
export interrupted
large response body
malformed event
```

The application should recover rather than corrupt state.

---

# 81. Performance Rules

Do not:

```text
deserialize huge response bodies on the main thread
load entire history
recompose entire history list
store duplicated giant JSON objects
send full bodies in every WebSocket event
```

Prefer:

```text
metadata events
lazy body loading
paging
Flow
background dispatchers
bounded event queues
compressed exports
```

---

# 82. Event Payload Size

A live event should normally contain:

```json
{
  "historyIdentifier": "history_123",
  "method": "GET",
  "host": "api.example.com",
  "path": "/api/user",
  "status": 200
}
```

Not:

```json
{
  "requestBody": "...5MB...",
  "responseBody": "...20MB..."
}
```

Use:

```text
event → metadata
query → full resource
```

This keeps the live channel fast.

---

# 83. Large Body Retrieval

When user opens a request:

```text
Android
   │
   │ GET /history/hist_123/message
   ▼
Extension
   │
   ▼
Burp
   │
   ▼
Response
```

If the item is archived:

```text
Android
   │
   ▼
Room
   │
   ▼
local body
```

No Burp connection required.

---

# 84. MVP

Do not implement everything immediately.

## Milestone 1

```text
Android
Burp Extension
Protocol
Connection
Pairing
Status
```

## Milestone 2

```text
HTTP history
Live events
Room archive
Offline history
```

## Milestone 3

```text
Intercept
Forward
Drop
Modify
```

## Milestone 4

```text
Repeater
```

## Milestone 5

```text
Sharing
Export
Import
Redaction
```

## Milestone 6

```text
Screenshot detection
OCR
Beautification
```

## Milestone 7

```text
Investigation
Timeline
Advanced projections
```

---

# 85. Development Order

The implementation order should be:

```text
1. Protocol specification
2. Domain model
3. Event model
4. CQRS model
5. Event store abstraction
6. Burp extension
7. CLI test client
8. Android remote transport
9. Room database
10. Projections
11. Compose UI
12. History
13. Intercept
14. Repeater
15. Archive
16. Export/import
17. Screenshot engine
18. Investigation
19. Performance
20. Security hardening
```

Do NOT begin by designing 30 Compose screens.

The protocol and domain model must come first.

---

# 86. CLI Development Tool

Before Android is complete, create:

```text
burpctl
```

Example:

```text
burpctl status

burpctl history

burpctl history get hist_123

burpctl intercept list

burpctl intercept forward int_123

burpctl intercept drop int_123
```

This gives us a deterministic way to test the Burp extension.

Architecture:

```text
burpctl
   │
   ▼
Protocol
   │
   ▼
Burp Extension
   │
   ▼
Burp
```

If the CLI works, Android becomes primarily a client/UI problem.

---

# 87. Repository-Level Architecture

Final repository:

```text
BurpsuiteRemote/
│
├── .trae/
│   ├── plan.md
│   └── rules.md
│
├── plugins/
│   └── burp-remote-extension/
│
├── client/
│   └── mobileapp/
│       ├── app/
│       ├── core/
│       ├── domain/
│       ├── data/
│       ├── feature/
│       └── ui/
│
├── protocol/
│
├── docs/
│
├── scripts/
│   └── protocol-verifier-cli/
│
├── .github/
│   └── workflows/
│
├── README.md
├── ARCHITECTURE.md
├── SECURITY.md
└── LICENSE
```

---

# 88. Definition of Done

The architecture is considered successful when:

```text
[ ] Android connects to Burp
[ ] Device pairing works
[ ] Authentication works
[ ] Protocol version negotiation works
[ ] WebSocket events work
[ ] REST commands work
[ ] Events have monotonic sequence numbers
[ ] Event replay works
[ ] Resume works
[ ] Resync works
[ ] Commands are idempotent
[ ] Room persistence works
[ ] History projection works
[ ] History survives app restart
[ ] Saved history works offline
[ ] Saved history is read-only
[ ] History paging works
[ ] Search works
[ ] Filtering works
[ ] Intercept works
[ ] Forward works
[ ] Drop works
[ ] Modify works
[ ] Repeater works
[ ] Export works
[ ] Import works
[ ] Sensitive-data redaction works
[ ] Screenshot detection works
[ ] Screenshot beautification works
[ ] Timeline projection works
[ ] English localization works
[ ] Chinese localization works
[ ] German localization works
[ ] Light theme works
[ ] Dark theme works
[ ] Orange branding works
[ ] Crash recovery works
[ ] Network recovery works
[ ] Protocol tests exist
[ ] Projection tests exist
[ ] Security tests exist
```

---

# 89. Final Architecture

The resulting system should look like this:

```text
                         BURP SUITE
                    ┌───────────────────┐
                    │ Proxy             │
                    │ HTTP History      │
                    │ Intercept         │
                    │ Repeater          │
                    │ HTTP Engine       │
                    └─────────┬─────────┘
                              │
                         Montoya API
                              │
                    ┌─────────▼─────────┐
                    │ BURP REMOTE       │
                    │ EXTENSION         │
                    │                   │
                    │ Adapter Layer     │
                    │ Command Bus       │
                    │ Event Bus         │
                    │ Event Store       │
                    │ Pairing           │
                    │ Auth              │
                    │ WebSocket         │
                    │ REST              │
                    └─────────┬─────────┘
                              │
                         TLS / LAN
                              │
══════════════════════════════╪══════════════════════════════
                              │
                    ┌─────────▼─────────┐
                    │ ANDROID           │
                    │                   │
                    │ Remote Transport  │
                    │        ↓          │
                    │ CQRS              │
                    │        ↓          │
                    │ Event Store       │
                    │        ↓          │
                    │ Projections       │
                    │        ↓          │
                    │ Room / SQLite     │
                    │        ↓          │
                    │ MVI               │
                    │        ↓          │
                    │ Compose           │
                    └─────────┬─────────┘
                              │
             ┌────────────────┼────────────────┐
             │                │                │
             ▼                ▼                ▼
          LIVE             ARCHIVE          SHARE
             │                │                │
             │                │                ▼
             │                │          .burpremote
             │                │                │
             │                │                ▼
             │                │          Redaction
             │                │                │
             │                │                ▼
             │                │          Android Share
             │                │
             │                ▼
             │          SQLite / Room
             │
             ▼
        Event Timeline
             │
             ▼
       Screenshot Engine
             │
             ▼
       Beautified Report
```

---

# 90. Architectural Rule

> **Burp executes. Events describe what happened. CQRS separates intent from observation. Room stores local projections and archived history. Compose renders state.**

Do not allow the Android application to become a second Burp.

The Android application is:

```text
REMOTE CONTROL
+
EVENT STREAM
+
LOCAL ARCHIVE
+
INVESTIGATION WORKSPACE
+
SHARING / PRESENTATION TOOL
```