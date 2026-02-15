package com.bladderdiary.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VoidingEventRemoteDto(
    val id: String,
    @SerialName("user_id")
    val userId: String,
    @SerialName("voided_at")
    val voidedAt: String,
    @SerialName("local_date")
    val localDate: String,
    @SerialName("client_ref")
    val clientRef: String,
    @SerialName("deleted_at")
    val deletedAt: String? = null
)

@Serializable
data class SoftDeleteRequestDto(
    @SerialName("deleted_at")
    val deletedAt: String
)
