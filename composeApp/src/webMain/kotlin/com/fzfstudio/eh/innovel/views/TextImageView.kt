package com.fzfstudio.eh.innovel.views

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.fzfstudio.eh.innovel.sdk.*
import kotlinx.coroutines.launch
import kotlin.js.js

/**
 * Text image view component.
 * Contains an adjustable canvas for drawing a 3D rectangle, plus test and exit buttons.
 */
@Composable
fun TextImageView() {
    val coroutineScope = rememberCoroutineScope()
    var containerId by remember { mutableStateOf<Int?>(null) }
    
    // Width/height state, default 90
    var width by remember { mutableStateOf(90) }
    var height by remember { mutableStateOf(90) }
    var widthText by remember { mutableStateOf("90") }
    var heightText by remember { mutableStateOf("90") }
    
    // Calculate display size (use the larger of width/height to ensure canvas is fully visible)
    val displaySize = maxOf(width, height).coerceAtLeast(50).coerceAtMost(200)
    
    Column(
        modifier = Modifier
            .width((displaySize + 100).dp)
            .fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Width/height input fields
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Width x Height", style = MaterialTheme.typography.labelMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Width input field
                Column(modifier = Modifier.weight(1f)) {
                    BasicTextField(
                        value = widthText,
                        onValueChange = { newValue ->
                            // Only allow digits
                            if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                                widthText = newValue
                                newValue.toIntOrNull()?.let { w ->
                                    if (w > 0 && w <= 500) {
                                        width = w
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusable()
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        decorationBox = { innerTextField ->
                            if (widthText.isEmpty()) {
                                Text(
                                    text = "90",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                            innerTextField()
                        }
                    )
                }
                Text("x", modifier = Modifier.padding(horizontal = 4.dp))
                // Height input field
                Column(modifier = Modifier.weight(1f)) {
                    BasicTextField(
                        value = heightText,
                        onValueChange = { newValue ->
                            // Only allow digits
                            if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                                heightText = newValue
                                newValue.toIntOrNull()?.let { h ->
                                    if (h > 0 && h <= 500) {
                                        height = h
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusable()
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        decorationBox = { innerTextField ->
                            if (heightText.isEmpty()) {
                                Text(
                                    text = "90",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                            innerTextField()
                        }
                    )
                }
            }
        }
        
        // Canvas (display only, uses calculated display size)
        Canvas(
            modifier = Modifier
                .size(displaySize.dp)
        ) {
            draw3DRectangleWithSize(width, height)
        }
        
        // Test button
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                coroutineScope.launch {
                    handleTestButtonClick(width, height) { id ->
                        containerId = id
                    }
                }
            }
        ) {
            Text("Test Upload (${width}x${height})")
        }
        
        // Exit button
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                coroutineScope.launch {
                    handleExitButtonClick()
                }
            }
        ) {
            Text("Exit EvenHub")
        }
    }
}

/**
 * Draw a 3D rectangle (using actual width/height).
 */
private fun DrawScope.draw3DRectangleWithSize(actualWidth: Int, actualHeight: Int) {
    val canvasWidth = size.width
    val canvasHeight = size.height
    
    // Calculate scale ratio to fit content within canvas size
    val scaleX = canvasWidth / actualWidth.toFloat()
    val scaleY = canvasHeight / actualHeight.toFloat()
    val scale = minOf(scaleX, scaleY)
    
    // Calculate centering offset
    val offsetX = (canvasWidth - actualWidth * scale) / 2f
    val offsetY = (canvasHeight - actualHeight * scale) / 2f
    
    val padding = 10f * scale
    val rectWidth = actualWidth * scale - padding * 2
    val rectHeight = actualHeight * scale - padding * 2
    val rectX = offsetX + padding
    val rectY = offsetY + padding
    
    // Main rectangle (light color)
    drawRect(
        color = Color(0xFF4A90E2),
        topLeft = Offset(rectX, rectY),
        size = Size(rectWidth, rectHeight)
    )
    
    // Draw 3D effect - top and left (light faces)
    val depth = 8f * scale
    
    // Top light face
    drawRect(
        color = Color(0xFF6BA3E8),
        topLeft = Offset(rectX, rectY),
        size = Size(rectWidth, depth)
    )

    // Left light face
    drawRect(
        color = Color(0xFF6BA3E8),
        topLeft = Offset(rectX, rectY),
        size = Size(depth, rectHeight)
    )

    // Bottom and right (dark faces)
    // Bottom dark face
    drawRect(
        color = Color(0xFF2E5C8A),
        topLeft = Offset(rectX, rectY + rectHeight - depth),
        size = Size(rectWidth, depth)
    )

    // Right dark face
    drawRect(
        color = Color(0xFF2E5C8A),
        topLeft = Offset(rectX + rectWidth - depth, rectY),
        size = Size(depth, rectHeight)
    )
}

/**
 * Handle test button click event.
 * @param width Image width (integer)
 * @param height Image height (integer)
 */
private suspend fun handleTestButtonClick(
    width: Int,
    height: Int,
    onContainerCreated: (Int?) -> Unit
) {
    try {
        // 1. Create HTML Canvas and draw content (using input width/height, integer dimensions)
        @Suppress("UNCHECKED_CAST")
        val createCanvas = js("(function(w, h) { var c = document.createElement('canvas'); c.width = w; c.height = h; return c; })") as (Int, Int) -> Any?
        val canvas = createCanvas(width, height) ?: throw Exception("Failed to create canvas")
        
        @Suppress("UNCHECKED_CAST")
        val getContext = js("(function(canvas) { return canvas.getContext('2d'); })") as (Any?) -> Any?
        val ctx = getContext(canvas) ?: throw Exception("Failed to get 2D context")
        
        // Draw 3D rectangle (using input width/height)
        draw3DRectangleOnCanvas(ctx, width, height)
        
        // 2. Convert canvas to image data (base64)
        @Suppress("UNCHECKED_CAST")
        val toDataURL = js("(function(canvas) { return canvas.toDataURL('image/png'); })") as (Any?) -> String
        val imageDataUrl = toDataURL(canvas)
        
        // 3. Convert base64 to ArrayBuffer
        val base64Data = imageDataUrl.substringAfter(",")
        @Suppress("UNCHECKED_CAST")
        val atob = js("(function(str) { return window.atob(str); })") as (String) -> String
        val binaryString = atob(base64Data)
        val length = binaryString.length
        
        @Suppress("UNCHECKED_CAST")
        val createUint8Array = js("(function(len) { return new Uint8Array(len); })") as (Int) -> Any?
        val bytes = createUint8Array(length) ?: throw Exception("Failed to create Uint8Array")
        
        @Suppress("UNCHECKED_CAST")
        val setByte = js("(function(arr, index, value) { arr[index] = value; })") as (Any?, Int, Byte) -> Unit
        for (i in 0 until length) {
            setByte(bytes, i, binaryString[i].code.toByte())
        }
        
        @Suppress("UNCHECKED_CAST")
        val getBuffer = js("(function(arr) { return arr.buffer; })") as (Any?) -> Any?
        val arrayBuffer = getBuffer(bytes)
        
        // 4. Create image container (using input width/height, integer dimensions)
        val imageContainer = ImageContainerProperty(
            containerID = 100, // Use a unique ID
            containerName = "testImage",
            xPosition = 0,
            yPosition = 0,
            width = width,  // Use input width
            height = height  // Use input height
        )
        
        val container = CreateStartUpPageContainer(
            containerTotalNum = 1,
            imageObject = listOf(imageContainer)
        )
        
        val createdContainerId = createStartUpPageContainer(container)
        onContainerCreated(createdContainerId)
        
        if (createdContainerId != null) {
            // 5. Upload image data
            val imageUpdate = ImageRawDataUpdate(
                containerID = 100,
                containerName = "testImage",
                imageData = arrayBuffer // Can be ArrayBuffer, converted to number[] during toJson
            )
            
            val success = updateImageRawData(imageUpdate)
            if (success) {
                println("Image uploaded successfully")
            } else {
                println("Failed to upload image")
            }
        }
        
    } catch (e: Exception) {
        println("Error in handleTestButtonClick: ${e.message}")
        e.printStackTrace()
    }
}

/**
 * Draw a 3D rectangle on an HTML Canvas.
 * @param ctx Canvas 2D context
 * @param width Canvas width (integer)
 * @param height Canvas height (integer)
 */
private fun draw3DRectangleOnCanvas(
    ctx: Any?,
    width: Int,
    height: Int
) {
    if (ctx == null) return
    
    // Use integer calculations to avoid decimals
    val padding = 10
    val rectWidth = width - padding * 2
    val rectHeight = height - padding * 2
    val rectX = padding
    val rectY = padding
    val depth = 8
    
    @Suppress("UNCHECKED_CAST")
    val setFillStyle = js("(function(ctx, color) { ctx.fillStyle = color; })") as (Any?, String) -> Unit
    @Suppress("UNCHECKED_CAST")
    val fillRect = js("(function(ctx, x, y, w, h) { ctx.fillRect(Math.round(x), Math.round(y), Math.round(w), Math.round(h)); })") as (Any?, Double, Double, Double, Double) -> Unit
    
    // Draw main rectangle (front face)
    setFillStyle(ctx, "#4A90E2")
    fillRect(ctx, rectX.toDouble(), rectY.toDouble(), rectWidth.toDouble(), rectHeight.toDouble())
    
    // Top light face
    setFillStyle(ctx, "#6BA3E8")
    fillRect(ctx, rectX.toDouble(), rectY.toDouble(), rectWidth.toDouble(), depth.toDouble())

    // Left light face
    setFillStyle(ctx, "#6BA3E8")
    fillRect(ctx, rectX.toDouble(), rectY.toDouble(), depth.toDouble(), rectHeight.toDouble())

    // Bottom dark face
    setFillStyle(ctx, "#2E5C8A")
    fillRect(ctx, rectX.toDouble(), (rectY + rectHeight - depth).toDouble(), rectWidth.toDouble(), depth.toDouble())

    // Right dark face
    setFillStyle(ctx, "#2E5C8A")
    fillRect(ctx, (rectX + rectWidth - depth).toDouble(), rectY.toDouble(), depth.toDouble(), rectHeight.toDouble())
}

/**
 * Handle exit button click event.
 */
private suspend fun handleExitButtonClick() {
    try {
        val success = shutDownPageContainer(ShutDownContainer(exitMode = 0))
        if (success) {
            println("EvenHub exited successfully")
        } else {
            println("Failed to exit EvenHub")
        }
    } catch (e: Exception) {
        println("Error in handleExitButtonClick: ${e.message}")
        e.printStackTrace()
    }
}
