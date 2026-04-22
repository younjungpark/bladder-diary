package com.bladderdiary.app.presentation.pin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bladderdiary.app.ui.theme.appExtraColors

@Composable
fun PinScreen(
    viewModel: PinViewModel,
    onCancel: (() -> Unit)? = null
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val pinFocusRequester = remember { FocusRequester() }
    val confirmFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val primaryGlow = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
    val secondaryGlow = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.18f)

    LaunchedEffect(
        state.mode,
        state.pin.length,
        state.confirmPin.length,
        state.isLocked,
        state.isSubmitting
    ) {
        if (state.isLocked || state.isSubmitting) return@LaunchedEffect
        if (state.mode == PinMode.SETUP) {
            if (state.pin.length < 4) {
                pinFocusRequester.requestFocus()
                keyboardController?.show()
            } else if (state.confirmPin.length < 4) {
                confirmFocusRequester.requestFocus()
                keyboardController?.show()
            }
        } else {
            pinFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .drawBehind {
                drawCircle(
                    color = primaryGlow,
                    radius = size.minDimension * 0.32f,
                    center = Offset(size.width * 0.12f, size.height * 0.12f)
                )
                drawCircle(
                    color = secondaryGlow,
                    radius = size.minDimension * 0.24f,
                    center = Offset(size.width * 0.92f, size.height * 0.18f)
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
            SecurityHeader(
                title = "PIN 보안 잠금",
                description = if (state.mode == PinMode.SETUP) {
                    "기기에서 빠르게 잠금 해제할 수 있도록 4자리 PIN을 설정합니다."
                } else {
                    "짧고 빠르게 확인되는 보호 절차로 앱 접근을 관리합니다."
                }
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
                    StatusPill(
                        text = if (state.mode == PinMode.SETUP) "앱 잠금 설정" else "앱 잠금 해제",
                        containerColor = MaterialTheme.appExtraColors.securityContainer,
                        contentColor = MaterialTheme.appExtraColors.onSecurityContainer
                    )

                    Text(
                        text = if (state.mode == PinMode.SETUP) "PIN 입력" else "PIN 확인",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )

                    PinSlotsField(
                        value = state.pin,
                        placeholder = "PIN 4자리",
                        focusRequester = pinFocusRequester,
                        onValueChange = viewModel::onPinChange,
                        onDone = viewModel::submit
                    )

                    if (state.mode == PinMode.SETUP) {
                        Text(
                            text = "PIN 확인",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        PinSlotsField(
                            value = state.confirmPin,
                            placeholder = "PIN 확인 4자리",
                            focusRequester = confirmFocusRequester,
                            onValueChange = viewModel::onConfirmPinChange,
                            onDone = viewModel::submit
                        )
                    } else {
                        MessageBanner(
                            text = "PIN 4자리를 입력하면 자동으로 잠금이 해제됩니다. " +
                                "남은 시도 ${state.remainingAttempts}회",
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                                .copy(alpha = 0.35f),
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (state.isLocked) {
                        MessageBanner(
                            text = "오입력 5회로 잠금되었습니다. ${state.lockedRemainingSeconds}초 후 다시 시도해주세요.",
                            containerColor = MaterialTheme.appExtraColors.warningContainer,
                            contentColor = MaterialTheme.appExtraColors.onWarningContainer
                        )
                    }

                    state.errorMessage?.let {
                        MessageBanner(
                            text = it,
                            containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    }

                    state.infoMessage?.let {
                        MessageBanner(
                            text = it,
                            containerColor = MaterialTheme.appExtraColors.successContainer,
                            contentColor = MaterialTheme.appExtraColors.onSuccessContainer
                        )
                    }

                    if (state.mode == PinMode.SETUP) {
                        Button(
                            onClick = viewModel::submit,
                            enabled = state.submitEnabled,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text("PIN 설정")
                        }

                        if (onCancel != null) {
                            TextButton(
                                onClick = onCancel,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("설정 취소")
                            }
                        }
                    } else {
                        TextButton(
                            onClick = viewModel::forgotPin,
                            enabled = !state.isSubmitting,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("PIN 분실")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SecurityHeader(
    title: String,
    description: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
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
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.appExtraColors.onSecurityContainer
                )
            }
        }

        Text(
            text = title,
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PinSlotsField(
    value: String,
    placeholder: String,
    focusRequester: FocusRequester,
    onValueChange: (String) -> Unit,
    onDone: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(76.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            repeat(4) { index ->
                val filled = index < value.length
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize(),
                    color = if (filled) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f)
                    },
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(
                        1.dp,
                        if (filled) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                        } else {
                            Color.Transparent
                        }
                    )
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (filled) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                            )
                        } else {
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                    .copy(alpha = 0.28f)
                            )
                        }
                    }
                }
            }
        }

        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.NumberPassword,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { onDone() }),
            visualTransformation = PasswordVisualTransformation(),
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.02f)
                .focusRequester(focusRequester)
        )
    }

    if (value.isEmpty()) {
        Text(
            text = placeholder,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StatusPill(
    text: String,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color
) {
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(14.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
        )
    }
}

@Composable
private fun MessageBanner(
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
