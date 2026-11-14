package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.AccountStatuses
import com.example.data.model.AdminGrant
import com.example.data.model.AdminLog
import com.example.data.model.Comment
import com.example.data.model.ContentStatuses
import com.example.data.model.Conversation
import com.example.data.model.Post
import com.example.data.model.Report
import com.example.data.model.Story
import com.example.data.model.UserPrivateProfile
import com.example.data.model.UserRoles
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

class AdminRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        auth = Firebase.auth
    )

    private fun requireAdminAuth(): Pair<String, String> {
        val user = auth.currentUser ?: throw IllegalStateException("Admin sign-in required.")
        return user.uid to (user.email ?: "admin@darlingdate.app")
    }

    fun observeAllUsers(): Flow<List<UserPrivateProfile>> = flow {
        val path = "users"
        emitAll(
            db.collection(path)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        UserPrivateProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val fsErr = unwrapFirestoreError(error)
                    if (fsErr == null && error is CancellationException) throw error
                    handleFirestoreError(fsErr ?: error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeAllPosts(): Flow<List<Post>> = flow {
        val path = "posts"
        emitAll(
            db.collection(path)
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
                    handleFirestoreError(fsErr ?: error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeAllStories(): Flow<List<Story>> = flow {
        val path = "stories"
        emitAll(
            db.collection(path)
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
                    handleFirestoreError(fsErr ?: error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeAllComments(): Flow<List<Comment>> = flow {
        val path = "comments(group)"
        emitAll(
            db.collectionGroup("comments")
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        Comment::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val fsErr = unwrapFirestoreError(error)
                    if (fsErr == null && error is CancellationException) throw error
                    handleFirestoreError(fsErr ?: error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeAllConversations(): Flow<List<Conversation>> = flow {
        val path = "conversations"
        emitAll(
            db.collection(path)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        Conversation::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val fsErr = unwrapFirestoreError(error)
                    if (fsErr == null && error is CancellationException) throw error
                    handleFirestoreError(fsErr ?: error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeAllReports(): Flow<List<Report>> = flow {
        val path = "reports"
        emitAll(
            db.collection(path)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        Report::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val fsErr = unwrapFirestoreError(error)
                    if (fsErr == null && error is CancellationException) throw error
                    handleFirestoreError(fsErr ?: error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeAdminLogs(): Flow<List<AdminLog>> = flow {
        val path = "admin_logs"
        emitAll(
            db.collection(path)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        AdminLog::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val fsErr = unwrapFirestoreError(error)
                    if (fsErr == null && error is CancellationException) throw error
                    handleFirestoreError(fsErr ?: error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeAdminGrants(): Flow<List<AdminGrant>> = flow {
        val path = "admins"
        emitAll(
            db.collection(path)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        AdminGrant::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val fsErr = unwrapFirestoreError(error)
                    if (fsErr == null && error is CancellationException) throw error
                    handleFirestoreError(fsErr ?: error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun updateUserStatus(
        targetUserId: String,
        newStatus: String,
        reason: String
    ): Result<Unit> = runCatching {
        val (adminUid, adminEmail) = requireAdminAuth()
        require(newStatus in setOf(AccountStatuses.ACTIVE, AccountStatuses.SUSPENDED, AccountStatuses.BANNED))
        val userRef = db.collection("users").document(targetUserId)
        val publicRef = db.collection("public_profiles").document(targetUserId)
        val logRef = db.collection("admin_logs").document()

        try {
            val batch = db.batch()
            batch.update(
                userRef,
                mapOf(
                    "status" to newStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            if (publicRef.get().await().exists()) {
                batch.update(
                    publicRef,
                    mapOf(
                        "accountStatus" to newStatus,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.set(
                logRef,
                mapOf(
                    "adminId" to adminUid,
                    "adminEmail" to adminEmail,
                    "action" to "SET_USER_STATUS_${newStatus.uppercase()}",
                    "targetType" to "user",
                    "targetId" to targetUserId,
                    "details" to reason.ifBlank { "Account status updated to $newStatus" }.take(500),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, userRef.path)
            throw e
        }
    }

    suspend fun deleteUserRecord(
        targetUserId: String,
        username: String,
        reason: String
    ): Result<Unit> = runCatching {
        val (adminUid, adminEmail) = requireAdminAuth()
        require(targetUserId != adminUid) { "Cannot delete your own active admin account." }
        val userRef = db.collection("users").document(targetUserId)
        val publicRef = db.collection("public_profiles").document(targetUserId)
        val usernameRef = db.collection("usernames").document(username.lowercase())
        val logRef = db.collection("admin_logs").document()

        try {
            val batch = db.batch()
            batch.delete(userRef)
            batch.delete(publicRef)
            if (username.isNotBlank()) batch.delete(usernameRef)
            batch.set(
                logRef,
                mapOf(
                    "adminId" to adminUid,
                    "adminEmail" to adminEmail,
                    "action" to "DELETE_USER_ACCOUNT",
                    "targetType" to "user",
                    "targetId" to targetUserId,
                    "details" to reason.ifBlank { "Legal/policy account removal (@$username)" }.take(500),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, userRef.path)
            throw e
        }
    }

    suspend fun setPostStatus(postId: String, newStatus: String, reason: String): Result<Unit> = runCatching {
        val (adminUid, adminEmail) = requireAdminAuth()
        val postRef = db.collection("posts").document(postId)
        val logRef = db.collection("admin_logs").document()
        try {
            val batch = db.batch()
            batch.update(
                postRef,
                mapOf(
                    "status" to newStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                logRef,
                mapOf(
                    "adminId" to adminUid,
                    "adminEmail" to adminEmail,
                    "action" to if (newStatus == ContentStatuses.REMOVED) "REMOVE_POST" else "RESTORE_POST",
                    "targetType" to "post",
                    "targetId" to postId,
                    "details" to reason.ifBlank { "Content moderation status set to $newStatus" }.take(500),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, postRef.path)
            throw e
        }
    }

    suspend fun removeStory(storyId: String, reason: String): Result<Unit> = runCatching {
        val (adminUid, adminEmail) = requireAdminAuth()
        val storyRef = db.collection("stories").document(storyId)
        val logRef = db.collection("admin_logs").document()
        try {
            val batch = db.batch()
            batch.update(storyRef, mapOf("status" to ContentStatuses.REMOVED))
            batch.set(
                logRef,
                mapOf(
                    "adminId" to adminUid,
                    "adminEmail" to adminEmail,
                    "action" to "REMOVE_STORY",
                    "targetType" to "story",
                    "targetId" to storyId,
                    "details" to reason.ifBlank { "Removed story for policy violation" }.take(500),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, storyRef.path)
            throw e
        }
    }

    suspend fun removeComment(postId: String, commentId: String, reason: String): Result<Unit> = runCatching {
        val (adminUid, adminEmail) = requireAdminAuth()
        val commentRef = db.collection("posts").document(postId).collection("comments").document(commentId)
        val logRef = db.collection("admin_logs").document()
        try {
            val batch = db.batch()
            batch.update(
                commentRef,
                mapOf(
                    "status" to ContentStatuses.REMOVED,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                logRef,
                mapOf(
                    "adminId" to adminUid,
                    "adminEmail" to adminEmail,
                    "action" to "REMOVE_COMMENT",
                    "targetType" to "comment",
                    "targetId" to commentId,
                    "details" to reason.ifBlank { "Removed comment on post $postId" }.take(500),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, commentRef.path)
            throw e
        }
    }

    suspend fun resolveReport(
        reportId: String,
        resolutionStatus: String,
        resolutionNote: String
    ): Result<Unit> = runCatching {
        val (adminUid, adminEmail) = requireAdminAuth()
        val reportRef = db.collection("reports").document(reportId)
        val logRef = db.collection("admin_logs").document()
        try {
            val batch = db.batch()
            batch.update(
                reportRef,
                mapOf(
                    "status" to resolutionStatus,
                    "resolutionNote" to resolutionNote.ifBlank { "Reviewed by moderator" }.take(400),
                    "resolvedBy" to adminUid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                logRef,
                mapOf(
                    "adminId" to adminUid,
                    "adminEmail" to adminEmail,
                    "action" to "REPORT_${resolutionStatus.uppercase()}",
                    "targetType" to "report",
                    "targetId" to reportId,
                    "details" to resolutionNote.ifBlank { "Report marked $resolutionStatus" }.take(500),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, reportRef.path)
            throw e
        }
    }

    suspend fun grantAdminRole(
        targetUserId: String,
        email: String,
        displayName: String
    ): Result<Unit> = runCatching {
        val (adminUid, adminEmail) = requireAdminAuth()
        val adminGrantRef = db.collection("admins").document(targetUserId)
        val userRef = db.collection("users").document(targetUserId)
        val logRef = db.collection("admin_logs").document()
        try {
            val batch = db.batch()
            batch.set(
                adminGrantRef,
                mapOf(
                    "userId" to targetUserId,
                    "email" to email,
                    "displayName" to displayName.take(80),
                    "grantedBy" to adminEmail,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            if (userRef.get().await().exists()) {
                batch.update(
                    userRef,
                    mapOf(
                        "role" to UserRoles.ADMIN,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.set(
                logRef,
                mapOf(
                    "adminId" to adminUid,
                    "adminEmail" to adminEmail,
                    "action" to "GRANT_ADMIN_ROLE",
                    "targetType" to "user",
                    "targetId" to targetUserId,
                    "details" to "Granted admin privileges to $email",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, adminGrantRef.path)
            throw e
        }
    }

    suspend fun revokeAdminRole(targetUserId: String, email: String): Result<Unit> = runCatching {
        val (adminUid, adminEmail) = requireAdminAuth()
        require(targetUserId != adminUid) { "Cannot revoke your own active admin role." }
        val adminGrantRef = db.collection("admins").document(targetUserId)
        val userRef = db.collection("users").document(targetUserId)
        val logRef = db.collection("admin_logs").document()
        try {
            val batch = db.batch()
            batch.delete(adminGrantRef)
            if (userRef.get().await().exists()) {
                batch.update(
                    userRef,
                    mapOf(
                        "role" to UserRoles.USER,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.set(
                logRef,
                mapOf(
                    "adminId" to adminUid,
                    "adminEmail" to adminEmail,
                    "action" to "REVOKE_ADMIN_ROLE",
                    "targetType" to "user",
                    "targetId" to targetUserId,
                    "details" to "Revoked admin privileges from $email",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, adminGrantRef.path)
            throw e
        }
    }
}
