package com.example.myandroidapp.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

/**
 * Plays only a media URL returned by the application's authorized backend.
 * Do not pass scraped, copied, or otherwise unauthorized RWA media URLs here.
 */
@Composable
fun AuthorizedVideoPlayer(
    mediaUrl: String,
    modifier: Modifier = Modifier
) {
    require(mediaUrl.startsWith("https://")) { "Only HTTPS playback URLs are accepted." }
    val context = LocalContext.current
    val player = androidx.compose.runtime.remember(mediaUrl) {
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
        factory = { viewContext -> PlayerView(viewContext).apply { this.player = player } },
        update = { it.player = player }
    )
}
