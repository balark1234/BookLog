package com.booklog.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.booklog.app.ui.components.BookCover
import com.booklog.app.ui.components.LogCompletionDialog
import com.booklog.app.viewmodel.LogCompletionEvent
import com.booklog.app.viewmodel.LogCompletionViewModel
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogCompletionScreen(
    viewModel: LogCompletionViewModel,
    activeReaderLabel: String,
    onBack: () -> Unit,
    onSuccess: (title: String, rewardCents: Int) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is LogCompletionEvent.Success -> onSuccess(event.title, event.rewardCents)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Log Completion", fontWeight = FontWeight.Bold)
                        Text(
                            "For $activeReaderLabel",
                            style = MaterialTheme.typography.bodySmall,
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
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            BookCover(
                title = state.book.title,
                coverUrl = state.book.coverUrl,
                isbn = state.book.isbn,
                modifier = Modifier.size(160.dp, 240.dp),
            )
            if (state.isSaving) {
                CircularProgressIndicator()
            }
            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }
        }
    }

    LogCompletionDialog(
        title = state.book.title,
        author = state.book.author,
        pageCount = state.book.pageCount ?: 0,
        priorHistory = state.priorHistory,
        isSaving = state.isSaving,
        onDismiss = onBack,
        onSave = viewModel::logCompletion,
    )
}