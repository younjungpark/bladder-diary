package com.bladderdiary.app.presentation.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate

@Composable
internal fun EventEditorDialog(
    isVisible: Boolean,
    title: String,
    confirmLabel: String,
    timeText: String,
    urgency: Int,
    hasIncontinence: Boolean,
    volumeText: String,
    memoText: String,
    isE2eeChecking: Boolean,
    onUrgencyChange: (Int) -> Unit,
    onPickTime: () -> Unit,
    onIncontinenceChange: (Boolean) -> Unit,
    onVolumeChange: (String) -> Unit,
    onMemoChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    if (!isVisible) return
    val isValidVolume = volumeText.isBlank() || volumeText.toVolumeMlOrNull() != null
    val scrollState = rememberScrollState()
    val maxDialogContentHeight = 520.dp

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxDialogContentHeight)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                EditorFieldLabel("기록 시각")
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isE2eeChecking, onClick = onPickTime)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = timeText,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                EditorSectionDivider()

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    EditorFieldLabel("절박감")
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
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                EditorSectionDivider()

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    EditorFieldLabel("요실금 여부")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = !hasIncontinence,
                            onClick = { onIncontinenceChange(false) },
                            label = { Text("없음") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = hasIncontinence,
                            onClick = { onIncontinenceChange(true) },
                            label = { Text("있음") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                EditorSectionDivider()

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    EditorFieldLabel("배뇨량")
                    OutlinedTextField(
                        value = volumeText,
                        onValueChange = onVolumeChange,
                        placeholder = { Text("예: 250") },
                        trailingIcon = {
                            Text(
                                text = "mL",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        modifier = Modifier
                            .widthIn(min = 132.dp, max = 172.dp)
                            .heightIn(min = 50.dp),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = !isValidVolume,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }
                EditorSectionDivider()

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    EditorFieldLabel("메모")
                    OutlinedTextField(
                        value = memoText,
                        onValueChange = onMemoChange,
                        placeholder = { Text("증상이나 상황을 짧게 남겨보세요.") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 90.dp),
                        shape = RoundedCornerShape(16.dp),
                        minLines = 2,
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = isValidVolume && !isE2eeChecking
            ) {
                Text(confirmLabel)
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
private fun EditorSectionDivider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.42f),
        thickness = 1.dp
    )
}

@Composable
private fun EditorFieldLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.SemiBold
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
