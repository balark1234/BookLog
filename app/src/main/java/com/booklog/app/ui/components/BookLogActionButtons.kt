package com.booklog.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.booklog.app.ui.theme.DeepPurple
import com.booklog.app.ui.theme.Lavender
import com.booklog.app.ui.theme.SkyBlue

@Composable
fun BookLogActionButtons(
    onScanClick: () -> Unit,
    onManualAddClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FloatingActionButton(
            onClick = onManualAddClick,
            modifier = Modifier.size(52.dp),
            shape = CircleShape,
            containerColor = Lavender,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add manually")
        }
        FloatingActionButton(
            onClick = onScanClick,
            modifier = Modifier.size(72.dp),
            shape = RoundedCornerShape(22.dp),
            containerColor = DeepPurple,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ) {
            Icon(
                Icons.Default.QrCodeScanner,
                contentDescription = "Scan book barcode",
                modifier = Modifier.size(34.dp),
            )
        }
    }
}