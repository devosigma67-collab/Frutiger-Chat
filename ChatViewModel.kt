package com.example.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AeroAiService
import com.example.crypto.CryptoManager
import com.example.data.local.FrutigerDao
import com.example.data.local.LocalProfileEntity
import com.example.data.model.ChatMessage
import com.example.data.model.ChatRoom
import com.example.data.model.DecryptedMessage
import com.example.data.model.FriendContact
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.repository.FrutigerChatRepository
import com.example.network.SocketConnectionState
import com.example.network.SocketIoManager
import com.example.network.SocketRealtimeEvent
import com.example.ui.auth.PendingRegistrationData
import com.example.util.AeroNotificationManager
import com.google.firebase.Timestamp
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModel(
  private val repository: FrutigerChatRepository,
  private val localDao: FrutigerDao,
  private val socketIoManager: SocketIoManager,
  val currentUserId: String,
  initialRegistration: PendingRegistrationData? = null,
  val appContext: Context? = null,
) : ViewModel() {

  private val _searchQuery = MutableStateFlow("")
  val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

  private val _selectedChatId = MutableStateFlow<String?>(null)
  val selectedChatId: StateFlow<String?> = _selectedChatId.asStateFlow()

  private val _vaultPassphrase =
    MutableStateFlow(initialRegistration?.vaultPassword ?: "frutiger-aero-e2ee")
  val vaultPassphrase: StateFlow<String> = _vaultPassphrase.asStateFlow()

  private val _waterEffectsEnabled = MutableStateFlow(true)
  val waterEffectsEnabled: StateFlow<Boolean> = _waterEffectsEnabled.asStateFlow()

  private val _socketServerUrl = MutableStateFlow(SocketIoManager.DEFAULT_SOCKET_URL)
  val socketServerUrl: StateFlow<String> = _socketServerUrl.asStateFlow()

  private val _nudgeTriggerCount = MutableStateFlow(0)
  val nudgeTriggerCount: StateFlow<Int> = _nudgeTriggerCount.asStateFlow()

  private val _typingBuddyName = MutableStateFlow<String?>(null)
  val typingBuddyName: StateFlow<String?> = _typingBuddyName.asStateFlow()

  private val _statusBannerMessage = MutableStateFlow<String?>(null)
  val statusBannerMessage: StateFlow<String?> = _statusBannerMessage.asStateFlow()

  val socketConnectionState: StateFlow<SocketConnectionState> = socketIoManager.connectionState
  val socketPacketCounter: StateFlow<Int> = socketIoManager.packetCounter

  // Two-tier Flow error handling for UserProfile
  val userProfileState: StateFlow<UiState<UserProfile>> =
    repository
      .observeUserProfile(currentUserId)
      .map<UserProfile?, UiState<UserProfile>> { profile ->
        UiState.Success(
          profile
            ?: UserProfile(
              id = currentUserId,
              userId = currentUserId,
              username = initialRegistration?.username ?: "AeroWave",
              signature = initialRegistration?.signature ?: "Custom signature text ✨💧",
              currentActivity = "🎵 Listening to: Aquatic Ambience",
              presenceStatus = "online",
              avatarColor = initialRegistration?.avatarColor ?: "aqua",
              publicKeyFingerprint = CryptoManager.generateIdentityFingerprint(currentUserId),
            )
        )
      }
      .catch { error ->
        Log.w(TAG, "Error observing user profile", error)
        emit(UiState.Error(error.message ?: "Failed to load profile"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  // Two-tier Flow error handling for Chats
  private val rawChatsState: StateFlow<UiState<List<ChatRoom>>> =
    repository
      .observeChats(currentUserId)
      .map<List<ChatRoom>, UiState<List<ChatRoom>>> { UiState.Success(it) }
      .catch { error ->
        Log.w(TAG, "Error observing chats", error)
        emit(UiState.Error(error.message ?: "Failed to observe chats"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  val chatsState: StateFlow<UiState<List<ChatRoom>>> =
    combine(rawChatsState, _searchQuery) { state, query ->
        if (state is UiState.Success && query.isNotBlank()) {
          val q = query.trim().lowercase()
          UiState.Success(
            state.data.filter { chat ->
              chat.title.lowercase().contains(q) ||
                chat.lastMessagePreview.lowercase().contains(q) ||
                chat.participantNames.lowercase().contains(q)
            }
          )
        } else {
          state
        }
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  // Two-tier Flow error handling for Friends
  val friendsState: StateFlow<UiState<List<FriendContact>>> =
    repository
      .observeFriends(currentUserId)
      .map<List<FriendContact>, UiState<List<FriendContact>>> { UiState.Success(it) }
      .catch { error ->
        Log.w(TAG, "Error observing friends", error)
        emit(UiState.Error(error.message ?: "Failed to observe friends"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  // Two-tier Flow error handling for All Users (Live multi-device directory sync)
  val allUsersState: StateFlow<UiState<List<UserProfile>>> =
    repository
      .observeAllUsers()
      .map<List<UserProfile>, UiState<List<UserProfile>>> { users ->
        UiState.Success(users.filter { it.userId != currentUserId })
      }
      .catch { error ->
        Log.w(TAG, "Error observing all users", error)
        emit(UiState.Error(error.message ?: "Failed to observe users"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  // Matched Users for search query
  val matchedUsersState: StateFlow<List<UserProfile>> =
    combine(allUsersState, _searchQuery) { usersState, query ->
      if (usersState is UiState.Success && query.isNotBlank()) {
        val q = query.trim().lowercase()
        usersState.data.filter { user ->
          user.username.lowercase().contains(q) ||
            user.signature.lowercase().contains(q) ||
            user.currentActivity.lowercase().contains(q)
        }
      } else {
        emptyList()
      }
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = emptyList(),
    )

  // Local Room cache counters for Settings & Offline Privacy verification
  val cachedChatsCount: StateFlow<Int> =
    localDao
      .observeTotalCachedChatsCount(currentUserId)
      .catch { emit(0) }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), 0)

  val cachedMessagesCount: StateFlow<Int> =
    localDao
      .observeTotalCachedMessagesCount(currentUserId)
      .catch { emit(0) }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), 0)

  // Active ChatRoom
  val activeChatRoom: StateFlow<ChatRoom?> =
    combine(rawChatsState, _selectedChatId) { state, selectedId ->
        if (state is UiState.Success && selectedId != null) {
          state.data.find { it.id == selectedId }
        } else {
          null
        }
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

  // Active Chat Decrypted Messages
  val activeMessagesState: StateFlow<UiState<List<DecryptedMessage>>> =
    combine(_selectedChatId, activeChatRoom, _vaultPassphrase) { chatId, chatRoom, passphrase ->
        Triple(chatId, chatRoom?.encryptionKeyId ?: "aero_key_default", passphrase)
      }
      .flatMapLatest { (chatId, encryptionKeyId, passphrase) ->
        if (chatId == null) {
          flowOf<UiState<List<DecryptedMessage>>>(UiState.Success(emptyList()))
        } else {
          repository
            .observeMessages(chatId = chatId, userId = currentUserId)
            .map<List<ChatMessage>, UiState<List<DecryptedMessage>>> { rawMessages ->
              val decrypted =
                rawMessages.map { msg ->
                  val plain =
                    CryptoManager.decryptMessage(
                      ciphertextBase64 = msg.ciphertext,
                      ivBase64 = msg.iv,
                      encryptionKeyId = encryptionKeyId,
                      vaultPassphrase = "frutiger-aero-e2ee",
                    )
                  val validSig =
                    CryptoManager.verifySignature(
                      plaintext = plain,
                      encryptionKeyId = encryptionKeyId,
                      expectedSignature = msg.signatureHash,
                    )
                  val ts = (msg.createdAt ?: msg.updatedAt ?: Timestamp.now()).toDate().time
                  DecryptedMessage(
                    id = msg.id,
                    chatId = msg.chatId,
                    senderId = msg.senderId,
                    senderName = msg.senderName,
                    plaintext = plain,
                    ciphertext = msg.ciphertext,
                    iv = msg.iv,
                    signatureHash = msg.signatureHash,
                    isSignatureValid = validSig,
                    messageType = msg.messageType,
                    deliveryStatus = msg.deliveryStatus,
                    timestampMillis = ts,
                    isFromMe = msg.senderId == currentUserId,
                  )
                }
              UiState.Success(decrypted)
            }
            .catch { error ->
              Log.w(TAG, "Error observing messages", error)
              emit(UiState.Error(error.message ?: "Failed to load messages"))
            }
        }
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading,
      )

  init {
    viewModelScope.launch {
      val initUsername = initialRegistration?.username ?: "AeroWave"
      val initSignature =
        initialRegistration?.signature ?: "Custom signature text ✨💧"
      val initAvatarColor = initialRegistration?.avatarColor ?: "aqua"

      val profileRes =
        repository.createOrInitializeUserProfile(
          username = initUsername,
          signature = initSignature,
          currentActivity = "🎵 Listening to: Lease - Takeshi Abo",
          presenceStatus = "online",
          avatarColor = initAvatarColor,
        )
      val activeName = profileRes.getOrNull()?.username ?: initUsername
      socketIoManager.connect(
        serverUrl = _socketServerUrl.value,
        userId = currentUserId,
        username = activeName,
      )
      repository.ensureStarterDataSeeded(activeName)
    }

    viewModelScope.launch {
      socketIoManager.incomingEvents.collect { event: SocketRealtimeEvent ->
        if (event.eventName == "nudge") {
          _nudgeTriggerCount.value += 1
          appContext?.let { ctx ->
            AeroNotificationManager.showIncomingMessageNotification(
              context = ctx,
              chatId = event.chatId,
              senderName = event.senderName,
              messageText = "👋 Sent you a Frutiger Nudge!",
              isSpecialEffect = true,
            )
          }
        }
      }
    }

    // Android System Notification listener for incoming messages from other buddies
    viewModelScope.launch {
      var initialChatsLoaded = false
      val seenPreviews = mutableSetOf<String>()
      repository.observeChats(currentUserId).collect { chatsList ->
        if (!initialChatsLoaded) {
          initialChatsLoaded = true
          chatsList.forEach { c ->
            seenPreviews.add("${c.id}_${c.lastMessagePreview}")
          }
          return@collect
        }

        val myName = (userProfileState.value as? UiState.Success)?.data?.username ?: "AeroWave"
        chatsList.forEach { c ->
          val key = "${c.id}_${c.lastMessagePreview}"
          if (!seenPreviews.contains(key) && c.lastMessagePreview.isNotBlank()) {
            seenPreviews.add(key)
            if (c.lastMessageSender.isNotBlank() && !c.lastMessageSender.equals(myName, ignoreCase = true)) {
              appContext?.let { ctx ->
                val isNudge = c.lastMessagePreview.contains("sent a Nudge") || c.lastMessagePreview.contains("👋")
                AeroNotificationManager.showIncomingMessageNotification(
                  context = ctx,
                  chatId = c.id,
                  senderName = c.lastMessageSender,
                  messageText = c.lastMessagePreview,
                  isSpecialEffect = isNudge,
                )
              }
            }
          }
        }
      }
    }
  }

  fun updateSearchQuery(query: String) {
    _searchQuery.value = query
  }

  fun openChat(chatId: String) {
    _selectedChatId.value = chatId
  }

  fun closeActiveChat() {
    _selectedChatId.value = null
  }

  fun dismissStatusBanner() {
    _statusBannerMessage.value = null
  }

  fun cyclePresenceStatus() {
    val currentProfile = (userProfileState.value as? UiState.Success)?.data ?: return
    val nextStatus =
      when (currentProfile.presenceStatus.lowercase()) {
        "online" -> "away"
        "away" -> "busy"
        "busy" -> "offline"
        else -> "online"
      }
    updateProfile(
      username = currentProfile.username,
      signature = currentProfile.signature,
      currentActivity = currentProfile.currentActivity,
      presenceStatus = nextStatus,
      avatarColor = currentProfile.avatarColor,
      wallpaperTheme = currentProfile.wallpaperTheme,
    )
  }

  fun updateProfile(
    username: String,
    signature: String,
    currentActivity: String,
    presenceStatus: String,
    avatarColor: String,
    wallpaperTheme: String = "default",
  ) {
    viewModelScope.launch {
      val result =
        repository.updateUserProfile(
          username = username,
          signature = signature,
          currentActivity = currentActivity,
          presenceStatus = presenceStatus,
          avatarColor = avatarColor,
          wallpaperTheme = wallpaperTheme,
        )
      if (result.isSuccess) {
        socketIoManager.emitEvent(
          eventName = "presence_update",
          chatId = "global",
          senderName = username,
          payload = "$presenceStatus • $signature",
        )
        _statusBannerMessage.value = "Profile & wallpaper updated ✨"
      }
    }
  }

  fun openAeroAiChat() {
    viewModelScope.launch {
      val existingChats = (rawChatsState.value as? UiState.Success<List<ChatRoom>>)?.data ?: emptyList()
      val aiChat = existingChats.find { it.id == "chat_aero_ai" || it.title.equals("Aero AI", ignoreCase = true) }
      if (aiChat != null) {
        _selectedChatId.value = aiChat.id
        return@launch
      }

      val myName = (userProfileState.value as? UiState.Success)?.data?.username ?: "AeroWave"
      val res =
        repository.createChatRoom(
          title = "Aero AI",
          isGroup = false,
          avatarColor = "aqua",
          presenceStatus = "online",
          participantNames = "$myName, Aero AI",
          participantUids = listOf(currentUserId, AeroAiService.AI_USER_ID),
          initialMessagePreview = "Hey! 🫧 I'm Aero AI, your intelligent companion. Ask me anything!",
          lastMessageSender = "Aero AI",
          customChatId = "chat_aero_ai",
        )
      res.onSuccess { chatId ->
        val keyId = "e2ee_aero_ai_key"
        repository.sendEncryptedMessage(
          chatId = chatId,
          encryptionKeyId = keyId,
          senderId = AeroAiService.AI_USER_ID,
          senderName = AeroAiService.AI_USERNAME,
          plaintext = "Hello! 🫧 I'm Aero AI, your intelligent Frutiger companion. What would you like to explore today? Ask me questions, generate status signatures, or chat about anything! ✨💧",
          participantUids = listOf(currentUserId, AeroAiService.AI_USER_ID),
        )
        _selectedChatId.value = chatId
      }
    }
  }

  fun startOrOpenChatWithUser(user: UserProfile, openImmediately: Boolean = true) {
    viewModelScope.launch {
      val existingChats = (rawChatsState.value as? UiState.Success<List<ChatRoom>>)?.data ?: emptyList()
      val deterministicChatId = "dm_" + listOf(currentUserId, user.userId).sorted().joinToString("_")
      val existing =
        existingChats.find { chat ->
          !chat.isRealGroup && !chat.isGroup && chat.avatarColor != "group" && (
            chat.id == deterministicChatId ||
            (chat.participantUids.contains(user.userId) && chat.participantUids.contains(currentUserId) && chat.participantUids.size <= 2) ||
            chat.participantNames.split(",").map { it.trim() }.let { names ->
              names.size == 2 && names.any { it.equals(user.username, ignoreCase = true) }
            }
          )
        }

      if (existing != null) {
        if (openImmediately) {
          _selectedChatId.value = existing.id
        }
        return@launch
      }

      val myName = (userProfileState.value as? UiState.Success)?.data?.username ?: "AeroWave"
      val newChatResult =
        repository.createChatRoom(
          title = user.username,
          isGroup = false,
          avatarColor = user.avatarColor,
          presenceStatus = user.presenceStatus,
          participantNames = "$myName, ${user.username}",
          participantUids = listOf(currentUserId, user.userId),
          initialMessagePreview = "Hey ${user.username}! Welcome to Frutiger Chat 🌊✨",
          lastMessageSender = myName,
          customChatId = deterministicChatId,
        )

      newChatResult.onSuccess { chatId ->
        val keyId = "e2ee_${chatId.takeLast(16)}"
        repository.sendEncryptedMessage(
          chatId = chatId,
          encryptionKeyId = keyId,
          senderId = currentUserId,
          senderName = myName,
          plaintext = "Hey ${user.username}! Welcome to our Frutiger Aero channel 🌊✨",
          participantUids = listOf(currentUserId, user.userId),
        )
        // Also auto-add to friends so they permanently appear
        repository.addFriendContact(
          friendUsername = user.username,
          friendSignature = user.signature,
          friendActivity = user.currentActivity,
          friendStatus = user.presenceStatus,
          avatarColor = user.avatarColor,
        )
        if (openImmediately) {
          _selectedChatId.value = chatId
        }
      }
    }
  }

  fun createNewDirectMessage(
    friendName: String,
    avatarColor: String,
    presenceStatus: String = "online",
    openImmediately: Boolean = true,
  ) {
    viewModelScope.launch {
      val discoveredUsers = (allUsersState.value as? UiState.Success)?.data ?: emptyList()
      val matchedUser = discoveredUsers.find { it.username.equals(friendName.trim(), ignoreCase = true) }
      if (matchedUser != null) {
        startOrOpenChatWithUser(matchedUser, openImmediately)
        return@launch
      }

      val existingChats = (rawChatsState.value as? UiState.Success<List<ChatRoom>>)?.data ?: emptyList()
      val targetUid = "uid_${friendName.lowercase().replace(" ", "_")}"
      val deterministicChatId = "dm_" + listOf(currentUserId, targetUid).sorted().joinToString("_")

      val existing =
        existingChats.find { chat ->
          !chat.isRealGroup && !chat.isGroup && chat.avatarColor != "group" && (
            chat.id == deterministicChatId ||
            (chat.participantUids.contains(targetUid) && chat.participantUids.contains(currentUserId) && chat.participantUids.size <= 2) ||
            chat.participantNames.split(",").map { it.trim() }.let { names ->
              names.size == 2 && names.any { it.equals(friendName.trim(), ignoreCase = true) }
            }
          )
        }

      if (existing != null) {
        if (openImmediately) {
          _selectedChatId.value = existing.id
        }
        return@launch
      }

      val myName =
        (userProfileState.value as? UiState.Success)?.data?.username ?: "AeroWave"

      val res =
        repository.createChatRoom(
          title = friendName,
          isGroup = false,
          avatarColor = avatarColor,
          presenceStatus = presenceStatus,
          participantNames = "$myName, $friendName",
          participantUids = listOf(currentUserId, targetUid),
          initialMessagePreview = "Hey $friendName! Glad we're connected on Frutiger Chat 🌊✨",
          lastMessageSender = myName,
          customChatId = deterministicChatId,
        )
      res.onSuccess { chatId ->
        val chat = repository.getChatById(chatId).getOrNull()
        val keyId = chat?.encryptionKeyId ?: "e2ee_${chatId.takeLast(16)}"
        repository.sendEncryptedMessage(
          chatId = chatId,
          encryptionKeyId = keyId,
          senderId = currentUserId,
          senderName = myName,
          plaintext = "Hey $friendName! Glad we're connected on Frutiger Chat 🌊✨",
          participantUids = listOf(currentUserId, targetUid),
        )
        repository.addFriendContact(
          friendUsername = friendName,
          friendSignature = "Frutiger buddy 🫧",
          friendActivity = "Online on Frutiger Chat",
          friendStatus = presenceStatus,
          avatarColor = avatarColor,
        )
        if (openImmediately) {
          _selectedChatId.value = chatId
        }
      }
    }
  }

  fun createNewGroupChat(
    groupTitle: String,
    memberNames: List<String>,
    openImmediately: Boolean = true,
  ) {
    viewModelScope.launch {
      val myName =
        (userProfileState.value as? UiState.Success)?.data?.username ?: "AeroWave"
      val allParticipants = (listOf(myName) + memberNames).distinct().joinToString(", ")
      val discoveredUsers = (allUsersState.value as? UiState.Success)?.data ?: emptyList()
      val memberUids = memberNames.map { name ->
        discoveredUsers.find { it.username.equals(name.trim(), ignoreCase = true) }?.userId
          ?: "uid_${name.lowercase().replace(" ", "_")}"
      }
      val allUids = (listOf(currentUserId) + memberUids).distinct()

      val res =
        repository.createChatRoom(
          title = groupTitle.ifBlank { "Aero Group Chat" },
          isGroup = true,
          avatarColor = "group",
          presenceStatus = "online",
          participantNames = allParticipants,
          participantUids = allUids,
          initialMessagePreview = "🔒 Group E2EE key ring initialized ($allParticipants)",
          lastMessageSender = myName,
        )
      res.onSuccess { chatId ->
        val chat = repository.getChatById(chatId).getOrNull()
        val keyId = chat?.encryptionKeyId ?: "e2ee_${chatId.takeLast(16)}"
        repository.sendEncryptedMessage(
          chatId = chatId,
          encryptionKeyId = keyId,
          senderId = currentUserId,
          senderName = myName,
          plaintext = "Welcome to $groupTitle! Group AES-256-GCM encryption is active 🫧🌿",
          participantUids = allUids,
        )
        if (openImmediately) {
          _selectedChatId.value = chatId
        }
      }
    }
  }

  fun addFriend(
    friendUsername: String,
    friendSignature: String,
    friendActivity: String,
    friendStatus: String,
    avatarColor: String,
  ) {
    viewModelScope.launch {
      val discoveredUsers = (allUsersState.value as? UiState.Success)?.data ?: emptyList()
      val matchedUser = discoveredUsers.find { it.username.equals(friendUsername.trim(), ignoreCase = true) }
      val targetColor = matchedUser?.avatarColor ?: avatarColor
      val targetStatus = matchedUser?.presenceStatus ?: friendStatus
      val targetSig = matchedUser?.signature ?: friendSignature
      val targetAct = matchedUser?.currentActivity ?: friendActivity

      val res =
        repository.addFriendContact(
          friendUsername = friendUsername,
          friendSignature = targetSig,
          friendActivity = targetAct,
          friendStatus = targetStatus,
          avatarColor = targetColor,
        )
      if (res.isSuccess) {
        _statusBannerMessage.value = "Added $friendUsername to your Frutiger Buddy List!"
        // Also ensure direct chat is ready so brother never hides
        createNewDirectMessage(
          friendName = friendUsername,
          avatarColor = targetColor,
          presenceStatus = targetStatus,
          openImmediately = false,
        )
      }
    }
  }

  fun deleteFriend(friendDocId: String) {
    viewModelScope.launch {
      repository.deleteFriendContact(friendDocId)
    }
  }

  fun deleteChat(chatId: String) {
    viewModelScope.launch {
      if (_selectedChatId.value == chatId) {
        _selectedChatId.value = null
      }
      repository.deleteChatRoom(chatId)
    }
  }

  fun sendMessage(plaintext: String, messageType: String = "text") {
    val chat = activeChatRoom.value ?: return
    val trimmed = plaintext.trim()
    if (trimmed.isEmpty()) return

    val myName =
      (userProfileState.value as? UiState.Success)?.data?.username ?: "AeroWave"

    viewModelScope.launch {
      socketIoManager.emitEvent(
        eventName = if (messageType == "nudge") "nudge" else "encrypted_message",
        chatId = chat.id,
        senderName = myName,
        payload = trimmed,
      )

      val sendRes =
        repository.sendEncryptedMessage(
          chatId = chat.id,
          encryptionKeyId = chat.encryptionKeyId,
          senderId = currentUserId,
          senderName = myName,
          plaintext = trimmed,
          messageType = messageType,
          participantUids = chat.participantUids,
        )

      if (sendRes.isSuccess) {
        val isAiChat = chat.id == "chat_aero_ai" ||
          chat.participantUids.contains(AeroAiService.AI_USER_ID) ||
          chat.title.equals("Aero AI", ignoreCase = true)

        if (isAiChat) {
          _typingBuddyName.value = "Aero AI"
          val aiReply = AeroAiService.generateAiResponse(trimmed)
          delay(650L)
          _typingBuddyName.value = null

          repository.sendEncryptedMessage(
            chatId = chat.id,
            encryptionKeyId = chat.encryptionKeyId,
            senderId = AeroAiService.AI_USER_ID,
            senderName = AeroAiService.AI_USERNAME,
            plaintext = aiReply,
            messageType = "text",
            participantUids = chat.participantUids,
          )

          appContext?.let { ctx ->
            AeroNotificationManager.showIncomingMessageNotification(
              context = ctx,
              chatId = chat.id,
              senderName = AeroAiService.AI_USERNAME,
              messageText = aiReply,
            )
          }
        } else {
          // Only trigger nostalgic starter simulation if chatting with seeded demo buddies
          val isStarterDemoBot = chat.title.startsWith("Friend ")
          if (isStarterDemoBot) {
            val responderName =
              if (chat.isRealGroup) {
                chat.participantNames
                  .split(",")
                  .map { it.trim() }
                  .firstOrNull { it.isNotEmpty() && !it.equals(myName, ignoreCase = true) }
                  ?: "Friend 2"
              } else {
                chat.title
              }

            _typingBuddyName.value = responderName
            delay(1200L)
            _typingBuddyName.value = null

            val replyText = generateNostalgicAeroReply(responderName, trimmed, messageType)
            repository.sendEncryptedMessage(
              chatId = chat.id,
              encryptionKeyId = chat.encryptionKeyId,
              senderId = "buddy_${responderName.lowercase().replace(" ", "_")}",
              senderName = responderName,
              plaintext = replyText,
              messageType = "text",
              participantUids = chat.participantUids,
            )
          }
        }
      }
    }
  }

  fun sendNudgeToChat(chat: ChatRoom) {
    val myName =
      (userProfileState.value as? UiState.Success)?.data?.username ?: "AeroWave"
    _nudgeTriggerCount.value += 1
    viewModelScope.launch {
      socketIoManager.emitEvent(
        eventName = "nudge",
        chatId = chat.id,
        senderName = myName,
        payload = "NUDGE_FRAME",
      )
      repository.sendEncryptedMessage(
        chatId = chat.id,
        encryptionKeyId = chat.encryptionKeyId,
        senderId = currentUserId,
        senderName = myName,
        plaintext = "👋 You just sent a Nudge!",
        messageType = "nudge",
      )
    }
  }

  fun toggleWaterEffects(enabled: Boolean) {
    _waterEffectsEnabled.value = enabled
    viewModelScope.launch {
      val existing = localDao.getProfile(currentUserId)
      if (existing != null) {
        localDao.upsertProfile(existing.copy(waterEffectsEnabled = enabled))
      }
    }
  }

  fun updateSocketServerUrl(newUrl: String) {
    _socketServerUrl.value = newUrl
    val myName =
      (userProfileState.value as? UiState.Success)?.data?.username ?: "AeroWave"
    socketIoManager.connect(
      serverUrl = newUrl,
      userId = currentUserId,
      username = myName,
    )
    _statusBannerMessage.value = "Socket.IO reconnected to $newUrl"
  }

  fun updateVaultPassphrase(newPassphrase: String) {
    if (newPassphrase.isNotBlank()) {
      _vaultPassphrase.value = newPassphrase.trim()
      _statusBannerMessage.value = "Local PBKDF2 AES-256-GCM key vault passphrase updated!"
    }
  }

  private fun generateNostalgicAeroReply(
    buddyName: String,
    userMessage: String,
    messageType: String,
  ): String {
    if (messageType == "nudge") {
      return "Whoa!! My whole Frutiger Aero window just shook! 👋😂"
    }
    if (messageType == "water_ripple") {
      return "Ooh that glossy water splash effect is pure 2007 bliss! 💧🌊✨"
    }
    val lower = userMessage.lowercase()
    return when {
      lower.contains("hello") || lower.contains("hey") || lower.contains("hi") ->
        "Hey there! Love your glossy status signature today ~ 🌿✨"
      lower.contains("encrypt") || lower.contains("key") || lower.contains("security") ->
        "Verified! Our AES-256-GCM cipher & SHA-256 signature hash match 100% 🔒"
      lower.contains("music") || lower.contains("song") || lower.contains("listen") ->
        "Currently blasting Lease by Takeshi Abo on Windows Media Player 11 🎵💿"
      else ->
        "Got your encrypted packet on $buddyName's terminal! Glossy & crystal clear 🫧🌊"
    }
  }

  override fun onCleared() {
    super.onCleared()
    socketIoManager.disconnect()
  }

  private companion object {
    const val TAG = "ChatViewModel"
    const val STOP_TIMEOUT_MILLIS = 5000L
  }
}
