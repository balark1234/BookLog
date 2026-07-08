package com.booklog.app.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.booklog.app.ui.components.BookCover
import com.booklog.app.ui.components.FocusScrollOutlinedTextField
import com.booklog.app.ui.components.KeyboardAwareScrollColumn
import com.booklog.app.ui.theme.MintGreen
import com.booklog.app.viewmodel.AddBookEvent
import com.booklog.app.viewmodel.AddBookViewModel
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBookScreen(
    viewModel: AddBookViewModel,
    activeReaderLabel: String = "Parent",
    onBack: () -> Unit,
    onSaved: (com.booklog.app.data.local.Book) -> Unit,
    scannedIsbn: String? = null,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(scannedIsbn) {
        if (!scannedIsbn.isNullOrBlank()) {
            viewModel.applyScannedIsbn(scannedIsbn)
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is AddBookEvent.BookSaved -> onSaved(event.book)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Add Manually", fontWeight = FontWeight.Bold)
                        Text(
                            "Search by ISBN or title for $activeReaderLabel",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        KeyboardAwareScrollColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Type an ISBN or search by title & author. Use the Scan button on the Books screen for barcode scanning.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (state.saveSuccess) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MintGreen.copy(alpha = 0.25f)),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Row(
                        Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MintGreen)
                        Text("Saved! Taking you to your library…", fontWeight = FontWeight.Bold)
                    }
                }
            }

            FocusScrollOutlinedTextField(
                value = state.isbn,
                onValueChange = viewModel::updateIsbn,
                label = { Text("ISBN") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = viewModel::lookupIsbn,
                    enabled = !state.isLoading && state.isbn.isNotBlank(),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(Icons.Default.Search, contentDescription = null)
                    Text("ISBN", modifier = Modifier.padding(start = 6.dp))
                }
                OutlinedButton(
                    onClick = viewModel::searchByTitleAndAuthor,
                    enabled = !state.isLoading && (state.title.isNotBlank() || state.author.isNotBlank()),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(Icons.Default.Search, contentDescription = null)
                    Text("Title & Author", modifier = Modifier.padding(start = 6.dp))
                }
            }

            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }

            state.info?.let {
                Text(it, color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.bodyMedium)
            }
            state.error?.let {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(12.dp))
                }
            }

            BookCover(
                title = state.title.ifBlank { "Book cover" },
                coverUrl = state.coverUrl,
                isbn = state.isbn,
                modifier = Modifier
                    .size(140.dp, 210.dp)
                    .align(Alignment.CenterHorizontally),
            )

            FocusScrollOutlinedTextField(
                value = state.title,
                onValueChange = viewModel::updateTitle,
                label = { Text("Title *") },
                modifier = Modifier.fillMaxWidth(),
            )
            FocusScrollOutlinedTextField(
                value = state.author,
                onValueChange = viewModel::updateAuthor,
                label = { Text("Author *") },
                modifier = Modifier.fillMaxWidth(),
            )

            state.searchResults.forEach { result ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.applySearchResult(result) },
                    shape = RoundedCornerShape(14.dp),
                    elevation = CardDefaults.cardElevation(2.dp),
                ) {
                    Row(
                        Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        BookCover(
                            title = result.title,
                            coverUrl = result.coverUrl,
                            isbn = result.isbn,
                            modifier = Modifier.size(48.dp, 72.dp),
                        )
                        Column(Modifier.weight(1f)) {
                            Text(result.title, fontWeight = FontWeight.Bold)
                            Text(result.author, style = MaterialTheme.typography.bodySmall)
                            result.publishedYear?.let {
                                Text(it, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
            FocusScrollOutlinedTextField(
                value = state.pageCount,
                onValueChange = viewModel::updatePageCount,
                label = { Text("Page count") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            FocusScrollOutlinedTextField(
                value = state.publishedYear,
                onValueChange = viewModel::updatePublishedYear,
                label = { Text("Published year") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            Button(
                onClick = viewModel::saveBook,
                enabled = !state.isLoading && !state.saveSuccess,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text("Save for $activeReaderLabel")
            }
        }
    }
}