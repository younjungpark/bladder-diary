package com.bladderdiary.app.data.remote.dto

import com.bladderdiary.app.data.security.MemoEncryptionScheme
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
    val deletedAt: String? = null,
    @SerialName("volume_ml")
    val volumeMl: Int? = null,
    @SerialName("urgency")
    val urgency: Int? = null,
    @SerialName("has_incontinence")
    val hasIncontinence: Boolean = false,
    @SerialName("is_nocturia")
    val isNocturia: Boolean = false,
    @SerialName("memo_ciphertext")
    val memoCiphertext: String? = null,
    @SerialName("memo_encryption")
    val memoEncryption: String = MemoEncryptionScheme.NONE
)

@Serializable
data class SoftDeleteRequestDto(
    @SerialName("deleted_at")
    val deletedAt: String
)
