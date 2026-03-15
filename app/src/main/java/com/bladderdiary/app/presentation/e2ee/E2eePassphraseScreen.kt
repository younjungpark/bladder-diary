package com.bladderdiary.app.presentation.e2ee

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bladderdiary.app.ui.theme.appExtraColors

@Composable
fun E2eePassphraseScreen(
    viewModel: E2eePassphraseViewModel,
    entryMode: E2eeEntryMode = E2eeEntryMode.AUTO,
    onCancel: (() -> Unit)? = null,
    onPassphraseChanged: ((String) -> Unit)? = null,
    onSignOut: (() -> Unit)? = null
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(entryMode) {
        viewModel.setEntryMode(entryMode)
    }
    LaunchedEffect(viewModel, onPassphraseChanged) {
        if (onPassphraseChanged == null) return@LaunchedEffect
        viewModel.events.collect { event ->
            when (event) {
                is E2eePassphraseEvent.PassphraseChanged -> onPassphraseChanged(event.message)
            }
        }
    }

    val title = when (state.mode) {
        E2eeMode.SETUP -> "메모 종단간 암호화 설정"
        E2eeMode.UNLOCK -> "암호화 메모 잠금 해제"
        E2eeMode.CHANGE -> "비밀문구 변경"
    }
    val descriptionText = when (state.mode) {
        E2eeMode.SETUP -> "메모 내용을 기기 외부에서 읽을 수 없도록 보호합니다."
        E2eeMode.UNLOCK -> "이 기기에서 암호화된 메모를 다시 확인하기 위한 단계입니다."
        E2eeMode.CHANGE -> "복호화 가능한 상태에서 새 비밀문구로 안전하게 교체합니다."
    }
    val helperText = when (state.mode) {
        E2eeMode.SETUP -> "비밀문구를 잊어버리면 서버에서도 메모를 복구할 수 없습니다."
        E2eeMode.UNLOCK -> "재설치나 새 기기에서도 동일한 비밀문구가 필요합니다."
        E2eeMode.CHANGE -> "변경 후에는 새 비밀문구로만 메모 잠금 해제가 가능합니다."
    }
    val passphraseLabel = if (state.mode == E2eeMode.CHANGE) "새 비밀문구" else "비밀문구"
    val confirmLabel = if (state.mode == E2eeMode.CHANGE) "새 비밀문구 확인" else "비밀문구 확인"
    val submitText = when (state.mode) {
        E2eeMode.SETUP -> "E2EE 설정"
        E2eeMode.UNLOCK -> "잠금 해제"
        E2eeMode.CHANGE -> "비밀문구 변경"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 24.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            color = MaterialTheme.appExtraColors.securityContainer,
            shape = CircleShape
        ) {
            Box(
                modifier = Modifier.size(56.dp),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Default.VpnKey,
                    contentDescription = null,
                    tint = MaterialTheme.appExtraColors.onSecurityContainer
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = descriptionText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                SecurityChip(
                    text = when (state.mode) {
                        E2eeMode.SETUP -> "새 보호 설정"
                        E2eeMode.UNLOCK -> "잠금 해제 필요"
                        E2eeMode.CHANGE -> "보안 정보 변경"
                    }
                )

                MessageCard(
                    text = helperText,
                    containerColor = MaterialTheme.appExtraColors.warningContainer,
                    contentColor = MaterialTheme.appExtraColors.onWarningContainer
                )

                if (state.isCheckingRemoteState) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    OutlinedTextField(
                        value = state.passphrase,
                        onValueChange = viewModel::onPassphraseChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(passphraseLabel) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        shape = RoundedCornerShape(18.dp)
                    )

                    if (state.mode != E2eeMode.UNLOCK) {
                        OutlinedTextField(
                            value = state.confirmPassphrase,
                            onValueChange = viewModel::onConfirmPassphraseChange,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(confirmLabel) },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            shape = RoundedCornerShape(18.dp)
                        )
                    }

                    state.errorMessage?.let {
                        MessageCard(
                            text = it,
                            containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    }

                    state.infoMessage?.let {
                        MessageCard(
                            text = it,
                            containerColor = MaterialTheme.appExtraColors.successContainer,
                            contentColor = MaterialTheme.appExtraColors.onSuccessContainer
                        )
                    }

                    Button(
                        onClick = viewModel::submit,
                        enabled = state.submitEnabled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Text(text = submitText)
                    }

                    if (onCancel != null) {
                        TextButton(
                            onClick = onCancel,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (state.mode == E2eeMode.CHANGE) "닫기" else "설정 취소")
                        }
                    }

                    if (state.mode == E2eeMode.UNLOCK && onSignOut != null) {
                        TextButton(
                            onClick = onSignOut,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("로그아웃")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SecurityChip(text: String) {
    Surface(
        color = MaterialTheme.appExtraColors.securityContainer,
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.appExtraColors.onSecurityContainer,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun MessageCard(
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
