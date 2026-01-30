@file:OptIn(ExperimentalWasmJsInterop::class)

package com.fzfstudio.eh.pepper.sdk

import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny

// Property access and conversions implemented via JS snippets.
actual fun jsGet(obj: JsAny?, key: String): JsAny? = jsGetImpl(obj, key)

actual fun jsToStringOrNull(value: JsAny?): String? = jsToStringOrNullImpl(value)

actual fun jsToDoubleOrNull(value: JsAny?): Double? = jsToDoubleOrNullImpl(value)

actual fun jsToBoolOrNull(value: JsAny?): Boolean? = jsToBoolOrNullImpl(value)

actual fun jsStringify(obj: JsAny?): String = jsStringifyImpl(obj)

actual fun jsParseJson(text: String): JsAny = jsParseJsonImpl(text)

// Safe property getter for JsAny objects.
@JsFun("(obj, key) => (obj && obj[key] !== undefined) ? obj[key] : undefined")
private external fun jsGetImpl(obj: JsAny?, key: String): JsAny?

// Best-effort string conversion.
@JsFun("(v) => (v === undefined || v === null) ? null : String(v)")
private external fun jsToStringOrNullImpl(value: JsAny?): String?

// Best-effort number conversion.
@JsFun("(v) => { const n = Number(v); return (v === undefined || v === null || Number.isNaN(n)) ? null : n; }")
private external fun jsToDoubleOrNullImpl(value: JsAny?): Double?

// Best-effort boolean conversion.
@JsFun("(v) => { if (v === undefined || v === null) return null; if (typeof v === 'boolean') return v; if (typeof v === 'number') return v !== 0; const s = String(v).toLowerCase(); return s === 'true' || s === '1'; }")
private external fun jsToBoolOrNullImpl(value: JsAny?): Boolean?

// Serialize with JSON for logging/debug.
@JsFun("(obj) => JSON.stringify(obj)")
private external fun jsStringifyImpl(obj: JsAny?): String

// Parse JSON string into JS object for SDK params.
@JsFun("(text) => JSON.parse(text)")
private external fun jsParseJsonImpl(text: String): JsAny
