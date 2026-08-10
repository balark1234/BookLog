package com.brk.booklogger.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.brk.booklogger.data.local.KidProfile
import com.brk.booklogger.data.local.ReadingStatus
import com.brk.booklogger.data.profiles.KidAgeCalculator
import com.brk.booklogger.data.streak.ReadingStreakInfo
import com.brk.booklogger.ui.components.KeyboardAwareScrollColumn
import com.brk.booklogger.ui.theme.CoralPink
import com.brk.booklogger.ui.theme.Lavender
import com.brk.booklogger.ui.theme.MintGreen
import com.brk.booklogger.ui.theme.SkyBlue
import com.brk.booklogger.ui.theme.SunnyYellow
import com.brk.booklogger.viewmodel.KidsViewModel
import com.brk.booklogger.viewmodel.LibraryViewModel
import com.brk.booklogger.viewmodel.MilestonesViewModel

/**
 * "Reader" tab: profile details and stats for the currently selected reader.
 * Account / settings live in the hamburger drawer.
 */
@Composable
fun ReaderHomeScreen(
    kidsViewModel: KidsViewModel,
    libraryViewModel: LibraryViewModel,
    milestonesViewModel: MilestonesViewModel,
    streakInfo: ReadingStreakInfo,
    onManageReaders: () -> Unit,
    onMilestonesClick: () -> Unit,
    onLibraryClick: () -> Unit,
) {
    val kidsState by kidsViewModel.uiState.collectAsStateWithLifecycle()
    val books by libraryViewModel.books.collectAsStateWithLifecycle()
    val milestonesState by milestonesViewModel.uiState.collectAsStateWithLifecycle()
    val reader = kidsState.activeKid

    val total = books.size
    val want = books.count { it.status == ReadingStatus.WANT_TO_READ }
    val reading = books.count { it.status == ReadingStatus.READING }
    val finished = books.count { it.status == ReadingStatus.FINISHED }
    val pagesRead = books
        .filter { it.status == ReadingStatus.FINISHED }
        .sumOf { it.pageCount ?: 0 }
    val balanceLabel = com.brk.booklogger.data.repository.RewardRepository
        .formatCents(milestonesState.snapshot.availableBalanceCents)
    val unlocked = milestonesState.unlockedCount

    KeyboardAwareScrollColumn(
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        Text(
            "Reader",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            "Stats and details for the selected reader",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (reader == null) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MintGreen.copy(alpha = 0.25f)),
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("No reader selected", fontWeight = FontWeight.Bold)
                    Text(
                        "Use the reader menu on the top right, or add readers from Settings.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    OutlinedButton(onClick = onManageReaders, shape = RoundedCornerShape(12.dp)) {
                        Text("Manage readers")
                    }
                }
            }
        } else {
            ReaderProfileCard(reader = reader)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                MiniStatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.MenuBook,
                    label = "Books",
                    value = total.toString(),
                    color = SkyBlue,
                )
                MiniStatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.LocalFireDepartment,
                    label = "Streak",
                    value = "${streakInfo.currentStreak}d",
                    color = CoralPink,
                )
                MiniStatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.EmojiEvents,
                    label = "Badges",
                    value = unlocked.toString(),
                    color = SunnyYellow,
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(3.dp),
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Reading stats", fontWeight = FontWeight.Bold)
                    DetailRow("Want to read", want.toString())
                    DetailRow("Currently reading", reading.toString())
                    DetailRow("Finished", finished.toString())
                    DetailRow("Pages finished", pagesRead.toString())
                    DetailRow("Read today", if (streakInfo.readToday) "Yes (${streakInfo.pagesToday} pages)" else "Not yet")
                    DetailRow("Reward balance", balanceLabel)
                    DetailRow(
                        "Milestones unlocked",
                        "$unlocked / ${milestonesState.milestones.size}",
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = onLibraryClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text("Library")
                }
                OutlinedButton(
                    onClick = onMilestonesClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text("Milestones")
                }
            }
            OutlinedButton(
                onClick = onManageReaders,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text("Edit reader profiles")
            }
        }
    }
}

@Composable
private fun ReaderProfileCard(reader: KidProfile) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Lavender.copy(alpha = 0.22f)),
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(SkyBlue, Lavender))),
                contentAlignment = Alignment.Center,
            ) {
                Text(reader.emoji, style = MaterialTheme.typography.headlineMedium)
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(reader.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "${reader.typeLabel} · ${reader.genderLabel}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    KidAgeCalculator.ageLabel(reader.dateOfBirth),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (reader.favoriteGenre.isNotBlank()) {
                    Text(
                        "Loves: ${reader.favoriteGenre}",
                        style = MaterialTheme.typography.labelLarge,
                        color = CoralPink,
                    )
                }
                if (reader.notes.isNotBlank()) {
                    Text(
                        reader.notes,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 3,
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniStatCard(
    icon: ImageVector,
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.28f)),
    ) {
        Column(
            Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
            Text(value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}
