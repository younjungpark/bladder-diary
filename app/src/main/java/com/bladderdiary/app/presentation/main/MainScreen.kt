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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bladderdiary.app.domain.model.VoidingEvent
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

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
}
