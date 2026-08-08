package com.brk.booklogger.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brk.booklogger.data.local.ReadingStatus
import com.brk.booklogger.data.streak.ReadingStreakInfo
import com.brk.booklogger.ui.components.BookCard
import com.brk.booklogger.ui.components.BookLogActionButtons
import com.brk.booklogger.ui.theme.CoralPink
import com.brk.booklogger.ui.theme.Lavender
import com.brk.booklogger.ui.theme.SkyBlue
import com.brk.booklogger.ui.theme.SunnyYellow
import com.brk.booklogger.viewmodel.LibraryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    activeReaderLabel: String = "Parent",
    streakInfo: ReadingStreakInfo = ReadingStreakInfo(0, false, 0, 0),
    onBookClick: (com.brk.booklogger.data.local.Book) -> Unit,
    onManualAddClick: () -> Unit,
    onScanClick: () -> Unit,
    onMilestonesClick: () -> Unit,
) {
    val books by viewModel.books.collectAsState()
    var selectedFilter by remember { mutableStateOf<ReadingStatus?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                ),
                title = {
                    Column {
                        Text(
                            "ðŸ“š BookLog",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Text(
                            "${books.size} books Â· $activeReaderLabel",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onMilestonesClick) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = "Milestones")
                    }
                },
            )
        },
        floatingActionButton = {
            BookLogActionButtons(
                onScanClick = onScanClick,
                onManualAddClick = onManualAddClick,
            )
        },
        floatingActionButtonPosition = FabPosition.Center,
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            DailyStreakCard(streakInfo = streakInfo)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = selectedFilter == null,
                    onClick = {
                        selectedFilter = null
                        viewModel.setFilter(null)
                    },
                    label = { Text("All") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SkyBlue.copy(alpha = 0.35f),
                    ),
                )
                ReadingStatus.entries.forEach { status ->
                    FilterChip(
                        selected = selectedFilter == status,
                        onClick = {
                            selectedFilter = status
                            viewModel.setFilter(status)
                        },
                        label = { Text(status.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Lavender.copy(alpha = 0.4f),
                        ),
                    )
                }
            }

            if (books.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text("ðŸŒŸ", fontSize = 56.sp)
                    Text(
                        "Your bookshelf is waiting!",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                    Text(
                        "No books for $activeReaderLabel yet. Tap Scan to add by barcode, or + to search manually.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    items(books, key = { it.id }) { book ->
                        BookCard(book = book, onClick = { onBookClick(book) })
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyStreakCard(streakInfo: ReadingStreakInfo) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (streakInfo.readToday) {
                SunnyYellow.copy(alpha = 0.35f)
            } else {
                CoralPink.copy(alpha = 0.15f)
            },
        ),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    if (streakInfo.currentStreak > 0) "ðŸ”¥ ${streakInfo.currentStreak}-day streak" else "ðŸ“– Daily reading streak",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    if (streakInfo.readToday) "Today âœ“" else "Log a page!",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                when {
                    streakInfo.readToday && streakInfo.pagesToday > 0 ->
                        "${streakInfo.pagesToday} page${if (streakInfo.pagesToday == 1) "" else "s"} logged today â€” keep it going!"
                    streakInfo.readToday ->
                        "You read today! Log pages in any book to grow your streak."
                    streakInfo.consecutiveMissedDays in 1..5 ->
                        "We miss you! Read today to restart your streak (${streakInfo.consecutiveMissedDays} day${if (streakInfo.consecutiveMissedDays == 1) "" else "s"} away)."
                    else ->
                        "Read a little every day to earn rewards and build a super reading habit."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}