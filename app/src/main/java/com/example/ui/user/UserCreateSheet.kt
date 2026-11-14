package com.example.ui.user

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.PublicProfile
import com.example.data.util.MediaHelper
import com.example.data.util.MusicCatalog
import com.example.data.util.MusicTrack
import com.example.ui.components.BackgroundMusicEffect
import com.example.ui.components.DarlingMediaImage
import com.example.ui.components.DarlingReelMediaStage
import com.example.ui.components.StatusPill
import com.example.ui.theme.DarlingRose
import com.example.ui.theme.DarlingViolet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateContentBottomSheet(
    initialMode: Int, // 0 = Post, 1 = Reel, 2 = Story
    author: PublicProfile?,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onPublishPostOrReel: (
        caption: String,
        mediaType: String,
        mediaData: String?,
        location: String?,
        isReel: Boolean,
        videoUri: String?,
        videoDurationMs: Long?,
        musicTrackId: String?,
        musicTitle: String?,
        musicArtist: String?,
        muteOriginalAudio: Boolean
    ) -> Unit,
    onPublishStory: (caption: String, mediaData: String?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var mode by remember { mutableIntStateOf(initialMode) }
    var caption by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var mediaData by remember { mutableStateOf<String?>(null) }
    var videoUri by remember { mutableStateOf<String?>(null) }
    var videoDurationMs by remember { mutableStateOf<Long?>(null) }
    var mediaBrightness by remember { mutableStateOf<Float?>(null) }
    var isVideoSelected by remember { mutableStateOf(false) }
    var isPreparingMedia by remember { mutableStateOf(false) }

    var autoSuggestMusicEnabled by remember { mutableStateOf(initialMode == 1) }
    var userManuallyTouchedMusic by remember { mutableStateOf(false) }
    var selectedTrack by remember { mutableStateOf<MusicTrack?>(null) }
    var muteOriginalAudio by remember { mutableStateOf(false) }
    var previewingTrackId by remember { mutableStateOf<String?>(null) }
    var showMusicBrowser by remember { mutableStateOf(false) }

    val userInterests = author?.interests.orEmpty()
    val suggestions = remember(caption, location, videoDurationMs, mediaBrightness, userInterests) {
        MusicCatalog.suggestTracks(
            caption = caption,
            location = location,
            durationMs = videoDurationMs,
            averageBrightness = mediaBrightness,
            userInterests = userInterests
        )
    }

    // Automatically assign the best matching track whenever video is picked or Reel auto-music is on
    LaunchedEffect(suggestions, autoSuggestMusicEnabled, mode, isVideoSelected) {
        if (mode != 2 && autoSuggestMusicEnabled && !userManuallyTouchedMusic) {
            if (isVideoSelected || mode == 1) {
                selectedTrack = suggestions.firstOrNull()?.track
            }
        }
    }

    val activePreviewTrack = remember(previewingTrackId) {
        MusicCatalog.findById(previewingTrackId)
    }

    // Real audio preview playback while creating
    BackgroundMusicEffect(
        rawResId = activePreviewTrack?.rawResId,
        isPlaying = activePreviewTrack != null
    )

    val handlePickedUri: (android.net.Uri) -> Unit = { uri ->
        scope.launch {
            isPreparingMedia = true
            val isVid = withContext(Dispatchers.IO) { MediaHelper.isVideoMimeType(context, uri) }
            if (isVid && mode != 2) {
                val prepared = withContext(Dispatchers.IO) { MediaHelper.preparePickedVideo(context, uri) }
                if (prepared != null) {
                    videoUri = prepared.playableUri
                    mediaData = prepared.thumbnailDataUri
                    videoDurationMs = prepared.durationMs
                    mediaBrightness = prepared.averageBrightness
                    isVideoSelected = true
                    autoSuggestMusicEnabled = true
                    userManuallyTouchedMusic = false
                    val best = MusicCatalog.suggestTracks(
                        caption = caption,
                        location = location,
                        durationMs = prepared.durationMs,
                        averageBrightness = prepared.averageBrightness,
                        userInterests = userInterests
                    ).firstOrNull()?.track
                    selectedTrack = best
                    previewingTrackId = best?.id
                }
            } else {
                val imgData = withContext(Dispatchers.IO) { MediaHelper.uriToCompactDataUri(context, uri) }
                if (imgData != null) {
                    mediaData = imgData
                    videoUri = null
                    videoDurationMs = null
                    isVideoSelected = false
                    mediaBrightness = withContext(Dispatchers.IO) { MediaHelper.estimateBrightnessFromDataUri(imgData) }
                    if (mode == 1 && autoSuggestMusicEnabled && !userManuallyTouchedMusic) {
                        selectedTrack = suggestions.firstOrNull()?.track
                    }
                }
            }
            isPreparingMedia = false
        }
    }

    val mediaPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            handlePickedUri(uri)
        }
    }

    ModalBottomSheet(onDismissRequest = {
        previewingTrackId = null
        onDismiss()
    }) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Create on Darling Date",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                listOf("Post", "Reel", "Story").forEachIndexed { idx, label ->
                    SegmentedButton(
                        selected = mode == idx,
                        onClick = {
                            mode = idx
                            if (idx == 1) {
                                autoSuggestMusicEnabled = true
                                if (!userManuallyTouchedMusic) {
                                    selectedTrack = suggestions.firstOrNull()?.track
                                }
                            } else if (idx == 2) {
                                previewingTrackId = null
                                isVideoSelected = false
                                videoUri = null
                            }
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = idx, count = 3)
                    ) {
                        Text(label)
                    }
                }
            }

            if (isPreparingMedia) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(color = DarlingRose, modifier = Modifier.size(28.dp))
                        Text("Processing video & analyzing mood for music...", style = MaterialTheme.typography.bodySmall)
                    }
                }
            } else if (isVideoSelected && !videoUri.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 10f)
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    DarlingReelMediaStage(
                        videoUri = videoUri,
                        fallbackMediaData = mediaData,
                        fallbackText = caption.ifBlank { "Video Clip" },
                        musicTrackId = null, // preview handled separately by previewingTrackId
                        isActive = true,
                        isUserPaused = false,
                        isMuted = muteOriginalAudio,
                        muteOriginalVideoAudio = muteOriginalAudio,
                        modifier = Modifier.matchParentSize()
                    )
                    StatusPill(
                        label = buildString {
                            append("VIDEO")
                            val dur = MediaHelper.formatDuration(videoDurationMs)
                            if (dur.isNotBlank()) append(" • $dur")
                        },
                        textColor = Color.White,
                        backgroundColor = Color.Black.copy(alpha = 0.65f),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp)
                    )
                    IconButton(
                        onClick = {
                            videoUri = null
                            mediaData = null
                            videoDurationMs = null
                            isVideoSelected = false
                        },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f))
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Remove video", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            } else if (mediaData != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 10f)
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    DarlingMediaImage(
                        mediaDataOrUrl = mediaData,
                        contentDescription = "Selected media preview",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize()
                    )
                    IconButton(
                        onClick = { mediaData = null },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f))
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Remove photo", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(104.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable {
                                mediaPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            }
                            .testTag("pick_photo_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, tint = DarlingRose, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Pick Photo", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    if (mode != 2) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(104.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(DarlingViolet.copy(alpha = 0.12f))
                                .clickable {
                                    mediaPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                                }
                                .testTag("pick_video_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Filled.VideoLibrary, contentDescription = null, tint = DarlingViolet, modifier = Modifier.size(28.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Upload Video", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = DarlingViolet)
                            }
                        }
                    }
                }
            }

            OutlinedTextField(
                value = caption,
                onValueChange = { caption = it.take(if (mode == 2) 200 else 2200) },
                label = { Text(if (mode == 2) "Story caption" else "Write a caption...") },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("create_caption_input")
            )

            if (mode != 2) {
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it.take(100) },
                    label = { Text("Location (optional)") },
                    leadingIcon = { Icon(Icons.Filled.LocationOn, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Auto-Suggest Background Music Panel
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("music_suggestion_panel")
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = DarlingRose, modifier = Modifier.size(18.dp))
                                Column {
                                    Text(
                                        text = "Background Music",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (isVideoSelected) "Auto-suggested from your video & vibe" else "Auto-suggested for your post",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            TextButton(
                                onClick = { showMusicBrowser = true },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Filled.LibraryMusic, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Browse")
                            }
                        }

                        // Selected Track Preview Bar
                        selectedTrack?.let { current ->
                            val isPlayingThis = previewingTrackId == current.id
                            val matchReason = suggestions.firstOrNull { it.track.id == current.id }?.reason ?: current.mood
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        IconButton(
                                            onClick = {
                                                previewingTrackId = if (isPlayingThis) null else current.id
                                            },
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(DarlingRose)
                                        ) {
                                            Icon(
                                                imageVector = if (isPlayingThis) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                                contentDescription = if (isPlayingThis) "Pause music" else "Preview music",
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Column {
                                            Text(
                                                text = current.title,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${current.artist} • $matchReason",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = DarlingViolet,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = {
                                            userManuallyTouchedMusic = true
                                            autoSuggestMusicEnabled = false
                                            selectedTrack = null
                                            if (isPlayingThis) previewingTrackId = null
                                        }
                                    ) {
                                        Icon(Icons.Filled.Close, contentDescription = "Remove music", modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }

                        // Quick Auto-Suggested Chips
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(suggestions.take(5), key = { it.track.id }) { suggestion ->
                                val isChosen = selectedTrack?.id == suggestion.track.id
                                FilterChip(
                                    selected = isChosen,
                                    onClick = {
                                        userManuallyTouchedMusic = true
                                        selectedTrack = suggestion.track
                                        previewingTrackId = suggestion.track.id
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = if (isChosen) Icons.Filled.CheckCircle else Icons.Filled.MusicNote,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = suggestion.track.title,
                                            maxLines = 1
                                        )
                                    }
                                )
                            }
                        }

                        if (isVideoSelected && selectedTrack != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Filled.Videocam, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Text("Mute original video audio", style = MaterialTheme.typography.bodySmall)
                                }
                                Switch(
                                    checked = muteOriginalAudio,
                                    onCheckedChange = { muteOriginalAudio = it }
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        previewingTrackId = null
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        if (author == null) return@Button
                        previewingTrackId = null
                        if (mode == 2) {
                            onPublishStory(caption, mediaData)
                        } else {
                            val mType = when {
                                isVideoSelected || mode == 1 -> if (isVideoSelected) "video" else if (!mediaData.isNullOrBlank()) "photo" else "video"
                                !mediaData.isNullOrBlank() -> "photo"
                                else -> "text"
                            }
                            onPublishPostOrReel(
                                caption,
                                mType,
                                mediaData,
                                location,
                                mode == 1,
                                videoUri,
                                videoDurationMs,
                                selectedTrack?.id,
                                selectedTrack?.title,
                                selectedTrack?.artist,
                                muteOriginalAudio
                            )
                        }
                    },
                    enabled = !isBusy && !isPreparingMedia && author != null &&
                        (caption.isNotBlank() || !mediaData.isNullOrBlank() || !videoUri.isNullOrBlank()),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("publish_content_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = DarlingRose)
                ) {
                    if (isBusy) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text(
                            when (mode) {
                                1 -> "Share Reel"
                                2 -> "Add Story"
                                else -> "Share Post"
                            }
                        )
                    }
                }
            }
        }
    }

    if (showMusicBrowser) {
        MusicLibrarySheet(
            suggestions = suggestions,
            selectedTrackId = selectedTrack?.id,
            previewingTrackId = previewingTrackId,
            onTogglePreview = { id ->
                previewingTrackId = if (previewingTrackId == id) null else id
            },
            onSelectTrack = { track ->
                userManuallyTouchedMusic = true
                selectedTrack = track
                previewingTrackId = track?.id
                showMusicBrowser = false
            },
            onDismiss = { showMusicBrowser = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun MusicLibrarySheet(
    suggestions: List<com.example.data.util.SuggestedTrack>,
    selectedTrackId: String?,
    previewingTrackId: String?,
    onTogglePreview: (String) -> Unit,
    onSelectTrack: (MusicTrack?) -> Unit,
    onDismiss: () -> Unit
) {
    var search by remember { mutableStateOf("") }
    var selectedMood by remember { mutableStateOf("All") }

    val filtered = remember(suggestions, search, selectedMood) {
        val q = search.trim().lowercase()
        suggestions.filter { item ->
            val moodOk = selectedMood == "All" || item.track.mood.equals(selectedMood, ignoreCase = true)
            val queryOk = q.isEmpty() ||
                item.track.title.lowercase().contains(q) ||
                item.track.artist.lowercase().contains(q) ||
                item.track.mood.lowercase().contains(q)
            moodOk && queryOk
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Select Background Music", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                TextButton(onClick = { onSelectTrack(null) }) {
                    Icon(Icons.Filled.MusicOff, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("No Music")
                }
            }

            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                placeholder = { Text("Search songs, mood, or artist...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                MusicCatalog.moodFilters.forEach { mood ->
                    FilterChip(
                        selected = selectedMood == mood,
                        onClick = { selectedMood = mood },
                        label = { Text(mood) }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(filtered, key = { it.track.id }) { item ->
                    val isSelected = selectedTrackId == item.track.id
                    val isPreviewing = previewingTrackId == item.track.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) DarlingRose.copy(alpha = 0.1f)
                                else Color.Transparent
                            )
                            .clickable { onSelectTrack(item.track) }
                            .padding(horizontal = 10.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            IconButton(
                                onClick = { onTogglePreview(item.track.id) },
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(if (isPreviewing) DarlingRose else MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Icon(
                                    imageVector = if (isPreviewing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = "Preview ${item.track.title}",
                                    tint = if (isPreviewing) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Column {
                                Text(item.track.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "${item.track.artist} • ${item.track.durationLabel} • ${item.reason}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        if (isSelected) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = "Selected", tint = DarlingRose)
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
