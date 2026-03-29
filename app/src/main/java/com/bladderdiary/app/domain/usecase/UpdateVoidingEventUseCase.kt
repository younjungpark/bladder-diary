package com.bladderdiary.app.domain.usecase

import com.bladderdiary.app.domain.model.VoidingRepository

class UpdateVoidingEventUseCase(
    private val repository: VoidingRepository
) {
    suspend operator fun invoke(
        localId: String,
        urgency: Int,
        hasIncontinence: Boolean,
        memo: String?,
        volumeMl: Int?
    ): Result<Unit> {
        return repository.updateEvent(
            localId = localId,
            urgency = urgency,
            hasIncontinence = hasIncontinence,
            memo = memo,
            volumeMl = volumeMl
        )
    }
}
