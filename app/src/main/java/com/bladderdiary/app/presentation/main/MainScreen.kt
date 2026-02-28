package com.bladderdiary.app.presentation.main

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bladderdiary.app.domain.model.VoidingEvent
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onShowCalendar: () -> Unit,
    onSignOut: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var showAddDialog by remember { mutableStateOf(false) }
    var inputMemo by remember { mutableStateOf("") }
    var addTime by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    
    // 기존 메모 조회 및 수정을 위한 상태
    var viewMemoEvent by remember { mutableStateOf<VoidingEvent?>(null) }
    var editMemoText by remember { mutableStateOf("") }

    LaunchedEffect(state.message) {
        val msg = state.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.consumeMessage()
    }

    val selected = state.selectedDate
    val picker = remember(selected) {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                viewModel.setDate(kotlinx.datetime.LocalDate(year, month + 1, dayOfMonth))
            },
            selected.year,
            selected.monthNumber - 1,
            selected.dayOfMonth
        )
    }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "BladderDiary",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.primary
                ),
                actions = {
                    IconButton(onClick = onShowCalendar) {
                        Icon(
                            Icons.Default.DateRange,
                            contentDescription = "캘린더 보기",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onSignOut) {
                        Icon(
                            Icons.Default.ExitToApp,
                            contentDescription = "로그아웃",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(
                            onClick = viewModel::goPreviousDay
                        ) {
                            Text("이전", style = MaterialTheme.typography.labelLarge)
                        }
                        TextButton(
                            onClick = { picker.show() }
                        ) {
                            Text(state.selectedDate.toString(), style = MaterialTheme.typography.labelLarge)
                        }
                        TextButton(
                            onClick = viewModel::goNextDay
                        ) {
                            Text("다음", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            addTime = null
                            inputMemo = ""
                            showAddDialog = true
                        },
                        enabled = !state.isAdding,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("지금 기록", style = MaterialTheme.typography.labelMedium, maxLines = 1)
                    }
                    Button(
                        onClick = {
                            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                            val isToday = state.selectedDate == now.date
                            val initialHour = if (isToday) now.hour else 12
                            val initialMinute = if (isToday) now.minute else 0
                            TimePickerDialog(
                                context,
                                { _, hourOfDay, minute ->
                                    addTime = Pair(hourOfDay, minute)
                                    inputMemo = ""
                                    showAddDialog = true
                                },
                                initialHour,
                                initialMinute,
                                DateFormat.is24HourFormat(context)
                            ).show()
                        },
                        enabled = !state.isAdding,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text(
                            text = "시간 지정",
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Text(
                    text = "총 ${state.dailyCount}회",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (state.isSyncing) {
                    Text(
                        text = "동기화 중...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (state.pendingSyncCount > 0) {
                    Text(
                        text = "동기화 대기 ${state.pendingSyncCount}건",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    state.pendingSyncError?.let { rawError ->
                        Text(
                            text = "동기화 오류: ${rawError.toUiErrorText()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                if (state.events.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.Info,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "이날의 배뇨 기록이 없습니다.",
                                color = MaterialTheme.colorScheme.secondary,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        itemsIndexed(state.events, key = { _, it -> it.localId }) { index, event ->
                            val previousEvent = if (index < state.events.size - 1) state.events[index + 1] else null
                            val intervalMs = previousEvent?.let { event.voidedAtEpochMs - it.voidedAtEpochMs }
                            val intervalText = intervalMs?.let { ms ->
                                if (ms > 0) {
                                    val minutes = ms / (1000 * 60)
                                    val hours = minutes / 60
                                    val remainingMinutes = minutes % 60
                                    if (hours > 0) "${hours}시간 ${remainingMinutes}분 경과" else "${remainingMinutes}분 경과"
                                } else null
                            }
                            EventItem(
                                event = event,
                                intervalText = intervalText,
                                onViewMemo = { 
                                    viewMemoEvent = event
                                    editMemoText = event.memo ?: ""
                                },
                                onDelete = { viewModel.askDelete(event.localId) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = {
                Text(
                    text = if (addTime == null) "지금 기록 추가"
                           else "${addTime!!.first}시 ${addTime!!.second}분 기록 추가"
                )
            },
            text = {
                OutlinedTextField(
                    value = inputMemo,
                    onValueChange = { inputMemo = it },
                    label = { Text("메모 (선택사항)") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showAddDialog = false
                    val memoToSave = inputMemo.trim().takeIf { it.isNotEmpty() }
                    if (addTime == null) {
                        viewModel.addNow(memoToSave)
                    } else {
                        viewModel.addAtSelectedTime(addTime!!.first, addTime!!.second, memoToSave)
                    }
                }) {
                    Text("저장")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("취소")
                }
            }
        )
    }

    if (viewMemoEvent != null) {
        AlertDialog(
            onDismissRequest = { viewMemoEvent = null },
            title = {
                Text(text = "메모 조회/수정")
            },
            text = {
                OutlinedTextField(
                    value = editMemoText,
                    onValueChange = { editMemoText = it },
                    label = { Text("메모") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val eventToUpdate = viewMemoEvent
                    if (eventToUpdate != null) {
                        viewModel.updateMemo(eventToUpdate.localId, editMemoText.trim().takeIf { it.isNotEmpty() })
                    }
                    viewMemoEvent = null
                }) {
                    Text("저장")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewMemoEvent = null }) {
                    Text("닫기")
                }
            }
        )
    }

    if (state.confirmDeleteEventId != null) {
        AlertDialog(
            onDismissRequest = viewModel::dismissDeleteDialog,
            title = { Text("기록 삭제") },
            text = { Text("이 기록을 삭제할까요?") },
            confirmButton = {
                TextButton(onClick = viewModel::confirmDelete) {
                    Text("삭제")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDeleteDialog) {
                    Text("취소")
                }
            }
        )
    }
}

@Composable
private fun EventItem(
    event: VoidingEvent,
    intervalText: String?,
    onViewMemo: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = event.voidedAtEpochMs.toTimeText(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (intervalText != null) {
                        Text(
                            text = intervalText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onViewMemo,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.List,
                            contentDescription = "메모 보기",
                            tint = if (!event.memo.isNullOrBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                        )
                    }
                    Spacer(modifier = Modifier.size(8.dp))
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "삭제",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
    }
}

private fun Long.toTimeText(): String {
    val formatter = DateTimeFormatter.ofPattern("HH:mm")
    return Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .toLocalTime()
        .format(formatter)
}

private fun String.toUiErrorText(maxLen: Int = 120): String {
    val normalized = replace('\n', ' ').replace('\r', ' ').trim()
    return if (normalized.length <= maxLen) normalized else normalized.take(maxLen) + "..."
}
