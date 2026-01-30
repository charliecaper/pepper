@file:OptIn(ExperimentalWasmJsInterop::class)

package com.fzfstudio.eh.pepper

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fzfstudio.eh.pepper.sdk.*
import com.fzfstudio.eh.pepper.theme.PepperTheme
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
        var showTimer by remember { mutableStateOf(true) }
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
                                    val minutes = timerSeconds / 60
                                    val seconds = timerSeconds % 60
                                    updateGlassesText(msg.text1, msg.text2, "$minutes:${seconds.toString().padStart(2, '0')}")
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

private suspend fun createGlassesContainers(text1: String, text2: String) {
    val container = CreateStartUpPageContainer(
        containerTotalNum = 3,
        textObject = listOf(
            TextContainerProperty(
                containerID = 1,
                containerName = "line1",
                xPosition = 0,
                yPosition = 0,
                width = 440,
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
            TextContainerProperty(
                containerID = 3,
                containerName = "timer",
                xPosition = 450,
                yPosition = 0,
                width = 126,
                height = 44,
                content = "0:00",
                isEventCapture = 0,
            ),
        )
    )
    createStartUpPageContainer(container)
}

private fun updateGlassesText(text1: String, text2: String, timerText: String) {
    try {
        val container = RebuildPageContainer(
            containerTotalNum = 3,
            textObject = listOf(
                TextContainerProperty(
                    containerID = 1,
                    containerName = "line1",
                    xPosition = 0,
                    yPosition = 0,
                    width = 440,
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
                TextContainerProperty(
                    containerID = 3,
                    containerName = "timer",
                    xPosition = 450,
                    yPosition = 0,
                    width = 126,
                    height = 44,
                    content = timerText,
                    isEventCapture = 0,
                ),
            )
        )
        @OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
        GlobalScope.launch {
            rebuildPageContainer(container)
        }
    } catch (e: Exception) {
        println("Failed to update glasses: ${e.message}")
    }
}
