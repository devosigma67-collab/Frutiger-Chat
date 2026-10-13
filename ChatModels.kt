package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
  data object Loading : UiState<Nothing>
  data class Success<T>(val data: T) : UiState<T>
  data class Error(val message: String) : UiState<Nothing>
}

@IgnoreExtraProperties
data class UserProfile(
  @DocumentId val id: String = "",
  val userId: String = "",
  val username: String = "AeroUser",
  val signature: String = "Custom signature text ✨💧",
  val currentActivity: String = "🎵 Listening to: Aquatic Ambience",
  val presenceStatus: String = "online",
  val avatarColor: String = "aqua",
  val wallpaperTheme: String = "default",
  val publicKeyFingerprint: String = "4F9A:82C1:E03B:77D4:19A8:55F2",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class FriendContact(
  @DocumentId val id: String = "",
  val userId: String = "",
  val friendUid: String = "",
  val friendUsername: String = "",
  val friendSignature: String = "",
  val friendActivity: String = "",
  val friendStatus: String = "online",
  val avatarColor: String = "green",
  val publicKeyFingerprint: String = "8A1B:44C9:09E2:3F71:62D0:CC18",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class ChatRoom(
  @DocumentId val id: String = "",
  val ownerId: String = "",
  val participantUids: List<String> = emptyList(),
  val title: String = "",
  val isGroup: Boolean = false,
  val avatarColor: String = "green",
  val presenceStatus: String = "online",
  val participantNames: String = "",
  val lastMessagePreview: String = "",
  val lastMessageSender: String = "",
  val encryptionKeyId: String = "aero_key_default",
  val unreadCount: Long = 0L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  val isRealGroup: Boolean
    get() = isGroup || avatarColor == "group" || participantUids.size > 2
}

@IgnoreExtraProperties
data class ChatMessage(
  @DocumentId val id: String = "",
  val chatId: String = "",
  val ownerId: String = "",
  val participantUids: List<String> = emptyList(),
  val senderId: String = "",
  val senderName: String = "",
  val ciphertext: String = "",
  val iv: String = "",
  val signatureHash: String = "",
  val messageType: String = "text",
  val deliveryStatus: String = "sent",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

data class DecryptedMessage(
  val id: String,
  val chatId: String,
  val senderId: String,
  val senderName: String,
  val plaintext: String,
  val ciphertext: String,
  val iv: String,
  val signatureHash: String,
  val isSignatureValid: Boolean,
  val messageType: String,
  val deliveryStatus: String,
  val timestampMillis: Long,
  val isFromMe: Boolean,
)
