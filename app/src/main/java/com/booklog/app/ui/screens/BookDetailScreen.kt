package com.booklog.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.booklog.app.data.local.ReadingStatus
import com.booklog.app.ui.components.BookCover
import com.booklog.app.ui.components.KeyboardAwareScrollColumn
import com.booklog.app.ui.components.StatusBadge
import com.booklog.app.viewmodel.BookDetailEvent
import com.booklog.app.viewmodel.BookDetailViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailScreen(
    viewModel: BookDetailViewModel,
    onBack: () -> Unit,
    onSaved: (String) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is BookDetailEvent.BookSaved -> {
                    onSaved(event.title)
                    onBack()
                }
            }
        }
    }
    val book = uiState.book ?: return
    var notes by remember(book.id) { mutableStateOf(book.notes) }
    var currentPage by remember(book.id) { mutableStateOf(book.currentPage?.toString() ?: "") }
    var rating by remember(book.id) { mutableFloatStateOf(book.rating ?: 0f) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete book?") },
            text = { Text("This will permanently remove \"${book.title}\" from your library.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.deleteBook(onBack)
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Book Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
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
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                BookCover(
                    title = book.title,
                    coverUrl = book.coverUrl,
                    isbn = book.isbn,
                    modifier = Modifier.size(130.dp, 195.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(book.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(book.author, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    StatusBadge(status = book.status, modifier = Modifier.padding(vertical = 6.dp))
                    book.isbn?.let { Text("ISBN: $it", style = MaterialTheme.typography.bodySmall) }
                    book.publishedYear?.let { Text("Published: $it", style = MaterialTheme.typography.bodySmall) }
                    book.pageCount?.let { Text("$it pages", style = MaterialTheme.typography.bodySmall) }
                }
            }

            Text("Reading status", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ReadingStatus.entries.forEach { status ->
                    FilterChip(
                        selected = book.status == status,
                        onClick = { viewModel.updateStatus(status) },
                        label = { Text(status.label) },
                    )
                }
            }

            Text("Rating", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                (1..5).forEach { star ->
                    IconButton(onClick = { rating = star.toFloat() }) {
                        Icon(
                            imageVector = if (star <= rating) Icons.Filled.Star else Icons.Outlined.Star,
                            contentDescription = "$star stars",
                            tint = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                }
            }

            OutlinedTextField(
                value = currentPage,
                onValueChange = { currentPage = it.filter { ch -> ch.isDigit() } },
                label = { Text("Current page") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
            )

            book.dateStarted?.let {
                Text("Started: ${dateFormat.format(Date(it))}", style = MaterialTheme.typography.bodySmall)
            }
            book.dateFinished?.let {
                Text("Finished: ${dateFormat.format(Date(it))}", style = MaterialTheme.typography.bodySmall)
            }

            uiState.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            Button(
                onClick = {
                    viewModel.saveChanges(
                        book.copy(
                            notes = notes,
                            rating = rating.takeIf { it > 0f },
                            currentPage = currentPage.toIntOrNull(),
                        )
                    )
                },
                enabled = !uiState.isSaving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (uiState.isSaving) "Saving..." else "Save changes")
            }
        }
    }
}