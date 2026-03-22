package com.bladderdiary.app.presentation.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bladderdiary.app.domain.model.VoidingEvent
import com.bladderdiary.app.ui.theme.appExtraColors
import kotlinx.datetime.LocalDate

@Composable
internal fun MainContent(
    state: MainUiState,
    today: LocalDate,
    palette: HomePalette,
    modifier: Modifier = Modifier,
    isPinSet: Boolean,
    isE2eeEnabled: Boolean,
    isE2eeChecking: Boolean,
    menuExpanded: Boolean,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onPickDate: () -> Unit,
    onOpenMenu: () -> Unit,
    onDismissMenu: () -> Unit,
    onTogglePin: () -> Unit,
    onOpenE2eeSettings: () -> Unit,
    onOpenPdfExport: () -> Unit,
    isExportingPdf: Boolean,
    onSignOut: () -> Unit,
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
    val syncSummary = remember(state.pendingSyncCount, state.pendingSyncError, state.isSyncing) {
        state.toSyncSummary()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        HeroDateCard(
            palette = palette,
            selectedDate = state.selectedDate,
            onPreviousDay = onPreviousDay,
            onNextDay = onNextDay,
            onPickDate = onPickDate
        )

        DailySummaryCard(
            palette = palette,
            dailyVolumeMl = state.dailyVolumeMl ?: 0,
            dailyCount = state.dailyCount,
            averageIntervalMillis = averageIntervalMillis,
            syncSummary = syncSummary
        )

        if (state.pendingSyncCount > 0) {
            InlineNotice(
                text = state.pendingSyncError?.let { rawError ->
                    if (rawError.isLikelyOfflineSyncError()) {
                        "오프라인 상태입니다. 연결되면 자동으로 동기화됩니다."
                    } else {
                        "동기화 오류: ${rawError.toUiErrorText()}"
                    }
                } ?: "기록이 로컬에 보관되어 있으며 연결되면 자동으로 동기화됩니다.",
                containerColor = MaterialTheme.appExtraColors.warningContainer,
                contentColor = MaterialTheme.appExtraColors.onWarningContainer
            )
        }

        RecordsPanel(
            palette = palette,
            events = sortedEvents,
            modifier = Modifier.weight(1f),
            selectedDate = state.selectedDate,
            today = today,
            isPinSet = isPinSet,
            isE2eeEnabled = isE2eeEnabled,
            isE2eeChecking = isE2eeChecking,
            menuExpanded = menuExpanded,
            onOpenMenu = onOpenMenu,
            onDismissMenu = onDismissMenu,
            onTogglePin = onTogglePin,
            onOpenE2eeSettings = onOpenE2eeSettings,
            onOpenPdfExport = onOpenPdfExport,
            isExportingPdf = isExportingPdf,
            onSignOut = onSignOut,
            onOpenMemo = onOpenMemo,
            onOpenVolume = onOpenVolume,
            onDeleteEvent = onDeleteEvent
        )
    }
}

@Composable
private fun MainOverflowMenu(
    palette: HomePalette,
    isPinSet: Boolean,
    isE2eeEnabled: Boolean,
    isE2eeChecking: Boolean,
    menuExpanded: Boolean,
    onOpenMenu: () -> Unit,
    onDismissMenu: () -> Unit,
    onTogglePin: () -> Unit,
    onOpenE2eeSettings: () -> Unit,
    onOpenPdfExport: () -> Unit,
    isExportingPdf: Boolean,
    onSignOut: () -> Unit
) {
    Box {
        GlassIconButton(
            palette = palette,
            icon = Icons.Default.Settings,
            contentDescription = "전체 설정",
            buttonSize = 34.dp,
            iconSize = 16.dp,
            cornerRadius = 13.dp,
            onClick = onOpenMenu
        )

        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = onDismissMenu
        ) {
            DropdownMenuItem(
                text = { Text(if (isE2eeEnabled) "메모 암호화 관리" else "메모 암호화 설정") },
                leadingIcon = {
                    Icon(
                        imageVector = if (isE2eeEnabled) Icons.Filled.VpnKey else Icons.Outlined.VpnKey,
                        contentDescription = null
                    )
                },
                onClick = onOpenE2eeSettings,
                enabled = !isE2eeChecking
            )
            DropdownMenuItem(
                text = { Text(if (isExportingPdf) "PDF 생성 중" else "PDF 내보내기") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = null
                    )
                },
                onClick = onOpenPdfExport,
                enabled = !isExportingPdf
            )
            DropdownMenuItem(
                text = { Text(if (isPinSet) "PIN 해제" else "PIN 설정") },
                leadingIcon = {
                    Icon(
                        imageVector = if (isPinSet) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = null
                    )
                },
                onClick = onTogglePin
            )
            DropdownMenuItem(
                text = { Text("로그아웃") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = null
                    )
                },
                onClick = onSignOut
            )
        }
    }
}

@Composable
private fun HeroDateCard(
    palette: HomePalette,
    selectedDate: LocalDate,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onPickDate: () -> Unit
) {
    val outerShape = RoundedCornerShape(26.dp)
    val pillShape = RoundedCornerShape(16.dp)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = palette.glassPanel,
        contentColor = palette.titleText,
        shape = outerShape,
        border = BorderStroke(1.dp, palette.glassBorder),
        shadowElevation = 14.dp
    ) {
        ProvideFixedFontScale {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassIconButton(
                    palette = palette,
                    icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "이전 날짜",
                    buttonSize = 38.dp,
                    iconSize = 16.dp,
                    cornerRadius = 14.dp,
                    onClick = onPreviousDay
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(pillShape)
                        .background(
                            brush = Brush.verticalGradient(
                                listOf(palette.datePillStart, palette.datePillEnd)
                            )
                        )
                        .border(
                            width = 1.dp,
                            color = palette.datePillBorder,
                            shape = pillShape
                        )
                        .clickable(onClick = onPickDate)
                        .padding(horizontal = 14.dp, vertical = 9.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        Text(
                            text = "Bladder Diary",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.mutedText,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.9.sp
                        )
                        Text(
                            text = selectedDate.toHeroDateText(),
                            style = MaterialTheme.typography.titleLarge,
                            color = palette.titleText,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                GlassIconButton(
                    palette = palette,
                    icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "다음 날짜",
                    buttonSize = 38.dp,
                    iconSize = 16.dp,
                    cornerRadius = 14.dp,
                    onClick = onNextDay
                )
            }
        }
    }
}

@Composable
private fun DailySummaryCard(
    palette: HomePalette,
    dailyVolumeMl: Int,
    dailyCount: Int,
    averageIntervalMillis: Long?,
    syncSummary: SyncSummary
) {
    val shape = RoundedCornerShape(24.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 16.dp, shape = shape)
            .clip(shape)
            .background(
                brush = Brush.verticalGradient(
                    listOf(palette.summaryStart, palette.summaryEnd)
                )
            )
            .border(1.dp, palette.summaryBorder, shape)
            .padding(horizontal = 10.dp, vertical = 9.dp)
    ) {
        ProvideFixedFontScale {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Daily Summary",
                    style = MaterialTheme.typography.titleSmall,
                    color = palette.titleText,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SummaryMetricCard(
                        palette = palette,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        label = "총배뇨량",
                        value = dailyVolumeMl.toString(),
                        unit = "mL"
                    )
                    SummaryMetricCard(
                        palette = palette,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        label = "배뇨횟수",
                        value = dailyCount.toString(),
                        unit = "회"
                    )
                    SummaryMetricCard(
                        palette = palette,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        label = "평균간격",
                        value = averageIntervalMillis.toMetricValue(),
                        unit = averageIntervalMillis?.let { "h" }
                    )
                    SummaryMetricCard(
                        palette = palette,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        label = "동기화",
                        value = syncSummary.value,
                        unit = null
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryMetricCard(
    palette: HomePalette,
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    unit: String?
) {
    val shape = RoundedCornerShape(14.dp)
    val valueFontSize = when {
        value.length >= 5 -> 16.sp
        value.length >= 3 -> 18.sp
        else -> 20.sp
    }

    Box(
        modifier = modifier
            .height(72.dp)
            .clip(shape)
            .background(palette.metricPanel)
            .border(1.dp, palette.metricBorder, shape)
            .padding(horizontal = 8.dp, vertical = 7.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = palette.metricLabelText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = value,
                    color = palette.metricValueText,
                    fontSize = valueFontSize,
                    lineHeight = valueFontSize,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
                unit?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.metricSubText,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Clip,
                        modifier = Modifier.padding(bottom = 1.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun RecordsPanel(
    palette: HomePalette,
    events: List<VoidingEvent>,
    modifier: Modifier = Modifier,
    selectedDate: LocalDate,
    today: LocalDate,
    isPinSet: Boolean,
    isE2eeEnabled: Boolean,
    isE2eeChecking: Boolean,
    menuExpanded: Boolean,
    onOpenMenu: () -> Unit,
    onDismissMenu: () -> Unit,
    onTogglePin: () -> Unit,
    onOpenE2eeSettings: () -> Unit,
    onOpenPdfExport: () -> Unit,
    isExportingPdf: Boolean,
    onSignOut: () -> Unit,
    onOpenMemo: (VoidingEvent) -> Unit,
    onOpenVolume: (VoidingEvent) -> Unit,
    onDeleteEvent: (String) -> Unit
) {
    val outerShape = RoundedCornerShape(30.dp)
    val innerShape = RoundedCornerShape(20.dp)

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = palette.glassPanel,
        contentColor = palette.titleText,
        shape = outerShape,
        border = BorderStroke(1.dp, palette.glassBorder),
        shadowElevation = 16.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (selectedDate == today) "오늘의 기록" else "선택한 날짜의 기록",
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.titleText,
                    fontWeight = FontWeight.Bold
                )

                MainOverflowMenu(
                    palette = palette,
                    isPinSet = isPinSet,
                    isE2eeEnabled = isE2eeEnabled,
                    isE2eeChecking = isE2eeChecking,
                    menuExpanded = menuExpanded,
                    onOpenMenu = onOpenMenu,
                    onDismissMenu = onDismissMenu,
                    onTogglePin = onTogglePin,
                    onOpenE2eeSettings = onOpenE2eeSettings,
                    onOpenPdfExport = onOpenPdfExport,
                    isExportingPdf = isExportingPdf,
                    onSignOut = onSignOut
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(innerShape)
                    .background(palette.tableBackground)
                    .border(1.dp, palette.tableBorder, innerShape)
            ) {
                ProvideFixedFontScale {
                    Column(modifier = Modifier.fillMaxSize()) {
                        RecordsTableHeader(palette = palette)

                        if (events.isEmpty()) {
                            RecordsEmptyState(
                                palette = palette,
                                selectedDate = selectedDate,
                                today = today
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 10.dp)
                            ) {
                                itemsIndexed(events, key = { _, item -> item.localId }) { index, event ->
                                    val previousEvent = events.getOrNull(index + 1)
                                    val intervalText = previousEvent
                                        ?.let { event.voidedAtEpochMs - it.voidedAtEpochMs }
                                        ?.takeIf { it > 0 }
                                        ?.toIntervalText()
                                        ?: "-"

                                    EventRow(
                                        palette = palette,
                                        event = event,
                                        intervalText = intervalText,
                                        onOpenMemo = { onOpenMemo(event) },
                                        onEditVolume = { onOpenVolume(event) },
                                        onDelete = { onDeleteEvent(event.localId) }
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
private fun RecordsTableHeader(palette: HomePalette) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.tableHeaderBackground)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "시간",
            style = MaterialTheme.typography.labelSmall,
            color = palette.tableHeaderText,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1.35f)
        )
        Text(
            text = "간격",
            style = MaterialTheme.typography.labelSmall,
            color = palette.tableHeaderText,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1.05f)
        )
        Text(
            text = "배뇨량",
            style = MaterialTheme.typography.labelSmall,
            color = palette.tableHeaderText,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(0.72f)
        )
        Text(
            text = "관리",
            style = MaterialTheme.typography.labelSmall,
            color = palette.tableHeaderText,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(0.48f)
        )
    }
}

@Composable
private fun RecordsEmptyState(
    palette: HomePalette,
    selectedDate: LocalDate,
    today: LocalDate
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (selectedDate == today) {
                "아직 오늘 기록이 없습니다."
            } else {
                "선택한 날짜의 기록이 없습니다."
            },
            style = MaterialTheme.typography.titleSmall,
            color = palette.titleText,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "하단의 '지금 기록' 또는 '시간 지정'으로 빠르게 입력하실 수 있습니다.",
            style = MaterialTheme.typography.bodyMedium,
            color = palette.mutedText,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun EventRow(
    palette: HomePalette,
    event: VoidingEvent,
    intervalText: String,
    onOpenMemo: () -> Unit,
    onEditVolume: () -> Unit,
    onDelete: () -> Unit
) {
    val (timeText, periodText) = event.voidedAtEpochMs.toTimeDisplay()
    val hasVolume = event.volumeMl != null
    val hasMemo = !event.memo.isNullOrBlank()
    val timeLabel = "$periodText $timeText"
    var actionsExpanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 11.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1.35f),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = timeLabel,
                    color = palette.titleText,
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (hasMemo) {
                    MemoIndicatorButton(
                        tint = palette.actionIconTint,
                        onClick = onOpenMemo
                    )
                }
            }

            Text(
                text = intervalText,
                color = if (intervalText == "-") palette.mutedText else palette.bodyText,
                fontSize = 14.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1.05f)
            )

            Text(
                text = event.volumeMl?.toVolumeLabel() ?: "-",
                color = if (hasVolume) palette.volumeText else palette.mutedText,
                fontSize = 14.sp,
                lineHeight = 18.sp,
                fontWeight = if (hasVolume) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(0.72f)
            )

            Box(
                modifier = Modifier.weight(0.48f),
                contentAlignment = Alignment.CenterEnd
            ) {
                TinyActionButton(
                    palette = palette,
                    icon = Icons.Default.MoreVert,
                    contentDescription = "기록 관리",
                    buttonSize = 28.dp,
                    iconSize = 16.dp,
                    onClick = { actionsExpanded = true }
                )

                DropdownMenu(
                    expanded = actionsExpanded,
                    onDismissRequest = { actionsExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(if (hasMemo) "메모 편집" else "메모 입력") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            actionsExpanded = false
                            onOpenMemo()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("배뇨량 입력") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.LocalDrink,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            actionsExpanded = false
                            onEditVolume()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("기록 삭제") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            actionsExpanded = false
                            onDelete()
                        }
                    )
                }
            }
        }

        androidx.compose.material3.HorizontalDivider(
            color = palette.rowDivider,
            thickness = 1.dp
        )
    }
}

@Composable
private fun InlineNotice(
    text: String,
    containerColor: Color,
    contentColor: Color
) {
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
        )
    }
}

private data class SyncSummary(
    val value: String,
    val sub: String
)

private fun MainUiState.toSyncSummary(): SyncSummary {
    return when {
        isSyncing -> SyncSummary(value = "동기화", sub = "진행")
        pendingSyncError != null && pendingSyncCount > 0 -> SyncSummary(value = "주의", sub = "${pendingSyncCount}건")
        pendingSyncCount > 0 -> SyncSummary(value = "대기", sub = "${pendingSyncCount}건")
        else -> SyncSummary(value = "정상", sub = "완료")
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
