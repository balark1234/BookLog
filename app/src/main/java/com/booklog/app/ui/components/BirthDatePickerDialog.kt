package com.booklog.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.booklog.app.data.profiles.KidAgeCalculator
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BirthDatePickerDialog(
    initialMillis: Long?,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
) {
    val today = remember { Calendar.getInstance() }
    val maxYear = today.get(Calendar.YEAR)
    val minYear = maxYear - 17

    val initialParts = remember(initialMillis) {
        if (initialMillis != null) {
            KidAgeCalculator.millisToLocalParts(initialMillis)
        } else {
            Triple(maxYear - 8, Calendar.JANUARY, 1)
        }
    }

    var year by remember(initialMillis) { mutableIntStateOf(initialParts.first) }
    var month by remember(initialMillis) { mutableIntStateOf(initialParts.second) }
    var day by remember(initialMillis) { mutableIntStateOf(initialParts.third) }

    var yearExpanded by remember { mutableStateOf(false) }
    var monthExpanded by remember { mutableStateOf(false) }
    var dayExpanded by remember { mutableStateOf(false) }

    val maxDay = KidAgeCalculator.daysInMonth(year, month)
    LaunchedEffect(year, month) {
        if (day > maxDay) day = maxDay
    }

    val years = remember(minYear, maxYear) { (minYear..maxYear).toList().reversed() }
    val days = remember(maxDay) { (1..maxDay).toList() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Date of birth") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DateDropdown(
                    label = "Month",
                    value = KidAgeCalculator.monthNames[month],
                    expanded = monthExpanded,
                    onExpandedChange = { monthExpanded = it },
                ) {
                    KidAgeCalculator.monthNames.forEachIndexed { index, name ->
                        DropdownMenuItem(
                            text = { Text(name) },
                            onClick = {
                                month = index
                                monthExpanded = false
                            },
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    DateDropdown(
                        label = "Day",
                        value = day.toString(),
                        expanded = dayExpanded,
                        onExpandedChange = { dayExpanded = it },
                        modifier = Modifier.weight(1f),
                    ) {
                        days.forEach { d ->
                            DropdownMenuItem(
                                text = { Text(d.toString()) },
                                onClick = {
                                    day = d
                                    dayExpanded = false
                                },
                            )
                        }
                    }
                    DateDropdown(
                        label = "Year",
                        value = year.toString(),
                        expanded = yearExpanded,
                        onExpandedChange = { yearExpanded = it },
                        modifier = Modifier.weight(1f),
                    ) {
                        years.forEach { y ->
                            DropdownMenuItem(
                                text = { Text(y.toString()) },
                                onClick = {
                                    year = y
                                    yearExpanded = false
                                },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(KidAgeCalculator.localDateToMillis(year, month, day))
                },
            ) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateDropdown(
    label: String,
    value: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    menuContent: @Composable () -> Unit,
) {
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
        ) {
            menuContent()
        }
    }
}