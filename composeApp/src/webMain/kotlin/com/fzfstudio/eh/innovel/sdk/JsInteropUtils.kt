@file:OptIn(ExperimentalWasmJsInterop::class)
@file:Suppress("UNCHECKED_CAST", "CAST_NEVER_SUCCEEDS")

package com.fzfstudio.eh.innovel.sdk

import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.Promise
import kotlin.js.js
import kotlinx.coroutines.await

// ==================== Fetch API external declarations (must be at top level) ====================

/**
 * JavaScript fetch function.
 */
external fun fetch(url: String): Promise<JsAny>

/**
 * JavaScript interop utility class.
 *
 * Encapsulates common JavaScript interaction functionality, including:
 * - Type conversion (JsAny to Kotlin types)
 * - Promise handling
 * - JSON serialization/deserialization
 * - Array and object access
 * - Fetch API wrappers
 */
object JsInteropUtils {
    
    // ==================== Type Conversion ====================

    /**
     * Convert JsAny to String, returns null if null or undefined.
     */
    fun toStringOrNull(value: JsAny?): String? = jsToStringOrNull(value)
    
    /**
     * Convert JsAny to String, returns empty string if null or undefined.
     */
    fun toString(value: JsAny?): String = toStringOrNull(value) ?: ""
    
    /**
     * Convert JsAny to Int, returns null if null or undefined.
     */
    fun toIntOrNull(value: JsAny?): Int? = jsToDoubleOrNull(value)?.toInt()
    
    /**
     * Convert JsAny to Int, returns 0 if null or undefined.
     */
    fun toInt(value: JsAny?): Int = toIntOrNull(value) ?: 0
    
    /**
     * Convert JsAny to Double, returns null if null or undefined.
     */
    fun toDoubleOrNull(value: JsAny?): Double? = jsToDoubleOrNull(value)
    
    /**
     * Convert JsAny to Boolean, returns null if null or undefined.
     */
    fun toBooleanOrNull(value: JsAny?): Boolean? = jsToBoolOrNull(value)
    
    // ==================== Object Property Access ====================

    /**
     * Safely get a JavaScript object's property value.
     * @param obj JavaScript object
     * @param key Property name
     * @return Property value, or null if it doesn't exist
     */
    fun getProperty(obj: JsAny?, key: String): JsAny? = jsGet(obj, key)
    
    /**
     * Get a string property from an object.
     */
    fun getStringProperty(obj: JsAny?, key: String): String? = 
        toStringOrNull(getProperty(obj, key))
    
    /**
     * Get an integer property from an object.
     */
    fun getIntProperty(obj: JsAny?, key: String): Int? = 
        toIntOrNull(getProperty(obj, key))
    
    /**
     * Get a boolean property from an object.
     */
    fun getBooleanProperty(obj: JsAny?, key: String): Boolean? = 
        toBooleanOrNull(getProperty(obj, key))
    
    // ==================== Type Checking ====================

    /**
     * Get the type string of a JavaScript value.
     * @param value JavaScript value
     * @return Type string ('number', "string", "boolean", "object", "null", "undefined")
     */
    fun getType(value: JsAny?): String {
        if (value == null) return "null"
        return when (value) {
            is Number -> "number"
            is Boolean -> "boolean"
            is String -> "string"
            else -> {
                // For objects, check for length property to determine if it's an array
                val length = getProperty(value, "length")
                if (length != null && length is Number) {
                    "object" // Possibly an array
                } else {
                    "object"
                }
            }
        }
    }
    
    /**
     * Check if a value is a JavaScript array.
     * @param value JavaScript value
     * @return true if it's an array
     */
    fun isArray(value: JsAny?): Boolean {
        if (value == null) return false
        val length = getProperty(value, "length")
        return length != null && length is Number && getType(length) == "number"
    }
    
    /**
     * Get array length.
     * @param array JavaScript array
     * @return Array length, or 0 if not an array
     */
    fun getArrayLength(array: JsAny?): Int {
        if (!isArray(array)) return 0
        return toIntOrNull(getProperty(array, "length")) ?: 0
    }
    
    /**
     * Get an array element.
     * @param array JavaScript array
     * @param index Index
     * @return Array element, or null if it doesn't exist
     */
    fun getArrayElement(array: JsAny?, index: Int): JsAny? {
        if (!isArray(array)) return null
        return getProperty(array, index.toString())
    }
    
    // ==================== JSON Processing ====================

    /**
     * Serialize a JavaScript object to a JSON string.
     * @param obj JavaScript object
     * @return JSON string
     */
    fun stringify(obj: JsAny?): String = jsStringify(obj)
    
    /**
     * Parse a JSON string into a JavaScript object.
     * @param text JSON string
     * @return JavaScript object
     */
    fun parseJson(text: String): JsAny = jsParseJson(text)
    
    // ==================== Promise Handling ====================

    // Note: Uses kotlinx.coroutines.await extension function instead of custom implementation
    // to avoid linker errors in WebView environments
    
    // ==================== Fetch API ====================
    
    /**
     * Fetch text content using the Fetch API.
     * @param url Resource URL
     * @return Text content
     */
    suspend fun fetchText(url: String): String {
        val response = fetch(url).await()
        // Use js() to call response.text()
        // Note: js() requires a string expression and can't use variables directly,
        // so we use a wrapper function
        @Suppress("UNCHECKED_CAST")
        val callText = js("(function(r) { return r.text(); })") as (JsAny) -> Promise<JsAny>
        val textPromise = callText(response)
        val textJs = textPromise.await()
        return toStringOrNull(textJs) ?: ""
    }
    
    /**
     * Fetch JSON content using the Fetch API.
     * @param url Resource URL
     * @return Parsed JavaScript object
     */
    suspend fun fetchJson(url: String): JsAny? {
        val text = fetchText(url)
        return if (text.isNotEmpty()) {
            try {
                parseJson(text)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }
    
    // ==================== JSON Building Utilities ====================

    /**
     * Build a JSON object string.
     * @param fields Field list (key-value pairs)
     * @return JSON object string
     */
    @Suppress("UNCHECKED_CAST")
    fun buildJsonObject(vararg fields: Pair<String, Any?>): String {
        // In Kotlin/JS, vararg is converted to Array
        // To avoid Cloneable errors, we use jsGet to access the array
        @Suppress("UNCHECKED_CAST")
        val fieldsJs = fields as? JsAny ?: return "{}"
        
        // Use jsGet to get array length
        val length = toIntOrNull(getProperty(fieldsJs, "length")) ?: 0
        val fieldsList = mutableListOf<Pair<String, Any?>>()
        
        // Use jsGet to access array elements and Pair properties
        // In Kotlin/JS, try multiple ways to access Pair properties
        for (i in 0 until length) {
            val fieldJs = getProperty(fieldsJs, i.toString())
            if (fieldJs != null) {
                var key: String? = null
                var value: Any? = null
                
                // Method 1: Try direct type cast to Pair
                try {
                    @Suppress("UNCHECKED_CAST")
                    val pair = fieldJs as? Pair<*, *>
                    if (pair != null) {
                        key = pair.first as? String
                        value = pair.second
                    }
                } catch (e: Exception) {
                    // Continue trying other methods
                }
                
                // Method 2: If method 1 failed, try using getProperty to access "first" and "second"
                if (key == null) {
                    key = toStringOrNull(getProperty(fieldJs, "first"))
                    value = getProperty(fieldJs, "second") as? Any?
                }
                
                // Method 3: If still failed, try bracket notation (via jsGet)
                if (key == null) {
                    // Try accessing other possible property names
                    key = toStringOrNull(getProperty(fieldJs, "component1"))
                    if (key == null) {
                        value = getProperty(fieldJs, "component2") as? Any?
                    }
                }
                
                if (key != null) {
                    fieldsList.add(key to value)
                }
            }
        }
        
        return buildJsonObjectInternal(fieldsList)
    }
    
    /**
     * Internal implementation: Build JSON object using List.
     */
    private fun buildJsonObjectInternal(fields: List<Pair<String, Any?>>): String {
        val parts = mutableListOf<String>()
        for (field in fields) {
            val (key, value) = field
            if (value != null) {
                val jsonValueStr = jsonValue(value)
                parts.add("\"${escapeJson(key)}\":$jsonValueStr")
            }
        }
        val body = parts.joinToString(",")
        val result = "{$body}"
        return result
    }
    
    /**
     * Build a JSON array string.
     * @param values Value list
     * @return JSON array string
     */
    fun buildJsonArray(values: List<*>): String {
        return values.joinToString(prefix = "[", postfix = "]") { jsonValue(it) }
    }
    
    /**
     * Convert a value to its JSON string representation.
     */
    @Suppress("UNCHECKED_CAST")
    private fun jsonValue(value: Any?): String {
        return when {
            value == null -> "null"
            value is Number -> {
                val asLong = value.toLong()
                if (asLong < 0) {
                    val unsigned = asLong and 0xFFFFFFFFL
                    unsigned.toString()
                } else {
                    asLong.toString()
                }
            }
            value is Boolean -> value.toString()
            value is String -> "\"${escapeJson(value)}\""
            // Check for List first (in Kotlin/JS, List is an array at runtime)
            // Note: This check must come before JsAny since List may not be JsAny
            value is List<*> -> {
                buildJsonArray(value)
            }
            // Handle JsAny types from jsGet, try to identify as array or List
            // Note: In Kotlin/JS, List is a JavaScript array at runtime, so check for array first
            value is JsAny -> {
                // Check if it's an array (by checking length property)
                // In Kotlin/JS, List is an array at runtime, so isArray should match List
                if (isArray(value)) {
                    val length = getArrayLength(value)
                    val list = mutableListOf<Any?>()
                    for (i in 0 until length) {
                        val element = getArrayElement(value, i)
                        @Suppress("UNCHECKED_CAST")
                        list.add(element as? Any?)
                    }
                    buildJsonArray(list)
                } else {
                    // Try to identify as container property types
                    val className = value::class.simpleName
                    when (className) {
                        "ListItemContainerProperty" -> {
                            @Suppress("UNCHECKED_CAST")
                            val prop = value as? ListItemContainerProperty
                            if (prop != null) {
                                buildJsonObject(
                                    "itemCount" to prop.itemCount,
                                    "itemWidth" to prop.itemWidth,
                                    "isItemSelectBorderEn" to prop.isItemSelectBorderEn,
                                    "itemName" to prop.itemName,
                                )
                            } else {
                                // If type cast failed, try accessing via properties
                                buildJsonObject(
                                    "itemCount" to getIntProperty(value, "itemCount"),
                                    "itemWidth" to getIntProperty(value, "itemWidth"),
                                    "isItemSelectBorderEn" to getIntProperty(value, "isItemSelectBorderEn"),
                                    "itemName" to getProperty(value, "itemName"),
                                )
                            }
                        }
                        "ListContainerProperty" -> {
                            @Suppress("UNCHECKED_CAST")
                            val prop = value as? ListContainerProperty
                            if (prop != null) {
                                buildJsonObject(
                                    "xPosition" to prop.xPosition,
                                    "yPosition" to prop.yPosition,
                                    "width" to prop.width,
                                    "height" to prop.height,
                                    "borderWidth" to prop.borderWidth,
                                    "borderColor" to prop.borderColor,
                                    "borderRdaius" to prop.borderRdaius,
                                    "paddingLength" to prop.paddingLength,
                                    "containerID" to prop.containerID,
                                    "containerName" to prop.containerName,
                                    "itemContainer" to prop.itemContainer,
                                    "isEventCapture" to prop.isEventCapture,
                                )
                            } else {
                                // Access via properties
                                buildJsonObject(
                                    "xPosition" to getIntProperty(value, "xPosition"),
                                    "yPosition" to getIntProperty(value, "yPosition"),
                                    "width" to getIntProperty(value, "width"),
                                    "height" to getIntProperty(value, "height"),
                                    "borderWidth" to getIntProperty(value, "borderWidth"),
                                    "borderColor" to getIntProperty(value, "borderColor"),
                                    "borderRdaius" to getIntProperty(value, "borderRdaius"),
                                    "paddingLength" to getIntProperty(value, "paddingLength"),
                                    "containerID" to getIntProperty(value, "containerID"),
                                    "containerName" to getStringProperty(value, "containerName"),
                                    "itemContainer" to getProperty(value, "itemContainer"),
                                    "isEventCapture" to getIntProperty(value, "isEventCapture"),
                                )
                            }
                        }
                        "TextContainerProperty" -> {
                            @Suppress("UNCHECKED_CAST")
                            val prop = value as? TextContainerProperty
                            if (prop != null) {
                                buildJsonObject(
                                    "xPosition" to prop.xPosition,
                                    "yPosition" to prop.yPosition,
                                    "width" to prop.width,
                                    "height" to prop.height,
                                    "borderWidth" to prop.borderWidth,
                                    "borderColor" to prop.borderColor,
                                    "borderRdaius" to prop.borderRdaius,
                                    "paddingLength" to prop.paddingLength,
                                    "containerID" to prop.containerID,
                                    "containerName" to prop.containerName,
                                    "isEventCapture" to prop.isEventCapture,
                                    "content" to prop.content,
                                )
                            } else {
                                // Access via properties
                                buildJsonObject(
                                    "xPosition" to getIntProperty(value, "xPosition"),
                                    "yPosition" to getIntProperty(value, "yPosition"),
                                    "width" to getIntProperty(value, "width"),
                                    "height" to getIntProperty(value, "height"),
                                    "borderWidth" to getIntProperty(value, "borderWidth"),
                                    "borderColor" to getIntProperty(value, "borderColor"),
                                    "borderRdaius" to getIntProperty(value, "borderRdaius"),
                                    "paddingLength" to getIntProperty(value, "paddingLength"),
                                    "containerID" to getIntProperty(value, "containerID"),
                                    "containerName" to getStringProperty(value, "containerName"),
                                    "isEventCapture" to getIntProperty(value, "isEventCapture"),
                                    "content" to getStringProperty(value, "content"),
                                )
                            }
                        }
                        "ImageContainerProperty" -> {
                            @Suppress("UNCHECKED_CAST")
                            val prop = value as? ImageContainerProperty
                            if (prop != null) {
                                buildJsonObject(
                                    "xPosition" to prop.xPosition,
                                    "yPosition" to prop.yPosition,
                                    "width" to prop.width,
                                    "height" to prop.height,
                                    "containerID" to prop.containerID,
                                    "containerName" to prop.containerName,
                                )
                            } else {
                                // Access via properties
                                buildJsonObject(
                                    "xPosition" to getIntProperty(value, "xPosition"),
                                    "yPosition" to getIntProperty(value, "yPosition"),
                                    "width" to getIntProperty(value, "width"),
                                    "height" to getIntProperty(value, "height"),
                                    "containerID" to getIntProperty(value, "containerID"),
                                    "containerName" to getStringProperty(value, "containerName"),
                                )
                            }
                        }
                        else -> {
                            // Try to convert to string
                            "\"${escapeJson(value.toString())}\""
                        }
                    }
                }
            }
            // Handle container property types (direct type check, for non-JsAny cases)
            value is ListItemContainerProperty -> buildJsonObject(
                "itemCount" to value.itemCount,
                "itemWidth" to value.itemWidth,
                "isItemSelectBorderEn" to value.isItemSelectBorderEn,
                "itemName" to value.itemName,
                )
            value is ListContainerProperty -> buildJsonObject(
                "xPosition" to value.xPosition,
                "yPosition" to value.yPosition,
                "width" to value.width,
                "height" to value.height,
                "borderWidth" to value.borderWidth,
                "borderColor" to value.borderColor,
                "borderRdaius" to value.borderRdaius,
                "paddingLength" to value.paddingLength,
                "containerID" to value.containerID,
                "containerName" to value.containerName,
                "itemContainer" to value.itemContainer,
                "isEventCapture" to value.isEventCapture,
                )
            value is TextContainerProperty -> buildJsonObject(
                "xPosition" to value.xPosition,
                "yPosition" to value.yPosition,
                "width" to value.width,
                "height" to value.height,
                "borderWidth" to value.borderWidth,
                "borderColor" to value.borderColor,
                "borderRdaius" to value.borderRdaius,
                "paddingLength" to value.paddingLength,
                "containerID" to value.containerID,
                "containerName" to value.containerName,
                "isEventCapture" to value.isEventCapture,
                "content" to value.content,
                )
            value is ImageContainerProperty -> buildJsonObject(
                "xPosition" to value.xPosition,
                "yPosition" to value.yPosition,
                "width" to value.width,
                "height" to value.height,
                "containerID" to value.containerID,
                "containerName" to value.containerName,
                )
            else -> {
                // Handle array types, using class name string check to avoid Cloneable issues
                val arrayValue = tryConvertToArray(value)
                if (arrayValue != null) {
                    buildJsonArray(arrayValue)
                } else {
                    "\"${escapeJson(value.toString())}\""
                }
            }
        }
    }

    /**
     * Try to convert a value to List, for handling ByteArray and IntArray.
     */
    @Suppress("UNCHECKED_CAST")
    private fun tryConvertToArray(value: Any?): List<*>? {
        if (value == null) return null
        return try {
            val className = value::class.simpleName
            when (className) {
                "ByteArray", "IntArray" -> {
                    val jsValue = value as? JsAny
                    if (jsValue != null) {
                        val length = toIntOrNull(getProperty(jsValue, "length"))
                            ?: toIntOrNull(getProperty(jsValue, "size")) ?: 0
                        (0 until length).mapNotNull { index ->
                            toDoubleOrNull(getProperty(jsValue, index.toString()))
                        }
                    } else {
                        null
                    }
                }
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Escape special characters in a JSON string.
     */
    private fun escapeJson(value: String): String = buildString {
        for (ch in value) {
            when (ch) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(ch)
            }
        }
    }
}
