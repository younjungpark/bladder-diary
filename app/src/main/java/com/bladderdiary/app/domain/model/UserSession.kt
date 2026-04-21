package com.bladderdiary.app.domain.model

data class UserSession(
    val userId: String,
    val accessToken: String,
    val refreshToken: String,
    val email: String? = null,
    val provider: String? = null
)
