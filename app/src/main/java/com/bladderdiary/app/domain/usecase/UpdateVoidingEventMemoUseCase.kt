package com.bladderdiary.app.domain.usecase

import com.bladderdiary.app.domain.model.VoidingRepository

class UpdateVoidingEventMemoUseCase(
    private val repository: VoidingRepository
) {
    suspend operator fun invoke(localId: String, memo: String?): Result<Unit> {
        return repository.updateMemo(localId, memo)
    }
}
