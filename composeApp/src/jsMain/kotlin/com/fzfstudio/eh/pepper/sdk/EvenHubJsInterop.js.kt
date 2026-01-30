@file:OptIn(ExperimentalWasmJsInterop::class)

package com.fzfstudio.eh.pepper.sdk

import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.jsTypeOf

// Safe property access with undefined protection.
actual fun jsGet(obj: JsAny?, key: String): JsAny? {
    if (obj == null) return null
    val value = obj.asDynamic()[key]
    return if (jsTypeOf(value) == "undefined") null else value as JsAny
}

// Best-effort string conversion.
actual fun jsToStringOrNull(value: JsAny?): String? {
    if (value == null || jsTypeOf(value) == "undefined") return null
    return value.toString()
}

// Best-effort number conversion (Number or numeric string).
actual fun jsToDoubleOrNull(value: JsAny?): Double? {
    if (value == null || jsTypeOf(value) == "undefined") return null
    val number = when (value) {
        is Number -> value.toDouble()
        else -> value.toString().toDoubleOrNull()
    }
    return number
}

// Best-effort boolean conversion.
actual fun jsToBoolOrNull(value: JsAny?): Boolean? {
    if (value == null || jsTypeOf(value) == "undefined") return null
    return when (value) {
        is Boolean -> value
        is Number -> value.toInt() != 0
        else -> value.toString().equals("true", ignoreCase = true)
    }
}

// Serialize with native JSON for debugging/logging.
actual fun jsStringify(obj: JsAny?): String = JSON.stringify(obj)

// Parse JSON string into JS object for SDK params.
actual fun jsParseJson(text: String): JsAny = JSON.parse(text)

private external object JSON {
    fun parse(text: String): JsAny
    fun stringify(obj: JsAny?): String
}
