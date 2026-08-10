package com.brk.booklogger.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.brk.booklogger.data.local.KidProfile

/**
 * Top app bar: hamburger/settings (left) + title + reader dropdown (right).
 * When [settingsOpen] is true, the hamburger morphs into a close (X) button.
 */
@Composable
fun AppTopBar(
    settingsOpen: Boolean,
    onToggleSettings: () -> Unit,
    kids: List<KidProfile>,
    activeKidId: Long?,
    onSelectKid: (Long?) -> Unit,
    title: String = "BookLog",
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = onToggleSettings) {
                AnimatedContent(
                    targetState = settingsOpen,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "menu_close",
                ) { open ->
                    if (open) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close settings",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    } else {
                        Icon(
                            Icons.Default.Menu,
                            contentDescription = "Open settings",
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }

            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )

            ActiveReaderSelector(
                kids = kids,
                activeKidId = activeKidId,
                onSelectKid = onSelectKid,
                modifier = Modifier.padding(end = 8.dp),
            )
        }
    }
}
