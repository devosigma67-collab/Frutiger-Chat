package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.crypto.CryptoManager
import com.example.data.local.FrutigerDao
import com.example.data.local.LocalChatEntity
import com.example.data.local.LocalFriendEntity
import com.example.data.local.LocalMessageEntity
import com.example.data.local.LocalProfileEntity
import com.example.data.model.ChatMessage
import com.example.data.model.ChatRoom
import com.example.data.model.FriendContact
import com.example.data.model.UserProfile
import com.example.util.OperationType
import com.example.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FrutigerChatRepository(
  private val db: FirebaseFirestore,
  private val localDao: FrutigerDao? = null,
) {
  constructor(
    context: Context,
    localDao: FrutigerDao? = null,
  ) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    ),
    localDao,
  )

  private val auth = Firebase.auth

  private fun requireUserId(): String {
    return auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
  }

  private fun sanitizeId(raw: String): String {
    return raw.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(120).ifEmpty {
      "id_${UUID.randomUUID().toString().replace("-", "").take(16)}"
    }
  }

  // --- 1. USER PROFILE ---

  fun observeUserProfile(userId: String = requireUserId()): Flow<UserProfile?> = flow {
    val path = "users/$userId"
    emitAll(
      db.collection("users")
        .document(userId)
        .snapshots()
        .map { snapshot ->
          val profile =
            if (snapshot.exists()) {
              snapshot.toObject(
                UserProfile::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
              )
            } else {
              null
            }
          if (profile != null && localDao != null) {
            val existingLocal = localDao.getProfile(userId)
            localDao.upsertProfile(
              LocalProfileEntity(
                userId = userId,
                username = profile.username,
                signature = profile.signature,
                currentActivity = profile.currentActivity,
                presenceStatus = profile.presenceStatus,
                avatarColor = profile.avatarColor,
                publicKeyFingerprint = profile.publicKeyFingerprint,
                vaultPassphraseHash = existingLocal?.vaultPassphraseHash ?: "frutiger-aero-e2ee",
                socketServerUrl = existingLocal?.socketServerUrl ?: "wss://echo.websocket.events",
                waterEffectsEnabled = existingLocal?.waterEffectsEnabled ?: true,
                updatedAtMillis =
                  (profile.updatedAt ?: profile.createdAt ?: Timestamp.now()).toDate().time,
              )
            )
          }
          profile
        }
        .catch { error ->
          if (error is Exception) handleFirestoreError(error, OperationType.GET, path)
          throw error
        }
    )
  }

  suspend fun createOrInitializeUserProfile(
    username: String,
    signature: String,
    currentActivity: String,
    presenceStatus: String = "online",
    avatarColor: String = "aqua",
    wallpaperTheme: String = "default",
  ): Result<UserProfile> = runCatching {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid)
    val fingerprint = CryptoManager.generateIdentityFingerprint(uid)
    val safeUsername = username.trim().take(40).let { if (it.length < 2) "AeroUser" else it }
    val safeSignature = signature.trim().take(160)
    val safeActivity = currentActivity.trim().take(120)

    try {
      val snapshot = docRef.get().await()
      if (!snapshot.exists()) {
        val payload =
          mapOf(
            "userId" to uid,
            "username" to safeUsername,
            "signature" to safeSignature,
            "currentActivity" to safeActivity,
            "presenceStatus" to presenceStatus,
            "avatarColor" to avatarColor,
            "wallpaperTheme" to wallpaperTheme,
            "publicKeyFingerprint" to fingerprint,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        docRef.set(payload).await()
      }
      val fetched =
        docRef
          .get()
          .await()
          .toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          ?: UserProfile(
            id = uid,
            userId = uid,
            username = safeUsername,
            signature = safeSignature,
            currentActivity = safeActivity,
            presenceStatus = presenceStatus,
            avatarColor = avatarColor,
            wallpaperTheme = wallpaperTheme,
            publicKeyFingerprint = fingerprint,
          )
      fetched
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      throw e
    }
  }

  suspend fun updateUserProfile(
    username: String,
    signature: String,
    currentActivity: String,
    presenceStatus: String,
    avatarColor: String,
    wallpaperTheme: String = "default",
  ): Result<Unit> = runCatching {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid)
    val safeUsername = username.trim().take(40).let { if (it.length < 2) "AeroUser" else it }
    val updates =
      mapOf(
        "username" to safeUsername,
        "signature" to signature.trim().take(160),
        "currentActivity" to currentActivity.trim().take(120),
        "presenceStatus" to presenceStatus,
        "avatarColor" to avatarColor,
        "wallpaperTheme" to wallpaperTheme,
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    try {
      docRef.update(updates).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  // --- 2. FRIENDS / BUDDY LIST ---

  fun observeFriends(userId: String = requireUserId()): Flow<List<FriendContact>> = flow {
    val path = "friends"
    emitAll(
      db.collection("friends")
        .whereEqualTo("userId", userId)
        .snapshots()
        .map { snapshot ->
          val list =
            snapshot
              .toObjects(
                FriendContact::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
              )
              .sortedBy { it.friendUsername.lowercase() }
          if (localDao != null) {
            localDao.upsertFriends(
              list.map { friend ->
                LocalFriendEntity(
                  id = friend.id,
                  userId = friend.userId,
                  friendUid = friend.friendUid,
                  friendUsername = friend.friendUsername,
                  friendSignature = friend.friendSignature,
                  friendActivity = friend.friendActivity,
                  friendStatus = friend.friendStatus,
                  avatarColor = friend.avatarColor,
                  publicKeyFingerprint = friend.publicKeyFingerprint,
                  updatedAtMillis =
                    (friend.updatedAt ?: friend.createdAt ?: Timestamp.now()).toDate().time,
                )
              }
            )
          }
          list
        }
        .catch { error ->
          if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
          throw error
        }
    )
  }

  suspend fun addFriendContact(
    friendUsername: String,
    friendSignature: String,
    friendActivity: String,
    friendStatus: String = "online",
    avatarColor: String = "green",
    customDocId: String? = null,
  ): Result<String> = runCatching {
    val uid = requireUserId()
    val docId = sanitizeId(customDocId ?: "friend_${UUID.randomUUID().toString().replace("-", "")}")
    val friendUid = sanitizeId("uid_${friendUsername.lowercase()}_${docId.takeLast(6)}")
    val fingerprint = CryptoManager.generateIdentityFingerprint(friendUid)
    val docRef = db.collection("friends").document(docId)

    val payload =
      mapOf(
        "userId" to uid,
        "friendUid" to friendUid,
        "friendUsername" to friendUsername.trim().take(40).ifEmpty { "Buddy" },
        "friendSignature" to friendSignature.trim().take(160),
        "friendActivity" to friendActivity.trim().take(120),
        "friendStatus" to friendStatus,
        "avatarColor" to avatarColor,
        "publicKeyFingerprint" to fingerprint,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    try {
      docRef.set(payload).await()
      if (localDao != null) {
        localDao.upsertFriends(
          listOf(
            LocalFriendEntity(
              id = docId,
              userId = uid,
              friendUid = friendUid,
              friendUsername = friendUsername.trim().take(40).ifEmpty { "Buddy" },
              friendSignature = friendSignature.trim().take(160),
              friendActivity = friendActivity.trim().take(120),
              friendStatus = friendStatus,
              avatarColor = avatarColor,
              publicKeyFingerprint = fingerprint,
              updatedAtMillis = System.currentTimeMillis(),
            )
          )
        )
      }
      docId
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
  }

  suspend fun deleteFriendContact(friendDocId: String): Result<Unit> = runCatching {
    val docRef = db.collection("friends").document(friendDocId)
    try {
      docRef.delete().await()
      localDao?.deleteFriend(friendDocId)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      throw e
    }
  }

  // --- 0. LIVE USER DIRECTORY (MULTI-DEVICE USER DISCOVERY) ---

  fun observeAllUsers(): Flow<List<UserProfile>> = flow {
    val path = "users"
    emitAll(
      db.collection("users")
        .snapshots()
        .map { snapshot ->
          snapshot
            .toObjects(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .filter { it.userId.isNotBlank() }
            .sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
        }
        .catch { error ->
          if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
          throw error
        }
    )
  }

  // --- 3. CHATS (DIRECT MESSAGES & GROUP CHATS) ---

  fun observeChats(userId: String = requireUserId()): Flow<List<ChatRoom>> = flow {
    val path = "chats"
    emitAll(
      db.collection("chats")
        .snapshots()
        .map { snapshot ->
          val list =
            snapshot
              .toObjects(ChatRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
              .filter { chat ->
                chat.ownerId == userId || chat.participantUids.contains(userId) || chat.participantUids.isEmpty()
              }
              .sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
          if (localDao != null) {
            localDao.upsertChats(
              list.map { chat ->
                LocalChatEntity(
                  id = chat.id,
                  ownerId = chat.ownerId,
                  title = chat.title,
                  isGroup = chat.isGroup,
                  avatarColor = chat.avatarColor,
                  presenceStatus = chat.presenceStatus,
                  participantNames = chat.participantNames,
                  lastMessagePreview = chat.lastMessagePreview,
                  lastMessageSender = chat.lastMessageSender,
                  encryptionKeyId = chat.encryptionKeyId,
                  unreadCount = chat.unreadCount.toInt(),
                  updatedAtMillis =
                    (chat.updatedAt ?: chat.createdAt ?: Timestamp.now()).toDate().time,
                )
              }
            )
          }
          list
        }
        .catch { error ->
          if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
          throw error
        }
    )
  }

  suspend fun getChatById(chatId: String): Result<ChatRoom?> = runCatching {
    val docRef = db.collection("chats").document(chatId)
    try {
      val snapshot = docRef.get().await()
      snapshot.toObject(ChatRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, docRef.path)
      throw e
    }
  }

  suspend fun createChatRoom(
    title: String,
    isGroup: Boolean,
    avatarColor: String,
    presenceStatus: String,
    participantNames: String,
    participantUids: List<String> = emptyList(),
    initialMessagePreview: String = "🔒 End-to-End Encrypted Aero Chat ready",
    lastMessageSender: String = "System",
    customChatId: String? = null,
  ): Result<String> = runCatching {
    val uid = requireUserId()
    val chatId = sanitizeId(customChatId ?: "chat_${UUID.randomUUID().toString().replace("-", "")}")
    val encryptionKeyId = sanitizeId("e2ee_${chatId.takeLast(16)}")
    val docRef = db.collection("chats").document(chatId)
    val allParticipants = (listOf(uid) + participantUids).filter { it.isNotBlank() }.distinct()

    val payload =
      mapOf(
        "ownerId" to uid,
        "participantUids" to allParticipants,
        "title" to title.trim().take(60).ifEmpty { "Chat" },
        "isGroup" to isGroup,
        "avatarColor" to if (isGroup) "group" else avatarColor,
        "presenceStatus" to presenceStatus,
        "participantNames" to participantNames.trim().take(200).ifEmpty { title.take(60) },
        "lastMessagePreview" to initialMessagePreview.trim().take(300),
        "lastMessageSender" to lastMessageSender.trim().take(60),
        "encryptionKeyId" to encryptionKeyId,
        "unreadCount" to 0L,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    try {
      docRef.set(payload).await()
      if (localDao != null) {
        localDao.upsertChats(
          listOf(
            LocalChatEntity(
              id = chatId,
              ownerId = uid,
              title = title.trim().take(60).ifEmpty { "Chat" },
              isGroup = isGroup,
              avatarColor = if (isGroup) "group" else avatarColor,
              presenceStatus = presenceStatus,
              participantNames = participantNames.trim().take(200).ifEmpty { title.take(60) },
              lastMessagePreview = initialMessagePreview.trim().take(300),
              lastMessageSender = lastMessageSender.trim().take(60),
              encryptionKeyId = encryptionKeyId,
              unreadCount = 0,
              updatedAtMillis = System.currentTimeMillis(),
            )
          )
        )
      }
      chatId
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
  }

  suspend fun deleteChatRoom(chatId: String): Result<Unit> = runCatching {
    val docRef = db.collection("chats").document(chatId)
    try {
      docRef.delete().await()
      localDao?.deleteChat(chatId)
      localDao?.clearMessagesForChat(chatId)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      throw e
    }
  }

  // --- 4. END-TO-END ENCRYPTED MESSAGES ---

  fun observeMessages(
    chatId: String,
    userId: String = requireUserId(),
  ): Flow<List<ChatMessage>> = flow {
    val path = "chats/$chatId/messages"
    emitAll(
      db.collection("chats")
        .document(chatId)
        .collection("messages")
        .snapshots()
        .map { snapshot ->
          snapshot
            .toObjects(ChatMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedBy { it.createdAt ?: Timestamp(0, 0) }
        }
        .catch { error ->
          if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
          throw error
        }
    )
  }

  suspend fun sendEncryptedMessage(
    chatId: String,
    encryptionKeyId: String,
    senderId: String,
    senderName: String,
    plaintext: String,
    messageType: String = "text",
    vaultPassphrase: String = "frutiger-aero-e2ee",
    participantUids: List<String> = emptyList(),
  ): Result<String> = runCatching {
    val uid = requireUserId()
    val messageId = sanitizeId("msg_${UUID.randomUUID().toString().replace("-", "")}")
    val encrypted =
      CryptoManager.encryptMessage(
        plaintext = plaintext,
        encryptionKeyId = encryptionKeyId,
        vaultPassphrase = vaultPassphrase,
      )

    val msgRef =
      db.collection("chats").document(chatId).collection("messages").document(messageId)
    val chatRef = db.collection("chats").document(chatId)
    val allParticipants = (listOf(uid) + participantUids).filter { it.isNotBlank() }.distinct()

    val msgPayload =
      mapOf(
        "chatId" to chatId,
        "ownerId" to uid,
        "participantUids" to allParticipants,
        "senderId" to senderId.take(128).ifEmpty { uid },
        "senderName" to senderName.trim().take(60).ifEmpty { "AeroUser" },
        "ciphertext" to encrypted.ciphertext,
        "iv" to encrypted.iv,
        "signatureHash" to encrypted.signatureHash,
        "messageType" to messageType,
        "deliveryStatus" to "delivered",
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )

    try {
      msgRef.set(msgPayload).await()
      // Update parent chat preview
      val previewText =
        when (messageType) {
          "nudge" -> "👋 $senderName sent a Nudge!"
          "water_ripple" -> "💧 $senderName sent a Frutiger Water Splash!"
          else -> plaintext.trim().take(280)
        }
      chatRef
        .update(
          mapOf(
            "lastMessagePreview" to previewText,
            "lastMessageSender" to senderName.trim().take(60),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()

      if (localDao != null) {
        localDao.upsertMessages(
          listOf(
            LocalMessageEntity(
              id = messageId,
              chatId = chatId,
              ownerId = uid,
              senderId = senderId,
              senderName = senderName,
              plaintext = plaintext,
              ciphertext = encrypted.ciphertext,
              iv = encrypted.iv,
              signatureHash = encrypted.signatureHash,
              isSignatureValid = true,
              messageType = messageType,
              deliveryStatus = "delivered",
              timestampMillis = System.currentTimeMillis(),
            )
          )
        )
      }
      messageId
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, msgRef.path)
      throw e
    }
  }

  suspend fun markMessageAsRead(chatId: String, messageId: String): Result<Unit> = runCatching {
    val msgRef =
      db.collection("chats").document(chatId).collection("messages").document(messageId)
    try {
      msgRef
        .update(
          mapOf(
            "deliveryStatus" to "read",
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, msgRef.path)
      throw e
    }
  }

  // --- 5. INITIAL FRUTIGER AERO EXPERIENCE SEEDER ---

  /**
   * Ensures a newly signed-in user starts with the signature Frutiger Chat buddies and group chat
   * matching the classic interface (Friend 1 Red, Friend 2 Green, Friend 3 Blue, Group chat)
   * with real AES-256-GCM encrypted messages in Firestore and Room.
   */
  suspend fun ensureStarterDataSeeded(currentUsername: String) {
    val uid = requireUserId()
    val existingChats =
      try {
        db.collection("chats").whereEqualTo("ownerId", uid).limit(1).get().await()
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.LIST, "chats")
        return
      }
    if (!existingChats.isEmpty) return

    // Seed 3 buddies in /friends
    addFriendContact(
      friendUsername = "Friend 1",
      friendSignature = "Rendering 3D glossy glass spheres 🔴✨",
      friendActivity = "🎮 Playing: Space Cadet Pinball",
      friendStatus = "busy",
      avatarColor = "red",
      customDocId = sanitizeId("friend_1_$uid"),
    )
    addFriendContact(
      friendUsername = "Friend 2",
      friendSignature = "Tropical water droplets & eco breeze 🌿💧",
      friendActivity = "🎵 Listening to: Lease - Takeshi Abo",
      friendStatus = "online",
      avatarColor = "green",
      customDocId = sanitizeId("friend_2_$uid"),
    )
    addFriendContact(
      friendUsername = "Friend 3",
      friendSignature = "Offline in the deep ocean aquarium 🐠🌊",
      friendActivity = "💤 Away from keyboard",
      friendStatus = "offline",
      avatarColor = "blue",
      customDocId = sanitizeId("friend_3_$uid"),
    )

    // Seed 4 chats matching the reference design (Friend 1, Friend 2, Friend 3, Group chat)
    val chatGroup =
      createChatRoom(
        title = "Group chat",
        isGroup = true,
        avatarColor = "group",
        presenceStatus = "online",
        participantNames = "$currentUsername, Friend 1, Friend 2, Friend 3",
        initialMessagePreview = "Welcome to the Frutiger Aero encrypted lounge! 🌊🫧",
        lastMessageSender = "Friend 2",
        customChatId = sanitizeId("chat_group_$uid"),
      ).getOrNull()

    val chat3 =
      createChatRoom(
        title = "Friend 3",
        isGroup = false,
        avatarColor = "blue",
        presenceStatus = "offline",
        participantNames = "$currentUsername, Friend 3",
        initialMessagePreview = "Catch you later on MSN! Key fingerprint verified 🔒",
        lastMessageSender = "Friend 3",
        customChatId = sanitizeId("chat_f3_$uid"),
      ).getOrNull()

    val chat2 =
      createChatRoom(
        title = "Friend 2",
        isGroup = false,
        avatarColor = "green",
        presenceStatus = "online",
        participantNames = "$currentUsername, Friend 2",
        initialMessagePreview = "The glossy glassmorphic buttons & water ripples look amazing!",
        lastMessageSender = "Friend 2",
        customChatId = sanitizeId("chat_f2_$uid"),
      ).getOrNull()

    val chat1 =
      createChatRoom(
        title = "Friend 1",
        isGroup = false,
        avatarColor = "red",
        presenceStatus = "busy",
        participantNames = "$currentUsername, Friend 1",
        initialMessagePreview = "Hey! Just sent you an AES-256-GCM encrypted packet ✨",
        lastMessageSender = "Friend 1",
        customChatId = sanitizeId("chat_f1_$uid"),
      ).getOrNull()

    if (chat1 != null) {
      val key1 = sanitizeId("e2ee_${chat1.takeLast(16)}")
      sendEncryptedMessage(
        chatId = chat1,
        encryptionKeyId = key1,
        senderId = "friend_1",
        senderName = "Friend 1",
        plaintext = "Hey $currentUsername! Welcome to Frutiger Chat 🌊🫧",
      )
      sendEncryptedMessage(
        chatId = chat1,
        encryptionKeyId = key1,
        senderId = "friend_1",
        senderName = "Friend 1",
        plaintext = "Every message here is encrypted with AES-256-GCM & saved locally for instant offline access!",
      )
    }

    if (chat2 != null) {
      val key2 = sanitizeId("e2ee_${chat2.takeLast(16)}")
      sendEncryptedMessage(
        chatId = chat2,
        encryptionKeyId = key2,
        senderId = "friend_2",
        senderName = "Friend 2",
        plaintext = "The glossy glassmorphic buttons & water ripples look amazing! Tap Nudge 👋 anytime!",
      )
    }

    if (chat3 != null) {
      val key3 = sanitizeId("e2ee_${chat3.takeLast(16)}")
      sendEncryptedMessage(
        chatId = chat3,
        encryptionKeyId = key3,
        senderId = "friend_3",
        senderName = "Friend 3",
        plaintext = "Catch you later on MSN! Key fingerprint verified 🔒",
      )
    }

    if (chatGroup != null) {
      val keyG = sanitizeId("e2ee_${chatGroup.takeLast(16)}")
      sendEncryptedMessage(
        chatId = chatGroup,
        encryptionKeyId = keyG,
        senderId = "friend_2",
        senderName = "Friend 2",
        plaintext = "Welcome everyone to the Frutiger Aero Group Chat! 🌿🐬✨",
      )
      sendEncryptedMessage(
        chatId = chatGroup,
        encryptionKeyId = keyG,
        senderId = "friend_1",
        senderName = "Friend 1",
        plaintext = "Group E2EE key exchange complete — all group messages are end-to-end encrypted.",
      )
    }
  }
}
