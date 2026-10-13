package com.example.ui.screens

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChatRoom
import com.example.data.model.DecryptedMessage
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.ui.components.AeroGlassCard
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
import kotlin.math.roundToInt

@Composable
fun ConversationScreen(
  chatRoom: ChatRoom,
  messagesState: UiState<List<DecryptedMessage>>,
  typingBuddyName: String?,
  nudgeTriggerCount: Int,
  currentUserId: String = "",
  currentUsername: String = "",
  allUsers: List<UserProfile> = emptyList(),
  onBack: () -> Unit,
  onSendMessage: (String, String) -> Unit,
  onSendNudge: () -> Unit,
  modifier: Modifier = Modifier,
) {
  BackHandler(onBack = onBack)

  val context = LocalContext.current
  var messageInput by remember { mutableStateOf("") }
  val listState = rememberLazyListState()

  // Classic MSN Messenger Nudge! Window Shake Animation + Haptic Vibration
  val shakeOffsetX = remember { Animatable(0f) }
  LaunchedEffect(nudgeTriggerCount) {
    if (nudgeTriggerCount > 0) {
      triggerDeviceNudgeVibration(context)
      val offsets = listOf(-18f, 18f, -14f, 14f, -10f, 10f, -6f, 6f, 0f)
      offsets.forEach { target ->
        shakeOffsetX.animateTo(target, animationSpec = tween(durationMillis = 38))
      }
    }
  }

  val messages = (messagesState as? UiState.Success)?.data ?: emptyList()
  LaunchedEffect(messages.size) {
    if (messages.isNotEmpty()) {
      listState.scrollToItem(messages.size - 1)
    }
  }

  Column(
    modifier =
      modifier
        .fillMaxSize()
        .imePadding()
        .windowInsetsPadding(WindowInsets.navigationBars)
        .offset { IntOffset(shakeOffsetX.value.roundToInt(), 0) }
        .padding(horizontal = 12.dp, vertical = 6.dp),
  ) {
    // 1. TOP CONVERSATION GLASS HEADER
    AeroGlassCard(
      cornerRadius = 18.dp,
      modifier = Modifier.fillMaxWidth().testTag("conversation_header_card"),
    ) {
      Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.fillMaxWidth(),
        ) {
          IconButton(
            onClick = onBack,
            modifier = Modifier.size(40.dp).testTag("conversation_back_button"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back to Chats",
              tint = AeroIceWhite,
            )
          }

          val isAeroAi = chatRoom.id == "chat_aero_ai" || chatRoom.title.equals("Aero AI", ignoreCase = true)

          val otherUser =
            remember(chatRoom, allUsers, currentUserId, currentUsername) {
              if (chatRoom.isRealGroup || isAeroAi) {
                null
              } else {
                val otherUid = chatRoom.participantUids.firstOrNull { it != currentUserId && it.isNotBlank() }
                if (otherUid != null) {
                  allUsers.find { it.userId == otherUid }
                } else {
                  val names = chatRoom.participantNames.split(",").map { it.trim() }
                  val otherName = names.firstOrNull { it.isNotBlank() && !it.equals(currentUsername, ignoreCase = true) }
                  if (otherName != null) allUsers.find { it.username.equals(otherName, ignoreCase = true) } else null
                }
              }
            }

          val displayTitle =
            remember(chatRoom, otherUser, currentUsername, isAeroAi) {
              when {
                isAeroAi -> "Aero AI"
                chatRoom.isRealGroup -> chatRoom.title.ifBlank { "Group Chat" }
                otherUser != null -> otherUser.username
                else -> {
                  val names = chatRoom.participantNames.split(",").map { it.trim() }.filter { it.isNotBlank() }
                  val otherName = names.firstOrNull { !it.equals(currentUsername, ignoreCase = true) }
                  when {
                    otherName != null -> otherName
                    chatRoom.lastMessageSender.isNotBlank() && !chatRoom.lastMessageSender.equals(currentUsername, ignoreCase = true) -> chatRoom.lastMessageSender
                    !chatRoom.title.equals(currentUsername, ignoreCase = true) && chatRoom.title.isNotBlank() -> chatRoom.title
                    else -> "Friend"
                  }
                }
              }
            }

          val displayAvatarColor =
            remember(chatRoom, otherUser, isAeroAi) {
              when {
                isAeroAi -> "aqua"
                chatRoom.isRealGroup -> "group"
                else -> otherUser?.avatarColor ?: chatRoom.avatarColor
              }
            }

          val displayPresenceStatus =
            remember(chatRoom, otherUser, isAeroAi) {
              if (isAeroAi) "online" else (otherUser?.presenceStatus ?: chatRoom.presenceStatus)
            }

          GlossyBuddyAvatar(
            colorKey = displayAvatarColor,
            size = 46.dp,
          )

          Spacer(modifier = Modifier.width(10.dp))

          Column(modifier = Modifier.weight(1f)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Text(
                text = displayTitle,
                style = MaterialTheme.typography.titleMedium,
                color = AeroIceWhite,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              if (!chatRoom.isRealGroup) {
                PresenceDot(status = displayPresenceStatus, size = 10.dp)
              }
            }
            Text(
              text =
                when {
                  isAeroAi -> "🤖 AI Companion • Online"
                  chatRoom.isRealGroup -> "Group: ${chatRoom.participantNames}"
                  else -> displayPresenceStatus.replaceFirstChar { it.uppercase() }
                },
              style = MaterialTheme.typography.labelSmall,
              color = AeroSoftMint,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }

          // Water splash action
          Surface(
            shape = RoundedCornerShape(50),
            color = Color(0x550891B2),
            border = BorderStroke(1.dp, AeroAquaCyan.copy(alpha = 0.5f)),
            modifier =
              Modifier.clip(RoundedCornerShape(50))
                .clickable {
                  onSendMessage("💧 Sent a Frutiger Aero Water Splash!", "water_ripple")
                }
                .testTag("water_splash_button"),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            ) {
              Icon(
                imageVector = Icons.Default.WaterDrop,
                contentDescription = null,
                tint = AeroAquaCyan,
                modifier = Modifier.size(14.dp),
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "Splash",
                style = MaterialTheme.typography.labelSmall,
                color = AeroIceWhite,
              )
            }
          }

          Spacer(modifier = Modifier.width(4.dp))

          // Classic MSN "Nudge!" Button
          IconButton(
            onClick = onSendNudge,
            modifier = Modifier.size(40.dp).testTag("send_nudge_button"),
          ) {
            Icon(
              imageVector = Icons.Default.NotificationsActive,
              contentDescription = "Send Nudge!",
              tint = Color(0xFFFACC15),
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // 2. ENCRYPTED MESSAGES LIST
    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
      when (messagesState) {
        is UiState.Loading -> {
          Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            CircularProgressIndicator(color = AeroAquaCyan)
          }
        }
        is UiState.Error -> {
          AeroGlassCard(modifier = Modifier.fillMaxWidth()) {
            Text(
              text = messagesState.message,
              color = AeroSoftMint,
              modifier = Modifier.padding(16.dp),
            )
          }
        }
        is UiState.Success -> {
          LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 8.dp),
            modifier = Modifier.fillMaxSize().testTag("messages_lazy_column"),
          ) {
            items(messages, key = { it.id }) { msg ->
              GlossyMessageBubble(message = msg)
            }
          }
        }
      }
    }

    // Typing Indicator
    AnimatedVisibility(visible = typingBuddyName != null) {
      Text(
        text = "💬 ${typingBuddyName ?: "Buddy"} is typing…",
        style = MaterialTheme.typography.labelSmall,
        color = AeroAquaCyan,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
      )
    }

    // 3. GLOSSY MESSAGE COMPOSER BAR
    AeroGlassCard(
      cornerRadius = 24.dp,
      modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
      ) {
        Box(
          contentAlignment = Alignment.CenterStart,
          modifier =
            Modifier.weight(1f)
              .padding(horizontal = 6.dp, vertical = 8.dp),
        ) {
          if (messageInput.isEmpty()) {
            Text(
              text = "Type a message…",
              style = MaterialTheme.typography.bodyMedium,
              color = AeroSoftMint.copy(alpha = 0.7f),
            )
          }
          BasicTextField(
            value = messageInput,
            onValueChange = { messageInput = it.take(1000) },
            textStyle =
              TextStyle(
                color = AeroIceWhite,
                fontSize = 15.sp,
              ),
            cursorBrush = SolidColor(AeroAquaCyan),
            modifier = Modifier.fillMaxWidth().testTag("message_text_input"),
          )
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Glossy Aqua-Emerald Send Button
        Box(
          contentAlignment = Alignment.Center,
          modifier =
            Modifier.size(44.dp)
              .clip(CircleShape)
              .background(
                brush =
                  Brush.verticalGradient(
                    colors =
                      listOf(
                        AeroAquaCyan,
                        AeroEmeraldBright,
                        Color(0xFF047857),
                      )
                  )
              )
              .border(1.dp, Color.White.copy(alpha = 0.8f), CircleShape)
              .clickable {
                if (messageInput.isNotBlank()) {
                  onSendMessage(messageInput, "text")
                  messageInput = ""
                }
              }
              .testTag("send_message_button"),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.Send,
            contentDescription = "Send message",
            tint = Color(0xFF022C16),
            modifier = Modifier.size(20.dp),
          )
        }
      }
    }
  }
}

@Composable
private fun GlossyMessageBubble(
  message: DecryptedMessage,
) {
  val timeText =
    remember(message.timestampMillis) {
      SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(message.timestampMillis))
    }

  val isSpecialEffect = message.messageType == "nudge" || message.messageType == "water_ripple"

  Column(
    horizontalAlignment =
      when {
        isSpecialEffect -> Alignment.CenterHorizontally
        message.isFromMe -> Alignment.End
        else -> Alignment.Start
      },
    modifier = Modifier.fillMaxWidth(),
  ) {
    if (isSpecialEffect) {
      Surface(
        shape = RoundedCornerShape(16.dp),
        color =
          if (message.messageType == "nudge") {
            Color(0x88713F12)
          } else {
            Color(0x880E7490)
          },
        border =
          BorderStroke(
            1.dp,
            if (message.messageType == "nudge") Color(0xFFFACC15) else AeroAquaCyan,
          ),
        modifier = Modifier.padding(vertical = 2.dp),
      ) {
        Text(
          text = "${message.plaintext} • $timeText",
          style = MaterialTheme.typography.labelLarge,
          color = AeroIceWhite,
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
        )
      }
    } else {
      val bubbleShape =
        if (message.isFromMe) {
          RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp)
        } else {
          RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 4.dp, bottomEnd = 18.dp)
        }

      val bubbleBrush =
        if (message.isFromMe) {
          Brush.verticalGradient(
            colors =
              listOf(
                Color(0xDD10B981),
                Color(0xDD047857),
                Color(0xEE064E3B),
              )
          )
        } else {
          Brush.verticalGradient(
            colors =
              listOf(
                Color(0xDD1B4D3E),
                Color(0xDD0B2E22),
                Color(0xEE061E16),
              )
          )
        }

      Box(
        modifier =
          Modifier.widthIn(max = 310.dp)
            .clip(bubbleShape)
            .background(brush = bubbleBrush, shape = bubbleShape)
            .border(
              width = 1.dp,
              brush =
                Brush.verticalGradient(
                  colors =
                    listOf(
                      Color.White.copy(alpha = 0.65f),
                      AeroAquaCyan.copy(alpha = 0.35f),
                    )
                ),
              shape = bubbleShape,
            )
      ) {
        // Top-half glossy glass reflection inside the message bubble
        Box(
          modifier =
            Modifier.fillMaxWidth()
              .height(18.dp)
              .background(
                Brush.verticalGradient(
                  colors = listOf(Color.White.copy(alpha = 0.28f), Color.Transparent)
                )
              )
        )

        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
          if (!message.isFromMe) {
            Text(
              text = message.senderName,
              style = MaterialTheme.typography.labelSmall,
              color = AeroAquaCyan,
              fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(2.dp))
          }

          Text(
            text = message.plaintext,
            style = MaterialTheme.typography.bodyLarge,
            color = AeroIceWhite,
          )

          Spacer(modifier = Modifier.height(4.dp))

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
            modifier = Modifier.align(Alignment.End),
          ) {
            Text(
              text = timeText,
              style = MaterialTheme.typography.labelSmall,
              color = AeroSoftMint.copy(alpha = 0.85f),
              fontSize = 10.sp,
            )
            if (message.isFromMe) {
              Spacer(modifier = Modifier.width(4.dp))
              Icon(
                imageVector = Icons.Default.DoneAll,
                contentDescription = "Delivered",
                tint = AeroAquaCyan,
                modifier = Modifier.size(13.dp),
              )
            }
          }
        }
      }
    }
  }
}

@Suppress("DEPRECATION")
private fun triggerDeviceNudgeVibration(context: Context) {
  try {
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      vibrator.vibrate(
        VibrationEffect.createWaveform(longArrayOf(0, 60, 40, 80, 40, 60), -1)
      )
    } else {
      vibrator.vibrate(200)
    }
  } catch (_: Exception) {}
}
