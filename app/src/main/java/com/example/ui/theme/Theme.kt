package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// --- Light Color Schemes ---
private val SerenityLightScheme = lightColorScheme(
    primary = SerenityPrimary,
    secondary = SerenitySecondary,
    tertiary = SerenityTertiary,
    background = SerenityBackground,
    surface = SerenitySurface,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = Color(0xFF1B2A4A),
    onSurface = Color(0xFF1B2A4A)
)

private val VerdantLightScheme = lightColorScheme(
    primary = VerdantPrimary,
    secondary = VerdantSecondary,
    tertiary = VerdantTertiary,
    background = VerdantBackground,
    surface = VerdantSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.Black,
    onBackground = Color(0xFF19201B),
    onSurface = Color(0xFF19201B)
)

// --- Dark Color Schemes ---
private val SerenityDarkScheme = darkColorScheme(
    primary = SerenityTertiary,
    secondary = SerenitySecondary,
    tertiary = SerenityPrimary,
    background = SerenityDarkBg,
    surface = SerenityDarkSurface,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onTertiary = Color.White,
    onBackground = Color(0xFFE3F2FD),
    onSurface = Color(0xFFE3F2FD)
)

private val VerdantDarkScheme = darkColorScheme(
    primary = VerdantTertiary,
    secondary = VerdantSecondary,
    tertiary = VerdantPrimary,
    background = VerdantDarkBg,
    surface = VerdantDarkSurface,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFFE8F5E9),
    onSurface = Color(0xFFE8F5E9)
)

@Composable
fun ARTheme(
    themeName: String = "Serenity",
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeName) {
        "Verdant" -> if (darkTheme) VerdantDarkScheme else VerdantLightScheme
        else -> if (darkTheme) SerenityDarkScheme else SerenityLightScheme // Serenity is default
    }

    // Set typography font families depending on atmosphere!
    val fontFamily = when (themeName) {
        "Serenity" -> FontFamily.Serif // Elegant serif
        "Verdant" -> FontFamily.Default // Soft standard/rounded sans-serif layout
        else -> FontFamily.Default
    }

    val customTypography = androidx.compose.material3.Typography(
        displayLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp
        ),
        headlineLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp
        ),
        titleLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp
        ),
        labelLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp
        )
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = customTypography,
        content = content
    )
}
