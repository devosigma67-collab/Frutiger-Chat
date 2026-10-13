package com.example.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

/**
 * Real Android System Notification Manager for Frutiger Chat.
 * Provides high-priority heads-up notifications with sounds, lights, vibration, and tap-to-open actions.
 */
object AeroNotificationManager {
  private const val CHANNEL_ID = "frutiger_chat_messages"
  private const val CHANNEL_NAME = "Frutiger Chat Messages"

  fun createNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel =
        NotificationChannel(
          CHANNEL_ID,
          CHANNEL_NAME,
          NotificationManager.IMPORTANCE_HIGH,
        ).apply {
          description = "Incoming instant messages and nudges from Frutiger Chat"
          enableLights(true)
          enableVibration(true)
          vibrationPattern = longArrayOf(0, 180, 80, 180)
          setShowBadge(true)
        }
      val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
      manager?.createNotificationChannel(channel)
    }
  }

  fun showIncomingMessageNotification(
    context: Context,
    chatId: String,
    senderName: String,
    messageText: String,
    isSpecialEffect: Boolean = false,
  ) {
    createNotificationChannel(context)

    // Vibration feedback
    vibratePhone(context, isSpecialEffect)

    // Launch MainActivity with target chat ID
    val intent =
      Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        putExtra("EXTRA_OPEN_CHAT_ID", chatId)
      }

    val pendingIntent =
      PendingIntent.getActivity(
        context,
        chatId.hashCode(),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )

    val title = if (isSpecialEffect) "👋 Nudge from $senderName!" else senderName
    val preview = if (isSpecialEffect) "$senderName sent you a nudge!" else messageText

    val builder =
      NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.mipmap.ic_launcher)
        .setContentTitle(title)
        .setContentText(preview)
        .setStyle(NotificationCompat.BigTextStyle().bigText(preview))
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setDefaults(NotificationCompat.DEFAULT_ALL)
        .setAutoCancel(true)
        .setContentIntent(pendingIntent)

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
      ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    ) {
      try {
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.notify(chatId.hashCode(), builder.build())
      } catch (e: SecurityException) {
        // Permission was revoked or unavailable
      }
    }
  }

  fun vibratePhone(context: Context, isNudge: Boolean) {
    try {
      val vibrator =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
          val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
          vibratorManager?.defaultVibrator
        } else {
          @Suppress("DEPRECATION")
          context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

      if (vibrator != null && vibrator.hasVibrator()) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          if (isNudge) {
            val timings = longArrayOf(0, 100, 60, 140, 60, 200)
            val amplitudes = intArrayOf(0, 200, 0, 255, 0, 255)
            vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
          } else {
            vibrator.vibrate(VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE))
          }
        } else {
          @Suppress("DEPRECATION")
          if (isNudge) {
            vibrator.vibrate(longArrayOf(0, 100, 60, 140, 60, 200), -1)
          } else {
            vibrator.vibrate(120)
          }
        }
      }
    } catch (_: Exception) {
      // Graceful fallback if vibrator permission or hardware is unavailable
    }
  }
}
