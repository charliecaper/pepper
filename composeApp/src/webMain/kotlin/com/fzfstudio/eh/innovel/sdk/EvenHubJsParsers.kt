@file:OptIn(ExperimentalWasmJsInterop::class)
@file:Suppress("UNCHECKED_CAST", "CAST_NEVER_SUCCEEDS")

package com.fzfstudio.eh.innovel.sdk

import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.js

/**
 * Parse JS SDK returns into Kotlin models.
 *
 * Uses JsInterop to avoid duplicate JS/Wasm implementations.
 */
internal fun userInfoFromJs(raw: JsAny?): UserInfo? {
    if (raw == null) return null
    return UserInfo(
        uid = JsInteropUtils.getIntProperty(raw, "uid"),
        name = JsInteropUtils.getStringProperty(raw, "name") ?: "",
        avatar = JsInteropUtils.getStringProperty(raw, "avatar") ?: "",
        country = JsInteropUtils.getStringProperty(raw, "country") ?: "",
    )
}

internal fun deviceInfoFromJs(raw: JsAny?): DeviceInfo? {
    if (raw == null) return null
    val sn = JsInteropUtils.getStringProperty(raw, "sn") ?: ""
    val status = deviceStatusFromJs(JsInteropUtils.getProperty(raw, "status"))
    return DeviceInfo(
        model = DeviceModel.fromString(JsInteropUtils.getStringProperty(raw, "model")),
        sn = sn,
        status = status,
    )
}

internal fun deviceStatusFromJs(raw: JsAny?): DeviceStatus? {
    if (raw == null) return null
    val sn = JsInteropUtils.getStringProperty(raw, "sn") ?: ""
    return DeviceStatus(
        sn = sn,
        connectType = DeviceConnectType.fromString(JsInteropUtils.getStringProperty(raw, "connectType")),
        isWearing = JsInteropUtils.getBooleanProperty(raw, "isWearing"),
        batteryLevel = JsInteropUtils.getIntProperty(raw, "batteryLevel"),
        isCharging = JsInteropUtils.getBooleanProperty(raw, "isCharging"),
        isInCase = JsInteropUtils.getBooleanProperty(raw, "isInCase"),
    )
}

internal fun evenHubEventFromJs(raw: JsAny?): EvenHubEvent? {
    if (raw == null) return null
    
    // New structure: { listEvent?, textEvent?, sysEvent?, jsonData? }
    // Check each event property directly
    val listEventRaw = JsInteropUtils.getProperty(raw, "listEvent")
    val textEventRaw = JsInteropUtils.getProperty(raw, "textEvent")
    val sysEventRaw = JsInteropUtils.getProperty(raw, "sysEvent")
    val jsonDataRaw = JsInteropUtils.getProperty(raw, "jsonData")
    
    // Parse each event object
    val listEvent = if (listEventRaw != null) listItemEventFromJs(listEventRaw) else null
    val textEvent = if (textEventRaw != null) textItemEventFromJs(textEventRaw) else null
    val sysEvent = if (sysEventRaw != null) sysItemEventFromJs(sysEventRaw) else null
    
    // Convert jsonData to string (if present)
    val jsonData = if (jsonDataRaw != null) {
        JsInteropUtils.stringify(jsonDataRaw)
    } else {
        // If no jsonData but at least one event exists, serialize the entire object as jsonData
        if (listEvent != null || textEvent != null || sysEvent != null) {
            JsInteropUtils.stringify(raw)
        } else {
            null
        }
    }
    
    return EvenHubEvent(
        listEvent = listEvent,
        textEvent = textEvent,
        sysEvent = sysEvent,
        jsonData = jsonData,
    )
}

/**
 * Parse OsEventTypeList enum value from JS.
 */
internal fun osEventTypeListFromJs(raw: JsAny?): OsEventTypeList? {
    if (raw == null) return null
    
    // If raw is a number (OsEventTypeList enum values are integers)
    val directInt = JsInteropUtils.toIntOrNull(raw)
    if (directInt != null && directInt >= 0 && directInt <= 6) {
        return OsEventTypeList.fromInt(directInt)
    }
    
    // Try to get from the object's value property
    val intValue = JsInteropUtils.getIntProperty(raw, "value")
        ?: JsInteropUtils.toIntOrNull(raw)
    if (intValue != null && intValue >= 0 && intValue <= 6) {
        return OsEventTypeList.fromInt(intValue)
    }
    
    // Try parsing as a string
    val stringValue = JsInteropUtils.toStringOrNull(raw)
    if (stringValue != null) {
        return OsEventTypeList.fromString(stringValue)
    }
    
    return null
}

/**
 * Parse ListItemEvent from JS.
 * Compatible with multiple field name formats: camelCase and protoName (e.g. Container_ID)
 */
internal fun listItemEventFromJs(raw: JsAny?): ListItemEvent? {
    if (raw == null) return null
    
    // Try multiple field name formats
    val containerID = JsInteropUtils.getIntProperty(raw, "containerID")
        ?: JsInteropUtils.getIntProperty(raw, "ContainerID")
        ?: JsInteropUtils.getIntProperty(raw, "Container_ID")
    
    val containerName = JsInteropUtils.getStringProperty(raw, "containerName")
        ?: JsInteropUtils.getStringProperty(raw, "ContainerName")
        ?: JsInteropUtils.getStringProperty(raw, "Container_Name")
    
    val currentSelectItemName = JsInteropUtils.getStringProperty(raw, "currentSelectItemName")
        ?: JsInteropUtils.getStringProperty(raw, "CurrentSelectItemName")
        ?: JsInteropUtils.getStringProperty(raw, "CurrentSelect_ItemName")
    
    val currentSelectItemIndex = JsInteropUtils.getIntProperty(raw, "currentSelectItemIndex")
        ?: JsInteropUtils.getIntProperty(raw, "CurrentSelectItemIndex")
        ?: JsInteropUtils.getIntProperty(raw, "CurrentSelect_ItemIndex")
    
    val eventTypeRaw = JsInteropUtils.getProperty(raw, "eventType")
        ?: JsInteropUtils.getProperty(raw, "EventType")
        ?: JsInteropUtils.getProperty(raw, "Event_Type")
    
    val eventType = osEventTypeListFromJs(eventTypeRaw)
    
    return ListItemEvent(
        containerID = containerID,
        containerName = containerName,
        currentSelectItemName = currentSelectItemName,
        currentSelectItemIndex = currentSelectItemIndex,
        eventType = eventType,
    )
}

/**
 * Parse TextItemEvent from JS.
 * Compatible with multiple field name formats: camelCase and protoName
 */
internal fun textItemEventFromJs(raw: JsAny?): TextItemEvent? {
    if (raw == null) return null
    
    val containerID = JsInteropUtils.getIntProperty(raw, "containerID")
        ?: JsInteropUtils.getIntProperty(raw, "ContainerID")
        ?: JsInteropUtils.getIntProperty(raw, "Container_ID")
    
    val containerName = JsInteropUtils.getStringProperty(raw, "containerName")
        ?: JsInteropUtils.getStringProperty(raw, "ContainerName")
        ?: JsInteropUtils.getStringProperty(raw, "Container_Name")
    
    val eventTypeRaw = JsInteropUtils.getProperty(raw, "eventType")
        ?: JsInteropUtils.getProperty(raw, "EventType")
        ?: JsInteropUtils.getProperty(raw, "Event_Type")
    
    val eventType = osEventTypeListFromJs(eventTypeRaw)
    
    return TextItemEvent(
        containerID = containerID,
        containerName = containerName,
        eventType = eventType,
    )
}

/**
 * Parse SysItemEvent from JS.
 * Compatible with multiple field name formats: camelCase and protoName
 */
internal fun sysItemEventFromJs(raw: JsAny?): SysItemEvent? {
    if (raw == null) return null
    
    val eventTypeRaw = JsInteropUtils.getProperty(raw, "eventType")
        ?: JsInteropUtils.getProperty(raw, "EventType")
        ?: JsInteropUtils.getProperty(raw, "Event_Type")
    
    val eventType = osEventTypeListFromJs(eventTypeRaw)
    
    return SysItemEvent(
        eventType = eventType,
    )
}

internal fun CreateStartUpPageContainer.toJsonString(): String =
    JsInteropUtils.buildJsonObject(
        "containerTotalNum" to containerTotalNum,
        "listObject" to listObject,
        "textObject" to textObject,
        "imageObject" to imageObject,
    )

internal fun RebuildPageContainer.toJsonString(): String =
    JsInteropUtils.buildJsonObject(
        "containerTotalNum" to containerTotalNum,
        "listObject" to listObject,
        "textObject" to textObject,
        "imageObject" to imageObject,
    )

internal fun ImageRawDataUpdate.toJsonString(): String {
    // Normalize imageData: convert Uint8Array/ArrayBuffer to number[]
    val normalizedImageData = normalizeImageData(imageData)
    return JsInteropUtils.buildJsonObject(
        "containerID" to containerID,
        "containerName" to containerName,
        "imageData" to normalizedImageData,
    )
}

/**
 * Normalize image data: convert Uint8Array/ArrayBuffer to number[].
 * Corresponds to TypeScript's `ImageRawDataUpdate.normalizeImageData`
 */
private fun normalizeImageData(raw: Any?): Any? {
    if (raw == null) return null
    if (raw is String) return raw
    
    // Use js() to create wrappers for accessing properties
    @Suppress("UNCHECKED_CAST")
    val isArray = js("(function(obj) { return Array.isArray(obj); })") as (Any?) -> Boolean
    @Suppress("UNCHECKED_CAST")
    val getLength = js("(function(obj) { return obj != null && typeof obj.length === 'number' ? obj.length : null; })") as (Any?) -> Int?
    @Suppress("UNCHECKED_CAST")
    val getByteLength = js("(function(obj) { return obj != null && typeof obj.byteLength === 'number' ? obj.byteLength : null; })") as (Any?) -> Int?
    @Suppress("UNCHECKED_CAST")
    val getArrayElement = js("(function(arr, index) { return arr != null ? arr[index] : null; })") as (Any?, Int) -> Any?
    @Suppress("UNCHECKED_CAST")
    val createUint8ArrayFromBuffer = js("(function(buffer) { return new Uint8Array(buffer); })") as (Any?) -> Any?
    
    // Check if it's a JavaScript array (avoid Array<*> type check to prevent Cloneable error)
    if (isArray(raw)) {
        val length = getLength(raw) ?: return raw
        return (0 until length).map { index ->
            val value = getArrayElement(raw, index) as? Number
            value?.toInt()?.and(0xff) ?: 0
        }
    }
    
    // Check if it's a Uint8Array (by checking for length property)
    try {
        val length = getLength(raw)
        if (length != null && length >= 0) {
            // Possibly a Uint8Array, try converting to array
            return (0 until length).map { index ->
                val value = getArrayElement(raw, index) as? Number
                value?.toInt()?.and(0xff) ?: 0
            }
        }
    } catch (e: Exception) {
        // Ignore error, continue checking for ArrayBuffer
    }
    
    // Check if it's an ArrayBuffer (by checking byteLength property)
    try {
        val byteLength = getByteLength(raw)
        if (byteLength != null && byteLength >= 0) {
            // It's an ArrayBuffer, convert to Uint8Array then to array
            val uint8ArrayFromBuffer = createUint8ArrayFromBuffer(raw) ?: return raw
            val length = getLength(uint8ArrayFromBuffer) ?: return raw
            return (0 until length).map { index ->
                val value = getArrayElement(uint8ArrayFromBuffer, index) as? Number
                value?.toInt() ?: 0
            }
        }
    } catch (e: Exception) {
        // Ignore error
    }
    
    return raw
}

internal fun TextContainerUpgrade.toJsonString(): String =
    JsInteropUtils.buildJsonObject(
        "containerID" to containerID,
        "containerName" to containerName,
        "contentOffset" to contentOffset,
        "contentLength" to contentLength,
        "content" to content,
    )

internal fun ShutDownContainer.toJsonString(): String =
    JsInteropUtils.buildJsonObject("exitMode" to exitMode)

// Note: JSON building functionality has been moved to JsInteropUtils. Only container property extension functions remain here.

// Extension functions: Convert container properties to JSON Map (for internal serialization).
// Note: These functions need access to JsInteropUtils.buildJsonObject, but since they handle
// special types (like ListItemContainerProperty), they are kept here for now.

// Note: These toJsonMap functions need to handle ListItemContainerProperty and similar types.
// Since JsInteropUtils.buildJsonObject already handles basic types, we just call it here,
// but need to ensure ListItemContainerProperty and similar types serialize correctly.
private fun ListItemContainerProperty.toJsonMap(): String =
    JsInteropUtils.buildJsonObject(
        "itemCount" to itemCount,
        "itemWidth" to itemWidth,
        "isItemSelectBorderEn" to isItemSelectBorderEn,
        "itemName" to itemName,
    )

private fun ListContainerProperty.toJsonMap(): String =
    JsInteropUtils.buildJsonObject(
        "xPosition" to xPosition,
        "yPosition" to yPosition,
        "width" to width,
        "height" to height,
        "borderWidth" to borderWidth,
        "borderColor" to borderColor,
        "borderRdaius" to borderRdaius,
        "paddingLength" to paddingLength,
        "containerID" to containerID,
        "containerName" to containerName,
        "itemContainer" to itemContainer,
        "isEventCapture" to isEventCapture,
    )

private fun TextContainerProperty.toJsonMap(): String =
    JsInteropUtils.buildJsonObject(
        "xPosition" to xPosition,
        "yPosition" to yPosition,
        "width" to width,
        "height" to height,
        "borderWidth" to borderWidth,
        "borderColor" to borderColor,
        "borderRdaius" to borderRdaius,
        "paddingLength" to paddingLength,
        "containerID" to containerID,
        "containerName" to containerName,
        "isEventCapture" to isEventCapture,
        "content" to content,
    )

private fun ImageContainerProperty.toJsonMap(): String =
    JsInteropUtils.buildJsonObject(
        "xPosition" to xPosition,
        "yPosition" to yPosition,
        "width" to width,
        "height" to height,
        "containerID" to containerID,
        "containerName" to containerName,
    )
