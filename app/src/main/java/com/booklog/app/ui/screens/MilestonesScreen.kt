package com.booklog.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.booklog.app.data.audio.AppAudioManager
import com.booklog.app.ui.components.KeyboardAwareScrollColumn
import com.booklog.app.data.audio.AppSound
import com.booklog.app.data.local.RewardTransaction
import com.booklog.app.data.milestones.Milestone
import com.booklog.app.data.milestones.MilestoneCategory
import com.booklog.app.data.repository.RewardRepository
import com.booklog.app.data.rewards.RewardPurchaseCategory
import com.booklog.app.ui.theme.CoralPink
import com.booklog.app.ui.theme.Lavender
import com.booklog.app.ui.theme.MintGreen
import com.booklog.app.ui.theme.SkyBlue
import com.booklog.app.ui.theme.SunnyYellow
import com.booklog.app.viewmodel.MilestonesViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MilestonesScreen(
    viewModel: MilestonesViewModel,
    audioManager: AppAudioManager,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.milestones) {
        audioManager.celebrateNewMilestones(state.milestones)
    }

    LaunchedEffect(state.snapshot, state.milestones) {
        viewModel.syncMilestonesToCloud(state.snapshot, state.milestones)
    }

    LaunchedEffect(state.redeemSuccess) {
        state.redeemSuccess?.let {
            audioManager.playSound(AppSound.REWARD_REDEEMED)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.statusBarsPadding(),
                title = {
                    Column {
                        Text(
                            "Milestones",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Text(
                            "${state.unlockedCount} of ${state.totalCount} unlocked",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        KeyboardAwareScrollColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ReadingRewardsCard(state.snapshot)
            RedeemRewardsCard(
                availableBalanceCents = state.snapshot.availableBalanceCents,
                totalRedeemedCents = state.snapshot.totalRedeemedCents,
                isRedeeming = state.isRedeeming,
                error = state.redeemError,
                success = state.redeemSuccess,
                onRedeem = { amount, category, note ->
                    viewModel.clearRedeemMessages()
                    viewModel.redeemReward(amount, category, note, state.snapshot.availableBalanceCents)
                },
            )
            if (state.transactions.isNotEmpty()) {
                TransactionHistorySection(state.transactions)
            }

            MilestoneCategory.entries.forEach { category ->
                val items = state.milestones.filter {
                    it.category == category && !it.isFeatured
                }
                if (items.isNotEmpty()) {
                    CategorySection(category.label, items)
                }
            }
        }
    }
}

@Composable
private fun ReadingRewardsCard(snapshot: com.booklog.app.data.milestones.ReadingSnapshot) {
    val earned = String.format(Locale.US, "$%.2f", snapshot.readingDollars)
    val available = String.format(Locale.US, "$%.2f", snapshot.availableDollars)
    val redeemed = String.format(Locale.US, "$%.2f", snapshot.totalRedeemedCents / 100.0)
    val progress = snapshot.centsTowardNextDollar / 100f

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SunnyYellow.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(6.dp),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("💰", fontSize = 36.sp)
                Column {
                    Text("Reading Rewards", fontWeight = FontWeight.Bold, fontSize = 22.sp)
                    Text("1¢/page + 2¢/min · re-reads earn time only", style = MaterialTheme.typography.bodySmall)
                }
            }
            Text(available, fontSize = 40.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text("available to spend", style = MaterialTheme.typography.labelLarge)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total earned: $earned", style = MaterialTheme.typography.bodySmall)
                Text("Spent: $redeemed", style = MaterialTheme.typography.bodySmall)
            }
            Text(
                when {
                    snapshot.earnedCents == 0 -> "Log a book completion to earn your first reward!"
                    snapshot.totalMinutesRead > 0 ->
                        "${snapshot.totalMinutesRead} min read · ${snapshot.totalPages} pages · ${snapshot.centsTowardNextDollar}¢ toward next dollar"
                    else ->
                        "${snapshot.totalPages} pages read · ${snapshot.centsTowardNextDollar}¢ toward next dollar"
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(50)),
                color = MintGreen,
                trackColor = Lavender.copy(alpha = 0.3f),
            )
            Text(
                "${snapshot.centsTowardNextDollar}¢ toward your next \$1",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RedeemRewardsCard(
    availableBalanceCents: Int,
    totalRedeemedCents: Int,
    isRedeeming: Boolean,
    error: String?,
    success: String?,
    onRedeem: (String, RewardPurchaseCategory, String) -> Unit,
) {
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var categoryExpanded by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf(RewardPurchaseCategory.TREAT) }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MintGreen.copy(alpha = 0.2f)),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("🎁 Redeem Reward Money", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(
                "When a parent buys something with earned rewards, log it here. The amount is deducted from the balance.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = it },
            ) {
                OutlinedTextField(
                    value = "${selectedCategory.emoji} ${selectedCategory.label}",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("What did they get?") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                )
                DropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false },
                ) {
                    RewardPurchaseCategory.entries.forEach { category ->
                        DropdownMenuItem(
                            text = { Text("${category.emoji} ${category.label}") },
                            onClick = {
                                selectedCategory = category
                                categoryExpanded = false
                            },
                        )
                    }
                }
            }

            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it.filter { ch -> ch.isDigit() || ch == '.' } },
                label = { Text("Amount (\$)") },
                placeholder = { Text("e.g. 2.50") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                prefix = { Text("$") },
            )

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Details (optional)") },
                placeholder = { Text("e.g. chocolate ice cream cone") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            success?.let {
                Text(it, color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick = { onRedeem(amount, selectedCategory, note) },
                enabled = !isRedeeming && availableBalanceCents > 0 && amount.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text(if (isRedeeming) "Recording..." else "Record Redemption")
            }

            if (totalRedeemedCents > 0) {
                Text(
                    "Lifetime spent: ${RewardRepository.formatCents(totalRedeemedCents)}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun TransactionHistorySection(transactions: List<RewardTransaction>) {
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.getDefault()) }

    Text("Reward Transactions", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    transactions.forEach { transaction ->
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(2.dp),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        RewardRepository.categoryLabel(transaction.category),
                        fontWeight = FontWeight.Bold,
                    )
                    if (transaction.note.isNotBlank()) {
                        Text(transaction.note, style = MaterialTheme.typography.bodySmall)
                    }
                    Text(
                        dateFormat.format(Date(transaction.createdAt)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    "-${RewardRepository.formatCents(transaction.amountCents)}",
                    fontWeight = FontWeight.Bold,
                    color = CoralPink,
                    fontSize = 18.sp,
                )
            }
        }
    }
}

@Composable
private fun CategorySection(title: String, milestones: List<Milestone>) {
    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    milestones.forEach { milestone ->
        MilestoneCard(milestone)
    }
}

@Composable
private fun MilestoneCard(milestone: Milestone) {
    val containerColor = if (milestone.isUnlocked) MintGreen.copy(alpha = 0.25f)
    else MaterialTheme.colorScheme.surface

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(if (milestone.isUnlocked) 4.dp else 2.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(milestone.emoji, fontSize = 28.sp)
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(milestone.title, fontWeight = FontWeight.Bold)
                        if (milestone.isUnlocked) {
                            Text("✓", color = MintGreen, fontWeight = FontWeight.Bold)
                        }
                    }
                    Text(
                        milestone.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (!milestone.isUnlocked && milestone.target > 1) {
                LinearProgressIndicator(
                    progress = { milestone.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(50)),
                    color = SkyBlue,
                    trackColor = CoralPink.copy(alpha = 0.2f),
                )
            }
            Text(
                milestone.progressLabel,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}