package com.bladderdiary.app.presentation.e2ee

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bladderdiary.app.domain.model.E2eeRepository
import com.bladderdiary.app.domain.model.E2eeState
import com.bladderdiary.app.domain.model.VoidingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val MIN_PASSPHRASE_LENGTH = 8

enum class E2eeEntryMode {
    AUTO,
    MANAGE
}

enum class E2eeMode {
    SETUP,
    UNLOCK,
    CHANGE
}

data class E2eePassphraseUiState(
    val mode: E2eeMode = E2eeMode.SETUP,
    val passphrase: String = "",
    val confirmPassphrase: String = "",
    val isEnabled: Boolean = false,
    val isUnlocked: Boolean = false,
    val isCheckingRemoteState: Boolean = false,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null
) {
    val submitEnabled: Boolean
        get() = !isSubmitting &&
            !isCheckingRemoteState &&
            passphrase.length >= MIN_PASSPHRASE_LENGTH &&
            (mode == E2eeMode.UNLOCK || confirmPassphrase.length >= MIN_PASSPHRASE_LENGTH)
}

sealed interface E2eePassphraseEvent {
    data class PassphraseChanged(val message: String) : E2eePassphraseEvent
}

class E2eePassphraseViewModel(
    private val e2eeRepository: E2eeRepository,
    private val voidingRepository: VoidingRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(E2eePassphraseUiState())
    val uiState: StateFlow<E2eePassphraseUiState> = _uiState.asStateFlow()
    private val _events = MutableSharedFlow<E2eePassphraseEvent>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()
    private var latestState = E2eeState()
    private var entryMode = E2eeEntryMode.AUTO
    private var lastStateErrorMessage: String? = null

    init {
        e2eeRepository.observeState()
            .onEach { state ->
                latestState = state
                applyState(state)
            }
            .launchIn(viewModelScope)
    }

    fun setEntryMode(mode: E2eeEntryMode) {
        if (entryMode == mode && _uiState.value.mode == resolveMode(latestState, mode)) {
            return
        }
        entryMode = mode
        _uiState.update {
            it.copy(
                mode = resolveMode(latestState, mode),
                passphrase = "",
                confirmPassphrase = "",
                errorMessage = null,
                infoMessage = null
            )
        }
    }

    fun onPassphraseChange(value: String) {
        _uiState.update {
            it.copy(
                passphrase = value,
                errorMessage = null,
                infoMessage = null
            )
        }
    }

    fun onConfirmPassphraseChange(value: String) {
        _uiState.update {
            it.copy(
                confirmPassphrase = value,
                errorMessage = null,
                infoMessage = null
            )
        }
    }

    fun submit() {
        val current = _uiState.value
        if (current.isCheckingRemoteState || current.isSubmitting) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null, infoMessage = null) }
            when (current.mode) {
                E2eeMode.SETUP -> submitSetup()
                E2eeMode.UNLOCK -> submitUnlock()
                E2eeMode.CHANGE -> submitChange()
            }
            _uiState.update { it.copy(isSubmitting = false) }
        }
    }

    fun clearRuntimeUnlock() {
        e2eeRepository.clearRuntimeUnlock()
    }

    private suspend fun submitSetup() {
        val current = _uiState.value
        if (current.passphrase.length < MIN_PASSPHRASE_LENGTH) {
            _uiState.update {
                it.copy(errorMessage = "비밀문구는 최소 ${MIN_PASSPHRASE_LENGTH}자 이상이어야 합니다.")
            }
            return
        }
        if (current.passphrase != current.confirmPassphrase) {
            _uiState.update { it.copy(errorMessage = "비밀문구가 일치하지 않습니다.") }
            return
        }

        val setupResult = e2eeRepository.setupPassphrase(current.passphrase)
        if (setupResult.isFailure) {
            _uiState.update {
                it.copy(errorMessage = setupResult.exceptionOrNull()?.message ?: "E2EE 설정에 실패했습니다.")
            }
            return
        }

        val requeueResult = voidingRepository.requeueAllForUpload()
        _uiState.update {
            it.copy(
                passphrase = "",
                confirmPassphrase = "",
                errorMessage = requeueResult.exceptionOrNull()?.message,
                infoMessage = if (requeueResult.isSuccess) {
                    "E2EE가 활성화되었고 기존 메모 재업로드를 시작했습니다."
                } else {
                    "E2EE는 활성화되었지만 기존 메모 재업로드 준비에 실패했습니다."
                }
            )
        }
    }

    private suspend fun submitChange() {
        val current = _uiState.value
        if (current.passphrase.length < MIN_PASSPHRASE_LENGTH) {
            _uiState.update {
                it.copy(errorMessage = "비밀문구는 최소 ${MIN_PASSPHRASE_LENGTH}자 이상이어야 합니다.")
            }
            return
        }
        if (current.passphrase != current.confirmPassphrase) {
            _uiState.update { it.copy(errorMessage = "비밀문구가 일치하지 않습니다.") }
            return
        }

        val changeResult = e2eeRepository.changePassphrase(current.passphrase)
        if (changeResult.isFailure) {
            _uiState.update {
                it.copy(errorMessage = changeResult.exceptionOrNull()?.message ?: "비밀문구 변경에 실패했습니다.")
            }
            return
        }

        _uiState.update {
            it.copy(
                passphrase = "",
                confirmPassphrase = "",
                errorMessage = null,
                infoMessage = null
            )
        }
        _events.tryEmit(E2eePassphraseEvent.PassphraseChanged("비밀문구가 변경되었습니다."))
    }

    private suspend fun submitUnlock() {
        val unlockResult = e2eeRepository.unlock(_uiState.value.passphrase)
        if (unlockResult.isFailure) {
            _uiState.update {
                it.copy(errorMessage = unlockResult.exceptionOrNull()?.message ?: "비밀문구 확인에 실패했습니다.")
            }
            return
        }

        val syncResult = voidingRepository.fetchAndSyncAll()
        _uiState.update {
            it.copy(
                passphrase = "",
                confirmPassphrase = "",
                errorMessage = syncResult.exceptionOrNull()?.message,
                infoMessage = if (syncResult.isSuccess) {
                    null
                } else {
                    "비밀문구 확인은 되었지만 메모 동기화에 실패했습니다."
                }
            )
        }
    }

    private fun applyState(state: E2eeState) {
        val resolvedMode = resolveMode(state, entryMode)
        val modeChanged = _uiState.value.mode != resolvedMode
        val nextStateErrorMessage = state.lastErrorMessage
            ?.takeIf { it != lastStateErrorMessage }
        lastStateErrorMessage = state.lastErrorMessage
        _uiState.update { current ->
            current.copy(
                mode = resolvedMode,
                isEnabled = state.isEnabled,
                isUnlocked = state.isUnlocked,
                isCheckingRemoteState = state.isCheckingRemoteState,
                passphrase = if (modeChanged) "" else current.passphrase,
                confirmPassphrase = if (modeChanged) "" else current.confirmPassphrase,
                errorMessage = when {
                    current.errorMessage != null && !modeChanged -> current.errorMessage
                    nextStateErrorMessage != null -> nextStateErrorMessage
                    else -> if (modeChanged) null else current.errorMessage
                },
                infoMessage = if (modeChanged) null else current.infoMessage
            )
        }
    }

    private fun resolveMode(state: E2eeState, requestedMode: E2eeEntryMode): E2eeMode {
        return when {
            !state.isEnabled -> E2eeMode.SETUP
            requestedMode == E2eeEntryMode.MANAGE && state.isUnlocked -> E2eeMode.CHANGE
            else -> E2eeMode.UNLOCK
        }
    }

    companion object {
        fun factory(
            e2eeRepository: E2eeRepository,
            voidingRepository: VoidingRepository
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return E2eePassphraseViewModel(e2eeRepository, voidingRepository) as T
                }
            }
        }
    }
}
