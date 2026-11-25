package com.finledger.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.finledger.domain.util.DateFormatter
import com.finledger.ui.theme.Spacing
import java.util.Calendar
import java.util.TimeZone

object QuickDateHelper {
    fun isSameLocalDay(aMs: Long, bMs: Long): Boolean {
        val a = Calendar.getInstance().apply { timeInMillis = aMs }
        val b = Calendar.getInstance().apply { timeInMillis = bMs }
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
            a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
    }

    fun todayWithTimeOfDay(referenceMs: Long, nowMs: Long = System.currentTimeMillis()): Long {
        return shiftLocalDayWithTimeOfDay(dayOffset = 0, referenceMs = referenceMs, nowMs = nowMs)
    }

    fun yesterdayWithTimeOfDay(referenceMs: Long, nowMs: Long = System.currentTimeMillis()): Long {
        return shiftLocalDayWithTimeOfDay(dayOffset = -1, referenceMs = referenceMs, nowMs = nowMs)
    }

    private fun shiftLocalDayWithTimeOfDay(dayOffset: Int, referenceMs: Long, nowMs: Long): Long {
        val target = Calendar.getInstance().apply {
            timeInMillis = nowMs
            add(Calendar.DAY_OF_YEAR, dayOffset)
        }
        val ref = Calendar.getInstance().apply { timeInMillis = referenceMs }
        return Calendar.getInstance().apply {
            set(
                target.get(Calendar.YEAR),
                target.get(Calendar.MONTH),
                target.get(Calendar.DAY_OF_MONTH),
                ref.get(Calendar.HOUR_OF_DAY),
                ref.get(Calendar.MINUTE),
                ref.get(Calendar.SECOND),
            )
            set(Calendar.MILLISECOND, ref.get(Calendar.MILLISECOND))
        }.timeInMillis
    }
}

@Composable
fun DatePickerField(
    dateMs: Long,
    onDateSelected: (Long) -> Unit,
    label: String = "Date",
    showQuickChips: Boolean = true,
    modifier: Modifier = Modifier,
) {
    var showDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        OutlinedTextField(
            value = DateFormatter.formatDate(dateMs),
            onValueChange = {},
            readOnly = true,
            enabled = false,
            label = { Text(label, style = MaterialTheme.typography.bodySmall) },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Outlined.CalendarToday,
                    contentDescription = "Select date",
                    tint = MaterialTheme.colorScheme.primary,
                )
            },
            shape = MaterialTheme.shapes.large,
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledBorderColor = MaterialTheme.colorScheme.outline,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledContainerColor = MaterialTheme.colorScheme.surface,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDialog = true },
        )

        if (showQuickChips) {
            val nowMs = System.currentTimeMillis()
            val yesterdayCal = Calendar.getInstance().apply {
                timeInMillis = nowMs
                add(Calendar.DAY_OF_YEAR, -1)
            }.timeInMillis
            val isToday = QuickDateHelper.isSameLocalDay(dateMs, nowMs)
            val isYesterday = !isToday && QuickDateHelper.isSameLocalDay(dateMs, yesterdayCal)

            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                FilterChip(
                    selected = isToday,
                    onClick = { onDateSelected(QuickDateHelper.todayWithTimeOfDay(dateMs)) },
                    label = { Text("Today") },
                )
                FilterChip(
                    selected = isYesterday,
                    onClick = { onDateSelected(QuickDateHelper.yesterdayWithTimeOfDay(dateMs)) },
                    label = { Text("Yesterday") },
                )
                FilterChip(
                    selected = !isToday && !isYesterday,
                    onClick = { showDialog = true },
                    label = { Text("Pick") },
                )
            }
        }
    }

    if (showDialog) {
        val initialUtcMs = remember(dateMs) { localMillisToUtcMidnight(dateMs) }
        val state = rememberDatePickerState(initialSelectedDateMillis = initialUtcMs)
        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            shape = RoundedCornerShape(28.dp),
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedUtcMs = state.selectedDateMillis ?: initialUtcMs
                        onDateSelected(utcMidnightToLocalMillis(selectedUtcMs, dateMs))
                        showDialog = false
                    }
                ) { Text("Done") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = state)
        }
    }
}

private fun localMillisToUtcMidnight(localMs: Long): Long {
    val local = Calendar.getInstance().apply { timeInMillis = localMs }
    val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(
            local.get(Calendar.YEAR),
            local.get(Calendar.MONTH),
            local.get(Calendar.DAY_OF_MONTH),
        )
    }
    return utc.timeInMillis
}

private fun utcMidnightToLocalMillis(utcMs: Long, referenceLocalMs: Long): Long {
    val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = utcMs }
    val ref = Calendar.getInstance().apply { timeInMillis = referenceLocalMs }
    return Calendar.getInstance().apply {
        set(
            utc.get(Calendar.YEAR),
            utc.get(Calendar.MONTH),
            utc.get(Calendar.DAY_OF_MONTH),
            ref.get(Calendar.HOUR_OF_DAY),
            ref.get(Calendar.MINUTE),
            ref.get(Calendar.SECOND),
        )
        set(Calendar.MILLISECOND, ref.get(Calendar.MILLISECOND))
    }.timeInMillis
}

