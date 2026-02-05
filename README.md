# Pepper

**Version 1.8**

A teleprompter for **Even G2 smart glasses**, controlled remotely via WebSocket. Designed for live performance — send text cues from QLab, TouchDesigner, or any tool that can send JSON over WebSocket.

## How it works

```
QLab / TouchDesigner / Script
        |
        | WebSocket JSON
        v
  ws-relay.js (Node.js, port 9000)
        |
        | relay
        v
  Pepper app (in Even App WebView on iPhone)
        |
        | BLE
        v
  Even G2 glasses display
```

The app shows two text lines and a timer on the glasses. External tools send `{"text1": "...", "text2": "..."}` over WebSocket to update them in real time.

## Quick start

### 1. Install dependencies

```bash
npm install
```

### 2. Build and run

**Option A: Production server (recommended)**

```bash
node tools/server.js
```

This automatically builds the app if needed, then serves both the web app (port 2000) and WebSocket relay (port 9000). Also generates a QR code for the Even App.

To force a rebuild:
```bash
node tools/server.js --rebuild
```

**Option B: Development mode**

```bash
# Terminal 1: WebSocket relay
node tools/ws-relay.js

# Terminal 2: Dev server with hot reload
./gradlew :composeApp:jsBrowserDevelopmentRun
```

The app serves on `http://<your-ip>:2000`.

### 4. Load in the Even App

Scan a QR code pointing to `http://<your-ip>:2000` from the Even App. The app will appear on the phone and glasses.

Tap **Connect WS** on the phone screen to connect to the relay server.

### 5. Send messages

**Commander GUI**: Open `tools/commander.html` in a browser. Select a command from the dropdown, fill in the fields, and click Send. Make sure the server is running first.

**From TouchDesigner** (WebSocket DAT connected to `localhost:9000`):

```python
op('websocket1').sendText('{"text1":"Line one","text2":"Line two"}')
```

**From QLab**: QLab speaks OSC over UDP, not WebSocket. You need a small bridge that receives OSC and forwards as WebSocket JSON. (Not yet included — contributions welcome.)

**From Python**:

```python
import asyncio, websockets, json

async def send():
    async with websockets.connect("ws://localhost:9000") as ws:
        await ws.send(json.dumps({"text1": "Hello", "text2": "World"}))

asyncio.run(send())
```

**From Node.js**:

```javascript
const WebSocket = require("ws");
const ws = new WebSocket("ws://localhost:9000");
ws.on("open", () => {
    ws.send(JSON.stringify({ text1: "Hello", text2: "World" }));
});
```

## Message format

### Display updates

```json
{"text1": "Current cue text", "text2": "Next cue text"}
```

Both fields are required. Updates the two text lines on the glasses.

### Commands

```json
{"command": "timerOn"}
{"command": "timerPacing", "time": "5.30"}
```

| Command | Description |
|---------|-------------|
| `timerOn` | Show timer on glasses |
| `timerOff` | Hide timer on glasses |
| `resetTimer` | Reset timer to 0:00 (keeps pacing target if set) |
| `timerPacing` | Set a pacing target time (format: `M.SS` or `MM.SS`) |
| `alert` | Show an alert message on glasses (auto-dismisses after 15s) |

#### Alert

```json
{"command": "alert", "text": "5 minutes left"}
```

Shows a right-aligned alert on the display. Auto-dismisses after 20 seconds, or is replaced by a new alert. Text longer than 30 characters is split into two lines. Send with empty text to clear immediately.

#### Pacing mode

The system timer always runs in the background. When you send a `timerPacing` command with a target time, the glasses display switches to countdown mode showing `target - elapsed`.

Example: If the system timer is at 1:00 and you send `{"command": "timerPacing", "time": "5.30"}`, the glasses will show `4:30` and count down. When the system timer reaches 5:30, it shows `0:00`. If you go over, it shows negative time (e.g., `-0:15`).

Use this to set checkpoints during a performance — "you should reach this cue by 5:30" — and the countdown shows how much time remains.

## Requirements

- **Node.js 20+**
- **JDK 24** (for building)
- **qrencode** (optional, for QR code generation: `brew install qrencode`)
- Even G2 glasses + Even App on iPhone

## Project layout

```
composeApp/src/
  commonMain/    Shared theme
  webMain/       App logic, SDK bridge, WebSocket receiver
  jsMain/        Kotlin/JS platform bindings
  wasmJsMain/    Kotlin/Wasm platform bindings
tools/
  server.js      Combined HTTP + WebSocket server (production)
  ws-relay.js    WebSocket relay only (development)
  commander.html GUI for sending commands
```

## Built with

- [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html) + [Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform)
- [Even Hub SDK](https://www.npmjs.com/package/@evenrealities/even_hub_sdk) (`@evenrealities/even_hub_sdk`)
