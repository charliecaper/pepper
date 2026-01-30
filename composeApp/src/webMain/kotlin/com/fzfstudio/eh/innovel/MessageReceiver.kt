@file:OptIn(ExperimentalWasmJsInterop::class)
@file:Suppress("UNCHECKED_CAST")

package com.fzfstudio.eh.innovel

import com.fzfstudio.eh.innovel.sdk.JsInteropUtils
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.js

/**
 * Message payload for display updates.
 */
data class NotifyMessage(
    val text1: String,
    val text2: String,
)

/**
 * Protocol-agnostic interface for receiving messages.
 * Swap implementations to change transport (WebSocket, OSC bridge, etc.).
 */
interface MessageReceiver {
    fun connect(onMessage: (NotifyMessage) -> Unit)
    fun disconnect()
}

/**
 * WebSocket-based message receiver.
 *
 * Connects to the given WebSocket URL and listens for JSON messages
 * in the format: {"text1": "...", "text2": "..."}
 *
 * Auto-reconnects on disconnect after a short delay.
 *
 * @param url WebSocket server URL (e.g. "ws://192.168.1.100:9000")
 */
class WebSocketReceiver(private val url: String) : MessageReceiver {
    private var ws: Any? = null
    private var shouldReconnect = true

    override fun connect(onMessage: (NotifyMessage) -> Unit) {
        shouldReconnect = true
        openConnection(onMessage)
    }

    override fun disconnect() {
        shouldReconnect = false
        try {
            val closeWs = js("(function(ws) { if (ws && ws.readyState <= 1) ws.close(); })") as (Any?) -> Unit
            closeWs(ws)
        } catch (_: Exception) {}
        ws = null
    }

    private fun openConnection(onMessage: (NotifyMessage) -> Unit) {
        try {
            val createWs = js("(function(url) { return new WebSocket(url); })") as (String) -> Any?
            val socket = createWs(url)
            ws = socket

            val setOnMessage = js("""(function(ws, callback) {
                ws.onmessage = function(event) { callback(event.data); };
            })""") as (Any?, (JsAny?) -> Unit) -> Unit

            val setOnClose = js("""(function(ws, callback) {
                ws.onclose = function() { callback(); };
            })""") as (Any?, () -> Unit) -> Unit

            val setOnError = js("""(function(ws, callback) {
                ws.onerror = function() { callback(); };
            })""") as (Any?, () -> Unit) -> Unit

            val setOnOpen = js("""(function(ws, callback) {
                ws.onopen = function() { callback(); };
            })""") as (Any?, () -> Unit) -> Unit

            setOnOpen(socket) {
                println("WebSocket connected to $url")
            }

            setOnMessage(socket) { data ->
                val text = JsInteropUtils.toStringOrNull(data) ?: return@setOnMessage
                val msg = parseMessage(text) ?: return@setOnMessage
                onMessage(msg)
            }

            setOnClose(socket) {
                println("WebSocket disconnected")
                scheduleReconnect(onMessage)
            }

            setOnError(socket) {
                println("WebSocket error")
            }
        } catch (e: Exception) {
            println("WebSocket connection failed: ${e.message}")
            scheduleReconnect(onMessage)
        }
    }

    private fun scheduleReconnect(onMessage: (NotifyMessage) -> Unit) {
        if (!shouldReconnect) return
        val setTimeout = js("(function(fn, ms) { setTimeout(fn, ms); })") as ((() -> Unit), Int) -> Unit
        setTimeout({
            if (shouldReconnect) {
                println("Reconnecting to $url...")
                openConnection(onMessage)
            }
        }, 3000)
    }

    private fun parseMessage(raw: String): NotifyMessage? {
        return try {
            val obj = JsInteropUtils.parseJson(raw)
            val text1 = JsInteropUtils.getStringProperty(obj, "text1")
            val text2 = JsInteropUtils.getStringProperty(obj, "text2")
            if (text1 != null && text2 != null) {
                NotifyMessage(text1, text2)
            } else {
                println("Message missing text1/text2 fields: $raw")
                null
            }
        } catch (e: Exception) {
            println("Failed to parse message: $raw")
            null
        }
    }
}
