# Even Hub Developer Guide

A comprehensive guide to building apps for the **Even G2 smart glasses** using the **Even Hub SDK**. This document is derived from the only existing example app ([EH-InNovel](https://github.com/even-realities/EH-InNovel)), the official SDK documentation (`@evenrealities/even_hub_sdk`), and publicly available hardware specifications.

---

## Table of Contents

1. [Platform Overview](#1-platform-overview)
2. [Hardware: The Even G2 Display](#2-hardware-the-even-g2-display)
3. [Architecture](#3-architecture)
4. [Getting Started](#4-getting-started)
5. [The Even Hub SDK](#5-the-even-hub-sdk)
6. [App Manifest (app.json)](#6-app-manifest-appjson)
7. [SDK API Reference](#7-sdk-api-reference)
8. [The Glasses UI System](#8-the-glasses-ui-system)
9. [Event System](#9-event-system)
10. [App Lifecycle](#10-app-lifecycle)
11. [Working with Images](#11-working-with-images)
12. [Best Practices](#12-best-practices)
13. [Common Patterns from the Example App](#13-common-patterns-from-the-example-app)
14. [Troubleshooting](#14-troubleshooting)
15. [Resources](#15-resources)

---

## 1. Platform Overview

**Even Hub** is the app platform for the Even G2 smart glasses. Apps are **web applications** (HTML/JS/Wasm) that run inside a WebView hosted by the **Even App** on the user's iPhone. The SDK provides a bridge between your web app and the native Even App, which communicates with the glasses over Bluetooth Low Energy (BLE 5.4).

### Key Concepts

- **Even App**: The companion iPhone app. It hosts your web app in a WebView and relays commands to the glasses via BLE.
- **Even Hub SDK**: An npm package (`@evenrealities/even_hub_sdk`) that provides the TypeScript/JavaScript bridge API between your web app and the Even App.
- **Page Containers**: The UI system on the glasses. You compose pages from text, list, and image containers positioned on a coordinate grid.
- **Events**: User interactions on the glasses (clicks, scrolls, double-clicks) and system events (foreground enter/exit) are pushed to your web app via the SDK.

### What You Can Build

- Text-based apps (readers, note viewers, teleprompters, translations)
- List-based apps (menus, navigation, selectable items)
- Image-based apps (simple graphics, icons, diagrams)
- Any combination of the above (up to 4 containers per page)

### What You Cannot Do (Current Limitations)

- No camera access (G2 has no camera -- this is by design for privacy)
- No audio output through the glasses (G2 is visual-only)
- No direct BLE access from the web app (all communication goes through the SDK bridge)
- No rich rendering on the glasses (no HTML/CSS -- you compose from containers)
- Maximum 4 containers per page
- Monochrome green display only

---

## 2. Hardware: The Even G2 Display

Understanding the hardware constraints is essential for designing good glasses UIs.

### Display Specifications

| Spec | Value |
|------|-------|
| Physical resolution | 640 x 350 pixels |
| **Logical canvas (for apps)** | **576 x 288 pixels** |
| Display type | Green microLED (monochrome) |
| Brightness | 1,200 nits |
| Field of view | 27.5 degrees |
| Refresh rate | 60 Hz |
| Ocularity | Binocular |
| Passthrough transparency | 98% |

### Coordinate System

```
(0,0) -------- X+ (576) ------->
  |
  |     Your containers go here
  |
  Y+ (288)
  |
  v
```

- **Origin**: Top-left corner at (0, 0)
- **X-axis**: Extends rightward, maximum 576
- **Y-axis**: Extends downward, maximum 288
- All container positions and sizes are specified in this coordinate space

### Display Characteristics

- **Monochrome green**: The display renders everything in green light on a transparent background. Design with high contrast in mind.
- **Transparent overlay**: Content appears overlaid on the real world. Avoid filling the entire canvas -- leave space for the user to see through.
- **Small FOV**: 27.5 degrees means content appears relatively small in the user's visual field. Keep text short and layouts simple.
- **Outdoor readability**: At 1,200 nits the display is bright, but in direct sunlight, simpler content with less detail reads better.

### Input Methods

The user interacts with your app primarily through the **Even R1 smart ring**:

| Gesture | Event Type | Value |
|---------|-----------|-------|
| Single click/tap | `CLICK_EVENT` | 0 |
| Scroll up | `SCROLL_TOP_EVENT` | 1 |
| Scroll down | `SCROLL_BOTTOM_EVENT` | 2 |
| Double click/tap | `DOUBLE_CLICK_EVENT` | 3 |

System events (not user-initiated):

| Event | Value | Description |
|-------|-------|-------------|
| Foreground enter | 4 | App enters foreground on glasses |
| Foreground exit | 5 | App leaves foreground on glasses |
| Abnormal exit | 6 | App crashed or was force-closed |

---

## 3. Architecture

```
+--------------------------------------------------+
|           Your Web App (HTML/JS/Wasm)            |
|               runs in WebView                     |
+--------------------------------------------------+
                    |
                    | @evenrealities/even_hub_sdk
                    | (EvenAppBridge singleton)
                    |
+--------------------------------------------------+
|         Even App (Flutter/Dart host)              |
|            on user's iPhone                       |
|     flutter_inappwebview hosts your app           |
+--------------------------------------------------+
                    |
                    | Protobuf over BLE 5.4
                    |
+--------------------------------------------------+
|          Even G2 Smart Glasses                    |
|     576x288 monochrome green microLED             |
+--------------------------------------------------+
```

### Communication Flow

**Outgoing (your app -> glasses):**
1. Your app calls an SDK method (e.g., `createStartUpPageContainer`)
2. The SDK serializes parameters to JSON and sends via `flutter_inappwebview.callHandler('evenAppMessage', ...)`
3. The Even App receives the message, encodes it as protobuf, and sends to the glasses via BLE
4. The glasses render the UI
5. The result propagates back as a resolved Promise

**Incoming (glasses -> your app):**
1. The user performs a gesture on the ring or a system event occurs
2. The glasses send the event to the Even App via BLE
3. The Even App calls `window._listenEvenAppMessage(...)` in your WebView
4. The SDK parses the event and dispatches it to your registered callbacks

---

## 4. Getting Started

### Prerequisites

- Node.js >= 20.0.0
- npm, yarn, or pnpm
- A web framework of your choice (React, Vue, Svelte, plain JS, Kotlin/Wasm, etc.)
- Access to the Even Hub developer program (apply at [evenhub.evenrealities.com](https://evenhub.evenrealities.com/))

### Installation

```bash
npm install @evenrealities/even_hub_sdk
```

### Minimal "Hello World" App

```typescript
import { waitForEvenAppBridge, CreateStartUpPageContainer } from '@evenrealities/even_hub_sdk';

async function main() {
  // Step 1: Wait for the bridge to be ready
  const bridge = await waitForEvenAppBridge();

  // Step 2: Get user and device info (optional but recommended)
  const user = await bridge.getUserInfo();
  const device = await bridge.getDeviceInfo();
  console.log(`Hello, ${user.name}! Device: ${device?.model}`);

  // Step 3: Create the glasses UI
  const result = await bridge.createStartUpPageContainer({
    containerTotalNum: 1,
    textObject: [{
      containerID: 1,
      containerName: 'hello',
      xPosition: 20,
      yPosition: 100,
      width: 536,
      height: 80,
      borderWidth: 1,
      borderColor: 13,
      borderRdaius: 6,
      paddingLength: 12,
      content: `Hello ${user.name}!`,
      isEventCapture: 1,
    }],
  });

  if (result === 0) {
    console.log('Glasses UI created successfully!');
  }

  // Step 4: Listen for events
  bridge.onEvenHubEvent((event) => {
    if (event.sysEvent) {
      console.log('System event:', event.sysEvent.eventType);
    }
    if (event.textEvent) {
      console.log('Text event:', event.textEvent.eventType);
    }
  });

  // Step 5: Listen for device status changes
  bridge.onDeviceStatusChanged((status) => {
    console.log('Device status:', status.connectType, 'Battery:', status.batteryLevel);
  });
}

main();
```

### Project Structure (Recommended)

```
your-app/
  app.json              # Even Hub manifest (required)
  index.html            # Entry point (referenced by app.json)
  package.json          # Dependencies including even_hub_sdk
  src/
    main.ts             # App entry, bridge initialization
    state.ts            # App state management
    containers.ts       # Glasses UI container builders
    events.ts           # Event handlers
    types.ts            # Your data models
  assets/               # Static assets (if needed)
```

---

## 5. The Even Hub SDK

### Package Details

| Field | Value |
|-------|-------|
| Package | `@evenrealities/even_hub_sdk` |
| Version | 0.0.6 (latest as of this writing) |
| License | MIT |
| Module type | ESM (with CJS fallback) |
| TypeScript | Full type definitions included |

### Initialization

Always initialize the bridge before calling any other SDK methods:

```typescript
import { waitForEvenAppBridge } from '@evenrealities/even_hub_sdk';

// Recommended approach: await the bridge
const bridge = await waitForEvenAppBridge();

// Alternative: get singleton directly (only if you know it's ready)
// import { EvenAppBridge } from '@evenrealities/even_hub_sdk';
// const bridge = EvenAppBridge.getInstance();
```

The bridge uses a singleton pattern. `waitForEvenAppBridge()` returns a Promise that resolves once the bridge has been initialized and the WebView communication channel is established. This is the safest way to start.

### How the Bridge Works Internally

1. On construction, `EvenAppBridge` exposes itself to `window.EvenAppBridge`
2. It registers `window._evenAppHandleMessage` for incoming messages from the Even App
3. It waits for the DOM to be ready, then marks itself as `ready`
4. Outgoing calls use `flutter_inappwebview.callHandler('evenAppMessage', message)` which returns a Promise with the native result
5. Incoming events arrive via `window._listenEvenAppMessage` and are parsed and dispatched to registered callbacks

---

## 6. App Manifest (app.json)

Every Even Hub app requires an `app.json` manifest in the project root:

```json
{
  "package_id": "com.yourcompany.yourapp",
  "edition": "202601",
  "name": "Your App Name",
  "version": "1.0.0",
  "min_app_version": "0.1.0",
  "tagline": "Short description of your app",
  "description": "Longer description of what your app does",
  "author": "Your Name <your.email@example.com>",
  "entrypoint": "index.html",
  "permissions": {
    "network": [
      "evenhub.evenrealities.com",
      "api.yourservice.com"
    ],
    "fs": [
      "./assets"
    ]
  }
}
```

### Field Reference

| Field | Type | Description |
|-------|------|-------------|
| `package_id` | string | Reverse-domain app identifier |
| `edition` | string | Edition code in YYYYMM format |
| `name` | string | Display name of the app |
| `version` | string | Semantic version (major.minor.patch) |
| `min_app_version` | string | Minimum Even App version required |
| `tagline` | string | Short one-line description |
| `description` | string | Full description |
| `author` | string | Author name and email |
| `entrypoint` | string | Path to the HTML entry point |
| `permissions.network` | string[] | Whitelisted network domains |
| `permissions.fs` | string[] | Allowed filesystem paths |

---

## 7. SDK API Reference

### Basic Information APIs

```typescript
// Get logged-in user info
const user: UserInfo = await bridge.getUserInfo();
// user.uid, user.name, user.avatar, user.country

// Get connected device info
const device: DeviceInfo | null = await bridge.getDeviceInfo();
// device.model (g1, g2, ring1), device.sn, device.status

// Persist data on the app side
await bridge.setLocalStorage('key', 'value');
const value: string = await bridge.getLocalStorage('key');
```

### Glasses UI APIs (Page Containers)

| Method | Returns | When to Use |
|--------|---------|-------------|
| `createStartUpPageContainer(config)` | `Promise<StartUpPageCreateResult>` | **Once** on first launch to create the initial UI |
| `rebuildPageContainer(config)` | `Promise<boolean>` | **All subsequent** page updates or new pages |
| `textContainerUpgrade(config)` | `Promise<boolean>` | Update text content in an existing container |
| `updateImageRawData(data)` | `Promise<ImageRawDataUpdateResult>` | Push image data to an image container |
| `shutDownPageContainer(exitMode?)` | `Promise<boolean>` | Exit the glasses UI (0 = immediate, 1 = ask user) |

### Event Listening APIs

```typescript
// Device status changes (connection, battery, wearing)
const unsubDevice = bridge.onDeviceStatusChanged((status: DeviceStatus) => {
  // status.connectType, status.batteryLevel, status.isWearing, etc.
});

// Glasses UI events (user interactions + system events)
const unsubEvents = bridge.onEvenHubEvent((event: EvenHubEvent) => {
  if (event.listEvent) { /* list item selected */ }
  if (event.textEvent) { /* scroll or click on text */ }
  if (event.sysEvent)  { /* double-click, foreground, etc. */ }
});

// Always clean up when done
unsubDevice();
unsubEvents();
```

### Data Types

#### UserInfo
```typescript
{
  uid: number;       // User ID
  name: string;      // Display name
  avatar: string;    // Avatar URL
  country: string;   // Country
}
```

#### DeviceInfo
```typescript
{
  model: DeviceModel;    // 'g1' | 'g2' | 'ring1'
  sn: string;            // Serial number (read-only)
  status: DeviceStatus;  // Current status
}
```

#### DeviceStatus
```typescript
{
  sn: string;                         // Serial number
  connectType: DeviceConnectType;     // 'none' | 'connecting' | 'connected' | 'disconnected' | 'connectionFailed'
  isWearing?: boolean;
  batteryLevel?: number;              // 0-100
  isCharging?: boolean;
  isInCase?: boolean;
}
```

#### StartUpPageCreateResult
```typescript
enum StartUpPageCreateResult {
  success = 0,
  invalid = 1,      // Invalid container configuration
  oversize = 2,     // Container exceeds canvas bounds
  outOfMemory = 3,  // Not enough memory on glasses
}
```

---

## 8. The Glasses UI System

The glasses UI is built from **containers** -- rectangular regions on the 576x288 canvas. You compose a page by specifying container positions, sizes, and content, then sending the configuration to the glasses.

### Container Types

There are three container types:

#### 1. Text Container
Displays text content. Supports event capture for scroll events.

```typescript
const textContainer: TextContainerProperty = {
  containerID: 1,          // Unique ID for this container
  containerName: 'mytext', // Name (max 16 chars)
  xPosition: 0,            // X position (0-576)
  yPosition: 0,            // Y position (0-288)
  width: 400,              // Width (0-576)
  height: 200,             // Height (0-288)
  borderWidth: 1,          // Border width (0-5)
  borderColor: 13,         // Border color (0-16)
  borderRdaius: 6,         // Border radius (0-10) [note: "Rdaius" is the SDK spelling]
  paddingLength: 12,       // Internal padding (0-32)
  content: 'Your text here', // Text content (max 1000 chars at startup)
  isEventCapture: 1,       // 1 = this container receives events, 0 = does not
};
```

#### 2. List Container
Displays a scrollable/selectable list of items. Reports selection events.

```typescript
const listContainer: ListContainerProperty = {
  containerID: 2,
  containerName: 'mylist',
  xPosition: 0,
  yPosition: 0,
  width: 110,
  height: 200,
  borderWidth: 1,
  borderColor: 13,
  borderRdaius: 6,
  paddingLength: 5,
  isEventCapture: 1,
  itemContainer: {
    itemCount: 5,                            // Number of items (1-20)
    itemWidth: 100,                          // Item width (0 = auto fill)
    isItemSelectBorderEn: 1,                 // 1 = highlight selected item
    itemName: ['Ch 1', 'Ch 2', 'Ch 3', 'Ch 4', 'Ch 5'],  // Item labels (max 64 chars each)
  },
};
```

#### 3. Image Container
Displays an image. Image data must be sent separately after creation.

```typescript
const imageContainer: ImageContainerProperty = {
  containerID: 3,
  containerName: 'myimage',
  xPosition: 0,
  yPosition: 0,
  width: 100,              // Width (20-200)
  height: 80,              // Height (20-100)
};
```

### Container Constraints Summary

| Property | Range | Notes |
|----------|-------|-------|
| `xPosition` | 0-576 | Left edge position |
| `yPosition` | 0-288 | Top edge position |
| `width` | 0-576 (images: 20-200) | |
| `height` | 0-288 (images: 20-100) | |
| `borderWidth` | 0-5 | |
| `borderColor` | 0-15 (list), 0-16 (text) | Numeric color values |
| `borderRdaius` | 0-10 | Note the "Rdaius" spelling in the SDK |
| `paddingLength` | 0-32 | |
| `containerName` | max 16 characters | |
| `content` (startup) | max 1000 characters | For `createStartUpPageContainer` |
| `content` (upgrade) | max 2000 characters | For `textContainerUpgrade` |
| `itemCount` | 1-20 | Maximum list items |
| `itemName` each | max 64 characters | |
| **Total containers** | **max 4 per page** | |
| **Event capture** | **exactly 1 per page** | Only one container can have `isEventCapture: 1` |

### Creating vs. Rebuilding Pages

This is a critical distinction:

- **`createStartUpPageContainer`**: Call this **exactly once** when your app first starts. It creates the initial glasses UI. Calling it again has no effect.
- **`rebuildPageContainer`**: Use this for **all subsequent** page changes. Same parameter structure. This tears down the current page and builds a new one.

```typescript
// First time: create
const result = await bridge.createStartUpPageContainer({
  containerTotalNum: 2,
  textObject: [titleContainer],
  listObject: [menuContainer],
});

// Later: rebuild with different content
await bridge.rebuildPageContainer({
  containerTotalNum: 1,
  textObject: [detailContainer],
});
```

### Updating Text Content

To update text in an existing container without rebuilding the entire page:

```typescript
await bridge.textContainerUpgrade({
  containerID: 1,           // Must match the container's ID
  containerName: 'mytext',  // Must match the container's name
  content: 'Updated text content (max 2000 characters)',
});
```

This is more efficient than `rebuildPageContainer` when you only need to change text content.

### Shutting Down

When your app is done with the glasses UI:

```typescript
// Exit immediately
await bridge.shutDownPageContainer(0);

// Or let the user decide
await bridge.shutDownPageContainer(1);
```

Always call this when the user exits your app's reading/active mode to properly release the glasses display.

---

## 9. Event System

### Event Types

All events come through `onEvenHubEvent`. The callback receives an `EvenHubEvent` object with three optional properties -- check which one is not null:

```typescript
bridge.onEvenHubEvent((event) => {
  if (event.listEvent) {
    // User interacted with a list container
    handleListEvent(event.listEvent);
  } else if (event.textEvent) {
    // User interacted with a text container
    handleTextEvent(event.textEvent);
  } else if (event.sysEvent) {
    // System-level event
    handleSysEvent(event.sysEvent);
  }
});
```

### List Events (List_ItemEvent)

Fired when a user selects an item in a list container.

```typescript
{
  containerID: number;           // Which container
  containerName: string;         // Container name
  currentSelectItemName: string; // Selected item's label
  currentSelectItemIndex: number;// Selected item's 0-based index
  eventType: OsEventTypeList;    // The interaction type (CLICK_EVENT, etc.)
}
```

### Text Events (Text_ItemEvent)

Fired when a user scrolls or clicks on a text container.

```typescript
{
  containerID: number;
  containerName: string;
  eventType: OsEventTypeList;  // SCROLL_TOP_EVENT, SCROLL_BOTTOM_EVENT, CLICK_EVENT, etc.
}
```

### System Events (Sys_ItemEvent)

Fired for system-level interactions.

```typescript
{
  eventType: OsEventTypeList;
  // DOUBLE_CLICK_EVENT (3): user double-tapped
  // FOREGROUND_ENTER_EVENT (4): app entered foreground
  // FOREGROUND_EXIT_EVENT (5): app left foreground
  // ABNORMAL_EXIT_EVENT (6): app crashed
}
```

### OsEventTypeList Values

| Name | Value | Typical Use |
|------|-------|-------------|
| `CLICK_EVENT` | 0 | Item selection, confirm action |
| `SCROLL_TOP_EVENT` | 1 | Navigate up, previous page |
| `SCROLL_BOTTOM_EVENT` | 2 | Navigate down, next page |
| `DOUBLE_CLICK_EVENT` | 3 | Toggle modes (e.g., full-screen) |
| `FOREGROUND_ENTER_EVENT` | 4 | Resume app state |
| `FOREGROUND_EXIT_EVENT` | 5 | Pause/save state |
| `ABNORMAL_EXIT_EVENT` | 6 | Emergency cleanup |

### Device Status Events

Separate from the EvenHub events, device status is monitored via:

```typescript
const unsub = bridge.onDeviceStatusChanged((status) => {
  console.log('Connection:', status.connectType);
  console.log('Battery:', status.batteryLevel);
  console.log('Wearing:', status.isWearing);
  console.log('Charging:', status.isCharging);
  console.log('In Case:', status.isInCase);
});
```

---

## 10. App Lifecycle

### Initialization Sequence

```
1. Bridge becomes ready
   └── await waitForEvenAppBridge()

2. Fetch user and device info
   ├── getUserInfo()
   └── getDeviceInfo()

3. Set up event listeners
   ├── onDeviceStatusChanged(callback)
   └── onEvenHubEvent(callback)

4. Create initial glasses UI
   └── createStartUpPageContainer(config)

5. App is now active
   └── Handle events, update content
```

### Page Navigation Pattern

```
createStartUpPageContainer()   <-- first and only call
        |
        v
rebuildPageContainer()         <-- for every subsequent page
        |
        v
textContainerUpgrade()         <-- for text-only updates
        |
        v
rebuildPageContainer()         <-- for layout changes
        |
        v
shutDownPageContainer()        <-- when exiting
```

### Cleanup

Always clean up when your app is done:

```typescript
// Unsubscribe from events
unsubscribeDeviceStatus();
unsubscribeEvenHubEvent();

// Shut down the glasses UI
await bridge.shutDownPageContainer(0);
```

### Handling System Events

```typescript
bridge.onEvenHubEvent((event) => {
  if (event.sysEvent) {
    switch (event.sysEvent.eventType) {
      case OsEventTypeList.FOREGROUND_ENTER_EVENT:
        // App came to foreground -- restore state
        break;
      case OsEventTypeList.FOREGROUND_EXIT_EVENT:
        // App went to background -- save state
        break;
      case OsEventTypeList.ABNORMAL_EXIT_EVENT:
        // App crashed -- emergency cleanup
        break;
      case OsEventTypeList.DOUBLE_CLICK_EVENT:
        // User double-tapped -- toggle mode
        break;
    }
  }
});
```

---

## 11. Working with Images

Image containers are more complex than text and list containers. The key difference is that **image data cannot be sent during page creation** -- you must create the container first, then push the image data separately.

### Image Workflow

```typescript
// 1. Create the page with an image placeholder
await bridge.createStartUpPageContainer({
  containerTotalNum: 1,
  imageObject: [{
    containerID: 1,
    containerName: 'img',
    xPosition: 0,
    yPosition: 0,
    width: 100,   // 20-200
    height: 80,   // 20-100
  }],
});

// 2. After creation succeeds, push image data
await bridge.updateImageRawData({
  containerID: 1,
  containerName: 'img',
  imageData: myImageBytes, // number[], Uint8Array, ArrayBuffer, or base64 string
});
```

### Image Data Formats

The `imageData` field accepts:
- **`number[]`** (recommended): Array of byte values (0-255)
- **`Uint8Array`**: Automatically converted to `number[]` by the SDK
- **`ArrayBuffer`**: Automatically converted to `number[]` by the SDK
- **`string`**: Base64-encoded image data

### Image Constraints

| Constraint | Value |
|------------|-------|
| Width | 20 - 200 pixels |
| Height | 20 - 100 pixels |
| Color | Monochrome (converted to grayscale/1-bit internally) |
| Transmission | **Sequential only** -- wait for each upload to complete |
| Frequency | Avoid rapid updates -- glasses have limited memory |

### Best Practices for Images

1. **Use simple graphics**: The display is monochrome green. High-contrast, simple shapes work best.
2. **Send sequentially**: Never send multiple images concurrently. Queue them and wait for each `updateImageRawData` to resolve before sending the next.
3. **Minimize frequency**: Due to memory limitations on the glasses, avoid updating images more than necessary.
4. **Keep sizes small**: Smaller images transmit faster over BLE. Use the minimum dimensions needed.

### Example: Canvas-to-Image Pipeline

From the InNovel example app (TextImageView), here is the pattern for rendering a canvas and uploading it:

```typescript
// 1. Create an HTML canvas at the desired dimensions
const canvas = document.createElement('canvas');
canvas.width = 100;
canvas.height = 80;
const ctx = canvas.getContext('2d');

// 2. Draw your content
ctx.fillStyle = '#4A90E2';
ctx.fillRect(10, 10, 80, 60);

// 3. Convert to PNG data URL
const dataUrl = canvas.toDataURL('image/png');

// 4. Decode base64 to binary
const base64 = dataUrl.split(',')[1];
const binary = atob(base64);
const bytes = new Uint8Array(binary.length);
for (let i = 0; i < binary.length; i++) {
  bytes[i] = binary.charCodeAt(i);
}

// 5. Upload to glasses
await bridge.updateImageRawData({
  containerID: 100,
  containerName: 'canvas-img',
  imageData: bytes.buffer, // ArrayBuffer -- SDK converts to number[]
});
```

---

## 12. Best Practices

### UI Design

1. **Keep it simple**: The display is 576x288 monochrome. Dense UIs are unreadable. Use large, clear text and simple layouts.

2. **One action at a time**: The user has limited input (click, scroll, double-click). Design linear flows that don't require complex navigation.

3. **Respect the canvas bounds**: Containers that exceed the 576x288 canvas will be rejected (`oversize` error). Always validate your positions + sizes fit within bounds.

4. **Use padding and borders**: The `paddingLength` and `borderWidth` properties help make text readable. The InNovel example uses `paddingLength: 12` and `borderWidth: 1` with `borderRdaius: 6` for a clean look.

5. **Limit text length**: At startup, text containers accept max 1000 characters. After startup, `textContainerUpgrade` allows up to 2000. For longer content, paginate it (see the fragment pattern below).

6. **Exactly one event-capturing container**: Every page must have exactly one container with `isEventCapture: 1`. This is the container that receives user interaction events. All others must be `0`.

### Content Pagination

For content longer than what fits on screen, split it into fragments and navigate with scroll events:

```typescript
function splitContent(text: string, maxChars: number = 200): string[] {
  const fragments: string[] = [];
  for (let i = 0; i < text.length; i += maxChars) {
    fragments.push(text.substring(i, i + maxChars));
  }
  return fragments;
}

// In your text event handler:
if (event.textEvent?.eventType === OsEventTypeList.SCROLL_BOTTOM_EVENT) {
  currentIndex = Math.min(currentIndex + 1, fragments.length - 1);
  await bridge.textContainerUpgrade({
    containerID: 4,
    containerName: 'reader',
    content: fragments[currentIndex],
  });
}
```

### State Management

1. **Track bridge readiness**: Don't attempt SDK calls before the bridge is ready. Gate your UI on this state.

2. **Store reading position**: Use `setLocalStorage` / `getLocalStorage` to persist user progress across sessions.

3. **Handle errors gracefully**: Every SDK call can fail. Use try/catch or `.catch()` and update your UI state accordingly.

4. **Clean up subscriptions**: Always store and call unsubscribe functions when you're done listening.

```typescript
// Store unsubscribe functions
const unsubDevice = bridge.onDeviceStatusChanged(handleStatus);
const unsubEvents = bridge.onEvenHubEvent(handleEvent);

// On cleanup
function dispose() {
  unsubDevice();
  unsubEvents();
}
```

### Performance

1. **Prefer `textContainerUpgrade` over `rebuildPageContainer`**: If only text content changes, updating in place is cheaper than rebuilding the entire page.

2. **Batch state changes**: Don't call multiple SDK methods in rapid succession. The BLE connection has limited throughput.

3. **Minimize initial payload**: Keep `createStartUpPageContainer` content minimal (under 1000 chars per text container). Load more content later via `textContainerUpgrade`.

4. **Don't poll**: Use event listeners instead of polling for state changes. The SDK pushes events to you.

### Error Handling

```typescript
try {
  const result = await bridge.createStartUpPageContainer(config);
  switch (result) {
    case 0: // success
      break;
    case 1: // invalid configuration
      console.error('Invalid container configuration');
      break;
    case 2: // oversize
      console.error('Container exceeds canvas bounds');
      break;
    case 3: // out of memory
      console.error('Glasses ran out of memory');
      break;
  }
} catch (e) {
  console.error('Bridge call failed:', e);
}
```

---

## 13. Common Patterns from the Example App

The InNovel example app demonstrates several patterns worth replicating.

### Pattern 1: Multi-Container Layout (List + Text)

A common pattern is a sidebar list with a detail text area:

```
+------------------------------------------+
|  Book Title -- Author                     |  <- Text container (info)
+----------+-------------------------------+
|          |                               |
| Ch 1     |  Chapter content preview...   |  <- List + Text containers
| Ch 2     |                               |
| Ch 3     |  Double-click for full screen |
| Ch 4     |                               |
| ...      |                               |
+----------+-------------------------------+
```

```typescript
const page = {
  containerTotalNum: 3,
  textObject: [
    { containerID: 2, containerName: 'info',
      xPosition: 0, yPosition: 0, width: 530, height: 30,
      content: 'Title -- Author', ...styles },
    { containerID: 3, containerName: 'content',
      xPosition: 115, yPosition: 35, width: 415, height: 200,
      content: 'Chapter preview...', ...styles },
  ],
  listObject: [
    { containerID: 1, containerName: 'chapters',
      xPosition: 0, yPosition: 35, width: 110, height: 200,
      isEventCapture: 1,
      itemContainer: {
        itemCount: 8,
        itemName: ['Ch 1', 'Ch 2', ...],
        isItemSelectBorderEn: 1,
      },
      ...styles },
  ],
};
```

### Pattern 2: Full-Screen Toggle via Double-Click

Use the system `DOUBLE_CLICK_EVENT` to toggle between a multi-container view and a single full-screen container:

```typescript
bridge.onEvenHubEvent((event) => {
  if (event.sysEvent?.eventType === OsEventTypeList.DOUBLE_CLICK_EVENT) {
    if (!isFullScreen) {
      // Enter full screen: rebuild with single text container
      await bridge.rebuildPageContainer({
        containerTotalNum: 1,
        textObject: [{
          containerID: 4,
          containerName: 'reader',
          xPosition: 0, yPosition: 0,
          width: 500, height: 235,
          isEventCapture: 1,
          content: currentFragment,
          ...styles,
        }],
      });
      isFullScreen = true;
    } else {
      // Exit full screen: rebuild with original multi-container layout
      await bridge.rebuildPageContainer(originalPageConfig);
      isFullScreen = false;
    }
  }
});
```

### Pattern 3: List Selection Updates Detail View

When the user selects a list item, update the text container:

```typescript
bridge.onEvenHubEvent((event) => {
  if (event.listEvent) {
    const index = event.listEvent.currentSelectItemIndex;
    if (index !== undefined && index >= 0) {
      const item = items[index];
      await bridge.textContainerUpgrade({
        containerID: 3,
        containerName: 'content',
        content: item.detail,
      });
    }
  }
});
```

### Pattern 4: Scroll-Based Pagination

For long text content in full-screen mode, split into fragments and navigate:

```typescript
bridge.onEvenHubEvent((event) => {
  if (!isFullScreen || !event.textEvent) return;

  switch (event.textEvent.eventType) {
    case OsEventTypeList.SCROLL_BOTTOM_EVENT:
      if (fragmentIndex < fragments.length - 1) {
        fragmentIndex++;
        await bridge.textContainerUpgrade({
          containerID: 4,
          containerName: 'reader',
          content: fragments[fragmentIndex],
        });
      }
      break;

    case OsEventTypeList.SCROLL_TOP_EVENT:
      if (fragmentIndex > 0) {
        fragmentIndex--;
        await bridge.textContainerUpgrade({
          containerID: 4,
          containerName: 'reader',
          content: fragments[fragmentIndex],
        });
      }
      break;
  }
});
```

### Pattern 5: Device-Aware UI

Check device connection before attempting glasses operations:

```typescript
const device = await bridge.getDeviceInfo();
if (!device || device.status?.connectType !== 'connected') {
  showMessage('Please connect your Even G2 glasses first');
  return;
}

// Safe to proceed with glasses UI operations
await bridge.createStartUpPageContainer(config);
```

---

## 14. Troubleshooting

### Common Issues

| Problem | Cause | Solution |
|---------|-------|----------|
| Bridge never becomes ready | App not running in the Even App WebView | Test only inside the Even App |
| `createStartUpPageContainer` returns 1 (invalid) | Bad container config (overlapping IDs, missing `isEventCapture`, etc.) | Check that exactly one container has `isEventCapture: 1`, IDs are unique, positions fit canvas |
| `createStartUpPageContainer` returns 2 (oversize) | Container dimensions exceed 576x288 | Check `xPosition + width <= 576` and `yPosition + height <= 288` |
| `createStartUpPageContainer` returns 3 (outOfMemory) | Too much content or too many containers | Reduce text length, use fewer containers, smaller images |
| Events not firing | No container with `isEventCapture: 1` | Ensure exactly one container per page has event capture enabled |
| Page doesn't update | Using `createStartUpPageContainer` again instead of `rebuildPageContainer` | Use `createStartUpPageContainer` only once; use `rebuildPageContainer` for all subsequent changes |
| Image not showing | Image data not sent after container creation | Call `updateImageRawData` after `createStartUpPageContainer` succeeds |
| `textContainerUpgrade` fails | Container ID/name mismatch | Ensure `containerID` and `containerName` match the original container |
| Field name "borderRdaius" | This is the actual spelling in the SDK protobuf definition | Use `borderRdaius` (not `borderRadius`) |

### Debugging Tips

1. **Use `console.log` extensively**: Since your app runs in a WebView, check the Even App's debug console or use remote debugging.
2. **Check `event.jsonData`**: The raw JSON data is preserved in every `EvenHubEvent` for debugging and replay.
3. **Monitor device status**: Log `onDeviceStatusChanged` events to understand connectivity issues.
4. **Test incrementally**: Build your page one container at a time to isolate configuration errors.

### Key Naming Compatibility

The SDK supports multiple naming conventions for event fields:

| camelCase | PascalCase | Proto Name |
|-----------|------------|------------|
| `containerID` | `ContainerID` | `Container_ID` |
| `containerName` | `ContainerName` | `Container_Name` |
| `currentSelectItemIndex` | `CurrentSelectItemIndex` | `CurrentSelect_ItemIndex` |
| `eventType` | `EventType` | `Event_Type` |

The SDK's `pickLoose` function normalizes keys by removing underscores and lowercasing, so all three forms work. When sending data, use camelCase.

---

## 15. Resources

### Official

- **Even Hub Developer Portal**: [evenhub.evenrealities.com](https://evenhub.evenrealities.com/) -- Apply for the Early Developer Program
- **Even Realities GitHub**: [github.com/even-realities](https://github.com/even-realities)
- **EH-InNovel Example App**: [github.com/even-realities/EH-InNovel](https://github.com/even-realities/EH-InNovel) -- The reference implementation this guide is based on
- **SDK npm Package**: `@evenrealities/even_hub_sdk` (v0.0.6)
- **SDK Contact**: Whiskee Chen (whiskee.chen@evenrealities.com)
- **Support Center**: [support.evenrealities.com](https://support.evenrealities.com/hc/en-us)

### Community

- **Discord**: Private channel for pilot developers (access via developer program)
- **awesome-even-realities-g1**: [github.com/galfaroth/awesome-even-realities-g1](https://github.com/galfaroth/awesome-even-realities-g1) -- Community project list
- **EvenDemoApp** (G1): [github.com/even-realities/EvenDemoApp](https://github.com/even-realities/EvenDemoApp) -- Low-level BLE demo for G1 (440+ stars)

### Technology Stack References

- [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)
- [Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform)
- [Kotlin/Wasm](https://kotl.in/wasm/)

---

## Appendix A: Complete Container Property Reference

### TextContainerProperty

| Property | Type | Range | Required | Notes |
|----------|------|-------|----------|-------|
| `containerID` | number | any | yes | Unique per page |
| `containerName` | string | max 16 chars | yes | |
| `xPosition` | number | 0-576 | yes | |
| `yPosition` | number | 0-288 | yes | |
| `width` | number | 0-576 | yes | |
| `height` | number | 0-288 | yes | |
| `borderWidth` | number | 0-5 | no | |
| `borderColor` | number | 0-16 | no | |
| `borderRdaius` | number | 0-10 | no | |
| `paddingLength` | number | 0-32 | no | |
| `content` | string | max 1000 (startup) / 2000 (upgrade) | no | |
| `isEventCapture` | number | 0 or 1 | yes | Exactly one per page must be 1 |

### ListContainerProperty

| Property | Type | Range | Required | Notes |
|----------|------|-------|----------|-------|
| `containerID` | number | any | yes | |
| `containerName` | string | max 16 chars | yes | |
| `xPosition` | number | 0-576 | yes | |
| `yPosition` | number | 0-288 | yes | |
| `width` | number | 0-576 | yes | |
| `height` | number | 0-288 | yes | |
| `borderWidth` | number | 0-5 | no | |
| `borderColor` | number | 0-15 | no | |
| `borderRdaius` | number | 0-10 | no | |
| `paddingLength` | number | 0-32 | no | |
| `isEventCapture` | number | 0 or 1 | yes | |
| `itemContainer` | object | | yes | See ListItemContainerProperty |

### ListItemContainerProperty

| Property | Type | Range | Required | Notes |
|----------|------|-------|----------|-------|
| `itemCount` | number | 1-20 | yes | |
| `itemWidth` | number | 0 = auto | no | 0 means auto-fill width |
| `isItemSelectBorderEn` | number | 0 or 1 | no | Show border on selected item |
| `itemName` | string[] | max 20 items, 64 chars each | yes | |

### ImageContainerProperty

| Property | Type | Range | Required | Notes |
|----------|------|-------|----------|-------|
| `containerID` | number | any | yes | |
| `containerName` | string | max 16 chars | yes | |
| `xPosition` | number | 0-576 | yes | |
| `yPosition` | number | 0-288 | yes | |
| `width` | number | 20-200 | yes | Smaller range than text/list |
| `height` | number | 20-100 | yes | Smaller range than text/list |

### TextContainerUpgrade

| Property | Type | Range | Required | Notes |
|----------|------|-------|----------|-------|
| `containerID` | number | | yes | Must match existing container |
| `containerName` | string | max 16 chars | yes | Must match existing container |
| `contentOffset` | number | | no | |
| `contentLength` | number | | no | |
| `content` | string | max 2000 chars | yes | |

### ImageRawDataUpdate

| Property | Type | Required | Notes |
|----------|------|----------|-------|
| `containerID` | number | yes | Must match existing container |
| `containerName` | string | yes | Must match existing container |
| `imageData` | number[] / Uint8Array / ArrayBuffer / string | yes | `number[]` recommended |

### ShutDownContainer

| Property | Type | Default | Notes |
|----------|------|---------|-------|
| `exitMode` | number | 0 | 0 = immediate, 1 = ask user |

---

## Appendix B: Using the SDK with Different Frameworks

The SDK is framework-agnostic. Here are notes for specific setups:

### Plain JavaScript / TypeScript
Direct usage as shown in this guide. Import from the npm package.

### React
```typescript
import { useEffect, useState } from 'react';
import { waitForEvenAppBridge, EvenAppBridge } from '@evenrealities/even_hub_sdk';

function useEvenBridge() {
  const [bridge, setBridge] = useState<EvenAppBridge | null>(null);

  useEffect(() => {
    waitForEvenAppBridge().then(setBridge);
  }, []);

  return bridge;
}
```

### Kotlin Multiplatform (Compose for Web)
This is what the InNovel example uses. The approach requires:
1. Add `@evenrealities/even_hub_sdk` as an npm dependency in your `build.gradle.kts`
2. Declare `external` classes in `jsMain` or `wasmJsMain` for `EvenAppBridge` and `waitForEvenAppBridge`
3. Use `expect`/`actual` to share bridge functions across `webMain`/`jsMain`/`wasmJsMain`
4. Bridge `Promise` results to Kotlin coroutines using `kotlinx.coroutines.await()` (JS) or a custom `awaitWasm()` (Wasm)
5. Parse JS objects to Kotlin data classes at the boundary

See the InNovel source code for a complete working implementation of this approach.

### Vue / Svelte / Other
The SDK exports standard ES modules. Import and use as you would any npm package. The bridge initialization is async, so initialize it in your app's startup hook and store the reference.

---

*This guide was assembled from the EH-InNovel reference implementation, the `@evenrealities/even_hub_sdk` v0.0.6 documentation, and publicly available Even Realities hardware specifications. For the latest information, check the [Even Hub developer portal](https://evenhub.evenrealities.com/).*
