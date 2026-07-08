package com.booklog.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val KidsColorScheme = lightColorScheme(
    primary = DeepPurple,
    onPrimary = Color.White,
    primaryContainer = Lavender.copy(alpha = 0.4f),
    onPrimaryContainer = DeepPurple,
    secondary = SkyBlue,
    onSecondary = Color.White,
    secondaryContainer = SkyBlue.copy(alpha = 0.25f),
    tertiary = SunnyYellow,
    onTertiary = Color(0xFF5D4037),
    background = PeachCream,
    onBackground = Color(0xFF2D2A32),
    surface = Color.White,
    onSurface = Color(0xFF2D2A32),
    surfaceVariant = Color(0xFFF3EFFF),
    onSurfaceVariant = Color(0xFF6B5B7B),
    error = CoralPink,
    onError = Color.White,
)

private val KidsTypography = Typography(
    headlineLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 32.sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 26.sp),
    headlineSmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 22.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 17.sp),
    bodyLarge = TextStyle(fontSize = 16.sp),
    bodyMedium = TextStyle(fontSize = 14.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp),
)

@Composable
fun BookLogTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = KidsColorScheme,
        typography = KidsTypography,
        content = content,
    )
}