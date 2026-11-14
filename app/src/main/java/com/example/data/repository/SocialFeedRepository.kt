package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.Comment
import com.example.data.model.ContentStatuses
import com.example.data.model.Post
import com.example.data.model.PublicProfile
import com.example.data.model.ReportStatuses
import com.example.data.model.Story
import com.example.data.util.OperationType
import com.example.data.util.handleFirestoreError
import com.example.data.util.unwrapFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class SocialFeedRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        auth = Firebase.auth
    )

    private fun requireUserId(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("Authentication required.")

    fun observeActivePosts(): Flow<List<Post>> = flow {
        val uid = requireUserId()
        val path = "posts"
        emitAll(
            db.collection(path)
                .whereEqualTo("status", ContentStatuses.ACTIVE)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        Post::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val fsErr = unwrapFirestoreError(error)
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) {
                        handleFirestoreError(fsErr ?: error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeActiveStories(): Flow<List<Story>> = flow {
        val uid = requireUserId()
        val path = "stories"
        emitAll(
            db.collection(path)
                .whereEqualTo("status", ContentStatuses.ACTIVE)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        Story::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val fsErr = unwrapFirestoreError(error)
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) {
                        handleFirestoreError(fsErr ?: error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun createPost(
        author: PublicProfile,
        caption: String,
        mediaType: String,
        mediaData: String?,
        location: String?,
        isReel: Boolean,
        videoUri: String? = null,
        videoDurationMs: Long? = null,
        musicTrackId: String? = null,
        musicTitle: String? = null,
        musicArtist: String? = null,
        muteOriginalAudio: Boolean = false
    ): Result<String> = runCatching {
        val uid = requireUserId()
        require(caption.isNotBlank() || !mediaData.isNullOrBlank() || !videoUri.isNullOrBlank()) {
            "Add a caption or select photo/video before posting."
        }
        val postRef = db.collection("posts").document()
        val payload = mutableMapOf<String, Any>(
            "authorId" to uid,
            "authorUsername" to author.username,
            "authorName" to author.fullName,
            "mediaType" to mediaType,
            "caption" to caption.trim().take(2200),
            "likesCount" to 0L,
            "commentsCount" to 0L,
            "isReel" to isReel,
            "muteOriginalAudio" to muteOriginalAudio,
            "status" to ContentStatuses.ACTIVE,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        author.avatarUrl?.takeIf { it.isNotBlank() }?.let { payload["authorAvatar"] = it }
        mediaData?.takeIf { it.isNotBlank() }?.let { payload["mediaData"] = it }
        videoUri?.trim()?.takeIf { it.isNotBlank() }?.let { payload["videoUri"] = it.take(1000) }
        videoDurationMs?.takeIf { it >= 0L }?.let { payload["videoDurationMs"] = it.coerceAtMost(3_600_000L) }
        musicTrackId?.trim()?.takeIf { it.isNotBlank() }?.let { payload["musicTrackId"] = it.take(80) }
        musicTitle?.trim()?.takeIf { it.isNotBlank() }?.let { payload["musicTitle"] = it.take(100) }
        musicArtist?.trim()?.takeIf { it.isNotBlank() }?.let { payload["musicArtist"] = it.take(80) }
        location?.trim()?.takeIf { it.isNotBlank() }?.let { payload["location"] = it.take(100) }

        try {
            val batch = db.batch()
            batch.set(postRef, payload)
            batch.update(
                db.collection("public_profiles").document(uid),
                mapOf(
                    "postsCount" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            postRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, postRef.path)
            throw e
        }
    }

    suspend fun createStory(
        author: PublicProfile,
        caption: String,
        mediaData: String?
    ): Result<String> = runCatching {
        val uid = requireUserId()
        require(caption.isNotBlank() || !mediaData.isNullOrBlank()) {
            "Add a story photo or caption."
        }
        val storyRef = db.collection("stories").document()
        val payload = mutableMapOf<String, Any>(
            "authorId" to uid,
            "authorUsername" to author.username,
            "authorName" to author.fullName,
            "caption" to caption.trim().take(200),
            "status" to ContentStatuses.ACTIVE,
            "createdAt" to FieldValue.serverTimestamp()
        )
        author.avatarUrl?.takeIf { it.isNotBlank() }?.let { payload["authorAvatar"] = it }
        mediaData?.takeIf { it.isNotBlank() }?.let { payload["mediaData"] = it }

        try {
            storyRef.set(payload).await()
            storyRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, storyRef.path)
            throw e
        }
    }

    suspend fun isPostLikedByMe(postId: String): Boolean {
        val uid = requireUserId()
        return try {
            db.collection("posts").document(postId)
                .collection("likes").document(uid).get().await().exists()
        } catch (_: Exception) {
            false
        }
    }

    suspend fun isPostBookmarkedByMe(postId: String): Boolean {
        val uid = requireUserId()
        return try {
            db.collection("posts").document(postId)
                .collection("bookmarks").document(uid).get().await().exists()
        } catch (_: Exception) {
            false
        }
    }

    suspend fun toggleLike(postId: String, currentlyLiked: Boolean): Result<Boolean> = runCatching {
        val uid = requireUserId()
        val postRef = db.collection("posts").document(postId)
        val likeRef = postRef.collection("likes").document(uid)
        try {
            val batch = db.batch()
            if (currentlyLiked) {
                batch.delete(likeRef)
                batch.update(
                    postRef,
                    mapOf(
                        "likesCount" to FieldValue.increment(-1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            } else {
                batch.set(
                    likeRef,
                    mapOf(
                        "userId" to uid,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.update(
                    postRef,
                    mapOf(
                        "likesCount" to FieldValue.increment(1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            !currentlyLiked
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, likeRef.path)
            throw e
        }
    }

    suspend fun toggleBookmark(postId: String, currentlySaved: Boolean): Result<Boolean> = runCatching {
        val uid = requireUserId()
        val bmRef = db.collection("posts").document(postId).collection("bookmarks").document(uid)
        try {
            if (currentlySaved) {
                bmRef.delete().await()
            } else {
                bmRef.set(
                    mapOf(
                        "userId" to uid,
                        "postId" to postId,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            !currentlySaved
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, bmRef.path)
            throw e
        }
    }

    fun observeComments(postId: String): Flow<List<Comment>> = flow {
        val uid = requireUserId()
        val path = "posts/$postId/comments"
        emitAll(
            db.collection("posts").document(postId).collection("comments")
                .whereEqualTo("status", ContentStatuses.ACTIVE)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        Comment::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val fsErr = unwrapFirestoreError(error)
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) {
                        handleFirestoreError(fsErr ?: error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun addComment(postId: String, author: PublicProfile, text: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val clean = text.trim()
        require(clean.isNotEmpty() && clean.length <= 500) { "Comment must be 1-500 characters." }
        val postRef = db.collection("posts").document(postId)
        val commentRef = postRef.collection("comments").document()
        val payload = mutableMapOf<String, Any>(
            "postId" to postId,
            "authorId" to uid,
            "authorUsername" to author.username,
            "text" to clean,
            "status" to ContentStatuses.ACTIVE,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        author.avatarUrl?.takeIf { it.isNotBlank() }?.let { payload["authorAvatar"] = it }

        try {
            val batch = db.batch()
            batch.set(commentRef, payload)
            batch.update(
                postRef,
                mapOf(
                    "commentsCount" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, commentRef.path)
            throw e
        }
    }

    suspend fun deleteOwnPost(postId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val postRef = db.collection("posts").document(postId)
        try {
            postRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, postRef.path)
            throw e
        }
    }

    suspend fun submitReport(
        targetType: String,
        targetId: String,
        targetParentId: String? = null,
        targetAuthorId: String? = null,
        reason: String,
        details: String? = null
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val cleanReason = reason.trim()
        require(cleanReason.length >= 2) { "Please select a report reason." }
        val reportRef = db.collection("reports").document()
        val payload = mutableMapOf<String, Any>(
            "reporterId" to uid,
            "targetType" to targetType,
            "targetId" to targetId,
            "reason" to cleanReason.take(120),
            "status" to ReportStatuses.PENDING,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        targetParentId?.takeIf { it.isNotBlank() }?.let { payload["targetParentId"] = it }
        targetAuthorId?.takeIf { it.isNotBlank() }?.let { payload["targetAuthorId"] = it }
        details?.trim()?.takeIf { it.isNotBlank() }?.let { payload["details"] = it.take(600) }

        try {
            reportRef.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, reportRef.path)
            throw e
        }
    }
}
