@file:OptIn(ExperimentalWasmJsInterop::class)

package com.fzfstudio.eh.innovel.sdk

import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny

/**
 * Web bridge API (implemented separately for Kotlin/JS and Kotlin/WasmJs).
 *
 * This is kept as expect/actual because Kotlin/Wasm JS interop has stricter type requirements than Kotlin/JS.
 *
 * Step 1: Add the local npm dependency `even_hub_sdk` in `composeApp/build.gradle.kts` under `webMain`.
 * Step 2: Call `ensureEvenAppBridge()` before using any SDK methods to wait for the bridge to be ready.
 * Step 3: Call native methods with `callEvenApp("method", params)`; prefer `callEvenAppJson("method", "{...}")` when parameters are needed.
 * Step 4: Listen for device status changes with `observeDeviceStatus { ... }`.
 *
 * Note: In the callEvenApp message structure, the data field equals params directly.
 * Message structure: { type: "call_even_app_method", method: method, data: params }
 */
expect suspend fun ensureEvenAppBridge()

/**
 * Call an Even App method.
 *
 * Note: params are passed directly as the message's data field.
 * The JS SDK builds the message structure internally: { type: "call_even_app_method", method: method, data: params }
 *
 * @param method Method name
 * @param params Method parameters (optional JsAny object, passed directly as data. Pass null or omit when no parameters are needed)
 * @return Method execution result
 */
expect suspend fun callEvenApp(method: String, params: JsAny? = null): JsAny?

/**
 * Convenience overload for shared `webMain` code: pass params as a JSON string (object/array/literal).
 * 
 * Note: paramsJson is parsed into a JS object, then passed directly as the message's data field.
 *
 * Example: `callEvenAppJson("setLocalStorage", "{\"key\":\"k\",\"value\":\"v\"}")`
 * This builds the message: { type: "call_even_app_method", method: "setLocalStorage", data: { key: "k", value: "v" } }
 */
expect suspend fun callEvenAppJson(method: String, paramsJson: String): JsAny?

/**
 * Get user info.
 */
expect suspend fun getUserInfo(): UserInfo?

/**
 * Get device info (glasses/ring info).
 */
expect suspend fun getDeviceInfo(): DeviceInfo?


/**
 * EvenHub - PB interface (aligned with host BleG2CmdProtoEvenHubExt).
 *
 * Note: Parameters are passed as JSON strings to avoid constructing JsAny directly in shared code.
 */
expect suspend fun createStartUpPageContainer(container: CreateStartUpPageContainer): Int?

expect suspend fun rebuildPageContainer(container: RebuildPageContainer): Boolean

expect suspend fun updateImageRawData(data: ImageRawDataUpdate): Boolean

expect suspend fun textContainerUpgrade(container: TextContainerUpgrade): Boolean

expect suspend fun shutDownPageContainer(container: ShutDownContainer): Boolean

/**
 * Observe device status changes.
 * @param onChange Callback invoked when the device status changes, receiving the full device status object
 * @return A function to cancel the observation
 */
expect fun observeDeviceStatus(onChange: (DeviceStatus?) -> Unit): () -> Unit

/**
 * Observe EvenHub events.
 * @param onChange Callback invoked when an event occurs, receiving an EvenHubEvent object
 * @return A function to cancel the observation
 */
expect fun observeEvenHubEvent(onChange: (EvenHubEvent?) -> Unit): () -> Unit
