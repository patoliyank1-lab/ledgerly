package com.finledger.ui.screens.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.finledger.data.local.entity.NetWorthSnapshotEntity
import com.finledger.domain.usecase.CategorySpendingItem
import com.finledger.domain.usecase.MonthlyCashFlowPoint
import com.finledger.ui.components.IconMapper
import com.finledger.ui.theme.ActionBlue
import com.finledger.ui.theme.FinLedgerTheme
import com.finledger.ui.theme.NegativeRed
import com.finledger.ui.theme.PositiveGreen
import com.finledger.ui.theme.Spacing

@Composable
fun SpendingPieChart(
    items: List<CategorySpendingItem>,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return
    val total = items.sumOf { it.totalPaise }.coerceAtLeast(1L).toFloat()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.sm),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(180.dp)) {
            val strokeWidth = 34.dp.toPx()
            val diameter = size.minDimension - strokeWidth
            val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)
            var startAngle = -90f
            items.forEach { item ->
                val sweep = (item.totalPaise.toFloat() / total) * 360f
                drawArc(
                    color = IconMapper.parseColorSafe(item.colorHex),
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = Size(diameter, diameter),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
                )
                startAngle += sweep
            }
        }
    }
}

@Composable
fun MonthlyBarChart(
    points: List<MonthlyCashFlowPoint>,
    modifier: Modifier = Modifier,
) {
    if (points.isEmpty()) return
    val maxVal = points.maxOf { maxOf(it.incomePaise, it.expensePaise) }.coerceAtLeast(1L).toFloat()

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .padding(horizontal = Spacing.xs),
        ) {
            val groupWidth = size.width / points.size
            val barWidth = (groupWidth * 0.28f).coerceAtLeast(8.dp.toPx())
            val gap = 4.dp.toPx()
            val chartHeight = size.height

            points.forEachIndexed { index, pt ->
                val cx = index * groupWidth + groupWidth / 2f
                val incHeight = (pt.incomePaise.toFloat() / maxVal) * chartHeight
                val expHeight = (pt.expensePaise.toFloat() / maxVal) * chartHeight

                drawRect(
                    color = PositiveGreen,
                    topLeft = Offset(cx - barWidth - gap / 2f, chartHeight - incHeight),
                    size = Size(barWidth, incHeight),
                )
                drawRect(
                    color = NegativeRed,
                    topLeft = Offset(cx + gap / 2f, chartHeight - expHeight),
                    size = Size(barWidth, expHeight),
                )
            }
        }
        Spacer(modifier = Modifier.height(Spacing.xxs))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            points.forEach { pt ->
                Text(
                    text = pt.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = FinLedgerTheme.colors.inkMuted48,
                )
            }
        }
    }
}

@Composable
fun NetWorthChart(
    snapshots: List<NetWorthSnapshotEntity>,
    modifier: Modifier = Modifier,
) {
    if (snapshots.size < 2) {
        Text(
            text = "Record at least 2 snapshots to visualize your net worth trend.",
            style = MaterialTheme.typography.bodySmall,
            color = FinLedgerTheme.colors.inkMuted48,
            modifier = modifier.padding(Spacing.md),
        )
        return
    }
    val minVal = snapshots.minOf { it.netWorthPaise }
    val maxVal = snapshots.maxOf { it.netWorthPaise }
    val span = (maxVal - minVal).toFloat()

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp)
            .padding(Spacing.sm),
    ) {
        val pad = 6.dp.toPx()
        val usableHeight = (size.height - 2f * pad).coerceAtLeast(1f)
        val path = Path()
        val stepX = (size.width - 2f * pad).coerceAtLeast(1f) / (snapshots.size - 1).coerceAtLeast(1)
        snapshots.forEachIndexed { idx, snap ->
            val x = pad + idx * stepX
            val ratio = if (span == 0f) 0.5f else (snap.netWorthPaise - minVal).toFloat() / span
            val y = size.height - pad - (ratio * usableHeight)
            if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
            drawCircle(
                color = ActionBlue,
                radius = 4.dp.toPx(),
                center = Offset(x, y),
            )
        }
        drawPath(
            path = path,
            color = ActionBlue,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
        )
    }
}

