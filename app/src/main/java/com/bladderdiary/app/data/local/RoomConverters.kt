package com.bladderdiary.app.data.local

import androidx.room.TypeConverter
import com.bladderdiary.app.domain.model.SyncAction
import com.bladderdiary.app.domain.model.SyncState

class RoomConverters {
    @TypeConverter
    fun toSyncState(value: String): SyncState = SyncState.valueOf(value)

    @TypeConverter
    fun fromSyncState(value: SyncState): String = value.name

    @TypeConverter
    fun toSyncAction(value: String): SyncAction = SyncAction.valueOf(value)

    @TypeConverter
    fun fromSyncAction(value: SyncAction): String = value.name
}
