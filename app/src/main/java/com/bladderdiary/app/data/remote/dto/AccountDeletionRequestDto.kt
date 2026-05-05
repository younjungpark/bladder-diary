package com.bladderdiary.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AccountDeletionRequestDto(
    @SerialName("user_id")
    val userId: String,
    @SerialName("email")
    val email: String?,
    @SerialName("provider")
    val provider: String?,
    @SerialName("account_summary")
    val accountSummary: String
)
