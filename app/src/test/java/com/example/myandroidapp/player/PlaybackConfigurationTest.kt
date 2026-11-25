package com.example.myandroidapp.player

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackConfigurationTest {
    @Test fun hlsIsDetected() { assertEquals(PlaybackMediaType.HLS, PlaybackConfiguration("https://media.test/a.m3u8").resolvedMediaType()) }
    @Test fun dashIsDetected() { assertEquals(PlaybackMediaType.DASH, PlaybackConfiguration("https://media.test/a.mpd").resolvedMediaType()) }
    @Test fun mp4IsDetected() { assertEquals(PlaybackMediaType.PROGRESSIVE, PlaybackConfiguration("https://media.test/a.mp4").resolvedMediaType()) }
}
