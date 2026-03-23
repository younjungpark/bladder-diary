package com.bladderdiary.app.presentation.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import java.util.Locale

@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel,
    onDateSelected: (LocalDate) -> Unit,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val today = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }
    val primaryGlow = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
    val secondaryGlow = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.22f)
    val totalCount = state.dailyCounts.values.sum()
    val activeDays = state.dailyCounts.count { it.value > 0 }
    val daysInMonth = getDaysInMonth(state.currentYearMonth.year, state.currentYearMonth.monthNumber)
    val average = if (daysInMonth == 0) 0.0 else totalCount.toDouble() / daysInMonth

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .drawBehind {
                drawCircle(
                    color = primaryGlow,
                    radius = size.minDimension * 0.3f,
                    center = Offset(size.width * 0.08f, size.height * 0.1f)
                )
                drawCircle(
                    color = secondaryGlow,
                    radius = size.minDimension * 0.24f,
                    center = Offset(size.width * 0.9f, size.height * 0.14f)
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.clickable(onClick = onBack),
                    color = MaterialTheme.colorScheme.surface,
                    shape = CircleShape,
                    shadowElevation = 8.dp
                ) {
                    Box(
                        modifier = Modifier.size(42.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "기록 캘린더",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${state.currentYearMonth.year}년 ${state.currentYearMonth.monthNumber}월",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(30.dp),
                shadowElevation = 10.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CalendarMetric(
                        modifier = Modifier.weight(1f),
                        label = "총 기록",
                        value = "${totalCount}회"
                    )
                    CalendarMetric(
                        modifier = Modifier.weight(1f),
                        label = "기록한 날",
                        value = "${activeDays}일"
                    )
                    CalendarMetric(
                        modifier = Modifier.weight(1f),
                        label = "일평균",
                        value = String.format(Locale.KOREA, "%.1f회", average)
                    )
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(30.dp),
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MonthNavButton(
                            label = "이전 달",
                            icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            onClick = viewModel::goPreviousMonth
                        )
                        Text(
                            text = "${state.currentYearMonth.monthNumber}월",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        MonthNavButton(
                            label = "다음 달",
                            icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            onClick = viewModel::goNextMonth
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        listOf("일", "월", "화", "수", "목", "금", "토").forEachIndexed { index, dayOfWeek ->
                            Text(
                                text = dayOfWeek,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (index == 0) {
                                    MaterialTheme.colorScheme.error.copy(alpha = 0.9f)
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    val days = generateCalendarDays(state.currentYearMonth)
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(7),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(days) { day ->
                            if (day == null) {
                                Box(modifier = Modifier.aspectRatio(CALENDAR_DAY_CELL_ASPECT_RATIO))
                            } else {
                                CalendarDayCell(
                                    day = day,
                                    count = state.dailyCounts[day] ?: 0,
                                    isToday = day == today,
                                    onClick = { onDateSelected(day) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarMetric(
    modifier: Modifier = Modifier,
    label: String,
    value: String
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun MonthNavButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun CalendarDayCell(
    day: LocalDate,
    count: Int,
    isToday: Boolean,
    onClick: () -> Unit
) {
    val fixedBadgeTextStyle = MaterialTheme.typography.labelSmall.withFixedFontScale()
    val backgroundColor = when {
        isToday -> MaterialTheme.colorScheme.primaryContainer
        count > 0 -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = dayCountAlpha(count))
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f)
    }
    val textColor = when {
        isToday -> MaterialTheme.colorScheme.onPrimaryContainer
        day.dayOfWeek.value == 7 -> MaterialTheme.colorScheme.error.copy(alpha = 0.9f)
        else -> MaterialTheme.colorScheme.onSurface
    }

    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .aspectRatio(CALENDAR_DAY_CELL_ASPECT_RATIO)
            .padding(4.dp)
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = "${day.monthNumber}월 ${day.dayOfMonth}일, 기록 ${count}회"
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 7.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = day.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                color = textColor
            )

            if (count > 0) {
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = if (isToday) 0.28f else 0.72f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "${count}회",
                        style = fixedBadgeTextStyle,
                        color = if (isToday) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun TextStyle.withFixedFontScale(): TextStyle {
    val fontScale = LocalDensity.current.fontScale
    return copy(
        fontSize = fontSize.value.div(fontScale).sp,
        lineHeight = lineHeight.value.div(fontScale).sp,
        letterSpacing = letterSpacing.value.div(fontScale).sp
    )
}

private const val CALENDAR_DAY_CELL_ASPECT_RATIO = 0.76f

private fun dayCountAlpha(count: Int): Float {
    return when (count) {
        0 -> 0f
        1 -> 0.38f
        2 -> 0.48f
        3 -> 0.58f
        4 -> 0.68f
        else -> 0.78f
    }
}

private fun generateCalendarDays(yearMonth: LocalDate): List<LocalDate?> {
    val firstDayOfMonth = LocalDate(yearMonth.year, yearMonth.month, 1)
    val daysInMonth = getDaysInMonth(yearMonth.year, yearMonth.monthNumber)
    val startDayOfWeek = firstDayOfMonth.dayOfWeek.value % 7

    val days = mutableListOf<LocalDate?>()
    repeat(startDayOfWeek) { days.add(null) }
    for (i in 1..daysInMonth) {
        days.add(LocalDate(yearMonth.year, yearMonth.month, i))
    }
    repeat((7 - (days.size % 7)) % 7) { days.add(null) }
    return days
}

private fun getDaysInMonth(year: Int, month: Int): Int {
    return when (month) {
        2 -> if (isLeapYear(year)) 29 else 28
        4, 6, 9, 11 -> 30
        else -> 31
    }
}

private fun isLeapYear(year: Int): Boolean {
    return year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)
}
