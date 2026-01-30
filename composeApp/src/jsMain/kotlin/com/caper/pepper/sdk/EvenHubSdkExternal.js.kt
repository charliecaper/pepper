@file:OptIn(ExperimentalWasmJsInterop::class)
@file:JsModule("@evenrealities/even_hub_sdk")
@file:JsNonModule

package com.caper.pepper.sdk

import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.Promise
import kotlin.js.definedExternally

/**
 * Kotlin/JS externals for @evenrealities/even_hub_sdk (declarations only, no implementation logic).
 *
 * Step 1: Keep this file as `external` declarations only (because `@JsModule` is used).
 * Step 2: For actual business calls, use the `actual` wrapper methods in `EvenHubBridge.js.kt`.
 * Step 3: SDK method signatures follow the TypeScript SDK (npm package `@evenrealities/even_hub_sdk`).
 */
external class EvenAppBridge {
    val ready: Boolean
    // Generic bridge entry: method name + optional params.
    fun callEvenApp(method: String, params: JsAny? = definedExternally): Promise<JsAny?>
    // Convenience wrappers provided by the JS SDK.
    fun getUserInfo(): Promise<JsAny?>
    fun getDeviceInfo(): Promise<JsAny?>
    fun onDeviceStatusChanged(callback: (status: JsAny?) -> Unit): () -> Unit
    fun onEvenHubEvent(callback: (event: JsAny?) -> Unit): () -> Unit

    companion object {
        fun getInstance(): EvenAppBridge
    }
}

external fun waitForEvenAppBridge(): Promise<EvenAppBridge>
