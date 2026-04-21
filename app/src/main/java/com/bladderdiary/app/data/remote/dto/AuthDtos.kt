package com.bladderdiary.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AuthRequest(
    val email: String,
    val password: String
)

@Serializable
data class RefreshTokenRequest(
    @SerialName("refresh_token")
    val refreshToken: String
)

@Serializable
data class AuthUserDto(
    val id: String,
    val email: String? = null,
    @SerialName("app_metadata")
    val appMetadata: AuthAppMetadataDto? = null
)

@Serializable
data class AuthAppMetadataDto(
    @SerialName("provider")
    val provider: String? = null
)

@Serializable
data class AuthResponseDto(
    @SerialName("access_token")
    val accessToken: String? = null,
    @SerialName("refresh_token")
    val refreshToken: String? = null,
    val user: AuthUserDto? = null
)
