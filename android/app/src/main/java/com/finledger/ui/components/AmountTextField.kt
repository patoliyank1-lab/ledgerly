package com.finledger.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.finledger.domain.util.MoneyFormatter

@Composable
fun AmountTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String = "Amount",
    currency: String = "INR",
    isError: Boolean = false,
    supportingText: String? = null,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { raw ->
            // Allow digits and at most one decimal point with 2 decimal digits
            val filtered = raw.filter { it.isDigit() || it == '.' }
            val parts = filtered.split(".")
            val sanitized = when {
                parts.size <= 1 -> filtered
                parts.size == 2 -> "${parts[0]}.${parts[1].take(2)}"
                else -> value
            }
            onValueChange(sanitized)
        },
        label = { Text(label, style = MaterialTheme.typography.bodySmall) },
        prefix = {
            Text(
                text = MoneyFormatter.currencySymbol(currency),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        textStyle = MaterialTheme.typography.headlineMedium,
        singleLine = true,
        isError = isError,
        supportingText = supportingText?.let { msg -> { Text(msg, style = MaterialTheme.typography.labelMedium) } },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        shape = MaterialTheme.shapes.large,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}

