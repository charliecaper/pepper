@file:OptIn(ExperimentalWasmJsInterop::class)
@file:JsModule("@evenrealities/even_hub_sdk")

package com.fzfstudio.eh.pepper.sdk

import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.Promise
import kotlin.js.definedExternally

/**
 * Kotlin/WasmJs externals for @evenrealities/even_hub_sdk.
 *
 * Step 1: This file only contains `external` declarations, mapping exports from npm package `@evenrealities/even_hub_sdk`.
 * Step 2: Wasm JS interop types are stricter; parameters/return values use `JsAny?`.
 * Step 3: Business calls go through `EvenHubBridge.wasmJs.kt`'s `actual` wrappers to avoid handling Promises everywhere.
 */
external class EvenAppBridge : JsAny {
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
