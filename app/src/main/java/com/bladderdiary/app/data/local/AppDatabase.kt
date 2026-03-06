package com.bladderdiary.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [VoidingEventEntity::class, SyncQueueEntity::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(RoomConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun voidingEventDao(): VoidingEventDao
    abstract fun syncQueueDao(): SyncQueueDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE voiding_events ADD COLUMN memo TEXT")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE voiding_events ADD COLUMN memo_ciphertext TEXT")
                db.execSQL("ALTER TABLE voiding_events ADD COLUMN memo_encryption TEXT NOT NULL DEFAULT 'NONE'")
                db.execSQL("UPDATE voiding_events SET memo_ciphertext = memo WHERE memo IS NOT NULL")
            }
        }

        fun create(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                "bladder_diary.db"
            )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
        }
    }
}
