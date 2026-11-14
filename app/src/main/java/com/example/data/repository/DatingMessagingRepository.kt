package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.ChatMessage
import com.example.data.model.Conversation
import com.example.data.model.DatingMatch
import com.example.data.model.Swipe
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

class DatingMessagingRepository(
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

    fun pairDocId(uidA: String, uidB: String): String =
        if (uidA < uidB) "${uidA}_${uidB}" else "${uidB}_${uidA}"

    fun observeMySwipes(userId: String): Flow<List<Swipe>> = flow {
        val path = "swipes"
        emitAll(
            db.collection(path)
                .whereEqualTo("fromUserId", userId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        Swipe::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val fsErr = unwrapFirestoreError(error)
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == userId) {
                        handleFirestoreError(fsErr ?: error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    /**
     * Records a dating swipe ("like", "pass", "super_spark").
     * Returns true if a mutual match was created!
     */
    suspend fun recordSwipe(targetUserId: String, action: String): Result<Boolean> = runCatching {
        val uid = requireUserId()
        require(uid != targetUserId) { "Cannot swipe on yourself." }
        val swipeId = "${uid}_${targetUserId}"
        val swipeRef = db.collection("swipes").document(swipeId)

        try {
            swipeRef.set(
                mapOf(
                    "fromUserId" to uid,
                    "toUserId" to targetUserId,
                    "action" to action,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, swipeRef.path)
            throw e
        }

        if (action == "pass") return@runCatching false

        // Check reverse swipe for mutual match
        val reverseId = "${targetUserId}_${uid}"
        val reverseSnap = try {
            db.collection("swipes").document(reverseId).get().await()
        } catch (_: Exception) {
            null
        }
        val reverseAction = reverseSnap?.getString("action")
        val isMutual = reverseAction == "like" || reverseAction == "super_spark"
        if (isMutual) {
            val pairId = pairDocId(uid, targetUserId)
            val sortedParticipants = listOf(uid, targetUserId).sorted()
            val matchRef = db.collection("matches").document(pairId)
            val convRef = db.collection("conversations").document(pairId)
            try {
                val batch = db.batch()
                if (!matchRef.get().await().exists()) {
                    batch.set(
                        matchRef,
                        mapOf(
                            "user1Id" to sortedParticipants[0],
                            "user2Id" to sortedParticipants[1],
                            "participantIds" to sortedParticipants,
                            "lastMessage" to "It's a Darling Match! Say hello.",
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
                if (!convRef.get().await().exists()) {
                    batch.set(
                        convRef,
                        mapOf(
                            "participantIds" to sortedParticipants,
                            "lastMessage" to "It's a Darling Match! Say hello.",
                            "messageCount" to 0L,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
                batch.commit().await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.WRITE, matchRef.path)
                throw e
            }
        }
        isMutual
    }

    fun observeMatches(userId: String): Flow<List<DatingMatch>> = flow {
        val path = "matches"
        emitAll(
            db.collection(path)
                .whereArrayContains("participantIds", userId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        DatingMatch::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val fsErr = unwrapFirestoreError(error)
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == userId) {
                        handleFirestoreError(fsErr ?: error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeConversations(userId: String): Flow<List<Conversation>> = flow {
        val path = "conversations"
        emitAll(
            db.collection(path)
                .whereArrayContains("participantIds", userId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        Conversation::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val fsErr = unwrapFirestoreError(error)
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == userId) {
                        handleFirestoreError(fsErr ?: error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeMessages(conversationId: String, currentUserId: String): Flow<List<ChatMessage>> = flow {
        val path = "conversations/$conversationId/messages"
        emitAll(
            db.collection("conversations").document(conversationId).collection("messages")
                .whereArrayContains("participantIds", currentUserId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        ChatMessage::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    val fsErr = unwrapFirestoreError(error)
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == currentUserId) {
                        handleFirestoreError(fsErr ?: error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun ensureConversationAndSendMessage(recipientId: String, text: String): Result<String> = runCatching {
        val uid = requireUserId()
        val clean = text.trim()
        require(clean.isNotEmpty() && clean.length <= 1000) { "Message must be 1-1000 characters." }
        val convId = pairDocId(uid, recipientId)
        val sortedParticipants = listOf(uid, recipientId).sorted()
        val convRef = db.collection("conversations").document(convId)
        val msgRef = convRef.collection("messages").document()

        try {
            val convExists = convRef.get().await().exists()
            val batch = db.batch()
            if (!convExists) {
                batch.set(
                    convRef,
                    mapOf(
                        "participantIds" to sortedParticipants,
                        "lastMessage" to clean.take(500),
                        "messageCount" to 1L,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            } else {
                batch.update(
                    convRef,
                    mapOf(
                        "lastMessage" to clean.take(500),
                        "messageCount" to FieldValue.increment(1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.set(
                msgRef,
                mapOf(
                    "senderId" to uid,
                    "recipientId" to recipientId,
                    "participantIds" to sortedParticipants,
                    "text" to clean,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            convId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, msgRef.path)
            throw e
        }
    }
}
