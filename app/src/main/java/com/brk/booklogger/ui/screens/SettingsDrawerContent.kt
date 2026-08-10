package com.brk.booklogger.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.brk.booklogger.R
import com.brk.booklogger.data.audio.AppAudioManager
import com.brk.booklogger.data.audio.AudioPreferences
import com.brk.booklogger.data.local.KidProfile
import com.brk.booklogger.data.profiles.KidAgeCalculator
import com.brk.booklogger.ui.theme.CoralPink
import com.brk.booklogger.ui.theme.Lavender
import com.brk.booklogger.ui.theme.MintGreen
import com.brk.booklogger.ui.theme.SkyBlue
import com.brk.booklogger.ui.theme.SunnyYellow
import com.brk.booklogger.viewmodel.AuthViewModel
import com.brk.booklogger.viewmodel.HouseholdUiState
import com.brk.booklogger.viewmodel.HouseholdViewModel
import com.brk.booklogger.viewmodel.KidsViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

/**
 * Full settings panel (account, audio, readers, household) for the left slide-out drawer.
 */
@Composable
fun SettingsDrawerContent(
    viewModel: AuthViewModel,
    kidsViewModel: KidsViewModel,
    householdViewModel: HouseholdViewModel,
    audioPreferences: AudioPreferences,
    audioManager: AppAudioManager,
    onLeaderboardClick: () -> Unit,
    onKidProfilesClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val kidsState by kidsViewModel.uiState.collectAsStateWithLifecycle()
    val householdState by householdViewModel.uiState.collectAsStateWithLifecycle()
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

    Column(
        modifier = modifier
            .width(320.dp)
            .fillMaxHeight()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            "Settings",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            "Account, sounds, readers & household",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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

        ReadersLinkCard(
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
        householdState.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }
        householdState.successMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.bodyMedium)
        }

        if (state.user != null) {
            val profile = state.profile
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(4.dp),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(SkyBlue, Lavender))),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                        Column {
                            Text(
                                profile?.displayName ?: state.user?.displayName ?: "Reader",
                                fontWeight = FontWeight.Bold,
                            )
                            Text(state.user?.email ?: "", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    StatRow("Books finished", "${profile?.booksFinished ?: 0}")
                    StatRow("Pages read", "${profile?.pagesRead ?: 0}")
                    StatRow("Milestones", "${profile?.milestonesUnlocked ?: 0}")
                    StatRow("Library total", "${profile?.booksTotal ?: 0}")
                }
            }

            PartnerHouseholdCard(
                state = householdState,
                onCreate = householdViewModel::createHousehold,
                onJoin = householdViewModel::joinHousehold,
                onLeave = householdViewModel::leaveHousehold,
                onRegenerate = householdViewModel::regenerateCode,
                onPull = householdViewModel::pullLibrary,
                onJoinCodeChange = householdViewModel::onJoinCodeChange,
            )

            Button(
                onClick = {
                    viewModel.syncNow()
                    householdViewModel.pullLibrary()
                },
                enabled = !state.isLoading && !householdState.isLoading,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(Icons.Default.Cloud, contentDescription = null)
                Text("  Sync library", modifier = Modifier.padding(start = 4.dp))
            }
            OutlinedButton(
                onClick = {
                    householdViewModel.clearMessages()
                    viewModel.signOut()
                },
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
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Guest mode", fontWeight = FontWeight.Bold)
                    Text(
                        "Your books stay on this device. Sign in anytime to sync.",
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
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
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
                    Text(if (isSignUp) "Create Account" else "Sign In")
                }
                OutlinedButton(
                    onClick = { isSignUp = !isSignUp; viewModel.clearMessages() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text(if (isSignUp) "Have an account? Sign in" else "New here? Create account")
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ReadersLinkCard(
    kidsCount: Int,
    activeKid: KidProfile?,
    onManageClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SkyBlue.copy(alpha = 0.15f)),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.ChildCare, contentDescription = null, tint = CoralPink)
                Text("Readers", fontWeight = FontWeight.Bold)
            }
            Text(
                if (kidsCount == 0) {
                    "Add adult and child readers so everyone can log books."
                } else {
                    val who = activeKid?.let { "${it.emoji} ${it.firstName}" } ?: "a reader"
                    "Active: $who · $kidsCount reader${if (kidsCount == 1) "" else "s"}"
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
                Text("Manage Readers")
            }
        }
    }
}

@Composable
private fun PartnerHouseholdCard(
    state: HouseholdUiState,
    onCreate: () -> Unit,
    onJoin: () -> Unit,
    onLeave: () -> Unit,
    onRegenerate: () -> Unit,
    onPull: () -> Unit,
    onJoinCodeChange: (String) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CoralPink.copy(alpha = 0.12f)),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Partner household", fontWeight = FontWeight.Bold)
            Text(
                "Share one library with a partner.",
                style = MaterialTheme.typography.bodyMedium,
            )
            if (state.household.isLinked) {
                Text(
                    "Members: ${state.household.memberNames.joinToString(" · ").ifBlank { "You + partner" }}",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    "Code: ${state.household.inviteCode}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onPull,
                        enabled = !state.isLoading,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                    ) { Text("Refresh") }
                    OutlinedButton(
                        onClick = onRegenerate,
                        enabled = !state.isLoading,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                    ) { Text("New code") }
                }
                OutlinedButton(
                    onClick = onLeave,
                    enabled = !state.isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                ) { Text("Leave household") }
            } else {
                Button(
                    onClick = onCreate,
                    enabled = !state.isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                ) { Text("Create household") }
                Text("Or join with a code:", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = state.joinCodeInput,
                    onValueChange = onJoinCodeChange,
                    label = { Text("Invite code") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedButton(
                    onClick = onJoin,
                    enabled = !state.isLoading && state.joinCodeInput.length >= 4,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                ) { Text("Join household") }
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
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.MusicNote, contentDescription = null, tint = CoralPink)
                Text("Sounds & Music", fontWeight = FontWeight.Bold)
            }
            AudioToggleRow(
                icon = Icons.Default.MusicNote,
                label = "Background music",
                subtitle = "Books & Milestones",
                checked = !musicMuted,
                onCheckedChange = { onMusicMutedChange(!it) },
            )
            AudioToggleRow(
                icon = if (soundsMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                label = "Sound effects",
                subtitle = "Saves, scans, milestones",
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
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = CoralPink)
                Text("Cloud + Leaderboards", fontWeight = FontWeight.Bold)
            }
            Text(
                "Sync your library and climb global leaderboards.",
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
