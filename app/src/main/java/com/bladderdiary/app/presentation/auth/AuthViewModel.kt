package com.bladderdiary.app.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bladderdiary.app.MainActivity
import com.bladderdiary.app.domain.model.AuthRepository
import com.bladderdiary.app.domain.model.VoidingRepository
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
    private val authRepository: AuthRepository,
    private val voidingRepository: VoidingRepository
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
            
        // 로그인 상태가 되면 기기 백업이 없을 경우를 대비해 1회 데이터 복원을 시도합니다.
        // sessionFlow에서 isLoggedIn이 true로 바뀔 때 바로 다운로드를 트리거할 수도 있지만
        // 명시적으로 submit / handleOAuthCallback 성공 시 호출하는 편이 안전합니다.
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
            } else if (result.isSuccess && isLoginMode) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = null,
                    oauthErrorMessage = null,
                    infoMessage = null
                )
                // 로그인 완료 후 데이터 다운로드
                viewModelScope.launch {
                    voidingRepository.fetchAndSyncAll()
                }
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
                // Oauth 로그인 완료 후 데이터 다운로드
                viewModelScope.launch {
                    voidingRepository.fetchAndSyncAll()
                }
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
        fun factory(authRepository: AuthRepository, voidingRepository: VoidingRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return AuthViewModel(authRepository, voidingRepository) as T
                }
            }
        }
    }
}
