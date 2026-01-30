@file:OptIn(ExperimentalWasmJsInterop::class)

package com.fzfstudio.eh.innovel

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fzfstudio.eh.innovel.sdk.*
import com.fzfstudio.eh.innovel.theme.InNovelTheme
import kotlin.js.ExperimentalWasmJsInterop

@Composable
fun App() {
    InNovelTheme {
        LaunchedEffect(Unit) {
            initGlassesDisplay()
        }

        Box(
            modifier = Modifier.fillMaxSize().padding(16.dp)
        ) {
            Text(
                text = "Hello Charlie",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.align(Alignment.TopStart)
            )
            Text(
                text = "Hello World",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.align(Alignment.BottomStart)
            )
        }
    }
}

private suspend fun initGlassesDisplay() {
    try {
        ensureEvenAppBridge()

        val container = CreateStartUpPageContainer(
            containerTotalNum = 2,
            textObject = listOf(
                TextContainerProperty(
                    containerID = 1,
                    containerName = "helloCharlie",
                    xPosition = 0,
                    yPosition = 0,
                    width = 576,
                    height = 144,
                    content = "Hello Charlie",
                    isEventCapture = 0,
                ),
                TextContainerProperty(
                    containerID = 2,
                    containerName = "helloWorld",
                    xPosition = 0,
                    yPosition = 200,
                    width = 576,
                    height = 88,
                    content = "Hello World",
                    isEventCapture = 0,
                ),
            )
        )
        createStartUpPageContainer(container)
    } catch (e: Exception) {
        println("Failed to initialize glasses display: ${e.message}")
    }
}
