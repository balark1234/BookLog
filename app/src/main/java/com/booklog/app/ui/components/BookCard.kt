package com.booklog.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.booklog.app.data.local.Book
import com.booklog.app.data.local.ReadingStatus
import com.booklog.app.ui.theme.CoralPink
import com.booklog.app.ui.theme.Lavender
import com.booklog.app.ui.theme.MintGreen
import com.booklog.app.ui.theme.SkyBlue
import com.booklog.app.ui.theme.SunnyYellow

@Composable
fun BookCard(
    book: Book,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BookCover(
                title = book.title,
                coverUrl = book.coverUrl,
                isbn = book.isbn,
                modifier = Modifier.size(72.dp, 108.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = book.author,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                StatusBadge(status = book.status, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}

@Composable
fun StatusBadge(status: ReadingStatus, modifier: Modifier = Modifier) {
    val (label, colors) = when (status) {
        ReadingStatus.WANT_TO_READ -> "Want to Read" to listOf(SkyBlue.copy(alpha = 0.3f), Lavender.copy(alpha = 0.2f))
        ReadingStatus.READING -> "Reading" to listOf(SunnyYellow.copy(alpha = 0.5f), CoralPink.copy(alpha = 0.2f))
        ReadingStatus.FINISHED -> "Finished" to listOf(MintGreen.copy(alpha = 0.4f), MintGreen.copy(alpha = 0.2f))
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(Brush.horizontalGradient(colors))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    }
}