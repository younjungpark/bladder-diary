package com.bladderdiary.app.presentation.main

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.text.format.DateFormat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bladderdiary.app.domain.model.SyncState
import com.bladderdiary.app.domain.model.VoidingEvent
import com.bladderdiary.app.ui.theme.appExtraColors
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
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
    val today = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date }
    var menuExpanded by remember { mutableStateOf(false) }
    var viewMemoEvent by remember { mutableStateOf<VoidingEvent?>(null) }
    var editMemoText by remember { mutableStateOf("") }

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
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            MainTopBar(
                selectedDateLabel = state.selectedDate.toRelativeDateLabel(today),
                isPinSet = isPinSet,
                isE2eeEnabled = isE2eeEnabled,
                isE2eeChecking = isE2eeChecking,
                menuExpanded = menuExpanded,
                onOpenCalendar = onShowCalendar,
                onDismissMenu = { menuExpanded = false },
                onOpenMenu = { menuExpanded = true },
                onTogglePin = {
                    menuExpanded = false
                    onTogglePin()
                },
                onOpenE2eeSettings = {
                    menuExpanded = false
                    onOpenE2eeSettings()
                },
                onSignOut = {
                    menuExpanded = false
                    onSignOut()
                }
            )
        },
        bottomBar = {
            QuickActionBar(
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
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        MainContent(
            state = state,
            today = today,
            modifier = Modifier.padding(padding),
            onPreviousDay = viewModel::goPreviousDay,
            onNextDay = viewModel::goNextDay,
            onPickDate = { picker.show() },
            onOpenMemo = { event ->
                viewMemoEvent = event
                editMemoText = event.memo ?: ""
            },
            onDeleteEvent = { viewModel.askDelete(it) }
        )
    }

    MemoEditDialog(
        event = viewMemoEvent,
        editMemoText = editMemoText,
        isE2eeChecking = isE2eeChecking,
        onValueChange = { editMemoText = it },
        onDismiss = { viewMemoEvent = null },
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

    DeleteDialog(
        confirmDeleteEventId = state.confirmDeleteEventId,
        onDismiss = viewModel::dismissDeleteDialog,
        onConfirm = viewModel::confirmDelete
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainTopBar(
    selectedDateLabel: String,
    isPinSet: Boolean,
    isE2eeEnabled: Boolean,
    isE2eeChecking: Boolean,
    menuExpanded: Boolean,
    onOpenCalendar: () -> Unit,
    onOpenMenu: () -> Unit,
    onDismissMenu: () -> Unit,
    onTogglePin: () -> Unit,
    onOpenE2eeSettings: () -> Unit,
    onSignOut: () -> Unit
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = "배뇨 기록",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = selectedDateLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground
        ),
        actions = {
            IconButton(onClick = onOpenCalendar) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = "캘린더 보기",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Box {
                IconButton(onClick = onOpenMenu) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "추가 메뉴"
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = onDismissMenu
                ) {
                    DropdownMenuItem(
                        text = { Text(if (isE2eeEnabled) "메모 암호화 관리" else "메모 암호화 설정") },
                        leadingIcon = {
                            Icon(
                                imageVector = if (isE2eeEnabled) {
                                    Icons.Filled.VpnKey
                                } else {
                                    Icons.Outlined.VpnKey
                                },
                                contentDescription = null
                            )
                        },
                        onClick = onOpenE2eeSettings,
                        enabled = !isE2eeChecking
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
    )
}

@Composable
private fun QuickActionBar(
    isAdding: Boolean,
    isE2eeChecking: Boolean,
    onAddNow: () -> Unit,
    onAddAtTime: () -> Unit
) {
    Surface(
        modifier = Modifier.navigationBarsPadding(),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.24f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onAddNow,
                enabled = !isAdding && !isE2eeChecking,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "지금 기록",
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1
                )
            }
            OutlinedButton(
                onClick = onAddAtTime,
                enabled = !isAdding && !isE2eeChecking,
                modifier = Modifier
                    .weight(1.1f)
                    .height(56.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "시간 지정",
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun MainContent(
    state: MainUiState,
    today: kotlinx.datetime.LocalDate,
    modifier: Modifier = Modifier,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onPickDate: () -> Unit,
    onOpenMemo: (VoidingEvent) -> Unit,
    onDeleteEvent: (String) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        DailyOverviewCard(
            state = state,
            today = today,
            onPreviousDay = onPreviousDay,
            onNextDay = onNextDay,
            onPickDate = onPickDate
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "기록 내역",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "메모 ${state.events.count { !it.memo.isNullOrBlank() }}건",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (state.events.isEmpty()) {
            EmptyStateCard(
                modifier = Modifier.weight(1f),
                selectedDate = state.selectedDate,
                today = today
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {
                itemsIndexed(state.events, key = { _, it -> it.localId }) { index, event ->
                    val previousEvent = state.events.getOrNull(index + 1)
                    val intervalText = previousEvent
                        ?.let { event.voidedAtEpochMs - it.voidedAtEpochMs }
                        ?.takeIf { it > 0 }
                        ?.toIntervalText()

                    EventItem(
                        event = event,
                        intervalText = intervalText,
                        onViewMemo = { onOpenMemo(event) },
                        onDelete = { onDeleteEvent(event.localId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DailyOverviewCard(
    state: MainUiState,
    today: kotlinx.datetime.LocalDate,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onPickDate: () -> Unit
) {
    val syncLabel = when {
        state.isSyncing -> "동기화 중"
        state.pendingSyncCount > 0 -> "동기화 대기 ${state.pendingSyncCount}건"
        else -> "동기화 정상"
    }
    val syncContainerColor = when {
        state.isSyncing -> MaterialTheme.colorScheme.secondaryContainer
        state.pendingSyncCount > 0 -> MaterialTheme.appExtraColors.warningContainer
        else -> MaterialTheme.appExtraColors.successContainer
    }
    val syncContentColor = when {
        state.isSyncing -> MaterialTheme.colorScheme.onSecondaryContainer
        state.pendingSyncCount > 0 -> MaterialTheme.appExtraColors.onWarningContainer
        else -> MaterialTheme.appExtraColors.onSuccessContainer
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.24f)),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onPreviousDay,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "이전 날짜"
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onPickDate)
                        .padding(horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = state.selectedDate.toKoreanFullDate(),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = state.selectedDate.toRelativeDateLabel(today),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(
                    onClick = onNextDay,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "다음 날짜"
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = state.dailyCount.toString(),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "회",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                Surface(
                    color = syncContainerColor,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = syncLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = syncContentColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

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
        }
    }
}

@Composable
private fun EmptyStateCard(
    modifier: Modifier = Modifier,
    selectedDate: kotlinx.datetime.LocalDate,
    today: kotlinx.datetime.LocalDate
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
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
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "하단의 '지금 기록' 또는 '시간 지정'으로 빠르게 입력하실 수 있습니다.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EventItem(
    event: VoidingEvent,
    intervalText: String?,
    onViewMemo: () -> Unit,
    onDelete: () -> Unit
) {
    val hasMemo = !event.memo.isNullOrBlank()
    val syncLabel = when (event.syncState) {
        SyncState.PENDING_CREATE -> "동기화 대기"
        SyncState.PENDING_DELETE -> "삭제 대기"
        SyncState.FAILED -> "동기화 실패"
        SyncState.SYNCED -> null
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = 1.dp,
            color = if (hasMemo) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
            }
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onViewMemo)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = event.voidedAtEpochMs.toTimeText(),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (intervalText != null) {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = intervalText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    syncLabel?.let {
                        Surface(
                            color = if (event.syncState == SyncState.FAILED) {
                                MaterialTheme.appExtraColors.warningContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (event.syncState == SyncState.FAILED) {
                                    MaterialTheme.appExtraColors.onWarningContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    IconButton(
                        onClick = onViewMemo,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = "메모 보기 및 수정",
                            tint = if (hasMemo) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "기록 삭제",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (hasMemo) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = event.memo.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MemoEditDialog(
    event: VoidingEvent?,
    editMemoText: String,
    isE2eeChecking: Boolean,
    onValueChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    if (event == null) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "메모 조회 및 수정") },
        text = {
            OutlinedTextField(
                value = editMemoText,
                onValueChange = onValueChange,
                label = { Text("메모") },
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
            TextButton(onClick = onDismiss) {
                Text("닫기")
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
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color
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

private fun kotlinx.datetime.LocalDate.toKoreanFullDate(): String {
    val dayOfWeekLabel = when (dayOfWeek.value) {
        1 -> "월요일"
        2 -> "화요일"
        3 -> "수요일"
        4 -> "목요일"
        5 -> "금요일"
        6 -> "토요일"
        else -> "일요일"
    }
    return "${year}년 ${monthNumber}월 ${dayOfMonth}일 $dayOfWeekLabel"
}

private fun kotlinx.datetime.LocalDate.toRelativeDateLabel(today: kotlinx.datetime.LocalDate): String {
    return when {
        this == today -> "오늘"
        this == today.plusDays(-1) -> "어제"
        this == today.plusDays(1) -> "내일"
        else -> "날짜를 눌러 직접 선택"
    }
}

private fun kotlinx.datetime.LocalDate.plusDays(days: Int): kotlinx.datetime.LocalDate {
    return this.plus(DatePeriod(days = days))
}

private fun Long.toTimeText(): String {
    val formatter = DateTimeFormatter.ofPattern("HH:mm")
    return Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .toLocalTime()
        .format(formatter)
}

private fun Long.toIntervalText(): String {
    val minutes = this / (1000 * 60)
    val hours = minutes / 60
    val remainingMinutes = minutes % 60
    return if (hours > 0) {
        "${hours}시간 ${remainingMinutes}분 간격"
    } else {
        "${remainingMinutes}분 간격"
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
