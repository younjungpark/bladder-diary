package com.bladderdiary.app.presentation.privacy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalConfiguration
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
            text = "배뇨 기록은 건강 관련 민감정보일 수 있으며, 동기화를 켜면 Supabase 클라우드에 저장·동기화됩니다.",
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
    val scrollState = rememberScrollState()
    val bodyMaxHeight = LocalConfiguration.current.screenHeightDp.dp * 0.48f

    AlertDialog(
        onDismissRequest = { onDismiss?.invoke() },
        title = { Text("민감정보 및 클라우드 저장 안내") },
        text = {
            SensitiveCloudNoticeBody(
                modifier = Modifier
                    .heightIn(max = bodyMaxHeight)
                    .verticalScroll(scrollState)
            )
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(confirmLabel)
            }
        }
    )
}

@Composable
private fun SensitiveCloudNoticeBody(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        NoticeParagraph(
            "배뇨 기록 시각, 날짜, 배뇨량, 절박감, 요실금 여부, 야간뇨 여부, 메모는 건강 관련 민감정보일 수 있습니다."
        )
        NoticeParagraph(
            "기록은 먼저 이 기기에 저장됩니다. 클라우드 동기화를 켜면 백업과 기기 간 동기화를 위해 Supabase에도 저장됩니다."
        )
        NoticeParagraph(
            "클라우드 기록 암호화를 설정하면 정확한 시각, 배뇨량, 절박감, 요실금 여부, 야간뇨 여부, 메모가 기기에서 암호화된 뒤 서버에 저장됩니다."
        )
        NoticeParagraph(
            "동기화를 켠 경우에도 날짜, 계정/기록 식별자, 삭제/동기화 메타데이터는 서버에 남을 수 있습니다."
        )
        NoticeParagraph(
            "기존 평문 클라우드 기록은 앱에서 암호문으로 교체하지만, 과거 백업이나 로그에 남은 데이터까지 즉시 삭제된다고 보장할 수는 없습니다."
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
