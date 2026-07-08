package com.booklog.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.booklog.app.data.remote.CoverUrlResolver
import com.booklog.app.ui.theme.CoralPink
import com.booklog.app.ui.theme.SkyBlue
import com.booklog.app.ui.theme.SunnyYellow

@Composable
fun BookCover(
    title: String,
    coverUrl: String?,
    isbn: String? = null,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val resolvedUrl = CoverUrlResolver.bestAvailable(isbn, coverUrl)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(SkyBlue.copy(alpha = 0.5f), CoralPink.copy(alpha = 0.5f)),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (resolvedUrl != null) {
            SubcomposeAsyncImage(
                model = resolvedUrl,
                contentDescription = title,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale,
                loading = { CoverPlaceholder(title) },
                error = { CoverPlaceholder(title) },
            )
        } else {
            CoverPlaceholder(title)
        }
    }
}

@Composable
private fun CoverPlaceholder(title: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(SkyBlue, SunnyYellow.copy(alpha = 0.8f)),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.MenuBook,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.9f),
            modifier = Modifier.padding(bottom = 24.dp),
        )
        Text(
            text = title.take(30),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(8.dp),
        )
    }
}