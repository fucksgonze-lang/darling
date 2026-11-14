package com.example.ui.admin

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ContentStatuses
import com.example.data.model.Report
import com.example.data.model.ReportStatuses
import com.example.ui.components.DarlingMediaImage
import com.example.ui.components.StatusPill
import com.example.ui.theme.AdminIndigo
import com.example.ui.theme.DarlingSoftLavender
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusDangerContainer
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusSuccessContainer
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.StatusWarningContainer
import com.example.ui.viewmodel.AdminPanelViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminContentTab(viewModel: AdminPanelViewModel) {
    val posts by viewModel.posts.collectAsStateWithLifecycle()
    val stories by viewModel.stories.collectAsStateWithLifecycle()
    val comments by viewModel.comments.collectAsStateWithLifecycle()
    val reports by viewModel.reports.collectAsStateWithLifecycle()

    var filter by remember { mutableStateOf("posts") } // posts, photos, videos, stories, comments, reported
    val reportedIds = remember(reports) { reports.map { it.targetId }.toSet() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("admin_content_tab")
    ) {
        FlowRow(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf(
                "posts" to "All Posts",
                "photos" to "Photos",
                "videos" to "Videos/Reels",
                "stories" to "Stories",
                "comments" to "Comments",
                "reported" to "Reported"
            ).forEach { (key, label) ->
                FilterChip(
                    selected = filter == key,
                    onClick = { filter = key },
                    label = { Text(label) }
                )
            }
        }

        LazyColumnContentList(
            filter = filter,
            posts = posts,
            stories = stories,
            comments = comments,
            reportedIds = reportedIds,
            viewModel = viewModel
        )
    }
}

@Composable
private fun LazyColumnContentList(
    filter: String,
    posts: List<com.example.data.model.Post>,
    stories: List<com.example.data.model.Story>,
    comments: List<com.example.data.model.Comment>,
    reportedIds: Set<String>,
    viewModel: AdminPanelViewModel
) {
    androidx.compose.foundation.lazy.LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        when (filter) {
            "stories" -> {
                items(stories.size, key = { stories[it].id }) { idx ->
                    val story = stories[idx]
                    Card(shape = RoundedCornerShape(16.dp)) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Story by @${story.authorUsername}", fontWeight = FontWeight.Bold)
                                StatusPill(
                                    story.status.uppercase(),
                                    if (story.status == ContentStatuses.ACTIVE) StatusSuccess else StatusDanger,
                                    if (story.status == ContentStatuses.ACTIVE) StatusSuccessContainer else StatusDangerContainer
                                )
                            }
                            if (!story.mediaData.isNullOrBlank()) {
                                DarlingMediaImage(
                                    mediaDataOrUrl = story.mediaData,
                                    contentDescription = story.caption,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(16f / 9f)
                                        .clip(RoundedCornerShape(12.dp))
                                )
                            }
                            Text(story.caption, style = MaterialTheme.typography.bodyMedium)
                            if (story.status == ContentStatuses.ACTIVE) {
                                Button(
                                    onClick = { viewModel.removeStory(story.id, "Policy violation") },
                                    colors = ButtonDefaults.buttonColors(containerColor = StatusDanger)
                                ) { Text("Remove Story") }
                            }
                        }
                    }
                }
            }
            "comments" -> {
                items(comments.size, key = { comments[it].id }) { idx ->
                    val c = comments[idx]
                    Card(shape = RoundedCornerShape(16.dp)) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("@${c.authorUsername} on post ${c.postId.take(6)}", fontWeight = FontWeight.Bold)
                                StatusPill(
                                    c.status.uppercase(),
                                    if (c.status == ContentStatuses.ACTIVE) StatusSuccess else StatusDanger,
                                    if (c.status == ContentStatuses.ACTIVE) StatusSuccessContainer else StatusDangerContainer
                                )
                            }
                            Text(c.text, style = MaterialTheme.typography.bodyMedium)
                            if (c.status == ContentStatuses.ACTIVE) {
                                OutlinedButton(onClick = { viewModel.removeComment(c.postId, c.id, "Violating comment") }) {
                                    Text("Remove Comment", color = StatusDanger)
                                }
                            }
                        }
                    }
                }
            }
            else -> {
                val filteredPosts = posts.filter { p ->
                    when (filter) {
                        "photos" -> p.mediaType == "photo"
                        "videos" -> p.isReel || p.mediaType == "video"
                        "reported" -> reportedIds.contains(p.id)
                        else -> true
                    }
                }
                items(filteredPosts.size, key = { filteredPosts[it].id }) { idx ->
                    val post = filteredPosts[idx]
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("@${post.authorUsername}", fontWeight = FontWeight.Bold)
                                    Text(
                                        text = if (post.isReel) "REEL / VIDEO" else post.mediaType.uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AdminIndigo
                                    )
                                }
                                StatusPill(
                                    post.status.uppercase(),
                                    if (post.status == ContentStatuses.ACTIVE) StatusSuccess else StatusDanger,
                                    if (post.status == ContentStatuses.ACTIVE) StatusSuccessContainer else StatusDangerContainer
                                )
                            }
                            if (!post.mediaData.isNullOrBlank()) {
                                DarlingMediaImage(
                                    mediaDataOrUrl = post.mediaData,
                                    contentDescription = post.caption,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(16f / 10f)
                                        .clip(RoundedCornerShape(12.dp))
                                )
                            }
                            Text(post.caption, style = MaterialTheme.typography.bodyMedium)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (post.status == ContentStatuses.ACTIVE) {
                                    Button(
                                        onClick = { viewModel.removePost(post.id, "Removed for policy violation") },
                                        colors = ButtonDefaults.buttonColors(containerColor = StatusDanger)
                                    ) { Text("Remove Content") }
                                } else {
                                    OutlinedButton(onClick = { viewModel.restorePost(post.id) }) {
                                        Text("Restore Post")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminReportsTab(viewModel: AdminPanelViewModel) {
    val reports by viewModel.reports.collectAsStateWithLifecycle()
    val logs by viewModel.adminLogs.collectAsStateWithLifecycle()
    var subTab by remember { mutableIntStateOf(0) } // 0 = Reports Queue, 1 = Moderation History
    var targetFilter by remember { mutableStateOf("all") } // all, user, post, reel, comment
    var reviewingReport by remember { mutableStateOf<Report?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("admin_reports_tab")
    ) {
        TabRow(selectedTabIndex = subTab) {
            Tab(selected = subTab == 0, onClick = { subTab = 0 }, text = { Text("Reports (${reports.size})") })
            Tab(selected = subTab == 1, onClick = { subTab = 1 }, text = { Text("Moderation History") })
        }

        if (subTab == 0) {
            FlowRow(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("all" to "All", "user" to "Users", "post" to "Posts", "reel" to "Reels", "comment" to "Comments").forEach { (k, label) ->
                    FilterChip(
                        selected = targetFilter == k,
                        onClick = { targetFilter = k },
                        label = { Text(label) }
                    )
                }
            }

            val filtered = reports.filter { targetFilter == "all" || it.targetType == targetFilter }
            androidx.compose.foundation.lazy.LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filtered.size, key = { filtered[it].id }) { idx ->
                    val rep = filtered[idx]
                    val (fg, bg) = when (rep.status) {
                        ReportStatuses.RESOLVED -> StatusSuccess to StatusSuccessContainer
                        ReportStatuses.DISMISSED -> AdminIndigo to DarlingSoftLavender
                        else -> StatusWarning to StatusWarningContainer
                    }
                    Card(shape = RoundedCornerShape(16.dp)) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${rep.targetType.uppercase()} • ${rep.reason}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                StatusPill(rep.status.uppercase(), fg, bg)
                            }
                            Text("Target ID: ${rep.targetId}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (!rep.details.isNullOrBlank()) {
                                Text(rep.details, style = MaterialTheme.typography.bodySmall)
                            }
                            if (!rep.resolutionNote.isNullOrBlank()) {
                                Text("Resolution: ${rep.resolutionNote}", style = MaterialTheme.typography.bodySmall, color = StatusSuccess)
                            }
                            if (rep.status == ReportStatuses.PENDING) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { reviewingReport = rep },
                                        colors = ButtonDefaults.buttonColors(containerColor = AdminIndigo)
                                    ) { Text("Review & Resolve") }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            androidx.compose.foundation.lazy.LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(logs.size, key = { logs[it].id }) { idx ->
                    val log = logs[idx]
                    Card(shape = RoundedCornerShape(14.dp)) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(log.action, style = MaterialTheme.typography.labelLarge, color = AdminIndigo)
                            Text("Target: ${log.targetType} (${log.targetId})", style = MaterialTheme.typography.bodySmall)
                            Text(log.details, style = MaterialTheme.typography.bodyMedium)
                            Text("Moderator: ${log.adminEmail}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }

    reviewingReport?.let { rep ->
        var note by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { reviewingReport = null },
            title = { Text("Resolve ${rep.targetType.uppercase()} Report") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Reason: ${rep.reason}", fontWeight = FontWeight.SemiBold)
                    if (!rep.details.isNullOrBlank()) {
                        Text(rep.details, style = MaterialTheme.typography.bodySmall)
                    }
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it.take(400) },
                        label = { Text("Resolution note") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (rep.targetType == "post" || rep.targetType == "reel") {
                        OutlinedButton(
                            onClick = { viewModel.removePost(rep.targetId, "Removed from report ${rep.id}") },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Remove Reported Post/Reel", color = StatusDanger)
                        }
                    } else if (rep.targetType == "user") {
                        OutlinedButton(
                            onClick = { viewModel.suspendUser(rep.targetId, "Suspended from report ${rep.id}") },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Suspend Reported User", color = StatusWarning)
                        }
                    } else if (rep.targetType == "comment" && !rep.targetParentId.isNullOrBlank()) {
                        OutlinedButton(
                            onClick = { viewModel.removeComment(rep.targetParentId, rep.targetId, "Removed from report ${rep.id}") },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Remove Reported Comment", color = StatusDanger)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resolveReport(rep.id, ReportStatuses.RESOLVED, note)
                        reviewingReport = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusSuccess)
                ) { Text("Mark Resolved") }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            viewModel.resolveReport(rep.id, ReportStatuses.DISMISSED, note)
                            reviewingReport = null
                        }
                    ) { Text("Dismiss") }
                    TextButton(onClick = { reviewingReport = null }) { Text("Cancel") }
                }
            }
        )
    }
}
