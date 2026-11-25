# FinLedger — Design Tokens (DESIGN.md → Android)

## Colors (`ui/theme/Color.kt`)

```kotlin
// Brand & Accent
val ActionBlue = Color(0xFF0066CC)         // primary
val FocusBlue = Color(0xFF0071E3)          // primary focus ring
val SkyLinkBlue = Color(0xFF2997FF)        // primary on dark surfaces

// Light Surfaces
val PureWhite = Color(0xFFFFFFFF)          // canvas
val Parchment = Color(0xFFF5F5F7)          // canvas-parchment / background
val PearlSurface = Color(0xFFFAFAFC)       // surface-pearl

// Dark Surfaces
val NearBlackTile1 = Color(0xFF272729)     // surface-tile-1
val NearBlackTile2 = Color(0xFF2A2A2C)     // surface-tile-2
val NearBlackTile3 = Color(0xFF252527)     // surface-tile-3
val PureBlack = Color(0xFF000000)          // surface-black (nav bar only)
val ChipGray = Color(0xFFD2D2D7)           // surface-chip-translucent

// Text
val NearBlackInk = Color(0xFF1D1D1F)       // ink / body
val InkMuted80 = Color(0xFF333333)         // ink-muted-80
val InkMuted48 = Color(0xFF7A7A7A)         // ink-muted-48
val BodyMuted = Color(0xFFCCCCCC)          // body-muted (on dark)
val PureWhiteText = Color(0xFFFFFFFF)      // body-on-dark

// Dividers
val DividerSoft = Color(0xFFF0F0F0)        // divider-soft
val Hairline = Color(0xFFE0E0E0)           // hairline borders
```

## Light Color Scheme (`LightColorScheme`)
```kotlin
val LightColorScheme = lightColorScheme(
    primary = ActionBlue,
    onPrimary = PureWhiteText,
    primaryContainer = ActionBlue,
    secondary = ActionBlue,
    background = Parchment,
    surface = PureWhite,
    surfaceVariant = PearlSurface,
    onBackground = NearBlackInk,
    onSurface = NearBlackInk,
    onSurfaceVariant = InkMuted80,
    outline = Hairline,
    outlineVariant = DividerSoft,
    inverseSurface = NearBlackTile1,
    inverseOnSurface = PureWhiteText,
)
```

## Dark Color Scheme (`DarkColorScheme`)
```kotlin
val DarkColorScheme = darkColorScheme(
    primary = SkyLinkBlue,
    onPrimary = PureWhiteText,
    background = NearBlackTile3,
    surface = NearBlackTile1,
    surfaceVariant = NearBlackTile2,
    onBackground = PureWhiteText,
    onSurface = PureWhiteText,
    onSurfaceVariant = BodyMuted,
    outline = Color(0xFF444446),
)
```

---

## Typography (`ui/theme/Type.kt`)

> SF Pro is Apple-proprietary. Android uses **Inter** (Google Fonts) as substitute.

```kotlin
// Add to build.gradle: implementation("androidx.compose.ui:ui-text-google-fonts")

val InterFamily = FontFamily(
    Font(GoogleFont("Inter"), weight = FontWeight.Light),     // 300
    Font(GoogleFont("Inter"), weight = FontWeight.Normal),    // 400
    Font(GoogleFont("Inter"), weight = FontWeight.SemiBold),  // 600
    Font(GoogleFont("Inter"), weight = FontWeight.Bold),      // 700
)

val FinLedgerTypography = Typography(
    // hero-display: 56sp / 600 / -0.28sp
    displayLarge = TextStyle(
        fontFamily = InterFamily,
        fontSize = 56.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = (56 * 1.07).sp,
        letterSpacing = (-0.28).sp
    ),
    // display-lg: 40sp / 600
    displayMedium = TextStyle(
        fontFamily = InterFamily,
        fontSize = 40.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = (40 * 1.1).sp,
        letterSpacing = 0.sp
    ),
    // display-md: 34sp / 600 / -0.374sp
    displaySmall = TextStyle(
        fontFamily = InterFamily,
        fontSize = 34.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = (34 * 1.47).sp,
        letterSpacing = (-0.374).sp
    ),
    // tagline: 21sp / 600 / 0.231sp
    headlineMedium = TextStyle(
        fontFamily = InterFamily,
        fontSize = 21.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = (21 * 1.19).sp,
        letterSpacing = 0.231.sp
    ),
    // body-strong: 17sp / 600 / -0.374sp
    bodyLarge = TextStyle(
        fontFamily = InterFamily,
        fontSize = 17.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = (17 * 1.24).sp,
        letterSpacing = (-0.374).sp
    ),
    // body: 17sp / 400 / -0.374sp
    bodyMedium = TextStyle(
        fontFamily = InterFamily,
        fontSize = 17.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = (17 * 1.47).sp,
        letterSpacing = (-0.374).sp
    ),
    // caption: 14sp / 400 / -0.224sp
    bodySmall = TextStyle(
        fontFamily = InterFamily,
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = (14 * 1.43).sp,
        letterSpacing = (-0.224).sp
    ),
    // button-utility: 14sp / 400 / -0.224sp
    labelLarge = TextStyle(
        fontFamily = InterFamily,
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = (14 * 1.29).sp,
        letterSpacing = (-0.224).sp
    ),
    // fine-print: 12sp / 400 / -0.12sp
    labelSmall = TextStyle(
        fontFamily = InterFamily,
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = (12 * 1.0).sp,
        letterSpacing = (-0.12).sp
    ),
)
```

---

## Shapes (`ui/theme/Shape.kt`)

```kotlin
val FinLedgerShapes = Shapes(
    // rounded.xs = 5dp (rare inline chips)
    extraSmall = RoundedCornerShape(5.dp),
    // rounded.sm = 8dp (utility/nav buttons)
    small = RoundedCornerShape(8.dp),
    // rounded.md = 11dp (pearl capsule)
    medium = RoundedCornerShape(11.dp),
    // rounded.lg = 18dp (store utility cards)
    large = RoundedCornerShape(18.dp),
    // rounded.pill = 9999dp (primary CTAs, search, chips)
    extraLarge = RoundedCornerShape(percent = 50),
)
```

---

## Spacing Constants

```kotlin
object Spacing {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 17.dp
    val lg = 24.dp
    val xl = 32.dp
    val xxl = 48.dp
    val section = 80.dp
}
```

---

## Component Specifications

### Primary Button
```kotlin
Button(
    onClick = { },
    shape = MaterialTheme.shapes.extraLarge,       // pill
    colors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primary,   // ActionBlue
        contentColor = MaterialTheme.colorScheme.onPrimary,   // white
    ),
    contentPadding = PaddingValues(horizontal = 22.dp, vertical = 11.dp),
) {
    Text(text, style = MaterialTheme.typography.bodyMedium)
}
```

### Secondary / Ghost Button
```kotlin
OutlinedButton(
    onClick = { },
    shape = MaterialTheme.shapes.extraLarge,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
    colors = ButtonDefaults.outlinedButtonColors(
        contentColor = MaterialTheme.colorScheme.primary
    ),
    contentPadding = PaddingValues(horizontal = 22.dp, vertical = 11.dp),
)
```

### Card (Utility)
```kotlin
Card(
    shape = MaterialTheme.shapes.large,            // 18dp corners
    colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface  // white
    ),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline), // hairline
    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), // flat
)
```

### Bottom Navigation
```kotlin
NavigationBar(
    containerColor = MaterialTheme.colorScheme.surface,
    tonalElevation = 3.dp,
)
```

### Top App Bar
```kotlin
TopAppBar(
    colors = TopAppBarDefaults.topAppBarColors(
        containerColor = MaterialTheme.colorScheme.background,
    ),
    title = {
        Text(style = MaterialTheme.typography.headlineMedium)
    }
)
```

---

## Elevation Rules (from DESIGN.md)

| Element | Elevation | Why |
|---|---|---|
| Cards | `0.dp` | Flat — color contrast provides hierarchy |
| Bottom nav | `3.dp` tonal | Subtle surface separation |
| FAB | Default Material3 | Standard floating action |
| Top app bar | `0.dp` / scrolled `3.dp` | Matches tile rhythm |
| Dialogs / Sheets | `6.dp` | Standard modal elevation |

> [!WARNING]
> NEVER add drop shadows to cards, text, or buttons. The design spec explicitly prohibits it. Use `elevation = 0.dp` on all `Card` and `Surface` composables unless specified above.

