package com.bladderdiary.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.bladderdiary.app.domain.model.SyncState

@Entity(tableName = "voiding_events")
data class VoidingEventEntity(
    @PrimaryKey
    @ColumnInfo(name = "local_id")
    val localId: String,
    @ColumnInfo(name = "user_id")
    val userId: String,
    @ColumnInfo(name = "voided_at_epoch_ms")
    val voidedAtEpochMs: Long,
    @ColumnInfo(name = "local_date")
    val localDate: String,
    @ColumnInfo(name = "is_deleted")
    val isDeleted: Boolean,
    @ColumnInfo(name = "sync_state")
    val syncState: SyncState,
    @ColumnInfo(name = "updated_at_epoch_ms")
    val updatedAtEpochMs: Long
)
