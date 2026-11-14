package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ChatMessage
import com.example.data.model.Comment
import com.example.data.model.Conversation
import com.example.data.model.DatingMatch
import com.example.data.model.Post
import com.example.data.model.PublicProfile
import com.example.data.model.Story
import com.example.data.model.Swipe
import com.example.data.model.UiState
import com.example.data.repository.AuthProfileRepository
import com.example.data.repository.DatingMessagingRepository
import com.example.data.repository.SocialFeedRepository
import com.example.data.util.unwrapFirestoreError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class UserPanelViewModel(
    private val currentUserId: String,
    private val authRepo: AuthProfileRepository,
    private val feedRepo: SocialFeedRepository,
    private val datingRepo: DatingMessagingRepository
) : ViewModel() {

    val postsState: StateFlow<UiState<List<Post>>> = feedRepo.observeActivePosts()
        .map<List<Post>, UiState<List<Post>>> { UiState.Success(it) }
        .catch { error ->
            val fsErr = unwrapFirestoreError(error)
            if (fsErr == null && error is CancellationException) throw error
            val root = fsErr ?: error
            Log.w(TAG, "Posts stream error", root)
            emit(UiState.Error(root.message ?: "Failed to load feed."))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val storiesState: StateFlow<UiState<List<Story>>> = feedRepo.observeActiveStories()
        .map<List<Story>, UiState<List<Story>>> { UiState.Success(it) }
        .catch { error ->
            val fsErr = unwrapFirestoreError(error)
            if (fsErr == null && error is CancellationException) throw error
            emit(UiState.Error((fsErr ?: error).message ?: "Failed to load stories."))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val profilesState: StateFlow<UiState<List<PublicProfile>>> = authRepo.observeActivePublicProfiles()
        .map<List<PublicProfile>, UiState<List<PublicProfile>>> { UiState.Success(it) }
        .catch { error ->
            val fsErr = unwrapFirestoreError(error)
            if (fsErr == null && error is CancellationException) throw error
            emit(UiState.Error((fsErr ?: error).message ?: "Failed to load profiles."))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val followingIds: StateFlow<Set<String>> = authRepo.observeFollowingIds(currentUserId)
        .catch { emit(emptySet()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptySet())

    val swipes: StateFlow<List<Swipe>> = datingRepo.observeMySwipes(currentUserId)
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val matches: StateFlow<List<DatingMatch>> = datingRepo.observeMatches(currentUserId)
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val conversations: StateFlow<List<Conversation>> = datingRepo.observeConversations(currentUserId)
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val _likedPostIds = MutableStateFlow<Set<String>>(emptySet())
    val likedPostIds: StateFlow<Set<String>> = _likedPostIds.asStateFlow()

    private val _savedPostIds = MutableStateFlow<Set<String>>(emptySet())
    val savedPostIds: StateFlow<Set<String>> = _savedPostIds.asStateFlow()

    private val _mutualMatchProfile = MutableStateFlow<PublicProfile?>(null)
    val mutualMatchProfile: StateFlow<PublicProfile?> = _mutualMatchProfile.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    fun syncPostInteractions(posts: List<Post>) {
        viewModelScope.launch {
            val liked = mutableSetOf<String>()
            val saved = mutableSetOf<String>()
            posts.take(30).forEach { post ->
                if (feedRepo.isPostLikedByMe(post.id)) liked.add(post.id)
                if (feedRepo.isPostBookmarkedByMe(post.id)) saved.add(post.id)
            }
            _likedPostIds.value = liked
            _savedPostIds.value = saved
        }
    }

    fun clearMessage() {
        _actionMessage.value = null
    }

    fun dismissMatchPopup() {
        _mutualMatchProfile.value = null
    }

    fun toggleFollow(targetUserId: String) {
        val currentlyFollowing = followingIds.value.contains(targetUserId)
        viewModelScope.launch {
            authRepo.toggleFollow(targetUserId, currentlyFollowing)
                .onFailure { _actionMessage.value = it.message ?: "Follow action failed." }
        }
    }

    fun toggleLike(postId: String) {
        val currentlyLiked = _likedPostIds.value.contains(postId)
        viewModelScope.launch {
            // Optimistic toggle
            _likedPostIds.value = if (currentlyLiked) {
                _likedPostIds.value - postId
            } else {
                _likedPostIds.value + postId
            }
            feedRepo.toggleLike(postId, currentlyLiked).onFailure {
                _likedPostIds.value = if (currentlyLiked) _likedPostIds.value + postId else _likedPostIds.value - postId
                _actionMessage.value = it.message ?: "Could not update like."
            }
        }
    }

    fun toggleBookmark(postId: String) {
        val currentlySaved = _savedPostIds.value.contains(postId)
        viewModelScope.launch {
            _savedPostIds.value = if (currentlySaved) {
                _savedPostIds.value - postId
            } else {
                _savedPostIds.value + postId
            }
            feedRepo.toggleBookmark(postId, currentlySaved).onFailure {
                _savedPostIds.value = if (currentlySaved) _savedPostIds.value + postId else _savedPostIds.value - postId
                _actionMessage.value = it.message ?: "Could not save post."
            }
        }
    }

    fun createPost(
        author: PublicProfile,
        caption: String,
        mediaType: String,
        mediaData: String?,
        location: String?,
        isReel: Boolean,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = feedRepo.createPost(author, caption, mediaType, mediaData, location, isReel)
            _isBusy.value = false
            res.onSuccess {
                _actionMessage.value = if (isReel) "Reel published!" else "Post shared to feed!"
                onSuccess()
            }.onFailure {
                _actionMessage.value = it.message ?: "Failed to publish."
            }
        }
    }

    fun createStory(
        author: PublicProfile,
        caption: String,
        mediaData: String?,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = feedRepo.createStory(author, caption, mediaData)
            _isBusy.value = false
            res.onSuccess {
                _actionMessage.value = "Story added!"
                onSuccess()
            }.onFailure {
                _actionMessage.value = it.message ?: "Failed to share story."
            }
        }
    }

    fun observeComments(postId: String): Flow<List<Comment>> =
        feedRepo.observeComments(postId).catch { emit(emptyList()) }

    fun addComment(postId: String, author: PublicProfile, text: String) {
        viewModelScope.launch {
            feedRepo.addComment(postId, author, text)
                .onFailure { _actionMessage.value = it.message ?: "Failed to post comment." }
        }
    }

    fun deleteOwnPost(postId: String) {
        viewModelScope.launch {
            feedRepo.deleteOwnPost(postId)
                .onSuccess { _actionMessage.value = "Post deleted." }
                .onFailure { _actionMessage.value = it.message ?: "Could not delete post." }
        }
    }

    fun submitReport(
        targetType: String,
        targetId: String,
        targetParentId: String? = null,
        targetAuthorId: String? = null,
        reason: String,
        details: String?
    ) {
        viewModelScope.launch {
            feedRepo.submitReport(targetType, targetId, targetParentId, targetAuthorId, reason, details)
                .onSuccess { _actionMessage.value = "Report submitted to Darling Date moderators." }
                .onFailure { _actionMessage.value = it.message ?: "Failed to submit report." }
        }
    }

    fun swipeProfile(target: PublicProfile, action: String) {
        viewModelScope.launch {
            datingRepo.recordSwipe(target.userId, action)
                .onSuccess { isMutual ->
                    if (isMutual) {
                        _mutualMatchProfile.value = target
                    }
                }
                .onFailure { _actionMessage.value = it.message ?: "Could not record swipe." }
        }
    }

    fun observeMessages(recipientId: String): Flow<List<ChatMessage>> {
        val convId = datingRepo.pairDocId(currentUserId, recipientId)
        return datingRepo.observeMessages(convId, currentUserId).catch { emit(emptyList()) }
    }

    fun sendMessage(recipientId: String, text: String) {
        viewModelScope.launch {
            datingRepo.ensureConversationAndSendMessage(recipientId, text)
                .onFailure { _actionMessage.value = it.message ?: "Message failed to send." }
        }
    }

    fun updateProfile(
        fullName: String,
        bio: String?,
        gender: String?,
        avatarUrl: String?,
        lookingFor: String?,
        interests: List<String>,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = authRepo.updateMyProfile(fullName, bio, gender, avatarUrl, lookingFor, interests)
            _isBusy.value = false
            res.onSuccess {
                _actionMessage.value = "Profile updated!"
                onSuccess()
            }.onFailure {
                _actionMessage.value = it.message ?: "Could not update profile."
            }
        }
    }

    private companion object {
        const val TAG = "UserPanelVM"
    }
}
