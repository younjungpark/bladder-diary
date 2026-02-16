package com.bladderdiary.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncQueueDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: SyncQueueEntity)

    @Update
    suspend fun update(item: SyncQueueEntity)

    @Query("SELECT * FROM sync_queue ORDER BY retry_count ASC")
    suspend fun getAll(): List<SyncQueueEntity>

    @Query(
        """
        SELECT sq.last_error
        FROM sync_queue sq
        INNER JOIN voiding_events ve ON ve.local_id = sq.event_local_id
        WHERE ve.user_id = :userId
          AND ve.sync_state IN ('PENDING_CREATE', 'PENDING_DELETE')
          AND sq.last_error IS NOT NULL
        ORDER BY sq.retry_count DESC
        LIMIT 1
        """
    )
    fun observeLastPendingError(userId: String): Flow<String?>

    @Query("DELETE FROM sync_queue WHERE queue_id = :queueId")
    suspend fun delete(queueId: String)
}
