package com.bladderdiary.app.data.repository

import com.bladderdiary.app.domain.model.AuthAccount
import com.bladderdiary.app.domain.model.UserSession
import com.bladderdiary.app.domain.model.toAuthAccount

internal object AccountSwitchGuard {
    fun ensureAllowed(
        rememberedAccount: AuthAccount?,
        candidateSession: UserSession,
        isAccountSwitchArmed: Boolean
    ) {
        if (
            rememberedAccount == null ||
            rememberedAccount.userId == candidateSession.userId ||
            isAccountSwitchArmed
        ) {
            return
        }

        val trustedLabel = rememberedAccount.summary
        val attemptedLabel = candidateSession.toAuthAccount().summary
        throw IllegalStateException(
            "이 기기의 기존 기록 계정은 $trustedLabel 입니다. " +
                "다른 계정($attemptedLabel)으로 로그인하려면 먼저 '다른 계정으로 로그인'을 선택해주세요."
        )
    }
}
