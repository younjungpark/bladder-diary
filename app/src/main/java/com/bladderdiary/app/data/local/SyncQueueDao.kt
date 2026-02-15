package com.bladderdiary.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface SyncQueueDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: SyncQueueEntity)

    @Update
    suspend fun update(item: SyncQueueEntity)

    @Query("SELECT * FROM sync_queue ORDER BY retry_count ASC")
    suspend fun getAll(): List<SyncQueueEntity>

    @Query("DELETE FROM sync_queue WHERE queue_id = :queueId")
    suspend fun delete(queueId: String)
}
