package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.AeroAquaCyan
import com.example.ui.theme.AeroEmeraldBright
import com.example.ui.theme.AeroIceWhite
import com.example.ui.theme.StatusAwayYellow
import com.example.ui.theme.StatusBusyRed
import com.example.ui.theme.StatusOfflineGray
import com.example.ui.theme.StatusOnlineGreen
import kotlinx.coroutines.launch

private class WaterRippleEffectState(
  val center: Offset,
  val progress: Animatable<Float, *> = Animatable(0f),
)

/**
 * Full-screen Frutiger Aero background container featuring the emerald/cyan aurora wallpaper,
 * floating ambient water bubbles, and interactive touch water-ripple rings.
 */
@Composable
fun FrutigerAeroWallpaperBox(
  waterEffectsEnabled: Boolean,
  wallpaperTheme: String = "default",
  modifier: Modifier = Modifier,
  content: @Composable BoxScope.() -> Unit,
) {
  val ripples = remember { mutableStateListOf<WaterRippleEffectState>() }
  val scope = rememberCoroutineScope()

  val infiniteTransition = rememberInfiniteTransition(label = "aero_bubbles")
  val bubbleFloatPhase by
    infiniteTransition.animateFloat(
      initialValue = 0f,
      targetValue = 1f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(durationMillis = 9000, easing = LinearEasing),
          repeatMode = RepeatMode.Reverse,
        ),
      label = "bubble_float",
    )

  val wallpaperRes =
    when (wallpaperTheme.lowercase().trim()) {
      "grass" -> R.drawable.img_bg_grass
      "aqua" -> R.drawable.img_bg_aqua
      else -> R.drawable.img_bg_default
    }

  Box(
    modifier =
      modifier
        .fillMaxSize()
        .background(Color(0xFF032312))
        .pointerInput(waterEffectsEnabled) {
          if (waterEffectsEnabled) {
            awaitPointerEventScope {
              while (true) {
                val event = awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                if (event.type == androidx.compose.ui.input.pointer.PointerEventType.Press) {
                  val change = event.changes.firstOrNull()
                  if (change != null) {
                    val ripple = WaterRippleEffectState(center = change.position)
                    ripples.add(ripple)
                    scope.launch {
                      ripple.progress.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(durationMillis = 750, easing = LinearEasing),
                      )
                      ripples.remove(ripple)
                    }
                  }
                }
              }
            }
          }
        }
  ) {
    Image(
      painter = painterResource(id = wallpaperRes),
      contentDescription = "Frutiger Aero Wallpaper",
      contentScale = ContentScale.Crop,
      modifier = Modifier.fillMaxSize(),
    )

    // Deep emerald/cyan atmospheric tint to match the reference contrast
    Box(
      modifier =
        Modifier.fillMaxSize()
          .background(
            Brush.verticalGradient(
              colors =
                listOf(
                  Color(0x77021C0E),
                  Color(0x44032B17),
                  Color(0x88021A0E),
                )
            )
          )
    )

    if (waterEffectsEnabled) {
      Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Ambient floating Frutiger Aero water droplets / bokeh
        val droplets =
          listOf(
            Triple(0.15f, 0.22f, 22f),
            Triple(0.85f, 0.18f, 30f),
            Triple(0.72f, 0.48f, 18f),
            Triple(0.20f, 0.68f, 26f),
            Triple(0.88f, 0.78f, 34f),
          )
        droplets.forEachIndexed { idx, (rx, ry, radiusDp) ->
          val driftY = (bubbleFloatPhase - 0.5f) * 35f * (if (idx % 2 == 0) 1f else -1f)
          val center = Offset(w * rx, h * ry + driftY)
          val r = radiusDp.dp.toPx()
          drawCircle(
            brush =
              Brush.radialGradient(
                colors =
                  listOf(
                    Color(0x38FFFFFF),
                    Color(0x182CE5E5),
                    Color.Transparent,
                  ),
                center = Offset(center.x - r * 0.3f, center.y - r * 0.3f),
                radius = r,
              ),
            radius = r,
            center = center,
          )
          drawCircle(
            color = Color(0x30FFFFFF),
            radius = r * 0.85f,
            center = center,
            style = Stroke(width = 1.dp.toPx()),
          )
        }

        // Interactive touch water ripples
        ripples.forEach { ripple ->
          val p = ripple.progress.value
          val maxRadius = 110.dp.toPx()
          val currentRadius = 12.dp.toPx() + maxRadius * p
          val alpha = (1f - p).coerceIn(0f, 1f)
          drawCircle(
            color = AeroAquaCyan.copy(alpha = alpha * 0.55f),
            radius = currentRadius,
            center = ripple.center,
            style = Stroke(width = 2.5.dp.toPx()),
          )
          drawCircle(
            color = Color.White.copy(alpha = alpha * 0.40f),
            radius = currentRadius * 0.68f,
            center = ripple.center,
            style = Stroke(width = 1.5.dp.toPx()),
          )
        }
      }
    }

    content()
  }
}

/**
 * Frosted dark-emerald Frutiger Aero glass card with top specular shine and glowing rim border.
 */
@Composable
fun AeroGlassCard(
  modifier: Modifier = Modifier,
  cornerRadius: Dp = 16.dp,
  onClick: (() -> Unit)? = null,
  content: @Composable BoxScope.() -> Unit,
) {
  val shape = RoundedCornerShape(cornerRadius)
  val clickMod =
    if (onClick != null) {
      Modifier.clip(shape).clickable(onClick = onClick)
    } else {
      Modifier.clip(shape)
    }

  Box(
    modifier =
      modifier
        .shadow(
          elevation = 10.dp,
          shape = shape,
          ambientColor = Color.Black.copy(alpha = 0.65f),
          spotColor = Color.Black.copy(alpha = 0.65f),
        )
        .then(clickMod)
        .background(
          brush =
            Brush.verticalGradient(
              colors =
                listOf(
                  Color(0xCC184F37),
                  Color(0xBF09291B),
                  Color(0xD9051A10),
                )
            ),
          shape = shape,
        )
        .border(
          BorderStroke(
            width = 1.dp,
            brush =
              Brush.verticalGradient(
                colors =
                  listOf(
                    Color(0x99A7F3D0),
                    Color(0x3334D399),
                    Color(0x5510B981),
                  )
              ),
          ),
          shape = shape,
        )
  ) {
    // Top-half glossy specular highlight band
    Box(
      modifier =
        Modifier.fillMaxWidth()
          .height(26.dp)
          .background(
            brush =
              Brush.verticalGradient(
                colors =
                  listOf(
                    Color(0x38FFFFFF),
                    Color(0x0DFFFFFF),
                    Color.Transparent,
                  )
              )
          )
    )
    content()
  }
}

/**
 * Classic Frutiger Aero glossy pill button with top-half glass reflection ("New direct message" / "New group chat").
 */
@Composable
fun AeroGlossyPillButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  accentTint: Color = Color(0xFF1F7A4C),
  enabled: Boolean = true,
) {
  val shape = RoundedCornerShape(10.dp)
  Surface(
    onClick = onClick,
    enabled = enabled,
    shape = shape,
    color = Color.Transparent,
    shadowElevation = 6.dp,
    modifier = modifier.height(36.dp),
  ) {
    Box(
      contentAlignment = Alignment.Center,
      modifier =
        Modifier.fillMaxSize()
          .background(
            brush =
              Brush.verticalGradient(
                colors =
                  listOf(
                    accentTint.copy(alpha = 0.85f),
                    accentTint.copy(alpha = 0.55f),
                    Color(0xCC0A2E1D),
                  )
              ),
            shape = shape,
          )
          .border(
            BorderStroke(
              width = 1.dp,
              brush =
                Brush.verticalGradient(
                  colors =
                    listOf(
                      Color(0xCCFFFFFF),
                      Color(0x556EE7B7),
                      Color(0x8834D399),
                    )
                ),
            ),
            shape = shape,
          ),
    ) {
      // Top-half crisp white glass reflection
      Box(
        modifier =
          Modifier.fillMaxWidth()
            .height(17.dp)
            .align(Alignment.TopCenter)
            .background(
              brush =
                Brush.verticalGradient(
                  colors =
                    listOf(
                      Color(0x66FFFFFF),
                      Color(0x1AFFFFFF),
                      Color.Transparent,
                    )
                )
            )
      )
      Text(
        text = text,
        color = AeroIceWhite,
        fontSize = 13.5.sp,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(horizontal = 10.dp),
      )
    }
  }
}

/**
 * Colored status indicator dot matching the screenshot (Green = online, Yellow = away, Red = busy, Gray = offline).
 */
@Composable
fun PresenceDot(
  status: String,
  size: Dp = 11.dp,
  modifier: Modifier = Modifier,
) {
  val dotColor =
    when (status.lowercase()) {
      "online" -> StatusOnlineGreen
      "away" -> StatusAwayYellow
      "busy" -> StatusBusyRed
      else -> StatusOfflineGray
    }

  Box(
    modifier =
      modifier
        .size(size)
        .shadow(3.dp, CircleShape)
        .clip(CircleShape)
        .background(
          brush =
            Brush.radialGradient(
              colors =
                listOf(
                  Color.White.copy(alpha = 0.75f),
                  dotColor,
                  dotColor.copy(alpha = 0.85f),
                )
            )
        )
        .border(1.dp, Color(0xAA000000), CircleShape)
  )
}

/**
 * Draws the iconic 3D translucent glossy MSN Messenger / Frutiger Aero buddy silhouette inside
 * a beveled glass squircle frame matching the reference screenshot.
 */
@Composable
fun GlossyBuddyAvatar(
  colorKey: String,
  size: Dp = 52.dp,
  modifier: Modifier = Modifier,
) {
  val corner = size * 0.22f
  val shape = RoundedCornerShape(corner)

  Box(
    modifier =
      modifier
        .size(size)
        .shadow(6.dp, shape)
        .clip(shape)
        .border(
          width = 1.5.dp,
          brush =
            Brush.verticalGradient(
              colors =
                listOf(
                  Color(0xAAFFFFFF),
                  Color(0x4480CBC4),
                  Color(0x7726A69A),
                )
            ),
          shape = shape,
        )
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val w = this.size.width
      val h = this.size.height

      // Beveled glass frame background
      val bgColors =
        when (colorKey.lowercase()) {
          "aqua" -> listOf(Color(0xFF1A5E57), Color(0xFF0C3B36), Color(0xFF062320))
          "red" -> listOf(Color(0xFF422C2C), Color(0xFF221818), Color(0xFF130D0D))
          "green" -> listOf(Color(0xFF1E492D), Color(0xFF0F2D1B), Color(0xFF081B10))
          "blue" -> listOf(Color(0xFF1E3A54), Color(0xFF102438), Color(0xFF091624))
          "gold" -> listOf(Color(0xFF4D3F1B), Color(0xFF2B230E), Color(0xFF181307))
          "purple" -> listOf(Color(0xFF3E2454), Color(0xFF231333), Color(0xFF130A1C))
          else -> listOf(Color(0xFF1E4D42), Color(0xFF0E2E27), Color(0xFF071B17))
        }

      drawRect(brush = Brush.verticalGradient(colors = bgColors))

      // Subtle inner radial glow
      drawCircle(
        brush =
          Brush.radialGradient(
            colors = listOf(Color(0x35FFFFFF), Color.Transparent),
            center = Offset(w * 0.5f, h * 0.45f),
            radius = w * 0.55f,
          ),
        radius = w * 0.55f,
        center = Offset(w * 0.5f, h * 0.45f),
      )

      if (colorKey.equals("group", ignoreCase = true)) {
        // Draw 3 overlapping glossy 3D pawns (Red at back-center, Green at front-left, Blue at front-right)
        // matching the "Group chat" icon in the reference screenshot!
        drawGlossyPawn(
          centerX = w * 0.54f,
          centerY = h * 0.44f,
          scale = 0.62f,
          lightColor = Color(0xFFFF6B6B),
          midColor = Color(0xFFE50914),
          darkColor = Color(0xFF6E0000),
        )
        drawGlossyPawn(
          centerX = w * 0.34f,
          centerY = h * 0.57f,
          scale = 0.64f,
          lightColor = Color(0xFF69F0AE),
          midColor = Color(0xFF00C853),
          darkColor = Color(0xFF004D25),
        )
        drawGlossyPawn(
          centerX = w * 0.67f,
          centerY = h * 0.60f,
          scale = 0.62f,
          lightColor = Color(0xFF64B5F6),
          midColor = Color(0xFF1565C0),
          darkColor = Color(0xFF082556),
        )
      } else {
        val (lightC, midC, darkC) =
          when (colorKey.lowercase()) {
            "aqua" -> Triple(Color(0xFF84FFFF), Color(0xFF00BFA5), Color(0xFF004D40))
            "red" -> Triple(Color(0xFFFF6E6E), Color(0xFFD50000), Color(0xFF5C0000))
            "green" -> Triple(Color(0xFF69F0AE), Color(0xFF00C853), Color(0xFF004D22))
            "blue" -> Triple(Color(0xFF64B5F6), Color(0xFF1565C0), Color(0xFF072252))
            "gold" -> Triple(Color(0xFFFFE57F), Color(0xFFFFAB00), Color(0xFF664200))
            "purple" -> Triple(Color(0xFFEA80FC), Color(0xFF8E24AA), Color(0xFF38004D))
            else -> Triple(Color(0xFF84FFFF), Color(0xFF00BFA5), Color(0xFF004D40))
          }

        drawGlossyPawn(
          centerX = w * 0.50f,
          centerY = h * 0.54f,
          scale = 0.88f,
          lightColor = lightC,
          midColor = midC,
          darkColor = darkC,
        )
      }

      // Top frame glossy glass reflection arc
      drawRoundRect(
        brush =
          Brush.verticalGradient(
            colors =
              listOf(
                Color(0x45FFFFFF),
                Color(0x10FFFFFF),
                Color.Transparent,
              ),
            startY = 0f,
            endY = h * 0.45f,
          ),
        topLeft = Offset(w * 0.06f, h * 0.04f),
        size = Size(w * 0.88f, h * 0.38f),
        cornerRadius = CornerRadius(w * 0.16f, w * 0.16f),
      )
    }
  }
}

private fun DrawScope.drawGlossyPawn(
  centerX: Float,
  centerY: Float,
  scale: Float,
  lightColor: Color,
  midColor: Color,
  darkColor: Color,
) {
  val w = size.width * scale
  val h = size.height * scale

  // Body / Shoulders dome path (classic MSN curved 3D bust)
  val bodyTopY = centerY - h * 0.05f
  val bodyBottomY = centerY + h * 0.36f
  val bodyHalfW = w * 0.36f

  val bodyPath =
    Path().apply {
      moveTo(centerX - bodyHalfW, bodyBottomY - h * 0.06f)
      cubicTo(
        centerX - bodyHalfW * 1.02f,
        bodyTopY + h * 0.05f,
        centerX - bodyHalfW * 0.45f,
        bodyTopY - h * 0.02f,
        centerX,
        bodyTopY - h * 0.02f,
      )
      cubicTo(
        centerX + bodyHalfW * 0.45f,
        bodyTopY - h * 0.02f,
        centerX + bodyHalfW * 1.02f,
        bodyTopY + h * 0.05f,
        centerX + bodyHalfW,
        bodyBottomY - h * 0.06f,
      )
      cubicTo(
        centerX + bodyHalfW * 0.6f,
        bodyBottomY + h * 0.05f,
        centerX - bodyHalfW * 0.6f,
        bodyBottomY + h * 0.05f,
        centerX - bodyHalfW,
        bodyBottomY - h * 0.06f,
      )
      close()
    }

  drawPath(
    path = bodyPath,
    brush =
      Brush.radialGradient(
        colors = listOf(lightColor, midColor, darkColor),
        center = Offset(centerX - bodyHalfW * 0.28f, bodyTopY + h * 0.08f),
        radius = bodyHalfW * 1.35f,
      ),
  )

  // Specular highlight curve on body shoulder
  val shinePath =
    Path().apply {
      moveTo(centerX - bodyHalfW * 0.68f, bodyBottomY - h * 0.14f)
      cubicTo(
        centerX - bodyHalfW * 0.65f,
        bodyTopY + h * 0.04f,
        centerX - bodyHalfW * 0.10f,
        bodyTopY + h * 0.02f,
        centerX + bodyHalfW * 0.25f,
        bodyTopY + h * 0.08f,
      )
      cubicTo(
        centerX - bodyHalfW * 0.05f,
        bodyTopY + h * 0.12f,
        centerX - bodyHalfW * 0.42f,
        bodyTopY + h * 0.18f,
        centerX - bodyHalfW * 0.68f,
        bodyBottomY - h * 0.14f,
      )
      close()
    }
  drawPath(path = shinePath, color = Color.White.copy(alpha = 0.50f))

  // 3D Head sphere
  val headRadius = w * 0.195f
  val headCenter = Offset(centerX, centerY - h * 0.22f)

  drawCircle(
    brush =
      Brush.radialGradient(
        colors = listOf(Color.White.copy(alpha = 0.9f), lightColor, midColor, darkColor),
        center = Offset(headCenter.x - headRadius * 0.32f, headCenter.y - headRadius * 0.32f),
        radius = headRadius * 1.3f,
      ),
    radius = headRadius,
    center = headCenter,
  )

  // Head top-left specular gleam
  drawCircle(
    color = Color.White.copy(alpha = 0.62f),
    radius = headRadius * 0.32f,
    center = Offset(headCenter.x - headRadius * 0.30f, headCenter.y - headRadius * 0.30f),
  )
}

/**
 * Compact E2EE security badge displaying AES-256-GCM lock status.
 */
@Composable
fun E2eeSecurityBadge(
  text: String = "🔒 AES-256-GCM",
  onClick: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  val shape = RoundedCornerShape(50)
  Surface(
    shape = shape,
    color = Color(0x55064E3B),
    border = BorderStroke(1.dp, AeroEmeraldBright.copy(alpha = 0.55f)),
    modifier =
      if (onClick != null) {
        modifier.clip(shape).clickable(onClick = onClick)
      } else {
        modifier
      },
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp),
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
      Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = AeroIceWhite,
        fontWeight = FontWeight.Medium,
      )
    }
  }
}
