package com.example.ui.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CancellationException
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.ui.components.AeroGlassCard
import com.example.ui.components.E2eeSecurityBadge
import com.example.ui.components.FrutigerAeroWallpaperBox
import com.example.ui.components.GlossyBuddyAvatar
import com.example.ui.components.PresenceDot
import com.example.ui.theme.AeroAquaCyan
import com.example.ui.theme.AeroEmeraldBright
import com.example.ui.theme.AeroIceWhite
import com.example.ui.theme.AeroSoftMint
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class PendingRegistrationData(
  val username: String,
  val signature: String,
  val avatarColor: String,
  val vaultPassword: String,
)

fun Context.findActivity(): Activity? {
  var currentContext = this
  while (currentContext is ContextWrapper) {
    if (currentContext is Activity) {
      return currentContext
    }
    currentContext = currentContext.baseContext
  }
  return null
}

fun attemptAutoSignIn(
  context: Context,
  credentialManager: CredentialManager,
  onAuthSuccess: () -> Unit,
  onUnauthenticated: () -> Unit,
  scope: CoroutineScope,
) {
  if (Firebase.auth.currentUser != null) {
    onAuthSuccess()
    return
  }
  val clientId =
    try {
      context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
      onUnauthenticated()
      return
    }

  val googleIdOption =
    GetGoogleIdOption.Builder()
      .setFilterByAuthorizedAccounts(true)
      .setServerClientId(clientId)
      .setAutoSelectEnabled(true)
      .build()

  val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()
  val activity = context.findActivity()
  val executionScope = (activity as? LifecycleOwner)?.lifecycleScope ?: scope

  executionScope.launch {
    try {
      val activityContext = activity ?: context
      val result = credentialManager.getCredential(activityContext, request)
      val credential = result.credential
      if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
        val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
        Firebase.auth.signInWithCredential(authCredential).await()
        onAuthSuccess()
      } else {
        onUnauthenticated()
      }
    } catch (e: CancellationException) {
      // Normal coroutine cancellation when composition or activity changes
      Log.d("Auth", "attemptAutoSignIn coroutine cancelled", e)
    } catch (e: Exception) {
      onUnauthenticated()
    }
  }
}

fun onGoogleSignInClicked(
  context: Context,
  credentialManager: CredentialManager,
  onAuthSuccess: () -> Unit,
  onAuthError: (String) -> Unit,
  scope: CoroutineScope,
  onAuthCancelled: () -> Unit = {},
) {
  val clientId =
    try {
      context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
      onAuthError("Google Sign-In configuration missing: default_web_client_id not found")
      return
    }

  val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
  val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()
  val activity = context.findActivity()
  val executionScope = (activity as? LifecycleOwner)?.lifecycleScope ?: scope

  executionScope.launch {
    try {
      val activityContext = activity ?: context
      val result = credentialManager.getCredential(activityContext, request)
      val credential = result.credential
      if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
        val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
        Firebase.auth.signInWithCredential(authCredential).await()
        onAuthSuccess()
      } else {
        onAuthError("Unexpected credential type")
      }
    } catch (e: GetCredentialCancellationException) {
      Log.w("Auth", "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
      onAuthCancelled()
    } catch (e: CancellationException) {
      // The coroutine scope left the composition or user navigated away
      Log.d("Auth", "Google Sign-In coroutine cancelled", e)
    } catch (e: Exception) {
      Log.e("Auth", "Google Sign-In failed", e)
      onAuthError(e.localizedMessage ?: "Sign in failed")
    }
  }
}

fun signOut(
  context: Context,
  credentialManager: CredentialManager,
  onSignOutComplete: () -> Unit,
  scope: CoroutineScope,
) {
  Firebase.auth.signOut()
  val activity = context.findActivity()
  val executionScope = (activity as? LifecycleOwner)?.lifecycleScope ?: scope

  executionScope.launch {
    try {
      credentialManager.clearCredentialState(ClearCredentialStateRequest())
    } catch (e: CancellationException) {
      // ignore
    } catch (e: Exception) {
      Log.e("Auth", "Failed to clear credential state", e)
    } finally {
      onSignOutComplete()
    }
  }
}

@Composable
fun AuthScreen(
  onAuthCompleted: (PendingRegistrationData) -> Unit,
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val credentialManager = remember { CredentialManager.create(context) }

  var username by remember { mutableStateOf("AeroWave") }
  var signature by remember { mutableStateOf("Feeling glossy & connected ~ 🌊🫧") }
  var vaultPassword by remember { mutableStateOf("frutiger-aero-2007") }
  var showPassword by remember { mutableStateOf(false) }
  var selectedAvatarColor by remember { mutableStateOf("aqua") }
  var isLoading by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  LaunchedEffect(Unit) {
    attemptAutoSignIn(
      context = context,
      credentialManager = credentialManager,
      onAuthSuccess = {
        onAuthCompleted(
          PendingRegistrationData(
            username = username,
            signature = signature,
            avatarColor = selectedAvatarColor,
            vaultPassword = vaultPassword,
          )
        )
      },
      onUnauthenticated = {},
      scope = coroutineScope,
    )
  }

  FrutigerAeroWallpaperBox(waterEffectsEnabled = true) {
    Box(
      contentAlignment = Alignment.Center,
      modifier =
        Modifier.fillMaxSize()
          .systemBarsPadding()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
      AeroGlassCard(
        cornerRadius = 24.dp,
        modifier = Modifier.fillMaxWidth().widthIn(max = 460.dp),
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier.fillMaxWidth().padding(24.dp),
        ) {
          // Classic MSN Messenger / Frutiger Aero Header Badge
          Box(contentAlignment = Alignment.BottomEnd) {
            GlossyBuddyAvatar(colorKey = selectedAvatarColor, size = 86.dp)
            PresenceDot(status = "online", size = 18.dp, modifier = Modifier.padding(4.dp))
          }

          Spacer(modifier = Modifier.height(14.dp))

          Text(
            text = "Frutiger Chat",
            style = MaterialTheme.typography.headlineLarge,
            color = AeroIceWhite,
          )

          Text(
            text = "Windows Live Aero • Nostalgic Instant Messenger",
            style = MaterialTheme.typography.bodyMedium,
            color = AeroSoftMint,
            textAlign = TextAlign.Center,
          )

          Spacer(modifier = Modifier.height(10.dp))

          E2eeSecurityBadge(text = "🔒 AES-256-GCM + Local Room Vault + Socket.IO")

          Spacer(modifier = Modifier.height(18.dp))

          // Avatar Color Picker Row
          Text(
            text = "Choose Your Glossy Buddy Icon:",
            style = MaterialTheme.typography.labelLarge,
            color = AeroSoftMint,
          )
          Spacer(modifier = Modifier.height(8.dp))
          Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            listOf("aqua", "green", "red", "blue", "gold", "purple").forEach { colorKey ->
              val isSelected = selectedAvatarColor == colorKey
              Box(
                modifier =
                  Modifier.clip(RoundedCornerShape(10.dp))
                    .border(
                      width = if (isSelected) 2.dp else 0.dp,
                      color = if (isSelected) AeroAquaCyan else Color.Transparent,
                      shape = RoundedCornerShape(10.dp),
                    )
                    .clickable { selectedAvatarColor = colorKey }
                    .padding(2.dp)
              ) {
                GlossyBuddyAvatar(colorKey = colorKey, size = 38.dp)
              }
            }
          }

          Spacer(modifier = Modifier.height(16.dp))

          // Username Field
          OutlinedTextField(
            value = username,
            onValueChange = { username = it.take(40) },
            label = { Text("Frutiger Display Username") },
            leadingIcon = {
              Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Username",
                tint = AeroAquaCyan,
              )
            },
            singleLine = true,
            colors = aeroTextFieldColors(),
            modifier = Modifier.fillMaxWidth().testTag("username_input"),
          )

          Spacer(modifier = Modifier.height(10.dp))

          // Custom Signature Field
          OutlinedTextField(
            value = signature,
            onValueChange = { signature = it.take(140) },
            label = { Text("Custom Personal Signature") },
            singleLine = true,
            colors = aeroTextFieldColors(),
            modifier = Modifier.fillMaxWidth().testTag("signature_input"),
          )

          Spacer(modifier = Modifier.height(10.dp))

          // Account passcode
          OutlinedTextField(
            value = vaultPassword,
            onValueChange = { vaultPassword = it.take(64) },
            label = { Text("Account Passcode (Optional)") },
            leadingIcon = {
              Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Account Passcode",
                tint = AeroEmeraldBright,
              )
            },
            trailingIcon = {
              IconButton(onClick = { showPassword = !showPassword }) {
                Icon(
                  imageVector =
                    if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                  contentDescription = "Toggle password visibility",
                  tint = AeroSoftMint,
                )
              }
            },
            visualTransformation =
              if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            singleLine = true,
            colors = aeroTextFieldColors(),
            modifier = Modifier.fillMaxWidth().testTag("vault_password_input"),
          )

          Spacer(modifier = Modifier.height(6.dp))

          Text(
            text = "Sign in with your Google account to access your Frutiger Chat profile and chats safely across devices.",
            style = MaterialTheme.typography.labelSmall,
            color = AeroSoftMint.copy(alpha = 0.85f),
            textAlign = TextAlign.Center,
          )

          if (errorMessage != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
              color = Color(0x667F1D1D),
              shape = RoundedCornerShape(10.dp),
              border = BorderStroke(1.dp, Color(0xFFF87171)),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Column(modifier = Modifier.padding(10.dp)) {
                Text(
                  text = errorMessage ?: "",
                  style = MaterialTheme.typography.bodyMedium,
                  color = Color(0xFFFECACA),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = "Tip: Ensure your Android device has a Google account signed in (Settings > Accounts) and an active Internet connection.",
                  style = MaterialTheme.typography.labelSmall,
                  color = Color(0xFFFCA5A5),
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(18.dp))

          // Glossy Frutiger Aero "Sign in with Google" Button
          Button(
            onClick = {
              errorMessage = null
              isLoading = true
              onGoogleSignInClicked(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {
                  isLoading = false
                  onAuthCompleted(
                    PendingRegistrationData(
                      username = username.ifBlank { "AeroUser" },
                      signature = signature,
                      avatarColor = selectedAvatarColor,
                      vaultPassword = vaultPassword.ifBlank { "frutiger-aero-e2ee" },
                    )
                  )
                },
                onAuthError = { msg ->
                  isLoading = false
                  errorMessage = msg
                },
                scope = coroutineScope,
                onAuthCancelled = { isLoading = false },
              )
            },
            enabled = !isLoading,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
            modifier =
              Modifier.fillMaxWidth()
                .height(52.dp)
                .testTag("google_sign_in_button"),
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
                            Color(0xFF34D399),
                            Color(0xFF059669),
                            Color(0xFF065F46),
                          )
                      ),
                    shape = RoundedCornerShape(14.dp),
                  )
                  .border(
                    width = 1.5.dp,
                    brush =
                      Brush.verticalGradient(
                        colors = listOf(Color.White, AeroAquaCyan, Color(0xFF10B981))
                      ),
                    shape = RoundedCornerShape(14.dp),
                  ),
            ) {
              // Top specular gloss highlight
              Box(
                modifier =
                  Modifier.fillMaxWidth()
                    .height(24.dp)
                    .align(Alignment.TopCenter)
                    .background(
                      brush =
                        Brush.verticalGradient(
                          colors = listOf(Color(0x70FFFFFF), Color.Transparent)
                        )
                    )
              )
              if (isLoading) {
                CircularProgressIndicator(
                  modifier = Modifier.size(24.dp),
                  color = AeroIceWhite,
                  strokeWidth = 2.5.dp,
                )
              } else {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.Center,
                ) {
                  Box(
                    contentAlignment = Alignment.Center,
                    modifier =
                      Modifier.size(26.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                  ) {
                    Text(
                      text = "G",
                      color = Color(0xFF047857),
                      fontWeight = FontWeight.ExtraBold,
                      fontSize = 15.sp,
                    )
                  }
                  Spacer(modifier = Modifier.width(10.dp))
                  Text(
                    text = "Sign in with Google",
                    color = AeroIceWhite,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun aeroTextFieldColors() =
  OutlinedTextFieldDefaults.colors(
    focusedTextColor = AeroIceWhite,
    unfocusedTextColor = AeroIceWhite,
    focusedContainerColor = Color(0x66041E13),
    unfocusedContainerColor = Color(0x4D041E13),
    focusedBorderColor = AeroAquaCyan,
    unfocusedBorderColor = Color(0x666EE7B7),
    focusedLabelColor = AeroAquaCyan,
    unfocusedLabelColor = AeroSoftMint,
    cursorColor = AeroAquaCyan,
  )
