package com.bladderdiary.app.presentation.e2ee

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
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
    val primaryGlow = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
    val secondaryGlow = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.18f)
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
        E2eeMode.SETUP -> "메모 암호화 비밀문구 설정"
        E2eeMode.UNLOCK -> "메모 암호화 잠금 해제"
        E2eeMode.CHANGE -> "메모 암호화 비밀문구 변경"
    }
    val descriptionText = when (state.mode) {
        E2eeMode.SETUP -> "메모를 더 안전하게 보관할 수 있도록 비밀문구를 설정합니다."
        E2eeMode.UNLOCK -> "암호화된 메모를 다시 보려면 같은 비밀문구가 필요합니다."
        E2eeMode.CHANGE -> "새 비밀문구로 메모 암호화 키를 갱신합니다."
    }
    val helperText = when (state.mode) {
        E2eeMode.SETUP -> "비밀문구를 잊어버리면 암호화된 메모를 복구할 수 없습니다."
        E2eeMode.UNLOCK -> "재설치나 새 기기에서도 같은 비밀문구를 입력해야 메모를 확인할 수 있습니다."
        E2eeMode.CHANGE -> "변경 후에는 새 비밀문구로만 메모 잠금을 해제할 수 있습니다."
    }
    val passphraseLabel = if (state.mode == E2eeMode.CHANGE) "새 메모 암호화 비밀문구" else "메모 암호화 비밀문구"
    val confirmLabel = if (state.mode == E2eeMode.CHANGE) "새 비밀문구 확인" else "비밀문구 확인"
    val submitText = when (state.mode) {
        E2eeMode.SETUP -> "메모 암호화 설정"
        E2eeMode.UNLOCK -> "메모 잠금 해제"
        E2eeMode.CHANGE -> "비밀문구 변경"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .drawBehind {
                drawCircle(
                    color = primaryGlow,
                    radius = size.minDimension * 0.32f,
                    center = Offset(size.width * 0.1f, size.height * 0.12f)
                )
                drawCircle(
                    color = secondaryGlow,
                    radius = size.minDimension * 0.24f,
                    center = Offset(size.width * 0.92f, size.height * 0.16f)
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                color = MaterialTheme.appExtraColors.securityContainer,
                shape = CircleShape,
                shadowElevation = 8.dp
            ) {
                Box(
                    modifier = Modifier.size(64.dp),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Default.VpnKey,
                        contentDescription = null,
                        tint = MaterialTheme.appExtraColors.onSecurityContainer
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = descriptionText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(22.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(32.dp),
                shadowElevation = 12.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 22.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SecurityChip(
                        text = when (state.mode) {
                            E2eeMode.SETUP -> "메모 암호화 설정"
                            E2eeMode.UNLOCK -> "메모 확인 필요"
                            E2eeMode.CHANGE -> "메모 암호화 변경"
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
                                .padding(vertical = 18.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                        PassphraseField(
                            value = state.passphrase,
                            label = passphraseLabel,
                            onValueChange = viewModel::onPassphraseChange
                        )

                        if (state.mode != E2eeMode.UNLOCK) {
                            PassphraseField(
                                value = state.confirmPassphrase,
                                label = confirmLabel,
                                onValueChange = viewModel::onConfirmPassphraseChange
                            )
                        }

                        state.errorMessage?.let {
                            MessageCard(
                                text = it,
                                containerColor = MaterialTheme.colorScheme.error
                                    .copy(alpha = 0.12f),
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
                                .height(56.dp),
                            shape = RoundedCornerShape(20.dp)
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
}

@Composable
private fun PassphraseField(
    value: String,
    label: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        shape = RoundedCornerShape(20.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
            focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.24f),
            unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent
        )
    )
}

@Composable
private fun SecurityChip(text: String) {
    Surface(
        color = MaterialTheme.appExtraColors.securityContainer,
        shape = RoundedCornerShape(14.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.appExtraColors.onSecurityContainer,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
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
        shape = RoundedCornerShape(18.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
        )
    }
}
