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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.PublicProfile
import com.example.data.model.UiState
import com.example.data.model.UserPrivateProfile
import com.example.data.util.MediaHelper
import com.example.ui.components.AvatarCircle
import com.example.ui.components.DarlingMediaImage
import com.example.ui.components.StatusPill
import com.example.ui.theme.AdminIndigo
import com.example.ui.theme.DarlingGradientBrush
import com.example.ui.theme.DarlingRose
import com.example.ui.theme.DarlingSoftLavender
import com.example.ui.viewmodel.UserPanelViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UserProfileTab(
    privateProfile: UserPrivateProfile,
    publicProfile: PublicProfile?,
    isServerVerifiedAdmin: Boolean,
    isDarkTheme: Boolean,
    onToggleDarkTheme: () -> Unit,
    showEditDialog: Boolean,
    onDismissEditDialog: () -> Unit,
    onOpenEditDialog: () -> Unit,
    onRequestAdminPortal: () -> Unit,
    onSignOut: () -> Unit,
    viewModel: UserPanelViewModel
) {
    val postsState by viewModel.postsState.collectAsStateWithLifecycle()
    val savedIds by viewModel.savedPostIds.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Posts, 1 = Reels, 2 = Saved

    val allPosts = (postsState as? UiState.Success)?.data.orEmpty()
    val myPosts = remember(allPosts, privateProfile.userId) {
        allPosts.filter { it.authorId == privateProfile.userId && !it.isReel }
    }
    val myReels = remember(allPosts, privateProfile.userId) {
        allPosts.filter { it.authorId == privateProfile.userId && it.isReel }
    }
    val savedPosts = remember(allPosts, savedIds) {
        allPosts.filter { savedIds.contains(it.id) }
    }

    val displayedPosts = when (selectedTab) {
        1 -> myReels
        2 -> savedPosts
        else -> myPosts
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("user_profile_tab"),
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
                        avatarUrl = publicProfile?.avatarUrl ?: privateProfile.avatarUrl,
                        displayName = privateProfile.fullName,
                        size = 82.dp,
                        isVerified = publicProfile?.isVerified == true
                    )
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ProfileStat("${myPosts.size + myReels.size}", "Posts")
                        ProfileStat("${publicProfile?.followersCount ?: 0}", "Followers")
                        ProfileStat("${publicProfile?.followingCount ?: 0}", "Following")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = privateProfile.fullName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    StatusPill(
                        label = privateProfile.role.uppercase(),
                        textColor = if (isServerVerifiedAdmin) AdminIndigo else DarlingRose,
                        backgroundColor = if (isServerVerifiedAdmin) DarlingSoftLavender else MaterialTheme.colorScheme.primaryContainer
                    )
                }
                Text(
                    text = "@${privateProfile.username} • ${privateProfile.email}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val bioText = publicProfile?.bio ?: privateProfile.bio
                if (!bioText.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = bioText, style = MaterialTheme.typography.bodyMedium)
                }

                val interests = publicProfile?.interests.orEmpty()
                if (interests.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        interests.forEach { tag ->
                            AssistChip(onClick = {}, label = { Text(tag) })
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onOpenEditDialog,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Edit Profile & Photo")
                }

                Spacer(modifier = Modifier.height(14.dp))
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Filled.DarkMode, contentDescription = null)
                                Text("Dark Appearance", style = MaterialTheme.typography.bodyMedium)
                            }
                            Switch(checked = isDarkTheme, onCheckedChange = { onToggleDarkTheme() })
                        }

                        HorizontalDivider()

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(onClick = onRequestAdminPortal)
                                .padding(vertical = 8.dp)
                                .testTag("admin_portal_entry_button"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Filled.AdminPanelSettings, contentDescription = null, tint = AdminIndigo)
                                Column {
                                    Text(
                                        text = if (isServerVerifiedAdmin) "Open Admin Dashboard" else "Admin Portal",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = if (isServerVerifiedAdmin) "Server-verified administrator access" else "Protected administrator route",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (isServerVerifiedAdmin) {
                                Icon(Icons.Filled.Verified, contentDescription = null, tint = AdminIndigo)
                            }
                        }

                        HorizontalDivider()

                        Button(
                            onClick = onSignOut,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("user_sign_out_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sign Out")
                        }
                    }
                }
            }
        }

        item {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Posts (${myPosts.size})") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Reels (${myReels.size})") })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Saved (${savedPosts.size})") })
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
                        text = "Nothing here yet.",
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

    if (showEditDialog) {
        EditProfileDialog(
            privateProfile = privateProfile,
            publicProfile = publicProfile,
            isBusy = isBusy,
            onDismiss = onDismissEditDialog,
            onSave = { name, bio, gender, avatar, lookingFor, interests ->
                viewModel.updateProfile(name, bio, gender, avatar, lookingFor, interests) {
                    onDismissEditDialog()
                }
            }
        )
    }
}

@Composable
private fun ProfileStat(count: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(count, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditProfileDialog(
    privateProfile: UserPrivateProfile,
    publicProfile: PublicProfile?,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String?, String?, String?, String?, List<String>) -> Unit
) {
    val context = LocalContext.current
    var fullName by remember { mutableStateOf(privateProfile.fullName) }
    var bio by remember { mutableStateOf(publicProfile?.bio ?: privateProfile.bio.orEmpty()) }
    var gender by remember { mutableStateOf(publicProfile?.gender ?: privateProfile.gender) }
    var avatarUrl by remember { mutableStateOf(publicProfile?.avatarUrl ?: privateProfile.avatarUrl) }
    var lookingFor by remember { mutableStateOf(publicProfile?.lookingFor ?: "Meaningful Connection") }
    var interests by remember { mutableStateOf(publicProfile?.interests.orEmpty().toSet()) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            MediaHelper.uriToCompactDataUri(context, uri)?.let { avatarUrl = it }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Profile") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AvatarCircle(avatarUrl = avatarUrl, displayName = fullName, size = 56.dp)
                    OutlinedButton(
                        onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                    ) {
                        Text("Change Photo")
                    }
                }
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it.take(80) },
                    label = { Text("Full Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it.take(300) },
                    label = { Text("Bio") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = lookingFor,
                    onValueChange = { lookingFor = it.take(60) },
                    label = { Text("Looking For") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Gender", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Woman", "Man", "Non-binary", "Prefer not to say").forEach { opt ->
                        FilterChip(
                            selected = gender == opt,
                            onClick = { gender = opt },
                            label = { Text(opt) }
                        )
                    }
                }
                Text("Interests", style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Coffee", "Travel", "Photography", "Music", "Fitness", "Foodie", "Art").forEach { tag ->
                        FilterChip(
                            selected = interests.contains(tag),
                            onClick = { interests = if (interests.contains(tag)) interests - tag else interests + tag },
                            label = { Text(tag) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(fullName, bio, gender, avatarUrl, lookingFor, interests.toList()) },
                enabled = !isBusy && fullName.trim().length >= 2,
                colors = ButtonDefaults.buttonColors(containerColor = DarlingRose)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
