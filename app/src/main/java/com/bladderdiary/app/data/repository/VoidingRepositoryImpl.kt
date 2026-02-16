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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
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
    private val activeSyncCount = MutableStateFlow(0)

    override suspend fun addNow(): Result<Unit> {
        val session = authRepository.getSession() ?: return Result.failure(
            IllegalStateException("로그인이 필요합니다.")
        )

        return runCatching {
            val now = Clock.System.now()
            addPendingCreate(
                userId = session.userId,
                epochMs = now.toEpochMilliseconds(),
                localDate = now.toLocalDate()
            )
            syncNowOrSchedule()
        }
    }

    override suspend fun addAt(date: LocalDate, hour: Int, minute: Int): Result<Unit> {
        val session = authRepository.getSession() ?: return Result.failure(
            IllegalStateException("로그인이 필요합니다.")
        )
        if (hour !in 0..23 || minute !in 0..59) {
            return Result.failure(IllegalArgumentException("시간 형식이 올바르지 않습니다."))
        }

        return runCatching {
            val localDateTime = LocalDateTime(date, LocalTime(hour = hour, minute = minute))
            val epochMs = localDateTime
                .toInstant(TimeZone.currentSystemDefault())
                .toEpochMilliseconds()
            addPendingCreate(
                userId = session.userId,
                epochMs = epochMs,
                localDate = date.toString()
            )
            syncNowOrSchedule()
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

    override fun observePendingSyncError(): Flow<String?> {
        return authRepository.sessionFlow.flatMapLatest { session ->
            if (session == null) {
                flowOf(null)
            } else {
                queueDao.observeLastPendingError(session.userId)
            }
        }
    }

    override fun observeSyncInProgress(): Flow<Boolean> = activeSyncCount.map { it > 0 }

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
            syncNowOrSchedule()
        }
    }

    override suspend fun syncPending(): Result<SyncReport> {
        activeSyncCount.update { it + 1 }
        try {
            val session = authRepository.getSession() ?: return Result.success(SyncReport(0, 0))
            return runCatching {
                val queue = queueDao.getAll()
                var accessToken = session.accessToken
                var successCount = 0
                var failCount = 0

                queue.forEach { item ->
                    val event = eventDao.getById(item.eventLocalId)
                    if (event == null) {
                        queueDao.delete(item.queueId)
                        return@forEach
                    }

                    val (result, updatedAccessToken) = syncWithAutoRefresh(item, event, accessToken)
                    accessToken = updatedAccessToken

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
        } finally {
            activeSyncCount.update { count -> (count - 1).coerceAtLeast(0) }
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

    private suspend fun addPendingCreate(userId: String, epochMs: Long, localDate: String) {
        val event = VoidingEventEntity(
            localId = UUID.randomUUID().toString(),
            userId = userId,
            voidedAtEpochMs = epochMs,
            localDate = localDate,
            isDeleted = false,
            syncState = SyncState.PENDING_CREATE,
            updatedAtEpochMs = Clock.System.now().toEpochMilliseconds()
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
    }

    private suspend fun syncNowOrSchedule() {
        val syncResult = syncPending()
        val shouldScheduleRetry = syncResult.isFailure || (syncResult.getOrNull()?.failCount ?: 0) > 0
        if (shouldScheduleRetry) {
            syncScheduler.request()
        }
    }

    private suspend fun syncWithAutoRefresh(
        item: SyncQueueEntity,
        event: VoidingEventEntity,
        accessToken: String
    ): Pair<Result<Unit>, String> {
        val first = runCatching {
            when (item.action) {
                SyncAction.CREATE -> syncCreate(accessToken, event)
                SyncAction.DELETE -> syncDelete(accessToken, event)
            }
        }
        if (first.isSuccess) return Result.success(Unit) to accessToken

        val firstError = first.exceptionOrNull()
        if (!firstError.isJwtExpired()) {
            return Result.failure<Unit>(firstError ?: IllegalStateException("동기화 실패")) to accessToken
        }

        val refreshedSession = authRepository.refreshSession()
        if (refreshedSession.isFailure) {
            val refreshError = refreshedSession.exceptionOrNull() ?: IllegalStateException("세션 갱신 실패")
            return Result.failure<Unit>(refreshError) to accessToken
        }

        val refreshedAccessToken = refreshedSession.getOrThrow().accessToken
        val retry = runCatching {
            when (item.action) {
                SyncAction.CREATE -> syncCreate(refreshedAccessToken, event)
                SyncAction.DELETE -> syncDelete(refreshedAccessToken, event)
            }
        }
        return if (retry.isSuccess) {
            Result.success(Unit) to refreshedAccessToken
        } else {
            Result.failure<Unit>(retry.exceptionOrNull() ?: IllegalStateException("동기화 재시도 실패")) to refreshedAccessToken
        }
    }
}

private fun Instant.toLocalDate(): String {
    return this.toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
}

private fun Throwable?.isJwtExpired(): Boolean {
    val text = this?.message?.lowercase() ?: return false
    return text.contains("jwt expired") || text.contains("pgrst303")
}
