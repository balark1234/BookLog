package com.booklog.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.booklog.app.data.local.CompletedBook
import com.booklog.app.data.repository.RewardRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ScanHistoryDialog(
    title: String,
    author: String,
    history: List<CompletedBook>,
    onLogAnotherRead: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("You've read this before!", fontWeight = FontWeight.Bold)
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text("by $author", style = MaterialTheme.typography.bodySmall)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "${history.size} previous completion${if (history.size == 1) "" else "s"}:",
                    style = MaterialTheme.typography.bodyMedium,
                )
                history.forEach { entry ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        ),
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                "Read #${entry.readCountNumber}",
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                "${entry.minutesRead} min · ${entry.pageCount} pages · ${RewardRepository.formatCents(entry.totalRewardCents)}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Text(
                                SimpleDateFormat("MMM d, yyyy", Locale.US).format(Date(entry.dateCompleted)),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onLogAnotherRead) {
                Text("Log Another Read")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}