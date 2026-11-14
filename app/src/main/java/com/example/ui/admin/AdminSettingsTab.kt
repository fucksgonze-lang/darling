package com.example.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SwitchAccount
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.UiState
import com.example.data.model.UserPrivateProfile
import com.example.data.model.UserRoles
import com.example.ui.components.AvatarCircle
import com.example.ui.components.StatusPill
import com.example.ui.theme.AdminIndigo
import com.example.ui.theme.DarlingSoftLavender
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusSuccessContainer
import com.example.ui.viewmodel.AdminPanelViewModel

@Composable
fun AdminSettingsTab(
    adminProfile: UserPrivateProfile,
    viewModel: AdminPanelViewModel,
    onSwitchToUserFeed: () -> Unit,
    onSignOut: () -> Unit
) {
    val grants by viewModel.adminGrants.collectAsStateWithLifecycle()
    val logs by viewModel.adminLogs.collectAsStateWithLifecycle()
    val usersState by viewModel.usersState.collectAsStateWithLifecycle()
    val allUsers = (usersState as? UiState.Success)?.data.orEmpty()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("admin_settings_tab"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(shape = RoundedCornerShape(18.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        AvatarCircle(
                            avatarUrl = adminProfile.avatarUrl,
                            displayName = adminProfile.fullName,
                            size = 56.dp,
                            isVerified = true
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(adminProfile.fullName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("@${adminProfile.username} • ${adminProfile.email}", style = MaterialTheme.typography.bodySmall)
                        }
                        StatusPill("ADMIN", AdminIndigo, DarlingSoftLavender)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = onSwitchToUserFeed,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("admin_preview_user_feed_button")
                        ) {
                            Icon(Icons.Filled.SwitchAccount, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("User Feed")
                        }
                        Button(
                            onClick = onSignOut,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("admin_sign_out_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = StatusDanger)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Log Out")
                        }
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.Security, contentDescription = null, tint = AdminIndigo)
                        Text("Secure Role Management", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = "Admin roles are enforced server-side in Firestore Security Rules via verified designated email claims and the protected /admins collection. Ordinary users cannot escalate their own role.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    HorizontalDivider()
                    Text("Authorized Admins (${grants.size})", style = MaterialTheme.typography.labelLarge)
                    grants.forEach { grant ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(grant.displayName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text(grant.email, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (grant.userId != adminProfile.userId) {
                                OutlinedButton(onClick = { viewModel.revokeAdminRole(grant.userId, grant.email) }) {
                                    Text("Revoke", color = StatusDanger)
                                }
                            } else {
                                StatusPill("CURRENT", StatusSuccess, StatusSuccessContainer)
                            }
                        }
                    }

                    val promotableUsers = allUsers.filter { u ->
                        u.userId != adminProfile.userId && u.role != UserRoles.ADMIN && grants.none { it.userId == u.userId }
                    }
                    if (promotableUsers.isNotEmpty()) {
                        HorizontalDivider()
                        Text("Grant Secondary Admin Role", style = MaterialTheme.typography.labelLarge)
                        promotableUsers.take(5).forEach { candidate ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(candidate.fullName, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                                    Text(candidate.email, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                OutlinedButton(onClick = { viewModel.grantAdminRole(candidate) }) {
                                    Text("Make Admin")
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "Admin Activity Logs (${logs.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        items(logs.take(30), key = { it.id }) { log ->
            Card(shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(log.action, style = MaterialTheme.typography.labelMedium, color = AdminIndigo, fontWeight = FontWeight.Bold)
                    Text(log.details, style = MaterialTheme.typography.bodySmall)
                    Text("${log.adminEmail} • ${log.targetType}:${log.targetId}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
