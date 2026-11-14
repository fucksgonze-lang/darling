package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.AccountStatuses
import com.example.data.model.PublicProfile
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
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AuthProfileRepository(
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

    fun observePrivateProfile(userId: String): Flow<UserPrivateProfile?> = flow {
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(
                            UserPrivateProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else null
                }
                .catch { error ->
                    val fsErr = unwrapFirestoreError(error)
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == userId) {
                        handleFirestoreError(fsErr ?: error, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    /**
     * Authoritative server-controlled admin role verification.
     * Queries Firestore `/admins/{uid}` and `/users/{uid}` and never trusts local client state.
     * For the primary designated admin email (with email_verified == true), Firestore Rules
     * permit bootstrapping the `/admins/{uid}` document on first sign-in. Ordinary users
     * attempting this receive PERMISSION_DENIED from Firestore Security Rules.
     */
    suspend fun verifyServerAdminRole(
        userId: String,
        email: String?,
        displayName: String?,
        emailVerified: Boolean
    ): Boolean {
        val adminDocRef = db.collection("admins").document(userId)
        try {
            val existing = adminDocRef.get().await()
            if (existing.exists()) {
                return true
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val fsErr = unwrapFirestoreError(e)
            if (fsErr?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                return false
            }
            handleFirestoreError(e, OperationType.GET, adminDocRef.path)
        }

        if (!emailVerified || email.isNullOrBlank()) {
            return false
        }

        // Attempt server-verified bootstrap of /admins/{uid}; Security Rules enforce designated admin token
        return try {
            val payload = mapOf(
                "userId" to userId,
                "email" to email,
                "displayName" to (displayName?.take(80)?.ifBlank { "Darling Admin" } ?: "Darling Admin"),
                "grantedBy" to "server_verified_admin_claim",
                "createdAt" to FieldValue.serverTimestamp()
            )
            adminDocRef.set(payload).await()
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val fsErr = unwrapFirestoreError(e)
            if (fsErr?.code != FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                handleFirestoreError(e, OperationType.CREATE, adminDocRef.path)
            }
            false
        }
    }

    suspend fun isUsernameAvailable(username: String): Boolean {
        requireUserId()
        val clean = username.trim().lowercase(Locale.US)
        if (!clean.matches(Regex("^[a-z0-9_]{3,30}$"))) return false
        val path = "usernames/$clean"
        return try {
            val snap = db.collection("usernames").document(clean).get().await()
            !snap.exists()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            false
        }
    }

    suspend fun completeRegistration(
        fullName: String,
        username: String,
        dateOfBirth: String,
        gender: String?,
        avatarUrl: String?,
        bio: String?,
        lookingFor: String?,
        interests: List<String>,
        rememberMe: Boolean,
        termsAccepted: Boolean
    ): Result<UserPrivateProfile> = runCatching {
        val user = auth.currentUser ?: throw IllegalStateException("Please sign in with Google first.")
        val uid = user.uid
        val cleanName = fullName.trim()
        val cleanUsername = username.trim().lowercase(Locale.US)
        val cleanDob = dateOfBirth.trim()
        val email = user.email?.trim().orEmpty()

        require(cleanName.length in 2..80) { "Full name must be between 2 and 80 characters." }
        require(cleanUsername.matches(Regex("^[a-z0-9_]{3,30}$"))) {
            "Username must be 3-30 lowercase letters, numbers, or underscores."
        }
        require(termsAccepted) { "You must accept the Terms of Service and Privacy Policy." }
        val age = computeAgeYears(cleanDob)
            ?: throw IllegalArgumentException("Please enter Date of Birth as YYYY-MM-DD.")
        require(age >= 18) { "You must be at least 18 years old to join Darling Date." }
        require(email.isNotBlank()) { "Verified Google email is required." }

        val usernameRef = db.collection("usernames").document(cleanUsername)
        val existingUsername = try {
            usernameRef.get().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, usernameRef.path)
            throw e
        }
        if (existingUsername.exists() && existingUsername.getString("userId") != uid) {
            throw IllegalStateException("That username is already taken. Please choose another.")
        }

        val isServerAdmin = verifyServerAdminRole(
            userId = uid,
            email = email,
            displayName = cleanName,
            emailVerified = user.isEmailVerified
        )
        val assignedRole = if (isServerAdmin) UserRoles.ADMIN else UserRoles.USER

        val privateRef = db.collection("users").document(uid)
        val publicRef = db.collection("public_profiles").document(uid)

        val privatePayload = mutableMapOf<String, Any>(
            "userId" to uid,
            "fullName" to cleanName,
            "username" to cleanUsername,
            "email" to email,
            "dateOfBirth" to cleanDob,
            "role" to assignedRole,
            "status" to AccountStatuses.ACTIVE,
            "termsAccepted" to true,
            "rememberMe" to rememberMe,
            "emailVerified" to user.isEmailVerified,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
            "lastActiveAt" to FieldValue.serverTimestamp()
        )
        gender?.trim()?.takeIf { it.isNotEmpty() }?.let { privatePayload["gender"] = it }
        avatarUrl?.trim()?.takeIf { it.isNotEmpty() }?.let { privatePayload["avatarUrl"] = it }
        bio?.trim()?.takeIf { it.isNotEmpty() }?.let { privatePayload["bio"] = it }

        val publicPayload = mutableMapOf<String, Any>(
            "userId" to uid,
            "fullName" to cleanName,
            "username" to cleanUsername,
            "accountStatus" to AccountStatuses.ACTIVE,
            "followersCount" to 0L,
            "followingCount" to 0L,
            "postsCount" to 0L,
            "isVerified" to isServerAdmin,
            "ageGroup" to age.toLong(),
            "interests" to interests.take(10),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        avatarUrl?.trim()?.takeIf { it.isNotEmpty() }?.let { publicPayload["avatarUrl"] = it }
        bio?.trim()?.takeIf { it.isNotEmpty() }?.let { publicPayload["bio"] = it }
        gender?.trim()?.takeIf { it.isNotEmpty() }?.let { publicPayload["gender"] = it }
        lookingFor?.trim()?.takeIf { it.isNotEmpty() }?.let { publicPayload["lookingFor"] = it }

        val usernamePayload = mapOf(
            "userId" to uid,
            "username" to cleanUsername,
            "createdAt" to FieldValue.serverTimestamp()
        )

        try {
            val batch = db.batch()
            if (!existingUsername.exists()) {
                batch.set(usernameRef, usernamePayload)
            }
            batch.set(privateRef, privatePayload)
            batch.set(publicRef, publicPayload)
            batch.commit().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, privateRef.path)
            throw e
        }

        privateRef.get().await().toObject(
            UserPrivateProfile::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
        ) ?: UserPrivateProfile(
            id = uid,
            userId = uid,
            fullName = cleanName,
            username = cleanUsername,
            email = email,
            dateOfBirth = cleanDob,
            gender = gender,
            avatarUrl = avatarUrl,
            bio = bio,
            role = assignedRole,
            status = AccountStatuses.ACTIVE,
            termsAccepted = true,
            rememberMe = rememberMe,
            emailVerified = user.isEmailVerified
        )
    }

    suspend fun updateMyProfile(
        fullName: String,
        bio: String?,
        gender: String?,
        avatarUrl: String?,
        lookingFor: String?,
        interests: List<String>
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val cleanName = fullName.trim()
        require(cleanName.length in 2..80) { "Name must be 2-80 characters." }

        val privateUpdates = mutableMapOf<String, Any>(
            "fullName" to cleanName,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (!bio.isNullOrBlank()) privateUpdates["bio"] = bio.trim().take(300)
        if (!gender.isNullOrBlank()) privateUpdates["gender"] = gender.trim().take(40)
        if (!avatarUrl.isNullOrBlank()) privateUpdates["avatarUrl"] = avatarUrl.trim()

        val publicUpdates = mutableMapOf<String, Any>(
            "fullName" to cleanName,
            "interests" to interests.take(10),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (!bio.isNullOrBlank()) publicUpdates["bio"] = bio.trim().take(300)
        if (!gender.isNullOrBlank()) publicUpdates["gender"] = gender.trim().take(40)
        if (!avatarUrl.isNullOrBlank()) publicUpdates["avatarUrl"] = avatarUrl.trim()
        if (!lookingFor.isNullOrBlank()) publicUpdates["lookingFor"] = lookingFor.trim().take(60)

        try {
            val batch = db.batch()
            batch.update(db.collection("users").document(uid), privateUpdates)
            batch.update(db.collection("public_profiles").document(uid), publicUpdates)
            batch.commit().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$uid")
            throw e
        }
    }

    fun observeActivePublicProfiles(): Flow<List<PublicProfile>> = flow {
        val uid = requireUserId()
        val path = "public_profiles"
        emitAll(
            db.collection(path)
                .whereEqualTo("accountStatus", AccountStatuses.ACTIVE)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        PublicProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.followersCount }
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

    fun observeFollowingIds(userId: String): Flow<Set<String>> = flow {
        val path = "follows"
        emitAll(
            db.collection(path)
                .whereEqualTo("followerId", userId)
                .snapshots()
                .map { snap ->
                    snap.documents.mapNotNull { it.getString("targetId") }.toSet()
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

    suspend fun toggleFollow(targetUserId: String, currentlyFollowing: Boolean): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(uid != targetUserId) { "Cannot follow yourself." }
        val followId = "${uid}_${targetUserId}"
        val followRef = db.collection("follows").document(followId)
        val myPublicRef = db.collection("public_profiles").document(uid)
        val targetPublicRef = db.collection("public_profiles").document(targetUserId)

        try {
            val batch = db.batch()
            if (currentlyFollowing) {
                batch.delete(followRef)
                batch.update(
                    myPublicRef,
                    mapOf(
                        "followingCount" to FieldValue.increment(-1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.update(
                    targetPublicRef,
                    mapOf(
                        "followersCount" to FieldValue.increment(-1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            } else {
                batch.set(
                    followRef,
                    mapOf(
                        "followerId" to uid,
                        "targetId" to targetUserId,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.update(
                    myPublicRef,
                    mapOf(
                        "followingCount" to FieldValue.increment(1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.update(
                    targetPublicRef,
                    mapOf(
                        "followersCount" to FieldValue.increment(1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, followRef.path)
            throw e
        }
    }

    companion object {
        fun computeAgeYears(dobIso: String): Int? {
            return try {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
                val birthDate = sdf.parse(dobIso) ?: return null
                val dob = Calendar.getInstance().apply { time = birthDate }
                val today = Calendar.getInstance()
                var age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR)
                if (today.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)) {
                    age--
                }
                if (age < 0 || age > 120) null else age
            } catch (_: Exception) {
                null
            }
        }
    }
}
