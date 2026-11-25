package com.finledger.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Immutable
data class FinLedgerExtendedColors(
    val parchment: Color,
    val pearl: Color,
    val tileDark1: Color,
    val tileDark2: Color,
    val tileDark3: Color,
    val skyLinkBlue: Color,
    val inkMuted80: Color,
    val inkMuted48: Color,
    val bodyMuted: Color,
    val dividerSoft: Color,
    val hairline: Color,
    val positive: Color,
    val negative: Color,
    val warning: Color,
    val isDark: Boolean,
)

private val LightExtendedColors = FinLedgerExtendedColors(
    parchment = Parchment,
    pearl = PearlSurface,
    tileDark1 = NearBlackTile1,
    tileDark2 = NearBlackTile2,
    tileDark3 = NearBlackTile3,
    skyLinkBlue = SkyLinkBlue,
    inkMuted80 = InkMuted80,
    inkMuted48 = InkMuted48,
    bodyMuted = BodyMuted,
    dividerSoft = DividerSoft,
    hairline = Hairline,
    positive = PositiveGreen,
    negative = NegativeRed,
    warning = WarningAmber,
    isDark = false,
)

private val DarkExtendedColors = FinLedgerExtendedColors(
    parchment = NearBlackTile2,
    pearl = NearBlackTile1,
    tileDark1 = NearBlackTile1,
    tileDark2 = NearBlackTile2,
    tileDark3 = NearBlackTile3,
    skyLinkBlue = SkyLinkBlue,
    inkMuted80 = BodyMuted,
    inkMuted48 = InkMuted48,
    bodyMuted = BodyMuted,
    dividerSoft = DarkHairline,
    hairline = DarkHairline,
    positive = PositiveGreenOnDark,
    negative = NegativeRedOnDark,
    warning = WarningAmberOnDark,
    isDark = true,
)

val LocalFinLedgerColors = staticCompositionLocalOf { LightExtendedColors }

private val LightColorScheme = lightColorScheme(
    primary = ActionBlue,
    onPrimary = PureWhiteText,
    primaryContainer = ActionBlue,
    onPrimaryContainer = PureWhiteText,
    secondary = ActionBlue,
    onSecondary = PureWhiteText,
    background = Parchment,
    onBackground = NearBlackInk,
    surface = PureWhite,
    onSurface = NearBlackInk,
    surfaceVariant = PearlSurface,
    onSurfaceVariant = InkMuted80,
    outline = Hairline,
    outlineVariant = DividerSoft,
    inverseSurface = NearBlackTile1,
    inverseOnSurface = PureWhiteText,
    error = NegativeRed,
    onError = PureWhiteText,
)

private val DarkColorScheme = darkColorScheme(
    primary = SkyLinkBlue,
    onPrimary = PureWhiteText,
    primaryContainer = ActionBlue,
    onPrimaryContainer = PureWhiteText,
    secondary = SkyLinkBlue,
    onSecondary = PureWhiteText,
    background = NearBlackTile3,
    onBackground = PureWhiteText,
    surface = NearBlackTile1,
    onSurface = PureWhiteText,
    surfaceVariant = NearBlackTile2,
    onSurfaceVariant = BodyMuted,
    outline = DarkHairline,
    outlineVariant = DarkHairline,
    inverseSurface = PureWhite,
    inverseOnSurface = NearBlackInk,
    error = NegativeRedOnDark,
    onError = PureBlack,
)

@Composable
fun FinLedgerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalFinLedgerColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = FinLedgerTypography,
            shapes = FinLedgerShapes,
            content = content,
        )
    }
}

object FinLedgerTheme {
    val colors: FinLedgerExtendedColors
        @Composable get() = LocalFinLedgerColors.current
}

