package com.example.myandroidapp.player

import java.time.Instant
import java.time.format.DateTimeParseException

enum class PlaybackMediaType {
    AUTO,
    HLS,
    DASH,
    PROGRESSIVE
}

/** Playback metadata supplied by the application's authorized backend. */
data class PlaybackConfiguration(
    val mediaUrl: String,
    val title: String? = null,
    val mediaType: PlaybackMediaType = PlaybackMediaType.AUTO,
    val expiresAt: String? = null
) {
    init {
        require(mediaUrl.startsWith("https://", ignoreCase = true)) {
            "Only HTTPS playback URLs are accepted."
        }
    }

    fun resolvedMediaType(): PlaybackMediaType {
        if (mediaType != PlaybackMediaType.AUTO) return mediaType
        val path = mediaUrl.substringBefore('?').substringBefore('#').lowercase()
        return when {
            path.endsWith(".m3u8") -> PlaybackMediaType.HLS
            path.endsWith(".mpd") -> PlaybackMediaType.DASH
            path.endsWith(".mp4") || path.endsWith(".m4v") -> PlaybackMediaType.PROGRESSIVE
            else -> PlaybackMediaType.AUTO
        }
    }

    fun mimeType(): String? = when (resolvedMediaType()) {
        PlaybackMediaType.HLS -> "application/x-mpegURL"
        PlaybackMediaType.DASH -> "application/dash+xml"
        PlaybackMediaType.PROGRESSIVE -> "video/mp4"
        PlaybackMediaType.AUTO -> null
    }

    /**
     * Expiry is security-sensitive: missing expiry remains allowed for providers
     * that do not publish it, but a supplied malformed expiry fails closed.
     */
    fun isExpired(nowEpochMillis: Long = System.currentTimeMillis()): Boolean {
        val value = expiresAt ?: return false
        val expiry = try {
            Instant.parse(value).toEpochMilli()
        } catch (_: DateTimeParseException) {
            return true
        } catch (_: ArithmeticException) {
            return true
        }
        return nowEpochMillis >= expiry
    }
}
