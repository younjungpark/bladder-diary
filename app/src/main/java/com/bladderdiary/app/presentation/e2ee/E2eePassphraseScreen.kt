package com.bladderdiary.app.presentation.e2ee

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

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

    val descriptionText = when (state.mode) {
        E2eeMode.SETUP -> "앱 재설치 후에도 메모를 복호화할 수 있도록 비밀문구를 설정해주세요."
        E2eeMode.UNLOCK -> "암호화된 메모를 열기 위해 비밀문구를 입력해주세요."
        E2eeMode.CHANGE -> "현재 기기에서 복호화 가능한 상태입니다. 새 비밀문구로 변경할 수 있습니다."
    }
    val passphraseLabel = if (state.mode == E2eeMode.CHANGE) "새 비밀문구" else "비밀문구"
    val confirmLabel = if (state.mode == E2eeMode.CHANGE) "새 비밀문구 확인" else "비밀문구 확인"
    val helperText = when (state.mode) {
        E2eeMode.SETUP -> "비밀문구를 잊어버리면 서버에서도 메모를 복구할 수 없습니다."
        E2eeMode.UNLOCK -> "이 비밀문구는 재설치나 새 기기에서 메모를 복호화할 때 다시 필요합니다."
        E2eeMode.CHANGE -> "기존 메모는 유지되며, 이후에는 새 비밀문구로만 잠금 해제할 수 있습니다."
    }
    val submitText = when (state.mode) {
        E2eeMode.SETUP -> "E2EE 설정"
        E2eeMode.UNLOCK -> "잠금 해제"
        E2eeMode.CHANGE -> "비밀문구 변경"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
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
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "메모 종단간 암호화",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = descriptionText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(18.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    if (state.isCheckingRemoteState) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
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
                            visualTransformation = PasswordVisualTransformation()
                        )

                        if (state.mode != E2eeMode.UNLOCK) {
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = state.confirmPassphrase,
                                onValueChange = viewModel::onConfirmPassphraseChange,
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text(confirmLabel) },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation()
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = helperText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )

                        state.errorMessage?.let {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        state.infoMessage?.let {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = viewModel::submit,
                            enabled = state.submitEnabled,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = submitText)
                        }

                        if (onCancel != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(
                                onClick = onCancel,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (state.mode == E2eeMode.CHANGE) "닫기" else "설정 취소")
                            }
                        }

                        if (state.mode == E2eeMode.UNLOCK && onSignOut != null) {
                            Spacer(modifier = Modifier.height(8.dp))
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
}
