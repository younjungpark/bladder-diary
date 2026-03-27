package com.bladderdiary.app.presentation.main

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bladderdiary.app.domain.model.VoidingEvent
import kotlinx.datetime.LocalDate

@Composable
internal fun MainContent(
    state: MainUiState,
    today: LocalDate,
    palette: HomePalette,
    modifier: Modifier = Modifier,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onPickDate: () -> Unit,
    onOpenMemo: (VoidingEvent) -> Unit,
    onOpenVolume: (VoidingEvent) -> Unit,
    onDeleteEvent: (String) -> Unit
) {
    val sortedEvents = remember(state.events) {
        state.events.sortedByDescending { it.voidedAtEpochMs }
    }
    val averageIntervalMillis = remember(sortedEvents) {
        sortedEvents.toAverageIntervalMillis()
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 124.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            HeroDateCard(
                palette = palette,
                selectedDate = state.selectedDate,
                today = today,
                onPreviousDay = onPreviousDay,
                onNextDay = onNextDay,
                onPickDate = onPickDate
            )
        }

        item {
            SummarySection(
                palette = palette,
                dailyVolumeMl = state.dailyVolumeMl ?: 0,
                dailyCount = state.dailyCount,
                averageIntervalMillis = averageIntervalMillis
            )
        }

        if (state.pendingSyncCount > 0) {
            item {
                InlineNotice(
                    palette = palette,
                    text = state.pendingSyncError?.let { rawError ->
                        if (rawError.isLikelyOfflineSyncError()) {
                            "오프라인 상태입니다. 연결되면 자동으로 동기화됩니다."
                        } else {
                            "동기화 오류: ${rawError.toUiErrorText()}"
                        }
                    } ?: "기록은 안전하게 로컬에 보관되며 연결되면 자동으로 동기화됩니다."
                )
            }
        }

        item {
            SectionHeader(
                title = if (state.selectedDate == today) "오늘의 기록" else "선택한 날짜의 기록",
                palette = palette
            )
        }

        if (sortedEvents.isEmpty()) {
            item {
                RecordsEmptyState(
                    palette = palette,
                    selectedDate = state.selectedDate,
                    today = today
                )
            }
        } else {
            itemsIndexed(sortedEvents, key = { _, item -> item.localId }) { index, event ->
                val previousEvent = sortedEvents.getOrNull(index + 1)
                DiaryTimelineItem(
                    palette = palette,
                    event = event,
                    intervalText = previousEvent
                        ?.let { event.voidedAtEpochMs - it.voidedAtEpochMs }
                        ?.takeIf { it > 0 }
                        ?.toIntervalText(),
                    isFirst = index == 0,
                    isLast = index == sortedEvents.lastIndex,
                    onOpenMemo = { onOpenMemo(event) },
                    onOpenVolume = { onOpenVolume(event) },
                    onDelete = { onDeleteEvent(event.localId) }
                )
            }
        }
    }
}

@Composable
private fun HeroDateCard(
    palette: HomePalette,
    selectedDate: LocalDate,
    today: LocalDate,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onPickDate: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = palette.surfaceStrong,
        shape = RoundedCornerShape(26.dp),
        shadowElevation = 10.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassIconButton(
                palette = palette,
                icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "이전 날짜",
                buttonSize = 38.dp,
                iconSize = 18.dp,
                cornerRadius = 14.dp,
                onClick = onPreviousDay
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable(onClick = onPickDate)
                    .background(palette.surfaceMuted)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = selectedDate.toHeroDateText(),
                    style = MaterialTheme.typography.titleLarge,
                    color = palette.titleText,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = selectedDate.toHeroCaption(today),
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            GlassIconButton(
                palette = palette,
                icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "다음 날짜",
                buttonSize = 38.dp,
                iconSize = 18.dp,
                cornerRadius = 14.dp,
                onClick = onNextDay
            )
        }
    }
}

@Composable
private fun SummarySection(
    palette: HomePalette,
    dailyVolumeMl: Int,
    dailyCount: Int,
    averageIntervalMillis: Long?
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader(
            title = "오늘의 요약",
            palette = palette
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SummaryMetricCard(
                modifier = Modifier.weight(1f),
                palette = palette,
                icon = Icons.Default.WaterDrop,
                label = "배뇨량",
                value = dailyVolumeMl.toString(),
                unit = "mL"
            )
            SummaryMetricCard(
                modifier = Modifier.weight(1f),
                palette = palette,
                icon = Icons.AutoMirrored.Filled.FormatListBulleted,
                label = "횟수",
                value = dailyCount.toString(),
                unit = "회"
            )
            SummaryMetricCard(
                modifier = Modifier.weight(1f),
                palette = palette,
                icon = Icons.Default.Schedule,
                label = "평균 간격",
                value = averageIntervalMillis.toMetricValue(),
                unit = averageIntervalMillis?.let { "h" }
            )
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    supporting: String? = null,
    palette: HomePalette
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (supporting != null) Arrangement.SpaceBetween else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 18.sp,
                lineHeight = 22.sp
            ),
            color = palette.titleText,
            fontWeight = FontWeight.SemiBold
        )
        supporting?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelSmall,
                color = palette.subtleText,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SummaryMetricCard(
    modifier: Modifier = Modifier,
    palette: HomePalette,
    icon: ImageVector,
    label: String,
    value: String,
    unit: String?
) {
    val labelFontSize = if (label.length >= 5) 9.sp else 10.sp
    val labelLineHeight = if (label.length >= 5) 11.sp else 12.sp
    val valueFontSize = when {
        value.length >= 5 -> 18.sp
        value.length >= 3 -> 20.sp
        else -> 22.sp
    }
    val valueLineHeight = when {
        value.length >= 5 -> 20.sp
        value.length >= 3 -> 22.sp
        else -> 24.sp
    }

    Surface(
        modifier = modifier,
        color = palette.surfaceStrong,
        shape = RoundedCornerShape(22.dp),
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(palette.surfaceTint),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = palette.primaryStrong,
                        modifier = Modifier.size(13.dp)
                    )
                }

                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = labelFontSize,
                        lineHeight = labelLineHeight
                    ),
                    color = palette.bodyText,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = valueFontSize,
                        lineHeight = valueLineHeight
                    ),
                    color = palette.titleText,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                unit?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            lineHeight = 12.sp
                        ),
                        color = palette.mutedText,
                        modifier = Modifier.padding(bottom = 3.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DiaryTimelineItem(
    palette: HomePalette,
    event: VoidingEvent,
    intervalText: String?,
    isFirst: Boolean,
    isLast: Boolean,
    onOpenMemo: () -> Unit,
    onOpenVolume: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        TimelineRail(
            palette = palette,
            isFirst = isFirst,
            isLast = isLast,
            isHighlighted = isFirst
        )

        DiaryEventCard(
            palette = palette,
            event = event,
            intervalText = intervalText,
            onOpenMemo = onOpenMemo,
            onOpenVolume = onOpenVolume,
            onDelete = onDelete
        )
    }
}

@Composable
private fun TimelineRail(
    palette: HomePalette,
    isFirst: Boolean,
    isLast: Boolean,
    isHighlighted: Boolean
) {
    Box(
        modifier = Modifier
            .width(22.dp)
            .fillMaxHeight()
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val centerX = size.width / 2f
            val dotCenterY = 26.dp.toPx()
            val gap = 7.dp.toPx()

            if (!isFirst) {
                drawLine(
                    color = palette.lineColor,
                    start = androidx.compose.ui.geometry.Offset(centerX, 0f),
                    end = androidx.compose.ui.geometry.Offset(centerX, dotCenterY - gap),
                    strokeWidth = 2.dp.toPx()
                )
            }

            if (!isLast) {
                drawLine(
                    color = palette.lineColor,
                    start = androidx.compose.ui.geometry.Offset(centerX, dotCenterY + gap),
                    end = androidx.compose.ui.geometry.Offset(centerX, size.height),
                    strokeWidth = 2.dp.toPx()
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 20.dp)
                .size(12.dp)
                .clip(CircleShape)
                .background(if (isHighlighted) palette.primaryStrong else palette.trackMuted)
        )
    }
}

@Composable
private fun DiaryEventCard(
    palette: HomePalette,
    event: VoidingEvent,
    intervalText: String?,
    onOpenMemo: () -> Unit,
    onOpenVolume: () -> Unit,
    onDelete: () -> Unit
) {
    val (timeText, periodText) = event.voidedAtEpochMs.toTimeDisplay()
    val hasMemo = !event.memo.isNullOrBlank()
    val hasVolume = event.volumeMl != null

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = palette.surfaceStrong,
        shape = RoundedCornerShape(28.dp),
        shadowElevation = 10.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "$periodText $timeText",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontSize = 16.sp,
                            lineHeight = 20.sp
                        ),
                        color = palette.titleText,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (hasMemo) {
                            SmallBadge(
                                text = "메모",
                                containerColor = palette.surfaceTint,
                                contentColor = palette.primaryStrong
                            )
                        }
                        event.urgency?.let { urgency ->
                            val badgeColors = urgencyBadgeColors(urgency)
                            SmallBadge(
                                text = "절박감 ${urgency.toUrgencyLabel()}",
                                containerColor = badgeColors.first,
                                contentColor = badgeColors.second
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = palette.subtleText,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = intervalText?.let { "$it 간격" } ?: "이전 기록 없음",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                lineHeight = 14.sp
                            ),
                            color = palette.mutedText
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (hasVolume) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = event.volumeMl.toString(),
                                style = MaterialTheme.typography.displaySmall,
                                color = palette.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "mL",
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.primary,
                                modifier = Modifier.padding(bottom = 5.dp)
                            )
                        }
                    } else {
                        Text(
                            text = "-",
                            style = MaterialTheme.typography.displaySmall,
                            color = palette.subtleText,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            if (hasMemo) {
                Surface(
                    color = palette.noteSurface,
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text(
                        text = event.memo.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        ),
                        color = palette.bodyText,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                EventActionButton(
                    palette = palette,
                    icon = Icons.Default.Description,
                    contentDescription = if (hasMemo) "메모 수정" else "메모 입력",
                    active = hasMemo,
                    onClick = onOpenMemo
                )
                EventActionButton(
                    palette = palette,
                    icon = Icons.Default.LocalDrink,
                    contentDescription = if (hasVolume) "배뇨량 수정" else "배뇨량 입력",
                    active = hasVolume,
                    onClick = onOpenVolume
                )
                Spacer(modifier = Modifier.weight(1f))
                EventActionButton(
                    palette = palette,
                    icon = Icons.Default.Delete,
                    contentDescription = "기록 삭제",
                    active = false,
                    onClick = onDelete
                )
            }
        }
    }
}

@Composable
private fun SmallBadge(
    text: String,
    containerColor: Color,
    contentColor: Color
) {
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(9.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                lineHeight = 12.sp
            ),
            color = contentColor,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun EventActionButton(
    palette: HomePalette,
    icon: ImageVector,
    contentDescription: String,
    active: Boolean,
    onClick: () -> Unit
) {
    val background = if (active) palette.surfaceTint else palette.surfaceMuted
    val tint = if (active) palette.primaryStrong else palette.iconTint

    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(background)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun InlineNotice(
    palette: HomePalette,
    text: String
) {
    Surface(
        color = palette.warningBackground,
        shape = RoundedCornerShape(20.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = palette.warningText,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
        )
    }
}

@Composable
private fun RecordsEmptyState(
    palette: HomePalette,
    selectedDate: LocalDate,
    today: LocalDate
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = palette.surfaceStrong,
        shape = RoundedCornerShape(28.dp),
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = if (selectedDate == today) "아직 오늘 기록이 없습니다." else "선택한 날짜의 기록이 없습니다.",
                style = MaterialTheme.typography.titleLarge,
                color = palette.titleText,
                textAlign = TextAlign.Center
            )
            Text(
                text = "하단의 빠른 액션 버튼으로 지금 바로 기록을 추가해보세요.",
                style = MaterialTheme.typography.bodyMedium,
                color = palette.mutedText,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun List<VoidingEvent>.toAverageIntervalMillis(): Long? {
    val intervals = zipWithNext { current, next ->
        current.voidedAtEpochMs - next.voidedAtEpochMs
    }.filter { it > 0 }

    return if (intervals.isEmpty()) {
        null
    } else {
        intervals.sum() / intervals.size
    }
}

private fun urgencyBadgeColors(level: Int): Pair<Color, Color> {
    return when (level) {
        1 -> Color(0xFFE7F4F0) to Color(0xFF2D6B5B)
        2 -> Color(0xFFE8F1F1) to Color(0xFF416464)
        3 -> Color(0xFFDDEDED) to Color(0xFF006767)
        4 -> Color(0xFFFFE6D6) to Color(0xFFB46800)
        else -> Color(0xFFFFDED6) to Color(0xFFB2453B)
    }
}
