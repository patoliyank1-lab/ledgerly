package com.finledger.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Typography mapped strictly from DESIGN.md.
 * Uses FontFamily.SansSerif for 100% offline reliability without network font downloads.
 * Weight ladder strictly follows DESIGN.md: 300 (Light), 400 (Normal), 600 (SemiBold), 700 (Bold).
 */
private val AppFontFamily = FontFamily.SansSerif

val FinLedgerTypography = Typography(
    // hero-display: 56px / 600 / 1.07 / -0.28px
    displayLarge = TextStyle(
        fontFamily = AppFontFamily,
        fontSize = 56.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 60.sp,
        letterSpacing = (-0.28).sp
    ),
    // display-lg: 40px / 600 / 1.10 / 0px
    displayMedium = TextStyle(
        fontFamily = AppFontFamily,
        fontSize = 40.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 44.sp,
        letterSpacing = 0.sp
    ),
    // display-md: 34px / 600 / 1.47 / -0.374px
    displaySmall = TextStyle(
        fontFamily = AppFontFamily,
        fontSize = 34.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 50.sp,
        letterSpacing = (-0.374).sp
    ),
    // lead: 28px / 400 / 1.14 / 0.196px
    headlineLarge = TextStyle(
        fontFamily = AppFontFamily,
        fontSize = 28.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 32.sp,
        letterSpacing = 0.196.sp
    ),
    // tagline: 21px / 600 / 1.19 / 0.231px
    headlineMedium = TextStyle(
        fontFamily = AppFontFamily,
        fontSize = 21.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 25.sp,
        letterSpacing = 0.231.sp
    ),
    // lead-airy: 24px / 300 / 1.5 / 0px
    headlineSmall = TextStyle(
        fontFamily = AppFontFamily,
        fontSize = 24.sp,
        fontWeight = FontWeight.Light,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    // body-strong: 17px / 600 / 1.24 / -0.374px
    titleMedium = TextStyle(
        fontFamily = AppFontFamily,
        fontSize = 17.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 21.sp,
        letterSpacing = (-0.374).sp
    ),
    // caption-strong: 14px / 600 / 1.29 / -0.224px
    titleSmall = TextStyle(
        fontFamily = AppFontFamily,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 18.sp,
        letterSpacing = (-0.224).sp
    ),
    // body-strong (M3 bodyLarge): 17px / 600 / 1.24 / -0.374px
    bodyLarge = TextStyle(
        fontFamily = AppFontFamily,
        fontSize = 17.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 21.sp,
        letterSpacing = (-0.374).sp
    ),
    // body (default paragraph): 17px / 400 / 1.47 / -0.374px
    bodyMedium = TextStyle(
        fontFamily = AppFontFamily,
        fontSize = 17.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 25.sp,
        letterSpacing = (-0.374).sp
    ),
    // caption: 14px / 400 / 1.43 / -0.224px
    bodySmall = TextStyle(
        fontFamily = AppFontFamily,
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 20.sp,
        letterSpacing = (-0.224).sp
    ),
    // button-utility: 14px / 400 / 1.29 / -0.224px
    labelLarge = TextStyle(
        fontFamily = AppFontFamily,
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 18.sp,
        letterSpacing = (-0.224).sp
    ),
    // fine-print / nav-link: 12px / 400 / 1.0 / -0.12px
    labelMedium = TextStyle(
        fontFamily = AppFontFamily,
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 14.sp,
        letterSpacing = (-0.12).sp
    ),
    // micro-legal: 10px / 400 / 1.3 / -0.08px
    labelSmall = TextStyle(
        fontFamily = AppFontFamily,
        fontSize = 10.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 13.sp,
        letterSpacing = (-0.08).sp
    ),
)

