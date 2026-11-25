package com.finledger.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

object IconMapper {
    fun categoryIcon(iconName: String): ImageVector = when (iconName) {
        "restaurant" -> Icons.Outlined.Restaurant
        "shopping_cart" -> Icons.Outlined.ShoppingCart
        "directions_car" -> Icons.Outlined.DirectionsCar
        "shopping_bag" -> Icons.Outlined.ShoppingBag
        "receipt" -> Icons.Outlined.Receipt
        "home" -> Icons.Outlined.Home
        "medical_services" -> Icons.Outlined.MedicalServices
        "movie" -> Icons.Outlined.Movie
        "flight" -> Icons.Outlined.Flight
        "school" -> Icons.Outlined.School
        "spa" -> Icons.Outlined.Spa
        "trending_up" -> Icons.Outlined.TrendingUp
        "percent" -> Icons.Outlined.Percent
        "work" -> Icons.Outlined.Work
        "laptop" -> Icons.Outlined.Laptop
        "storefront" -> Icons.Outlined.Storefront
        "apartment" -> Icons.Outlined.Apartment
        "savings" -> Icons.Outlined.Savings
        "card_giftcard" -> Icons.Outlined.CardGiftcard
        "payments" -> Icons.Outlined.Payments
        "account_balance" -> Icons.Outlined.AccountBalance
        "credit_card" -> Icons.Outlined.CreditCard
        "wallet" -> Icons.Outlined.AccountBalanceWallet
        else -> Icons.Outlined.Category
    }

    fun parseColorSafe(hex: String, fallback: Color = Color(0xFF0066CC)): Color {
        return try {
            Color(android.graphics.Color.parseColor(hex))
        } catch (_: Exception) {
            fallback
        }
    }
}

