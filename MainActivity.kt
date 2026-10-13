package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.local.FrutigerDatabase
import com.example.data.model.UiState
import com.example.data.repository.FrutigerChatRepository
import com.example.network.SocketIoManager
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.PendingRegistrationData
import com.example.ui.auth.signOut
import com.example.ui.components.FrutigerAeroWallpaperBox
import com.example.ui.screens.ChatsTabScreen
import com.example.ui.screens.ConversationScreen
import com.example.ui.screens.FriendsTabScreen
import com.example.ui.screens.ProfileTabScreen
import com.example.ui.screens.SettingsTabScreen
import com.example.ui.theme.AeroAquaCyan
import com.example.ui.theme.AeroIceWhite
import com.example.ui.theme.AeroSoftMint
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.ChatViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        AppNavigation()
      }
    }
  }
}

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
  val listener =
    FirebaseAuth.AuthStateListener { auth ->
      trySend(auth.currentUser)
    }
  addAuthStateListener(listener)
  awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
  val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
  var pendingRegistration by remember { mutableStateOf<PendingRegistrationData?>(null) }

  val user = currentUser
  if (user == null) {
    AuthScreen(
      onAuthCompleted = { regData ->
        pendingRegistration = regData
      }
    )
  } else {
    FrutigerMainScreen(
      currentUserId = user.uid,
      initialRegistration = pendingRegistration,
    )
  }
}

private enum class FrutigerTab(
  val label: String,
  val icon: ImageVector,
  val testTag: String,
) {
  CHATS("Chats", Icons.Default.Forum, "nav_tab_chats"),
  FRIENDS("Friends", Icons.Default.Group, "nav_tab_friends"),
  PROFILE("Profile", Icons.Default.AccountCircle, "nav_tab_profile"),
  SETTINGS("Settings", Icons.Default.Settings, "nav_tab_settings"),
}

@Composable
fun FrutigerMainScreen(
  currentUserId: String,
  initialRegistration: PendingRegistrationData?,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember { CredentialManager.create(context) }

  val viewModel: ChatViewModel =
    viewModel(
      key = currentUserId,
      factory =
        viewModelFactory {
          initializer {
            val app =
              checkNotNull(this[APPLICATION_KEY]) {
                "APPLICATION_KEY missing from CreationExtras"
              }
            val databaseId = app.getString(R.string.firestore_database_id)
            val db = FirebaseFirestore.getInstance(databaseId)
            val localDb = FrutigerDatabase.getInstance(app)
            val localDao = localDb.frutigerDao()
            val repo = FrutigerChatRepository(db, localDao)
            val socketIo = SocketIoManager()
            ChatViewModel(
              repository = repo,
              localDao = localDao,
              socketIoManager = socketIo,
              currentUserId = currentUserId,
              initialRegistration = initialRegistration,
            )
          }
        },
    )

  val userProfileState by viewModel.userProfileState.collectAsStateWithLifecycle()
  val chatsState by viewModel.chatsState.collectAsStateWithLifecycle()
  val friendsState by viewModel.friendsState.collectAsStateWithLifecycle()
  val allUsersState by viewModel.allUsersState.collectAsStateWithLifecycle()
  val matchedUsers by viewModel.matchedUsersState.collectAsStateWithLifecycle()
  val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
  val activeChatRoom by viewModel.activeChatRoom.collectAsStateWithLifecycle()
  val activeMessagesState by viewModel.activeMessagesState.collectAsStateWithLifecycle()
  val typingBuddyName by viewModel.typingBuddyName.collectAsStateWithLifecycle()
  val nudgeTriggerCount by viewModel.nudgeTriggerCount.collectAsStateWithLifecycle()
  val waterEffectsEnabled by viewModel.waterEffectsEnabled.collectAsStateWithLifecycle()
  val socketServerUrl by viewModel.socketServerUrl.collectAsStateWithLifecycle()
  val socketConnectionState by viewModel.socketConnectionState.collectAsStateWithLifecycle()
  val socketPacketCounter by viewModel.socketPacketCounter.collectAsStateWithLifecycle()
  val cachedChatsCount by viewModel.cachedChatsCount.collectAsStateWithLifecycle()
  val cachedMessagesCount by viewModel.cachedMessagesCount.collectAsStateWithLifecycle()
  val statusBannerMessage by viewModel.statusBannerMessage.collectAsStateWithLifecycle()

  var currentTab by rememberSaveable { mutableStateOf(FrutigerTab.CHATS) }

  BackHandler(enabled = activeChatRoom != null || currentTab != FrutigerTab.CHATS) {
    if (activeChatRoom != null) {
      viewModel.closeActiveChat()
    } else {
      currentTab = FrutigerTab.CHATS
    }
  }

  val notificationPermissionLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.RequestPermission(),
      onResult = {},
    )

  LaunchedEffect(Unit) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
      }
    }
  }

  // Handle launch from notification intent
  val incomingChatId = remember {
    (context as? ComponentActivity)?.intent?.getStringExtra("EXTRA_OPEN_CHAT_ID")
  }
  LaunchedEffect(incomingChatId) {
    if (!incomingChatId.isNullOrBlank()) {
      viewModel.openChat(incomingChatId)
    }
  }

  val currentWallpaperTheme = (userProfileState as? UiState.Success)?.data?.wallpaperTheme ?: "default"

  FrutigerAeroWallpaperBox(
    waterEffectsEnabled = waterEffectsEnabled,
    wallpaperTheme = currentWallpaperTheme,
  ) {
    Column(
      modifier =
        Modifier.fillMaxSize()
          .windowInsetsPadding(WindowInsets.statusBars),
    ) {
      // Optional feedback toast banner
      AnimatedVisibility(visible = statusBannerMessage != null) {
        Surface(
          color = Color(0xDD065F46),
          shape = RoundedCornerShape(12.dp),
          border = BorderStroke(1.dp, AeroAquaCyan),
          modifier =
            Modifier.fillMaxWidth()
              .padding(horizontal = 14.dp, vertical = 4.dp)
              .clickable { viewModel.dismissStatusBanner() },
        ) {
          Text(
            text = "✨ ${statusBannerMessage ?: ""}",
            style = MaterialTheme.typography.labelLarge,
            color = AeroIceWhite,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
          )
        }
      }

      // Main Content Area
      Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
        val openRoom = activeChatRoom
        if (openRoom != null) {
          val myProfile = (userProfileState as? UiState.Success)?.data
          val myDiscoveredUsers = (allUsersState as? UiState.Success)?.data ?: emptyList()
          ConversationScreen(
            chatRoom = openRoom,
            messagesState = activeMessagesState,
            typingBuddyName = typingBuddyName,
            nudgeTriggerCount = nudgeTriggerCount,
            currentUserId = myProfile?.userId ?: viewModel.currentUserId,
            currentUsername = myProfile?.username ?: "AeroWave",
            allUsers = myDiscoveredUsers,
            onBack = { viewModel.closeActiveChat() },
            onSendMessage = { text, type -> viewModel.sendMessage(text, type) },
            onSendNudge = { viewModel.sendNudgeToChat(openRoom) },
          )
        } else {
          when (currentTab) {
            FrutigerTab.CHATS ->
              ChatsTabScreen(
                userProfileState = userProfileState,
                chatsState = chatsState,
                friendsState = friendsState,
                allUsersState = allUsersState,
                matchedUsers = matchedUsers,
                searchQuery = searchQuery,
                onSearchQueryChanged = viewModel::updateSearchQuery,
                onCyclePresenceStatus = viewModel::cyclePresenceStatus,
                onUpdateProfileQuick = viewModel::updateProfile,
                onOpenChat = viewModel::openChat,
                onStartOrOpenChatWithUser = viewModel::startOrOpenChatWithUser,
                onCreateDirectMessage = { name, color, status ->
                  viewModel.createNewDirectMessage(name, color, status, openImmediately = true)
                },
                onCreateGroupChat = { title, members ->
                  viewModel.createNewGroupChat(title, members, openImmediately = true)
                },
                onSendNudge = viewModel::sendNudgeToChat,
                onDeleteChat = viewModel::deleteChat,
              )

            FrutigerTab.FRIENDS ->
              FriendsTabScreen(
                friendsState = friendsState,
                allUsersState = allUsersState,
                onStartDirectChat = { name, color, status ->
                  currentTab = FrutigerTab.CHATS
                  viewModel.createNewDirectMessage(name, color, status, openImmediately = true)
                },
                onStartChatWithUser = { user ->
                  currentTab = FrutigerTab.CHATS
                  viewModel.startOrOpenChatWithUser(user, openImmediately = true)
                },
                onAddFriend = viewModel::addFriend,
                onDeleteFriend = viewModel::deleteFriend,
              )

            FrutigerTab.PROFILE ->
              ProfileTabScreen(
                userProfileState = userProfileState,
                onSaveProfile = viewModel::updateProfile,
              )

            FrutigerTab.SETTINGS ->
              SettingsTabScreen(
                waterEffectsEnabled = waterEffectsEnabled,
                socketServerUrl = socketServerUrl,
                socketConnectionState = socketConnectionState,
                socketPacketCounter = socketPacketCounter,
                cachedChatsCount = cachedChatsCount,
                cachedMessagesCount = cachedMessagesCount,
                onToggleWaterEffects = viewModel::toggleWaterEffects,
                onUpdateSocketUrl = viewModel::updateSocketServerUrl,
                onUpdateVaultPassphrase = viewModel::updateVaultPassphrase,
                onSignOut = {
                  signOut(
                    context = context,
                    credentialManager = credentialManager,
                    onSignOutComplete = {},
                    scope = scope,
                  )
                },
              )
          }
        }
      }

      // Glossy Bottom Navigation Bar matching the reference screenshot (Chats | Friends | Profile | Settings)
      if (activeChatRoom == null) {
        FrutigerGlossyBottomNavBar(
          selectedTab = currentTab,
          onSelectTab = { tab -> currentTab = tab },
        )
      }
    }
  }
}

@Composable
private fun FrutigerGlossyBottomNavBar(
  selectedTab: FrutigerTab,
  onSelectTab: (FrutigerTab) -> Unit,
) {
  Box(
    modifier =
      Modifier.fillMaxWidth()
        .background(
          brush =
            Brush.verticalGradient(
              colors =
                listOf(
                  Color(0xD90A3522),
                  Color(0xF2041A10),
                  Color(0xFF021009),
                )
            )
        )
        .border(
          width = 1.dp,
          brush =
            Brush.verticalGradient(
              colors = listOf(Color(0x886EE7B7), Color.Transparent)
            ),
          shape = androidx.compose.ui.graphics.RectangleShape,
        )
        .windowInsetsPadding(WindowInsets.navigationBars)
        .padding(horizontal = 16.dp, vertical = 6.dp),
  ) {
    Row(
      horizontalArrangement = Arrangement.SpaceAround,
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.fillMaxWidth(),
    ) {
      FrutigerTab.entries.forEach { tab ->
        val isSelected = selectedTab == tab
        val pillShape = RoundedCornerShape(16.dp)

        val itemModifier =
          if (isSelected) {
            Modifier.shadow(8.dp, pillShape)
              .clip(pillShape)
              .background(
                brush =
                  Brush.verticalGradient(
                    colors =
                      listOf(
                        Color(0xCC2C7A68),
                        Color(0xCC134E3A),
                        Color(0xEE0B3325),
                      )
                  ),
                shape = pillShape,
              )
              .border(
                width = 1.dp,
                brush =
                  Brush.verticalGradient(
                    colors = listOf(Color.White.copy(alpha = 0.75f), AeroAquaCyan.copy(alpha = 0.45f))
                  ),
                shape = pillShape,
              )
          } else {
            Modifier.clip(pillShape)
          }

        Box(
          contentAlignment = Alignment.Center,
          modifier =
            itemModifier
              .clickable { onSelectTab(tab) }
              .padding(horizontal = 16.dp, vertical = 6.dp)
              .testTag(tab.testTag),
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
              imageVector = tab.icon,
              contentDescription = tab.label,
              tint = AeroIceWhite,
              modifier = Modifier.size(28.dp),
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = tab.label,
              color = if (isSelected) AeroIceWhite else AeroSoftMint,
              fontSize = 12.sp,
              fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            )
          }
        }
      }
    }
  }
}
