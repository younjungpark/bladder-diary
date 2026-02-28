package com.bladderdiary.app.data.repository

import com.bladderdiary.app.data.local.VoidingEventEntity
import com.bladderdiary.app.domain.model.VoidingEvent

fun VoidingEventEntity.toDomain(): VoidingEvent {
    return VoidingEvent(
        localId = localId,
        userId = userId,
        voidedAtEpochMs = voidedAtEpochMs,
        localDate = localDate,
        isDeleted = isDeleted,
        syncState = syncState,
        updatedAtEpochMs = updatedAtEpochMs,
        memo = memo
    )
}
