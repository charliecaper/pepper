@file:OptIn(ExperimentalWasmJsInterop::class)

package com.caper.pepper

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.caper.pepper.sdk.*
import com.caper.pepper.theme.EvenColors
import com.caper.pepper.theme.PepperTheme
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.js.ExperimentalWasmJsInterop

private const val WS_PORT = 9000
private const val ALERT_DURATION_MS = 20000L
private const val ALERT_MAX_LINE_LENGTH = 30

/**
 * Parse time string in format "M.SS" or "MM.SS" (e.g., "5.30" = 5:30, "59.59" = 59:59)
 * Returns total seconds, or null if invalid.
 */
private fun parseTimeArg(time: String?): Int? {
    if (time == null) return null
    val parts = time.split(".")
    if (parts.size != 2) return null
    val minutes = parts[0].toIntOrNull() ?: return null
    val seconds = parts[1].toIntOrNull() ?: return null
    if (seconds < 0 || seconds > 59) return null
    if (minutes < 0) return null
    return minutes * 60 + seconds
}

/**
 * Format alert text, splitting into up to 3 lines if longer than ALERT_MAX_LINE_LENGTH.
 * Returns a list of 1-3 lines.
 */
private fun formatAlertText(text: String): List<String> {
    if (text.length <= ALERT_MAX_LINE_LENGTH) {
        return listOf(text)
    }

    val lines = mutableListOf<String>()
    var remaining = text

    while (remaining.isNotEmpty() && lines.size < 3) {
        if (remaining.length <= ALERT_MAX_LINE_LENGTH) {
            lines.add(remaining)
            break
        }

        // Find a good split point near the max length
        var splitIndex = remaining.lastIndexOf(' ', ALERT_MAX_LINE_LENGTH)
        if (splitIndex <= 0) {
            // No space found, hard split at max length
            splitIndex = ALERT_MAX_LINE_LENGTH
        }

        lines.add(remaining.substring(0, splitIndex).trim())
        remaining = remaining.substring(splitIndex).trim()
    }

    // If there's still text remaining after 3 lines, append to last line
    if (remaining.isNotEmpty() && lines.size == 3) {
        lines[2] = lines[2] + " " + remaining
    }

    return lines
}

@Composable
fun App() {
    PepperTheme {
        var text1 by remember { mutableStateOf("Current Cue") }
        var text2 by remember { mutableStateOf("Next Cue") }
        var bridgeReady by remember { mutableStateOf(false) }
        var showTimer by remember { mutableStateOf(false) }
        var timerSeconds by remember { mutableStateOf(0) }  // System timer, always runs
        var pacingTargetSeconds by remember { mutableStateOf<Int?>(null) }  // Target time for pacing mode
        var alertText by remember { mutableStateOf<String?>(null) }  // Current alert text
        var alertTimestamp by remember { mutableStateOf(0L) }  // When alert was shown
        var statusMessage by remember { mutableStateOf("Ready") }
        var showInstructions by remember { mutableStateOf(false) }
        var wsConnected by remember { mutableStateOf(false) }
        val wsHost = remember { JsInteropUtils.getPageHostname() ?: "localhost" }
        val wsUrl = remember { "ws://$wsHost:$WS_PORT" }
        val receiver = remember { WebSocketReceiver(wsUrl) }

        fun setStatus(msg: String) {
            statusMessage = msg
        }

        // Helper to format timer display based on mode
        fun formatTimerDisplay(): String {
            val target = pacingTargetSeconds
            return if (target != null) {
                // Pacing mode: show countdown (target - elapsed)
                val remaining = target - timerSeconds
                val absRemaining = kotlin.math.abs(remaining)
                val m = absRemaining / 60
                val s = absRemaining % 60
                val timeStr = "$m:${s.toString().padStart(2, '0')}"
                if (remaining < 0) "-$timeStr" else timeStr
            } else {
                // Normal mode: show elapsed time
                val m = timerSeconds / 60
                val s = timerSeconds % 60
                "$m:${s.toString().padStart(2, '0')}"
            }
        }

        // Connect to WebSocket
        fun connectWebSocket() {
            if (wsConnected) return
            receiver.connect(
                onMessage = { msg ->
                    text1 = msg.text1
                    text2 = msg.text2
                    if (bridgeReady) {
                        val timer = if (showTimer) formatTimerDisplay() else null
                        updateGlassesDisplay(msg.text1, msg.text2, timer, alertText)
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
                                updateGlassesDisplay(text1, text2, formatTimerDisplay(), alertText)
                            }
                            setStatus("Timer visible")
                        }
                        "timerOff" -> {
                            showTimer = false
                            if (bridgeReady) {
                                updateGlassesDisplay(text1, text2, null, alertText)
                            }
                            setStatus("Timer hidden")
                        }
                        "timerPacing" -> {
                            val targetSecs = parseTimeArg(cmd.time)
                            if (targetSecs != null) {
                                pacingTargetSeconds = targetSecs
                                showTimer = true
                                if (bridgeReady) {
                                    updateGlassesDisplay(text1, text2, formatTimerDisplay(), alertText)
                                }
                                setStatus("Pacing: ${cmd.time}")
                            } else {
                                setStatus("Invalid time format: ${cmd.time}")
                            }
                        }
                        "alert" -> {
                            val newAlertText = cmd.text
                            if (newAlertText != null && newAlertText.isNotEmpty()) {
                                alertText = newAlertText
                                alertTimestamp = currentTimeMs().toLong()
                                if (bridgeReady) {
                                    updateGlassesDisplay(text1, text2, if (showTimer) formatTimerDisplay() else null, newAlertText)
                                }
                                setStatus("Alert: $newAlertText")
                            } else {
                                alertText = null
                                alertTimestamp = 0L
                                if (bridgeReady) {
                                    updateGlassesDisplay(text1, text2, if (showTimer) formatTimerDisplay() else null, null)
                                }
                                setStatus("Alert cleared")
                            }
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
        }

        // System timer always runs in background
        LaunchedEffect(Unit) {
            while (true) {
                delay(1000)
                timerSeconds++
                // Update glasses display if timer is visible
                if (showTimer && bridgeReady) {
                    try {
                        textContainerUpgrade(TextContainerUpgrade(
                            containerID = 3,
                            containerName = "timer",
                            content = formatTimerDisplay(),
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
                // Auto-connect to WebSocket on startup
                connectWebSocket()
            } catch (e: Exception) {
                setStatus("Glasses init failed: ${e.message}")
            }
        }

        // Auto-dismiss alerts after 15 seconds
        LaunchedEffect(alertTimestamp) {
            if (alertText != null && alertTimestamp > 0) {
                delay(ALERT_DURATION_MS)
                // Only dismiss if this is still the same alert
                if (alertTimestamp > 0 && currentTimeMs().toLong() - alertTimestamp >= ALERT_DURATION_MS) {
                    alertText = null
                    alertTimestamp = 0L
                    if (bridgeReady) {
                        updateGlassesDisplay(text1, text2, if (showTimer) formatTimerDisplay() else null, null)
                    }
                    setStatus("Alert dismissed")
                }
            }
        }

        // Main background
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(EvenColors.BackgroundMain)
                .padding(horizontal = 12.dp) // Screen side margins per design spec
        ) {
            Spacer(modifier = Modifier.height(24.dp)) // Section spacing

            // App title - Large Title (20 Regular)
            Text(
                text = "Pepper Prompter",
                style = MaterialTheme.typography.headlineMedium,
            )

            Spacer(modifier = Modifier.height(24.dp)) // Different sections spacing

            // Current text card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp)) // Default radius per design spec
                    .background(EvenColors.BackgroundSecondary)
                    .padding(16.dp) // Card internal margins
            ) {
                // Normal Title for label
                Text(
                    text = "Current Text",
                    style = MaterialTheme.typography.titleSmall,
                )

                Spacer(modifier = Modifier.height(12.dp)) // Related items spacing

                // Medium Body for content
                Text(
                    text = text1,
                    style = MaterialTheme.typography.bodyLarge,
                )

                Spacer(modifier = Modifier.height(6.dp)) // Same-element spacing

                Text(
                    text = text2,
                    style = MaterialTheme.typography.bodyLarge,
                )

                if (showTimer) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = formatTimerDisplay(),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp)) // Different sections spacing

            // Primary action button - dark background, white text
            Button(
                onClick = {
                    if (!wsConnected) {
                        connectWebSocket()
                    } else {
                        receiver.disconnect()
                        wsConnected = false
                        setStatus("Disconnected")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (wsConnected) EvenColors.BackgroundElevated else EvenColors.HighlightAction,
                    contentColor = if (wsConnected) EvenColors.TextPrimary else EvenColors.TextHighlight,
                ),
            ) {
                Text(
                    text = if (wsConnected) "Disconnect" else "Connect WebSocket $wsHost:$WS_PORT",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = if (wsConnected) EvenColors.TextPrimary else EvenColors.TextHighlight
                    ),
                )
            }

            Spacer(modifier = Modifier.height(12.dp)) // Related items spacing

            // Timer control buttons - Normal buttons (light background)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = {
                        showTimer = !showTimer
                        if (bridgeReady) {
                            val timer = if (showTimer) formatTimerDisplay() else null
                            updateGlassesDisplay(text1, text2, timer, alertText)
                        }
                        setStatus(if (showTimer) "Timer visible" else "Timer hidden")
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (showTimer) EvenColors.AccentOngoing else EvenColors.ButtonPrimary,
                        contentColor = EvenColors.TextPrimary,
                    ),
                ) {
                    Text(
                        text = if (showTimer) "Hide Timer" else "Show Timer",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }

                Button(
                    onClick = {
                        timerSeconds = 0
                        setStatus("Timer reset")
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EvenColors.ButtonPrimary,
                        contentColor = EvenColors.TextPrimary,
                    ),
                ) {
                    Text(
                        text = "Reset Timer",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp)) // Related items spacing

            // Status - Subtitle style, secondary text color
            Text(
                text = statusMessage,
                style = MaterialTheme.typography.titleSmall,
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Instructions button
            Button(
                onClick = { showInstructions = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EvenColors.ButtonPrimary,
                    contentColor = EvenColors.TextPrimary,
                ),
            ) {
                Text(
                    text = "Show Commands",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }

        // Modal overlay for instructions
        if (showInstructions) {
            CommandsModal(onDismiss = { showInstructions = false })
        }
    }
}

@Composable
private fun CommandsModal(onDismiss: () -> Unit) {
    // Modal overlay (50% black per design guide)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(EvenColors.TextPrimary.copy(alpha = 0.5f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        // Modal content - centered, scrollable
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(6.dp))
                .background(EvenColors.BackgroundSecondary)
                .clickable(enabled = false, onClick = {}) // Prevent clicks from dismissing
                .padding(16.dp)
        ) {
            Text(
                text = "WebSocket Commands",
                style = MaterialTheme.typography.titleLarge,
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Scrollable command list
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
            ) {
                CommandRow("{\"text1\": \"...\", \"text2\": \"...\"}", "Update cue text")
                CommandRow("{\"command\": \"timerOn\"}", "Show timer")
                CommandRow("{\"command\": \"timerOff\"}", "Hide timer")
                CommandRow("{\"command\": \"resetTimer\"}", "Reset to 0:00")
                CommandRow("{\"command\": \"timerPacing\", \"time\": \"5.30\"}", "Countdown to target")
                CommandRow("{\"command\": \"alert\", \"text\": \"...\"}", "Show alert (20s)")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Close button - primary action per design guide
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EvenColors.HighlightAction,
                    contentColor = EvenColors.TextHighlight,
                ),
            ) {
                Text(
                    text = "Close",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = EvenColors.TextHighlight
                    ),
                )
            }
        }
    }
}

@Composable
private fun CommandRow(command: String, description: String) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(
            text = command,
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = description,
            style = MaterialTheme.typography.titleSmall,
        )
    }
}

private fun glassesTextContainers(text1: String, text2: String, timerText: String?, alertText: String? = null): List<TextContainerProperty> {
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
    if (alertText != null) {
        val lines = formatAlertText(alertText)
        val lineCount = lines.size
        // Alert positioned right-aligned, adjust y position based on number of lines
        val yPos = when (lineCount) {
            1 -> 140
            2 -> 120
            else -> 100  // 3 lines
        }
        val height = when (lineCount) {
            1 -> 44
            2 -> 88
            else -> 132  // 3 lines
        }
        containers.add(TextContainerProperty(
            containerID = 4,
            containerName = "alert",
            xPosition = 300,  // Right side
            yPosition = yPos,
            width = 276,
            height = height,
            content = lines.joinToString("\n"),
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
    updateGlassesDisplay(text1, text2, timerText, null)
}

private fun updateGlassesDisplay(text1: String, text2: String, timerText: String?, alertText: String?) {
    try {
        val texts = glassesTextContainers(text1, text2, timerText, alertText)
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
