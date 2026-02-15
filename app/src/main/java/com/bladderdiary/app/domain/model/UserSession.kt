package com.bladderdiary.app.domain.model

data class UserSession(
    val userId: String,
    val accessToken: String,
    val refreshToken: String
)
