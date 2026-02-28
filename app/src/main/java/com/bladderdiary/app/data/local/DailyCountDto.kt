package com.bladderdiary.app.data.local

import androidx.room.ColumnInfo

data class DailyCountDto(
    @ColumnInfo(name = "local_date") val localDate: String,
    @ColumnInfo(name = "count") val count: Int
)
