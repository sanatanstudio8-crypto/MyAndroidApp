package com.example.myandroidapp.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.launch
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

/**
 * Plays only a media URL returned by the application's authorized backend.
 * The backend remains responsible for issuing and authorizing playback sessions.
 *
 * The legacy mediaUrl call remains source-compatible. Optional metadata can be
 * passed without changing the surrounding screen UI.
 */
@Composable
fun AuthorizedVideoPlayer(
    mediaUrl: String,
    modifier: Modifier = Modifier,
    title: String? = null,
    mediaType: PlaybackMediaType = PlaybackMediaType.AUTO,
    expiresAt: String? = null,
    onRetrySession: (suspend () -> String)? = null,
    onCloseSession: (suspend () -> Unit)? = null
) {
    val scope = rememberCoroutineScope()
    val refreshedUrl = remember(mediaUrl) { mutableStateOf(mediaUrl) }
    val configuration = remember(refreshedUrl.value, title, mediaType, expiresAt) {
        PlaybackConfiguration(refreshedUrl.value, title, mediaType, expiresAt)
    }
    val expired = configuration.isExpired()
    val context = LocalContext.current
    val isLoading = remember(configuration) { mutableStateOf(!expired) }
    val errorMessage = remember(configuration) { mutableStateOf<String?>(null) }
    val retryAttempt = remember(configuration) { mutableIntStateOf(0) }

    val player = remember(configuration, expired, retryAttempt.intValue) {
        if (expired) null else ExoPlayer.Builder(context).build().apply {
            val metadata = MediaMetadata.Builder()
                .setTitle(configuration.title?.takeIf { it.isNotBlank() })
                .setDisplayTitle(configuration.title?.takeIf { it.isNotBlank() })
                .build()
            val itemBuilder = MediaItem.Builder()
                .setUri(configuration.mediaUrl)
                .setMediaMetadata(metadata)
            configuration.mimeType()?.let(itemBuilder::setMimeType)
            setMediaItem(itemBuilder.build())
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    isLoading.value = playbackState == Player.STATE_BUFFERING
                }

                override fun onIsLoadingChanged(loading: Boolean) {
                    isLoading.value = loading
                }

                override fun onPlayerError(error: PlaybackException) {
                    isLoading.value = false
                    errorMessage.value = error.message ?: "Playback failed. Please try again."
                }
            })
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(player, onCloseSession) {
        onDispose {
            player?.release()
            if (onCloseSession != null) scope.launch { runCatching { onCloseSession.invoke() } }
        }
    }

    Box(modifier = modifier) {
        if (player != null) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { viewContext ->
                    PlayerView(viewContext).apply {
                        useController = true
                        controllerAutoShow = true
                        this.player = player
                    }
                },
                update = { it.player = player }
            )
            if (isLoading.value && errorMessage.value == null) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            errorMessage.value?.let { message ->
                androidx.compose.foundation.layout.Column(
                    modifier = Modifier.align(Alignment.Center).padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = message)
                    Button(onClick = {
                        errorMessage.value = null
                        isLoading.value = true
                        if (onRetrySession != null) {
                            scope.launch {
                                runCatching { onRetrySession.invoke() }
                                    .onSuccess { url -> refreshedUrl.value = url; retryAttempt.intValue += 1 }
                                    .onFailure { errorMessage.value = it.message ?: "Playback session refresh failed."; isLoading.value = false }
                            }
                        } else retryAttempt.intValue += 1
                    }) { Text(if (onRetrySession != null) "Refresh session and retry" else "Retry playback") }
                }
            }
        } else {
            Text(
                text = if (expired) "Playback session expired. Please request a new session."
                    else "Playback unavailable.",
                modifier = Modifier.align(Alignment.Center).padding(16.dp)
            )
        }
    }
}
