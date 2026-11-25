package com.finledger.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.finledger.domain.util.AmountExpressionEvaluator
import com.finledger.domain.util.MoneyFormatter
import com.finledger.ui.theme.Spacing

@Composable
fun AmountTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String = "Amount",
    currency: String = "INR",
    isError: Boolean = false,
    supportingText: String? = null,
    modifier: Modifier = Modifier,
    autoFocus: Boolean = false,
    focusRequester: FocusRequester? = null,
    imeAction: ImeAction = ImeAction.Done,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    allowCalculator: Boolean = false,
) {
    val activeFocusRequester = if (autoFocus || focusRequester != null) {
        remember(focusRequester) { focusRequester ?: FocusRequester() }
    } else {
        null
    }

    LaunchedEffect(autoFocus, activeFocusRequester) {
        if (autoFocus && activeFocusRequester != null) {
            activeFocusRequester.requestFocus()
        }
    }

    val previewText = if (allowCalculator && value.any { it == '+' || it == '-' }) {
        val evaluated = AmountExpressionEvaluator.evaluateToPaise(value)
        if (evaluated != null && evaluated > 0L) {
            "= ${MoneyFormatter.format(evaluated, currency)}"
        } else {
            null
        }
    } else {
        null
    }
    val effectiveSupportingText = supportingText ?: previewText

    OutlinedTextField(
        value = value,
        onValueChange = { raw ->
            val sanitized = if (allowCalculator) {
                AmountExpressionEvaluator.sanitizeInput(raw, value)
            } else {
                // Allow digits and at most one decimal point with 2 decimal digits
                val filtered = raw.filter { it.isDigit() || it == '.' }
                val parts = filtered.split(".")
                when {
                    parts.size <= 1 -> filtered
                    parts.size == 2 -> "${parts[0]}.${parts[1].take(2)}"
                    else -> value
                }
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
        trailingIcon = if (allowCalculator) {
            {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(end = Spacing.xs),
                ) {
                    listOf('+', '-').forEach { op ->
                        val canAppend = value.isNotEmpty() && value.last() != '+' && value.last() != '-'
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier
                                .size(30.dp)
                                .clip(MaterialTheme.shapes.small)
                                .clickable(enabled = canAppend) {
                                    onValueChange("$value$op")
                                },
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = op.toString(),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (canAppend) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        } else {
            null
        },
        textStyle = MaterialTheme.typography.headlineMedium,
        singleLine = true,
        isError = isError,
        supportingText = effectiveSupportingText?.let { msg -> { Text(msg, style = MaterialTheme.typography.labelMedium) } },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Decimal,
            imeAction = imeAction,
        ),
        keyboardActions = keyboardActions,
        shape = MaterialTheme.shapes.large,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
        ),
        modifier = modifier
            .fillMaxWidth()
            .let { base ->
                if (activeFocusRequester != null) base.focusRequester(activeFocusRequester) else base
            },
    )
}

