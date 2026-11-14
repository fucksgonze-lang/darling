package com.example.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.components.DarlingLogoBadge
import com.example.ui.components.StatusPill
import com.example.ui.theme.DarlingCoral
import com.example.ui.theme.DarlingGradientBrush
import com.example.ui.theme.DarlingRose
import com.example.ui.theme.DarlingViolet
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusSuccessContainer
import com.example.ui.theme.StoryRingBrush
import kotlinx.coroutines.launch

private data class FeaturePreviewItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val badge: String,
    val highlights: List<String>,
    val gradient: List<Color>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuestPreviewScreen(
    isDarkTheme: Boolean,
    onToggleDarkTheme: () -> Unit,
    onSignInRequested: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0=Feed, 1=Discover, 2=Reels, 3=Messages, 4=Profile
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val promptSignUp: (String) -> Unit = { actionLabel ->
        scope.launch {
            snackbarHostState.showSnackbar("Preview Mode: Sign up or log in to $actionLabel.")
        }
    }

    val navItems = listOf("Feed", "Discover", "Reels", "Messages", "Profile")

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .testTag("guest_preview_screen")
    ) {
        val isWideScreen = maxWidth >= 680.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DarlingLogoBadge(size = 28.dp)
                            Text(
                                text = "Darling Date",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            StatusPill(
                                label = "Preview",
                                textColor = DarlingViolet,
                                backgroundColor = DarlingViolet.copy(alpha = 0.12f)
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { promptSignUp("publish posts, reels, or stories") },
                            modifier = Modifier.testTag("preview_create_button")
                        ) {
                            Icon(Icons.Filled.AddBox, contentDescription = "Preview create", tint = DarlingRose)
                        }
                        FilledTonalButton(
                            onClick = onSignInRequested,
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .testTag("preview_sign_in_button"),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Login,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sign Up")
                        }
                    }
                )
            },
            bottomBar = {
                if (!isWideScreen) {
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
                                            2 -> if (selected) Icons.Filled.VideoLibrary else Icons.Outlined.VideoLibrary
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
                                            2 -> if (selected) Icons.Filled.VideoLibrary else Icons.Outlined.VideoLibrary
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
                    PreviewBanner(onSignInRequested = onSignInRequested)
                    when (selectedTab) {
                        0 -> FeedFeaturePreview(onPromptSignUp = promptSignUp, onSignInRequested = onSignInRequested)
                        1 -> DiscoverReelsFeaturePreview(
                            onPromptSignUp = promptSignUp,
                            onOpenReelsTab = { selectedTab = 2 },
                            onSignInRequested = onSignInRequested
                        )
                        2 -> ReelsFeaturePreview(onSignInRequested = onSignInRequested)
                        3 -> MessagesFeaturePreview(onSignInRequested = onSignInRequested)
                        else -> ProfileFeaturePreview(
                            isDarkTheme = isDarkTheme,
                            onToggleDarkTheme = onToggleDarkTheme,
                            onSignInRequested = onSignInRequested
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewBanner(onSignInRequested: () -> Unit) {
    Surface(
        color = DarlingRose.copy(alpha = 0.1f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.LockOpen,
                    contentDescription = null,
                    tint = DarlingRose,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Guest Feature Preview • No account required to browse features",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                text = "Log In",
                style = MaterialTheme.typography.labelLarge,
                color = DarlingRose,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onSignInRequested)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun FeedFeaturePreview(
    onPromptSignUp: (String) -> Unit,
    onSignInRequested: () -> Unit
) {
    val storyPreviews = remember {
        listOf(
            "Stories" to "24h moments",
            "Filters" to "Photo & text",
            "Safety" to "18+ verified",
            "Reels" to "Short clips"
        )
    }
    val feedFeatures = remember {
        listOf(
            FeaturePreviewItem(
                id = "feed_social",
                title = "Live Social Feed & Moments",
                subtitle = "Share photos, videos, captions, and location tags with followers in real time.",
                badge = "Social Feed",
                highlights = listOf("Likes & real-time counters", "Nested comments", "Private bookmarks"),
                gradient = listOf(DarlingViolet, DarlingRose)
            ),
            FeaturePreviewItem(
                id = "feed_stories",
                title = "Stories & Quick Reactions",
                subtitle = "Tap story rings at the top of your feed to view full-screen moments from people you follow.",
                badge = "Stories",
                highlights = listOf("Photo Picker upload", "Instant story viewer", "Community reporting"),
                gradient = listOf(DarlingRose, DarlingCoral)
            )
        )
    }
    var likedPreviewIds by remember { mutableStateOf(setOf<String>()) }
    var savedPreviewIds by remember { mutableStateOf(setOf<String>()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(storyPreviews) { (label, caption) ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(72.dp)
                            .clickable { onPromptSignUp("share and view member stories") }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .border(2.5.dp, StoryRingBrush, CircleShape)
                                .padding(4.dp)
                                .clip(CircleShape)
                                .background(DarlingGradientBrush),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CameraAlt,
                                contentDescription = label,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                        Text(caption, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            HorizontalDivider()
        }

        items(feedFeatures, key = { it.id }) { item ->
            val liked = likedPreviewIds.contains(item.id)
            val saved = savedPreviewIds.contains(item.id)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.45f)
                            .background(Brush.linearGradient(item.gradient))
                            .padding(20.dp)
                    ) {
                        StatusPill(
                            label = item.badge,
                            textColor = Color.White,
                            backgroundColor = Color.Black.copy(alpha = 0.28f),
                            modifier = Modifier.align(Alignment.TopStart)
                        )
                        Column(
                            modifier = Modifier.align(Alignment.BottomStart),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.headlineSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = item.subtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.92f)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = {
                                likedPreviewIds = if (liked) likedPreviewIds - item.id else likedPreviewIds + item.id
                            }) {
                                Icon(
                                    imageVector = if (liked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                    contentDescription = "Try like interaction",
                                    tint = if (liked) DarlingRose else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            IconButton(onClick = { onPromptSignUp("comment on posts") }) {
                                Icon(
                                    imageVector = Icons.Outlined.ChatBubbleOutline,
                                    contentDescription = "Comments"
                                )
                            }
                        }
                        IconButton(onClick = {
                            savedPreviewIds = if (saved) savedPreviewIds - item.id else savedPreviewIds + item.id
                        }) {
                            Icon(
                                imageVector = if (saved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = "Try bookmark interaction",
                                tint = if (saved) DarlingViolet else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        item.highlights.forEach { bullet ->
                            Text(
                                text = "• $bullet",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Ready to join the real community?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Create your verified 18+ profile with Google to follow members, post photos, and see live community updates.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = onSignInRequested,
                        colors = ButtonDefaults.buttonColors(containerColor = DarlingRose),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Sign Up / Log In Now")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DiscoverReelsFeaturePreview(
    onPromptSignUp: (String) -> Unit,
    onOpenReelsTab: () -> Unit,
    onSignInRequested: () -> Unit
) {
    var segment by remember { mutableIntStateOf(0) }
    var cardIndex by remember { mutableIntStateOf(0) }

    val cardStages = remember {
        listOf(
            FeaturePreviewItem(
                id = "spark_cards",
                title = "Swipe Dating Sparks",
                subtitle = "Pass, Like, or send a Super Spark. Mutual likes instantly create a Darling Match and open direct messaging.",
                badge = "Dating Discovery",
                highlights = listOf("18+ verified profiles", "Interest tags", "Mutual match detection"),
                gradient = listOf(DarlingRose, DarlingCoral)
            ),
            FeaturePreviewItem(
                id = "spark_intent",
                title = "Match by Intent & Interests",
                subtitle = "Filter sparks by what people are looking for—Meaningful Connection, Casual Dates, or Long-Term.",
                badge = "Smart Matching",
                highlights = listOf("Coffee", "Travel", "Photography", "Music"),
                gradient = listOf(DarlingViolet, DarlingRose)
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = segment == 0,
                onClick = { segment = 0 },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
            ) {
                Text("Dating Sparks")
            }
            SegmentedButton(
                selected = segment == 1,
                onClick = { segment = 1 },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
            ) {
                Text("Reels Showcase")
            }
        }

        if (segment == 0) {
            val activeCard = cardStages[cardIndex % cardStages.size]
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.linearGradient(activeCard.gradient))
                        .padding(24.dp)
                ) {
                    StatusPill(
                        label = activeCard.badge,
                        textColor = Color.White,
                        backgroundColor = Color.Black.copy(alpha = 0.28f),
                        modifier = Modifier.align(Alignment.TopStart)
                    )
                    Column(
                        modifier = Modifier.align(Alignment.BottomStart),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = activeCard.title,
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = activeCard.subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.94f)
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            activeCard.highlights.forEach { tag ->
                                AssistChip(
                                    onClick = {},
                                    label = { Text(tag, color = Color.White) }
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalIconButton(
                    onClick = {
                        cardIndex++
                        onPromptSignUp("swipe on real dating profiles")
                    },
                    modifier = Modifier.size(58.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Try Pass")
                }

                FilledTonalIconButton(
                    onClick = {
                        cardIndex++
                        onPromptSignUp("send a Super Spark")
                    },
                    modifier = Modifier.size(58.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = DarlingViolet.copy(alpha = 0.16f),
                        contentColor = DarlingViolet
                    )
                ) {
                    Icon(Icons.Filled.Star, contentDescription = "Try Super Spark")
                }

                FilledTonalIconButton(
                    onClick = {
                        cardIndex++
                        onPromptSignUp("like & match with real people")
                    },
                    modifier = Modifier.size(66.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = DarlingRose,
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Filled.Favorite, contentDescription = "Try Like", modifier = Modifier.size(30.dp))
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.2f)
                            .background(DarlingGradientBrush),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayCircleFilled,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(64.dp)
                            )
                            Text(
                                text = "Darling Reels",
                                style = MaterialTheme.typography.headlineSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Watch and publish vertical video clips with smart background music suggestions.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.92f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onOpenReelsTab,
                            colors = ButtonDefaults.buttonColors(containerColor = DarlingRose),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Explore Reels Feature")
                        }
                        OutlinedButton(
                            onClick = onSignInRequested,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Sign Up to Publish Reels")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReelsFeaturePreview(onSignInRequested: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.2f)
                .clip(RoundedCornerShape(24.dp))
                .background(DarlingGradientBrush),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayCircleFilled,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(68.dp)
                )
                Text(
                    text = "Vertical Video Reels + Music",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Upload videos from your gallery, get automatic background music recommendations matched to your clip mood, mute or mix original audio, and swipe full-screen Reels.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.94f),
                    textAlign = TextAlign.Center
                )
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onSignInRequested,
            colors = ButtonDefaults.buttonColors(containerColor = DarlingRose),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text("Sign Up to Watch & Upload Reels")
        }
    }
}

@Composable
private fun MessagesFeaturePreview(onSignInRequested: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(84.dp)
                .clip(CircleShape)
                .background(DarlingGradientBrush),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.ChatBubble,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(40.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Real-Time Match Messaging",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Once two members spark each other in Discover, a private 1-on-1 conversation unlocks automatically with real-time message delivery.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(20.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = StatusSuccess, modifier = Modifier.size(18.dp))
                    Text("Participant-only Firestore security rules", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = DarlingRose, modifier = Modifier.size(18.dp))
                    Text("Instant icebreakers when a mutual match happens", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                }
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onSignInRequested,
            colors = ButtonDefaults.buttonColors(containerColor = DarlingRose),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text("Create Account to Start Chatting")
        }
    }
}

@Composable
private fun ProfileFeaturePreview(
    isDarkTheme: Boolean,
    onToggleDarkTheme: () -> Unit,
    onSignInRequested: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DarlingLogoBadge(size = 56.dp)
                    Column {
                        Text(
                            text = "Guest Preview",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        StatusPill(
                            label = "Not Signed In",
                            textColor = StatusSuccess,
                            backgroundColor = StatusSuccessContainer
                        )
                    }
                }
                Text(
                    text = "Sign up to claim a unique @username, customize your bio & dating interests, upload photos, and track followers.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Filled.DarkMode, contentDescription = null)
                    Text("Dark Appearance", style = MaterialTheme.typography.bodyLarge)
                }
                Switch(
                    checked = isDarkTheme,
                    onCheckedChange = { onToggleDarkTheme() }
                )
            }
        }

        Button(
            onClick = onSignInRequested,
            colors = ButtonDefaults.buttonColors(containerColor = DarlingRose),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text("Sign Up / Log In with Google")
        }

        OutlinedButton(
            onClick = onSignInRequested,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text("Back to Login Screen")
        }
    }
}
