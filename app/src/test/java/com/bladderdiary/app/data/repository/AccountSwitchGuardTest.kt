package com.bladderdiary.app.data.repository

import com.bladderdiary.app.domain.model.AuthAccount
import com.bladderdiary.app.domain.model.UserSession
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountSwitchGuardTest {
    @Test
    fun `기존 기록 계정과 다른 계정이면 명시적 허용 없이 차단한다`() {
        val error = runCatching {
            AccountSwitchGuard.ensureAllowed(
                rememberedAccount = AuthAccount(
                    userId = "trusted",
                    email = "trusted@example.com",
                    provider = "kakao"
                ),
                candidateSession = UserSession(
                    userId = "other",
                    accessToken = "access",
                    refreshToken = "refresh",
                    email = "other@example.com",
                    provider = "google"
                ),
                isAccountSwitchArmed = false
            )
        }.exceptionOrNull()

        assertTrue(error is IllegalStateException)
        assertTrue(error?.message?.contains("'다른 계정으로 로그인'") == true)
    }

    @Test
    fun `명시적으로 계정 전환을 허용하면 다른 계정도 통과한다`() {
        val result = runCatching {
            AccountSwitchGuard.ensureAllowed(
                rememberedAccount = AuthAccount(
                    userId = "trusted",
                    email = "trusted@example.com",
                    provider = "kakao"
                ),
                candidateSession = UserSession(
                    userId = "other",
                    accessToken = "access",
                    refreshToken = "refresh",
                    email = "other@example.com",
                    provider = "google"
                ),
                isAccountSwitchArmed = true
            )
        }

        assertTrue(result.isSuccess)
    }
}
