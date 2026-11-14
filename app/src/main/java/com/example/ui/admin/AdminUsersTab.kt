package com.example.ui.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AccountStatuses
import com.example.data.model.UiState
import com.example.data.model.UserPrivateProfile
import com.example.ui.components.AvatarCircle
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
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminUsersTab(
    currentAdminUid: String,
    viewModel: AdminPanelViewModel
) {
    val usersState by viewModel.usersState.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("all") }
    var inspectedUser by remember { mutableStateOf<UserPrivateProfile?>(null) }
    var pendingAction by remember { mutableStateOf<Pair<UserPrivateProfile, String>?>(null) } // user, action

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("admin_users_tab")
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search users by name, @username, or email...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_user_search_input")
            )

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("all" to "All", AccountStatuses.ACTIVE to "Active", AccountStatuses.SUSPENDED to "Suspended", AccountStatuses.BANNED to "Banned").forEach { (key, label) ->
                    FilterChip(
                        selected = statusFilter == key,
                        onClick = { statusFilter = key },
                        label = { Text(label) }
                    )
                }
            }
        }

        when (val state = usersState) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AdminIndigo)
                }
            }
            is UiState.Error -> {
                Text(state.message, color = StatusDanger, modifier = Modifier.padding(20.dp))
            }
            is UiState.Success -> {
                val filtered = remember(state.data, query, statusFilter) {
                    val q = query.trim().lowercase(Locale.US)
                    state.data.filter { u ->
                        val matchesStatus = statusFilter == "all" || u.status == statusFilter
                        val matchesQuery = q.isEmpty() ||
                            u.fullName.lowercase(Locale.US).contains(q) ||
                            u.username.lowercase(Locale.US).contains(q) ||
                            u.email.lowercase(Locale.US).contains(q)
                        matchesStatus && matchesQuery
                    }
                }

                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filtered, key = { it.userId }) { user ->
                        AdminUserRowCard(
                            user = user,
                            isSelf = user.userId == currentAdminUid,
                            onInspect = { inspectedUser = user },
                            onSuspend = { pendingAction = user to "suspend" },
                            onBan = { pendingAction = user to "ban" },
                            onRestore = { pendingAction = user to "restore" },
                            onDelete = { pendingAction = user to "delete" }
                        )
                    }
                }
            }
        }
    }

    inspectedUser?.let { user ->
        val regDate = user.createdAt?.toDate()?.let {
            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(it)
        } ?: "N/A"
        AlertDialog(
            onDismissRequest = { inspectedUser = null },
            title = { Text(user.fullName) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Username: @${user.username}")
                    Text("Email: ${user.email}")
                    Text("Role: ${user.role}")
                    Text("Status: ${user.status}")
                    Text("Date of Birth: ${user.dateOfBirth}")
                    Text("Gender: ${user.gender ?: "Not specified"}")
                    Text("Registered: $regDate")
                    Text("UID: ${user.userId}", style = MaterialTheme.typography.labelSmall)
                    if (!user.bio.isNullOrBlank()) {
                        Text("Bio: ${user.bio}")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { inspectedUser = null }) { Text("Close") }
            }
        )
    }

    pendingAction?.let { (user, action) ->
        var reason by remember { mutableStateOf("") }
        val (title, confirmLabel, confirmColor) = when (action) {
            "suspend" -> Triple("Suspend @${user.username}?", "Suspend Account", StatusWarning)
            "ban" -> Triple("Ban @${user.username}?", "Ban Account", StatusDanger)
            "restore" -> Triple("Restore @${user.username}?", "Restore Account", StatusSuccess)
            else -> Triple("Delete @${user.username}?", "Delete Account", StatusDanger)
        }

        AlertDialog(
            onDismissRequest = { pendingAction = null },
            title = { Text(title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (action == "delete") {
                            "Legal/permanent deletion removes the user's private profile, public profile, and username reservation."
                        } else {
                            "Record a moderation reason for the immutable admin audit log:"
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it.take(300) },
                        label = { Text("Moderation reason") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        when (action) {
                            "suspend" -> viewModel.suspendUser(user.userId, reason)
                            "ban" -> viewModel.banUser(user.userId, reason)
                            "restore" -> viewModel.restoreUser(user.userId, reason)
                            "delete" -> viewModel.deleteUser(user.userId, user.username, reason)
                        }
                        pendingAction = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = confirmColor)
                ) {
                    Text(confirmLabel)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingAction = null }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdminUserRowCard(
    user: UserPrivateProfile,
    isSelf: Boolean,
    onInspect: () -> Unit,
    onSuspend: () -> Unit,
    onBan: () -> Unit,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    val regDate = remember(user.createdAt) {
        user.createdAt?.toDate()?.let {
            SimpleDateFormat("MMM d, yyyy", Locale.US).format(it)
        } ?: "Today"
    }
    val (statusTextColor, statusBgColor) = when (user.status) {
        AccountStatuses.SUSPENDED -> StatusWarning to StatusWarningContainer
        AccountStatuses.BANNED -> StatusDanger to StatusDangerContainer
        else -> StatusSuccess to StatusSuccessContainer
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onInspect)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                    AvatarCircle(avatarUrl = user.avatarUrl, displayName = user.fullName, size = 46.dp)
                    Column {
                        Text(user.fullName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text("@${user.username} • ${user.email}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Joined $regDate", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    StatusPill(user.status.uppercase(), statusTextColor, statusBgColor)
                    StatusPill(user.role.uppercase(), AdminIndigo, DarlingSoftLavender)
                }
            }

            if (!isSelf) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (user.status == AccountStatuses.ACTIVE) {
                        OutlinedButton(onClick = onSuspend) { Text("Suspend") }
                        OutlinedButton(onClick = onBan) { Text("Ban", color = StatusDanger) }
                    } else {
                        Button(
                            onClick = onRestore,
                            colors = ButtonDefaults.buttonColors(containerColor = StatusSuccess)
                        ) { Text("Restore") }
                    }
                    OutlinedButton(onClick = onDelete) {
                        Icon(Icons.Filled.DeleteForever, contentDescription = null, tint = StatusDanger)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete", color = StatusDanger)
                    }
                }
            }
        }
    }
}
