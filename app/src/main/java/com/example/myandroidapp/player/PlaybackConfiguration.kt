package com.example.myandroidapp.player

import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

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

    /** Parses ISO-8601 expiry timestamps on the app's minSdk 24. */
    fun isExpired(nowEpochMillis: Long = System.currentTimeMillis()): Boolean {
        val value = expiresAt ?: return false
        val patterns = listOf("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", "yyyy-MM-dd'T'HH:mm:ssXXX")
        val expiry = patterns.firstNotNullOfOrNull { pattern ->
            val parser = SimpleDateFormat(pattern, Locale.US).apply {
                isLenient = false
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val position = ParsePosition(0)
            parser.parse(value, position)?.takeIf { position.index == value.length }?.time
        } ?: return false
        return nowEpochMillis >= expiry
    }
}
