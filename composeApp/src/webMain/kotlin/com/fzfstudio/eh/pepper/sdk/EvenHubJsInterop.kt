@file:OptIn(ExperimentalWasmJsInterop::class)

package com.fzfstudio.eh.pepper.sdk

import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny

/**
 * Minimal JS interop helpers for parsing JsAny.
 *
 * Use top-level expect/actual functions to avoid expect/actual class warnings.
 */
expect fun jsGet(obj: JsAny?, key: String): JsAny?

expect fun jsToStringOrNull(value: JsAny?): String?

expect fun jsToDoubleOrNull(value: JsAny?): Double?

expect fun jsToBoolOrNull(value: JsAny?): Boolean?

expect fun jsStringify(obj: JsAny?): String

expect fun jsParseJson(text: String): JsAny
