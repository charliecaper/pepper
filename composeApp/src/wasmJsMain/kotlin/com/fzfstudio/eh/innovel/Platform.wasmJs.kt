@file:OptIn(ExperimentalWasmJsInterop::class)

package com.fzfstudio.eh.innovel

import kotlin.js.ExperimentalWasmJsInterop

class WasmPlatform : Platform {
    override val name: String = "Web with Kotlin/Wasm"
}

actual fun getPlatform(): Platform = WasmPlatform()

// Use JS interop to get current time
@JsFun("() => Date.now()")
private external fun jsDateNow(): Double

actual fun currentTimeMs(): Double = jsDateNow()