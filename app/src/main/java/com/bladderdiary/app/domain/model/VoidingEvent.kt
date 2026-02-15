package com.bladderdiary.app.domain.model

data class VoidingEvent(
    val localId: String,
    val userId: String,
    val voidedAtEpochMs: Long,
    val localDate: String,
    val isDeleted: Boolean,
    val syncState: SyncState,
    val updatedAtEpochMs: Long
)
