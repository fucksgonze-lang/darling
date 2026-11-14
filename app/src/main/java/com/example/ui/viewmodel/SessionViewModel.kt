package com.example.ui.viewmodel

import android.util.Log
import com.example.data.model.AccountStatuses
import com.example.data.model.UserPrivateProfile
import com.example.data.model.UserRoles
import com.example.data.repository.AuthProfileRepository
import com.example.data.util.PreferencesManager
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

enum class AppMode {
    USER,
    ADMIN,
    ACCESS_DENIED
}

sealed interface SessionUiState {
    data object Unauthenticated : SessionUiState
    data object CheckingSession : SessionUiState
    data class NeedsRegistration(
        val uid: String,
        val email: String,
        val displayName: String,
        val photoUrl: String?,
        val emailVerified: Boolean
    ) : SessionUiState
    data class Restricted(val profile: UserPrivateProfile) : SessionUiState
    data class Authenticated(
        val uid: String,
        val profile: UserPrivateProfile,
        val isServerVerifiedAdmin: Boolean,
        val activeMode: AppMode
    ) : SessionUiState
}

class SessionViewModel(
    private val authRepo: AuthProfileRepository,
    private val prefs: PreferencesManager,
    private val auth: FirebaseAuth = Firebase.auth
) : ViewModel() {

    private val _sessionState = MutableStateFlow<SessionUiState>(SessionUiState.CheckingSession)
    val sessionState: StateFlow<SessionUiState> = _sessionState.asStateFlow()

    private val _registrationError = MutableStateFlow<String?>(null)
    val registrationError: StateFlow<String?> = _registrationError.asStateFlow()

    private val _isSubmittingRegistration = MutableStateFlow(false)
    val isSubmittingRegistration: StateFlow<Boolean> = _isSubmittingRegistration.asStateFlow()

    private val _isGuestPreview = MutableStateFlow(false)
    val isGuestPreview: StateFlow<Boolean> = _isGuestPreview.asStateFlow()

    val rememberMe: StateFlow<Boolean> = prefs.rememberMeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), true)

    val darkModeOverride: StateFlow<Boolean?> = prefs.darkModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    private var profileJob: Job? = null
    private var lastObservedUid: String? = null
    private var preferredMode: AppMode? = null

    private val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        handleAuthChange(firebaseAuth.currentUser)
    }

    init {
        auth.addAuthStateListener(authListener)
        handleAuthChange(auth.currentUser)
    }

    fun enterGuestPreview() {
        val current = _sessionState.value
        if (current is SessionUiState.Unauthenticated || current is SessionUiState.NeedsRegistration) {
            _isGuestPreview.value = true
        }
    }

    fun exitGuestPreview() {
        _isGuestPreview.value = false
    }

    private fun handleAuthChange(user: FirebaseUser?) {
        if (user == null) {
            lastObservedUid = null
            preferredMode = null
            profileJob?.cancel()
            profileJob = null
            _sessionState.value = SessionUiState.Unauthenticated
            return
        }

        _isGuestPreview.value = false
        if (lastObservedUid == user.uid && profileJob != null) return
        lastObservedUid = user.uid
        preferredMode = null
        profileJob?.cancel()
        _sessionState.value = SessionUiState.CheckingSession

        profileJob = viewModelScope.launch {
            val isServerAdmin = try {
                authRepo.verifyServerAdminRole(
                    userId = user.uid,
                    email = user.email,
                    displayName = user.displayName,
                    emailVerified = user.isEmailVerified
                )
            } catch (e: Exception) {
                Log.w("SessionVM", "Admin verification check returned false", e)
                false
            }

            authRepo.observePrivateProfile(user.uid)
                .catch { error ->
                    Log.w("SessionVM", "Error observing user profile", error)
                    _sessionState.value = SessionUiState.NeedsRegistration(
                        uid = user.uid,
                        email = user.email.orEmpty(),
                        displayName = user.displayName.orEmpty(),
                        photoUrl = user.photoUrl?.toString(),
                        emailVerified = user.isEmailVerified
                    )
                }
                .collect { profile ->
                    if (profile == null) {
                        _sessionState.value = SessionUiState.NeedsRegistration(
                            uid = user.uid,
                            email = user.email.orEmpty(),
                            displayName = user.displayName.orEmpty(),
                            photoUrl = user.photoUrl?.toString(),
                            emailVerified = user.isEmailVerified
                        )
                    } else if (profile.status == AccountStatuses.SUSPENDED || profile.status == AccountStatuses.BANNED) {
                        _sessionState.value = SessionUiState.Restricted(profile)
                    } else {
                        val effectiveAdmin = isServerAdmin && profile.role == UserRoles.ADMIN
                        val defaultMode = if (effectiveAdmin) AppMode.ADMIN else AppMode.USER
                        val mode = when (preferredMode) {
                            AppMode.ADMIN -> if (effectiveAdmin) AppMode.ADMIN else AppMode.ACCESS_DENIED
                            AppMode.ACCESS_DENIED -> AppMode.ACCESS_DENIED
                            AppMode.USER -> AppMode.USER
                            null -> defaultMode
                        }
                        _sessionState.value = SessionUiState.Authenticated(
                            uid = user.uid,
                            profile = profile,
                            isServerVerifiedAdmin = effectiveAdmin,
                            activeMode = mode
                        )
                    }
                }
        }
    }

    fun requestModeSwitch(targetMode: AppMode) {
        val current = _sessionState.value as? SessionUiState.Authenticated ?: return
        val resolvedMode = when (targetMode) {
            AppMode.ADMIN -> if (current.isServerVerifiedAdmin) AppMode.ADMIN else AppMode.ACCESS_DENIED
            AppMode.USER -> AppMode.USER
            AppMode.ACCESS_DENIED -> AppMode.ACCESS_DENIED
        }
        preferredMode = resolvedMode
        _sessionState.value = current.copy(activeMode = resolvedMode)
    }

    fun setRememberMe(enabled: Boolean) {
        viewModelScope.launch {
            prefs.setRememberMe(enabled)
        }
    }

    fun toggleDarkMode(currentlyDark: Boolean) {
        viewModelScope.launch {
            prefs.setDarkMode(!currentlyDark)
        }
    }

    suspend fun checkUsernameAvailable(username: String): Boolean =
        authRepo.isUsernameAvailable(username)

    fun clearRegistrationError() {
        _registrationError.value = null
    }

    fun completeRegistration(
        fullName: String,
        username: String,
        dateOfBirth: String,
        gender: String?,
        avatarUrl: String?,
        bio: String?,
        lookingFor: String?,
        interests: List<String>,
        termsAccepted: Boolean
    ) {
        viewModelScope.launch {
            _isSubmittingRegistration.value = true
            _registrationError.value = null
            val result = authRepo.completeRegistration(
                fullName = fullName,
                username = username,
                dateOfBirth = dateOfBirth,
                gender = gender,
                avatarUrl = avatarUrl,
                bio = bio,
                lookingFor = lookingFor,
                interests = interests,
                rememberMe = rememberMe.value,
                termsAccepted = termsAccepted
            )
            _isSubmittingRegistration.value = false
            result.onFailure { err ->
                _registrationError.value = err.message ?: "Unable to complete registration."
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        auth.removeAuthStateListener(authListener)
        profileJob?.cancel()
    }
}
