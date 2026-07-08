package com.booklog.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.booklog.app.data.cloud.LeaderboardEntry
import com.booklog.app.data.cloud.LeaderboardType
import com.booklog.app.data.leaderboard.LocalLeaderboardEntry
import com.booklog.app.data.repository.RewardRepository
import com.booklog.app.ui.theme.CoralPink
import com.booklog.app.ui.theme.Lavender
import com.booklog.app.ui.theme.SkyBlue
import com.booklog.app.ui.theme.SunnyYellow
import com.booklog.app.viewmodel.LeaderboardScope
import com.booklog.app.viewmodel.LeaderboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    viewModel: LeaderboardViewModel,
    isSignedIn: Boolean,
    currentUserId: String?,
    onSignInClick: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Column {
                    Text("Reading Champions", fontWeight = FontWeight.Bold)
                    Text(
                        if (state.scope == LeaderboardScope.LOCAL) {
                            "Family rankings by reading time"
                        } else {
                            state.type.subtitle
                        },
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            },
            actions = {
                IconButton(onClick = viewModel::refresh) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                }
            },
        )

        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            SegmentedButton(
                selected = state.scope == LeaderboardScope.LOCAL,
                onClick = { viewModel.selectScope(LeaderboardScope.LOCAL) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            ) {
                Text("Family")
            }
            SegmentedButton(
                selected = state.scope == LeaderboardScope.GLOBAL,
                onClick = { viewModel.selectScope(LeaderboardScope.GLOBAL) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
            ) {
                Text("Global")
            }
        }

        if (state.scope == LeaderboardScope.GLOBAL) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                LeaderboardType.entries.forEach { type ->
                    FilterChip(
                        selected = state.type == type,
                        onClick = { viewModel.selectType(type) },
                        label = { Text(type.label) },
                    )
                }
            }
        }

        if (state.scope == LeaderboardScope.GLOBAL &&
            !isSignedIn &&
            state.type in setOf(LeaderboardType.READERS, LeaderboardType.MILESTONES)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Lavender.copy(alpha = 0.3f)),
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = SunnyYellow, modifier = Modifier.size(40.dp))
                    Text("Join the competition!", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Create an account so you and your kids appear on reader and milestone leaderboards.")
                    OutlinedButton(onClick = onSignInClick) {
                        Text("Create Account")
                    }
                }
            }
        }

        when {
            state.isLoading -> CircularProgressIndicator(
                Modifier.align(Alignment.CenterHorizontally).padding(32.dp),
            )
            state.scope == LeaderboardScope.LOCAL -> {
                if (state.localEntries.isEmpty()) {
                    Column(
                        Modifier.fillMaxSize().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text("👨‍👩‍👧‍👦", fontSize = 48.sp)
                        Text("No family readers yet!", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                        Text(
                            "Add kid profiles and log book completions to see who reads the most!",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(state.localEntries, key = { it.kidId }) { entry ->
                            LocalLeaderboardRow(entry = entry)
                        }
                    }
                }
            }
            state.entries.isEmpty() -> Column(
                Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("🏆", fontSize = 48.sp)
                Text("No champions yet!", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                Text(
                    emptyMessage(state.type),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            else -> LazyColumn(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.entries, key = { it.id }) { entry ->
                    LeaderboardRow(
                        entry = entry,
                        type = state.type,
                        isCurrentUser = entry.id == currentUserId,
                    )
                }
            }
        }
    }
}

@Composable
private fun LocalLeaderboardRow(entry: LocalLeaderboardEntry) {
    val medal = when (entry.rank) {
        1 -> "🥇"
        2 -> "🥈"
        3 -> "🥉"
        else -> "#${entry.rank}"
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(medal, fontSize = 22.sp, modifier = Modifier.size(40.dp))
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(SkyBlue, CoralPink))),
                contentAlignment = Alignment.Center,
            ) {
                Text(entry.emoji, fontWeight = FontWeight.Bold)
            }
            Column(Modifier.weight(1f)) {
                Text(entry.displayName, fontWeight = FontWeight.Bold)
                Text(
                    "${entry.minutesRead} min · ${entry.pagesRead} pages · ${entry.booksCompleted} books",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "Streak: ${entry.currentStreak} day${if (entry.currentStreak == 1) "" else "s"} · ${RewardRepository.formatCents(entry.rewardCents)} earned",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun emptyMessage(type: LeaderboardType): String = when (type) {
    LeaderboardType.READERS -> "Be the first to finish a book and claim the top spot!"
    LeaderboardType.KIDS -> "Add kid profiles in your account, then finish books to rank them here!"
    LeaderboardType.AUTHORS -> "Finish books to see which authors are most popular globally!"
    LeaderboardType.PUBLISHERS -> "Finish books to rank top publishers!"
    LeaderboardType.GENRES -> "Finish books to see which genres everyone loves!"
    LeaderboardType.MILESTONES -> "Unlock milestones to climb the badge leaderboard!"
}

@Composable
private fun LeaderboardRow(
    entry: LeaderboardEntry,
    type: LeaderboardType,
    isCurrentUser: Boolean,
) {
    val medal = when (entry.rank) {
        1 -> "🥇"
        2 -> "🥈"
        3 -> "🥉"
        else -> "#${entry.rank}"
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentUser) SunnyYellow.copy(alpha = 0.35f)
            else MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(if (isCurrentUser) 6.dp else 2.dp),
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(medal, fontSize = 22.sp, modifier = Modifier.size(40.dp))
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(SkyBlue, CoralPink))),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    entry.emoji ?: entry.displayName.firstOrNull()?.uppercase() ?: "?",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                )
            }
            Column(Modifier.weight(1f)) {
                Text(entry.displayName, fontWeight = FontWeight.Bold)
                Text(
                    statLine(type, entry),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun statLine(type: LeaderboardType, entry: LeaderboardEntry): String = when (type) {
    LeaderboardType.MILESTONES -> "${entry.primaryValue} badges unlocked"
    LeaderboardType.AUTHORS, LeaderboardType.PUBLISHERS, LeaderboardType.GENRES ->
        "${entry.primaryValue} books finished · ${entry.secondaryValue} pages"
    else -> "${entry.primaryValue} books · ${entry.secondaryValue} pages"
}