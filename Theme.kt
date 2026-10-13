package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val FrutigerAeroColorScheme =
  darkColorScheme(
    primary = AeroEmeraldBright,
    onPrimary = AeroDeepForest,
    primaryContainer = AeroEmeraldMid,
    onPrimaryContainer = AeroIceWhite,
    secondary = AeroAquaCyan,
    onSecondary = AeroDeepForest,
    secondaryContainer = AeroGlassSurfaceLight,
    onSecondaryContainer = AeroSoftMint,
    tertiary = AeroSkyBlue,
    background = AeroDeepForest,
    onBackground = AeroIceWhite,
    surface = AeroGlassSurface,
    onSurface = AeroIceWhite,
    surfaceVariant = AeroEmeraldDark,
    onSurfaceVariant = AeroSoftMint,
    outline = AeroGlassBorder,
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = FrutigerAeroColorScheme,
    typography = Typography,
    content = content,
  )
}
