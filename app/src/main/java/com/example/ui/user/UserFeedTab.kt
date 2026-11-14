package com.example.ui.user

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.data.model.Comment
import com.example.data.model.Post
import com.example.data.model.PublicProfile
import com.example.data.model.Story
import com.example.data.model.UiState
import com.example.ui.components.AvatarCircle
import com.example.ui.components.DarlingMediaImage
import com.example.ui.components.ReportDialog
import com.example.ui.theme.DarlingGradientBrush
import com.example.ui.theme.DarlingRose
import com.example.ui.theme.StatusSuccess
import com.example.ui.viewmodel.UserPanelViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserFeedTab(
    currentUserId: String,
    myPublicProfile: PublicProfile?,
    viewModel: UserPanelViewModel,
    onOpenCreatePost: () -> Unit,
    onOpenCreateStory: () -> Unit,
    onOpenEditProfile: () -> Unit
) {
    val postsState by viewModel.postsState.collectAsStateWithLifecycle()
    val storiesState by viewModel.storiesState.collectAsStateWithLifecycle()
    val profilesState by viewModel.profilesState.collectAsStateWithLifecycle()
    val followingIds by viewModel.followingIds.collectAsStateWithLifecycle()
    val likedIds by viewModel.likedPostIds.collectAsStateWithLifecycle()
    val savedIds by viewModel.savedPostIds.collectAsStateWithLifecycle()

    var activeStory by remember { mutableStateOf<Story?>(null) }
    var commentsTargetPost by remember { mutableStateOf<Post?>(null) }
    var reportTarget by remember { mutableStateOf<Triple<String, String, String?>?>(null) } // type, id, authorId

    val stories = (storiesState as? UiState.Success)?.data.orEmpty()
    val allProfiles = (profilesState as? UiState.Success)?.data.orEmpty()
    val suggestedPeople = remember(allProfiles, currentUserId, followingIds) {
        allProfiles.filter { it.userId != currentUserId && !followingIds.contains(it.userId) }.take(12)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("user_home_feed"),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Stories Tray
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(68.dp)
                            .clickable(onClick = onOpenCreateStory)
                            .testTag("add_story_button")
                    ) {
                        Box(modifier = Modifier.size(64.dp)) {
                            AvatarCircle(
                                avatarUrl = myPublicProfile?.avatarUrl,
                                displayName = myPublicProfile?.fullName ?: "You",
                                size = 62.dp
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(DarlingRose)
                                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = "Add story",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Your Story",
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                items(stories, key = { it.id }) { story ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(68.dp)
                            .clickable { activeStory = story }
                    ) {
                        AvatarCircle(
                            avatarUrl = story.authorAvatar,
                            displayName = story.authorName,
                            size = 60.dp,
                            hasStoryRing = true
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = story.authorUsername,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        }

        // Onboarding Checklist for New Users
        if (myPublicProfile != null && (myPublicProfile.bio.isNullOrBlank() || myPublicProfile.postsCount == 0L || followingIds.isEmpty())) {
            item {
                OnboardingChecklistCard(
                    hasAvatar = !myPublicProfile.avatarUrl.isNullOrBlank(),
                    hasBio = !myPublicProfile.bio.isNullOrBlank(),
                    hasPost = myPublicProfile.postsCount > 0L,
                    hasFollowed = followingIds.isNotEmpty(),
                    onSetupProfile = onOpenEditProfile,
                    onCreatePost = onOpenCreatePost
                )
            }
        }

        // Suggested People to Follow
        if (suggestedPeople.isNotEmpty()) {
            item {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Text(
                        text = "Suggested People to Follow",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(suggestedPeople, key = { it.userId }) { person ->
                            SuggestedPersonCard(
                                profile = person,
                                isFollowing = followingIds.contains(person.userId),
                                onFollowToggle = { viewModel.toggleFollow(person.userId) }
                            )
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            }
        }

        // Feed Posts
        when (val state = postsState) {
            is UiState.Loading -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = DarlingRose)
                    }
                }
            }
            is UiState.Error -> {
                item {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(24.dp)
                    )
                }
            }
            is UiState.Success -> {
                val feedPosts = state.data.filter { !it.isReel }
                if (feedPosts.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(36.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Your Darling Date feed is quiet",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Share your first photo or moment to start sparking conversations.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = onOpenCreatePost,
                                colors = ButtonDefaults.buttonColors(containerColor = DarlingRose)
                            ) {
                                Text("Share First Post")
                            }
                        }
                    }
                } else {
                    items(feedPosts, key = { it.id }) { post ->
                        PostFeedCard(
                            post = post,
                            isOwnPost = post.authorId == currentUserId,
                            isLiked = likedIds.contains(post.id),
                            isSaved = savedIds.contains(post.id),
                            onLikeClick = { viewModel.toggleLike(post.id) },
                            onCommentClick = { commentsTargetPost = post },
                            onBookmarkClick = { viewModel.toggleBookmark(post.id) },
                            onDeleteClick = { viewModel.deleteOwnPost(post.id) },
                            onReportClick = { reportTarget = Triple("post", post.id, post.authorId) }
                        )
                    }
                }
            }
        }
    }

    // Story Viewer Dialog
    activeStory?.let { story ->
        Dialog(
            onDismissRequest = { activeStory = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                if (!story.mediaData.isNullOrBlank()) {
                    DarlingMediaImage(
                        mediaDataOrUrl = story.mediaData,
                        contentDescription = "Story by ${story.authorUsername}",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(DarlingGradientBrush),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = story.caption,
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White,
                            modifier = Modifier.padding(32.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .align(Alignment.TopCenter),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AvatarCircle(
                            avatarUrl = story.authorAvatar,
                            displayName = story.authorName,
                            size = 38.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "@${story.authorUsername}",
                                color = Color.White,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = story.authorName,
                                color = Color.White.copy(alpha = 0.75f),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                    Row {
                        if (story.authorId != currentUserId) {
                            IconButton(
                                onClick = {
                                    reportTarget = Triple("story", story.id, story.authorId)
                                    activeStory = null
                                }
                            ) {
                                Icon(Icons.Filled.Flag, contentDescription = "Report story", tint = Color.White)
                            }
                        }
                        IconButton(onClick = { activeStory = null }) {
                            Icon(Icons.Filled.Close, contentDescription = "Close story", tint = Color.White)
                        }
                    }
                }

                if (story.caption.isNotBlank() && !story.mediaData.isNullOrBlank()) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(24.dp)
                    ) {
                        Text(
                            text = story.caption,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                        )
                    }
                }
            }
        }
    }

    // Comments Bottom Sheet
    commentsTargetPost?.let { targetPost ->
        val comments by viewModel.observeComments(targetPost.id)
            .collectAsStateWithLifecycle(initialValue = emptyList())
        var commentText by remember { mutableStateOf("") }

        ModalBottomSheet(onDismissRequest = { commentsTargetPost = null }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Comments (${comments.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))
                if (comments.isEmpty()) {
                    Text(
                        text = "No comments yet. Be the first to reply!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(comments, key = { it.id }) { comment ->
                            CommentRow(
                                comment = comment,
                                onReport = {
                                    reportTarget = Triple("comment", comment.id, comment.authorId)
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { commentText = it.take(500) },
                        placeholder = { Text("Add a kind comment...") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            if (commentText.isNotBlank() && myPublicProfile != null) {
                                viewModel.addComment(targetPost.id, myPublicProfile, commentText)
                                commentText = ""
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send comment",
                            tint = DarlingRose
                        )
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
                viewModel.submitReport(
                    targetType = type,
                    targetId = id,
                    targetParentId = if (type == "comment") commentsTargetPost?.id else null,
                    targetAuthorId = authorId,
                    reason = reason,
                    details = details
                )
                reportTarget = null
            }
        )
    }
}

@Composable
private fun OnboardingChecklistCard(
    hasAvatar: Boolean,
    hasBio: Boolean,
    hasPost: Boolean,
    hasFollowed: Boolean,
    onSetupProfile: () -> Unit,
    onCreatePost: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Welcome! Finish setting up Darling Date",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            ChecklistItem("Add a profile picture", hasAvatar)
            ChecklistItem("Write a short dating bio", hasBio)
            ChecklistItem("Follow someone you vibe with", hasFollowed)
            ChecklistItem("Share your first post", hasPost)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!hasAvatar || !hasBio) {
                    OutlinedButton(onClick = onSetupProfile) {
                        Text("Complete Profile")
                    }
                }
                if (!hasPost) {
                    Button(
                        onClick = onCreatePost,
                        colors = ButtonDefaults.buttonColors(containerColor = DarlingRose)
                    ) {
                        Text("Create Post")
                    }
                }
            }
        }
    }
}

@Composable
private fun ChecklistItem(label: String, done: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(
            imageVector = if (done) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (done) StatusSuccess else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = if (done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SuggestedPersonCard(
    profile: PublicProfile,
    isFollowing: Boolean,
    onFollowToggle: () -> Unit
) {
    Card(
        modifier = Modifier.width(156.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            AvatarCircle(
                avatarUrl = profile.avatarUrl,
                displayName = profile.fullName,
                size = 56.dp,
                isVerified = profile.isVerified
            )
            Text(
                text = profile.fullName,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "@${profile.username}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Button(
                onClick = onFollowToggle,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp),
                contentPadding = PaddingValues(horizontal = 8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isFollowing) MaterialTheme.colorScheme.surface else DarlingRose,
                    contentColor = if (isFollowing) MaterialTheme.colorScheme.onSurface else Color.White
                )
            ) {
                Text(
                    text = if (isFollowing) "Following" else "Follow",
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
fun PostFeedCard(
    post: Post,
    isOwnPost: Boolean,
    isLiked: Boolean,
    isSaved: Boolean,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onReportClick: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val dateText = remember(post.createdAt) {
        post.createdAt?.toDate()?.let {
            SimpleDateFormat("MMM d, h:mm a", Locale.US).format(it)
        } ?: "Just now"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AvatarCircle(
                    avatarUrl = post.authorAvatar,
                    displayName = post.authorName,
                    size = 40.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "@${post.authorUsername}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    val subtitle = listOfNotNull(post.location?.takeIf { it.isNotBlank() }, dateText).joinToString(" • ")
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Post options")
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    if (isOwnPost) {
                        DropdownMenuItem(
                            text = { Text("Delete Post") },
                            leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onDeleteClick()
                            }
                        )
                    } else {
                        DropdownMenuItem(
                            text = { Text("Report Post") },
                            leadingIcon = { Icon(Icons.Filled.Flag, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onReportClick()
                            }
                        )
                    }
                }
            }
        }

        if (!post.mediaData.isNullOrBlank()) {
            DarlingMediaImage(
                mediaDataOrUrl = post.mediaData,
                contentDescription = post.caption,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(DarlingGradientBrush)
                    .padding(24.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = post.caption,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onLikeClick) {
                    Icon(
                        imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (isLiked) DarlingRose else MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "${post.likesCount}",
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = onCommentClick) {
                    Icon(
                        imageVector = Icons.Outlined.ChatBubbleOutline,
                        contentDescription = "Comments"
                    )
                }
                Text(
                    text = "${post.commentsCount}",
                    style = MaterialTheme.typography.labelLarge
                )
            }
            IconButton(onClick = onBookmarkClick) {
                Icon(
                    imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = "Save post",
                    tint = if (isSaved) DarlingRose else MaterialTheme.colorScheme.onSurface
                )
            }
        }

        if (post.caption.isNotBlank() && !post.mediaData.isNullOrBlank()) {
            Text(
                text = "@${post.authorUsername}  ${post.caption}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp)
            )
        }

        TextButton(
            onClick = onCommentClick,
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp)
        ) {
            Text(
                text = "View all ${post.commentsCount} comments",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    }
}

@Composable
private fun CommentRow(
    comment: Comment,
    onReport: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AvatarCircle(
                avatarUrl = comment.authorAvatar,
                displayName = comment.authorUsername,
                size = 32.dp
            )
            Column {
                Text(
                    text = "@${comment.authorUsername}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = comment.text,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        IconButton(onClick = onReport) {
            Icon(
                imageVector = Icons.Filled.Flag,
                contentDescription = "Report comment",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
