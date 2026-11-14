package com.example.ui.user

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Post
import com.example.data.model.PublicProfile
import com.example.data.model.UiState
import com.example.ui.components.AvatarCircle
import com.example.ui.components.DarlingMediaImage
import com.example.ui.components.ReportDialog
import com.example.ui.theme.DarlingCoral
import com.example.ui.theme.DarlingRose
import com.example.ui.theme.DarlingViolet
import com.example.ui.theme.StatusDanger
import com.example.ui.viewmodel.UserPanelViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UserDiscoverReelsTab(
    currentUserId: String,
    viewModel: UserPanelViewModel,
    onMessageMatch: (PublicProfile) -> Unit,
    onOpenReels: () -> Unit = {},
    onVisitUser: (String) -> Unit
) {
    var selectedSegment by remember { mutableIntStateOf(0) } // 0 = Dating, 1 = Reels
    val profilesState by viewModel.profilesState.collectAsStateWithLifecycle()
    val swipes by viewModel.swipes.collectAsStateWithLifecycle()
    val postsState by viewModel.postsState.collectAsStateWithLifecycle()
    val likedIds by viewModel.likedPostIds.collectAsStateWithLifecycle()
    val mutualMatch by viewModel.mutualMatchProfile.collectAsStateWithLifecycle()

    var reportTarget by remember { mutableStateOf<Triple<String, String, String?>?>(null) }

    val swipedIds = remember(swipes) { swipes.map { it.toUserId }.toSet() }
    val allProfiles = (profilesState as? UiState.Success)?.data.orEmpty()
    val discoverDeck = remember(allProfiles, swipedIds, currentUserId) {
        allProfiles.filter { it.userId != currentUserId && !swipedIds.contains(it.userId) }
    }
    val reels = remember(postsState) {
        (postsState as? UiState.Success)?.data.orEmpty().filter { it.isReel }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("user_discover_reels_tab")
    ) {
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            SegmentedButton(
                selected = selectedSegment == 0,
                onClick = { selectedSegment = 0 },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
            ) {
                Text("Dating Sparks")
            }
            SegmentedButton(
                selected = selectedSegment == 1,
                onClick = { selectedSegment = 1 },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
            ) {
                Text("Reels (${reels.size})")
            }
        }

        if (selectedSegment == 0) {
            if (discoverDeck.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = DarlingRose,
                            modifier = Modifier.size(56.dp)
                        )
                        Text(
                            text = "You've seen everyone nearby!",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "New people join Darling Date every day. Check your Matches & Messages or watch Reels.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        ElevatedButton(onClick = onOpenReels) {
                            Icon(Icons.Filled.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Watch Reels")
                        }
                    }
                }
            } else {
                val topProfile = discoverDeck.first()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        shape = RoundedCornerShape(28.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            DarlingMediaImage(
                                mediaDataOrUrl = topProfile.avatarUrl,
                                contentDescription = topProfile.fullName,
                                fallbackText = topProfile.fullName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                Color.Black.copy(alpha = 0.25f),
                                                Color.Black.copy(alpha = 0.85f)
                                            )
                                        )
                                    )
                            )
                            Surface(
                                onClick = { onVisitUser(topProfile.userId) },
                                color = Color.Black.copy(alpha = 0.55f),
                                shape = RoundedCornerShape(50),
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(14.dp)
                                    .testTag("discover_view_profile_button")
                            ) {
                                Text(
                                    text = "View Full Profile",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                )
                            }

                            IconButton(
                                onClick = { reportTarget = Triple("user", topProfile.userId, topProfile.userId) },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(12.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.45f))
                            ) {
                                Icon(Icons.Filled.Flag, contentDescription = "Report user", tint = Color.White)
                            }

                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = buildString {
                                            append(topProfile.fullName)
                                            topProfile.ageGroup?.let { append(", $it") }
                                        },
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "@${topProfile.username}" + (topProfile.lookingFor?.let { " • Looking for $it" } ?: ""),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                                if (!topProfile.bio.isNullOrBlank()) {
                                    Text(
                                        text = topProfile.bio,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White.copy(alpha = 0.92f)
                                    )
                                }
                                if (topProfile.interests.isNotEmpty()) {
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        topProfile.interests.take(5).forEach { tag ->
                                            Surface(
                                                color = Color.White.copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(50)
                                            ) {
                                                Text(
                                                    text = tag,
                                                    color = Color.White,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalIconButton(
                            onClick = { viewModel.swipeProfile(topProfile, "pass") },
                            modifier = Modifier
                                .size(60.dp)
                                .testTag("swipe_pass_button"),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "Pass", tint = StatusDanger, modifier = Modifier.size(28.dp))
                        }

                        FilledTonalIconButton(
                            onClick = { viewModel.swipeProfile(topProfile, "super_spark") },
                            modifier = Modifier
                                .size(56.dp)
                                .testTag("swipe_spark_button"),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = DarlingViolet.copy(alpha = 0.16f)
                            )
                        ) {
                            Icon(Icons.Filled.Star, contentDescription = "Super Spark", tint = DarlingViolet, modifier = Modifier.size(26.dp))
                        }

                        FilledTonalIconButton(
                            onClick = { viewModel.swipeProfile(topProfile, "like") },
                            modifier = Modifier
                                .size(64.dp)
                                .testTag("swipe_like_button"),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = DarlingRose
                            )
                        ) {
                            Icon(Icons.Filled.Favorite, contentDescription = "Like", tint = Color.White, modifier = Modifier.size(30.dp))
                        }
                    }
                }
            }
        } else {
            if (reels.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No reels yet. Tap + in the top bar to publish a Reel!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(reels, key = { it.id }) { reel ->
                        ReelShowcaseCard(
                            reel = reel,
                            isLiked = likedIds.contains(reel.id),
                            onAuthorClick = { onVisitUser(reel.authorId) },
                            onLike = { viewModel.toggleLike(reel.id) },
                            onReport = { reportTarget = Triple("reel", reel.id, reel.authorId) }
                        )
                    }
                }
            }
        }
    }

    mutualMatch?.let { matchedProfile ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissMatchPopup() },
            icon = { Icon(Icons.Filled.Favorite, contentDescription = null, tint = DarlingRose, modifier = Modifier.size(40.dp)) },
            title = { Text("It's a Darling Match!", fontWeight = FontWeight.Bold) },
            text = {
                Text("You and ${matchedProfile.fullName} (@${matchedProfile.username}) liked each other. Say hello now!")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.dismissMatchPopup()
                        onMessageMatch(matchedProfile)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DarlingRose)
                ) {
                    Text("Send a Message")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissMatchPopup() }) {
                    Text("Keep Exploring")
                }
            }
        )
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

@Composable
private fun ReelShowcaseCard(
    reel: Post,
    isLiked: Boolean,
    onAuthorClick: () -> Unit,
    onLike: () -> Unit,
    onReport: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(9f / 11f)
        ) {
            DarlingMediaImage(
                mediaDataOrUrl = reel.mediaData,
                contentDescription = reel.caption,
                fallbackText = reel.authorUsername,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.78f))
                        )
                    )
            )
            Surface(
                color = Color.Black.copy(alpha = 0.55f),
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Filled.PlayCircleFilled, contentDescription = null, tint = DarlingCoral, modifier = Modifier.size(16.dp))
                    Text("REEL", color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
                    .fillMaxWidth(0.8f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.2f))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    AvatarCircle(
                        avatarUrl = reel.authorAvatar,
                        displayName = reel.authorName,
                        size = 34.dp,
                        onClick = onAuthorClick
                    )
                    TextButton(
                        onClick = onAuthorClick,
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                    ) {
                        Text("@${reel.authorUsername}", color = Color.White, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                }
                Text(reel.caption, color = Color.White, style = MaterialTheme.typography.bodyMedium)
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                IconButton(onClick = onLike) {
                    Icon(
                        Icons.Filled.Favorite,
                        contentDescription = "Like reel",
                        tint = if (isLiked) DarlingRose else Color.White
                    )
                }
                Text("${reel.likesCount}", color = Color.White, style = MaterialTheme.typography.labelSmall)
                IconButton(onClick = onReport) {
                    Icon(Icons.Filled.Flag, contentDescription = "Report reel", tint = Color.White)
                }
            }
        }
    }
}
