@file:OptIn(ExperimentalWasmJsInterop::class)

package com.caper.pepper

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.caper.pepper.sdk.*
import com.caper.pepper.theme.PepperTheme
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.js.ExperimentalWasmJsInterop

private const val WS_PORT = 9000

@Composable
fun App() {
    PepperTheme {
        var text1 by remember { mutableStateOf("Hello Charlie2") }
        var text2 by remember { mutableStateOf("Hello World") }
        var bridgeReady by remember { mutableStateOf(false) }
        var showTimer by remember { mutableStateOf(false) }
        var timerSeconds by remember { mutableStateOf(0) }
        var statusMessage by remember { mutableStateOf("Ready") }
        var wsConnected by remember { mutableStateOf(false) }
        val wsUrl = remember {
            val host = JsInteropUtils.getPageHostname() ?: "localhost"
            "ws://$host:$WS_PORT"
        }
        val receiver = remember { WebSocketReceiver(wsUrl) }

        fun setStatus(msg: String) {
            statusMessage = msg
        }

        LaunchedEffect(showTimer) {
            if (!showTimer) return@LaunchedEffect
            while (true) {
                delay(1000)
                timerSeconds++
                if (bridgeReady) {
                    val minutes = timerSeconds / 60
                    val seconds = timerSeconds % 60
                    try {
                        textContainerUpgrade(TextContainerUpgrade(
                            containerID = 3,
                            containerName = "timer",
                            content = "$minutes:${seconds.toString().padStart(2, '0')}",
                        ))
                    } catch (_: Exception) {}
                }
            }
        }

        LaunchedEffect(Unit) {
            try {
                setStatus("Initializing glasses...")
                ensureEvenAppBridge()
                createGlassesContainers(text1, text2)
                bridgeReady = true
                setStatus("Glasses ready")
            } catch (e: Exception) {
                setStatus("Glasses init failed: ${e.message}")
            }
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp)
        ) {
            // Current values
            Text(text = text1, style = MaterialTheme.typography.titleMedium)
            Text(text = text2, style = MaterialTheme.typography.titleMedium)
            if (showTimer) {
                val minutes = timerSeconds / 60
                val seconds = timerSeconds % 60
                Text(
                    text = "$minutes:${seconds.toString().padStart(2, '0')}",
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (!wsConnected) {
                        receiver.connect(
                            onMessage = { msg ->
                                text1 = msg.text1
                                text2 = msg.text2
                                if (bridgeReady) {
                                    val timer = if (showTimer) {
                                        val m = timerSeconds / 60
                                        val s = timerSeconds % 60
                                        "$m:${s.toString().padStart(2, '0')}"
                                    } else null
                                    updateGlassesText(msg.text1, msg.text2, timer)
                                }
                            },
                            onCommand = { cmd ->
                                when (cmd.command) {
                                    "resetTimer" -> {
                                        timerSeconds = 0
                                        setStatus("Timer reset")
                                    }
                                    "timerOn" -> {
                                        showTimer = true
                                        if (bridgeReady) {
                                            val m = timerSeconds / 60
                                            val s = timerSeconds % 60
                                            updateGlassesText(text1, text2, "$m:${s.toString().padStart(2, '0')}")
                                        }
                                        setStatus("Timer visible")
                                    }
                                    "timerOff" -> {
                                        showTimer = false
                                        if (bridgeReady) {
                                            updateGlassesText(text1, text2, null)
                                        }
                                        setStatus("Timer hidden")
                                    }
                                    else -> setStatus("Unknown command: ${cmd.command}")
                                }
                            },
                            onStatus = { msg ->
                                setStatus(msg)
                                wsConnected = msg.startsWith("Connected")
                            },
                        )
                        wsConnected = true
                    } else {
                        receiver.disconnect()
                        wsConnected = false
                        setStatus("Disconnected")
                    }
                },
            ) {
                Text(if (wsConnected) "Disconnect" else "Connect WS")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Status log area
            Text(
                text = statusMessage,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

private fun glassesTextContainers(text1: String, text2: String, timerText: String?): List<TextContainerProperty> {
    val containers = mutableListOf(
        TextContainerProperty(
            containerID = 1,
            containerName = "line1",
            xPosition = 0,
            yPosition = 0,
            width = if (timerText != null) 440 else 576,
            height = 144,
            content = text1,
            isEventCapture = 1,
        ),
        TextContainerProperty(
            containerID = 2,
            containerName = "line2",
            xPosition = 0,
            yPosition = 200,
            width = 576,
            height = 88,
            content = text2,
            isEventCapture = 0,
        ),
    )
    if (timerText != null) {
        containers.add(TextContainerProperty(
            containerID = 3,
            containerName = "timer",
            xPosition = 450,
            yPosition = 0,
            width = 126,
            height = 44,
            content = timerText,
            isEventCapture = 0,
        ))
    }
    return containers
}

private suspend fun createGlassesContainers(text1: String, text2: String) {
    val texts = glassesTextContainers(text1, text2, null)
    val container = CreateStartUpPageContainer(
        containerTotalNum = texts.size,
        textObject = texts,
    )
    createStartUpPageContainer(container)
}

private fun updateGlassesText(text1: String, text2: String, timerText: String?) {
    try {
        val texts = glassesTextContainers(text1, text2, timerText)
        val container = RebuildPageContainer(
            containerTotalNum = texts.size,
            textObject = texts,
        )
        @OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
        GlobalScope.launch {
            rebuildPageContainer(container)
        }
    } catch (e: Exception) {
        println("Failed to update glasses: ${e.message}")
    }
}
