package com.bladderdiary.app.data.backup

import com.bladderdiary.app.data.local.VoidingEventEntity
import com.bladderdiary.app.domain.model.SyncState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupRestorePlannerTest {
    @Test
    fun `병합 복원은 같은 localId에서 더 최신 백업 기록만 반영한다`() {
        val localOlder = entity(
            localId = "same-id",
            updatedAtEpochMs = 100L,
            syncState = SyncState.PENDING_CREATE,
            memo = "old-local"
        )
        val localNewer = entity(
            localId = "local-newer",
            updatedAtEpochMs = 300L,
            syncState = SyncState.PENDING_CREATE,
            memo = "new-local"
        )
        val plan = BackupRestorePlanner.plan(
            userId = "user-1",
            localRecords = listOf(localOlder, localNewer),
            backupRecords = listOf(
                record(localId = "same-id", updatedAtEpochMs = 200L, memo = "backup-newer"),
                record(localId = "local-newer", updatedAtEpochMs = 200L, memo = "backup-older"),
                record(localId = "new-id", updatedAtEpochMs = 150L, memo = "new-backup")
            ),
            mode = BackupRestoreMode.MERGE
        )

        assertEquals(listOf("same-id", "new-id"), plan.recordsToUpsert.map { it.localId })
        assertEquals(listOf("same-id", "new-id"), plan.syncQueueEventLocalIdsToClear)
        assertEquals(2, plan.report.restoredCount)
        assertEquals(1, plan.report.replacedExistingCount)
        assertEquals(1, plan.report.keptLocalNewerCount)
        assertTrue(plan.recordsToUpsert.all { it.syncState == SyncState.SYNCED })
    }

    @Test
    fun `교체 복원은 기존 사용자 기록 전체 삭제와 백업 기록 upsert를 계획한다`() {
        val plan = BackupRestorePlanner.plan(
            userId = "user-1",
            localRecords = listOf(
                entity(localId = "drop-me", updatedAtEpochMs = 100L),
                entity(localId = "replace-me", updatedAtEpochMs = 100L)
            ),
            backupRecords = listOf(
                record(localId = "replace-me", updatedAtEpochMs = 200L, memo = "backup"),
                record(localId = "new-id", updatedAtEpochMs = 200L, memo = "backup")
            ),
            mode = BackupRestoreMode.REPLACE
        )

        assertTrue(plan.deleteExistingUserEvents)
        assertEquals(listOf("replace-me", "new-id"), plan.recordsToUpsert.map { it.localId })
        assertEquals(
            listOf("drop-me", "replace-me", "new-id"),
            plan.syncQueueEventLocalIdsToClear
        )
        assertEquals(2, plan.report.restoredCount)
        assertEquals(1, plan.report.replacedExistingCount)
        assertEquals(1, plan.report.deletedExistingCount)
    }

    @Test
    fun `중복 백업 기록은 updatedAt이 가장 큰 항목만 사용한다`() {
        val plan = BackupRestorePlanner.plan(
            userId = "user-1",
            localRecords = emptyList(),
            backupRecords = listOf(
                record(localId = "dup", updatedAtEpochMs = 100L, memo = "old"),
                record(localId = "dup", updatedAtEpochMs = 300L, memo = "new")
            ),
            mode = BackupRestoreMode.MERGE
        )

        assertEquals(1, plan.recordsToUpsert.size)
        assertEquals("new", plan.recordsToUpsert.single().memo)
    }

    @Test
    fun `병합 복원은 백업보다 최신인 로컬 삭제 기록을 유지한다`() {
        val plan = BackupRestorePlanner.plan(
            userId = "user-1",
            localRecords = listOf(
                entity(
                    localId = "deleted-locally",
                    updatedAtEpochMs = 300L,
                    syncState = SyncState.PENDING_DELETE,
                    isDeleted = true
                )
            ),
            backupRecords = listOf(
                record(
                    localId = "deleted-locally",
                    updatedAtEpochMs = 200L,
                    memo = "backup-before-delete"
                )
            ),
            mode = BackupRestoreMode.MERGE
        )

        assertTrue(plan.recordsToUpsert.isEmpty())
        assertTrue(plan.syncQueueEventLocalIdsToClear.isEmpty())
        assertEquals(0, plan.report.restoredCount)
        assertEquals(1, plan.report.keptLocalNewerCount)
    }

    private fun entity(
        localId: String,
        updatedAtEpochMs: Long,
        syncState: SyncState = SyncState.SYNCED,
        memo: String? = null,
        isDeleted: Boolean = false
    ): VoidingEventEntity = VoidingEventEntity(
        localId = localId,
        userId = "user-1",
        voidedAtEpochMs = 1_777_000_000_000L,
        localDate = "2026-04-20",
        isDeleted = isDeleted,
        syncState = syncState,
        updatedAtEpochMs = updatedAtEpochMs,
        memo = memo,
        volumeMl = 100,
        urgency = 3
    )

    private fun record(localId: String, updatedAtEpochMs: Long, memo: String?): BackupRecordV1 =
        BackupRecordV1(
            localId = localId,
            voidedAtEpochMs = 1_777_000_000_000L,
            localDate = "2026-04-20",
            isDeleted = false,
            updatedAtEpochMs = updatedAtEpochMs,
            memo = memo,
            volumeMl = 200,
            urgency = 4
        )
}
