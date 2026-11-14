package com.example.ui.admin

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.AdminIndigo
import com.example.ui.theme.DarlingCoral
import com.example.ui.theme.DarlingRose
import com.example.ui.theme.DarlingViolet
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import com.example.ui.viewmodel.AdminPanelViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun AdminOverviewTab(
    viewModel: AdminPanelViewModel,
    onNavigateToReports: () -> Unit,
    onNavigateToUsers: () -> Unit
) {
    val stats by viewModel.overviewStats.collectAsStateWithLifecycle()
    val logs by viewModel.adminLogs.collectAsStateWithLifecycle()

    val kpis = listOf(
        KpiItem("Total Users", stats.totalUsers.toString(), Icons.Filled.Group, AdminIndigo),
        KpiItem("Active Users", stats.activeUsers.toString(), Icons.Filled.HowToReg, StatusSuccess),
        KpiItem("Total Posts", stats.totalPosts.toString(), Icons.Filled.PhotoLibrary, DarlingRose),
        KpiItem("Total Reels", stats.totalReels.toString(), Icons.Filled.Movie, DarlingViolet),
        KpiItem("Total Stories", stats.totalStories.toString(), Icons.Filled.Visibility, DarlingCoral),
        KpiItem("Total Messages", stats.totalMessages.toString(), Icons.Filled.ChatBubble, Color(0xFF0EA5E9)),
        KpiItem("Total Reports", "${stats.totalReports} (${stats.pendingReports} open)", Icons.Filled.Flag, StatusDanger),
        KpiItem("New Reg (7d)", stats.newRegistrations7d.toString(), Icons.Filled.PersonAdd, StatusWarning)
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("admin_overview_tab"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Platform Overview",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Live Firestore metrics across Darling Date social feed, dating, and moderation.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(kpis.chunked(2)) { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                pair.forEach { item ->
                    KpiCard(
                        item = item,
                        modifier = Modifier.weight(1f),
                        onClick = when (item.title) {
                            "Total Reports" -> onNavigateToReports
                            "Total Users", "Active Users", "New Reg (7d)" -> onNavigateToUsers
                            else -> null
                        }
                    )
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Account Health Breakdown",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    val total = stats.totalUsers.coerceAtLeast(1)
                    HealthBarRow("Active", stats.activeUsers, total, StatusSuccess)
                    HealthBarRow("Suspended", stats.suspendedUsers, total, StatusWarning)
                    HealthBarRow("Banned", stats.bannedUsers, total, StatusDanger)
                }
            }
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.History, contentDescription = null, tint = AdminIndigo)
                Text(
                    text = "Recent Moderation Activity",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (logs.isEmpty()) {
            item {
                Text(
                    text = "No moderation actions recorded yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(logs.take(8), key = { it.id }) { log ->
                val timeStr = remember(log.createdAt) {
                    log.createdAt?.toDate()?.let {
                        SimpleDateFormat("MMM d, HH:mm", Locale.US).format(it)
                    } ?: "Just now"
                }
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(log.action, style = MaterialTheme.typography.labelLarge, color = AdminIndigo)
                            Text(log.details, style = MaterialTheme.typography.bodySmall)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(timeStr, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

private data class KpiItem(
    val title: String,
    val value: String,
    val icon: ImageVector,
    val accent: Color
)

@Composable
private fun KpiCard(
    item: KpiItem,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Card(
        onClick = onClick ?: {},
        enabled = onClick != null,
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(item.accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(item.icon, contentDescription = null, tint = item.accent, modifier = Modifier.size(20.dp))
            }
            Text(
                text = item.value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = item.title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HealthBarRow(label: String, count: Int, total: Int, color: Color) {
    val fraction = (count.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text("$count", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surface)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
            )
        }
    }
}
