# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Pepper is a WebSocket-controlled teleprompter for Even G2 smart glasses. External tools (QLab, TouchDesigner, Python, Node.js) send JSON over WebSocket to a relay server, which forwards messages to a Kotlin Multiplatform web app running in the Even App's WebView on iPhone, which then displays text on the glasses via BLE.

## Build & Run Commands

```bash
# Install npm dependencies (required first)
npm install

# Start the WebSocket relay server (port 9000)
node tools/ws-relay.js

# Start dev server (Kotlin/JS, serves on port 2000)
./gradlew :composeApp:jsBrowserDevelopmentRun

# Start dev server (Kotlin/Wasm, experimental)
./gradlew :composeApp:wasmJsBrowserDevelopmentRun

# Run tests
./gradlew :composeApp:jsTest
```

## Requirements

- JDK 24
- Node.js >= 20

## Architecture

### Multiplatform Source Layout

The project uses Kotlin Multiplatform with a custom source set hierarchy (default hierarchy is disabled in `gradle.properties`):

```
commonMain        Shared theme (Material3 + MiSans font)
  └── webMain     Shared browser code: App UI, WebSocket receiver, SDK bridge (expect declarations)
       ├── jsMain        Kotlin/JS actual implementations (asDynamic(), await())
       └── wasmJsMain    Kotlin/Wasm actual implementations (@JsFun, suspendCancellableCoroutine)
```

### Message Flow

1. External tool sends JSON to `ws-relay.js` (port 9000)
2. Relay broadcasts to all connected WebSocket clients
3. `MessageReceiver.kt` (WebSocketReceiver) receives and parses messages
4. `App.kt` updates Compose state and calls `rebuildPageContainer()` via the Even Hub SDK bridge
5. Even App sends display update to glasses over BLE

### Key Source Files

- **`composeApp/src/webMain/.../App.kt`** — Main Composable: manages glasses containers (text1, text2, timer), handles WebSocket messages and commands, calls SDK to update display
- **`composeApp/src/webMain/.../MessageReceiver.kt`** — WebSocketReceiver: connects to relay, auto-reconnects after 3s, parses `NotifyMessage` (text updates) and `CommandMessage` (resetTimer, timerOn, timerOff)
- **`composeApp/src/webMain/.../sdk/EvenHubBridge.kt`** — expect interface for Even App SDK methods (createStartUpPageContainer, rebuildPageContainer, observeDeviceStatus)
- **`composeApp/src/webMain/.../sdk/EvenHubTypes.kt`** — Data models for SDK types (DeviceStatus, TextContainerProperty, CreateStartUpPageContainer, RebuildPageContainer)
- **`composeApp/src/webMain/.../sdk/JsInteropUtils.kt`** — Type conversion utilities for JS interop (property access, JSON building, Fetch wrappers)
- **`tools/ws-relay.js`** — Node.js WebSocket relay server

### Glasses Display Layout

The app manages 3 containers on the glasses:
- Container 1: Line 1 (text1) — width varies based on timer visibility (440px with timer, 576px without)
- Container 2: Line 2 (text2) — 576px wide
- Container 3: Timer (optional) — 126px wide, toggled via commands

### WebSocket Protocol

Display updates: `{"text1": "...", "text2": "..."}`
Commands: `{"command": "resetTimer"}`, `{"command": "timerOn"}`, `{"command": "timerOff"}`

### SDK Bridge Pattern

The Even Hub SDK integration uses expect/actual across platforms:
- **Kotlin/JS** (`jsMain`): Uses `external class EvenAppBridge` from npm, `asDynamic()`, direct `await()` on Promises
- **Kotlin/Wasm** (`wasmJsMain`): Uses `@JsFun` annotations, `suspendCancellableCoroutine` for Promise-to-suspend conversion

### Build Configuration

- Gradle version catalog at `gradle/libs.versions.toml` (Kotlin 2.2.21, Compose 1.9.3)
- Webpack dev server configured in `composeApp/webpack.config.d/` (host 0.0.0.0, all hosts allowed for LAN access)
- `resolve-even-hub-sdk.js` handles scoped npm package resolution for webpack
- Even App manifest at `app.json` (package ID: com.caper.pepper)
