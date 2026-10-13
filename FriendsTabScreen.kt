package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

@Composable
fun FriendsTabScreen(
  friendsState: UiState<List<FriendContact>>,
  allUsersState: UiState<List<UserProfile>> = UiState.Success(emptyList()),
  onStartDirectChat: (String, String, String) -> Unit,
  onStartChatWithUser: (UserProfile) -> Unit = {},
  onAddFriend: (String, String, String, String, String) -> Unit,
  onDeleteFriend: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  var showAddBuddyModal by remember { mutableStateOf(false) }
  var selectedStatusFilter by remember { mutableStateOf("all") }
  val discoveredUsers = (allUsersState as? UiState.Success)?.data ?: emptyList()

  Column(
    modifier = modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 10.dp),
  ) {
    // Header Glass Card
    AeroGlassCard(cornerRadius = 18.dp, modifier = Modifier.fillMaxWidth()) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth().padding(16.dp),
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Frutiger Buddy Directory",
            style = MaterialTheme.typography.titleLarge,
            color = AeroIceWhite,
          )
          Text(
            text = "Verified E2EE Public Key Contacts & Live Presence",
            style = MaterialTheme.typography.bodyMedium,
            color = AeroSoftMint,
          )
        }
        AeroGlossyPillButton(
          text = "+ Add Buddy",
          onClick = { showAddBuddyModal = true },
          accentTint = Color(0xFF10B981),
          modifier = Modifier.width(120.dp).testTag("add_buddy_button"),
        )
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Status Filter Pills
    Row(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.fillMaxWidth(),
    ) {
      listOf("all", "online", "away", "busy", "offline").forEach { filter ->
        val selected = selectedStatusFilter == filter
        Surface(
          shape = RoundedCornerShape(50),
          color = if (selected) Color(0xCC10B981) else Color(0x88072D1C),
          border = BorderStroke(1.dp, AeroAquaCyan.copy(alpha = 0.5f)),
          modifier =
            Modifier.clip(RoundedCornerShape(50))
              .clickable { selectedStatusFilter = filter },
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
          ) {
            if (filter != "all") {
              PresenceDot(status = filter, size = 8.dp)
              Spacer(modifier = Modifier.width(5.dp))
            }
            Text(
              text = filter.replaceFirstChar { it.uppercase() },
              fontSize = 12.sp,
              fontWeight = FontWeight.SemiBold,
              color = if (selected) Color(0xFF022010) else AeroIceWhite,
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    when (friendsState) {
      is UiState.Loading -> {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
          CircularProgressIndicator(color = AeroAquaCyan)
        }
      }
      is UiState.Error -> {
        AeroGlassCard(modifier = Modifier.fillMaxWidth()) {
          Text(friendsState.message, color = AeroSoftMint, modifier = Modifier.padding(16.dp))
        }
      }
      is UiState.Success -> {
        val filteredFriends =
          friendsState.data.filter {
            selectedStatusFilter == "all" ||
              it.friendStatus.equals(selectedStatusFilter, ignoreCase = true)
          }

        LazyColumn(
          verticalArrangement = Arrangement.spacedBy(10.dp),
          contentPadding = PaddingValues(bottom = 16.dp),
          modifier = Modifier.fillMaxWidth().weight(1f).testTag("friends_lazy_column"),
        ) {
          if (discoveredUsers.isNotEmpty() && selectedStatusFilter == "all") {
            item {
              Text(
                text = "✨ Active On Frutiger Chat • Live Sync (${discoveredUsers.size})",
                style = MaterialTheme.typography.titleMedium,
                color = AeroAquaCyan,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
              )
            }
            items(discoveredUsers, key = { "disc_${it.userId}" }) { user ->
              AeroGlassCard(
                cornerRadius = 16.dp,
                onClick = { onStartChatWithUser(user) },
                modifier = Modifier.fillMaxWidth(),
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.fillMaxWidth().padding(14.dp),
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
                      Text(user.username, color = AeroIceWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                      Spacer(modifier = Modifier.width(6.dp))
                      Text("• LIVE", color = AeroEmeraldBright, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(user.signature.ifBlank { "Active on Frutiger Chat" }, color = AeroSoftMint, fontSize = 12.sp, maxLines = 1)
                  }
                  AeroGlossyPillButton(
                    text = "Chat",
                    onClick = { onStartChatWithUser(user) },
                    accentTint = Color(0xFF10B981),
                    modifier = Modifier.width(80.dp),
                  )
                }
              }
            }

            item {
              Text(
                text = "My Buddy List",
                style = MaterialTheme.typography.titleMedium,
                color = AeroIceWhite,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
              )
            }
          }

          items(filteredFriends, key = { it.id }) { friend ->
            AeroGlassCard(
              cornerRadius = 16.dp,
              onClick = {
                onStartDirectChat(friend.friendUsername, friend.avatarColor, friend.friendStatus)
              },
              modifier = Modifier.fillMaxWidth(),
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(14.dp),
              ) {
                GlossyBuddyAvatar(colorKey = friend.avatarColor, size = 56.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                  ) {
                    Text(
                      text = friend.friendUsername,
                      style = MaterialTheme.typography.titleMedium,
                      color = AeroIceWhite,
                      fontWeight = FontWeight.Bold,
                    )
                    PresenceDot(status = friend.friendStatus, size = 10.dp)
                  }
                  Text(
                    text = friend.friendSignature.ifBlank { "No custom signature" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = AeroSoftMint,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                  if (friend.friendActivity.isNotBlank()) {
                    Text(
                      text = friend.friendActivity,
                      style = MaterialTheme.typography.labelSmall,
                      color = AeroAquaCyan,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                  }
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                    text = "🔑 Key: ${friend.publicKeyFingerprint}",
                    fontFamily = RobotoMonoFontFamily,
                    fontSize = 10.sp,
                    color = AeroMutedMint,
                  )
                }

                IconButton(
                  onClick = {
                    onStartDirectChat(
                      friend.friendUsername,
                      friend.avatarColor,
                      friend.friendStatus,
                    )
                  }
                ) {
                  Icon(
                    imageVector = Icons.AutoMirrored.Filled.Chat,
                    contentDescription = "Message ${friend.friendUsername}",
                    tint = AeroEmeraldBright,
                  )
                }

                IconButton(onClick = { onDeleteFriend(friend.id) }) {
                  Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Remove ${friend.friendUsername}",
                    tint = Color(0xFFFCA5A5),
                  )
                }
              }
            }
          }
        }
      }
    }
  }

  if (showAddBuddyModal) {
    AddBuddyDialog(
      allUsers = discoveredUsers,
      onDismiss = { showAddBuddyModal = false },
      onAdd = { username, sig, act, status, color ->
        onAddFriend(username, sig, act, status, color)
        showAddBuddyModal = false
      },
    )
  }
}

@Composable
private fun AddBuddyDialog(
  allUsers: List<UserProfile> = emptyList(),
  onDismiss: () -> Unit,
  onAdd: (String, String, String, String, String) -> Unit,
) {
  var username by remember { mutableStateOf("") }
  var signature by remember { mutableStateOf("Gliding on Frutiger Aero waves 🌊✨") }
  var activity by remember { mutableStateOf("🎵 Listening to: Eco-Futurism Mix") }
  var status by remember { mutableStateOf("online") }
  var avatarColor by remember { mutableStateOf("aqua") }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = Color(0xFF073521),
    titleContentColor = AeroIceWhite,
    textContentColor = AeroSoftMint,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.PersonAdd, contentDescription = null, tint = AeroAquaCyan)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Add Frutiger Aero Buddy")
      }
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (allUsers.isNotEmpty()) {
          Text("Quick add from online users:", style = MaterialTheme.typography.labelMedium, color = AeroAquaCyan)
          allUsers.take(3).forEach { user ->
            Surface(
              color = Color(0x66041E13),
              shape = RoundedCornerShape(8.dp),
              border = BorderStroke(1.dp, Color(0x446EE7B7)),
              modifier =
                Modifier.fillMaxWidth()
                  .clip(RoundedCornerShape(8.dp))
                  .clickable {
                    username = user.username
                    signature = user.signature
                    activity = user.currentActivity
                    status = user.presenceStatus
                    avatarColor = user.avatarColor
                  },
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(6.dp),
              ) {
                GlossyBuddyAvatar(colorKey = user.avatarColor, size = 28.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(user.username, color = AeroIceWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                  Text(user.signature.ifBlank { "Active now" }, color = AeroSoftMint, fontSize = 10.sp, maxLines = 1)
                }
                PresenceDot(status = user.presenceStatus, size = 8.dp)
              }
            }
          }
        }
        OutlinedTextField(
          value = username,
          onValueChange = { username = it.take(40) },
          label = { Text("Buddy Username") },
          singleLine = true,
          colors = aeroTextFieldColors(),
          modifier = Modifier.fillMaxWidth().testTag("add_buddy_username_input"),
        )
        OutlinedTextField(
          value = signature,
          onValueChange = { signature = it.take(160) },
          label = { Text("Custom Signature") },
          singleLine = true,
          colors = aeroTextFieldColors(),
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = activity,
          onValueChange = { activity = it.take(120) },
          label = { Text("Current Activity") },
          singleLine = true,
          colors = aeroTextFieldColors(),
          modifier = Modifier.fillMaxWidth(),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          listOf("aqua", "green", "red", "blue", "gold", "purple").forEach { c ->
            Box(
              modifier =
                Modifier.clip(RoundedCornerShape(8.dp))
                  .border(
                    width = if (avatarColor == c) 2.dp else 0.dp,
                    color = if (avatarColor == c) AeroAquaCyan else Color.Transparent,
                    shape = RoundedCornerShape(8.dp),
                  )
                  .clickable { avatarColor = c }
                  .padding(2.dp)
            ) {
              GlossyBuddyAvatar(colorKey = c, size = 34.dp)
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(
        onClick = {
          if (username.isNotBlank()) {
            onAdd(username.trim(), signature, activity, status, avatarColor)
          }
        },
        enabled = username.isNotBlank(),
      ) {
        Text("Add Buddy", color = AeroAquaCyan, fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel", color = AeroSoftMint)
      }
    },
  )
}
