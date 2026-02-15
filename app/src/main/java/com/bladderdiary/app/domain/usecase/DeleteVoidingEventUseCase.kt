package com.bladderdiary.app.domain.usecase

import com.bladderdiary.app.domain.model.VoidingRepository

class DeleteVoidingEventUseCase(
    private val repository: VoidingRepository
) {
    suspend operator fun invoke(localId: String): Result<Unit> = repository.delete(localId)
}
