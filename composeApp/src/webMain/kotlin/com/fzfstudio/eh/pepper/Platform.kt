package com.fzfstudio.eh.pepper

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

/**
 * Get current timestamp in milliseconds, used for performance tracking.
 */
expect fun currentTimeMs(): Double