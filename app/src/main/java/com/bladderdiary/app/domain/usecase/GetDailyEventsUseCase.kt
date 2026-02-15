package com.bladderdiary.app.domain.usecase

import com.bladderdiary.app.domain.model.VoidingEvent
import com.bladderdiary.app.domain.model.VoidingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

class GetDailyEventsUseCase(
    private val repository: VoidingRepository
) {
    operator fun invoke(date: LocalDate): Flow<List<VoidingEvent>> = repository.observeByDate(date)
}
