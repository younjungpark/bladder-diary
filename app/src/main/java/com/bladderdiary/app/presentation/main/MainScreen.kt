package com.bladderdiary.app.presentation.main

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.text.format.DateFormat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bladderdiary.app.domain.model.VoidingEvent
import com.bladderdiary.app.ui.theme.appExtraColors
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import java.time.Instant
import java.time.ZoneId

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    isPinSet: Boolean,
    isE2eeEnabled: Boolean,
    isE2eeChecking: Boolean,
    e2eeNoticeMessage: String?,
    onShowCalendar: () -> Unit,
    onTogglePin: () -> Unit,
    onOpenE2eeSettings: () -> Unit,
    onConsumeE2eeNotice: () -> Unit,
    onSignOut: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val palette = rememberHomePalette()
    val today = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date }

    var menuExpanded by remember { mutableStateOf(false) }
    var viewMemoEvent by remember { mutableStateOf<VoidingEvent?>(null) }
    var editMemoText by remember { mutableStateOf("") }
    var viewVolumeEvent by remember { mutableStateOf<VoidingEvent?>(null) }
    var editVolumeText by remember { mutableStateOf("") }
    var showPdfExportDialog by remember { mutableStateOf(false) }
    var pdfStartDate by remember(state.selectedDate) { mutableStateOf(state.selectedDate) }
    var pdfEndDate by remember(state.selectedDate) { mutableStateOf(state.selectedDate) }
    var pdfIncludeMemo by remember { mutableStateOf(false) }

    LaunchedEffect(state.message) {
        val msg = state.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.consumeMessage()
    }

    LaunchedEffect(e2eeNoticeMessage) {
        val msg = e2eeNoticeMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        onConsumeE2eeNotice()
    }

    LaunchedEffect(state.pendingPdfShareFile) {
        val payload = state.pendingPdfShareFile ?: return@LaunchedEffect
        val shareUri = Uri.parse(payload.uriString)
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, shareUri)
            clipData = ClipData.newRawUri(payload.fileName, shareUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooserIntent = Intent.createChooser(sendIntent, "PDF 전송").apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching {
            context.startActivity(chooserIntent)
        }.onFailure {
            snackbarHostState.showSnackbar("PDF 공유 화면을 열 수 없습니다.")
        }
        viewModel.consumePendingPdfShareFile()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        HomeBackground(
            palette = palette,
            modifier = Modifier.fillMaxSize()
        )

        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                QuickActionBar(
                    palette = palette,
                    isAdding = state.isAdding,
                    isE2eeChecking = isE2eeChecking,
                    onAddNow = { viewModel.addNow(null) },
                    onAddAtTime = {
                        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                        val isToday = state.selectedDate == now.date
                        val initialHour = if (isToday) now.hour else 12
                        val initialMinute = if (isToday) now.minute else 0
                        TimePickerDialog(
                            context,
                            { _, hourOfDay, minute ->
                                viewModel.addAtSelectedTime(hourOfDay, minute, null)
                            },
                            initialHour,
                            initialMinute,
                            DateFormat.is24HourFormat(context)
                        ).show()
                    }
                )
            }
        ) { padding ->
            MainContent(
                state = state,
                today = today,
                palette = palette,
                modifier = Modifier.padding(padding),
                isPinSet = isPinSet,
                isE2eeEnabled = isE2eeEnabled,
                isE2eeChecking = isE2eeChecking,
                menuExpanded = menuExpanded,
                onPreviousDay = viewModel::goPreviousDay,
                onNextDay = viewModel::goNextDay,
                onPickDate = onShowCalendar,
                onOpenMenu = { menuExpanded = true },
                onDismissMenu = { menuExpanded = false },
                onTogglePin = {
                    menuExpanded = false
                    onTogglePin()
                },
                onOpenE2eeSettings = {
                    menuExpanded = false
                    onOpenE2eeSettings()
                },
                onOpenPdfExport = {
                    menuExpanded = false
                    pdfStartDate = state.selectedDate
                    pdfEndDate = state.selectedDate
                    pdfIncludeMemo = false
                    showPdfExportDialog = true
                },
                isExportingPdf = state.isExportingPdf,
                onSignOut = {
                    menuExpanded = false
                    onSignOut()
                },
                onOpenMemo = { event ->
                    viewMemoEvent = event
                    editMemoText = event.memo ?: ""
                },
                onOpenVolume = { event ->
                    viewVolumeEvent = event
                    editVolumeText = event.volumeMl?.toString().orEmpty()
                },
                onDeleteEvent = { viewModel.askDelete(it) }
            )
        }
    }

    MemoEditDialog(
        event = viewMemoEvent,
        editMemoText = editMemoText,
        isE2eeChecking = isE2eeChecking,
        onValueChange = { editMemoText = it },
        onDismiss = { viewMemoEvent = null },
        onDelete = {
            viewMemoEvent?.let { eventToUpdate ->
                viewModel.updateMemo(eventToUpdate.localId, null)
                viewMemoEvent = null
            }
        },
        onConfirm = {
            viewMemoEvent?.let { eventToUpdate ->
                viewModel.updateMemo(
                    eventToUpdate.localId,
                    editMemoText.trim().takeIf { text -> text.isNotEmpty() }
                )
                viewMemoEvent = null
            }
        }
    )

    VolumeEditDialog(
        event = viewVolumeEvent,
        editVolumeText = editVolumeText,
        onValueChange = { next ->
            editVolumeText = next.filter(Char::isDigit).take(4)
        },
        onDismiss = { viewVolumeEvent = null },
        onDelete = {
            viewVolumeEvent?.let { eventToUpdate ->
                viewModel.updateVolume(eventToUpdate.localId, null)
                viewVolumeEvent = null
            }
        },
        onConfirm = {
            viewVolumeEvent?.let { eventToUpdate ->
                viewModel.updateVolume(
                    eventToUpdate.localId,
                    editVolumeText.toVolumeMlOrNull()
                )
                viewVolumeEvent = null
            }
        }
    )

    DeleteDialog(
        confirmDeleteEventId = state.confirmDeleteEventId,
        onDismiss = viewModel::dismissDeleteDialog,
        onConfirm = viewModel::confirmDelete
    )

    PdfExportDialog(
        isVisible = showPdfExportDialog,
        startDate = pdfStartDate,
        endDate = pdfEndDate,
        includeMemo = pdfIncludeMemo,
        isExporting = state.isExportingPdf,
        onToggleIncludeMemo = { pdfIncludeMemo = it },
        onPickStartDate = {
            DatePickerDialog(
                context,
                { _, year, month, dayOfMonth ->
                    val selectedDate = kotlinx.datetime.LocalDate(year, month + 1, dayOfMonth)
                    pdfStartDate = selectedDate
                    if (pdfEndDate < selectedDate) {
                        pdfEndDate = selectedDate
                    }
                },
                pdfStartDate.year,
                pdfStartDate.monthNumber - 1,
                pdfStartDate.dayOfMonth
            ).show()
        },
        onPickEndDate = {
            DatePickerDialog(
                context,
                { _, year, month, dayOfMonth ->
                    pdfEndDate = kotlinx.datetime.LocalDate(year, month + 1, dayOfMonth)
                },
                pdfEndDate.year,
                pdfEndDate.monthNumber - 1,
                pdfEndDate.dayOfMonth
            ).show()
        },
        onDismiss = { showPdfExportDialog = false },
        onConfirm = {
            showPdfExportDialog = false
            viewModel.exportPdf(
                startDate = pdfStartDate,
                endDate = pdfEndDate,
                includeMemo = pdfIncludeMemo
            )
        }
    )
}

@Composable
private fun MainContent(
    state: MainUiState,
    today: kotlinx.datetime.LocalDate,
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
    selectedDate: kotlinx.datetime.LocalDate,
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
    selectedDate: kotlinx.datetime.LocalDate,
    today: kotlinx.datetime.LocalDate,
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
    selectedDate: kotlinx.datetime.LocalDate,
    today: kotlinx.datetime.LocalDate
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
private fun ProvideFixedFontScale(content: @Composable () -> Unit) {
    val density = LocalDensity.current
    val fixedDensity = remember(density.density) {
        Density(density = density.density, fontScale = 1f)
    }

    CompositionLocalProvider(LocalDensity provides fixedDensity) {
        content()
    }
}

@Composable
private fun MemoIndicatorButton(
    tint: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(20.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Description,
            contentDescription = "메모 보기",
            tint = tint,
            modifier = Modifier.size(15.dp)
        )
    }
}

@Composable
private fun QuickActionBar(
    palette: HomePalette,
    isAdding: Boolean,
    isE2eeChecking: Boolean,
    onAddNow: () -> Unit,
    onAddAtTime: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        color = palette.bottomBarBackground,
        contentColor = palette.titleText,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, palette.bottomBarBorder),
        shadowElevation = 14.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GradientActionButton(
                modifier = Modifier.weight(1.05f),
                text = if (isAdding) "저장 중" else "지금 기록",
                icon = Icons.Default.Add,
                background = Brush.verticalGradient(
                    listOf(palette.primaryButtonStart, palette.primaryButtonEnd)
                ),
                topGlow = palette.primaryButtonGlow,
                contentColor = palette.primaryButtonText,
                enabled = !isAdding && !isE2eeChecking,
                onClick = onAddNow
            )
            GradientActionButton(
                modifier = Modifier.weight(1f),
                text = "시간 지정",
                icon = Icons.Default.Edit,
                background = Brush.verticalGradient(
                    listOf(palette.secondaryButtonStart, palette.secondaryButtonEnd)
                ),
                topGlow = palette.secondaryButtonGlow,
                contentColor = palette.secondaryButtonText,
                borderColor = palette.secondaryButtonBorder,
                enabled = !isAdding && !isE2eeChecking,
                onClick = onAddAtTime
            )
        }
    }
}

@Composable
private fun GradientActionButton(
    modifier: Modifier = Modifier,
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    background: Brush,
    topGlow: Color,
    contentColor: Color,
    borderColor: Color = Color.Transparent,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(22.dp)

    Box(
        modifier = modifier
            .height(52.dp)
            .shadow(elevation = 10.dp, shape = shape)
            .clip(shape)
            .background(background)
            .drawBehind {
                drawCircle(
                    color = topGlow,
                    radius = size.width * 0.45f,
                    center = androidx.compose.ui.geometry.Offset(
                        x = size.width * 0.5f,
                        y = 0f
                    )
                )
            }
            .border(1.dp, borderColor, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor.copy(alpha = if (enabled) 1f else 0.45f),
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = text,
                color = contentColor.copy(alpha = if (enabled) 1f else 0.45f),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun GlassIconButton(
    palette: HomePalette,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    buttonSize: androidx.compose.ui.unit.Dp = 44.dp,
    iconSize: androidx.compose.ui.unit.Dp = 18.dp,
    cornerRadius: androidx.compose.ui.unit.Dp = 16.dp,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(buttonSize)
            .clip(RoundedCornerShape(cornerRadius))
            .background(palette.iconButtonBackground)
            .border(1.dp, palette.iconButtonBorder, RoundedCornerShape(cornerRadius))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = palette.iconTint,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
private fun TinyActionButton(
    palette: HomePalette,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    buttonSize: androidx.compose.ui.unit.Dp = 24.dp,
    iconSize: androidx.compose.ui.unit.Dp = 12.dp,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(buttonSize)
            .clip(RoundedCornerShape(9.dp))
            .background(palette.miniButtonBackground)
            .border(1.dp, palette.miniButtonBorder, RoundedCornerShape(9.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = palette.actionIconTint,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
private fun HomeBackground(
    palette: HomePalette,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                brush = Brush.verticalGradient(
                    listOf(palette.backgroundTop, palette.backgroundBottom)
                )
            )
            .drawBehind {
                drawCircle(
                    color = palette.backgroundGlowPrimary,
                    radius = size.minDimension * 0.26f,
                    center = androidx.compose.ui.geometry.Offset(
                        x = size.width * 0.15f,
                        y = size.height * 0.18f
                    )
                )
                drawCircle(
                    color = palette.backgroundGlowSecondary,
                    radius = size.minDimension * 0.2f,
                    center = androidx.compose.ui.geometry.Offset(
                        x = size.width * 0.82f,
                        y = size.height * 0.12f
                    )
                )
                drawCircle(
                    color = palette.backgroundGlowTertiary,
                    radius = size.minDimension * 0.28f,
                    center = androidx.compose.ui.geometry.Offset(
                        x = size.width * 0.5f,
                        y = size.height
                    )
                )
            }
    )
}

@Composable
private fun PdfExportDialog(
    isVisible: Boolean,
    startDate: kotlinx.datetime.LocalDate,
    endDate: kotlinx.datetime.LocalDate,
    includeMemo: Boolean,
    isExporting: Boolean,
    onToggleIncludeMemo: (Boolean) -> Unit,
    onPickStartDate: () -> Unit,
    onPickEndDate: () -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    if (!isVisible) return
    val isValidRange = startDate <= endDate

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("PDF 내보내기") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onPickStartDate,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("시작일: ${startDate.toKoreanShortDate()}")
                }
                OutlinedButton(
                    onClick = onPickEndDate,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("종료일: ${endDate.toKoreanShortDate()}")
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Checkbox(
                        checked = includeMemo,
                        onCheckedChange = onToggleIncludeMemo
                    )
                    Column {
                        Text(
                            text = "메모 포함",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "기본값은 제외이며, 필요할 때만 메모를 PDF에 포함합니다.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (!isValidRange) {
                    Text(
                        text = "종료일은 시작일보다 빠를 수 없습니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = isValidRange && !isExporting
            ) {
                Text(if (isExporting) "생성 중" else "내보내기")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isExporting
            ) {
                Text("취소")
            }
        }
    )
}

@Composable
private fun MemoEditDialog(
    event: VoidingEvent?,
    editMemoText: String,
    isE2eeChecking: Boolean,
    onValueChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onConfirm: () -> Unit
) {
    if (event == null) return
    val showMemoLabel = editMemoText.isNotBlank()
    val hasMemo = !event.memo.isNullOrBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("메모 조회 및 수정") },
        text = {
            OutlinedTextField(
                value = editMemoText,
                onValueChange = onValueChange,
                label = if (showMemoLabel) {
                    { Text("메모") }
                } else {
                    null
                },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = !isE2eeChecking
            ) {
                Text("저장")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (hasMemo) {
                    TextButton(
                        onClick = onDelete,
                        enabled = !isE2eeChecking
                    ) {
                        Text("삭제")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("닫기")
                }
            }
        }
    )
}

@Composable
private fun VolumeEditDialog(
    event: VoidingEvent?,
    editVolumeText: String,
    onValueChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onConfirm: () -> Unit
) {
    if (event == null) return
    val hasInput = editVolumeText.isNotBlank()
    val isValidInput = editVolumeText.isBlank() || editVolumeText.toVolumeMlOrNull() != null
    val hasSavedVolume = event.volumeMl != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("배뇨량 입력") },
        text = {
            OutlinedTextField(
                value = editVolumeText,
                onValueChange = onValueChange,
                label = if (hasInput) {
                    { Text("배뇨량 (mL)") }
                } else {
                    null
                },
                placeholder = { Text("예: 250") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = !isValidInput
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = isValidInput
            ) {
                Text("저장")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (hasSavedVolume) {
                    TextButton(onClick = onDelete) {
                        Text("삭제")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("닫기")
                }
            }
        }
    )
}

@Composable
private fun DeleteDialog(
    confirmDeleteEventId: String?,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    if (confirmDeleteEventId == null) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("기록 삭제") },
        text = { Text("이 기록을 삭제할까요?") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("삭제")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
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

private fun Long?.toMetricValue(): String {
    if (this == null) return "-"
    val totalMinutes = this / (1000 * 60)
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return "${hours}:${minutes.toString().padStart(2, '0')}"
}

private fun kotlinx.datetime.LocalDate.toHeroDateText(): String {
    return "${monthNumber}월 ${dayOfMonth}일 ${dayOfWeek.toKoreanLabel()}"
}

private fun kotlinx.datetime.DayOfWeek.toKoreanLabel(): String {
    return when (value) {
        1 -> "월요일"
        2 -> "화요일"
        3 -> "수요일"
        4 -> "목요일"
        5 -> "금요일"
        6 -> "토요일"
        else -> "일요일"
    }
}

private fun kotlinx.datetime.LocalDate.toKoreanShortDate(): String {
    return "${year}년 ${monthNumber}월 ${dayOfMonth}일"
}

private fun kotlinx.datetime.LocalDate.plusDays(days: Int): kotlinx.datetime.LocalDate {
    return plus(DatePeriod(days = days))
}

private fun Long.toTimeDisplay(): Pair<String, String> {
    val localTime = Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .toLocalTime()

    val hour = localTime.hour
    val minute = localTime.minute
    val meridiem = if (hour < 12) "오전" else "오후"
    val displayHour = when (val normalized = hour % 12) {
        0 -> 12
        else -> normalized
    }

    return displayHour.toString().padStart(2, '0') + ":" + minute.toString().padStart(2, '0') to meridiem
}

private fun Long.toIntervalText(): String {
    val minutes = this / (1000 * 60)
    val hours = minutes / 60
    val remainingMinutes = minutes % 60
    return if (hours > 0) {
        if (remainingMinutes == 0L) {
            "${hours}시간"
        } else {
            "${hours}시간 ${remainingMinutes}분"
        }
    } else {
        "${remainingMinutes}분"
    }
}

private fun String.toUiErrorText(maxLen: Int = 120): String {
    val normalized = replace('\n', ' ').replace('\r', ' ').trim()
    return if (normalized.length <= maxLen) normalized else normalized.take(maxLen) + "..."
}

private fun String.isLikelyOfflineSyncError(): Boolean {
    val normalized = lowercase()
    return OFFLINE_SYNC_ERROR_PATTERNS.any(normalized::contains)
}

private val OFFLINE_SYNC_ERROR_PATTERNS = listOf(
    "unable to resolve host",
    "failed to connect",
    "network is unreachable",
    "no address associated with hostname",
    "no route to host",
    "software caused connection abort",
    "connection reset"
)

private fun String.toVolumeMlOrNull(): Int? {
    if (isBlank()) return null
    return toIntOrNull()?.takeIf { it > 0 }
}

private fun Int.toVolumeLabel(): String {
    return "$this mL"
}

@Composable
private fun rememberHomePalette(): HomePalette {
    return if (isSystemInDarkTheme()) {
        DarkHomePalette
    } else {
        LightHomePalette
    }
}

private data class HomePalette(
    val backgroundTop: Color,
    val backgroundBottom: Color,
    val backgroundGlowPrimary: Color,
    val backgroundGlowSecondary: Color,
    val backgroundGlowTertiary: Color,
    val glassPanel: Color,
    val glassBorder: Color,
    val titleText: Color,
    val bodyText: Color,
    val mutedText: Color,
    val accentText: Color,
    val badgeText: Color,
    val volumeText: Color,
    val rowDivider: Color,
    val iconButtonBackground: Color,
    val iconButtonBorder: Color,
    val iconTint: Color,
    val datePillStart: Color,
    val datePillEnd: Color,
    val datePillBorder: Color,
    val summaryStart: Color,
    val summaryEnd: Color,
    val summaryBorder: Color,
    val metricPanel: Color,
    val metricBorder: Color,
    val metricLabelText: Color,
    val metricValueText: Color,
    val metricSubText: Color,
    val recordsBadgeBackground: Color,
    val recordsBadgeBorder: Color,
    val tableBackground: Color,
    val tableBorder: Color,
    val tableHeaderBackground: Color,
    val tableHeaderText: Color,
    val miniButtonBackground: Color,
    val miniButtonBorder: Color,
    val actionIconTint: Color,
    val bottomBarBackground: Color,
    val bottomBarBorder: Color,
    val primaryButtonStart: Color,
    val primaryButtonEnd: Color,
    val primaryButtonGlow: Color,
    val primaryButtonText: Color,
    val secondaryButtonStart: Color,
    val secondaryButtonEnd: Color,
    val secondaryButtonGlow: Color,
    val secondaryButtonBorder: Color,
    val secondaryButtonText: Color
)

private val DarkHomePalette = HomePalette(
    backgroundTop = Color(0xFF0C1719),
    backgroundBottom = Color(0xFF061012),
    backgroundGlowPrimary = Color(0x3854E3C0),
    backgroundGlowSecondary = Color(0x423E98C4),
    backgroundGlowTertiary = Color(0x33175D56),
    glassPanel = Color(0xD6121E22),
    glassBorder = Color(0x1FA7D0CB),
    titleText = Color(0xFFEEF9F7),
    bodyText = Color(0xFFE7F4F2),
    mutedText = Color(0xFF9AB7B2),
    accentText = Color(0xFFBCE6DF),
    badgeText = Color(0xFFD9FFF1),
    volumeText = Color(0xFFD7FFF3),
    rowDivider = Color(0x0DFFFFFF),
    iconButtonBackground = Color(0x0DFFFFFF),
    iconButtonBorder = Color(0x1FA7D0CB),
    iconTint = Color(0xFFD6F9F1),
    datePillStart = Color(0x24B5F2E8),
    datePillEnd = Color(0x1091D0C5),
    datePillBorder = Color(0x26C6FBF2),
    summaryStart = Color(0xFF07797C),
    summaryEnd = Color(0xFF07535A),
    summaryBorder = Color(0x2E9EFFE8),
    metricPanel = Color(0x6B03151B),
    metricBorder = Color(0x14FFFFFF),
    metricLabelText = Color(0xC2CCF0EC),
    metricValueText = Color(0xFFEEF9F7),
    metricSubText = Color(0xADDEF4F0),
    recordsBadgeBackground = Color(0x1F53E3C0),
    recordsBadgeBorder = Color(0x3853E3C0),
    tableBackground = Color(0x08FFFFFF),
    tableBorder = Color(0x0FFFFFFF),
    tableHeaderBackground = Color(0x12C9F5EC),
    tableHeaderText = Color(0xFFB1D7D0),
    miniButtonBackground = Color(0xDB182D31),
    miniButtonBorder = Color(0x2971B7AA),
    actionIconTint = Color(0xFF79F1CB),
    bottomBarBackground = Color(0xC21D2B2F),
    bottomBarBorder = Color(0x19C0ECE7),
    primaryButtonStart = Color(0xFF8AF7CB),
    primaryButtonEnd = Color(0xFF4CE1BB),
    primaryButtonGlow = Color(0x33E3FFF1),
    primaryButtonText = Color(0xFF07352E),
    secondaryButtonStart = Color(0xFF0CA4A3),
    secondaryButtonEnd = Color(0xFF076F7D),
    secondaryButtonGlow = Color(0x1FB6FFF1),
    secondaryButtonBorder = Color(0x24B6FFF1),
    secondaryButtonText = Color(0xFFECFFFB)
)

private val LightHomePalette = HomePalette(
    backgroundTop = Color(0xFFF4FBFB),
    backgroundBottom = Color(0xFFE8F5F4),
    backgroundGlowPrimary = Color(0x426CE0C3),
    backgroundGlowSecondary = Color(0x2E4DAED7),
    backgroundGlowTertiary = Color(0x3D8DDFCB),
    glassPanel = Color(0xB8FFFFFF),
    glassBorder = Color(0x1A185A62),
    titleText = Color(0xFF18363D),
    bodyText = Color(0xFF32575D),
    mutedText = Color(0xFF6D8B90),
    accentText = Color(0xFF3F7F84),
    badgeText = Color(0xFF28656D),
    volumeText = Color(0xFF114E5B),
    rowDivider = Color(0x0F1F5F65),
    iconButtonBackground = Color(0xDBFFFFFF),
    iconButtonBorder = Color(0x1A185A62),
    iconTint = Color(0xFF2A6F78),
    datePillStart = Color(0xFAE4F7F3),
    datePillEnd = Color(0xF4D0EEEA),
    datePillBorder = Color(0x1F26767A),
    summaryStart = Color(0xF0AFEBE4),
    summaryEnd = Color(0xF07FD6CE),
    summaryBorder = Color(0x2933878B),
    metricPanel = Color(0xD1FFFFFF),
    metricBorder = Color(0x1423696D),
    metricLabelText = Color(0xFF4E7F84),
    metricValueText = Color(0xFF153B42),
    metricSubText = Color(0xFF5C8589),
    recordsBadgeBackground = Color(0x244AB9AC),
    recordsBadgeBorder = Color(0x384AB9AC),
    tableBackground = Color(0xC2FFFFFF),
    tableBorder = Color(0x141F5F65),
    tableHeaderBackground = Color(0xB2C4EBE5),
    tableHeaderText = Color(0xFF5A8085),
    miniButtonBackground = Color(0xFFF1FCFA),
    miniButtonBorder = Color(0x2948A49A),
    actionIconTint = Color(0xFF1C8D89),
    bottomBarBackground = Color(0xC7FFFFFF),
    bottomBarBorder = Color(0x17185A62),
    primaryButtonStart = Color(0xFF86EDC6),
    primaryButtonEnd = Color(0xFF57D9B8),
    primaryButtonGlow = Color(0x6BFFFFFF),
    primaryButtonText = Color(0xFF0D4D45),
    secondaryButtonStart = Color(0xFF22C0B7),
    secondaryButtonEnd = Color(0xFF118F9D),
    secondaryButtonGlow = Color(0x26E8FFFB),
    secondaryButtonBorder = Color(0x29229398),
    secondaryButtonText = Color(0xFFF5FFFD)
)
