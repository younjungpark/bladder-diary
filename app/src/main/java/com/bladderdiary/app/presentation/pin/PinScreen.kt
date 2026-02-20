package com.bladderdiary.app.presentation.pin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun PinScreen(
    viewModel: PinViewModel
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val pinFocusRequester = remember { FocusRequester() }
    val confirmFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(state.mode, state.pin.length, state.confirmPin.length, state.isLocked, state.isSubmitting) {
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
                text = "BladderDiary 보안 잠금",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (state.mode == PinMode.SETUP) "앱 보호를 위해 PIN을 설정해주세요." else "PIN을 입력해 앱을 잠금 해제해주세요.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(18.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    PinInputField(
                        value = state.pin,
                        onValueChange = viewModel::onPinChange,
                        placeholder = "PIN 4자리",
                        focusRequester = pinFocusRequester,
                        onDone = viewModel::submit
                    )

                    if (state.mode == PinMode.SETUP) {
                        Spacer(modifier = Modifier.height(12.dp))
                        PinInputField(
                            value = state.confirmPin,
                            onValueChange = viewModel::onConfirmPinChange,
                            placeholder = "PIN 확인 4자리",
                            focusRequester = confirmFocusRequester,
                            onDone = viewModel::submit
                        )
                    } else {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "PIN 4자리를 입력하면 자동으로 잠금 해제됩니다. (남은 시도 ${state.remainingAttempts}회)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    if (state.isLocked) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "오입력 5회로 잠금되었습니다. ${state.lockedRemainingSeconds}초 후 다시 시도해주세요.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    state.errorMessage?.let {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    state.infoMessage?.let {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (state.mode == PinMode.SETUP) {
                        Spacer(modifier = Modifier.height(18.dp))
                        Button(
                            onClick = viewModel::submit,
                            enabled = state.submitEnabled,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = "PIN 설정")
                        }
                    }

                    if (state.mode == PinMode.UNLOCK) {
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            onClick = viewModel::forgotPin,
                            enabled = !state.isSubmitting,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = "PIN 분실")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PinInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    focusRequester: FocusRequester,
    onDone: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = shape
            )
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.NumberPassword,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { onDone() }),
            visualTransformation = if (value.isEmpty()) VisualTransformation.None else PasswordVisualTransformation(),
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
        )
        if (value.isEmpty()) {
            Text(
                text = placeholder,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}
