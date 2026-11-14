package com.example.ui.admin

import android.app.Application
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwitchAccount
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
import com.example.data.model.UserPrivateProfile
import com.example.data.repository.AdminRepository
import com.example.ui.theme.AdminIndigo
import com.example.ui.viewmodel.AdminPanelViewModel
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(
    currentUserId: String,
    adminProfile: UserPrivateProfile,
    onSwitchToUserFeed: () -> Unit,
    onSignOut: () -> Unit
) {
    val viewModel: AdminPanelViewModel = viewModel(
        key = "admin_$currentUserId",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) as Application
                val dbId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(dbId)
                AdminPanelViewModel(AdminRepository(db))
            }
        }
    )

    var selectedTab by remember { mutableIntStateOf(0) } // 0=Overview, 1=Users, 2=Content, 3=Reports, 4=Settings
    val actionMessage by viewModel.actionMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(actionMessage) {
        val msg = actionMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearMessage()
    }

    val navItems = listOf(
        "Overview" to Icons.Filled.Dashboard,
        "Users" to Icons.Filled.Group,
        "Content" to Icons.Filled.PhotoLibrary,
        "Reports" to Icons.Filled.Flag,
        "Settings" to Icons.Filled.Settings
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .testTag("admin_dashboard_screen")
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.AdminPanelSettings, contentDescription = null, tint = AdminIndigo)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Darling Admin", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(adminProfile.email, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = onSwitchToUserFeed,
                            modifier = Modifier.testTag("admin_to_user_feed_button")
                        ) {
                            Icon(Icons.Filled.SwitchAccount, contentDescription = "Switch to User Feed")
                        }
                    }
                )
            },
            bottomBar = {
                if (!isWideScreen) {
                    NavigationBar {
                        navItems.forEachIndexed { idx, (label, icon) ->
                            NavigationBarItem(
                                selected = selectedTab == idx,
                                onClick = { selectedTab = idx },
                                icon = { Icon(icon, contentDescription = label) },
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
                        navItems.forEachIndexed { idx, (label, icon) ->
                            NavigationRailItem(
                                selected = selectedTab == idx,
                                onClick = { selectedTab = idx },
                                icon = { Icon(icon, contentDescription = label) },
                                label = { Text(label) }
                            )
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    when (selectedTab) {
                        0 -> AdminOverviewTab(
                            viewModel = viewModel,
                            onNavigateToReports = { selectedTab = 3 },
                            onNavigateToUsers = { selectedTab = 1 }
                        )
                        1 -> AdminUsersTab(
                            currentAdminUid = currentUserId,
                            viewModel = viewModel
                        )
                        2 -> AdminContentTab(viewModel = viewModel)
                        3 -> AdminReportsTab(viewModel = viewModel)
                        else -> AdminSettingsTab(
                            adminProfile = adminProfile,
                            viewModel = viewModel,
                            onSwitchToUserFeed = onSwitchToUserFeed,
                            onSignOut = onSignOut
                        )
                    }
                }
            }
        }
    }
}
