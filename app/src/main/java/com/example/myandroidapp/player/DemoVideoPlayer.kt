package com.example.myandroidapp.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

/** Plays the approved CC0 sample stream only after the demo backend grants a session. */
@Composable
fun DemoVideoPlayer(mediaUrl: String, modifier: Modifier = Modifier) {
    require(mediaUrl == DEMO_CC0_SAMPLE_URL) { "Only the approved CC0 demo test stream is allowed here." }
    val context = LocalContext.current
    val player = remember(mediaUrl) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(mediaUrl))
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(player) {
        onDispose { player.release() }
    }
    AndroidView(
        modifier = modifier,
        factory = { viewContext ->
            PlayerView(viewContext).apply {
                useController = true
                controllerAutoShow = true
                player = player
            }
        },
        update = { it.player = player }
    )
}

const val DEMO_CC0_SAMPLE_URL =
    "https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4"
