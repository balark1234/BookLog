package com.brk.booklogger.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.brk.booklogger.data.local.KidProfile
import com.brk.booklogger.ui.theme.Lavender
import com.brk.booklogger.ui.theme.SkyBlue

/**
 * Compact reader picker: chip + dropdown, designed for the top-right of the app bar.
 */
@Composable
fun ActiveReaderSelector(
    kids: List<KidProfile>,
    activeKidId: Long?,
    onSelectKid: (Long?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val active = kids.find { it.id == activeKidId }
    val label = active?.let { "${it.emoji} ${it.firstName}" } ?: "Reader"

    Box(modifier = modifier) {
        FilterChip(
            selected = true,
            onClick = { expanded = true },
            leadingIcon = {
                Icon(
                    imageVector = if (active?.isAdult == true) Icons.Default.Person else Icons.Default.Face,
                    contentDescription = "Select reader",
                )
            },
            trailingIcon = {
                Icon(Icons.Default.ExpandMore, contentDescription = null)
            },
            label = {
                Text(
                    label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.widthIn(max = 120.dp),
                )
            },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = if (active?.isAdult == true) {
                    Lavender.copy(alpha = 0.45f)
                } else {
                    SkyBlue.copy(alpha = 0.45f)
                },
            ),
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            if (kids.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("No readers yet") },
                    onClick = { expanded = false },
                    enabled = false,
                )
            } else {
                kids.forEach { kid ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                "${kid.emoji} ${kid.name} · ${kid.typeLabel}",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        },
                        onClick = {
                            onSelectKid(kid.id)
                            expanded = false
                        },
                        leadingIcon = {
                            Icon(
                                if (kid.isAdult) Icons.Default.Person else Icons.Default.Face,
                                contentDescription = null,
                            )
                        },
                    )
                }
            }
        }
    }
}
