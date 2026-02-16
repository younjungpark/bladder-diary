package com.bladderdiary.app.domain.model

import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

interface VoidingRepository {
    suspend fun addNow(): Result<Unit>
    suspend fun addAt(date: LocalDate, hour: Int, minute: Int): Result<Unit>
    fun observeByDate(date: LocalDate): Flow<List<VoidingEvent>>
    fun observeDailyCount(date: LocalDate): Flow<Int>
    fun observePendingSyncCount(): Flow<Int>
    fun observePendingSyncError(): Flow<String?>
    fun observeSyncInProgress(): Flow<Boolean>
    suspend fun delete(localId: String): Result<Unit>
    suspend fun syncPending(): Result<SyncReport>
}
