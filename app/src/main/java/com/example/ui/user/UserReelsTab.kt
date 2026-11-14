package com.example.ui.user

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Post
import com.example.data.model.PublicProfile
import com.example.data.model.UiState
import com.example.data.util.MediaHelper
import com.example.ui.components.AvatarCircle
import com.example.ui.components.DarlingReelMediaStage
import com.example.ui.components.MusicTrackPill
import com.example.ui.components.ReportDialog
import com.example.ui.components.StatusPill
import com.example.ui.theme.DarlingRose
import com.example.ui.theme.DarlingViolet
import com.example.ui.viewmodel.UserPanelViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserReelsTab(
    currentUserId: String,
    myPublicProfile: PublicProfile?,
    viewModel: UserPanelViewModel,
    onOpenCreateReel: () -> Unit,
    onVisitUser: (String) -> Unit
) {
    val postsState by viewModel.postsState.collectAsStateWithLifecycle()
    val likedIds by viewModel.likedPostIds.collectAsStateWithLifecycle()
    val savedIds by viewModel.savedPostIds.collectAsStateWithLifecycle()
    val followingIds by viewModel.followingIds.collectAsStateWithLifecycle()

    var isMuted by remember { mutableStateOf(false) }
    var pausedReelId by remember { mutableStateOf<String?>(null) }
    var commentsTargetReel by remember { mutableStateOf<Post?>(null) }
    var reportTarget by remember { mutableStateOf<Triple<String, String, String?>?>(null) }

    val reels = remember(postsState) {
        (postsState as? UiState.Success)?.data.orEmpty().filter { it.isReel }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("user_reels_tab")
    ) {
        if (reels.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayCircleFilled,
                    contentDescription = null,
                    tint = DarlingRose,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "No Reels Published Yet",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Upload your first video or photo reel and add auto-suggested background music.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.78f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(18.dp))
                Button(
                    onClick = onOpenCreateReel,
                    colors = ButtonDefaults.buttonColors(containerColor = DarlingRose),
                    modifier = Modifier.testTag("empty_create_reel_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Upload Reel")
                }
            }
        } else {
            val pagerState = rememberPagerState(pageCount = { reels.size })

            VerticalPager(
                state = pagerState,
                beyondViewportPageCount = 1,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val reel = reels[page]
                val isCurrentPage = pagerState.settledPage == page
                val isUserPaused = pausedReelId == reel.id
                val isLiked = likedIds.contains(reel.id)
                val isSaved = savedIds.contains(reel.id)
                val isFollowing = followingIds.contains(reel.authorId)

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable {
                            pausedReelId = if (isUserPaused) null else reel.id
                        }
                ) {
                    DarlingReelMediaStage(
                        videoUri = reel.videoUri,
                        fallbackMediaData = reel.mediaData,
                        fallbackText = reel.caption.ifBlank { "@${reel.authorUsername}" },
                        musicTrackId = reel.musicTrackId,
                        isActive = isCurrentPage,
                        isUserPaused = isUserPaused,
                        isMuted = isMuted,
                        muteOriginalVideoAudio = reel.muteOriginalAudio,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Subtle top & bottom readability gradient
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.35f),
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.85f)
                                    )
                                )
                            )
                    )

                    // Top bar overlay inside Reel
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatusPill(
                                label = buildString {
                                    append("Reels")
                                    val dur = MediaHelper.formatDuration(reel.videoDurationMs)
                                    if (dur.isNotBlank()) append(" • $dur")
                                },
                                textColor = Color.White,
                                backgroundColor = Color.Black.copy(alpha = 0.45f)
                            )
                        }
                        Surface(
                            onClick = onOpenCreateReel,
                            color = DarlingRose,
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.testTag("reels_tab_create_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Text("Create Reel", color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Bottom left author, caption, & background music pill
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 16.dp, end = 82.dp, bottom = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onVisitUser(reel.authorId) }
                                    .padding(vertical = 2.dp)
                            ) {
                                AvatarCircle(
                                    avatarUrl = reel.authorAvatar,
                                    displayName = reel.authorName,
                                    size = 38.dp
                                )
                                Text(
                                    text = "@${reel.authorUsername}",
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (reel.authorId != currentUserId) {
                                Surface(
                                    onClick = { viewModel.toggleFollow(reel.authorId) },
                                    color = if (isFollowing) Color.White.copy(alpha = 0.2f) else DarlingRose,
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Text(
                                        text = if (isFollowing) "Following" else "Follow",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        if (reel.caption.isNotBlank()) {
                            Text(
                                text = reel.caption,
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (!reel.musicTitle.isNullOrBlank()) {
                            MusicTrackPill(
                                title = reel.musicTitle,
                                artist = reel.musicArtist ?: "Original Sound",
                                isPlaying = isCurrentPage && !isUserPaused && !isMuted
                            )
                        }
                    }

                    // Right side action column
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 12.dp, bottom = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(
                                onClick = { viewModel.toggleLike(reel.id) },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.35f))
                            ) {
                                Icon(
                                    imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                    contentDescription = "Like reel",
                                    tint = if (isLiked) DarlingRose else Color.White
                                )
                            }
                            Text("${reel.likesCount}", color = Color.White, style = MaterialTheme.typography.labelSmall)
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(
                                onClick = { commentsTargetReel = reel },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.35f))
                            ) {
                                Icon(Icons.Filled.ChatBubble, contentDescription = "Comments", tint = Color.White)
                            }
                            Text("${reel.commentsCount}", color = Color.White, style = MaterialTheme.typography.labelSmall)
                        }

                        IconButton(
                            onClick = { viewModel.toggleBookmark(reel.id) },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.35f))
                        ) {
                            Icon(
                                imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = "Save reel",
                                tint = if (isSaved) DarlingRose else Color.White
                            )
                        }

                        IconButton(
                            onClick = { isMuted = !isMuted },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.35f))
                        ) {
                            Icon(
                                imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = if (isMuted) "Unmute" else "Mute",
                                tint = Color.White
                            )
                        }

                        if (reel.authorId != currentUserId) {
                            IconButton(
                                onClick = { reportTarget = Triple("reel", reel.id, reel.authorId) },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.35f))
                            ) {
                                Icon(Icons.Filled.Flag, contentDescription = "Report reel", tint = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }

    commentsTargetReel?.let { targetReel ->
        val comments by viewModel.observeComments(targetReel.id)
            .collectAsStateWithLifecycle(initialValue = emptyList())
        var commentText by remember { mutableStateOf("") }

        ModalBottomSheet(onDismissRequest = { commentsTargetReel = null }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("Comments (${comments.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(comments, key = { it.id }) { comment ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    commentsTargetReel = null
                                    onVisitUser(comment.authorId)
                                },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AvatarCircle(avatarUrl = comment.authorAvatar, displayName = comment.authorUsername, size = 32.dp)
                            Column(modifier = Modifier.weight(1f)) {
                                Text("@${comment.authorUsername}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                Text(comment.text, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { commentText = it.take(500) },
                        placeholder = { Text("Add a comment...") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            val author = myPublicProfile ?: return@IconButton
                            if (commentText.isNotBlank()) {
                                viewModel.addComment(targetReel.id, author, commentText)
                                commentText = ""
                            }
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send comment", tint = DarlingRose)
                    }
                }
            }
        }
    }

    reportTarget?.let { (type, id, authorId) ->
        ReportDialog(
            targetLabel = type.replaceFirstChar { it.uppercase() },
            onDismiss = { reportTarget = null },
            onSubmit = { reason, details ->
                viewModel.submitReport(type, id, null, authorId, reason, details)
                reportTarget = null
            }
        )
    }
}
