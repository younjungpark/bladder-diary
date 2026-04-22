package com.bladderdiary.app.presentation.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
    val palette = rememberHomePalette()
    val primaryGlow = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
    val secondaryGlow = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.22f)
    val totalCount = state.dailyCounts.values.sum()
    val activeDays = state.dailyCounts.count { it.value > 0 }
    val daysInMonth = getDaysInMonth(
        state.currentYearMonth.year,
        state.currentYearMonth.monthNumber
    )
    val average = if (daysInMonth == 0) 0.0 else totalCount.toDouble() / daysInMonth

    ProvideFixedFontScale {
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
                    .statusBarsPadding()
                    .padding(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 20.dp),
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

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "기록 캘린더",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${state.currentYearMonth.year}년 " +
                                "${state.currentYearMonth.monthNumber}월",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
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
                            .padding(horizontal = 12.dp, vertical = 16.dp),
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 2.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            listOf("일", "월", "화", "수", "목", "금", "토")
                                .forEachIndexed { index, dayOfWeek ->
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
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 2.dp)
                        ) {
                            items(days) { day ->
                                if (day == null) {
                                    Box(
                                        modifier = Modifier
                                            .aspectRatio(CALENDAR_DAY_CELL_ASPECT_RATIO)
                                            .heightIn(min = CALENDAR_DAY_CELL_MIN_HEIGHT)
                                    )
                                } else {
                                    CalendarDayCell(
                                        day = day,
                                        count = state.dailyCounts[day] ?: 0,
                                        isToday = day == today,
                                        palette = palette,
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
    palette: HomePalette,
    onClick: () -> Unit
) {
    val heatLevel = calendarHeatLevel(count)
    val usesCompactBadge = count >= 10
    val isSunday = day.dayOfWeek.value == 7
    val backgroundColor = calendarCellBackgroundColor(
        palette = palette,
        heatLevel = heatLevel,
        isToday = isToday
    )
    val textColor = when {
        isToday || heatLevel >= 4 -> palette.primaryButtonText
        isSunday -> palette.syncErrorTint.copy(alpha = 0.92f)
        else -> palette.titleText
    }

    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(CALENDAR_DAY_CELL_CORNER_RADIUS),
        border = calendarCellBorder(
            palette = palette,
            heatLevel = heatLevel,
            isToday = isToday
        ),
        modifier = Modifier
            .aspectRatio(CALENDAR_DAY_CELL_ASPECT_RATIO)
            .heightIn(min = CALENDAR_DAY_CELL_MIN_HEIGHT)
            .padding(2.dp)
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = "${day.monthNumber}월 ${day.dayOfMonth}일, 기록 ${count}회"
            }
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize()
        ) {
            val isCompactCell = maxWidth < 46.dp || maxHeight < 64.dp
            val isExtraCompactCell = maxWidth < 42.dp || maxHeight < 60.dp
            val fixedDayTextStyle = MaterialTheme.typography.bodySmall.copy(
                fontSize = if (isExtraCompactCell) 11.sp else 12.sp,
                lineHeight = if (isExtraCompactCell) 13.sp else 16.sp
            ).withFixedFontScale()
            val fixedBadgeTextStyle = MaterialTheme.typography.labelSmall.copy(
                fontSize = when {
                    isExtraCompactCell -> 8.sp
                    usesCompactBadge || isCompactCell -> 9.sp
                    else -> 10.sp
                },
                lineHeight = when {
                    isExtraCompactCell -> 9.sp
                    usesCompactBadge || isCompactCell -> 11.sp
                    else -> 12.sp
                }
            ).withFixedFontScale()
            val contentHorizontalPadding = when {
                isExtraCompactCell -> 4.dp
                usesCompactBadge || isCompactCell -> 5.dp
                else -> 6.dp
            }
            val contentVerticalPadding = when {
                isExtraCompactCell -> 5.dp
                isCompactCell -> 6.dp
                else -> 7.dp
            }
            val badgeHorizontalPadding = when {
                isExtraCompactCell -> 3.dp
                usesCompactBadge || isCompactCell -> 4.dp
                else -> 5.dp
            }
            val badgeVerticalPadding = when {
                isExtraCompactCell -> 0.dp
                usesCompactBadge || isCompactCell -> 1.dp
                else -> 2.dp
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal = contentHorizontalPadding,
                        vertical = contentVerticalPadding
                    ),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = day.dayOfMonth.toString(),
                    style = fixedDayTextStyle,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                    color = textColor,
                    maxLines = 1,
                    softWrap = false
                )

                if (count > 0) {
                    Surface(
                        color = calendarCountBadgeContainerColor(
                            palette = palette,
                            heatLevel = heatLevel,
                            isToday = isToday
                        ),
                        shape = RoundedCornerShape(
                            when {
                                isExtraCompactCell -> 8.dp
                                else -> 10.dp
                            }
                        ),
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(
                            text = calendarCountLabel(count),
                            style = fixedBadgeTextStyle,
                            color = calendarCountBadgeContentColor(
                                palette = palette,
                                heatLevel = heatLevel,
                                isToday = isToday
                            ),
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(
                                horizontal = badgeHorizontalPadding,
                                vertical = badgeVerticalPadding
                            )
                        )
                    }
                } else {
                    Spacer(
                        modifier = Modifier.height(
                            when {
                                isExtraCompactCell -> 6.dp
                                else -> 8.dp
                            }
                        )
                    )
                }
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

private const val CALENDAR_DAY_CELL_ASPECT_RATIO = 0.74f
private val CALENDAR_DAY_CELL_MIN_HEIGHT = 64.dp
private val CALENDAR_DAY_CELL_CORNER_RADIUS = 18.dp

private fun calendarHeatLevel(count: Int): Int {
    return when (count) {
        0 -> 0
        in 1..2 -> 1
        in 3..4 -> 2
        in 5..6 -> 3
        in 7..9 -> 4
        else -> 5
    }
}

private fun calendarCellBackgroundColor(
    palette: HomePalette,
    heatLevel: Int,
    isToday: Boolean
): Color {
    if (isToday) {
        return lerp(palette.surfaceStrong, palette.primaryStrong, 0.7f)
    }

    return when (heatLevel) {
        0 -> palette.surfaceMuted.copy(alpha = 0.34f)
        1 -> lerp(palette.surfaceStrong, palette.primaryStrong, 0.12f)
        2 -> lerp(palette.surfaceStrong, palette.primaryStrong, 0.24f)
        3 -> lerp(palette.surfaceStrong, palette.primaryStrong, 0.4f)
        4 -> lerp(palette.surfaceStrong, palette.primaryStrong, 0.58f)
        else -> lerp(palette.surfaceStrong, palette.primaryStrong, 0.78f)
    }
}

private fun calendarCellBorder(
    palette: HomePalette,
    heatLevel: Int,
    isToday: Boolean
): BorderStroke? {
    return when {
        isToday -> BorderStroke(1.5.dp, palette.primaryStrong.copy(alpha = 0.96f))
        heatLevel >= 5 -> BorderStroke(1.dp, palette.primaryStrong.copy(alpha = 0.26f))
        else -> null
    }
}

private fun calendarCountBadgeContainerColor(
    palette: HomePalette,
    heatLevel: Int,
    isToday: Boolean
): Color {
    return when {
        isToday || heatLevel >= 4 -> Color.White.copy(alpha = 0.18f)
        heatLevel >= 2 -> palette.surfaceStrong.copy(alpha = 0.82f)
        else -> palette.surfaceStrong.copy(alpha = 0.9f)
    }
}

private fun calendarCountBadgeContentColor(
    palette: HomePalette,
    heatLevel: Int,
    isToday: Boolean
): Color {
    return when {
        isToday || heatLevel >= 4 -> palette.primaryButtonText
        else -> palette.mutedText
    }
}

private fun calendarCountLabel(count: Int): String {
    return if (count >= 10) "10+" else "${count}회"
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
