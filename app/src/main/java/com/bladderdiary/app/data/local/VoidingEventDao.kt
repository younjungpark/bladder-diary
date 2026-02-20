package com.bladderdiary.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface VoidingEventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(event: VoidingEventEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(events: List<VoidingEventEntity>)

    @Update
    suspend fun update(event: VoidingEventEntity)

    @Query(
        """
        SELECT * FROM voiding_events
        WHERE user_id = :userId AND local_date = :date AND is_deleted = 0
        ORDER BY voided_at_epoch_ms DESC
        """
    )
    fun observeByDate(userId: String, date: String): Flow<List<VoidingEventEntity>>

    @Query(
        """
        SELECT COUNT(*) FROM voiding_events
        WHERE user_id = :userId AND local_date = :date AND is_deleted = 0
        """
    )
    fun observeDailyCount(userId: String, date: String): Flow<Int>

    @Query("SELECT * FROM voiding_events WHERE local_id = :localId LIMIT 1")
    suspend fun getById(localId: String): VoidingEventEntity?

    @Query(
        """
        SELECT COUNT(*) FROM voiding_events
        WHERE user_id = :userId
          AND sync_state IN ('PENDING_CREATE', 'PENDING_DELETE')
        """
    )
    fun observePendingCount(userId: String): Flow<Int>
}
