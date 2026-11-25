package com.finledger.domain.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object MoneyFormatter {

    val SUPPORTED_CURRENCIES = listOf("INR", "USD", "EUR", "GBP", "AED", "SGD")

    fun currencySymbol(currency: String): String = when (currency.uppercase()) {
        "INR" -> "₹"
        "USD" -> "$"
        "EUR" -> "€"
        "GBP" -> "£"
        "AED" -> "AED "
        "SGD" -> "S$"
        else -> "$currency "
    }

    fun format(paise: Long, currency: String = "INR", showPlus: Boolean = false): String {
        val negative = paise < 0
        val absPaise = kotlin.math.abs(paise)
        val rupees = absPaise / 100
        val remPaise = absPaise % 100

        val formattedWhole = if (currency.equals("INR", ignoreCase = true)) {
            formatIndianInteger(rupees)
        } else {
            val symbols = DecimalFormatSymbols(Locale.US)
            DecimalFormat("#,##0", symbols).format(rupees)
        }

        val decimalPart = if (remPaise == 0L) "" else ".${remPaise.toString().padStart(2, '0')}"
        val sign = when {
            negative -> "-"
            showPlus && paise > 0 -> "+"
            else -> ""
        }
        return "$sign${currencySymbol(currency)}$formattedWhole$decimalPart"
    }

    fun formatExact(paise: Long, currency: String = "INR"): String {
        val negative = paise < 0
        val absPaise = kotlin.math.abs(paise)
        val rupees = absPaise / 100
        val remPaise = (absPaise % 100).toString().padStart(2, '0')
        val formattedWhole = if (currency.equals("INR", ignoreCase = true)) {
            formatIndianInteger(rupees)
        } else {
            DecimalFormat("#,##0", DecimalFormatSymbols(Locale.US)).format(rupees)
        }
        val sign = if (negative) "-" else ""
        return "$sign${currencySymbol(currency)}$formattedWhole.$remPaise"
    }

    fun parseToPaise(input: String): Long? {
        val cleaned = input.trim()
            .replace(",", "")
            .replace(" ", "")
            .replace("S$", "")
            .replace("AED", "", ignoreCase = true)
            .replace("INR", "", ignoreCase = true)
            .replace("USD", "", ignoreCase = true)
            .replace("EUR", "", ignoreCase = true)
            .replace("GBP", "", ignoreCase = true)
            .replace("SGD", "", ignoreCase = true)
            .replace(Regex("[₹$€£¥]"), "")
        if (cleaned.isEmpty()) return null
        return try {
            BigDecimal(cleaned)
                .multiply(BigDecimal(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact()
        } catch (_: Exception) {
            null
        }
    }

    fun paiseToInputString(paise: Long): String {
        val abs = kotlin.math.abs(paise)
        val whole = abs / 100
        val rem = abs % 100
        return if (rem == 0L) whole.toString() else "$whole.${rem.toString().padStart(2, '0')}"
    }

    private fun formatIndianInteger(value: Long): String {
        val str = value.toString()
        if (str.length <= 3) return str
        val lastThree = str.takeLast(3)
        val rest = str.dropLast(3)
        val groupedRest = rest.reversed().chunked(2).joinToString(",").reversed()
        return "$groupedRest,$lastThree"
    }
}

/**
 * Evaluates plain decimal amounts and basic additive/subtractive expressions
 * (e.g., "250+80", "500-120.50") into exact paise, returning null on invalid syntax or overflow.
 */
object AmountExpressionEvaluator {
    private val termRegex = Regex("^\\d+(?:\\.\\d{1,2})?$|^\\.\\d{1,2}$")

    fun evaluateToPaise(input: String): Long? {
        val compact = input.replace(" ", "").replace(",", "")
        if (compact.isEmpty()) return null
        if (!compact.all { it.isDigit() || it == '.' || it == '+' || it == '-' }) return null
        if (compact.first() == '+' || compact.first() == '-' || compact.last() == '+' || compact.last() == '-') {
            return null
        }

        var total = 0L
        var currentSign = 1
        var index = 0
        val length = compact.length

        while (index < length) {
            var end = index
            while (end < length && compact[end] != '+' && compact[end] != '-') {
                end++
            }
            val term = compact.substring(index, end)
            if (term.isEmpty() || !termRegex.matches(term)) return null
            val termPaise = MoneyFormatter.parseToPaise(term) ?: return null
            total = try {
                if (currentSign > 0) {
                    Math.addExact(total, termPaise)
                } else {
                    Math.subtractExact(total, termPaise)
                }
            } catch (_: ArithmeticException) {
                return null
            }

            if (end < length) {
                currentSign = if (compact[end] == '+') 1 else -1
                index = end + 1
                if (index >= length || compact[index] == '+' || compact[index] == '-') return null
            } else {
                index = end
            }
        }
        return total
    }

    fun sanitizeInput(raw: String, previous: String): String {
        val filtered = raw.filter { it.isDigit() || it == '.' || it == '+' || it == '-' }
        if (filtered.isEmpty()) return ""
        val tokens = filtered.split('+', '-')
        for (token in tokens) {
            val dotCount = token.count { it == '.' }
            if (dotCount > 1) return previous
            if (dotCount == 1 && token.substringAfter('.').length > 2) return previous
        }
        return filtered
    }
}

object DateFormatter {
    private val dayMonthYear: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.getDefault())
    private val headerDate: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy", Locale.getDefault())
    private val monthYear: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
    private val timeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault())

    private fun toZonedDateTime(epochMs: Long) =
        Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault())

    fun formatDate(epochMs: Long): String = dayMonthYear.format(toZonedDateTime(epochMs))
    fun formatDateHeader(epochMs: Long): String = headerDate.format(toZonedDateTime(epochMs))
    fun formatMonthYear(epochMs: Long): String = monthYear.format(toZonedDateTime(epochMs))
    fun formatTime(epochMs: Long): String = timeFormat.format(toZonedDateTime(epochMs))
}

