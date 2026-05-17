package com.bladderdiary.app.data.backup

import com.bladderdiary.app.data.local.VoidingEventEntity
import com.bladderdiary.app.data.security.MemoEncryptionScheme
import com.bladderdiary.app.data.security.RecordEncryptionScheme
import com.bladderdiary.app.domain.model.SyncState

internal data class BackupRestorePlan(
    val recordsToUpsert: List<VoidingEventEntity>,
    val syncQueueEventLocalIdsToClear: List<String>,
    val deleteExistingUserEvents: Boolean,
    val report: BackupRestoreReport
)

internal object BackupRestorePlanner {
    fun plan(
        userId: String,
        localRecords: List<VoidingEventEntity>,
        backupRecords: List<BackupRecordV1>,
        mode: BackupRestoreMode
    ): BackupRestorePlan {
        val latestBackupRecords = backupRecords
            .groupBy { it.localId }
            .values
            .map { records -> records.maxBy { it.updatedAtEpochMs } }
        val localById = localRecords.associateBy { it.localId }
        return when (mode) {
            BackupRestoreMode.MERGE -> planMerge(
                userId = userId,
                localById = localById,
                backupRecords = latestBackupRecords
            )

            BackupRestoreMode.REPLACE -> planReplace(
                userId = userId,
                localRecords = localRecords,
                backupRecords = latestBackupRecords
            )
        }
    }

    private fun planMerge(
        userId: String,
        localById: Map<String, VoidingEventEntity>,
        backupRecords: List<BackupRecordV1>
    ): BackupRestorePlan {
        var replacedExistingCount = 0
        var keptLocalNewerCount = 0
        val recordsToUpsert = backupRecords.mapNotNull { backupRecord ->
            val local = localById[backupRecord.localId]
            if (local != null && local.updatedAtEpochMs > backupRecord.updatedAtEpochMs) {
                keptLocalNewerCount++
                return@mapNotNull null
            }
            if (local != null) {
                replacedExistingCount++
            }
            backupRecord.toEntity(userId)
        }
        return BackupRestorePlan(
            recordsToUpsert = recordsToUpsert,
            syncQueueEventLocalIdsToClear = recordsToUpsert.map { it.localId },
            deleteExistingUserEvents = false,
            report = BackupRestoreReport(
                mode = BackupRestoreMode.MERGE,
                restoredCount = recordsToUpsert.size,
                replacedExistingCount = replacedExistingCount,
                keptLocalNewerCount = keptLocalNewerCount,
                deletedExistingCount = 0
            )
        )
    }

    private fun planReplace(
        userId: String,
        localRecords: List<VoidingEventEntity>,
        backupRecords: List<BackupRecordV1>
    ): BackupRestorePlan {
        val recordsToUpsert = backupRecords.map { it.toEntity(userId) }
        val queueIdsToClear = (localRecords.map { it.localId } + recordsToUpsert.map { it.localId })
            .distinct()
        return BackupRestorePlan(
            recordsToUpsert = recordsToUpsert,
            syncQueueEventLocalIdsToClear = queueIdsToClear,
            deleteExistingUserEvents = true,
            report = BackupRestoreReport(
                mode = BackupRestoreMode.REPLACE,
                restoredCount = recordsToUpsert.size,
                replacedExistingCount = recordsToUpsert.count { restored ->
                    localRecords.any { local -> local.localId == restored.localId }
                },
                keptLocalNewerCount = 0,
                deletedExistingCount = localRecords.count { local ->
                    recordsToUpsert.none { restored -> restored.localId == local.localId }
                }
            )
        )
    }

    private fun BackupRecordV1.toEntity(userId: String): VoidingEventEntity = VoidingEventEntity(
        localId = localId,
        userId = userId,
        voidedAtEpochMs = voidedAtEpochMs,
        localDate = localDate,
        isDeleted = isDeleted,
        syncState = SyncState.SYNCED,
        updatedAtEpochMs = updatedAtEpochMs,
        memo = memo,
        volumeMl = volumeMl,
        urgency = urgency,
        hasIncontinence = hasIncontinence,
        isNocturia = isNocturia,
        memoCiphertext = null,
        memoEncryption = MemoEncryptionScheme.NONE,
        recordCiphertext = null,
        recordEncryption = RecordEncryptionScheme.NONE
    )
}
