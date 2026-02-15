package com.bladderdiary.app.domain.usecase

import com.bladderdiary.app.domain.model.VoidingRepository
import kotlinx.datetime.LocalDate

class AddVoidingEventUseCase(
    private val repository: VoidingRepository
) {
    suspend operator fun invoke(): Result<Unit> = repository.addNow()
    suspend operator fun invoke(date: LocalDate, hour: Int, minute: Int): Result<Unit> =
        repository.addAt(date, hour, minute)
}
