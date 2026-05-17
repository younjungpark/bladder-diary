package com.bladderdiary.app.data.backup

import androidx.room.withTransaction
import com.bladderdiary.app.data.local.AppDatabase
import kotlinx.datetime.Clock

class RoomBackupLocalDataSource(
    private val db: AppDatabase,
    private val metadataProvider: BackupAppMetadataProvider = DefaultBackupAppMetadataProvider(),
    private val clock: () -> Long = { Clock.System.now().toEpochMilliseconds() }
) : BackupLocalDataSource {
    private val eventDao = db.voidingEventDao()
    private val syncQueueDao = db.syncQueueDao()

    override suspend fun exportPayload(userId: String): BackupPlainPayloadV1 {
        val metadata = metadataProvider.current()
        val records = eventDao.getAllByUserId(userId).map { entity ->
            BackupRecordV1(
                localId = entity.localId,
                voidedAtEpochMs = entity.voidedAtEpochMs,
                localDate = entity.localDate,
                isDeleted = entity.isDeleted,
                updatedAtEpochMs = entity.updatedAtEpochMs,
                memo = entity.memo,
                volumeMl = entity.volumeMl,
                urgency = entity.urgency,
                hasIncontinence = entity.hasIncontinence,
                isNocturia = entity.isNocturia
            )
        }
        return BackupPlainPayloadV1(
            exportedAtEpochMs = clock(),
            sourceAppVersionName = metadata.versionName,
            sourceAppVersionCode = metadata.versionCode,
            sourceDatabaseVersion = metadata.databaseVersion,
            userId = userId,
            records = records
        )
    }

    override suspend fun restorePayload(
        userId: String,
        payload: BackupPlainPayloadV1,
        mode: BackupRestoreMode
    ): BackupRestoreReport {
        BackupPayloadValidator.validate(payload, userId)
        val localRecords = eventDao.getAllByUserId(userId)
        val plan = BackupRestorePlanner.plan(
            userId = userId,
            localRecords = localRecords,
            backupRecords = payload.records,
            mode = mode
        )
        db.withTransaction {
            if (plan.syncQueueEventLocalIdsToClear.isNotEmpty()) {
                syncQueueDao.deleteByEventLocalIds(plan.syncQueueEventLocalIdsToClear)
            }
            if (plan.deleteExistingUserEvents) {
                eventDao.deleteAllForUser(userId)
            }
            if (plan.recordsToUpsert.isNotEmpty()) {
                eventDao.upsertAll(plan.recordsToUpsert)
            }
        }
        return plan.report
    }
}
