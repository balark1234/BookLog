package com.booklog.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.booklog.app.data.local.KidProfile
import com.booklog.app.data.profiles.KidAgeCalculator
import com.booklog.app.ui.theme.MintGreen
import com.booklog.app.ui.theme.SkyBlue
import com.booklog.app.viewmodel.KidsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KidProfilesScreen(
    viewModel: KidsViewModel,
    onBack: () -> Unit,
    onAddKid: () -> Unit,
    onEditKid: (Long) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.statusBarsPadding(),
                title = {
                    Column {
                        Text(
                            "Kid Profiles",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Text(
                            "Who's reading today?",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
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
        floatingActionButton = {
            FloatingActionButton(onClick = onAddKid, containerColor = SkyBlue) {
                Icon(Icons.Default.Add, contentDescription = "Add kid")
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "Active reader",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                state.kids.forEach { kid ->
                    FilterChip(
                        selected = state.activeKidId == kid.id,
                        onClick = { viewModel.selectKid(kid.id) },
                        label = {
                            Text(
                                "${kid.emoji} ${kid.firstName}",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                    )
                }
                FilterChip(
                    selected = state.activeKidId == null,
                    onClick = { viewModel.selectKid(null) },
                    label = { Text("Parent") },
                )
            }
            Text(
                "Books you add will be tracked for the selected reader on milestones and leaderboards.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }
            state.successMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.secondary)
            }

            Text(
                "All profiles",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            if (state.kids.isEmpty()) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MintGreen.copy(alpha = 0.2f)),
                ) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("No kid profiles yet", fontWeight = FontWeight.Bold)
                        Text(
                            "Tap + to add name, birthday, gender, and more.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            } else {
                state.kids.forEach { kid ->
                    KidProfileListCard(kid = kid, onClick = { onEditKid(kid.id) })
                }
            }
        }
    }
}

@Composable
private fun KidProfileListCard(kid: KidProfile, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(3.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("${kid.emoji} ${kid.name}", fontWeight = FontWeight.Bold)
                Text(
                    "${kid.genderLabel} · ${KidAgeCalculator.ageLabel(kid.dateOfBirth)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (kid.favoriteGenre.isNotBlank()) {
                    Text("Loves: ${kid.favoriteGenre}", style = MaterialTheme.typography.labelSmall)
                }
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null)
        }
    }
}