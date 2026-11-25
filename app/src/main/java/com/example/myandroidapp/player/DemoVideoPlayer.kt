package com.example.myandroidapp.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.MediaItem
import androidx.media3.datasource.RawResourceDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

/** Plays a bundled, locally controlled demo asset after the demo backend grants a session. */
@Composable
fun DemoVideoPlayer(resourceId: Int, modifier: Modifier = Modifier) {
    require(resourceId != 0) { "A bundled demo media resource is required." }
    val context = LocalContext.current
    val player = remember(resourceId) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(RawResourceDataSource.buildRawResourceUri(resourceId)))
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
