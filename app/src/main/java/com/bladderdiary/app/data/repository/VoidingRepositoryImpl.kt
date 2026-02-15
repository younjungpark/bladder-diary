package com.bladderdiary.app.data.repository

import androidx.room.withTransaction
import com.bladderdiary.app.data.local.AppDatabase
import com.bladderdiary.app.data.local.SyncQueueEntity
import com.bladderdiary.app.data.local.VoidingEventEntity
import com.bladderdiary.app.data.remote.SupabaseApi
import com.bladderdiary.app.data.remote.dto.VoidingEventRemoteDto
import com.bladderdiary.app.domain.model.AuthRepository
import com.bladderdiary.app.domain.model.SyncAction
import com.bladderdiary.app.domain.model.SyncReport
import com.bladderdiary.app.domain.model.SyncState
import com.bladderdiary.app.domain.model.VoidingEvent
import com.bladderdiary.app.domain.model.VoidingRepository
import com.bladderdiary.app.worker.SyncScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.util.UUID

class VoidingRepositoryImpl(
    private val db: AppDatabase,
    private val authRepository: AuthRepository,
    private val api: SupabaseApi,
    private val syncScheduler: SyncScheduler
) : VoidingRepository {
    private val eventDao = db.voidingEventDao()
    private val queueDao = db.syncQueueDao()

    override suspend fun addNow(): Result<Unit> {
        val session = authRepository.getSession() ?: return Result.failure(
            IllegalStateException("로그인이 필요합니다.")
        )

        return runCatching {
            val now = Clock.System.now()
            val event = VoidingEventEntity(
                localId = UUID.randomUUID().toString(),
                userId = session.userId,
                voidedAtEpochMs = now.toEpochMilliseconds(),
                localDate = now.toLocalDate(),
                isDeleted = false,
                syncState = SyncState.PENDING_CREATE,
                updatedAtEpochMs = now.toEpochMilliseconds()
            )
            val queueItem = SyncQueueEntity(
                queueId = UUID.randomUUID().toString(),
                eventLocalId = event.localId,
                action = SyncAction.CREATE,
                retryCount = 0,
                lastError = null
            )
            db.withTransaction {
                eventDao.upsert(event)
                queueDao.upsert(queueItem)
            }
            syncScheduler.request()
        }
    }

    override fun observeByDate(date: kotlinx.datetime.LocalDate): Flow<List<VoidingEvent>> {
        return authRepository.sessionFlow.flatMapLatest { session ->
            if (session == null) {
                flowOf(emptyList())
            } else {
                eventDao.observeByDate(session.userId, date.toString()).map { list ->
                    list.map { it.toDomain() }
                }
            }
        }
    }

    override fun observeDailyCount(date: kotlinx.datetime.LocalDate): Flow<Int> {
        return authRepository.sessionFlow.flatMapLatest { session ->
            if (session == null) {
                flowOf(0)
            } else {
                eventDao.observeDailyCount(session.userId, date.toString())
            }
        }
    }

    override fun observePendingSyncCount(): Flow<Int> {
        return authRepository.sessionFlow.flatMapLatest { session ->
            if (session == null) {
                flowOf(0)
            } else {
                eventDao.observePendingCount(session.userId)
            }
        }
    }

    override suspend fun delete(localId: String): Result<Unit> {
        val event = eventDao.getById(localId) ?: return Result.failure(
            IllegalStateException("기록을 찾을 수 없습니다.")
        )

        return runCatching {
            val nowEpochMs = Clock.System.now().toEpochMilliseconds()
            val updatedEvent = event.copy(
                isDeleted = true,
                syncState = SyncState.PENDING_DELETE,
                updatedAtEpochMs = nowEpochMs
            )
            val queueItem = SyncQueueEntity(
                queueId = UUID.randomUUID().toString(),
                eventLocalId = localId,
                action = SyncAction.DELETE,
                retryCount = 0,
                lastError = null
            )
            db.withTransaction {
                eventDao.update(updatedEvent)
                queueDao.upsert(queueItem)
            }
            syncScheduler.request()
        }
    }

    override suspend fun syncPending(): Result<SyncReport> {
        val session = authRepository.getSession() ?: return Result.success(SyncReport(0, 0))
        return runCatching {
            val queue = queueDao.getAll()
            var successCount = 0
            var failCount = 0

            queue.forEach { item ->
                val event = eventDao.getById(item.eventLocalId)
                if (event == null) {
                    queueDao.delete(item.queueId)
                    return@forEach
                }

                val result = runCatching {
                    when (item.action) {
                        SyncAction.CREATE -> syncCreate(session.accessToken, event)
                        SyncAction.DELETE -> syncDelete(session.accessToken, event)
                    }
                }

                if (result.isSuccess) {
                    db.withTransaction {
                        val syncedEvent = event.copy(syncState = SyncState.SYNCED)
                        eventDao.update(syncedEvent)
                        queueDao.delete(item.queueId)
                    }
                    successCount++
                } else {
                    db.withTransaction {
                        val nextRetry = item.retryCount + 1
                        queueDao.update(
                            item.copy(
                                retryCount = nextRetry,
                                lastError = result.exceptionOrNull()?.message
                            )
                        )
                        if (nextRetry >= 5) {
                            eventDao.update(event.copy(syncState = SyncState.FAILED))
                        }
                    }
                    failCount++
                }
            }

            SyncReport(successCount = successCount, failCount = failCount)
        }
    }

    private suspend fun syncCreate(accessToken: String, event: VoidingEventEntity) {
        val instant = Instant.fromEpochMilliseconds(event.voidedAtEpochMs)
        val dto = VoidingEventRemoteDto(
            id = event.localId,
            userId = event.userId,
            voidedAt = instant.toString(),
            localDate = event.localDate,
            clientRef = event.localId,
            deletedAt = null
        )
        api.upsertVoidingEvent(accessToken, dto)
    }

    private suspend fun syncDelete(accessToken: String, event: VoidingEventEntity) {
        api.softDeleteVoidingEvent(
            accessToken = accessToken,
            id = event.localId,
            userId = event.userId,
            deletedAtIso = Instant.fromEpochMilliseconds(event.updatedAtEpochMs).toString()
        )
    }
}

private fun Instant.toLocalDate(): String {
    return this.toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
}
