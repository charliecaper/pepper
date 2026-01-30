# Pepper

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

### 2. Start the WebSocket relay server

```bash
node tools/ws-relay.js
```

This starts a WebSocket server on port 9000 that relays messages between your control tool and the app.

### 3. Start the dev server

```bash
./gradlew :composeApp:jsBrowserDevelopmentRun
```

The app serves on `http://<your-ip>:2000`.

### 4. Load in the Even App

Scan a QR code pointing to `http://<your-ip>:2000` from the Even App. The app will appear on the phone and glasses.

Tap **Connect WS** on the phone screen to connect to the relay server.

### 5. Send messages

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

```json
{"text1": "First line text", "text2": "Second line text"}
```

Both fields are required. Max 1000 characters per field at initial display, 2000 on update.

## Requirements

- **JDK 24**
- **Node.js >= 20**
- Even G2 glasses + Even App on iPhone

## Project layout

```
composeApp/src/
  commonMain/    Shared theme
  webMain/       App logic, SDK bridge, WebSocket receiver
  jsMain/        Kotlin/JS platform bindings
  wasmJsMain/    Kotlin/Wasm platform bindings
tools/
  ws-relay.js    WebSocket relay server
```

## Built with

- [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html) + [Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform)
- [Even Hub SDK](https://www.npmjs.com/package/@evenrealities/even_hub_sdk) (`@evenrealities/even_hub_sdk`)
