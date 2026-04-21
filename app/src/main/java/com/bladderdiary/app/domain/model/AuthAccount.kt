package com.bladderdiary.app.domain.model

data class AuthAccount(
    val userId: String,
    val email: String? = null,
    val provider: String? = null
) {
    val normalizedProvider: String?
        get() = provider?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }

    val providerLabel: String
        get() = when (normalizedProvider) {
            SocialProvider.GOOGLE.providerKey -> SocialProvider.GOOGLE.label
            SocialProvider.KAKAO.providerKey -> SocialProvider.KAKAO.label
            "email" -> "이메일"
            else -> "알 수 없는 로그인"
        }

    val summary: String
        get() = email?.trim()?.takeIf { it.isNotEmpty() }?.let { "$providerLabel · $it" } ?: providerLabel
}

fun UserSession.toAuthAccount(): AuthAccount = AuthAccount(
    userId = userId,
    email = email,
    provider = provider
)
