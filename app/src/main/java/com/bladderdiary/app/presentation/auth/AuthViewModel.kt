package com.bladderdiary.app.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bladderdiary.app.MainActivity
import com.bladderdiary.app.domain.model.AuthAccount
import com.bladderdiary.app.domain.model.AuthRepository
import com.bladderdiary.app.domain.model.SocialProvider
import com.bladderdiary.app.domain.model.VoidingRepository
import com.bladderdiary.app.domain.model.toAuthAccount
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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
    val currentAccount: AuthAccount? = null,
    val rememberedAccount: AuthAccount? = null,
    val isAccountSwitchArmed: Boolean = false,
    val isDeletingAccount: Boolean = false,
    val errorMessage: String? = null,
    val oauthErrorMessage: String? = null,
    val accountDeletionErrorMessage: String? = null,
    val infoMessage: String? = null
)

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val voidingRepository: VoidingRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()
    private var hydratedUserId: String? = null

    init {
        authRepository.sessionFlow.combine(
            voidingRepository.observeCloudSyncPreference()
        ) { session, cloudSyncPreference ->
            session to cloudSyncPreference
        }
            .onEach { (session, cloudSyncPreference) ->
                _uiState.value = _uiState.value.copy(
                    isLoggedIn = session != null,
                    currentAccount = session?.toAuthAccount(),
                    isDeletingAccount = if (session == null) {
                        false
                    } else {
                        _uiState.value.isDeletingAccount
                    }
                )
                if (session == null) {
                    debugTrace("sessionFlow: no active session")
                    hydratedUserId = null
                } else if (!cloudSyncPreference.isEnabled) {
                    debugTrace("sessionFlow: cloud sync disabled userId=${session.userId}")
                    hydratedUserId = null
                } else if (hydratedUserId != session.userId) {
                    debugTrace("sessionFlow: hydrate start userId=${session.userId}")
                    hydratedUserId = session.userId
                    val hydrateResult = voidingRepository.fetchAndSyncAll()
                    if (hydrateResult.isSuccess) {
                        debugTrace("sessionFlow: hydrate success userId=${session.userId}")
                    } else {
                        debugTrace(
                            "sessionFlow: hydrate failed userId=${session.userId}",
                            hydrateResult.exceptionOrNull()
                        )
                    }
                }
            }
            .launchIn(viewModelScope)

        authRepository.rememberedAccountFlow
            .onEach { account ->
                _uiState.value = _uiState.value.copy(rememberedAccount = account)
            }
            .launchIn(viewModelScope)

        authRepository.accountSwitchArmedFlow
            .onEach { isArmed ->
                _uiState.value = _uiState.value.copy(isAccountSwitchArmed = isArmed)
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
            } else if (result.isSuccess && isLoginMode) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = null,
                    oauthErrorMessage = null,
                    infoMessage = null
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
        val current = _uiState.value
        val rememberedProvider = current.rememberedAccount?.normalizedProvider
        if (!current.isAccountSwitchArmed &&
            rememberedProvider != null &&
            rememberedProvider != provider.providerKey
        ) {
            _uiState.value = current.copy(
                oauthErrorMessage = buildAccountSwitchGuidance(current.rememberedAccount),
                errorMessage = null,
                infoMessage = null,
                pendingProvider = null,
                isOAuthLoading = false
            )
            return
        }

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

    fun armAccountSwitch() {
        viewModelScope.launch {
            authRepository.armAccountSwitch()
            _uiState.value = _uiState.value.copy(
                errorMessage = null,
                oauthErrorMessage = null,
                infoMessage = "다음 로그인부터 다른 계정으로 전환할 수 있습니다."
            )
        }
    }

    fun cancelPendingAccountSwitch() {
        viewModelScope.launch {
            authRepository.clearPendingAccountSwitch()
            _uiState.value = _uiState.value.copy(
                errorMessage = null,
                oauthErrorMessage = null,
                infoMessage = "기존 기록 계정 보호 모드로 돌아왔습니다."
            )
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

    fun deleteAccountData() {
        if (_uiState.value.isDeletingAccount) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isDeletingAccount = true,
                accountDeletionErrorMessage = null,
                errorMessage = null,
                oauthErrorMessage = null,
                infoMessage = null
            )

            val result = authRepository.deleteAccountData()
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    isDeletingAccount = false,
                    accountDeletionErrorMessage = null
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isDeletingAccount = false,
                    accountDeletionErrorMessage = result.exceptionOrNull()?.message
                        ?: "회원탈퇴 처리에 실패했습니다."
                )
            }
        }
    }

    fun consumeAccountDeletionError() {
        _uiState.value = _uiState.value.copy(accountDeletionErrorMessage = null)
    }

    companion object {
        fun factory(
            authRepository: AuthRepository,
            voidingRepository: VoidingRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                AuthViewModel(authRepository, voidingRepository) as T
        }
    }
}

private fun debugTrace(message: String, throwable: Throwable? = null) {
    println("[AuthViewModel] $message")
    throwable?.printStackTrace()
}

private fun buildAccountSwitchGuidance(rememberedAccount: AuthAccount?): String {
    val summary = rememberedAccount?.summary ?: "기존 기록 계정"
    return "이 기기의 기존 기록 계정은 $summary 입니다. 다른 계정으로 로그인하려면 먼저 '다른 계정으로 로그인'을 선택해주세요."
}
