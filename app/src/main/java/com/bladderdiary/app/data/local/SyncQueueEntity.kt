package com.bladderdiary.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.bladderdiary.app.domain.model.SyncAction

@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey
    @ColumnInfo(name = "queue_id")
    val queueId: String,
    @ColumnInfo(name = "event_local_id")
    val eventLocalId: String,
    @ColumnInfo(name = "action")
    val action: SyncAction,
    @ColumnInfo(name = "retry_count")
    val retryCount: Int,
    @ColumnInfo(name = "last_error")
    val lastError: String?
)
