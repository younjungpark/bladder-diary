package com.bladderdiary.app.domain.usecase

import com.bladderdiary.app.domain.model.VoidingRepository

class AddVoidingEventUseCase(
    private val repository: VoidingRepository
) {
    suspend operator fun invoke(): Result<Unit> = repository.addNow()
}
