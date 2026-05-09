package com.bladderdiary.app.presentation.main

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.text.format.DateFormat
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bladderdiary.app.domain.model.VoidingEvent
import com.bladderdiary.app.presentation.privacy.SensitiveCloudNoticeDialog
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    currentAccountLabel: String?,
    isPinSet: Boolean,
    isE2eeEnabled: Boolean,
    isE2eeChecking: Boolean,
    e2eeNoticeMessage: String?,
    isDeletingAccount: Boolean,
    accountDeletionErrorMessage: String?,
    onShowCalendar: () -> Unit,
    onTogglePin: () -> Unit,
    onOpenE2eeSettings: () -> Unit,
    onSetCloudSyncEnabled: (Boolean) -> Unit,
    onConsumeE2eeNotice: () -> Unit,
    onConsumeAccountDeletionError: () -> Unit,
    onDeleteAccount: () -> Unit,
    onSignOut: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val palette = rememberHomePalette()
    val syncStatus = state.toHomeSyncStatus(palette)
    val today = remember {
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    }

    var menuExpanded by remember { mutableStateOf(false) }
    var showEventEditorDialog by rememberSaveable { mutableStateOf(false) }
    var editingEventId by rememberSaveable { mutableStateOf<String?>(null) }
    var editorTimeText by rememberSaveable { mutableStateOf("") }
    var editorSelectedHour by rememberSaveable { mutableStateOf<Int?>(null) }
    var editorSelectedMinute by rememberSaveable { mutableStateOf<Int?>(null) }
    var editorUrgency by rememberSaveable { mutableStateOf(3) }
    var editorHasIncontinence by rememberSaveable { mutableStateOf(false) }
    var editorIsNocturia by rememberSaveable { mutableStateOf(false) }
    var editorVolumeText by rememberSaveable { mutableStateOf("") }
    var editorMemoText by rememberSaveable { mutableStateOf("") }
    var showPdfExportDialog by remember { mutableStateOf(false) }
    var pdfStartDate by remember(state.selectedDate) { mutableStateOf(state.selectedDate) }
    var pdfEndDate by remember(state.selectedDate) { mutableStateOf(state.selectedDate) }
    var pdfIncludeMemo by remember { mutableStateOf(false) }
    var showAccountDeletionDialog by rememberSaveable { mutableStateOf(false) }
    var showCloudDataNoticeDialog by rememberSaveable { mutableStateOf(false) }
    var showCloudSyncDialog by rememberSaveable { mutableStateOf(false) }

    val clearEditorState = {
        showEventEditorDialog = false
        editingEventId = null
        editorTimeText = ""
        editorSelectedHour = null
        editorSelectedMinute = null
        editorUrgency = 3
        editorHasIncontinence = false
        editorIsNocturia = false
        editorVolumeText = ""
        editorMemoText = ""
    }
    val openNewEventEditor: (RecordEditorTimeState) -> Unit = { initialTime ->
        editingEventId = null
        editorTimeText = initialTime.label
        editorSelectedHour = initialTime.hour
        editorSelectedMinute = initialTime.minute
        editorUrgency = 3
        editorHasIncontinence = false
        editorIsNocturia = false
        editorVolumeText = ""
        editorMemoText = ""
        showEventEditorDialog = true
    }
    val openExistingEventEditor: (VoidingEvent) -> Unit = { event ->
        val timeState = event.toRecordEditorTimeState()
        editingEventId = event.localId
        editorTimeText = timeState.label
        editorSelectedHour = timeState.hour
        editorSelectedMinute = timeState.minute
        editorUrgency = event.urgency ?: 3
        editorHasIncontinence = event.hasIncontinence
        editorIsNocturia = event.isNocturia
        editorVolumeText = event.volumeMl?.toString().orEmpty()
        editorMemoText = event.memo.orEmpty()
        showEventEditorDialog = true
    }

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

    LaunchedEffect(accountDeletionErrorMessage) {
        val msg = accountDeletionErrorMessage ?: return@LaunchedEffect
        showAccountDeletionDialog = false
        snackbarHostState.showSnackbar(msg)
        onConsumeAccountDeletionError()
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
            topBar = {
                MainTopBar(
                    palette = palette,
                    syncStatus = syncStatus,
                    currentAccountLabel = currentAccountLabel,
                    isPinSet = isPinSet,
                    isE2eeEnabled = isE2eeEnabled,
                    isE2eeChecking = isE2eeChecking,
                    isCloudSyncEnabled = state.isCloudSyncEnabled,
                    isCloudSyncChanging = state.isCloudSyncChanging,
                    menuExpanded = menuExpanded,
                    onShowSyncStatus = {
                        coroutineScope.launch {
                            snackbarHostState.currentSnackbarData?.dismiss()
                            snackbarHostState.showSnackbar(syncStatus.message)
                        }
                    },
                    onOpenMenu = { menuExpanded = true },
                    onDismissMenu = { menuExpanded = false },
                    onTogglePin = {
                        menuExpanded = false
                        onTogglePin()
                    },
                    onOpenCloudDataNotice = {
                        menuExpanded = false
                        showCloudDataNoticeDialog = true
                    },
                    onOpenCloudSyncSettings = {
                        menuExpanded = false
                        showCloudSyncDialog = true
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
                    isDeletingAccount = isDeletingAccount,
                    onOpenAccountDeletion = {
                        menuExpanded = false
                        showAccountDeletionDialog = true
                    },
                    onSignOut = {
                        menuExpanded = false
                        onSignOut()
                    }
                )
            }
        ) { padding ->
            MainContent(
                state = state,
                today = today,
                palette = palette,
                modifier = Modifier.padding(padding),
                isAddActionEnabled = !state.isAdding && !isE2eeChecking,
                onPreviousDay = viewModel::goPreviousDay,
                onNextDay = viewModel::goNextDay,
                onPickDate = onShowCalendar,
                onAddEvent = {
                    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                    openNewEventEditor(defaultRecordEditorTime(state.selectedDate, now))
                },
                onEditEvent = openExistingEventEditor,
                onDeleteEvent = viewModel::askDelete
            )
        }
    }

    EventEditorDialog(
        isVisible = showEventEditorDialog,
        title = if (editingEventId == null) "기록 추가" else "기록 수정",
        confirmLabel = "저장",
        timeText = editorTimeText,
        urgency = editorUrgency,
        hasIncontinence = editorHasIncontinence,
        isNocturia = editorIsNocturia,
        volumeText = editorVolumeText,
        memoText = editorMemoText,
        isE2eeChecking = isE2eeChecking,
        onUrgencyChange = { editorUrgency = it },
        onPickTime = {
            val fallbackTime = defaultRecordEditorTime(
                selectedDate = state.selectedDate,
                now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            )
            val initialHour = editorSelectedHour ?: fallbackTime.hour
            val initialMinute = editorSelectedMinute ?: fallbackTime.minute
            TimePickerDialog(
                context,
                { _, hourOfDay, minute ->
                    editorSelectedHour = hourOfDay
                    editorSelectedMinute = minute
                    editorTimeText = hourOfDay.toRecordTimeLabel(minute)
                },
                initialHour,
                initialMinute,
                DateFormat.is24HourFormat(context)
            ).show()
        },
        onIncontinenceChange = { editorHasIncontinence = it },
        onNocturiaChange = { editorIsNocturia = it },
        onVolumeChange = { next ->
            editorVolumeText = next.filter(Char::isDigit).take(4)
        },
        onMemoChange = { editorMemoText = it },
        onDismiss = clearEditorState,
        onConfirm = {
            val normalizedMemo = editorMemoText.trim().takeIf { it.isNotEmpty() }
            val normalizedVolume = editorVolumeText.toVolumeMlOrNull()
            val hour = editorSelectedHour ?: return@EventEditorDialog
            val minute = editorSelectedMinute ?: return@EventEditorDialog
            val targetEventId = editingEventId
            if (targetEventId == null) {
                viewModel.addAtSelectedTime(
                    hour = hour,
                    minute = minute,
                    urgency = editorUrgency,
                    hasIncontinence = editorHasIncontinence,
                    isNocturia = editorIsNocturia,
                    memo = normalizedMemo,
                    volumeMl = normalizedVolume
                )
            } else {
                viewModel.updateEvent(
                    localId = targetEventId,
                    hour = hour,
                    minute = minute,
                    urgency = editorUrgency,
                    hasIncontinence = editorHasIncontinence,
                    isNocturia = editorIsNocturia,
                    memo = normalizedMemo,
                    volumeMl = normalizedVolume
                )
            }
            clearEditorState()
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
                    val selectedDate = LocalDate(year, month + 1, dayOfMonth)
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
                    pdfEndDate = LocalDate(year, month + 1, dayOfMonth)
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

    AccountDeletionDialog(
        isVisible = showAccountDeletionDialog,
        isDeleting = isDeletingAccount,
        onDismiss = {
            if (!isDeletingAccount) {
                showAccountDeletionDialog = false
            }
        },
        onConfirm = onDeleteAccount
    )

    if (showCloudDataNoticeDialog) {
        SensitiveCloudNoticeDialog(
            onConfirm = { showCloudDataNoticeDialog = false },
            onDismiss = { showCloudDataNoticeDialog = false },
            confirmLabel = "확인"
        )
    }

    CloudSyncToggleDialog(
        isVisible = showCloudSyncDialog,
        isEnabled = state.isCloudSyncEnabled,
        isChanging = state.isCloudSyncChanging,
        onDismiss = {
            if (!state.isCloudSyncChanging) {
                showCloudSyncDialog = false
            }
        },
        onConfirm = {
            showCloudSyncDialog = false
            onSetCloudSyncEnabled(!state.isCloudSyncEnabled)
        }
    )
}

internal data class RecordEditorTimeState(val hour: Int, val minute: Int, val label: String)

internal fun defaultRecordEditorTime(
    selectedDate: LocalDate,
    now: LocalDateTime
): RecordEditorTimeState {
    val hour = if (selectedDate == now.date) now.hour else 12
    val minute = if (selectedDate == now.date) now.minute else 0
    return RecordEditorTimeState(
        hour = hour,
        minute = minute,
        label = hour.toRecordTimeLabel(minute)
    )
}

internal fun VoidingEvent.toRecordEditorTimeState(): RecordEditorTimeState {
    val localDateTime = Instant.fromEpochMilliseconds(voidedAtEpochMs)
        .toLocalDateTime(TimeZone.currentSystemDefault())
    return RecordEditorTimeState(
        hour = localDateTime.hour,
        minute = localDateTime.minute,
        label = voidedAtEpochMs.toRecordTimeLabel()
    )
}

private fun Long.toRecordTimeLabel(): String {
    val (timeText, periodText) = toTimeDisplay()
    return "$periodText $timeText"
}

private fun Int.toRecordTimeLabel(minute: Int): String {
    val meridiem = if (this < 12) "오전" else "오후"
    val displayHour = when (val normalized = this % 12) {
        0 -> 12
        else -> normalized
    }
    val displayMinute = minute.toString().padStart(2, '0')
    return "$meridiem ${displayHour.toString().padStart(2, '0')}:$displayMinute"
}
