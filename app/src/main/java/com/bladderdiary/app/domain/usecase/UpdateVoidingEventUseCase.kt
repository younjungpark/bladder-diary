package com.bladderdiary.app.domain.usecase

import com.bladderdiary.app.domain.model.VoidingRepository

class UpdateVoidingEventUseCase(
    private val repository: VoidingRepository
) {
    suspend operator fun invoke(
        localId: String,
        hour: Int,
        minute: Int,
        urgency: Int,
        hasIncontinence: Boolean,
        isNocturia: Boolean,
        memo: String?,
        volumeMl: Int?
    ): Result<Unit> {
        return repository.updateEvent(
            localId = localId,
            hour = hour,
            minute = minute,
            urgency = urgency,
            hasIncontinence = hasIncontinence,
            isNocturia = isNocturia,
            memo = memo,
            volumeMl = volumeMl
        )
    }
}
