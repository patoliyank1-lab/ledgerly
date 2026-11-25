package com.finledger.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Border radius scale from DESIGN.md:
 * - extraSmall: 5dp (subtle inline chips)
 * - small: 8dp (dark utility buttons, inline imagery)
 * - medium: 11dp (pearl button capsules)
 * - large: 18dp (store utility cards, grids)
 * - extraLarge: full pill (primary CTAs, search inputs, option chips)
 */
val FinLedgerShapes = Shapes(
    extraSmall = RoundedCornerShape(5.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(11.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(percent = 50),
)

