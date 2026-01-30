@file:OptIn(ExperimentalWasmJsInterop::class)

package com.caper.pepper.sdk

import kotlinx.coroutines.await
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny

/**
 * Kotlin/JS implementation (actual) of even_hub_sdk.
 *
 * Step 1: Wait for the JS-side bridge to be ready via `waitForEvenAppBridge().await()`.
 * Step 2: Call methods: `callEvenApp("getUserInfo")` or `callEvenApp("method", params)`.
 * Step 3: Call with parameters: organize params as a JS object (or use `callEvenAppJson` with a JSON string).
 * Step 4: Listen for device status changes: `observeDeviceStatus { ... }`.
 *
 * Note: In the callEvenApp message structure, params are passed directly as the data field.
 * JS SDK internal message structure: { type: "call_even_app_method", method: method, data: params }
 */
actual suspend fun ensureEvenAppBridge() {
    waitForEvenAppBridge().await()
}

// Call with params packaged as a JSON string for shared code.
actual suspend fun callEvenApp(method: String, params: JsAny?): JsAny? =
    EvenAppBridge.getInstance().callEvenApp(method, params).await()

actual suspend fun callEvenAppJson(method: String, paramsJson: String): JsAny? =
    callEvenApp(method, jsParseJson(paramsJson))

actual suspend fun getUserInfo(): UserInfo? =
    userInfoFromJs(EvenAppBridge.getInstance().getUserInfo().await())

// Parse SDK returns into Kotlin models at the boundary.
actual suspend fun getDeviceInfo(): DeviceInfo? =
    deviceInfoFromJs(EvenAppBridge.getInstance().getDeviceInfo().await())

actual suspend fun createStartUpPageContainer(container: CreateStartUpPageContainer): Int? {
    val result = callEvenAppJson("createStartUpPageContainer", container.toJsonString())
    return jsToDoubleOrNull(result)?.toInt()
}

actual suspend fun rebuildPageContainer(container: RebuildPageContainer): Boolean {
    val result = callEvenAppJson("rebuildPageContainer", container.toJsonString())
    return jsToBoolOrNull(result) ?: false
}

actual suspend fun updateImageRawData(data: ImageRawDataUpdate): Boolean {
    val result = callEvenAppJson("updateImageRawData", data.toJsonString())
    return jsToBoolOrNull(result) ?: false
}

actual suspend fun textContainerUpgrade(container: TextContainerUpgrade): Boolean {
    val result = callEvenAppJson("textContainerUpgrade", container.toJsonString())
    return jsToBoolOrNull(result) ?: false
}

actual suspend fun shutDownPageContainer(container: ShutDownContainer): Boolean {
    val result = callEvenAppJson("shutDownPageContainer", container.toJsonString())
    return jsToBoolOrNull(result) ?: false
}

actual fun observeDeviceStatus(onChange: (DeviceStatus?) -> Unit): () -> Unit =
    EvenAppBridge.getInstance().onDeviceStatusChanged { status ->
        onChange(deviceStatusFromJs(status))
    }

// Bridge SDK events to Kotlin-friendly model.
actual fun observeEvenHubEvent(onChange: (EvenHubEvent?) -> Unit): () -> Unit =
    EvenAppBridge.getInstance().onEvenHubEvent { event ->
        onChange(evenHubEventFromJs(event))
    }