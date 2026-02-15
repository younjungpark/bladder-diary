package com.bladderdiary.app.domain.usecase

import com.bladderdiary.app.domain.model.SyncReport
import com.bladderdiary.app.domain.model.VoidingRepository

class SyncEventsUseCase(
    private val repository: VoidingRepository
) {
    suspend operator fun invoke(): Result<SyncReport> = repository.syncPending()
}
