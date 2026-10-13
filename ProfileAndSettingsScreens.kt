package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.network.SocketConnectionState
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
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

@Composable
fun ProfileTabScreen(
  userProfileState: UiState<UserProfile>,
  onSaveProfile: (String, String, String, String, String, String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val currentProfile =
    (userProfileState as? UiState.Success)?.data
      ?: UserProfile(
        username = "AeroWave",
        signature = "Custom signature text",
        currentActivity = "🎵 Listening to: Lease - Takeshi Abo",
        presenceStatus = "online",
        avatarColor = "aqua",
        wallpaperTheme = "default",
      )

  var username by remember { mutableStateOf(currentProfile.username) }
  var signature by remember { mutableStateOf(currentProfile.signature) }
  var activity by remember { mutableStateOf(currentProfile.currentActivity) }
  var presenceStatus by remember { mutableStateOf(currentProfile.presenceStatus) }
  var avatarColor by remember { mutableStateOf(currentProfile.avatarColor) }
  var wallpaperTheme by remember { mutableStateOf(currentProfile.wallpaperTheme.ifBlank { "default" }) }

  LaunchedEffect(currentProfile) {
    username = currentProfile.username
    signature = currentProfile.signature
    activity = currentProfile.currentActivity
    presenceStatus = currentProfile.presenceStatus
    avatarColor = currentProfile.avatarColor
    wallpaperTheme = currentProfile.wallpaperTheme.ifBlank { "default" }
  }

  val googleEmail = Firebase.auth.currentUser?.email ?: "Authenticated via Google Sign-In"

  Column(
    verticalArrangement = Arrangement.spacedBy(12.dp),
    modifier =
      modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 14.dp, vertical = 10.dp),
  ) {
    // Live Preview Card matching the top card on the Chats tab
    AeroGlassCard(cornerRadius = 20.dp, modifier = Modifier.fillMaxWidth()) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(20.dp),
      ) {
        Box(contentAlignment = Alignment.BottomEnd) {
          GlossyBuddyAvatar(colorKey = avatarColor, size = 88.dp)
          PresenceDot(status = presenceStatus, size = 18.dp, modifier = Modifier.padding(4.dp))
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
          text = username.ifBlank { "Username" },
          style = MaterialTheme.typography.headlineMedium,
          color = AeroIceWhite,
        )
        Text(
          text = googleEmail,
          style = MaterialTheme.typography.labelSmall,
          color = AeroAquaCyan,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "Theme: ${wallpaperTheme.replaceFirstChar { it.uppercase() }} Wallpaper",
          style = MaterialTheme.typography.labelMedium,
          color = AeroSoftMint,
        )
      }
    }

    // Wallpaper Selection Card
    AeroGlassCard(cornerRadius = 18.dp, modifier = Modifier.fillMaxWidth()) {
      Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth().padding(16.dp),
      ) {
        Text(
          text = "Aero Background Wallpaper",
          style = MaterialTheme.typography.titleMedium,
          color = AeroIceWhite,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = "Choose your full-screen Frutiger Aero backdrop:",
          style = MaterialTheme.typography.bodyMedium,
          color = AeroSoftMint,
        )

        Row(
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          val wallpapers = listOf(
            Triple("default", "✨ Default", "Aurora & clouds"),
            Triple("grass", "🌿 Grass", "Lush hills & dew"),
            Triple("aqua", "🌊 Aqua", "Tropical waters"),
          )

          wallpapers.forEach { (key, label, desc) ->
            val isSelected = wallpaperTheme.equals(key, ignoreCase = true)
            Surface(
              shape = RoundedCornerShape(14.dp),
              color = if (isSelected) Color(0xDD065F46) else Color(0x66041E13),
              border = BorderStroke(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) AeroAquaCyan else Color(0x4410B981),
              ),
              modifier =
                Modifier.weight(1f)
                  .clip(RoundedCornerShape(14.dp))
                  .clickable { wallpaperTheme = key }
                  .testTag("wallpaper_btn_$key"),
            ) {
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
              ) {
                Text(
                  text = label,
                  fontWeight = FontWeight.Bold,
                  fontSize = 13.sp,
                  color = if (isSelected) AeroIceWhite else AeroSoftMint,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = desc,
                  fontSize = 10.sp,
                  color = if (isSelected) AeroEmeraldBright else AeroMutedMint,
                  maxLines = 1,
                )
              }
            }
          }
        }
      }
    }

    // Customization Form Card
    AeroGlassCard(cornerRadius = 18.dp, modifier = Modifier.fillMaxWidth()) {
      Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth().padding(16.dp),
      ) {
        Text(
          text = "Customize Buddy Avatar & Status",
          style = MaterialTheme.typography.titleMedium,
          color = AeroIceWhite,
          fontWeight = FontWeight.Bold,
        )

        // Avatar Color Picker
        Row(
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          listOf("aqua", "green", "red", "blue", "gold", "purple").forEach { c ->
            Box(
              modifier =
                Modifier.clip(RoundedCornerShape(10.dp))
                  .border(
                    width = if (avatarColor == c) 2.dp else 0.dp,
                    color = if (avatarColor == c) AeroAquaCyan else Color.Transparent,
                    shape = RoundedCornerShape(10.dp),
                  )
                  .clickable { avatarColor = c }
                  .padding(2.dp)
            ) {
              GlossyBuddyAvatar(colorKey = c, size = 40.dp)
            }
          }
        }

        // Presence Status Selector
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          listOf("online", "away", "busy", "offline").forEach { st ->
            val selected = presenceStatus.equals(st, ignoreCase = true)
            Surface(
              shape = RoundedCornerShape(50),
              color = if (selected) Color(0xCC10B981) else Color(0x66041E13),
              border = BorderStroke(1.dp, AeroAquaCyan.copy(alpha = 0.5f)),
              modifier =
                Modifier.clip(RoundedCornerShape(50)).clickable { presenceStatus = st },
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
              ) {
                PresenceDot(status = st, size = 9.dp)
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                  text = st.replaceFirstChar { it.uppercase() },
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (selected) Color(0xFF022010) else AeroIceWhite,
                )
              }
            }
          }
        }

        OutlinedTextField(
          value = username,
          onValueChange = { username = it.take(40) },
          label = { Text("Display Username") },
          singleLine = true,
          colors = aeroTextFieldColors(),
          modifier = Modifier.fillMaxWidth().testTag("profile_username_input"),
        )

        OutlinedTextField(
          value = signature,
          onValueChange = { signature = it.take(160) },
          label = { Text("Custom signature text") },
          singleLine = true,
          colors = aeroTextFieldColors(),
          modifier = Modifier.fillMaxWidth().testTag("profile_signature_input"),
        )

        OutlinedTextField(
          value = activity,
          onValueChange = { activity = it.take(120) },
          label = { Text("Current activity (if any)") },
          singleLine = true,
          colors = aeroTextFieldColors(),
          modifier = Modifier.fillMaxWidth().testTag("profile_activity_input"),
        )

        AeroGlossyPillButton(
          text = "Save Profile & Wallpaper",
          onClick = {
            onSaveProfile(username, signature, activity, presenceStatus, avatarColor, wallpaperTheme)
          },
          accentTint = Color(0xFF10B981),
          modifier = Modifier.fillMaxWidth().testTag("save_profile_button"),
        )
      }
    }
  }
}

@Composable
fun SettingsTabScreen(
  waterEffectsEnabled: Boolean,
  socketServerUrl: String,
  socketConnectionState: SocketConnectionState,
  socketPacketCounter: Int,
  cachedChatsCount: Int,
  cachedMessagesCount: Int,
  onToggleWaterEffects: (Boolean) -> Unit,
  onUpdateSocketUrl: (String) -> Unit,
  onUpdateVaultPassphrase: (String) -> Unit,
  onSignOut: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var editedSocketUrl by remember(socketServerUrl) { mutableStateOf(socketServerUrl) }
  var editedPassphrase by remember { mutableStateOf("") }

  Column(
    verticalArrangement = Arrangement.spacedBy(12.dp),
    modifier =
      modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 14.dp, vertical = 10.dp),
  ) {
    // 1. Frutiger Aero Visual & Water Effects Card
    AeroGlassCard(cornerRadius = 18.dp, modifier = Modifier.fillMaxWidth()) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth().padding(16.dp),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f),
        ) {
          Icon(
            imageVector = Icons.Default.WaterDrop,
            contentDescription = null,
            tint = AeroAquaCyan,
            modifier = Modifier.size(28.dp),
          )
          Spacer(modifier = Modifier.width(12.dp))
          Column {
            Text(
              text = "Interactive Water Ripples & Droplets",
              style = MaterialTheme.typography.titleMedium,
              color = AeroIceWhite,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text = "Touch-activated aqua rings & floating glass bokeh",
              style = MaterialTheme.typography.bodyMedium,
              color = AeroSoftMint,
            )
          }
        }
        Switch(
          checked = waterEffectsEnabled,
          onCheckedChange = onToggleWaterEffects,
          colors =
            SwitchDefaults.colors(
              checkedThumbColor = AeroIceWhite,
              checkedTrackColor = AeroEmeraldBright,
            ),
          modifier = Modifier.testTag("water_effects_switch"),
        )
      }
    }

    // 2. Local Room Database & Privacy Cache Inspector Card
    AeroGlassCard(cornerRadius = 18.dp, modifier = Modifier.fillMaxWidth()) {
      Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().padding(16.dp),
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.Storage,
            contentDescription = null,
            tint = AeroEmeraldBright,
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Local On-Device Storage (Room SQLite + Cloud Sync)",
            style = MaterialTheme.typography.titleMedium,
            color = AeroIceWhite,
            fontWeight = FontWeight.Bold,
          )
        }
        Text(
          text = "All conversations, contacts, and decrypted messages are persisted locally in frutiger_aero_chat.db for instant offline access and privacy across devices.",
          style = MaterialTheme.typography.bodyMedium,
          color = AeroSoftMint,
        )
        Surface(
          color = Color(0x8803180F),
          shape = RoundedCornerShape(10.dp),
          border = BorderStroke(1.dp, AeroEmeraldBright.copy(alpha = 0.45f)),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            horizontalArrangement = Arrangement.SpaceAround,
            modifier = Modifier.fillMaxWidth().padding(12.dp),
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text(
                text = "$cachedChatsCount",
                style = MaterialTheme.typography.titleLarge,
                color = AeroAquaCyan,
              )
              Text("Cached Chats", style = MaterialTheme.typography.labelSmall, color = AeroSoftMint)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text(
                text = "$cachedMessagesCount",
                style = MaterialTheme.typography.titleLarge,
                color = AeroEmeraldBright,
              )
              Text("Cached Messages", style = MaterialTheme.typography.labelSmall, color = AeroSoftMint)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text(
                text = "Active",
                style = MaterialTheme.typography.titleLarge,
                color = AeroIceWhite,
              )
              Text("Private Chat", style = MaterialTheme.typography.labelSmall, color = AeroSoftMint)
            }
          }
        }
      }
    }

    // 3. Socket.IO Real-Time Transport & Firebase Firestore Status Card
    AeroGlassCard(cornerRadius = 18.dp, modifier = Modifier.fillMaxWidth()) {
      Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth().padding(16.dp),
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.CloudDone,
            contentDescription = null,
            tint = AeroAquaCyan,
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Real-Time Cloud Sync & Transport",
            style = MaterialTheme.typography.titleMedium,
            color = AeroIceWhite,
            fontWeight = FontWeight.Bold,
          )
        }

        val statusSummary =
          when (socketConnectionState) {
            is SocketConnectionState.Connected ->
              "Connected (${socketConnectionState.transport}) • Frames: $socketPacketCounter"
            is SocketConnectionState.Connecting ->
              "Connecting to ${socketConnectionState.endpoint}…"
            is SocketConnectionState.HybridFirestoreRelay ->
              "${socketConnectionState.reason} • Frames: $socketPacketCounter"
          }

        Text(
          text = statusSummary,
          fontFamily = RobotoMonoFontFamily,
          fontSize = 12.sp,
          color = AeroEmeraldBright,
        )

        OutlinedTextField(
          value = editedSocketUrl,
          onValueChange = { editedSocketUrl = it },
          label = { Text("Real-Time Relay Server URL") },
          singleLine = true,
          colors = aeroTextFieldColors(),
          modifier = Modifier.fillMaxWidth().testTag("socket_url_input"),
        )

        AeroGlossyPillButton(
          text = "Reconnect Relay Transport",
          onClick = { onUpdateSocketUrl(editedSocketUrl) },
          accentTint = Color(0xFF0E7490),
          modifier = Modifier.fillMaxWidth(),
        )
      }
    }

    // 5. Sign Out Button
    AeroGlassCard(
      cornerRadius = 16.dp,
      onClick = onSignOut,
      modifier = Modifier.fillMaxWidth().testTag("sign_out_card"),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth().padding(14.dp),
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.Logout,
          contentDescription = "Sign Out",
          tint = Color(0xFFFCA5A5),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Sign Out of Frutiger Chat",
          style = MaterialTheme.typography.titleMedium,
          color = Color(0xFFFCA5A5),
          fontWeight = FontWeight.Bold,
        )
      }
    }

    Spacer(modifier = Modifier.height(12.dp))
  }
}
