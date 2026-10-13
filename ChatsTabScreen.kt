package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.crypto.CryptoManager
import com.example.data.model.ChatRoom
import com.example.data.model.FriendContact
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.ui.auth.aeroTextFieldColors
import com.example.ui.components.AeroGlassCard
import com.example.ui.components.AeroGlossyPillButton
import com.example.ui.components.E2eeSecurityBadge
import com.example.ui.components.GlossyBuddyAvatar
import com.example.ui.components.PresenceDot
import com.example.ui.theme.AeroAquaCyan
import com.example.ui.theme.AeroEmeraldBright
import com.example.ui.theme.AeroIceWhite
import com.example.ui.theme.AeroMutedMint
import com.example.ui.theme.AeroSoftMint
import com.example.ui.theme.RobotoMonoFontFamily
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatsTabScreen(
  userProfileState: UiState<UserProfile>,
  chatsState: UiState<List<ChatRoom>>,
  friendsState: UiState<List<FriendContact>>,
  allUsersState: UiState<List<UserProfile>> = UiState.Success(emptyList()),
  matchedUsers: List<UserProfile> = emptyList(),
  searchQuery: String,
  onSearchQueryChanged: (String) -> Unit,
  onCyclePresenceStatus: () -> Unit,
  onUpdateProfileQuick: (String, String, String, String, String) -> Unit,
  onOpenChat: (String) -> Unit,
  onOpenAeroAi: () -> Unit = {},
  onStartOrOpenChatWithUser: (UserProfile) -> Unit = {},
  onCreateDirectMessage: (String, String, String) -> Unit,
  onCreateGroupChat: (String, List<String>) -> Unit,
  onSendNudge: (ChatRoom) -> Unit,
  onDeleteChat: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  var showNewDmDialog by remember { mutableStateOf(false) }
  var showNewGroupDialog by remember { mutableStateOf(false) }
  var showQuickStatusDialog by remember { mutableStateOf(false) }

  val profile =
    (userProfileState as? UiState.Success)?.data
      ?: UserProfile(
        username = "Username",
        signature = "Custom signature text",
        currentActivity = "Current activity (if any)",
        presenceStatus = "away",
        avatarColor = "aqua",
      )

  val friendsList = (friendsState as? UiState.Success)?.data ?: emptyList()
  val allDiscoveredUsers = (allUsersState as? UiState.Success)?.data ?: emptyList()

  Column(
    modifier = modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 8.dp),
  ) {
    // 1. TOP USER PROFILE GLASSMORPHIC CARD (Matches screenshot layout)
    AeroGlassCard(
      cornerRadius = 18.dp,
      onClick = { showQuickStatusDialog = true },
      modifier = Modifier.fillMaxWidth().testTag("top_user_profile_card"),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp),
      ) {
        GlossyBuddyAvatar(
          colorKey = profile.avatarColor,
          size = 76.dp,
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Text(
              text = profile.username.ifBlank { "Username" },
              style = MaterialTheme.typography.headlineMedium,
              color = AeroIceWhite,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            PresenceDot(
              status = profile.presenceStatus,
              size = 13.dp,
              modifier =
                Modifier.clickable { onCyclePresenceStatus() }
                  .testTag("profile_presence_dot"),
            )
          }

          Spacer(modifier = Modifier.height(2.dp))

          Text(
            text = profile.signature.ifBlank { "Custom signature text" },
            style = MaterialTheme.typography.bodyLarge,
            color = AeroSoftMint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )

          Spacer(modifier = Modifier.height(3.dp))

          Text(
            text = profile.currentActivity.ifBlank { "Current activity (if any)" },
            style = MaterialTheme.typography.bodyMedium,
            color = AeroMutedMint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // 2. TWO SIDE-BY-SIDE GLOSSY PILL BUTTONS ("New direct message" | "New group chat")
    Row(
      horizontalArrangement = Arrangement.spacedBy(10.dp),
      modifier = Modifier.fillMaxWidth(),
    ) {
      AeroGlossyPillButton(
        text = "New direct message",
        onClick = { showNewDmDialog = true },
        accentTint = Color(0xFF3E6B52),
        modifier = Modifier.weight(1f).testTag("new_direct_message_button"),
      )
      AeroGlossyPillButton(
        text = "New group chat",
        onClick = { showNewGroupDialog = true },
        accentTint = Color(0xFF288E5F),
        modifier = Modifier.weight(1f).testTag("new_group_chat_button"),
      )
    }

    Spacer(modifier = Modifier.height(10.dp))

    // 3. FROSTED GLASS SEARCH BAR ("🔍 Search people or chats...")
    FrostedSearchBar(
      query = searchQuery,
      onQueryChanged = onSearchQueryChanged,
      modifier = Modifier.fillMaxWidth().testTag("chats_search_input"),
    )

    Spacer(modifier = Modifier.height(10.dp))

    // 4. CHATS & LIVE USER LIST
    when (chatsState) {
      is UiState.Loading -> {
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier.fillMaxWidth().weight(1f),
        ) {
          CircularProgressIndicator(color = AeroAquaCyan)
        }
      }
      is UiState.Error -> {
        AeroGlassCard(modifier = Modifier.fillMaxWidth()) {
          Text(
            text = chatsState.message,
            color = AeroSoftMint,
            modifier = Modifier.padding(16.dp),
          )
        }
      }
      is UiState.Success -> {
        val chats = chatsState.data
        val isSearching = searchQuery.isNotBlank()

        LazyColumn(
          verticalArrangement = Arrangement.spacedBy(10.dp),
          contentPadding = PaddingValues(bottom = 16.dp),
          modifier = Modifier.fillMaxWidth().weight(1f).testTag("chats_lazy_column"),
        ) {
          // SEARCH RESULTS: Matching registered users who logged in
          if (isSearching) {
            if (matchedUsers.isNotEmpty()) {
              item {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.padding(vertical = 4.dp),
                ) {
                  Text(
                    text = "👥 People on Frutiger Chat (${matchedUsers.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = AeroAquaCyan,
                    fontWeight = FontWeight.Bold,
                  )
                }
              }
              items(matchedUsers, key = { "user_${it.userId}" }) { discoveredUser ->
                LiveUserSearchResultCard(
                  user = discoveredUser,
                  onChatClick = { onStartOrOpenChatWithUser(discoveredUser) },
                )
              }
            }

            if (chats.isNotEmpty()) {
              item {
                Text(
                  text = "💬 Matching Conversations (${chats.size})",
                  style = MaterialTheme.typography.titleMedium,
                  color = AeroIceWhite,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                )
              }
              items(chats, key = { it.id }) { chat ->
                ChatListItemCard(
                  chat = chat,
                  currentUserId = profile.userId,
                  currentUsername = profile.username,
                  allUsers = allDiscoveredUsers,
                  onClick = { onOpenChat(chat.id) },
                  onSendNudge = { onSendNudge(chat) },
                  onDeleteChat = { onDeleteChat(chat.id) },
                )
              }
            } else if (matchedUsers.isEmpty()) {
              item {
                AeroGlassCard(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                  Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                  ) {
                    Text(
                      text = "No users or chats matching \"$searchQuery\"",
                      color = AeroIceWhite,
                      style = MaterialTheme.typography.titleMedium,
                    )
                  }
                }
              }
            }
          } else {
            // NORMAL VIEW (No active search query):
            // 4a. Live Sync Users Section (If other users have logged in/registered)
            if (allDiscoveredUsers.isNotEmpty()) {
              item {
                Column(modifier = Modifier.fillMaxWidth()) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                  ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Box(
                        modifier =
                          Modifier.size(8.dp)
                            .clip(CircleShape)
                            .background(AeroEmeraldBright)
                      )
                      Spacer(modifier = Modifier.width(6.dp))
                      Text(
                        text = "Active on Frutiger Chat • Live Sync",
                        style = MaterialTheme.typography.labelLarge,
                        color = AeroAquaCyan,
                        fontWeight = FontWeight.SemiBold,
                      )
                    }
                    Text(
                      text = "${allDiscoveredUsers.size} online",
                      style = MaterialTheme.typography.labelSmall,
                      color = AeroSoftMint,
                    )
                  }

                  Spacer(modifier = Modifier.height(6.dp))

                  androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                  ) {
                    items(allDiscoveredUsers, key = { "online_${it.userId}" }) { user ->
                      LiveSyncBuddyBubble(
                        user = user,
                        onClick = { onStartOrOpenChatWithUser(user) },
                      )
                    }
                  }
                }
              }
            }

            // 4b. Featured Aero AI Intelligent Assistant Card
            item {
              AeroGlassCard(
                cornerRadius = 16.dp,
                onClick = onOpenAeroAi,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 4.dp).testTag("aero_ai_quick_card"),
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.fillMaxWidth().padding(12.dp),
                ) {
                  Box {
                    GlossyBuddyAvatar(colorKey = "aqua", size = 48.dp)
                    Box(
                      modifier =
                        Modifier.size(16.dp)
                          .clip(CircleShape)
                          .background(Color(0xFF0284C7))
                          .align(Alignment.BottomEnd)
                          .border(1.5.dp, Color.White, CircleShape),
                      contentAlignment = Alignment.Center,
                    ) {
                      Text("✨", fontSize = 9.sp)
                    }
                  }
                  Spacer(modifier = Modifier.width(12.dp))
                  Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Text(
                        text = "Aero AI",
                        style = MaterialTheme.typography.titleMedium,
                        color = AeroIceWhite,
                        fontWeight = FontWeight.Bold,
                      )
                      Spacer(modifier = Modifier.width(6.dp))
                      Surface(
                        color = Color(0x660284C7),
                        shape = RoundedCornerShape(4.dp),
                      ) {
                        Text(
                          text = "AI BOT",
                          color = AeroAquaCyan,
                          fontSize = 9.sp,
                          fontWeight = FontWeight.Bold,
                          modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                        )
                      }
                    }
                    Text(
                      text = "Frutiger companion • Ask questions, get signatures & ideas 🫧",
                      style = MaterialTheme.typography.bodySmall,
                      color = AeroSoftMint,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                  }
                  AeroGlossyPillButton(
                    text = "Chat",
                    onClick = onOpenAeroAi,
                    accentTint = Color(0xFF0284C7),
                    modifier = Modifier.width(72.dp),
                  )
                }
              }
            }

            // 4c. Active Conversations Header & List
            if (chats.isEmpty()) {
              item {
                AeroGlassCard(
                  modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                ) {
                  Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                  ) {
                    GlossyBuddyAvatar(colorKey = "group", size = 64.dp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                      text = "No direct chats yet",
                      style = MaterialTheme.typography.titleMedium,
                      color = AeroIceWhite,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                      text = "Tap Aero AI above, any active buddy, or 'New direct message' to start chatting!",
                      style = MaterialTheme.typography.bodyMedium,
                      color = AeroSoftMint,
                    )
                  }
                }
              }
            } else {
              item {
                Text(
                  text = "Conversations",
                  style = MaterialTheme.typography.titleMedium,
                  color = AeroIceWhite,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
                )
              }

              items(chats, key = { it.id }) { chat ->
                ChatListItemCard(
                  chat = chat,
                  currentUserId = profile.userId,
                  currentUsername = profile.username,
                  allUsers = allDiscoveredUsers,
                  onClick = { onOpenChat(chat.id) },
                  onSendNudge = { onSendNudge(chat) },
                  onDeleteChat = { onDeleteChat(chat.id) },
                )
              }
            }
          }
        }
      }
    }
  }

  // Quick Edit Signature / Activity Dialog when tapping the Top Profile Card
  if (showQuickStatusDialog) {
    QuickEditProfileDialog(
      profile = profile,
      onDismiss = { showQuickStatusDialog = false },
      onSave = { newUsername, newSig, newAct, newStatus, newColor ->
        onUpdateProfileQuick(newUsername, newSig, newAct, newStatus, newColor)
        showQuickStatusDialog = false
      },
    )
  }

  // New Direct Message Dialog
  if (showNewDmDialog) {
    NewDirectMessageDialog(
      friends = friendsList,
      allUsers = allDiscoveredUsers,
      onDismiss = { showNewDmDialog = false },
      onCreateDm = { name, color, status ->
        onCreateDirectMessage(name, color, status)
        showNewDmDialog = false
      },
    )
  }

  // New Group Chat Dialog
  if (showNewGroupDialog) {
    NewGroupChatDialog(
      friends = friendsList,
      allUsers = allDiscoveredUsers,
      onDismiss = { showNewGroupDialog = false },
      onCreateGroup = { groupTitle, selectedMembers ->
        onCreateGroupChat(groupTitle, selectedMembers)
        showNewGroupDialog = false
      },
    )
  }
}

@Composable
private fun FrostedSearchBar(
  query: String,
  onQueryChanged: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val shape = RoundedCornerShape(50)
  Box(
    contentAlignment = Alignment.CenterStart,
    modifier =
      modifier
        .height(38.dp)
        .clip(shape)
        .background(
          brush =
            Brush.verticalGradient(
              colors =
                listOf(
                  Color(0x99354F44),
                  Color(0xAA263B31),
                )
            ),
          shape = shape,
        )
        .border(
          width = 1.dp,
          brush =
            Brush.verticalGradient(
              colors = listOf(Color(0x88A7F3D0), Color(0x446EE7B7))
            ),
          shape = shape,
        )
        .padding(horizontal = 12.dp),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Icon(
        imageVector = Icons.Default.Search,
        contentDescription = "Search chats",
        tint = AeroSoftMint,
        modifier = Modifier.size(18.dp),
      )
      Spacer(modifier = Modifier.width(8.dp))
      Box(modifier = Modifier.weight(1f)) {
        if (query.isEmpty()) {
          Text(
            text = "Search",
            style = MaterialTheme.typography.bodyMedium,
            color = AeroSoftMint.copy(alpha = 0.78f),
          )
        }
        BasicTextField(
          value = query,
          onValueChange = onQueryChanged,
          singleLine = true,
          textStyle =
            TextStyle(
              color = AeroIceWhite,
              fontSize = 14.sp,
            ),
          cursorBrush = SolidColor(AeroAquaCyan),
          modifier = Modifier.fillMaxWidth(),
        )
      }
      if (query.isNotEmpty()) {
        Icon(
          imageVector = Icons.Default.Close,
          contentDescription = "Clear search",
          tint = AeroSoftMint,
          modifier =
            Modifier.size(18.dp).clickable { onQueryChanged("") },
        )
      }
    }
  }
}

@Composable
private fun ChatListItemCard(
  chat: ChatRoom,
  currentUserId: String = "",
  currentUsername: String = "",
  allUsers: List<UserProfile> = emptyList(),
  onClick: () -> Unit,
  onSendNudge: () -> Unit,
  onDeleteChat: () -> Unit,
) {
  var menuExpanded by remember { mutableStateOf(false) }

  val isAeroAi = chat.id == "chat_aero_ai" || chat.title.equals("Aero AI", ignoreCase = true)
  val isGroupChat = chat.isRealGroup || chat.isGroup || chat.avatarColor == "group"

  // Resolve true display title, avatar, and presence for 1-on-1 chats
  val otherUser =
    remember(chat, allUsers, currentUserId, currentUsername, isAeroAi, isGroupChat) {
      if (isGroupChat || isAeroAi) {
        null
      } else {
        // Find other participant UID
        val otherUid = chat.participantUids.firstOrNull { it != currentUserId && it.isNotBlank() }
        if (otherUid != null) {
          allUsers.find { it.userId == otherUid }
        } else {
          // Fallback matching by participantNames
          val names = chat.participantNames.split(",").map { it.trim() }.filter { it.isNotBlank() }
          val otherName = names.firstOrNull { !it.equals(currentUsername, ignoreCase = true) }
          if (otherName != null) allUsers.find { it.username.equals(otherName, ignoreCase = true) } else null
        }
      }
    }

  val displayTitle =
    remember(chat, otherUser, currentUsername, isAeroAi, isGroupChat) {
      when {
        isAeroAi -> "Aero AI"
        isGroupChat -> chat.title.ifBlank { "Group Chat" }
        otherUser != null -> otherUser.username
        else -> {
          // 1-on-1 chat: determine the other person's name, never current user's name
          val names = chat.participantNames.split(",").map { it.trim() }.filter { it.isNotBlank() }
          val otherName = names.firstOrNull { !it.equals(currentUsername, ignoreCase = true) }
          when {
            otherName != null -> otherName
            chat.lastMessageSender.isNotBlank() && !chat.lastMessageSender.equals(currentUsername, ignoreCase = true) -> chat.lastMessageSender
            chat.title.isNotBlank() && !chat.title.equals(currentUsername, ignoreCase = true) -> chat.title
            else -> "Friend"
          }
        }
      }
    }

  val displayAvatarColor =
    remember(chat, otherUser, isAeroAi, isGroupChat) {
      when {
        isAeroAi -> "aqua"
        isGroupChat -> "group"
        else -> otherUser?.avatarColor ?: chat.avatarColor
      }
    }

  val displayPresenceStatus =
    remember(chat, otherUser, isAeroAi) {
      if (isAeroAi) "online" else (otherUser?.presenceStatus ?: chat.presenceStatus)
    }

  val formattedTime =
    remember(chat.updatedAt, chat.createdAt) {
      val date: Date? = (chat.updatedAt ?: chat.createdAt)?.toDate()
      if (date != null) {
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(date)
      } else {
        "Just now"
      }
    }

  AeroGlassCard(
    cornerRadius = 15.dp,
    onClick = onClick,
    modifier = Modifier.fillMaxWidth().testTag("chat_card_${chat.id}"),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier =
        Modifier.fillMaxWidth()
          .padding(start = 12.dp, end = 6.dp, top = 11.dp, bottom = 11.dp),
    ) {
      // Left: 3D Glossy Buddy Avatar (Red, Green, Blue, or Group multi-buddy)
      GlossyBuddyAvatar(
        colorKey = displayAvatarColor,
        size = 54.dp,
      )

      Spacer(modifier = Modifier.width(12.dp))

      // Middle: Title + Status Dot + Time of last sent message + Last message preview
      Column(modifier = Modifier.weight(1f)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Text(
            text = displayTitle,
            style = MaterialTheme.typography.titleMedium,
            color = AeroIceWhite,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          if (!chat.isGroup) {
            PresenceDot(status = displayPresenceStatus, size = 10.dp)
          }
          Text(
            text = formattedTime,
            style = MaterialTheme.typography.labelSmall,
            color = AeroMutedMint,
            maxLines = 1,
          )
          Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = "End-to-End Encrypted",
            tint = AeroEmeraldBright.copy(alpha = 0.85f),
            modifier = Modifier.size(12.dp),
          )
        }

        Spacer(modifier = Modifier.height(3.dp))

        Text(
          text =
            chat.lastMessagePreview.ifBlank {
              "Last sent message text by friend or user"
            },
          style = MaterialTheme.typography.bodyMedium,
          color = AeroSoftMint.copy(alpha = 0.92f),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }

      // Right: Three-dots context menu matching the reference screenshot
      Box {
        IconButton(
          onClick = { menuExpanded = true },
          modifier = Modifier.size(40.dp).testTag("chat_more_button_${chat.id}"),
        ) {
          Icon(
            imageVector = Icons.Default.MoreHoriz,
            contentDescription = "Chat options",
            tint = AeroIceWhite,
          )
        }

        DropdownMenu(
          expanded = menuExpanded,
          onDismissRequest = { menuExpanded = false },
          modifier = Modifier.background(Color(0xFF073421)),
        ) {
          DropdownMenuItem(
            text = { Text("👋 Send MSN Nudge!", color = AeroAquaCyan) },
            onClick = {
              menuExpanded = false
              onSendNudge()
            },
          )
          DropdownMenuItem(
            text = { Text("🗑️ Delete Conversation", color = Color(0xFFFCA5A5)) },
            onClick = {
              menuExpanded = false
              onDeleteChat()
            },
          )
        }
      }
    }
  }
}

@Composable
private fun QuickEditProfileDialog(
  profile: UserProfile,
  onDismiss: () -> Unit,
  onSave: (String, String, String, String, String) -> Unit,
) {
  var username by remember { mutableStateOf(profile.username) }
  var signature by remember { mutableStateOf(profile.signature) }
  var activity by remember { mutableStateOf(profile.currentActivity) }
  var status by remember { mutableStateOf(profile.presenceStatus) }
  var avatarColor by remember { mutableStateOf(profile.avatarColor) }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = Color(0xFF073521),
    titleContentColor = AeroIceWhite,
    textContentColor = AeroSoftMint,
    title = { Text("Customize Frutiger Status Card") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          listOf("online", "away", "busy", "offline").forEach { st ->
            val selected = status.equals(st, ignoreCase = true)
            Surface(
              shape = RoundedCornerShape(50),
              color = if (selected) Color(0xFF10B981) else Color(0x55041E13),
              border = BorderStroke(1.dp, AeroAquaCyan.copy(alpha = 0.5f)),
              modifier = Modifier.clip(RoundedCornerShape(50)).clickable { status = st },
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              ) {
                PresenceDot(status = st, size = 8.dp)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = st.replaceFirstChar { it.uppercase() },
                  fontSize = 11.sp,
                  color = if (selected) Color.Black else AeroIceWhite,
                  fontWeight = FontWeight.SemiBold,
                )
              }
            }
          }
        }

        OutlinedTextField(
          value = username,
          onValueChange = { username = it.take(40) },
          label = { Text("Username") },
          singleLine = true,
          colors = aeroTextFieldColors(),
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = signature,
          onValueChange = { signature = it.take(160) },
          label = { Text("Custom signature text") },
          singleLine = true,
          colors = aeroTextFieldColors(),
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = activity,
          onValueChange = { activity = it.take(120) },
          label = { Text("Current activity (if any)") },
          singleLine = true,
          colors = aeroTextFieldColors(),
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    confirmButton = {
      TextButton(
        onClick = { onSave(username, signature, activity, status, avatarColor) }
      ) {
        Text("Save Aero Card", color = AeroAquaCyan, fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel", color = AeroSoftMint)
      }
    },
  )
}

@Composable
private fun NewDirectMessageDialog(
  friends: List<FriendContact>,
  allUsers: List<UserProfile> = emptyList(),
  onDismiss: () -> Unit,
  onCreateDm: (String, String, String) -> Unit,
) {
  var customBuddyName by remember { mutableStateOf("") }
  var selectedColor by remember { mutableStateOf("aqua") }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = Color(0xFF073521),
    titleContentColor = AeroIceWhite,
    textContentColor = AeroSoftMint,
    title = { Text("New Direct Message (E2EE)") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Discovered users on the app (such as brother or another phone)
        if (allUsers.isNotEmpty()) {
          Text("People on Frutiger Chat (Live Sync):", style = MaterialTheme.typography.labelLarge, color = AeroAquaCyan)
          allUsers.take(4).forEach { user ->
            Surface(
              color = Color(0x66041E13),
              shape = RoundedCornerShape(10.dp),
              border = BorderStroke(1.dp, Color(0x5510B981)),
              modifier =
                Modifier.fillMaxWidth()
                  .clip(RoundedCornerShape(10.dp))
                  .clickable {
                    onCreateDm(user.username, user.avatarColor, user.presenceStatus)
                  },
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(8.dp),
              ) {
                GlossyBuddyAvatar(colorKey = user.avatarColor, size = 34.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(user.username, color = AeroIceWhite, fontWeight = FontWeight.Bold)
                  Text(user.signature.ifBlank { "Active now" }, color = AeroSoftMint, fontSize = 11.sp, maxLines = 1)
                }
                PresenceDot(status = user.presenceStatus, size = 10.dp)
              }
            }
          }
        } else if (friends.isNotEmpty()) {
          Text("Quick select from your Buddy List:", style = MaterialTheme.typography.labelLarge)
          friends.take(4).forEach { friend ->
            Surface(
              color = Color(0x66041E13),
              shape = RoundedCornerShape(10.dp),
              border = BorderStroke(1.dp, Color(0x446EE7B7)),
              modifier =
                Modifier.fillMaxWidth()
                  .clip(RoundedCornerShape(10.dp))
                  .clickable {
                    onCreateDm(friend.friendUsername, friend.avatarColor, friend.friendStatus)
                  },
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(8.dp),
              ) {
                GlossyBuddyAvatar(colorKey = friend.avatarColor, size = 34.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(friend.friendUsername, color = AeroIceWhite, fontWeight = FontWeight.Bold)
                  Text(friend.friendSignature, color = AeroSoftMint, fontSize = 11.sp, maxLines = 1)
                }
                PresenceDot(status = friend.friendStatus, size = 10.dp)
              }
            }
          }
        }

        Text("Or start a direct chat by username:", style = MaterialTheme.typography.labelLarge)
        OutlinedTextField(
          value = customBuddyName,
          onValueChange = { customBuddyName = it.take(40) },
          label = { Text("Friend Username (e.g., SkyDolphin)") },
          singleLine = true,
          colors = aeroTextFieldColors(),
          modifier = Modifier.fillMaxWidth().testTag("new_dm_username_input"),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          listOf("aqua", "green", "red", "blue", "gold", "purple").forEach { c ->
            Box(
              modifier =
                Modifier.clip(RoundedCornerShape(8.dp))
                  .border(
                    width = if (selectedColor == c) 2.dp else 0.dp,
                    color = if (selectedColor == c) AeroAquaCyan else Color.Transparent,
                    shape = RoundedCornerShape(8.dp),
                  )
                  .clickable { selectedColor = c }
                  .padding(2.dp)
            ) {
              GlossyBuddyAvatar(colorKey = c, size = 32.dp)
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(
        onClick = {
          if (customBuddyName.isNotBlank()) {
            onCreateDm(customBuddyName.trim(), selectedColor, "online")
          }
        },
        enabled = customBuddyName.isNotBlank(),
      ) {
        Text("Start Encrypted DM", color = AeroAquaCyan, fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel", color = AeroSoftMint)
      }
    },
  )
}

@Composable
private fun NewGroupChatDialog(
  friends: List<FriendContact>,
  allUsers: List<UserProfile> = emptyList(),
  onDismiss: () -> Unit,
  onCreateGroup: (String, List<String>) -> Unit,
) {
  var groupTitle by remember { mutableStateOf("Frutiger Aero Lounge") }
  var extraMember by remember { mutableStateOf("") }
  val selectedMembers =
    remember {
      mutableStateListOf<String>().apply {
        allUsers.take(2).forEach { add(it.username) }
        friends.take(2).forEach { add(it.friendUsername) }
        if (isEmpty()) {
          addAll(listOf("Friend 1", "Friend 2", "Friend 3"))
        }
      }
    }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = Color(0xFF073521),
    titleContentColor = AeroIceWhite,
    textContentColor = AeroSoftMint,
    title = { Text("New Encrypted Group Chat") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          GlossyBuddyAvatar(colorKey = "group", size = 48.dp)
          Spacer(modifier = Modifier.width(12.dp))
          OutlinedTextField(
            value = groupTitle,
            onValueChange = { groupTitle = it.take(50) },
            label = { Text("Group Chat Name") },
            singleLine = true,
            colors = aeroTextFieldColors(),
            modifier = Modifier.weight(1f).testTag("new_group_title_input"),
          )
        }

        Text("Select Group Members:", style = MaterialTheme.typography.labelLarge)
        val candidateNames =
          (allUsers.map { it.username } + friends.map { it.friendUsername } + listOf("Friend 1", "Friend 2", "Friend 3")).distinct()
        candidateNames.take(8).forEach { name ->
          val checked = selectedMembers.contains(name)
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
              Modifier.fillMaxWidth()
                .clickable {
                  if (checked) selectedMembers.remove(name) else selectedMembers.add(name)
                },
          ) {
            Checkbox(
              checked = checked,
              onCheckedChange = {
                if (it) selectedMembers.add(name) else selectedMembers.remove(name)
              },
              colors =
                CheckboxDefaults.colors(
                  checkedColor = AeroEmeraldBright,
                  uncheckedColor = AeroSoftMint,
                ),
            )
            Text(text = name, color = AeroIceWhite)
          }
        }

        OutlinedTextField(
          value = extraMember,
          onValueChange = { extraMember = it.take(30) },
          label = { Text("Add another username (optional)") },
          singleLine = true,
          colors = aeroTextFieldColors(),
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    confirmButton = {
      TextButton(
        onClick = {
          val finalMembers =
            (selectedMembers.toList() + listOfNotNull(extraMember.trim().takeIf { it.isNotEmpty() }))
              .distinct()
          onCreateGroup(groupTitle.ifBlank { "Group chat" }, finalMembers)
        }
      ) {
        Text("Create Group Chat", color = AeroAquaCyan, fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel", color = AeroSoftMint)
      }
    },
  )
}


@Composable
private fun LiveUserSearchResultCard(
  user: UserProfile,
  onChatClick: () -> Unit,
) {
  AeroGlassCard(
    cornerRadius = 16.dp,
    onClick = onChatClick,
    modifier = Modifier.fillMaxWidth().testTag("live_user_search_card_${user.userId}"),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
      Box {
        GlossyBuddyAvatar(colorKey = user.avatarColor, size = 48.dp)
        PresenceDot(
          status = user.presenceStatus,
          size = 12.dp,
          modifier = Modifier.align(Alignment.BottomEnd),
        )
      }

      Spacer(modifier = Modifier.width(12.dp))

      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = user.username,
            style = MaterialTheme.typography.titleMedium,
            color = AeroIceWhite,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Spacer(modifier = Modifier.width(6.dp))
          Surface(
            color = Color(0x3310B981),
            shape = RoundedCornerShape(4.dp),
            border = BorderStroke(0.5.dp, AeroEmeraldBright.copy(alpha = 0.6f)),
          ) {
            Text(
              text = user.presenceStatus.uppercase(),
              color = AeroEmeraldBright,
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
            )
          }
        }

        if (user.signature.isNotBlank()) {
          Text(
            text = user.signature,
            style = MaterialTheme.typography.bodySmall,
            color = AeroSoftMint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }

        if (user.currentActivity.isNotBlank()) {
          Text(
            text = user.currentActivity,
            style = MaterialTheme.typography.labelSmall,
            color = AeroMutedMint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }

      Spacer(modifier = Modifier.width(8.dp))

      AeroGlossyPillButton(
        text = "Chat",
        onClick = onChatClick,
        accentTint = Color(0xFF10B981),
        modifier = Modifier.width(78.dp),
      )
    }
  }
}

@Composable
private fun LiveSyncBuddyBubble(
  user: UserProfile,
  onClick: () -> Unit,
) {
  Surface(
    color = Color(0x88062F1E),
    shape = RoundedCornerShape(16.dp),
    border = BorderStroke(1.dp, AeroAquaCyan.copy(alpha = 0.5f)),
    modifier =
      Modifier.clip(RoundedCornerShape(16.dp))
        .clickable { onClick() }
        .testTag("live_buddy_bubble_${user.userId}"),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
      Box {
        GlossyBuddyAvatar(colorKey = user.avatarColor, size = 32.dp)
        PresenceDot(
          status = user.presenceStatus,
          size = 9.dp,
          modifier = Modifier.align(Alignment.BottomEnd),
        )
      }
      Spacer(modifier = Modifier.width(8.dp))
      Column {
        Text(
          text = user.username,
          color = AeroIceWhite,
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
          maxLines = 1,
        )
        Text(
          text = user.signature.ifBlank { "Online on Frutiger Chat" },
          color = AeroSoftMint,
          fontSize = 10.sp,
          maxLines = 1,
          modifier = Modifier.widthIn(max = 110.dp),
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

