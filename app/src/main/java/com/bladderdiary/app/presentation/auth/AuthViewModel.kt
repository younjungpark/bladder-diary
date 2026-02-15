package com.bladderdiary.app.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bladderdiary.app.domain.model.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val isLoginMode: Boolean = true,
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null
)

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        authRepository.sessionFlow
            .onEach { session ->
                _uiState.value = _uiState.value.copy(isLoggedIn = session != null)
            }
            .launchIn(viewModelScope)
    }

    fun onEmailChange(value: String) {
        _uiState.value = _uiState.value.copy(email = value)
    }

    fun onPasswordChange(value: String) {
        _uiState.value = _uiState.value.copy(password = value)
    }

    fun switchMode(isLoginMode: Boolean) {
        _uiState.value = _uiState.value.copy(
            isLoginMode = isLoginMode,
            errorMessage = null,
            infoMessage = null
        )
    }

    fun submit() {
        val current = _uiState.value
        if (current.email.isBlank() || current.password.isBlank()) {
            _uiState.value = current.copy(
                errorMessage = "이메일과 비밀번호를 입력해주세요.",
                infoMessage = null
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                infoMessage = null
            )

            val isLoginMode = _uiState.value.isLoginMode
            val result = if (isLoginMode) {
                authRepository.signIn(current.email.trim(), current.password)
            } else {
                authRepository.signUp(current.email.trim(), current.password)
            }

            if (result.isSuccess && !isLoginMode) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isLoginMode = true,
                    password = "",
                    errorMessage = null,
                    infoMessage = "회원가입 요청이 처리되었습니다. 이메일을 확인한 뒤 로그인해주세요."
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.message,
                    infoMessage = null
                )
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
        }
    }

    companion object {
        fun factory(authRepository: AuthRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return AuthViewModel(authRepository) as T
                }
            }
        }
    }
}
