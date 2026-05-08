package com.bladderdiary.app.presentation.privacy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SensitiveCloudNoticeAcknowledgementBlock(
    isAcknowledged: Boolean,
    isEnabled: Boolean,
    onAcknowledge: () -> Unit,
    onShowDetails: () -> Unit
) {
    HorizontalDivider()
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "민감정보 및 클라우드 저장 안내",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "배뇨 기록은 건강 관련 민감정보일 수 있으며, 로그인하면 기기와 Supabase 클라우드에 저장·동기화됩니다.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isAcknowledged,
                onCheckedChange = { checked ->
                    if (checked) {
                        onAcknowledge()
                    }
                },
                enabled = isEnabled && !isAcknowledged
            )
            Text(
                text = "안내를 확인했습니다.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            TextButton(
                onClick = onShowDetails,
                enabled = isEnabled
            ) {
                Text("자세히")
            }
        }
    }
}

@Composable
fun SensitiveCloudNoticeDialog(
    onConfirm: () -> Unit,
    onDismiss: (() -> Unit)? = null,
    confirmLabel: String = "확인했습니다"
) {
    val dismissButton: (@Composable () -> Unit)? = if (onDismiss == null) {
        null
    } else {
        {
            TextButton(onClick = onDismiss) {
                Text("닫기")
            }
        }
    }

    AlertDialog(
        onDismissRequest = { onDismiss?.invoke() },
        title = { Text("민감정보 및 클라우드 저장 안내") },
        text = { SensitiveCloudNoticeBody() },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(confirmLabel)
            }
        },
        dismissButton = dismissButton
    )
}

@Composable
private fun SensitiveCloudNoticeBody() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        NoticeParagraph(
            "배뇨 기록 시각, 날짜, 배뇨량, 절박감, 요실금 여부, 야간뇨 여부, 메모는 건강 관련 민감정보일 수 있습니다."
        )
        NoticeParagraph(
            "로그인하면 기록은 이 기기에 저장되고, 백업과 기기 간 동기화를 위해 Supabase 클라우드에도 저장됩니다."
        )
        NoticeParagraph(
            "메모 E2EE는 선택 기능입니다. 사용하지 않으면 메모 내용이 동기화 목적으로 서버에 저장될 수 있습니다."
        )
        NoticeParagraph(
            "E2EE를 켜도 배뇨량, 절박감, 요실금 여부 같은 구조화된 기록은 통계와 동기화를 위해 서버에 저장될 수 있습니다."
        )
        NoticeParagraph(
            "자세한 내용은 개인정보 처리방침의 수집 정보, 보관 방식, 보안 조치 항목을 확인해주세요."
        )
    }
}

@Composable
private fun NoticeParagraph(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 2.dp)
    )
}
