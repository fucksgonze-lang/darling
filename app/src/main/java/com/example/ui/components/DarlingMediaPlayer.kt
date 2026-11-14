package com.example.ui.components

import android.media.MediaPlayer
import android.net.Uri
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.annotation.RawRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.data.util.MediaHelper
import com.example.data.util.MusicCatalog
import com.example.ui.theme.DarlingGradientBrush
import com.example.ui.theme.DarlingRose

/**
 * Plays a bundled raw background music resource cleanly in a loop while [isPlaying] is true.
 */
@Composable
fun BackgroundMusicEffect(
    @RawRes rawResId: Int?,
    isPlaying: Boolean,
    volume: Float = 0.85f
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val player = remember(rawResId) {
        if (rawResId == null) {
            null
        } else {
            runCatching {
                MediaPlayer.create(context, rawResId)?.apply {
                    isLooping = true
                    setVolume(volume, volume)
                }
            }.getOrNull()
        }
    }

    LaunchedEffect(player, isPlaying, volume) {
        if (player != null) {
            runCatching {
                player.setVolume(volume, volume)
                if (isPlaying && !player.isPlaying) {
                    player.start()
                } else if (!isPlaying && player.isPlaying) {
                    player.pause()
                }
            }
        }
    }

    DisposableEffect(lifecycleOwner, player) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                runCatching {
                    if (player?.isPlaying == true) player.pause()
                }
            } else if (event == Lifecycle.Event.ON_RESUME && isPlaying) {
                runCatching {
                    if (player != null && !player.isPlaying) player.start()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            runCatching {
                if (player?.isPlaying == true) player.stop()
                player?.release()
            }
        }
    }
}

/**
 * Full-bleed Media3 ExoPlayer video + synced optional background music renderer for Reels & Posts.
 */
@OptIn(UnstableApi::class)
@Composable
fun DarlingReelMediaStage(
    videoUri: String?,
    fallbackMediaData: String?,
    fallbackText: String,
    musicTrackId: String?,
    isActive: Boolean,
    isUserPaused: Boolean,
    isMuted: Boolean,
    muteOriginalVideoAudio: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val canPlayVideo = remember(videoUri) { MediaHelper.isPlayableVideoUri(context, videoUri) }
    val musicTrack = remember(musicTrackId) { MusicCatalog.findById(musicTrackId) }
    val shouldPlay = isActive && !isUserPaused

    BackgroundMusicEffect(
        rawResId = musicTrack?.rawResId,
        isPlaying = shouldPlay && !isMuted,
        volume = 0.85f
    )

    if (canPlayVideo && !videoUri.isNullOrBlank()) {
        val exoPlayer = remember(videoUri) {
            ExoPlayer.Builder(context).build().apply {
                repeatMode = Player.REPEAT_MODE_ONE
                setMediaItem(MediaItem.fromUri(Uri.parse(videoUri)))
                prepare()
            }
        }

        LaunchedEffect(exoPlayer, shouldPlay, isMuted, muteOriginalVideoAudio, musicTrack) {
            exoPlayer.volume = when {
                isMuted -> 0f
                muteOriginalVideoAudio && musicTrack != null -> 0f
                musicTrack != null -> 0.35f
                else -> 1f
            }
            exoPlayer.playWhenReady = shouldPlay
        }

        DisposableEffect(lifecycleOwner, exoPlayer) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> exoPlayer.pause()
                    Lifecycle.Event.ON_RESUME -> if (shouldPlay) exoPlayer.play()
                    else -> Unit
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                exoPlayer.release()
            }
        }

        Box(modifier = modifier.background(Color.Black)) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        player = exoPlayer
                    }
                },
                update = { view ->
                    if (view.player !== exoPlayer) {
                        view.player = exoPlayer
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            if (isUserPaused) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Paused",
                        tint = Color.White,
                        modifier = Modifier.size(38.dp)
                    )
                }
            }
        }
    } else {
        // Photo-reel or video thumbnail stage with smooth breathing motion
        val infiniteTransition = rememberInfiniteTransition(label = "reel_motion")
        val zoomScale by infiniteTransition.animateFloat(
            initialValue = 1.0f,
            targetValue = if (shouldPlay) 1.08f else 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 7000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "zoom"
        )

        Box(modifier = modifier.background(Color.Black)) {
            if (!fallbackMediaData.isNullOrBlank()) {
                DarlingMediaImage(
                    mediaDataOrUrl = fallbackMediaData,
                    contentDescription = fallbackText,
                    fallbackText = fallbackText,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = zoomScale
                            scaleY = zoomScale
                        }
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(DarlingGradientBrush),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = fallbackText,
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(28.dp)
                    )
                }
            }
            if (isUserPaused) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isUserPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                        contentDescription = "Paused",
                        tint = Color.White,
                        modifier = Modifier.size(38.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MusicTrackPill(
    title: String,
    artist: String,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "disc_spin")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4200, easing = LinearEasing)
        ),
        label = "spin"
    )

    Surface(
        color = Color.Black.copy(alpha = 0.52f),
        shape = RoundedCornerShape(50),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(DarlingRose)
                    .rotate(if (isPlaying) angle else 0f),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.MusicNote,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
            Text(
                text = "$title • $artist",
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
