package com.bladderdiary.app.domain.usecase

import com.bladderdiary.app.domain.model.VoidingRepository
import kotlinx.datetime.LocalDate

class AddVoidingEventUseCase(
    private val repository: VoidingRepository
) {
    suspend operator fun invoke(memo: String? = null): Result<Unit> = repository.addNow(memo)
    suspend operator fun invoke(date: LocalDate, hour: Int, minute: Int, memo: String? = null): Result<Unit> =
        repository.addAt(date, hour, minute, memo)
}
