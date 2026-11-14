package com.example.ui.user

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.PublicProfile
import com.example.data.model.UiState
import com.example.ui.components.AvatarCircle
import com.example.ui.theme.DarlingRose
import com.example.ui.viewmodel.UserPanelViewModel

@Composable
fun UserMessagesTab(
    currentUserId: String,
    activeChatPartner: PublicProfile?,
    onSelectPartner: (PublicProfile?) -> Unit,
    viewModel: UserPanelViewModel
) {
    val profilesState by viewModel.profilesState.collectAsStateWithLifecycle()
    val matches by viewModel.matches.collectAsStateWithLifecycle()
    val conversations by viewModel.conversations.collectAsStateWithLifecycle()

    val profilesById = remember(profilesState) {
        (profilesState as? UiState.Success)?.data.orEmpty().associateBy { it.userId }
    }

    if (activeChatPartner != null) {
        BackHandler { onSelectPartner(null) }
        DirectChatView(
            currentUserId = currentUserId,
            partner = activeChatPartner,
            viewModel = viewModel,
            onBack = { onSelectPartner(null) }
        )
        return
    }

    val matchedProfiles = remember(matches, profilesById, currentUserId) {
        matches.mapNotNull { m ->
            val otherId = m.participantIds.firstOrNull { it != currentUserId }
            otherId?.let { profilesById[it] }
        }
    }

    val activeThreads = remember(conversations, profilesById, currentUserId) {
        conversations.mapNotNull { conv ->
            val otherId = conv.participantIds.firstOrNull { it != currentUserId }
            otherId?.let { profilesById[it] }?.let { it to conv }
        }
    }

    val allPeopleToMessage = remember(profilesById, currentUserId) {
        profilesById.values.filter { it.userId != currentUserId }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("user_messages_tab"),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Text(
                text = "Matches (${matchedProfiles.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
            if (matchedProfiles.isEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.Favorite, contentDescription = null, tint = DarlingRose, modifier = Modifier.size(18.dp))
                    Text(
                        text = "Swipe on Dating Sparks to earn mutual matches!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(matchedProfiles, key = { it.userId }) { partner ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .width(68.dp)
                                .clickable { onSelectPartner(partner) }
                        ) {
                            AvatarCircle(
                                avatarUrl = partner.avatarUrl,
                                displayName = partner.fullName,
                                size = 58.dp,
                                hasStoryRing = true
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = partner.fullName.substringBefore(" "),
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider()
        }

        item {
            Text(
                text = "Messages",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }

        if (activeThreads.isEmpty() && allPeopleToMessage.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(36.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Filled.ChatBubbleOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(44.dp)
                    )
                    Text("No conversations yet", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "When other users join or match with you, your direct chats appear here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(activeThreads, key = { it.second.id }) { (partner, conv) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectPartner(partner) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AvatarCircle(
                        avatarUrl = partner.avatarUrl,
                        displayName = partner.fullName,
                        size = 50.dp,
                        isVerified = partner.isVerified
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = partner.fullName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = conv.lastMessage ?: "Tap to start chatting",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            }

            if (activeThreads.isEmpty() && allPeopleToMessage.isNotEmpty()) {
                item {
                    Text(
                        text = "Start a conversation",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
                items(allPeopleToMessage.take(10), key = { it.userId }) { person ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectPartner(person) }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AvatarCircle(
                            avatarUrl = person.avatarUrl,
                            displayName = person.fullName,
                            size = 44.dp
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(person.fullName, style = MaterialTheme.typography.titleSmall)
                            Text("@${person.username}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("Message", color = DarlingRose, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun DirectChatView(
    currentUserId: String,
    partner: PublicProfile,
    viewModel: UserPanelViewModel,
    onBack: () -> Unit
) {
    val messages by viewModel.observeMessages(partner.userId)
        .collectAsStateWithLifecycle(initialValue = emptyList())
    var draft by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(tonalElevation = 2.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to messages")
                }
                AvatarCircle(
                    avatarUrl = partner.avatarUrl,
                    displayName = partner.fullName,
                    size = 38.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(partner.fullName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text("@${partner.username}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        HorizontalDivider()

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                val isMine = msg.senderId == currentUserId
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = if (isMine) Alignment.CenterEnd else Alignment.CenterStart
                ) {
                    Box(
                        modifier = Modifier
                            .widthIn(max = 280.dp)
                            .clip(
                                RoundedCornerShape(
                                    topStart = 18.dp,
                                    topEnd = 18.dp,
                                    bottomStart = if (isMine) 18.dp else 4.dp,
                                    bottomEnd = if (isMine) 4.dp else 18.dp
                                )
                            )
                            .background(if (isMine) DarlingRose else MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = msg.text,
                            color = if (isMine) Color.White else MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        Surface(tonalElevation = 3.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it.take(1000) },
                    placeholder = { Text("Write a message...") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = {
                        if (draft.isNotBlank()) {
                            viewModel.sendMessage(partner.userId, draft)
                            draft = ""
                        }
                    }
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send message", tint = DarlingRose)
                }
            }
        }
    }
}
