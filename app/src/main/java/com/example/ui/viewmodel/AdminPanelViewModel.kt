package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AccountStatuses
import com.example.data.model.AdminGrant
import com.example.data.model.AdminLog
import com.example.data.model.AdminOverviewStats
import com.example.data.model.Comment
import com.example.data.model.ContentStatuses
import com.example.data.model.Conversation
import com.example.data.model.Post
import com.example.data.model.Report
import com.example.data.model.ReportStatuses
import com.example.data.model.Story
import com.example.data.model.UiState
import com.example.data.model.UserPrivateProfile
import com.example.data.repository.AdminRepository
import com.example.data.util.unwrapFirestoreError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AdminPanelViewModel(
    private val adminRepo: AdminRepository
) : ViewModel() {

    val usersState: StateFlow<UiState<List<UserPrivateProfile>>> = adminRepo.observeAllUsers()
        .map<List<UserPrivateProfile>, UiState<List<UserPrivateProfile>>> { UiState.Success(it) }
        .catch { err ->
            val fsErr = unwrapFirestoreError(err)
            if (fsErr == null && err is CancellationException) throw err
            Log.w(TAG, "Admin users stream error", fsErr ?: err)
            emit(UiState.Error((fsErr ?: err).message ?: "Permission denied loading users."))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val posts: StateFlow<List<Post>> = adminRepo.observeAllPosts()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val stories: StateFlow<List<Story>> = adminRepo.observeAllStories()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val comments: StateFlow<List<Comment>> = adminRepo.observeAllComments()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val conversations: StateFlow<List<Conversation>> = adminRepo.observeAllConversations()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val reports: StateFlow<List<Report>> = adminRepo.observeAllReports()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val adminLogs: StateFlow<List<AdminLog>> = adminRepo.observeAdminLogs()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val adminGrants: StateFlow<List<AdminGrant>> = adminRepo.observeAdminGrants()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val overviewStats: StateFlow<AdminOverviewStats> = combine(
        usersState,
        posts,
        stories,
        conversations,
        reports
    ) { usersUi, postList, storyList, convList, reportList ->
        val users = (usersUi as? UiState.Success)?.data.orEmpty()
        val sevenDaysAgoMs = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
        AdminOverviewStats(
            totalUsers = users.size,
            activeUsers = users.count { it.status == AccountStatuses.ACTIVE },
            suspendedUsers = users.count { it.status == AccountStatuses.SUSPENDED },
            bannedUsers = users.count { it.status == AccountStatuses.BANNED },
            totalPosts = postList.count { !it.isReel && it.status == ContentStatuses.ACTIVE },
            totalReels = postList.count { it.isReel && it.status == ContentStatuses.ACTIVE },
            totalStories = storyList.count { it.status == ContentStatuses.ACTIVE },
            totalMessages = convList.sumOf { it.messageCount },
            totalReports = reportList.size,
            pendingReports = reportList.count { it.status == ReportStatuses.PENDING },
            newRegistrations7d = users.count { u ->
                val ms = u.createdAt?.toDate()?.time ?: 0L
                ms >= sevenDaysAgoMs
            }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), AdminOverviewStats())

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    fun clearMessage() {
        _actionMessage.value = null
    }

    fun suspendUser(userId: String, reason: String) {
        viewModelScope.launch {
            adminRepo.updateUserStatus(userId, AccountStatuses.SUSPENDED, reason)
                .onSuccess { _actionMessage.value = "Account suspended." }
                .onFailure { _actionMessage.value = it.message ?: "Failed to suspend account." }
        }
    }

    fun banUser(userId: String, reason: String) {
        viewModelScope.launch {
            adminRepo.updateUserStatus(userId, AccountStatuses.BANNED, reason)
                .onSuccess { _actionMessage.value = "Account permanently banned." }
                .onFailure { _actionMessage.value = it.message ?: "Failed to ban account." }
        }
    }

    fun restoreUser(userId: String, reason: String) {
        viewModelScope.launch {
            adminRepo.updateUserStatus(userId, AccountStatuses.ACTIVE, reason)
                .onSuccess { _actionMessage.value = "Account restored to active." }
                .onFailure { _actionMessage.value = it.message ?: "Failed to restore account." }
        }
    }

    fun deleteUser(userId: String, username: String, reason: String) {
        viewModelScope.launch {
            adminRepo.deleteUserRecord(userId, username, reason)
                .onSuccess { _actionMessage.value = "User account deleted." }
                .onFailure { _actionMessage.value = it.message ?: "Failed to delete account." }
        }
    }

    fun removePost(postId: String, reason: String) {
        viewModelScope.launch {
            adminRepo.setPostStatus(postId, ContentStatuses.REMOVED, reason)
                .onSuccess { _actionMessage.value = "Content removed." }
                .onFailure { _actionMessage.value = it.message ?: "Failed to remove content." }
        }
    }

    fun restorePost(postId: String) {
        viewModelScope.launch {
            adminRepo.setPostStatus(postId, ContentStatuses.ACTIVE, "Restored by moderator")
                .onSuccess { _actionMessage.value = "Content restored." }
                .onFailure { _actionMessage.value = it.message ?: "Failed to restore content." }
        }
    }

    fun removeStory(storyId: String, reason: String) {
        viewModelScope.launch {
            adminRepo.removeStory(storyId, reason)
                .onSuccess { _actionMessage.value = "Story removed." }
                .onFailure { _actionMessage.value = it.message ?: "Failed to remove story." }
        }
    }

    fun removeComment(postId: String, commentId: String, reason: String) {
        viewModelScope.launch {
            adminRepo.removeComment(postId, commentId, reason)
                .onSuccess { _actionMessage.value = "Comment removed." }
                .onFailure { _actionMessage.value = it.message ?: "Failed to remove comment." }
        }
    }

    fun resolveReport(reportId: String, resolutionStatus: String, note: String) {
        viewModelScope.launch {
            adminRepo.resolveReport(reportId, resolutionStatus, note)
                .onSuccess { _actionMessage.value = "Report marked $resolutionStatus." }
                .onFailure { _actionMessage.value = it.message ?: "Failed to update report." }
        }
    }

    fun grantAdminRole(user: UserPrivateProfile) {
        viewModelScope.launch {
            adminRepo.grantAdminRole(user.userId, user.email, user.fullName)
                .onSuccess { _actionMessage.value = "Admin role granted to @${user.username}." }
                .onFailure { _actionMessage.value = it.message ?: "Failed to grant admin role." }
        }
    }

    fun revokeAdminRole(userId: String, email: String) {
        viewModelScope.launch {
            adminRepo.revokeAdminRole(userId, email)
                .onSuccess { _actionMessage.value = "Admin role revoked for $email." }
                .onFailure { _actionMessage.value = it.message ?: "Failed to revoke admin role." }
        }
    }

    private companion object {
        const val TAG = "AdminPanelVM"
    }
}
