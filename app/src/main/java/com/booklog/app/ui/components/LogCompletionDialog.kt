package com.booklog.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.booklog.app.data.local.CompletedBook
import com.booklog.app.data.rewards.RewardBreakdown
import com.booklog.app.data.rewards.RewardCalculator
import com.booklog.app.data.repository.RewardRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LogCompletionDialog(
    title: String,
    author: String,
    pageCount: Int,
    priorHistory: List<CompletedBook> = emptyList(),
    isSaving: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (minutes: Int) -> Unit,
) {
    var selectedMinutes by rememberSaveable { mutableIntStateOf(30) }
    var showCustom by rememberSaveable { mutableStateOf(false) }
    var customMinutes by rememberSaveable { mutableStateOf("") }

    val isReread = priorHistory.isNotEmpty()
    val minutes = if (showCustom) customMinutes.toIntOrNull() ?: 0 else selectedMinutes
    val breakdown = RewardCalculator.calculate(pageCount, minutes, isReread)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Log Reading Time", fontWeight = FontWeight.Bold)
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text("by $author", style = MaterialTheme.typography.bodySmall)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("$pageCount pages", style = MaterialTheme.typography.bodyMedium)

                if (priorHistory.isNotEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        ),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                "Read ${priorHistory.size} time${if (priorHistory.size == 1) "" else "s"} before",
                                fontWeight = FontWeight.SemiBold,
                            )
                            priorHistory.take(3).forEach { entry ->
                                Text(
                                    formatHistoryLine(entry),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            if (isReread) {
                                Text(
                                    "Re-read: page reward = \$0.00, time reward only",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                Text("How long did you read?", fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(15 to "15 min", 30 to "30 min", 45 to "45 min", 60 to "1 hr").forEach { (mins, label) ->
                        FilterChip(
                            selected = !showCustom && selectedMinutes == mins,
                            onClick = {
                                showCustom = false
                                selectedMinutes = mins
                            },
                            label = { Text(label) },
                        )
                    }
                    FilterChip(
                        selected = showCustom,
                        onClick = { showCustom = true },
                        label = { Text("Custom") },
                    )
                }

                if (showCustom) {
                    OutlinedTextField(
                        value = customMinutes,
                        onValueChange = { customMinutes = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Minutes") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                }

                RewardPreviewCard(breakdown = breakdown)
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(minutes) },
                enabled = !isSaving && minutes > 0,
            ) {
                Text(if (isSaving) "Saving…" else "Save & Earn Rewards")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun RewardPreviewCard(breakdown: RewardBreakdown) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
        ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Reward preview", fontWeight = FontWeight.SemiBold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Pages", style = MaterialTheme.typography.bodySmall)
                Text(RewardRepository.formatCents(breakdown.pageRewardCents))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Reading time", style = MaterialTheme.typography.bodySmall)
                Text(RewardRepository.formatCents(breakdown.timeRewardCents))
            }
            if (breakdown.bonusRewardCents > 0) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Bonus", style = MaterialTheme.typography.bodySmall)
                    Text(RewardRepository.formatCents(breakdown.bonusRewardCents))
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total", fontWeight = FontWeight.Bold)
                Text(RewardRepository.formatCents(breakdown.totalCents), fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun formatHistoryLine(entry: CompletedBook): String {
    val date = SimpleDateFormat("MMM d, yyyy", Locale.US).format(Date(entry.dateCompleted))
    return "#${entry.readCountNumber} · ${entry.minutesRead} min · ${RewardRepository.formatCents(entry.totalRewardCents)} · $date"
}