package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class FrutigerChatRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun createProfileAndChatAndMessage_authenticatedOwner_succeeds() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repository = FrutigerChatRepository(firestore)

    val profileResult =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repository.createOrInitializeUserProfile(
          username = "AeroAlice",
          signature = "Glossy bubbles",
          currentActivity = "Listening to Lease",
          presenceStatus = "online",
          avatarColor = "aqua",
        )
      }
    assertTrue("Profile creation should succeed", profileResult.isSuccess)

    val uniqueChatId = "chat_test_${UUID.randomUUID().toString().replace("-", "").take(12)}"
    val chatResult =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repository.createChatRoom(
          title = "Friend 1",
          isGroup = false,
          avatarColor = "red",
          presenceStatus = "online",
          participantNames = "AeroAlice, Friend 1",
          customChatId = uniqueChatId,
        )
      }
    assertTrue("Chat creation should succeed, but got: ${chatResult.exceptionOrNull()}", chatResult.isSuccess)
    val chatId = chatResult.getOrThrow()

    val msgResult =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repository.sendEncryptedMessage(
          chatId = chatId,
          encryptionKeyId = "e2ee_${chatId.takeLast(16)}",
          senderId = uid,
          senderName = "AeroAlice",
          plaintext = "Hello Frutiger Aero World!",
        )
      }
    assertTrue("Encrypted message creation should succeed", msgResult.isSuccess)
  }

  @Test
  fun observeChats_authenticatedOwner_emitsRealtimeUpdates() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repository = FrutigerChatRepository(firestore)
    val uniqueChatId = "chat_obs_${UUID.randomUUID().toString().replace("-", "").take(12)}"

    val chatId =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repository
          .createChatRoom(
            title = "Emerald Lounge",
            isGroup = true,
            avatarColor = "group",
            presenceStatus = "online",
            participantNames = "AeroAlice, Friend 2",
            customChatId = uniqueChatId,
          )
          .getOrThrow()
      }

    val emittedChats =
      withTimeout(FLOW_TIMEOUT_MS) {
        repository.observeChats(uid).first { list -> list.any { it.id == chatId } }
      }
    assertTrue(emittedChats.any { it.id == chatId })
  }

  @Test
  fun getChatById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
    signInTestUser(ALICE_EMAIL)
    val aliceRepo = FrutigerChatRepository(firestore)
    val uniqueChatId = "chat_sec_${UUID.randomUUID().toString().replace("-", "").take(12)}"
    val chatId =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        aliceRepo
          .createChatRoom(
            title = "Private Aero Chat",
            isGroup = false,
            avatarColor = "green",
            presenceStatus = "online",
            participantNames = "AeroAlice",
            customChatId = uniqueChatId,
          )
          .getOrThrow()
      }

    signInTestUser(BOB_EMAIL)
    val bobRepo = FrutigerChatRepository(firestore)
    val result = withTimeout(DEFAULT_TIMEOUT_MS) { bobRepo.getChatById(chatId) }
    assertTrue("Cross-user access must fail", result.isFailure)
    val exception = result.exceptionOrNull() as? FirebaseFirestoreException
    assertNotNull(exception)
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, exception?.code)
  }

  @Test
  fun observeChats_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
    auth.signOut()
    val repository = FrutigerChatRepository(firestore)

    try {
      withTimeout(FLOW_TIMEOUT_MS) { repository.observeChats("unauth_uid").first() }
      fail("Expected FirebaseFirestoreException PERMISSION_DENIED for unauthenticated caller")
    } catch (t: Throwable) {
      val firestoreEx =
        generateSequence(t) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
      assertNotNull("Expected FirebaseFirestoreException in cause chain, got $t", firestoreEx)
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
    }
  }

  private companion object {
    const val ALICE_EMAIL = "alice@frutiger.test"
    const val BOB_EMAIL = "bob@frutiger.test"
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 4000L
  }
}
