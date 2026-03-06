package com.bladderdiary.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class UserE2eeKeyRemoteDto(
    @SerialName("user_id")
    val userId: String,
    val kdf: String,
    @SerialName("kdf_salt")
    val kdfSalt: String,
    @SerialName("kdf_params")
    val kdfParams: JsonObject,
    @SerialName("wrapped_dek")
    val wrappedDek: String,
    @SerialName("key_version")
    val keyVersion: Int = 1
)
