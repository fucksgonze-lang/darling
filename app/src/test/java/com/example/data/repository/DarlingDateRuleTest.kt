package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.AccountStatuses
import com.example.data.model.PublicProfile
import com.example.data.model.UserRoles
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class DarlingDateRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun ordinaryUser_registrationAssignsUserRole_andBlocksAdminEscalation() = runBlocking {
        val suffix = UUID.randomUUID().toString().take(8)
        val uid = signInTestUser("user_$suffix@test.com")
        val authRepo = AuthProfileRepository(firestore, auth)

        val profile = withTimeout(TIMEOUT_MS) {
            authRepo.completeRegistration(
                fullName = "Alice Darling",
                username = "alice_$suffix",
                dateOfBirth = "1998-04-15",
                gender = "Woman",
                avatarUrl = null,
                bio = "Coffee & art",
                lookingFor = "Meaningful Connection",
                interests = listOf("Coffee", "Travel"),
                rememberMe = true,
                termsAccepted = true
            ).getOrThrow()
        }

        assertEquals(UserRoles.USER, profile.role)
        assertEquals(AccountStatuses.ACTIVE, profile.status)

        val isAdmin = withTimeout(TIMEOUT_MS) {
            authRepo.verifyServerAdminRole(uid, "user_$suffix@test.com", "Alice", true)
        }
        assertFalse(isAdmin)

        // Attempt to read /users collection list as ordinary user -> PERMISSION_DENIED
        val adminRepo = AdminRepository(firestore, auth)
        val listError = runCatching {
            withTimeout(FLOW_TIMEOUT_MS) { adminRepo.observeAllUsers().first() }
        }.exceptionOrNull()
        val fsErr = checkNotNull(listError?.unwrapFirestoreException()) {
            "Expected FirebaseFirestoreException, got $listError"
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsErr.code)
    }

    @Test
    fun ordinaryUser_under18_isRejectedLocallyBeforeWrite() = runBlocking {
        val suffix = UUID.randomUUID().toString().take(8)
        signInTestUser("minor_$suffix@test.com")
        val authRepo = AuthProfileRepository(firestore, auth)

        val result = withTimeout(TIMEOUT_MS) {
            authRepo.completeRegistration(
                fullName = "Young User",
                username = "young_$suffix",
                dateOfBirth = "2014-01-01",
                gender = null,
                avatarUrl = null,
                bio = null,
                lookingFor = null,
                interests = emptyList(),
                rememberMe = false,
                termsAccepted = true
            )
        }
        assertTrue(result.isFailure)
    }

    @Test
    fun socialFeed_createPostAndComment_andUnauthenticatedRejected() = runBlocking {
        val suffix = UUID.randomUUID().toString().take(8)
        val uid = signInTestUser("poster_$suffix@test.com")
        val authRepo = AuthProfileRepository(firestore, auth)
        authRepo.completeRegistration(
            fullName = "Poster One",
            username = "poster_$suffix",
            dateOfBirth = "1997-07-07",
            gender = null,
            avatarUrl = null,
            bio = "Photographer",
            lookingFor = null,
            interests = listOf("Photography"),
            rememberMe = true,
            termsAccepted = true
        ).getOrThrow()

        val feedRepo = SocialFeedRepository(firestore, auth)
        val author = PublicProfile(
            id = uid,
            userId = uid,
            fullName = "Poster One",
            username = "poster_$suffix"
        )
        val postId = withTimeout(TIMEOUT_MS) {
            feedRepo.createPost(
                author = author,
                caption = "Sunset walk $suffix",
                mediaType = "text",
                mediaData = null,
                location = "San Francisco",
                isReel = false
            ).getOrThrow()
        }
        assertTrue(postId.isNotBlank())

        val posts = withTimeout(FLOW_TIMEOUT_MS) {
            feedRepo.observeActivePosts().first { list -> list.any { it.id == postId } }
        }
        assertTrue(posts.any { it.id == postId })

        withTimeout(TIMEOUT_MS) {
            feedRepo.addComment(postId, author, "Lovely view!").getOrThrow()
        }

        // Sign out and verify unauthenticated query is rejected with PERMISSION_DENIED
        auth.signOut()
        val unauthError = runCatching {
            withTimeout(FLOW_TIMEOUT_MS) {
                firestore.collection("posts").whereEqualTo("status", "active").get().await()
            }
        }.exceptionOrNull()
        val fsErr = checkNotNull(unauthError?.unwrapFirestoreException()) {
            "Expected PERMISSION_DENIED when unauthenticated, got $unauthError"
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsErr.code)
    }

    @Test
    fun dating_mutualSwipeCreatesMatchAndConversation() = runBlocking {
        val suffix = UUID.randomUUID().toString().take(8)
        val aliceUid = signInTestUser("alice_match_$suffix@test.com")
        val aliceDating = DatingMessagingRepository(firestore, auth)

        val bobUid = signInTestUser("bob_match_$suffix@test.com")
        val bobDating = DatingMessagingRepository(firestore, auth)

        // Bob likes Alice first
        val firstMatch = withTimeout(TIMEOUT_MS) {
            bobDating.recordSwipe(aliceUid, "like").getOrThrow()
        }
        assertFalse(firstMatch)

        // Alice likes Bob -> mutual match!
        signInTestUser("alice_match_$suffix@test.com")
        val secondMatch = withTimeout(TIMEOUT_MS) {
            aliceDating.recordSwipe(bobUid, "super_spark").getOrThrow()
        }
        assertTrue(secondMatch)

        val convId = withTimeout(TIMEOUT_MS) {
            aliceDating.ensureConversationAndSendMessage(bobUid, "Hey! Excited we matched!").getOrThrow()
        }
        val messages = withTimeout(FLOW_TIMEOUT_MS) {
            aliceDating.observeMessages(convId, aliceUid).first { it.isNotEmpty() }
        }
        assertEquals("Hey! Excited we matched!", messages.first().text)
    }

    @Test
    fun designatedAdmin_verifiedLogin_bootstrapsAdminRoleAndModerates() = runBlocking {
        // First create an ordinary user + post + report so admin has real records to moderate
        val suffix = UUID.randomUUID().toString().take(8)
        val userUid = signInVerifiedTestUser("ordinary_$suffix@test.com")
        val userAuthRepo = AuthProfileRepository(firestore, auth)
        val userProfile = withTimeout(TIMEOUT_MS) {
            userAuthRepo.completeRegistration(
                fullName = "Ordinary Member",
                username = "member_$suffix",
                dateOfBirth = "1996-09-20",
                gender = "Man",
                avatarUrl = null,
                bio = "Foodie",
                lookingFor = "Casual Dates",
                interests = listOf("Foodie"),
                rememberMe = true,
                termsAccepted = true
            ).getOrThrow()
        }
        assertEquals(UserRoles.USER, userProfile.role)
        assertFalse(userAuthRepo.verifyServerAdminRole(userUid, userProfile.email, userProfile.fullName, true))

        val feedRepo = SocialFeedRepository(firestore, auth)
        val authorPublic = PublicProfile(
            id = userUid,
            userId = userUid,
            fullName = userProfile.fullName,
            username = userProfile.username
        )
        val postId = withTimeout(TIMEOUT_MS) {
            feedRepo.createPost(authorPublic, "Hello world $suffix", "text", null, null, false).getOrThrow()
        }
        withTimeout(TIMEOUT_MS) {
            feedRepo.submitReport("post", postId, null, userUid, "Spam or Scam", "Test report").getOrThrow()
        }

        // Now sign in as the verified designated admin email
        val adminUid = signInVerifiedTestUser("fucksgonze@gmail.com")
        val adminAuthRepo = AuthProfileRepository(firestore, auth)
        val verifiedAdmin = withTimeout(TIMEOUT_MS) {
            adminAuthRepo.verifyServerAdminRole(adminUid, "fucksgonze@gmail.com", "Darling Admin", true)
        }
        assertTrue("Verified designated admin email must be authorized server-side", verifiedAdmin)

        val adminProfile = withTimeout(TIMEOUT_MS) {
            adminAuthRepo.completeRegistration(
                fullName = "Darling Admin",
                username = "admin_$suffix",
                dateOfBirth = "1990-01-01",
                gender = null,
                avatarUrl = null,
                bio = "Moderator",
                lookingFor = null,
                interests = emptyList(),
                rememberMe = true,
                termsAccepted = true
            ).getOrThrow()
        }
        assertEquals(UserRoles.ADMIN, adminProfile.role)

        // Admin can list all users, suspend ordinary user, resolve report, and record immutable logs
        val adminRepo = AdminRepository(firestore, auth)
        val users = withTimeout(FLOW_TIMEOUT_MS) {
            adminRepo.observeAllUsers().first { list -> list.any { it.userId == userUid } }
        }
        assertTrue(users.any { it.userId == userUid })

        withTimeout(TIMEOUT_MS) {
            adminRepo.updateUserStatus(userUid, AccountStatuses.SUSPENDED, "Unit test suspension").getOrThrow()
        }
        val suspendedSnap = withTimeout(TIMEOUT_MS) {
            firestore.collection("users").document(userUid).get().await()
        }
        assertEquals(AccountStatuses.SUSPENDED, suspendedSnap.getString("status"))

        val logs = withTimeout(FLOW_TIMEOUT_MS) {
            adminRepo.observeAdminLogs().first { list -> list.any { it.targetId == userUid } }
        }
        assertTrue(logs.any { it.action == "SET_USER_STATUS_SUSPENDED" && it.targetId == userUid })
    }

    @Test
    fun unverifiedAdminEmail_cannotBootstrapAdminGrant() = runBlocking {
        // Deliberately create a fresh unverified account on the designated admin address -> rules must block admin bootstrap
        val uid = signInFreshUnverifiedUser("fucksgonze@gmail.com")
        val authRepo = AuthProfileRepository(firestore, auth)
        val isAdmin = withTimeout(TIMEOUT_MS) {
            authRepo.verifyServerAdminRole(uid, "fucksgonze@gmail.com", "Fake Admin", false)
        }
        assertFalse("Unverified email token must not grant admin role", isAdmin)
    }

    private companion object {
        const val TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
