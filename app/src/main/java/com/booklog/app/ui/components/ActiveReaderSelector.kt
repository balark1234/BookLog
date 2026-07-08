package com.booklog.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.booklog.app.data.local.KidProfile
import com.booklog.app.ui.theme.Lavender
import com.booklog.app.ui.theme.SkyBlue

@Composable
fun ActiveReaderSelector(
    kids: List<KidProfile>,
    activeKidId: Long?,
    onSelectKid: (Long?) -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Who is reading?",
    subtitle: String? = null,
    compact: Boolean = false,
) {
    val activeLabel = when {
        activeKidId == null -> "Parent"
        else -> kids.find { it.id == activeKidId }?.let { "${it.emoji} ${it.firstName}" } ?: "Reader"
    }

    val chipRow: @Composable () -> Unit = {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            kids.forEach { kid ->
                FilterChip(
                    selected = activeKidId == kid.id,
                    onClick = { onSelectKid(kid.id) },
                    label = {
                        Text(
                            "${kid.emoji} ${kid.firstName}",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SkyBlue.copy(alpha = 0.4f),
                    ),
                )
            }
            FilterChip(
                selected = activeKidId == null,
                onClick = { onSelectKid(null) },
                label = { Text("👤 Parent") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Lavender.copy(alpha = 0.35f),
                ),
            )
        }
    }

    if (compact) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            chipRow()
        }
    } else {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = SkyBlue.copy(alpha = 0.12f)),
        ) {
            Column(
                Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        subtitle ?: "Selected: $activeLabel — books, streaks, and rewards follow this reader.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                chipRow()
            }
        }
    }
}