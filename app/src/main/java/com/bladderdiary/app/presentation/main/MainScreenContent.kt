package com.bladderdiary.app.presentation.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bladderdiary.app.domain.model.VoidingEvent
import kotlinx.datetime.LocalDate
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
internal fun MainContent(
    state: MainUiState,
    today: LocalDate,
    palette: HomePalette,
    modifier: Modifier = Modifier,
    isAddActionEnabled: Boolean,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onPickDate: () -> Unit,
    onAddEvent: () -> Unit,
    onEditEvent: (VoidingEvent) -> Unit,
    onDeleteEvent: (String) -> Unit
) {
    val isCompactWidth = LocalConfiguration.current.screenWidthDp <= 390

    var previousDate by remember { mutableStateOf(state.selectedDate) }
    val slideDirection = remember(state.selectedDate) {
        val direction = when {
            state.selectedDate > previousDate -> 1
            state.selectedDate < previousDate -> -1
            else -> 0
        }
        previousDate = state.selectedDate
        direction
    }

    Column(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                HeroDateCard(
                    palette = palette,
                    selectedDate = state.selectedDate,
                    today = today,
                    compact = isCompactWidth,
                    onPreviousDay = onPreviousDay,
                    onNextDay = onNextDay,
                    onPickDate = onPickDate
                )
            }

            item {
                AnimatedContent(
                    targetState = state.selectedDate,
                    transitionSpec = {
                        val direction = slideDirection
                        (slideInHorizontally { fullWidth -> direction * fullWidth } + fadeIn())
                            .togetherWith(
                                slideOutHorizontally { fullWidth -> -direction * fullWidth } + fadeOut()
                            ).using(SizeTransform(clip = false))
                    },
                    label = "dateSummarySlide"
                ) { _ ->
                    SummarySection(
                        palette = palette,
                        dailyVolumeMl = state.dailyVolumeMl ?: 0,
                        dailyCount = state.dailyCount,
                        averageIntervalMillis = remember(state.events) {
                            state.events.sortedByDescending { it.voidedAtEpochMs }
                                .toAverageIntervalMillis()
                        },
                        compact = isCompactWidth
                    )
                }
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
                    palette = palette,
                    action = {
                        RecordAddActionButton(
                            palette = palette,
                            compact = isCompactWidth,
                            enabled = isAddActionEnabled,
                            onClick = onAddEvent
                        )
                    }
                )
            }

            item {
                val sortedEvents = remember(state.events) {
                    state.events.sortedByDescending { it.voidedAtEpochMs }
                }

                AnimatedContent(
                    targetState = state.selectedDate,
                    transitionSpec = {
                        val direction = slideDirection
                        (slideInHorizontally { fullWidth -> direction * fullWidth } + fadeIn())
                            .togetherWith(
                                slideOutHorizontally { fullWidth -> -direction * fullWidth } + fadeOut()
                            ).using(SizeTransform(clip = false))
                    },
                    label = "dateRecordsSlide"
                ) { _ ->
                    if (sortedEvents.isEmpty()) {
                        RecordsEmptyState(
                            palette = palette,
                            selectedDate = state.selectedDate,
                            today = today
                        )
                    } else {
                        AnimatedTimelineEvents(
                            palette = palette,
                            selectedDate = state.selectedDate,
                            events = sortedEvents,
                            onEditEvent = onEditEvent,
                            onDeleteEvent = onDeleteEvent
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroDateCard(
    palette: HomePalette,
    selectedDate: LocalDate,
    today: LocalDate,
    compact: Boolean,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onPickDate: () -> Unit
) {
    val buttonSize = if (compact) 34.dp else 38.dp
    val iconSize = if (compact) 16.dp else 18.dp
    val cardPaddingHorizontal = if (compact) 12.dp else 16.dp
    val cardPaddingVertical = if (compact) 10.dp else 8.dp
    val titleFontSize = if (compact) 22.sp else 26.sp
    val titleLineHeight = if (compact) 26.sp else 30.sp
    val heroDateText = if (compact) selectedDate.toCompactHeroDateText() else selectedDate.toHeroDateText()

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
                buttonSize = buttonSize,
                iconSize = iconSize,
                cornerRadius = 14.dp,
                onClick = onPreviousDay
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable(onClick = onPickDate)
                    .background(palette.surfaceMuted)
                    .padding(horizontal = cardPaddingHorizontal, vertical = cardPaddingVertical),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = heroDateText,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = titleFontSize,
                        lineHeight = titleLineHeight
                    ),
                    color = palette.titleText,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = if (compact) 2 else 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
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
                buttonSize = buttonSize,
                iconSize = iconSize,
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
    averageIntervalMillis: Long?,
    compact: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader(
            title = "오늘의 요약",
            palette = palette
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 10.dp)
        ) {
            SummaryMetricCard(
                modifier = Modifier.weight(1f),
                palette = palette,
                icon = Icons.Default.WaterDrop,
                label = "배뇨량",
                value = dailyVolumeMl.toString(),
                unit = "mL",
                compact = compact
            )
            SummaryMetricCard(
                modifier = Modifier.weight(1f),
                palette = palette,
                icon = Icons.AutoMirrored.Filled.FormatListBulleted,
                label = "횟수",
                value = dailyCount.toString(),
                unit = "회",
                compact = compact
            )
            SummaryMetricCard(
                modifier = Modifier.weight(1f),
                palette = palette,
                icon = Icons.Default.Schedule,
                label = "평균 간격",
                value = averageIntervalMillis.toMetricValue(),
                unit = null,
                compact = compact
            )
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    supporting: String? = null,
    palette: HomePalette,
    action: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 18.sp,
                    lineHeight = 22.sp
                ),
                color = palette.titleText,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
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
        action?.invoke()
    }
}

@Composable
private fun RecordAddActionButton(
    palette: HomePalette,
    compact: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)

    Surface(
        color = palette.surfaceStrong,
        shape = shape,
        shadowElevation = 6.dp,
        modifier = Modifier.padding(start = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .clip(shape)
                .clickable(enabled = enabled, onClick = onClick)
                .background(if (enabled) palette.surfaceTint else palette.surfaceMuted)
                .padding(horizontal = if (compact) 10.dp else 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = if (enabled) palette.primaryStrong else palette.subtleText,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "기록 추가",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = if (compact) 11.sp else 13.sp,
                    lineHeight = if (compact) 14.sp else 18.sp
                ),
                color = if (enabled) palette.primaryStrong else palette.subtleText,
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
    unit: String?,
    compact: Boolean = false
) {
    val labelFontSize = when {
        compact && label.length >= 5 -> 7.5.sp
        compact -> 8.5.sp
        label.length >= 5 -> 9.sp
        else -> 10.sp
    }
    val labelLineHeight = when {
        compact && label.length >= 5 -> 9.sp
        compact -> 10.sp
        label.length >= 5 -> 11.sp
        else -> 12.sp
    }
    val valueFontSize = when {
        compact && value.length >= 5 -> 15.sp
        compact && value.length >= 3 -> 17.sp
        compact -> 19.sp
        value.length >= 5 -> 18.sp
        value.length >= 3 -> 20.sp
        else -> 22.sp
    }
    val valueLineHeight = when {
        compact && value.length >= 5 -> 17.sp
        compact && value.length >= 3 -> 19.sp
        compact -> 21.sp
        value.length >= 5 -> 20.sp
        value.length >= 3 -> 22.sp
        else -> 24.sp
    }
    val iconContainerSize = if (compact) 20.dp else 24.dp
    val iconSize = if (compact) 11.dp else 13.dp
    val contentPaddingHorizontal = if (compact) 8.dp else 12.dp
    val contentPaddingVertical = if (compact) 8.dp else 10.dp

    Surface(
        modifier = modifier,
        color = palette.surfaceStrong,
        shape = RoundedCornerShape(if (compact) 20.dp else 22.dp),
        shadowElevation = if (compact) 6.dp else 8.dp
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = contentPaddingHorizontal,
                vertical = contentPaddingVertical
            ),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(iconContainerSize)
                        .clip(CircleShape)
                        .background(palette.surfaceTint),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = palette.primaryStrong,
                        modifier = Modifier.size(iconSize)
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
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
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
                            fontSize = if (compact) 8.5.sp else 10.sp,
                            lineHeight = if (compact) 10.sp else 12.sp
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
    onEdit: () -> Unit,
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
            onEdit = onEdit,
            onDelete = onDelete
        )
    }
}

@Composable
private fun AnimatedTimelineEvents(
    palette: HomePalette,
    selectedDate: LocalDate,
    events: List<VoidingEvent>,
    onEditEvent: (VoidingEvent) -> Unit,
    onDeleteEvent: (String) -> Unit
) {
    val renderedIds = remember(selectedDate) { mutableStateListOf<String>() }
    val renderedEvents = remember(selectedDate) { mutableStateMapOf<String, VoidingEvent>() }
    val coroutineScope = rememberCoroutineScope()
    val visibilityStates = remember(selectedDate) {
        mutableMapOf<String, MutableTransitionState<Boolean>>()
    }
    var hasInitialized by remember(selectedDate) { mutableStateOf(false) }

    LaunchedEffect(events) {
        val targetIds = events.map { it.localId }
        val targetIdSet = targetIds.toSet()
        val currentIds = renderedIds.toList()

        events.forEach { event ->
            renderedEvents[event.localId] = event
            val state = visibilityStates[event.localId]
            if (state == null) {
                visibilityStates[event.localId] =
                    MutableTransitionState(!hasInitialized).apply { targetState = true }
                if (event.localId !in renderedIds) {
                    renderedIds += event.localId
                }
            } else {
                state.targetState = true
            }
        }

        val exitingIdsWithIndex = currentIds.withIndex()
            .filter { (_, id) -> id !in targetIdSet }

        val reorderedIds = targetIds.toMutableList()
        exitingIdsWithIndex
            .sortedBy { it.index }
            .forEach { (index, id) ->
                reorderedIds.add(index.coerceAtMost(reorderedIds.size), id)
            }

        renderedIds.clear()
        renderedIds.addAll(reorderedIds)

        exitingIdsWithIndex.forEach { (_, id) ->
            visibilityStates[id]?.targetState = false
            coroutineScope.launch {
                delay(TIMELINE_EXIT_DURATION_MS)
                if (id !in targetIdSet && visibilityStates[id]?.targetState == false) {
                    renderedIds.remove(id)
                    renderedEvents.remove(id)
                    visibilityStates.remove(id)
                }
            }
        }

        hasInitialized = true
    }

    Column {
        renderedIds.forEachIndexed { index, id ->
            val event = renderedEvents[id] ?: return@forEachIndexed
            val previousEvent = nextRenderedEvent(
                renderedIds = renderedIds,
                renderedEvents = renderedEvents,
                startIndex = index + 1
            )
            val visibilityState = visibilityStates[id] ?: return@forEachIndexed

            key(id) {
                AnimatedVisibility(
                    visibleState = visibilityState,
                    enter = fadeIn(animationSpec = tween(TIMELINE_ENTER_DURATION_MS)) +
                        expandVertically(
                            animationSpec = tween(TIMELINE_ENTER_DURATION_MS),
                            expandFrom = Alignment.Top
                        ),
                    exit = fadeOut(animationSpec = tween(TIMELINE_EXIT_DURATION_MS.toInt())) +
                        shrinkVertically(
                            animationSpec = tween(TIMELINE_EXIT_DURATION_MS.toInt()),
                            shrinkTowards = Alignment.Top
                        )
                ) {
                    Column {
                        DiaryTimelineItem(
                            palette = palette,
                            event = event,
                            intervalText = previousEvent
                                ?.let { event.voidedAtEpochMs - it.voidedAtEpochMs }
                                ?.takeIf { it > 0 }
                                ?.toIntervalText(),
                            isFirst = index == 0,
                            isLast = index == renderedIds.lastIndex,
                            onEdit = { onEditEvent(event) },
                            onDelete = { onDeleteEvent(event.localId) }
                        )
                        if (index != renderedIds.lastIndex) {
                            Spacer(modifier = Modifier.height(14.dp))
                        }
                    }
                }
            }
        }
    }
}

private fun nextRenderedEvent(
    renderedIds: List<String>,
    renderedEvents: Map<String, VoidingEvent>,
    startIndex: Int
): VoidingEvent? {
    for (index in startIndex..renderedIds.lastIndex) {
        renderedEvents[renderedIds[index]]?.let { return it }
    }
    return null
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

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun DiaryEventCard(
    palette: HomePalette,
    event: VoidingEvent,
    intervalText: String?,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isCompactWidth = LocalConfiguration.current.screenWidthDp <= 390
    val (timeText, periodText) = event.voidedAtEpochMs.toTimeDisplay()
    val hasMemo = !event.memo.isNullOrBlank()
    val hasVolume = event.volumeMl != null

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
            }
            false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(28.dp))
                    .background(palette.dangerTint.copy(alpha = 0.15f))
                    .padding(end = 24.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "삭제",
                    tint = palette.dangerTint,
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .clickable(onClick = onEdit),
            color = palette.surfaceStrong,
            shape = RoundedCornerShape(28.dp),
            shadowElevation = 10.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
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

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            if (event.hasIncontinence) {
                                SmallBadge(
                                    text = "요실금",
                                    containerColor = palette.warningBackground,
                                    contentColor = palette.warningText
                                )
                            }
                            event.urgency?.let { urgency ->
                                val tone = palette.urgencyTone(urgency)
                                SmallBadge(
                                    text = "절박감 ${urgency.toUrgencyLabel()}",
                                    containerColor = tone.container,
                                    contentColor = tone.content
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
                                tint = palette.mutedText,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = intervalText?.let { "$it 간격" } ?: "이전 기록 없음",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp
                                ),
                                color = palette.bodyText
                            )
                        }
                    }

                    if (hasVolume) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = event.volumeMl.toString(),
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontSize = if (isCompactWidth) 24.sp else 28.sp,
                                    lineHeight = if (isCompactWidth) 26.sp else 30.sp
                                ),
                                color = palette.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "mL",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = if (isCompactWidth) 11.sp else 12.sp,
                                    lineHeight = if (isCompactWidth) 13.sp else 14.sp
                                ),
                                color = palette.primary,
                                modifier = Modifier.padding(bottom = if (isCompactWidth) 4.dp else 5.dp)
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
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
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
                text = "위의 기록 추가 버튼으로 지금 바로 기록을 남겨보세요.",
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

private const val TIMELINE_ENTER_DURATION_MS = 220
private const val TIMELINE_EXIT_DURATION_MS = 180L
