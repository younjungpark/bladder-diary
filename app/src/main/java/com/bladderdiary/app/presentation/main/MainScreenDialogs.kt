package com.bladderdiary.app.presentation.main

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EventEditorDialog(
    isVisible: Boolean,
    title: String,
    confirmLabel: String,
    timeText: String,
    urgency: Int,
    hasIncontinence: Boolean,
    isNocturia: Boolean,
    volumeText: String,
    memoText: String,
    isE2eeChecking: Boolean,
    onUrgencyChange: (Int) -> Unit,
    onPickTime: () -> Unit,
    onIncontinenceChange: (Boolean) -> Unit,
    onNocturiaChange: (Boolean) -> Unit,
    onVolumeChange: (String) -> Unit,
    onMemoChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    if (!isVisible) return
    val palette = rememberHomePalette()
    val isValidVolume = volumeText.isBlank() || volumeText.toVolumeMlOrNull() != null
    val scrollState = rememberScrollState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.surfaceStrong,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 14.dp, bottom = 6.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(palette.trackMuted)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = palette.titleText,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp)
            )

            // 기록 시각
            EditorSection(palette = palette, label = "기록 시각") {
                Surface(
                    color = palette.surfaceTint,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isE2eeChecking, onClick = onPickTime)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = timeText,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 20.sp,
                                lineHeight = 24.sp
                            ),
                            color = palette.titleText,
                            fontWeight = FontWeight.Bold
                        )
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(palette.surfaceStrong),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = palette.primaryStrong,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            EditorDivider(palette)

            // 절박감
            EditorSection(palette = palette, label = "절박감") {
                Text(
                    text = "1은 절박감 없음, 5는 절박감 강함입니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.mutedText
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    (1..5).forEach { level ->
                        UrgencyChip(
                            level = level,
                            isSelected = urgency == level,
                            palette = palette,
                            onClick = { onUrgencyChange(level) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Text(
                    text = "현재 선택: ${urgency.toUrgencyDescription()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.primaryStrong,
                    fontWeight = FontWeight.SemiBold
                )
            }

            EditorDivider(palette)

            // 요실금 여부
            EditorSection(palette = palette, label = "요실금 여부") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ToggleChip(
                        text = "없음",
                        isSelected = !hasIncontinence,
                        palette = palette,
                        onClick = { onIncontinenceChange(false) },
                        modifier = Modifier.weight(1f)
                    )
                    ToggleChip(
                        text = "있음",
                        isSelected = hasIncontinence,
                        palette = palette,
                        onClick = { onIncontinenceChange(true) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            EditorDivider(palette)

            // 야간뇨 여부
            EditorSection(palette = palette, label = "야간뇨 여부") {
                Text(
                    text = "밤에 잠들었다가 깨어 화장실에 간 기록이라면 선택해 주세요.",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.mutedText
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ToggleChip(
                        text = "일반 배뇨",
                        isSelected = !isNocturia,
                        palette = palette,
                        onClick = { onNocturiaChange(false) },
                        modifier = Modifier.weight(1f)
                    )
                    ToggleChip(
                        text = "야간뇨",
                        isSelected = isNocturia,
                        palette = palette,
                        onClick = { onNocturiaChange(true) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            EditorDivider(palette)

            // 배뇨량
            EditorSection(palette = palette, label = "배뇨량") {
                OutlinedTextField(
                    value = volumeText,
                    onValueChange = onVolumeChange,
                    placeholder = {
                        Text(
                            text = "예: 250",
                            color = palette.subtleText
                        )
                    },
                    trailingIcon = {
                        Text(
                            text = "mL",
                            style = MaterialTheme.typography.labelLarge,
                            color = palette.mutedText,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    modifier = Modifier
                        .widthIn(min = 140.dp, max = 180.dp)
                        .heightIn(min = 54.dp),
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = !isValidVolume,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = palette.surfaceMuted,
                        focusedContainerColor = palette.surfaceMuted,
                        unfocusedBorderColor = palette.borderSoft,
                        focusedBorderColor = palette.primaryStrong,
                        cursorColor = palette.primaryStrong
                    )
                )
            }

            EditorDivider(palette)

            // 메모
            EditorSection(palette = palette, label = "메모") {
                OutlinedTextField(
                    value = memoText,
                    onValueChange = onMemoChange,
                    placeholder = {
                        Text(
                            text = "증상이나 상황을 짧게 남겨보세요.",
                            color = palette.subtleText
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 96.dp),
                    shape = RoundedCornerShape(18.dp),
                    minLines = 2,
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = palette.surfaceMuted,
                        focusedContainerColor = palette.surfaceMuted,
                        unfocusedBorderColor = palette.borderSoft,
                        focusedBorderColor = palette.primaryStrong,
                        cursorColor = palette.primaryStrong
                    )
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 하단 버튼
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    color = palette.secondaryButtonBackground,
                    shape = RoundedCornerShape(18.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        palette.secondaryButtonBorder
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .clickable(onClick = onDismiss)
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "취소",
                            style = MaterialTheme.typography.titleSmall,
                            color = palette.secondaryButtonText,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            brush = if (isValidVolume && !isE2eeChecking) {
                                Brush.horizontalGradient(
                                    listOf(palette.primaryButtonStart, palette.primaryButtonEnd)
                                )
                            } else {
                                Brush.horizontalGradient(
                                    listOf(palette.surfaceMuted, palette.surfaceMuted)
                                )
                            }
                        )
                        .clickable(
                            enabled = isValidVolume && !isE2eeChecking,
                            onClick = onConfirm
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = confirmLabel,
                        style = MaterialTheme.typography.titleSmall,
                        color = if (isValidVolume && !isE2eeChecking) {
                            palette.primaryButtonText
                        } else {
                            palette.subtleText
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun EditorSection(palette: HomePalette, label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 14.sp,
                lineHeight = 18.sp
            ),
            color = palette.titleText,
            fontWeight = FontWeight.Bold
        )
        content()
    }
}

@Composable
private fun EditorDivider(palette: HomePalette) {
    HorizontalDivider(
        color = palette.borderSoft,
        thickness = 1.dp,
        modifier = Modifier.padding(vertical = 2.dp)
    )
}

@Composable
private fun UrgencyChip(
    level: Int,
    isSelected: Boolean,
    palette: HomePalette,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tone = palette.urgencyTone(level)
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) tone.container else palette.surfaceMuted,
        animationSpec = tween(200),
        label = "urgencyBg"
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) tone.content else palette.bodyText,
        animationSpec = tween(200),
        label = "urgencyText"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) tone.border else palette.borderSoft,
        animationSpec = tween(200),
        label = "urgencyBorder"
    )

    Box(
        modifier = modifier
            .height(46.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = level.toString(),
            style = MaterialTheme.typography.titleSmall,
            color = textColor,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ToggleChip(
    text: String,
    isSelected: Boolean,
    palette: HomePalette,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) palette.surfaceTint else palette.surfaceMuted,
        animationSpec = tween(200),
        label = "toggleBg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) palette.primaryStrong else palette.borderSoft,
        animationSpec = tween(200),
        label = "toggleBorder"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) palette.primaryStrong else palette.bodyText,
        animationSpec = tween(200),
        label = "toggleText"
    )

    Box(
        modifier = modifier
            .height(46.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            color = contentColor,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
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

@Composable
internal fun AccountDeletionDialog(
    isVisible: Boolean,
    isDeleting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    if (!isVisible) return

    AlertDialog(
        onDismissRequest = {
            if (!isDeleting) {
                onDismiss()
            }
        },
        title = { Text("회원탈퇴") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("로그인 계정 삭제는 운영자 확인을 거쳐 수동으로 처리되므로 완료까지 시간이 다소 소요될 수 있습니다.")
                Text(
                    "다만, 클라우드에 저장된 앱 기록과 기록 암호화 키, 그리고 이 기기의 로컬 데이터(DB, PIN 설정, 로그인 정보)는 즉시 초기화됩니다."
                )
                Text(
                    text = "이 작업은 되돌릴 수 없습니다. 계속 진행하시려면 'OK'를 눌러주세요.",
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = !isDeleting
            ) {
                Text(if (isDeleting) "처리 중" else "OK")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isDeleting
            ) {
                Text("취소")
            }
        }
    )
}

@Composable
internal fun CloudSyncToggleDialog(
    isVisible: Boolean,
    isEnabled: Boolean,
    isChanging: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    if (!isVisible) return

    AlertDialog(
        onDismissRequest = {
            if (!isChanging) {
                onDismiss()
            }
        },
        title = {
            Text(if (isEnabled) "클라우드 동기화 끄기" else "클라우드 동기화 켜기")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isEnabled) {
                    Text("앞으로 기록 변경사항을 Supabase 클라우드에 업로드하거나 내려받지 않습니다.")
                    Text("이 기기의 기록은 유지되지만, 이미 클라우드에 저장된 데이터는 자동으로 삭제되지 않습니다.")
                } else {
                    Text("기록 암호화 설정 후 이 기기의 배뇨 기록과 이후 변경사항이 Supabase 클라우드에 저장·동기화됩니다.")
                    Text("다른 기기에 저장된 기존 클라우드 기록이 이 기기로 병합될 수 있습니다.")
                    Text("Supabase에는 날짜, 계정/기록 식별자, 삭제/동기화 메타데이터가 남을 수 있습니다.")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = !isChanging
            ) {
                Text(
                    when {
                        isChanging -> "처리 중"
                        isEnabled -> "끄기"
                        else -> "켜기"
                    }
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isChanging
            ) {
                Text("취소")
            }
        }
    )
}

@Composable
internal fun CloudSyncRequiredChoiceDialog(
    isChanging: Boolean,
    onUseLocalOnly: () -> Unit,
    onEnableCloudSync: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text("클라우드 동기화 선택") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("배뇨 기록은 기본적으로 이 기기에 저장됩니다.")
                Text("클라우드 동기화를 켜면 기록 암호화 설정 후 Supabase에 저장되고 다른 기기와 병합됩니다.")
                Text("로컬만 사용하면 기록은 이 기기에만 남으며, 설정에서 나중에 동기화를 켤 수 있습니다.")
            }
        },
        confirmButton = {
            TextButton(
                onClick = onUseLocalOnly,
                enabled = !isChanging
            ) {
                Text(if (isChanging) "처리 중" else "로컬만 사용")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onEnableCloudSync,
                enabled = !isChanging
            ) {
                Text("동기화 켜기")
            }
        }
    )
}
