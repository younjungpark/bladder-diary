package com.bladderdiary.app.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bladderdiary.app.MainActivity
import com.bladderdiary.app.domain.model.AuthRepository
import com.bladderdiary.app.domain.model.SocialProvider
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
    val isOAuthLoading: Boolean = false,
    val pendingProvider: SocialProvider? = null,
    val isLoggedIn: Boolean = false,
    val errorMessage: String? = null,
    val oauthErrorMessage: String? = null,
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

        MainActivity.oauthCallbackFlow
            .onEach { callbackUrl ->
                handleOAuthCallback(callbackUrl)
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
            oauthErrorMessage = null,
            infoMessage = null
        )
    }

    fun submit() {
        val current = _uiState.value
        if (current.email.isBlank() || current.password.isBlank()) {
            _uiState.value = current.copy(
                errorMessage = "이메일과 비밀번호를 입력해주세요.",
                oauthErrorMessage = null,
                infoMessage = null
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                oauthErrorMessage = null,
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
                    oauthErrorMessage = null,
                    infoMessage = "회원가입 요청이 처리되었습니다. 이메일을 확인한 뒤 로그인해주세요."
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.message,
                    oauthErrorMessage = null,
                    infoMessage = null
                )
            }
        }
    }

    fun signInWithSocial(provider: SocialProvider) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isOAuthLoading = true,
                pendingProvider = provider,
                errorMessage = null,
                oauthErrorMessage = null,
                infoMessage = null
            )

            val result = authRepository.signInWithSocial(provider)
            if (result.isFailure) {
                _uiState.value = _uiState.value.copy(
                    isOAuthLoading = false,
                    pendingProvider = null,
                    oauthErrorMessage = result.exceptionOrNull()?.message ?: "소셜 로그인 시작에 실패했습니다."
                )
            }
        }
    }

    private fun handleOAuthCallback(callbackUrl: String) {
        viewModelScope.launch {
            val result = authRepository.handleOAuthCallback(callbackUrl)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isOAuthLoading = false,
                    pendingProvider = null,
                    oauthErrorMessage = null,
                    infoMessage = null
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isOAuthLoading = false,
                    pendingProvider = null,
                    oauthErrorMessage = result.exceptionOrNull()?.message ?: "소셜 로그인 처리에 실패했습니다."
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
