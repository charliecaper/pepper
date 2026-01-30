@file:OptIn(ExperimentalWasmJsInterop::class)
@file:Suppress("UNCHECKED_CAST")

package com.fzfstudio.eh.pepper

import com.fzfstudio.eh.pepper.sdk.JsInteropUtils
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.js

// ==================== Top-level JS interop helpers for WebSocketReceiver ====================

private val jsCloseWs: JsAny = js("(function(ws) { if (ws && ws.readyState <= 1) ws.close(); })")

private val jsCreateWs: JsAny = js("(function(url) { return new WebSocket(url); })")

private val jsSetOnMessage: JsAny = js("(function(ws, callback) { ws.onmessage = function(event) { callback(event.data); }; })")

private val jsSetOnClose: JsAny = js("(function(ws, callback) { ws.onclose = function() { callback(); }; })")

private val jsSetOnError: JsAny = js("(function(ws, callback) { ws.onerror = function() { callback(); }; })")

private val jsSetOnOpen: JsAny = js("(function(ws, callback) { ws.onopen = function() { callback(); }; })")

private val jsSetTimeout: JsAny = js("(function(fn, ms) { setTimeout(fn, ms); })")

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
    fun connect(onMessage: (NotifyMessage) -> Unit, onStatus: (String) -> Unit = {})
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
    private var statusCallback: (String) -> Unit = {}

    override fun connect(onMessage: (NotifyMessage) -> Unit, onStatus: (String) -> Unit) {
        statusCallback = onStatus
        shouldReconnect = true
        onStatus("Connecting to $url...")
        openConnection(onMessage)
    }

    override fun disconnect() {
        shouldReconnect = false
        try {
            @Suppress("UNCHECKED_CAST")
            val closeWs = jsCloseWs as (Any?) -> Unit
            closeWs(ws)
        } catch (_: Exception) {}
        ws = null
    }

    private fun openConnection(onMessage: (NotifyMessage) -> Unit) {
        try {
            @Suppress("UNCHECKED_CAST")
            val createWs = jsCreateWs as (String) -> Any?
            val socket = createWs(url)
            ws = socket

            @Suppress("UNCHECKED_CAST")
            val setOnMessage = jsSetOnMessage as (Any?, (JsAny?) -> Unit) -> Unit

            @Suppress("UNCHECKED_CAST")
            val setOnClose = jsSetOnClose as (Any?, () -> Unit) -> Unit

            @Suppress("UNCHECKED_CAST")
            val setOnError = jsSetOnError as (Any?, () -> Unit) -> Unit

            @Suppress("UNCHECKED_CAST")
            val setOnOpen = jsSetOnOpen as (Any?, () -> Unit) -> Unit

            setOnOpen(socket) {
                statusCallback("Connected to $url")
            }

            setOnMessage(socket) { data ->
                val text = JsInteropUtils.toStringOrNull(data) ?: return@setOnMessage
                val msg = parseMessage(text) ?: return@setOnMessage
                statusCallback("Received: ${text.take(80)}")
                onMessage(msg)
            }

            setOnClose(socket) {
                statusCallback("Disconnected")
                scheduleReconnect(onMessage)
            }

            setOnError(socket) {
                statusCallback("Connection error")
            }
        } catch (e: Exception) {
            statusCallback("Connection failed: ${e.message}")
            scheduleReconnect(onMessage)
        }
    }

    private fun scheduleReconnect(onMessage: (NotifyMessage) -> Unit) {
        if (!shouldReconnect) return
        @Suppress("UNCHECKED_CAST")
        val setTimeout = jsSetTimeout as ((() -> Unit), Int) -> Unit
        statusCallback("Reconnecting in 3s...")
        setTimeout({
            if (shouldReconnect) {
                statusCallback("Reconnecting to $url...")
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
