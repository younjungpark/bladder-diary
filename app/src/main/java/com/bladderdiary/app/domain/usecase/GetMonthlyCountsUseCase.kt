package com.bladderdiary.app.domain.usecase

import com.bladderdiary.app.domain.model.VoidingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

class GetMonthlyCountsUseCase(
    private val repository: VoidingRepository
) {
    operator fun invoke(yearMonth: String): Flow<Map<LocalDate, Int>> {
        return repository.observeMonthlyCounts(yearMonth)
    }
}
