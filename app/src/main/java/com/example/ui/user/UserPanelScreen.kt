package com.example.ui.user

import android.app.Application
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.data.model.PublicProfile
import com.example.data.model.UiState
import com.example.data.model.UserPrivateProfile
import com.example.data.repository.AuthProfileRepository
import com.example.data.repository.DatingMessagingRepository
import com.example.data.repository.SocialFeedRepository
import com.example.ui.components.DarlingLogoBadge
import com.example.ui.theme.AdminIndigo
import com.example.ui.theme.DarlingRose
import com.example.ui.viewmodel.UserPanelViewModel
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserPanelScreen(
    currentUserId: String,
    privateProfile: UserPrivateProfile,
    isServerVerifiedAdmin: Boolean,
    isDarkTheme: Boolean,
    onToggleDarkTheme: () -> Unit,
    onRequestAdminPortal: () -> Unit,
    onSignOut: () -> Unit
) {
    val viewModel: UserPanelViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) as Application
                val dbId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(dbId)
                UserPanelViewModel(
                    currentUserId = currentUserId,
                    authRepo = AuthProfileRepository(db),
                    feedRepo = SocialFeedRepository(db),
                    datingRepo = DatingMessagingRepository(db)
                )
            }
        }
    )

    var selectedTab by remember { mutableIntStateOf(0) } // 0=Feed, 1=Discover, 2=Reels, 3=Messages, 4=Profile
    var showCreateSheet by remember { mutableStateOf(false) }
    var createSheetMode by remember { mutableIntStateOf(0) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showExploreMembers by remember { mutableStateOf(false) }
    var activeChatPartner by remember { mutableStateOf<PublicProfile?>(null) }
    var visitedProfileStack by remember { mutableStateOf<List<String>>(emptyList()) }

    val postsState by viewModel.postsState.collectAsStateWithLifecycle()
    val profilesState by viewModel.profilesState.collectAsStateWithLifecycle()
    val followingIds by viewModel.followingIds.collectAsStateWithLifecycle()
    val actionMessage by viewModel.actionMessage.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(postsState) {
        val list = (postsState as? UiState.Success)?.data ?: return@LaunchedEffect
        viewModel.syncPostInteractions(list)
    }

    LaunchedEffect(actionMessage) {
        val msg = actionMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearMessage()
    }

    val allProfiles = (profilesState as? UiState.Success)?.data.orEmpty()

    val myPublicProfile = remember(allProfiles, currentUserId, privateProfile) {
        allProfiles.firstOrNull { it.userId == currentUserId }
            ?: PublicProfile(
                id = currentUserId,
                userId = currentUserId,
                fullName = privateProfile.fullName,
                username = privateProfile.username,
                avatarUrl = privateProfile.avatarUrl,
                bio = privateProfile.bio,
                gender = privateProfile.gender,
                isVerified = isServerVerifiedAdmin
            )
    }

    val openUserProfile: (String) -> Unit = { targetId ->
        if (targetId == currentUserId) {
            visitedProfileStack = emptyList()
            selectedTab = 4
        } else if (visitedProfileStack.lastOrNull() != targetId) {
            visitedProfileStack = visitedProfileStack + targetId
        }
    }

    val navItems = listOf("Feed", "Discover", "Reels", "Messages", "Profile")

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 680.dp
        val isInDirectChat = selectedTab == 3 && activeChatPartner != null

        Scaffold(
            contentWindowInsets = WindowInsets.systemBars,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            DarlingLogoBadge(size = 30.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Darling Date",
                                style = MaterialTheme.typography.displaySmall.copy(
                                    fontSize = MaterialTheme.typography.titleLarge.fontSize
                                ),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { showExploreMembers = true },
                            modifier = Modifier.testTag("top_search_people_button")
                        ) {
                            Icon(Icons.Filled.Search, contentDescription = "Find & visit profiles")
                        }
                        IconButton(
                            onClick = {
                                createSheetMode = if (selectedTab == 2) 1 else 0
                                showCreateSheet = true
                            },
                            modifier = Modifier.testTag("top_create_button")
                        ) {
                            Icon(Icons.Filled.AddBox, contentDescription = "Create post, reel, or story", tint = DarlingRose)
                        }
                        if (isServerVerifiedAdmin) {
                            IconButton(
                                onClick = onRequestAdminPortal,
                                modifier = Modifier.testTag("switch_to_admin_button")
                            ) {
                                Icon(Icons.Filled.AdminPanelSettings, contentDescription = "Admin Dashboard", tint = AdminIndigo)
                            }
                        }
                    }
                )
            },
            bottomBar = {
                if (!isWideScreen && !isInDirectChat) {
                    NavigationBar {
                        navItems.forEachIndexed { idx, label ->
                            val selected = selectedTab == idx
                            NavigationBarItem(
                                selected = selected,
                                onClick = { selectedTab = idx },
                                icon = {
                                    Icon(
                                        imageVector = when (idx) {
                                            0 -> if (selected) Icons.Filled.Home else Icons.Outlined.Home
                                            1 -> if (selected) Icons.Filled.Explore else Icons.Outlined.Explore
                                            2 -> if (selected) Icons.Filled.PlayCircle else Icons.Outlined.PlayCircleOutline
                                            3 -> if (selected) Icons.Filled.ChatBubble else Icons.Outlined.ChatBubbleOutline
                                            else -> if (selected) Icons.Filled.Person else Icons.Outlined.Person
                                        },
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding)
                    .imePadding()
            ) {
                if (isWideScreen) {
                    NavigationRail(modifier = Modifier.fillMaxHeight()) {
                        navItems.forEachIndexed { idx, label ->
                            val selected = selectedTab == idx
                            NavigationRailItem(
                                selected = selected,
                                onClick = { selectedTab = idx },
                                icon = {
                                    Icon(
                                        imageVector = when (idx) {
                                            0 -> if (selected) Icons.Filled.Home else Icons.Outlined.Home
                                            1 -> if (selected) Icons.Filled.Explore else Icons.Outlined.Explore
                                            2 -> if (selected) Icons.Filled.PlayCircle else Icons.Outlined.PlayCircleOutline
                                            3 -> if (selected) Icons.Filled.ChatBubble else Icons.Outlined.ChatBubbleOutline
                                            else -> if (selected) Icons.Filled.Person else Icons.Outlined.Person
                                        },
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label) }
                            )
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    when (selectedTab) {
                        0 -> UserFeedTab(
                            currentUserId = currentUserId,
                            myPublicProfile = myPublicProfile,
                            viewModel = viewModel,
                            onOpenCreatePost = {
                                createSheetMode = 0
                                showCreateSheet = true
                            },
                            onOpenCreateStory = {
                                createSheetMode = 2
                                showCreateSheet = true
                            },
                            onOpenEditProfile = { showEditProfileDialog = true },
                            onVisitUser = openUserProfile
                        )
                        1 -> UserDiscoverReelsTab(
                            currentUserId = currentUserId,
                            viewModel = viewModel,
                            onMessageMatch = { partner ->
                                activeChatPartner = partner
                                selectedTab = 3
                            },
                            onOpenReels = { selectedTab = 2 },
                            onVisitUser = openUserProfile
                        )
                        2 -> UserReelsTab(
                            currentUserId = currentUserId,
                            myPublicProfile = myPublicProfile,
                            viewModel = viewModel,
                            onOpenCreateReel = {
                                createSheetMode = 1
                                showCreateSheet = true
                            },
                            onVisitUser = openUserProfile
                        )
                        3 -> UserMessagesTab(
                            currentUserId = currentUserId,
                            activeChatPartner = activeChatPartner,
                            onSelectPartner = { activeChatPartner = it },
                            onVisitUser = openUserProfile,
                            viewModel = viewModel
                        )
                        else -> UserProfileTab(
                            privateProfile = privateProfile,
                            publicProfile = myPublicProfile,
                            isServerVerifiedAdmin = isServerVerifiedAdmin,
                            isDarkTheme = isDarkTheme,
                            onToggleDarkTheme = onToggleDarkTheme,
                            showEditDialog = showEditProfileDialog,
                            onDismissEditDialog = { showEditProfileDialog = false },
                            onOpenEditDialog = { showEditProfileDialog = true },
                            onRequestAdminPortal = onRequestAdminPortal,
                            onSignOut = onSignOut,
                            onVisitUser = openUserProfile,
                            viewModel = viewModel
                        )
                    }
                }
            }
        }

        val currentVisitedUserId = visitedProfileStack.lastOrNull()
        if (currentVisitedUserId != null) {
            VisitedUserProfileOverlay(
                currentUserId = currentUserId,
                targetUserId = currentVisitedUserId,
                myPublicProfile = myPublicProfile,
                viewModel = viewModel,
                onBack = { visitedProfileStack = visitedProfileStack.dropLast(1) },
                onMessageUser = { partner ->
                    visitedProfileStack = emptyList()
                    activeChatPartner = partner
                    selectedTab = 3
                },
                onVisitAnotherUser = openUserProfile
            )
        }
    }

    if (showExploreMembers) {
        ExploreMembersSheet(
            currentUserId = currentUserId,
            allProfiles = allProfiles,
            myFollowingIds = followingIds,
            onDismiss = { showExploreMembers = false },
            onFollowToggle = { viewModel.toggleFollow(it) },
            onVisitUser = { uid ->
                showExploreMembers = false
                openUserProfile(uid)
            }
        )
    }

    if (showCreateSheet) {
        CreateContentBottomSheet(
            initialMode = createSheetMode,
            author = myPublicProfile,
            isBusy = isBusy,
            onDismiss = { showCreateSheet = false },
            onPublishPostOrReel = { caption, mediaType, mediaData, location, isReel, videoUri, videoDurationMs, musicTrackId, musicTitle, musicArtist, muteOriginalAudio ->
                viewModel.createPost(
                    author = myPublicProfile,
                    caption = caption,
                    mediaType = mediaType,
                    mediaData = mediaData,
                    location = location,
                    isReel = isReel,
                    videoUri = videoUri,
                    videoDurationMs = videoDurationMs,
                    musicTrackId = musicTrackId,
                    musicTitle = musicTitle,
                    musicArtist = musicArtist,
                    muteOriginalAudio = muteOriginalAudio
                ) {
                    showCreateSheet = false
                }
            },
            onPublishStory = { caption, mediaData ->
                viewModel.createStory(myPublicProfile, caption, mediaData) {
                    showCreateSheet = false
                }
            }
        )
    }
}
