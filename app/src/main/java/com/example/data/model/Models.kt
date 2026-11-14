package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

object UserRoles {
    const val USER = "user"
    const val ADMIN = "admin"
}

object AccountStatuses {
    const val ACTIVE = "active"
    const val SUSPENDED = "suspended"
    const val BANNED = "banned"
}

object ContentStatuses {
    const val ACTIVE = "active"
    const val REMOVED = "removed"
}

object ReportStatuses {
    const val PENDING = "pending"
    const val RESOLVED = "resolved"
    const val DISMISSED = "dismissed"
}

@IgnoreExtraProperties
data class AdminGrant(
    @DocumentId val id: String = "",
    val userId: String = "",
    val email: String = "",
    val displayName: String = "",
    val grantedBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class UserPrivateProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val fullName: String = "",
    val username: String = "",
    val email: String = "",
    val dateOfBirth: String = "",
    val gender: String? = null,
    val avatarUrl: String? = null,
    val bio: String? = null,
    val role: String = UserRoles.USER,
    val status: String = AccountStatuses.ACTIVE,
    val termsAccepted: Boolean = false,
    val rememberMe: Boolean = true,
    val emailVerified: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
    @ServerTimestamp val lastActiveAt: Timestamp? = null
)

@IgnoreExtraProperties
data class PublicProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val fullName: String = "",
    val username: String = "",
    val avatarUrl: String? = null,
    val bio: String? = null,
    val gender: String? = null,
    val ageGroup: Long? = null,
    val interests: List<String> = emptyList(),
    val lookingFor: String? = null,
    val accountStatus: String = AccountStatuses.ACTIVE,
    val followersCount: Long = 0L,
    val followingCount: Long = 0L,
    val postsCount: Long = 0L,
    @get:PropertyName("isVerified") @set:PropertyName("isVerified") var isVerified: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Post(
    @DocumentId val id: String = "",
    val authorId: String = "",
    val authorUsername: String = "",
    val authorName: String = "",
    val authorAvatar: String? = null,
    val mediaType: String = "photo",
    val mediaData: String? = null,
    val videoUri: String? = null,
    val videoDurationMs: Long? = null,
    val musicTrackId: String? = null,
    val musicTitle: String? = null,
    val musicArtist: String? = null,
    val muteOriginalAudio: Boolean = false,
    val caption: String = "",
    val location: String? = null,
    val likesCount: Long = 0L,
    val commentsCount: Long = 0L,
    @get:PropertyName("isReel") @set:PropertyName("isReel") var isReel: Boolean = false,
    val status: String = ContentStatuses.ACTIVE,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Comment(
    @DocumentId val id: String = "",
    val postId: String = "",
    val authorId: String = "",
    val authorUsername: String = "",
    val authorAvatar: String? = null,
    val text: String = "",
    val status: String = ContentStatuses.ACTIVE,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Story(
    @DocumentId val id: String = "",
    val authorId: String = "",
    val authorUsername: String = "",
    val authorName: String = "",
    val authorAvatar: String? = null,
    val mediaData: String? = null,
    val caption: String = "",
    val status: String = ContentStatuses.ACTIVE,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Follow(
    @DocumentId val id: String = "",
    val followerId: String = "",
    val targetId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Swipe(
    @DocumentId val id: String = "",
    val fromUserId: String = "",
    val toUserId: String = "",
    val action: String = "like",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class DatingMatch(
    @DocumentId val id: String = "",
    val user1Id: String = "",
    val user2Id: String = "",
    val participantIds: List<String> = emptyList(),
    val lastMessage: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Conversation(
    @DocumentId val id: String = "",
    val participantIds: List<String> = emptyList(),
    val lastMessage: String? = null,
    val messageCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val senderId: String = "",
    val recipientId: String = "",
    val participantIds: List<String> = emptyList(),
    val text: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Report(
    @DocumentId val id: String = "",
    val reporterId: String = "",
    val targetType: String = "post",
    val targetId: String = "",
    val targetParentId: String? = null,
    val targetAuthorId: String? = null,
    val reason: String = "",
    val details: String? = null,
    val status: String = ReportStatuses.PENDING,
    val resolutionNote: String? = null,
    val resolvedBy: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AdminLog(
    @DocumentId val id: String = "",
    val adminId: String = "",
    val adminEmail: String = "",
    val action: String = "",
    val targetType: String = "",
    val targetId: String = "",
    val details: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

data class AdminOverviewStats(
    val totalUsers: Int = 0,
    val activeUsers: Int = 0,
    val suspendedUsers: Int = 0,
    val bannedUsers: Int = 0,
    val totalPosts: Int = 0,
    val totalReels: Int = 0,
    val totalStories: Int = 0,
    val totalMessages: Long = 0L,
    val totalReports: Int = 0,
    val pendingReports: Int = 0,
    val newRegistrations7d: Int = 0
)
