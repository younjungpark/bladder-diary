package com.bladderdiary.app.domain.usecase

import com.bladderdiary.app.domain.model.VoidingRepository

class UpdateVoidingEventVolumeUseCase(
    private val repository: VoidingRepository
) {
    suspend operator fun invoke(localId: String, volumeMl: Int?): Result<Unit> {
        return repository.updateVolume(localId, volumeMl)
    }
}
