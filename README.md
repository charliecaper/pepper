# Pepper

**Version 1.0.5**

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

### 1. Check Node.js version

Make sure you have Node.js 20 or later:

```bash
node --version
```

If not installed or outdated, download from [nodejs.org](https://nodejs.org/).

### 2. Install dependencies

```bash
npm install
```

### 3. Start the server

```bash
node tools/server.js
```

This will:
- Build the app automatically (first time only)
- Start the web server on port 2000
- Start the WebSocket relay on port 9000
- Generate and open a QR code

To force a rebuild after code changes:
```bash
node tools/server.js --rebuild
```

### 4. Connect your glasses

1. Open the **Even App** on your iPhone
2. Make sure your **Even G2 glasses** are connected to the app
3. Scan the QR code that popped up (or manually enter the URL shown in the terminal)
4. The Pepper app will load on your phone and glasses
5. It will automatically connect to the WebSocket server

### 5. Test with Commander

Open `tools/commander.html` in a browser to test sending commands. This is just for testing — the real use case is connecting to show control software.

### 6. Connect to show control

Pepper is designed to receive commands from show control software like QLab, TouchDesigner, or Keynote via WebSocket on port 9000.

**TouchDesigner** (WebSocket DAT):
```python
op('websocket1').sendText('{"text1":"Line one","text2":"Line two"}')
```

**QLab**: Use a Network cue with a script that sends WebSocket messages, or bridge OSC to WebSocket.

**Python**:
```python
import asyncio, websockets, json

async def send():
    async with websockets.connect("ws://localhost:9000") as ws:
        await ws.send(json.dumps({"text1": "Hello", "text2": "World"}))

asyncio.run(send())
```

**Node.js**:
```javascript
const WebSocket = require("ws");
const ws = new WebSocket("ws://localhost:9000");
ws.on("open", () => {
    ws.send(JSON.stringify({ text1: "Hello", text2: "World" }));
});
```

## Development

For development with hot reload:

```bash
# Terminal 1: WebSocket relay
node tools/ws-relay.js

# Terminal 2: Dev server with hot reload
./gradlew :composeApp:jsBrowserDevelopmentRun
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
