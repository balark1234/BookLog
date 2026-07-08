package com.booklog.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import com.booklog.app.ui.components.FocusScrollOutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.booklog.app.data.local.KidGender
import com.booklog.app.data.local.KidProfile
import com.booklog.app.data.profiles.KidAgeCalculator
import com.booklog.app.data.repository.KidProfileRepository
import com.booklog.app.ui.components.BirthDatePickerDialog
import com.booklog.app.ui.components.KeyboardAwareScrollColumn
import com.booklog.app.ui.theme.Lavender
import com.booklog.app.viewmodel.KidsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KidProfileDetailScreen(
    kidId: Long,
    viewModel: KidsViewModel,
    repository: KidProfileRepository,
    onBack: () -> Unit,
    onDeleted: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isNew = kidId == 0L

    var name by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf("📚") }
    var gender by remember { mutableStateOf(KidGender.PREFER_NOT_TO_SAY) }
    var dateOfBirth by remember { mutableStateOf<Long?>(null) }
    var favoriteGenre by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var loaded by remember { mutableStateOf(isNew) }
    var showDatePicker by remember { mutableStateOf(false) }
    var genderExpanded by remember { mutableStateOf(false) }
    var existingProfile by remember { mutableStateOf<KidProfile?>(null) }

    LaunchedEffect(kidId) {
        if (!isNew) {
            val profile = withContext(Dispatchers.IO) { repository.getById(kidId) }
            if (profile != null) {
                existingProfile = profile
                name = profile.name
                emoji = profile.emoji
                gender = KidGender.entries.find { it.name == profile.gender } ?: KidGender.PREFER_NOT_TO_SAY
                dateOfBirth = profile.dateOfBirth?.let(KidAgeCalculator::normalizeStoredMillis)
                favoriteGenre = profile.favoriteGenre
                notes = profile.notes
            }
            loaded = true
        }
    }

    if (showDatePicker) {
        BirthDatePickerDialog(
            initialMillis = dateOfBirth,
            onDismiss = { showDatePicker = false },
            onConfirm = { selected ->
                dateOfBirth = selected
                showDatePicker = false
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.statusBarsPadding(),
                title = {
                    Text(
                        if (isNew) "Add Kid Profile" else "Edit Profile",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!isNew && existingProfile != null) {
                        IconButton(onClick = {
                            existingProfile?.let {
                                viewModel.removeKid(it)
                                onDeleted()
                            }
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (!loaded) return@Scaffold

        KeyboardAwareScrollColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (dateOfBirth != null) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Lavender.copy(alpha = 0.25f)),
                ) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("$emoji $name".trim().ifBlank { "Reader" }, fontWeight = FontWeight.Bold)
                        Text(
                            KidAgeCalculator.ageLabel(dateOfBirth),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "Born ${KidAgeCalculator.birthdateLabel(dateOfBirth)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }

            FocusScrollOutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Full name *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            FocusScrollOutlinedTextField(
                value = emoji,
                onValueChange = { if (it.length <= 2) emoji = it },
                label = { Text("Emoji avatar") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            ExposedDropdownMenuBox(
                expanded = genderExpanded,
                onExpandedChange = { genderExpanded = it },
            ) {
                OutlinedTextField(
                    value = gender.label,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Gender") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = genderExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                )
                DropdownMenu(
                    expanded = genderExpanded,
                    onDismissRequest = { genderExpanded = false },
                ) {
                    KidGender.entries.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.label) },
                            onClick = {
                                gender = option
                                genderExpanded = false
                            },
                        )
                    }
                }
            }

            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text(
                    if (dateOfBirth == null) "Set date of birth" else "Born: ${KidAgeCalculator.birthdateLabel(dateOfBirth)}",
                )
            }

            FocusScrollOutlinedTextField(
                value = favoriteGenre,
                onValueChange = { favoriteGenre = it },
                label = { Text("Favorite genre") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            FocusScrollOutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
            )

            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            Button(
                onClick = {
                    viewModel.saveProfile(
                        KidProfile(
                            id = kidId,
                            name = name,
                            emoji = emoji.ifBlank { "📚" },
                            gender = gender.name,
                            dateOfBirth = dateOfBirth,
                            favoriteGenre = favoriteGenre.trim(),
                            notes = notes.trim(),
                            createdAt = existingProfile?.createdAt ?: System.currentTimeMillis(),
                        ),
                        onSuccess = onBack,
                    )
                },
                enabled = name.isNotBlank() && !state.isLoading,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(if (isNew) "Create Profile" else "Save Profile")
            }
        }
    }
}