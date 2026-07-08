package com.booklog.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.booklog.app.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.booklog.app.data.audio.AppAudioManager
import com.booklog.app.data.audio.AudioPreferences
import com.booklog.app.data.profiles.KidAgeCalculator
import com.booklog.app.ui.components.KeyboardAwareScrollColumn
import com.booklog.app.ui.theme.CoralPink
import com.booklog.app.ui.theme.Lavender
import com.booklog.app.ui.theme.MintGreen
import com.booklog.app.ui.theme.SkyBlue
import com.booklog.app.ui.theme.SunnyYellow
import com.booklog.app.viewmodel.AuthViewModel
import com.booklog.app.viewmodel.KidsViewModel

@Composable
fun ProfileScreen(
    viewModel: AuthViewModel,
    kidsViewModel: KidsViewModel,
    audioPreferences: AudioPreferences,
    audioManager: AppAudioManager,
    onLeaderboardClick: () -> Unit,
    onKidProfilesClick: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val kidsState by kidsViewModel.uiState.collectAsStateWithLifecycle()
    val musicMuted by audioPreferences.musicMuted.collectAsStateWithLifecycle()
    val soundsMuted by audioPreferences.soundsMuted.collectAsStateWithLifecycle()
    var isSignUp by rememberSaveable { mutableStateOf(true) }
    var displayName by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    val context = LocalContext.current
    val webClientId = stringResource(R.string.default_web_client_id)
    val googleSignInClient = remember(webClientId) {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
        GoogleSignIn.getClient(context, gso)
    }
    val googleSignInLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        runCatching {
            val account = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                .getResult(ApiException::class.java)
            val idToken = account.idToken
            if (idToken != null) {
                viewModel.signInWithGoogle(idToken)
            } else {
                viewModel.clearMessages()
            }
        }.onFailure {
            viewModel.clearMessages()
        }
    }

    KeyboardAwareScrollColumn(
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            "My Reading Journey",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )

        CloudPromoCard(onLeaderboardClick = onLeaderboardClick)

        AudioSettingsCard(
            musicMuted = musicMuted,
            soundsMuted = soundsMuted,
            onMusicMutedChange = { muted ->
                audioPreferences.setMusicMuted(muted)
                audioManager.onMusicPreferenceChanged()
            },
            onSoundsMutedChange = { audioPreferences.setSoundsMuted(it) },
        )

        KidProfilesLinkCard(
            kidsCount = kidsState.kids.size,
            activeKid = kidsState.activeKid,
            onManageClick = onKidProfilesClick,
        )

        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }
        state.successMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.bodyMedium)
        }

        if (state.user != null) {
            val profile = state.profile
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(4.dp),
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(SkyBlue, Lavender))),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                        }
                        Column {
                            Text(profile?.displayName ?: state.user?.displayName ?: "Reader", fontWeight = FontWeight.Bold)
                            Text(state.user?.email ?: "", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    StatRow("Books finished", "${profile?.booksFinished ?: 0}")
                    StatRow("Pages read", "${profile?.pagesRead ?: 0}")
                    StatRow("Milestones unlocked", "${profile?.milestonesUnlocked ?: 0}")
                    StatRow("Total in library", "${profile?.booksTotal ?: 0}")
                }
            }

            Button(
                onClick = viewModel::syncNow,
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(Icons.Default.Cloud, contentDescription = null)
                Text("  Sync to Cloud", modifier = Modifier.padding(start = 4.dp))
            }
            OutlinedButton(
                onClick = viewModel::signOut,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text("Sign Out")
            }
        } else if (state.isGuestMode) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MintGreen.copy(alpha = 0.25f)),
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Guest mode", fontWeight = FontWeight.Bold)
                    Text(
                        "Your books stay on this device. Sign in anytime to sync and join global leaderboards.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            OutlinedButton(
                onClick = viewModel::exitGuestMode,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text("Exit Guest Mode")
            }
        } else {
            Text(
                if (isSignUp) "Create your account" else "Welcome back!",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Save your library in the cloud, add kid profiles, and climb global leaderboards!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else {
                OutlinedButton(
                    onClick = {
                        viewModel.clearMessages()
                        viewModel.continueAsGuest()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text("Continue as Guest")
                }
                OutlinedButton(
                    onClick = {
                        viewModel.clearMessages()
                        googleSignInLauncher.launch(googleSignInClient.signInIntent)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text("Continue with Google")
                }
            }

            if (isSignUp) {
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Display name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
            )

            if (!state.isLoading) {
                Button(
                    onClick = {
                        viewModel.clearMessages()
                        kidsViewModel.clearMessages()
                        if (isSignUp) viewModel.signUp(displayName, email, password)
                        else viewModel.signIn(email, password)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                ) {
                    Text(if (isSignUp) "Create Account & Join Leaderboard" else "Sign In with Email")
                }
                OutlinedButton(
                    onClick = { isSignUp = !isSignUp; viewModel.clearMessages() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text(if (isSignUp) "Already have an account? Sign in" else "New here? Create account")
                }
            }
        }
    }
}

@Composable
private fun KidProfilesLinkCard(
    kidsCount: Int,
    activeKid: com.booklog.app.data.local.KidProfile?,
    onManageClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SkyBlue.copy(alpha = 0.15f)),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ChildCare, contentDescription = null, tint = CoralPink)
                Text("Kid Profiles", fontWeight = FontWeight.Bold)
            }
            Text(
                if (kidsCount == 0) {
                    "Create profiles with name, birthday, gender, and more."
                } else {
                    "Reading as: ${activeKid?.let { "${it.emoji} ${it.firstName}" } ?: "Parent"} · $kidsCount profile${if (kidsCount == 1) "" else "s"}"
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            activeKid?.dateOfBirth?.let {
                Text(
                    KidAgeCalculator.ageLabel(it),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OutlinedButton(
                onClick = onManageClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("Manage Kid Profiles")
            }
        }
    }
}

@Composable
private fun AudioSettingsCard(
    musicMuted: Boolean,
    soundsMuted: Boolean,
    onMusicMutedChange: (Boolean) -> Unit,
    onSoundsMutedChange: (Boolean) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Lavender.copy(alpha = 0.2f)),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.MusicNote, contentDescription = null, tint = CoralPink)
                Text("Sounds & Music", fontWeight = FontWeight.Bold)
            }
            Text(
                "Control fun background music and celebration sounds separately.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            AudioToggleRow(
                icon = Icons.Default.MusicNote,
                label = "Background music",
                subtitle = "Plays on Books and Milestones screens",
                checked = !musicMuted,
                onCheckedChange = { onMusicMutedChange(!it) },
            )
            AudioToggleRow(
                icon = if (soundsMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                label = "Sound effects",
                subtitle = "Milestones, saves, scans, and redemptions",
                checked = !soundsMuted,
                onCheckedChange = { onSoundsMutedChange(!it) },
            )
        }
    }
}

@Composable
private fun AudioToggleRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = SkyBlue)
            Column {
                Text(label, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun CloudPromoCard(onLeaderboardClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SunnyYellow.copy(alpha = 0.35f)),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = CoralPink)
                Text("Cloud + Leaderboards", fontWeight = FontWeight.Bold)
            }
            Text(
                "Global leaderboards for readers, kids, authors, publishers, genres, and milestones!",
                style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedButton(onClick = onLeaderboardClick, shape = RoundedCornerShape(12.dp)) {
                Text("View Leaderboards")
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Bold)
    }
}