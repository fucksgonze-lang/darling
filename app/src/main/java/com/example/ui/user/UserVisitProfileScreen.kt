package com.example.ui.user

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Post
import com.example.data.model.PublicProfile
import com.example.data.model.UiState
import com.example.ui.components.AvatarCircle
import com.example.ui.components.DarlingMediaImage
import com.example.ui.components.ReportDialog
import com.example.ui.components.StatusPill
import com.example.ui.theme.DarlingGradientBrush
import com.example.ui.theme.DarlingRose
import com.example.ui.theme.DarlingViolet
import com.example.ui.viewmodel.UserPanelViewModel

enum class FollowListMode {
    FOLLOWERS,
    FOLLOWING
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun VisitedUserProfileOverlay(
    currentUserId: String,
    targetUserId: String,
    myPublicProfile: PublicProfile,
    viewModel: UserPanelViewModel,
    onBack: () -> Unit,
    onMessageUser: (PublicProfile) -> Unit,
    onVisitAnotherUser: (String) -> Unit
) {
    BackHandler(onBack = onBack)

    val profilesState by viewModel.profilesState.collectAsStateWithLifecycle()
    val postsState by viewModel.postsState.collectAsStateWithLifecycle()
    val myFollowingIds by viewModel.followingIds.collectAsStateWithLifecycle()
    val likedIds by viewModel.likedPostIds.collectAsStateWithLifecycle()
    val savedIds by viewModel.savedPostIds.collectAsStateWithLifecycle()

    val targetFollowerIds by viewModel.observeUserFollowerIds(targetUserId)
        .collectAsStateWithLifecycle(initialValue = emptySet())
    val targetFollowingIds by viewModel.observeUserFollowingIds(targetUserId)
        .collectAsStateWithLifecycle(initialValue = emptySet())

    val allProfiles = (profilesState as? UiState.Success)?.data.orEmpty()
    val profile = remember(allProfiles, targetUserId) {
        allProfiles.firstOrNull { it.userId == targetUserId }
    }

    val allPosts = (postsState as? UiState.Success)?.data.orEmpty()
    val userPosts = remember(allPosts, targetUserId) {
        allPosts.filter { it.authorId == targetUserId && !it.isReel }
    }
    val userReels = remember(allPosts, targetUserId) {
        allPosts.filter { it.authorId == targetUserId && it.isReel }
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    var followListMode by remember { mutableStateOf<FollowListMode?>(null) }
    var selectedPost by remember { mutableStateOf<Post?>(null) }
    var commentsTargetPost by remember { mutableStateOf<Post?>(null) }
    var reportTarget by remember { mutableStateOf<Triple<String, String, String?>?>(null) }

    val isSelf = targetUserId == currentUserId
    val isFollowingTarget = myFollowingIds.contains(targetUserId)
    val displayedPosts = if (selectedTab == 0) userPosts else userReels

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        modifier = Modifier
            .fillMaxSize()
            .testTag("visited_user_profile_screen"),
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("visited_profile_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = profile?.let { "@${it.username}" } ?: "Profile",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (profile?.isVerified == true) {
                            Icon(
                                imageVector = Icons.Filled.Verified,
                                contentDescription = "Verified",
                                tint = DarlingViolet,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                actions = {
                    if (!isSelf && profile != null) {
                        IconButton(onClick = { reportTarget = Triple("user", profile.userId, profile.userId) }) {
                            Icon(Icons.Filled.Flag, contentDescription = "Report user")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        if (profile == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Profile unavailable", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "This account may have been suspended or removed.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(onClick = onBack) { Text("Go Back") }
                }
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 28.dp)
        ) {
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        AvatarCircle(
                            avatarUrl = profile.avatarUrl,
                            displayName = profile.fullName,
                            size = 84.dp,
                            isVerified = profile.isVerified
                        )
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            ClickableProfileStat("${userPosts.size + userReels.size}", "Posts")
                            ClickableProfileStat(
                                count = "${maxOf(profile.followersCount.toInt(), targetFollowerIds.size)}",
                                label = "Followers",
                                testTag = "visited_followers_stat",
                                onClick = { followListMode = FollowListMode.FOLLOWERS }
                            )
                            ClickableProfileStat(
                                count = "${maxOf(profile.followingCount.toInt(), targetFollowingIds.size)}",
                                label = "Following",
                                testTag = "visited_following_stat",
                                onClick = { followListMode = FollowListMode.FOLLOWING }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = buildString {
                            append(profile.fullName)
                            profile.ageGroup?.let { append(", $it") }
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "@${profile.username}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val metaParts = listOfNotNull(
                        profile.gender?.takeIf { it.isNotBlank() },
                        profile.lookingFor?.takeIf { it.isNotBlank() }?.let { "Looking for $it" }
                    )
                    if (metaParts.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        StatusPill(
                            label = metaParts.joinToString(" • "),
                            textColor = DarlingViolet,
                            backgroundColor = DarlingViolet.copy(alpha = 0.12f)
                        )
                    }

                    if (!profile.bio.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = profile.bio, style = MaterialTheme.typography.bodyMedium)
                    }

                    if (profile.interests.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            profile.interests.forEach { tag ->
                                AssistChip(onClick = {}, label = { Text(tag) })
                            }
                        }
                    }

                    if (!isSelf) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { viewModel.toggleFollow(profile.userId) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("visited_follow_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isFollowingTarget) MaterialTheme.colorScheme.surfaceVariant else DarlingRose,
                                    contentColor = if (isFollowingTarget) MaterialTheme.colorScheme.onSurface else Color.White
                                )
                            ) {
                                Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isFollowingTarget) "Following" else "Follow")
                            }
                            OutlinedButton(
                                onClick = { onMessageUser(profile) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("visited_message_button")
                            ) {
                                Icon(Icons.Filled.ChatBubble, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Message")
                            }
                        }
                    }
                }
            }

            item {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Posts (${userPosts.size})") })
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Reels (${userReels.size})") })
                }
            }

            if (displayedPosts.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (selectedTab == 0) "No posts shared yet." else "No reels shared yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(displayedPosts.chunked(3)) { rowPosts ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 2.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        rowPosts.forEach { post ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clickable { selectedPost = post }
                            ) {
                                if (!post.mediaData.isNullOrBlank()) {
                                    DarlingMediaImage(
                                        mediaDataOrUrl = post.mediaData,
                                        contentDescription = post.caption,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(DarlingGradientBrush)
                                            .padding(8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = post.caption,
                                            color = Color.White,
                                            style = MaterialTheme.typography.labelSmall,
                                            maxLines = 4
                                        )
                                    }
                                }
                            }
                        }
                        repeat(3 - rowPosts.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }

    followListMode?.let { mode ->
        FollowListBottomSheet(
            title = if (mode == FollowListMode.FOLLOWERS) "Followers" else "Following",
            userIds = if (mode == FollowListMode.FOLLOWERS) targetFollowerIds else targetFollowingIds,
            currentUserId = currentUserId,
            myFollowingIds = myFollowingIds,
            allProfiles = allProfiles,
            onDismiss = { followListMode = null },
            onFollowToggle = { viewModel.toggleFollow(it) },
            onVisitUser = { uid ->
                followListMode = null
                if (uid != targetUserId) {
                    onVisitAnotherUser(uid)
                }
            }
        )
    }

    selectedPost?.let { post ->
        val freshPost = allPosts.firstOrNull { it.id == post.id } ?: post
        PostDetailDialog(
            post = freshPost,
            isOwnPost = freshPost.authorId == currentUserId,
            isLiked = likedIds.contains(freshPost.id),
            isSaved = savedIds.contains(freshPost.id),
            onDismiss = { selectedPost = null },
            onLikeClick = { viewModel.toggleLike(freshPost.id) },
            onCommentClick = { commentsTargetPost = freshPost },
            onBookmarkClick = { viewModel.toggleBookmark(freshPost.id) },
            onDeleteClick = {
                viewModel.deleteOwnPost(freshPost.id)
                selectedPost = null
            },
            onReportClick = {
                reportTarget = Triple(if (freshPost.isReel) "reel" else "post", freshPost.id, freshPost.authorId)
            }
        )
    }

    commentsTargetPost?.let { targetPost ->
        val comments by viewModel.observeComments(targetPost.id)
            .collectAsStateWithLifecycle(initialValue = emptyList())
        var commentText by remember { mutableStateOf("") }

        ModalBottomSheet(onDismissRequest = { commentsTargetPost = null }) {
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
                                    commentsTargetPost = null
                                    if (comment.authorId != targetUserId) onVisitAnotherUser(comment.authorId)
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
                            if (commentText.isNotBlank()) {
                                viewModel.addComment(targetPost.id, myPublicProfile, commentText)
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

@Composable
fun ClickableProfileStat(
    count: String,
    label: String,
    testTag: String? = null,
    onClick: (() -> Unit)? = null
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .let { mod -> if (onClick != null) mod.clickable(onClick = onClick) else mod }
            .let { mod -> if (testTag != null) mod.testTag(testTag) else mod }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(count, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (onClick != null) DarlingRose else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (onClick != null) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowListBottomSheet(
    title: String,
    userIds: Set<String>,
    currentUserId: String,
    myFollowingIds: Set<String>,
    allProfiles: List<PublicProfile>,
    onDismiss: () -> Unit,
    onFollowToggle: (String) -> Unit,
    onVisitUser: (String) -> Unit
) {
    val profilesById = remember(allProfiles) { allProfiles.associateBy { it.userId } }
    val resolvedUsers = remember(userIds, profilesById) {
        userIds.mapNotNull { profilesById[it] }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("follow_list_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "$title (${resolvedUsers.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))
            if (resolvedUsers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (title == "Followers") "No followers yet." else "Not following anyone yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(resolvedUsers, key = { it.userId }) { person ->
                        val isMe = person.userId == currentUserId
                        val isFollowing = myFollowingIds.contains(person.userId)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onVisitUser(person.userId) }
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                AvatarCircle(
                                    avatarUrl = person.avatarUrl,
                                    displayName = person.fullName,
                                    size = 44.dp,
                                    isVerified = person.isVerified
                                )
                                Column {
                                    Text(
                                        text = person.fullName,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "@${person.username}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (!isMe) {
                                Button(
                                    onClick = { onFollowToggle(person.userId) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isFollowing) MaterialTheme.colorScheme.surfaceVariant else DarlingRose,
                                        contentColor = if (isFollowing) MaterialTheme.colorScheme.onSurface else Color.White
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(if (isFollowing) "Following" else "Follow", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreMembersSheet(
    currentUserId: String,
    allProfiles: List<PublicProfile>,
    myFollowingIds: Set<String>,
    onDismiss: () -> Unit,
    onFollowToggle: (String) -> Unit,
    onVisitUser: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(allProfiles, query, currentUserId) {
        val clean = query.trim().lowercase()
        allProfiles
            .filter { it.userId != currentUserId }
            .filter {
                clean.isEmpty() ||
                    it.fullName.lowercase().contains(clean) ||
                    it.username.lowercase().contains(clean) ||
                    it.interests.any { tag -> tag.lowercase().contains(clean) }
            }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("explore_members_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Find & Visit Profiles",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search by name, @username, or interest...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("explore_members_search")
            )
            Spacer(modifier = Modifier.height(10.dp))
            if (filtered.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No matching members found.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filtered, key = { it.userId }) { person ->
                        val isFollowing = myFollowingIds.contains(person.userId)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onVisitUser(person.userId) }
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                AvatarCircle(
                                    avatarUrl = person.avatarUrl,
                                    displayName = person.fullName,
                                    size = 46.dp,
                                    isVerified = person.isVerified
                                )
                                Column {
                                    Text(
                                        text = person.fullName,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "@${person.username} • ${person.followersCount} followers",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Button(
                                onClick = { onFollowToggle(person.userId) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isFollowing) MaterialTheme.colorScheme.surfaceVariant else DarlingRose,
                                    contentColor = if (isFollowing) MaterialTheme.colorScheme.onSurface else Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(if (isFollowing) "Following" else "Follow", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun PostDetailDialog(
    post: Post,
    isOwnPost: Boolean,
    isLiked: Boolean,
    isSaved: Boolean,
    onDismiss: () -> Unit,
    onAuthorClick: () -> Unit = {},
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onReportClick: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (post.isReel) "Reel" else "Post",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }
                PostFeedCard(
                    post = post,
                    isOwnPost = isOwnPost,
                    isLiked = isLiked,
                    isSaved = isSaved,
                    onAuthorClick = onAuthorClick,
                    onLikeClick = onLikeClick,
                    onCommentClick = onCommentClick,
                    onBookmarkClick = onBookmarkClick,
                    onDeleteClick = onDeleteClick,
                    onReportClick = onReportClick
                )
            }
        }
    }
}
