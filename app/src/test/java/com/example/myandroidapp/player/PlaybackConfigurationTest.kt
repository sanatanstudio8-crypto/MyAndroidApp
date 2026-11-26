package com.example.myandroidapp.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class PlaybackConfigurationTest {
    @Test fun hlsIsDetected() {
        val config = PlaybackConfiguration("https://media.test/a.m3u8?token=x")
        assertEquals(PlaybackMediaType.HLS, config.resolvedMediaType())
        assertEquals("application/x-mpegURL", config.mimeType())
    }

    @Test fun dashIsDetected() {
        val config = PlaybackConfiguration("https://media.test/a.mpd")
        assertEquals(PlaybackMediaType.DASH, config.resolvedMediaType())
        assertEquals("application/dash+xml", config.mimeType())
    }

    @Test fun progressiveMp4IsDetected() {
        val config = PlaybackConfiguration("https://media.test/a.mp4")
        assertEquals(PlaybackMediaType.PROGRESSIVE, config.resolvedMediaType())
        assertEquals("video/mp4", config.mimeType())
    }

    @Test fun explicitMediaTypeOverridesExtension() {
        val config = PlaybackConfiguration("https://media.test/stream", mediaType = PlaybackMediaType.HLS)
        assertEquals(PlaybackMediaType.HLS, config.resolvedMediaType())
    }

    @Test fun expiryMetadataIsComparedToProvidedClock() {
        val expiry = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(2026, Calendar.NOVEMBER, 25, 10, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val config = PlaybackConfiguration("https://media.test/a.mp4", expiresAt = "2026-11-25T10:00:00Z")
        assertTrue(config.isExpired(expiry))
        assertFalse(config.isExpired(expiry - 1000))
    }

    @Test fun malformedExpiryFailsClosed() {
        val config = PlaybackConfiguration("https://media.test/a.mp4", expiresAt = "not-a-timestamp")
        assertTrue(config.isExpired(0L))
    }

    @Test fun absentExpiryDoesNotExpireSession() {
        val config = PlaybackConfiguration("https://media.test/a.mp4")
        assertFalse(config.isExpired(Long.MAX_VALUE))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsNonHttpsUrl() {
        PlaybackConfiguration("http://media.test/a.mp4")
    }
}
