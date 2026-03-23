package com.bladderdiary.app.presentation.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.bladderdiary.app.domain.model.VoidingEvent
import kotlinx.datetime.LocalDate

@Composable
internal fun UrgencyInputDialog(
    isVisible: Boolean,
    urgency: Int,
    selectedTimeText: String?,
    onUrgencyChange: (Int) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    if (!isVisible) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("절박감 입력") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = selectedTimeText?.let { "$it 기록의 절박감을 선택해 주세요." }
                        ?: "이번 기록의 절박감을 선택해 주세요.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "1은 절박감 없음, 5는 절박감 강함입니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    (1..5).forEach { level ->
                        FilterChip(
                            selected = urgency == level,
                            onClick = { onUrgencyChange(level) },
                            label = { Text(level.toString()) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Text(
                    text = "현재 선택: ${urgency.toUrgencyDescription()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("기록")
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
internal fun PdfExportDialog(
    isVisible: Boolean,
    startDate: LocalDate,
    endDate: LocalDate,
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
internal fun MemoEditDialog(
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
internal fun VolumeEditDialog(
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
internal fun DeleteDialog(
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
