package com.fzfstudio.eh.pepper

import kotlin.js.Date

class JsPlatform : Platform {
    override val name: String = "Web with Kotlin/JS"
}

actual fun getPlatform(): Platform = JsPlatform()

actual fun currentTimeMs(): Double = Date.now()