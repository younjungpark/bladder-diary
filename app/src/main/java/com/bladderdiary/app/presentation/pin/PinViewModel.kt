package com.bladderdiary.app.presentation.pin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bladderdiary.app.domain.model.AuthRepository
import com.bladderdiary.app.domain.model.LockRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class PinMode {
    SETUP,
    UNLOCK
}

data class PinUiState(
    val mode: PinMode = PinMode.SETUP,
    val pin: String = "",
    val confirmPin: String = "",
    val isPinSet: Boolean = false,
    val isUnlocked: Boolean = false,
    val failedAttempts: Int = 0,
    val lockedUntilEpochMs: Long? = null,
    val nowEpochMs: Long = System.currentTimeMillis(),
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null
) {
    val remainingAttempts: Int
        get() = (MAX_FAILED_ATTEMPTS - failedAttempts).coerceAtLeast(0)

    val lockedRemainingSeconds: Long
        get() = (((lockedUntilEpochMs ?: 0L) - nowEpochMs).coerceAtLeast(0L) + 999L) / 1000L

    val isLocked: Boolean
        get() = lockedUntilEpochMs != null && lockedUntilEpochMs > nowEpochMs

    val submitEnabled: Boolean
        get() = !isSubmitting && !isLocked && pin.length == 4 && (mode == PinMode.UNLOCK || confirmPin.length == 4)

    companion object {
        const val MAX_FAILED_ATTEMPTS = 5
    }
}

class PinViewModel(
    private val authRepository: AuthRepository,
    private val lockRepository: LockRepository,
    private val startTicker: Boolean = true
) : ViewModel() {
    private val _uiState = MutableStateFlow(PinUiState())
    val uiState: StateFlow<PinUiState> = _uiState.asStateFlow()

    init {
        lockRepository.observeLockState()
            .onEach { lockState ->
                _uiState.update {
                    it.copy(
                        mode = if (lockState.isPinSet) PinMode.UNLOCK else PinMode.SETUP,
                        isPinSet = lockState.isPinSet,
                        isUnlocked = lockState.isUnlocked,
                        failedAttempts = lockState.failedAttempts,
                        lockedUntilEpochMs = lockState.lockedUntilEpochMs
                    )
                }
            }
            .launchIn(viewModelScope)

        if (startTicker) {
            viewModelScope.launch {
                while (isActive) {
                    delay(1_000)
                    _uiState.update { it.copy(nowEpochMs = System.currentTimeMillis()) }
                }
            }
        }
    }

    fun onPinChange(value: String) {
        val nextPin = value.filter(Char::isDigit).take(4)
        _uiState.update {
            it.copy(
                pin = nextPin,
                errorMessage = null,
                infoMessage = null
            )
        }
        val current = _uiState.value
        if (
            current.mode == PinMode.UNLOCK &&
            nextPin.length == 4 &&
            !current.isSubmitting &&
            !current.isLocked
        ) {
            submit()
        }
    }

    fun onConfirmPinChange(value: String) {
        val nextConfirmPin = value.filter(Char::isDigit).take(4)
        _uiState.update {
            it.copy(
                confirmPin = nextConfirmPin,
                errorMessage = null,
                infoMessage = null
            )
        }
        val current = _uiState.value
        if (
            current.mode == PinMode.SETUP &&
            current.pin.length == 4 &&
            nextConfirmPin.length == 4 &&
            !current.isSubmitting &&
            !current.isLocked
        ) {
            submit()
        }
    }

    fun submit() {
        val current = _uiState.value
        if (current.isLocked) {
            return
        }

        if (current.pin.length != 4) {
            _uiState.update { it.copy(errorMessage = "PIN 4자리를 입력해주세요.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null, infoMessage = null) }
            when (current.mode) {
                PinMode.SETUP -> submitSetup()
                PinMode.UNLOCK -> submitUnlock()
            }
            _uiState.update { it.copy(isSubmitting = false) }
        }
    }

    private suspend fun submitSetup() {
        val current = _uiState.value
        if (current.confirmPin.length != 4) {
            _uiState.update { it.copy(errorMessage = "PIN 확인 4자리를 입력해주세요.") }
            return
        }
        if (current.pin != current.confirmPin) {
            _uiState.update { it.copy(errorMessage = "PIN이 일치하지 않습니다.") }
            return
        }

        val result = lockRepository.setPin(current.pin)
        if (result.isSuccess) {
            _uiState.update {
                it.copy(
                    pin = "",
                    confirmPin = "",
                    errorMessage = null,
                    infoMessage = "PIN 설정이 완료되었습니다."
                )
            }
        } else {
            _uiState.update { it.copy(errorMessage = result.exceptionOrNull()?.message ?: "PIN 설정에 실패했습니다.") }
        }
    }

    private suspend fun submitUnlock() {
        val result = lockRepository.verifyPin(_uiState.value.pin)
        if (result.isSuccess && result.getOrNull() == true) {
            _uiState.update {
                it.copy(
                    pin = "",
                    errorMessage = null,
                    infoMessage = null
                )
            }
        } else if (result.isSuccess) {
            _uiState.update {
                it.copy(
                    pin = "",
                    errorMessage = "PIN이 올바르지 않습니다."
                )
            }
        } else {
            _uiState.update { it.copy(errorMessage = result.exceptionOrNull()?.message ?: "PIN 확인에 실패했습니다.") }
        }
    }

    fun forgotPin() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null, infoMessage = null) }
            val resetResult = lockRepository.resetForForgotPin()
            if (resetResult.isSuccess) {
                lockRepository.clearRuntimeUnlock()
                authRepository.signOut()
            } else {
                _uiState.update {
                    it.copy(errorMessage = resetResult.exceptionOrNull()?.message ?: "PIN 초기화에 실패했습니다.")
                }
            }
            _uiState.update { it.copy(isSubmitting = false) }
        }
    }

    fun clearRuntimeUnlock() {
        lockRepository.clearRuntimeUnlock()
    }

    companion object {
        fun factory(
            authRepository: AuthRepository,
            lockRepository: LockRepository
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return PinViewModel(authRepository, lockRepository) as T
                }
            }
        }
    }
}
