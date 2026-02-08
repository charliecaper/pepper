# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Pepper is a WebSocket-controlled teleprompter for Even G2 smart glasses. External tools (QLab, TouchDesigner, Python, Node.js) send JSON over WebSocket to a relay server, which forwards messages to a Kotlin Multiplatform web app running in the Even App's WebView on iPhone, which then displays text on the glasses via BLE.

## Build & Run Commands

```bash
# Install npm dependencies (required first)
npm install

# Production: Combined HTTP + WebSocket server (auto-builds if needed)
node tools/server.js

# Production: Force rebuild
node tools/server.js --rebuild

# Development: WebSocket relay only
node tools/ws-relay.js

# Development: Dev server with hot reload (run in separate terminal)
./gradlew :composeApp:jsBrowserDevelopmentRun

# Run tests
./gradlew :composeApp:jsTest
```

## Requirements

- Node.js 20+
- JDK 24 (for building)

## Tools

- **`tools/server.js`** — Combined HTTP (port 2000) + WebSocket relay (port 9000), auto-builds, generates QR code
- **`tools/ws-relay.js`** — WebSocket relay only (for development)
- **`tools/commander.html`** — GUI for testing WebSocket commands

## Architecture

### Multiplatform Source Layout

The project uses Kotlin Multiplatform with a custom source set hierarchy (default hierarchy is disabled in `gradle.properties`):

```
commonMain        Shared theme (Even OS design system, FK Grotesk font)
  └── webMain     Shared browser code: App UI, WebSocket receiver, SDK bridge (expect declarations)
       ├── jsMain        Kotlin/JS actual implementations (asDynamic(), await())
       └── wasmJsMain    Kotlin/Wasm actual implementations (@JsFun, suspendCancellableCoroutine)
```

### Message Flow

1. External tool sends JSON to WebSocket server (port 9000)
2. Server broadcasts to all connected WebSocket clients
3. `MessageReceiver.kt` (WebSocketReceiver) receives and parses messages
4. `App.kt` updates Compose state and calls `rebuildPageContainer()` via the Even Hub SDK bridge
5. Even App sends display update to glasses over BLE

### Key Source Files

- **`composeApp/src/webMain/.../App.kt`** — Main Composable: manages glasses containers (text1, text2, timer, alert), handles WebSocket messages and commands, calls SDK to update display
- **`composeApp/src/webMain/.../MessageReceiver.kt`** — WebSocketReceiver: connects to relay, auto-reconnects after 3s, parses `NotifyMessage` (text updates) and `CommandMessage`
- **`composeApp/src/webMain/.../sdk/EvenHubBridge.kt`** — expect interface for Even App SDK methods
- **`composeApp/src/webMain/.../sdk/EvenHubTypes.kt`** — Data models for SDK types
- **`composeApp/src/commonMain/.../theme/Theme.kt`** — Even OS 2.0 design system colors and typography

### Glasses Display Layout

The app manages up to 4 containers on the glasses:
- Container 1: Line 1 (text1) — width varies based on timer visibility (440px with timer, 576px without)
- Container 2: Line 2 (text2) — 576px wide
- Container 3: Timer (optional) — 126px wide, supports pacing countdown
- Container 4: Alert (optional) — right-aligned, auto-dismisses after 20s

### WebSocket Protocol

Display updates:
```json
{"text1": "...", "text2": "..."}
```

Commands:
```json
{"command": "timerOn"}
{"command": "timerOff"}
{"command": "resetTimer"}
{"command": "timerPacing", "time": "5.30"}
{"command": "alert", "text": "..."}
```

### Design System

Phone UI follows Even OS 2.0 design guidelines (`information/design_instructions.md`):
- FK Grotesk font
- Specific color palette (primary text #232323, backgrounds #EEEEEE/#F6F6F6)
- 6px corner radius, 12px margins
- Modal overlay for commands list
