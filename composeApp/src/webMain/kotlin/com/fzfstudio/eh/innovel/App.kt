@file:OptIn(ExperimentalWasmJsInterop::class)

package com.fzfstudio.eh.innovel

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fzfstudio.eh.innovel.sdk.*
import com.fzfstudio.eh.innovel.theme.InNovelTheme
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.js.ExperimentalWasmJsInterop

private const val WS_URL = "ws://localhost:9000"

@Composable
fun App() {
    InNovelTheme {
        var text1 by remember { mutableStateOf("Hello Charlie2") }
        var text2 by remember { mutableStateOf("Hello World") }
        var bridgeReady by remember { mutableStateOf(false) }
        var showTimer by remember { mutableStateOf(true) }
        var timerSeconds by remember { mutableStateOf(0) }

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
            // Initialize glasses display
            try {
                ensureEvenAppBridge()
                createGlassesContainers(text1, text2)
                bridgeReady = true
            } catch (e: Exception) {
                println("Failed to initialize glasses: ${e.message}")
            }

            // Connect to message source
            val receiver: MessageReceiver = WebSocketReceiver(WS_URL)
            receiver.connect { msg ->
                text1 = msg.text1
                text2 = msg.text2
                if (bridgeReady) {
                    val minutes = timerSeconds / 60
                    val seconds = timerSeconds % 60
                    updateGlassesText(msg.text1, msg.text2, "$minutes:${seconds.toString().padStart(2, '0')}")
                }
            }
        }

        Box(
            modifier = Modifier.fillMaxSize().padding(16.dp)
        ) {
            Text(
                text = text1,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.align(Alignment.TopStart)
            )
            Text(
                text = text2,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.align(Alignment.BottomStart)
            )
            if (showTimer) {
                val minutes = timerSeconds / 60
                val seconds = timerSeconds % 60
                Text(
                    text = "$minutes:${seconds.toString().padStart(2, '0')}",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.align(Alignment.TopEnd)
                )
            }
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
