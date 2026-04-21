package com.bladderdiary.app.domain.usecase

import com.bladderdiary.app.domain.model.VoidingRepository
import kotlinx.datetime.LocalDate

class AddVoidingEventUseCase(
    private val repository: VoidingRepository
) {
    suspend operator fun invoke(
        urgency: Int,
        hasIncontinence: Boolean,
        isNocturia: Boolean,
        memo: String? = null,
        volumeMl: Int? = null
    ): Result<Unit> = repository.addNow(urgency, hasIncontinence, isNocturia, memo, volumeMl)

    suspend operator fun invoke(
        date: LocalDate,
        hour: Int,
        minute: Int,
        urgency: Int,
        hasIncontinence: Boolean,
        isNocturia: Boolean,
        memo: String? = null,
        volumeMl: Int? = null
    ): Result<Unit> = repository.addAt(date, hour, minute, urgency, hasIncontinence, isNocturia, memo, volumeMl)
}
