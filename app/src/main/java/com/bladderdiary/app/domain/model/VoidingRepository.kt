package com.bladderdiary.app.domain.model

import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

interface VoidingRepository {
    suspend fun addNow(memo: String? = null, volumeMl: Int? = null): Result<Unit>
    suspend fun addAt(date: LocalDate, hour: Int, minute: Int, memo: String? = null, volumeMl: Int? = null): Result<Unit>
    suspend fun updateMemo(localId: String, memo: String?): Result<Unit>
    suspend fun updateVolume(localId: String, volumeMl: Int?): Result<Unit>
    suspend fun getByDateRange(startDate: LocalDate, endDate: LocalDate): Result<List<VoidingEvent>>
    fun observeByDate(date: LocalDate): Flow<List<VoidingEvent>>
    fun observeDailyCount(date: LocalDate): Flow<Int>
    fun observeMonthlyCounts(yearMonth: String): Flow<Map<LocalDate, Int>>
    fun observePendingSyncCount(): Flow<Int>
    fun observePendingSyncError(): Flow<String?>
    fun observeSyncInProgress(): Flow<Boolean>
    suspend fun delete(localId: String): Result<Unit>
    suspend fun fetchAndSyncAll(): Result<Unit>
    suspend fun syncPending(): Result<SyncReport>
    suspend fun requeueAllForUpload(): Result<Unit>
}
