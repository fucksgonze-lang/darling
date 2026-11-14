package com.example

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.repository.AuthProfileRepository
import com.example.data.util.PreferencesManager
import com.example.ui.admin.AdminPanelScreen
import com.example.ui.auth.GuestPreviewScreen
import com.example.ui.auth.LoginScreen
import com.example.ui.auth.RegistrationScreen
import com.example.ui.auth.attemptAutoSignIn
import com.example.ui.auth.signOutUser
import com.example.ui.components.AccessDeniedScreen
import com.example.ui.components.AccountRestrictedScreen
import com.example.ui.components.DarlingLogoBadge
import com.example.ui.theme.DarlingDateTheme
import com.example.ui.theme.DarlingRose
import com.example.ui.user.UserPanelScreen
import com.example.ui.viewmodel.AppMode
import com.example.ui.viewmodel.SessionUiState
import com.example.ui.viewmodel.SessionViewModel
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DarlingDateRootApp()
        }
    }
}

@Composable
fun DarlingDateRootApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val sessionViewModel: SessionViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) as Application
                val dbId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(dbId)
                SessionViewModel(
                    authRepo = AuthProfileRepository(db),
                    prefs = PreferencesManager(app)
                )
            }
        }
    )

    val sessionState by sessionViewModel.sessionState.collectAsStateWithLifecycle()
    val rememberMe by sessionViewModel.rememberMe.collectAsStateWithLifecycle()
    val darkModeOverride by sessionViewModel.darkModeOverride.collectAsStateWithLifecycle()
    val isSubmittingRegistration by sessionViewModel.isSubmittingRegistration.collectAsStateWithLifecycle()
    val registrationError by sessionViewModel.registrationError.collectAsStateWithLifecycle()
    val isGuestPreview by sessionViewModel.isGuestPreview.collectAsStateWithLifecycle()

    val effectiveDarkTheme = darkModeOverride ?: isSystemInDarkTheme()

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {},
            onUnauthenticated = {},
            scope = scope
        )
    }

    val performSignOut: () -> Unit = {
        signOutUser(
            credentialManager = credentialManager,
            scope = scope
        )
    }

    DarlingDateTheme(darkTheme = effectiveDarkTheme) {
        when (val state = sessionState) {
            is SessionUiState.Unauthenticated -> {
                if (isGuestPreview) {
                    BackHandler {
                        sessionViewModel.exitGuestPreview()
                    }
                    GuestPreviewScreen(
                        isDarkTheme = effectiveDarkTheme,
                        onToggleDarkTheme = {
                            sessionViewModel.toggleDarkMode(effectiveDarkTheme)
                        },
                        onSignInRequested = {
                            sessionViewModel.exitGuestPreview()
                        }
                    )
                } else {
                    LoginScreen(
                        rememberMe = rememberMe,
                        onRememberMeChange = { sessionViewModel.setRememberMe(it) },
                        onPreviewFeatures = { sessionViewModel.enterGuestPreview() },
                        onAuthSuccess = {}
                    )
                }
            }

            is SessionUiState.CheckingSession -> {
                SessionCheckingScreen()
            }

            is SessionUiState.NeedsRegistration -> {
                if (isGuestPreview) {
                    BackHandler {
                        sessionViewModel.exitGuestPreview()
                    }
                    GuestPreviewScreen(
                        isDarkTheme = effectiveDarkTheme,
                        onToggleDarkTheme = {
                            sessionViewModel.toggleDarkMode(effectiveDarkTheme)
                        },
                        onSignInRequested = {
                            sessionViewModel.exitGuestPreview()
                        }
                    )
                } else {
                    RegistrationScreen(
                        email = state.email,
                        initialDisplayName = state.displayName,
                        initialPhotoUrl = state.photoUrl,
                        emailVerified = state.emailVerified,
                        isSubmitting = isSubmittingRegistration,
                        serverError = registrationError,
                        onCheckUsernameAvailable = { candidate ->
                            sessionViewModel.checkUsernameAvailable(candidate)
                        },
                        onSubmitRegistration = { fullName, username, dob, gender, avatarUrl, bio, lookingFor, interests, termsAccepted ->
                            sessionViewModel.completeRegistration(
                                fullName = fullName,
                                username = username,
                                dateOfBirth = dob,
                                gender = gender,
                                avatarUrl = avatarUrl,
                                bio = bio,
                                lookingFor = lookingFor,
                                interests = interests,
                                termsAccepted = termsAccepted
                            )
                        },
                        onPreviewFeatures = { sessionViewModel.enterGuestPreview() },
                        onSignOutToLogin = performSignOut
                    )
                }
            }

            is SessionUiState.Restricted -> {
                AccountRestrictedScreen(
                    profile = state.profile,
                    onSignOut = performSignOut
                )
            }

            is SessionUiState.Authenticated -> {
                when (state.activeMode) {
                    AppMode.ADMIN -> {
                        if (state.isServerVerifiedAdmin) {
                            BackHandler {
                                sessionViewModel.requestModeSwitch(AppMode.USER)
                            }
                            AdminPanelScreen(
                                currentUserId = state.uid,
                                adminProfile = state.profile,
                                onSwitchToUserFeed = {
                                    sessionViewModel.requestModeSwitch(AppMode.USER)
                                },
                                onSignOut = performSignOut
                            )
                        } else {
                            BackHandler {
                                sessionViewModel.requestModeSwitch(AppMode.USER)
                            }
                            AccessDeniedScreen(
                                userEmail = state.profile.email,
                                onBackToUserPanel = {
                                    sessionViewModel.requestModeSwitch(AppMode.USER)
                                },
                                onSignOut = performSignOut
                            )
                        }
                    }

                    AppMode.ACCESS_DENIED -> {
                        BackHandler {
                            sessionViewModel.requestModeSwitch(AppMode.USER)
                        }
                        AccessDeniedScreen(
                            userEmail = state.profile.email,
                            onBackToUserPanel = {
                                sessionViewModel.requestModeSwitch(AppMode.USER)
                            },
                            onSignOut = performSignOut
                        )
                    }

                    AppMode.USER -> {
                        UserPanelScreen(
                            currentUserId = state.uid,
                            privateProfile = state.profile,
                            isServerVerifiedAdmin = state.isServerVerifiedAdmin,
                            isDarkTheme = effectiveDarkTheme,
                            onToggleDarkTheme = {
                                sessionViewModel.toggleDarkMode(effectiveDarkTheme)
                            },
                            onRequestAdminPortal = {
                                sessionViewModel.requestModeSwitch(AppMode.ADMIN)
                            },
                            onSignOut = performSignOut
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionCheckingScreen() {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("session_checking_screen"),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            DarlingLogoBadge(size = 84.dp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Darling Date",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Verifying your session & role permissions...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            CircularProgressIndicator(
                modifier = Modifier.size(32.dp),
                color = DarlingRose,
                strokeWidth = 3.dp
            )
        }
    }
}
