package com.bladderdiary.app.data.repository

import androidx.room.withTransaction
import com.bladderdiary.app.data.local.AppDatabase
import com.bladderdiary.app.data.local.SyncQueueEntity
import com.bladderdiary.app.data.local.VoidingEventEntity
import com.bladderdiary.app.data.remote.SupabaseApi
import com.bladderdiary.app.data.remote.dto.VoidingEventRemoteDto
import com.bladderdiary.app.data.security.MemoEncryptionScheme
import com.bladderdiary.app.domain.model.AuthRepository
import com.bladderdiary.app.domain.model.E2eeRepository
import com.bladderdiary.app.domain.model.SyncAction
import com.bladderdiary.app.domain.model.SyncReport
import com.bladderdiary.app.domain.model.SyncState
import com.bladderdiary.app.domain.model.VoidingEvent
import com.bladderdiary.app.domain.model.VoidingRepository
import com.bladderdiary.app.worker.SyncScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
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
    private val syncScheduler: SyncScheduler,
    private val e2eeRepository: E2eeRepository
) : VoidingRepository {
    private companion object {
        const val TAG = "VoidingRepository"
    }

    private val eventDao = db.voidingEventDao()
    private val queueDao = db.syncQueueDao()
    private val activeSyncCount = MutableStateFlow(0)

    override suspend fun addNow(
        urgency: Int,
        hasIncontinence: Boolean,
        isNocturia: Boolean,
        memo: String?,
        volumeMl: Int?
    ): Result<Unit> {
        val session = authRepository.getSession() ?: return Result.failure(
            IllegalStateException("로그인이 필요합니다.")
        )
        if (urgency !in 1..5) {
            return Result.failure(IllegalArgumentException("절박감은 1부터 5 사이여야 합니다."))
        }

        return runCatching {
            val now = Clock.System.now()
            addPendingCreate(
                userId = session.userId,
                epochMs = now.toEpochMilliseconds(),
                localDate = now.toLocalDate(),
                urgency = urgency,
                hasIncontinence = hasIncontinence,
                isNocturia = isNocturia,
                memo = memo,
                volumeMl = volumeMl
            )
            syncNowOrSchedule()
        }
    }

    override suspend fun addAt(
        date: LocalDate,
        hour: Int,
        minute: Int,
        urgency: Int,
        hasIncontinence: Boolean,
        isNocturia: Boolean,
        memo: String?,
        volumeMl: Int?
    ): Result<Unit> {
        val session = authRepository.getSession() ?: return Result.failure(
            IllegalStateException("로그인이 필요합니다.")
        )
        if (hour !in 0..23 || minute !in 0..59) {
            return Result.failure(IllegalArgumentException("시간 형식이 올바르지 않습니다."))
        }
        if (urgency !in 1..5) {
            return Result.failure(IllegalArgumentException("절박감은 1부터 5 사이여야 합니다."))
        }

        return runCatching {
            addPendingCreate(
                userId = session.userId,
                epochMs = date.toEpochMilliseconds(hour = hour, minute = minute),
                localDate = date.toString(),
                urgency = urgency,
                hasIncontinence = hasIncontinence,
                isNocturia = isNocturia,
                memo = memo,
                volumeMl = volumeMl
            )
            syncNowOrSchedule()
        }
    }

    override fun observeByDate(date: kotlinx.datetime.LocalDate): Flow<List<VoidingEvent>> =
        authRepository.sessionFlow.flatMapLatest { session ->
            if (session == null) {
                flowOf(emptyList())
            } else {
                eventDao.observeByDate(session.userId, date.toString()).map { list ->
                    list.map { it.toDomain() }
                }
            }
        }

    override suspend fun getByDateRange(
        startDate: LocalDate,
        endDate: LocalDate
    ): Result<List<VoidingEvent>> {
        val session = authRepository.getSession() ?: return Result.failure(
            IllegalStateException("로그인이 필요합니다.")
        )
        if (startDate > endDate) {
            return Result.failure(IllegalArgumentException("기간 범위가 올바르지 않습니다."))
        }

        return runCatching {
            eventDao.getByDateRange(
                userId = session.userId,
                startDate = startDate.toString(),
                endDate = endDate.toString()
            ).map { it.toDomain() }
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    override fun observeDailyCount(date: kotlinx.datetime.LocalDate): Flow<Int> =
        authRepository.sessionFlow.flatMapLatest { session ->
            if (session == null) {
                flowOf(0)
            } else {
                eventDao.observeDailyCount(session.userId, date.toString())
            }
        }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    override fun observeMonthlyCounts(
        yearMonth: String
    ): Flow<Map<kotlinx.datetime.LocalDate, Int>> {
        val pattern = "$yearMonth-%"
        return authRepository.sessionFlow.flatMapLatest { session ->
            if (session == null) {
                flowOf(emptyMap())
            } else {
                eventDao.observeMonthlyCounts(session.userId, pattern).map { dtoList ->
                    dtoList.associate { dto ->
                        kotlinx.datetime.LocalDate.parse(dto.localDate) to dto.count
                    }
                }
            }
        }.catch { emit(emptyMap()) }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    override fun observePendingSyncCount(): Flow<Int> =
        authRepository.sessionFlow.flatMapLatest { session ->
            if (session == null) {
                flowOf(0)
            } else {
                eventDao.observePendingCount(session.userId)
            }
        }

    override fun observePendingSyncError(): Flow<String?> =
        authRepository.sessionFlow.flatMapLatest { session ->
            if (session == null) {
                flowOf(null)
            } else {
                queueDao.observeLastPendingError(session.userId)
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

    override suspend fun updateEvent(
        localId: String,
        hour: Int,
        minute: Int,
        urgency: Int,
        hasIncontinence: Boolean,
        isNocturia: Boolean,
        memo: String?,
        volumeMl: Int?
    ): Result<Unit> {
        val event = eventDao.getById(localId) ?: return Result.failure(
            IllegalStateException("기록을 찾을 수 없습니다.")
        )
        if (hour !in 0..23 || minute !in 0..59) {
            return Result.failure(IllegalArgumentException("시간 형식이 올바르지 않습니다."))
        }
        if (urgency !in 1..5) {
            return Result.failure(IllegalArgumentException("절박감은 1부터 5 사이여야 합니다."))
        }

        return runCatching {
            val eventDate = LocalDate.parse(event.localDate)
            upsertPendingUpdate(
                event = event,
                voidedAtEpochMs = eventDate.toEpochMilliseconds(hour = hour, minute = minute),
                urgency = urgency,
                hasIncontinence = hasIncontinence,
                isNocturia = isNocturia,
                memo = memo,
                volumeMl = volumeMl
            )
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

    override suspend fun fetchAndSyncAll(): Result<Unit> {
        activeSyncCount.update { it + 1 }
        try {
            // 1. 기존 기기의 미동기화 기록 업로드 완료
            val uploadResult = syncPending()
            if (uploadResult.isFailure) {
                repoTrace("fetchAndSyncAll: syncPending failed", uploadResult.exceptionOrNull())
            } else {
                val report = uploadResult.getOrNull()
                repoTrace(
                    "fetchAndSyncAll: syncPending success " +
                        "successCount=${report?.successCount} " +
                        "failCount=${report?.failCount}"
                )
            }

            val session = authRepository.getSession()
                ?: return Result.failure(IllegalStateException("로그인이 필요합니다."))
            return runCatching {
                var accessToken = session.accessToken
                var remoteEventsResult = runCatching {
                    api.getVoidingEvents(accessToken, session.userId)
                }

                if (
                    remoteEventsResult.isFailure &&
                    remoteEventsResult.exceptionOrNull().isJwtExpired()
                ) {
                    val refreshed = authRepository.refreshSession()
                    if (refreshed.isSuccess) {
                        accessToken = refreshed.getOrThrow().accessToken
                        remoteEventsResult = runCatching {
                            api.getVoidingEvents(accessToken, session.userId)
                        }
                    }
                }

                val dtos = remoteEventsResult.getOrThrow()
                repoTrace(
                    "fetchAndSyncAll: downloaded remoteCount=${dtos.size} userId=${session.userId}"
                )

                // 2. Dto를 Entity로 변환하며 다운로드 반영
                val entities = dtos.map { dto ->
                    val isDeleted = dto.deletedAt != null
                    val epochMs = try {
                        Instant.parse(dto.voidedAt).toEpochMilliseconds()
                    } catch (e: Exception) {
                        0L // 기본값이나 에러처리
                    }
                    val updatedEpochMs = if (isDeleted) {
                        try {
                            Instant.parse(dto.deletedAt!!).toEpochMilliseconds()
                        } catch (e: Exception) {
                            Clock.System.now().toEpochMilliseconds()
                        }
                    } else {
                        Clock.System.now().toEpochMilliseconds()
                    }
                    val memoEncryption = dto.memoEncryption.ifBlank { MemoEncryptionScheme.NONE }
                    val memo = e2eeRepository.decryptMemo(
                        userId = session.userId,
                        eventId = dto.id,
                        localDate = dto.localDate,
                        memoCiphertext = dto.memoCiphertext,
                        memoEncryption = memoEncryption
                    ).getOrNull()

                    VoidingEventEntity(
                        localId = dto.id,
                        userId = session.userId,
                        voidedAtEpochMs = epochMs,
                        localDate = dto.localDate,
                        isDeleted = isDeleted,
                        syncState = SyncState.SYNCED,
                        updatedAtEpochMs = updatedEpochMs,
                        memo = memo,
                        volumeMl = dto.volumeMl.normalizedVolumeMl(),
                        urgency = dto.urgency.normalizedUrgency(),
                        hasIncontinence = dto.hasIncontinence,
                        isNocturia = dto.isNocturia,
                        memoCiphertext = dto.memoCiphertext,
                        memoEncryption = memoEncryption
                    )
                }

                // 3. 내부 DB 갱신
                if (entities.isNotEmpty()) {
                    db.withTransaction {
                        eventDao.upsertAll(entities)
                    }
                }
                val localCount = eventDao.getActiveByUserId(session.userId).size
                repoTrace(
                    "fetchAndSyncAll: local activeCount=$localCount " +
                        "after merge userId=${session.userId}"
                )
                Unit
            }.onFailure { error ->
                repoTrace("fetchAndSyncAll: failed userId=${session.userId}", error)
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
            deletedAt = null,
            volumeMl = event.volumeMl,
            urgency = event.urgency,
            hasIncontinence = event.hasIncontinence,
            isNocturia = event.isNocturia,
            memoCiphertext = event.memoCiphertext,
            memoEncryption = event.memoEncryption
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

    private suspend fun addPendingCreate(
        userId: String,
        epochMs: Long,
        localDate: String,
        urgency: Int,
        hasIncontinence: Boolean,
        isNocturia: Boolean,
        memo: String?,
        volumeMl: Int?
    ) {
        val localId = UUID.randomUUID().toString()
        val memoPayload = e2eeRepository.prepareMemoSyncPayload(
            userId = userId,
            eventId = localId,
            localDate = localDate,
            memo = memo
        ).getOrThrow()
        val event = VoidingEventEntity(
            localId = localId,
            userId = userId,
            voidedAtEpochMs = epochMs,
            localDate = localDate,
            isDeleted = false,
            syncState = SyncState.PENDING_CREATE,
            updatedAtEpochMs = Clock.System.now().toEpochMilliseconds(),
            memo = memo,
            volumeMl = volumeMl.normalizedVolumeMl(),
            urgency = urgency.normalizedUrgency(),
            hasIncontinence = hasIncontinence,
            isNocturia = isNocturia,
            memoCiphertext = memoPayload.memoCiphertext,
            memoEncryption = memoPayload.memoEncryption
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
        val shouldScheduleRetry = syncResult.isFailure ||
            (syncResult.getOrNull()?.failCount ?: 0) > 0
        if (shouldScheduleRetry) {
            syncScheduler.request()
        }
    }

    override suspend fun requeueAllForUpload(): Result<Unit> {
        val session = authRepository.getSession() ?: return Result.failure(
            IllegalStateException("로그인이 필요합니다.")
        )
        return runCatching {
            val nowEpochMs = Clock.System.now().toEpochMilliseconds()
            val activeEvents = eventDao.getActiveByUserId(session.userId)
            val updatedEvents = activeEvents.map { event ->
                val memoPayload = e2eeRepository.prepareMemoSyncPayload(
                    userId = event.userId,
                    eventId = event.localId,
                    localDate = event.localDate,
                    memo = event.memo
                ).getOrThrow()
                event.copy(
                    syncState = SyncState.PENDING_CREATE,
                    updatedAtEpochMs = nowEpochMs,
                    volumeMl = event.volumeMl.normalizedVolumeMl(),
                    urgency = event.urgency.normalizedUrgency(),
                    hasIncontinence = event.hasIncontinence,
                    memoCiphertext = memoPayload.memoCiphertext,
                    memoEncryption = memoPayload.memoEncryption
                )
            }
            val queueItems = updatedEvents.map { event ->
                SyncQueueEntity(
                    queueId = UUID.randomUUID().toString(),
                    eventLocalId = event.localId,
                    action = SyncAction.CREATE,
                    retryCount = 0,
                    lastError = null
                )
            }
            db.withTransaction {
                eventDao.upsertAll(updatedEvents)
                queueDao.upsertAll(queueItems)
            }
            syncNowOrSchedule()
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
            return Result.failure<Unit>(
                firstError ?: IllegalStateException("동기화 실패")
            ) to accessToken
        }

        val refreshedSession = authRepository.refreshSession()
        if (refreshedSession.isFailure) {
            val refreshError = refreshedSession.exceptionOrNull()
                ?: IllegalStateException("세션 갱신 실패")
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
            Result.failure<Unit>(
                retry.exceptionOrNull() ?: IllegalStateException("동기화 재시도 실패")
            ) to refreshedAccessToken
        }
    }

    private suspend fun upsertPendingUpdate(
        event: VoidingEventEntity,
        voidedAtEpochMs: Long,
        urgency: Int,
        hasIncontinence: Boolean,
        isNocturia: Boolean,
        memo: String?,
        volumeMl: Int?
    ) {
        val nowEpochMs = Clock.System.now().toEpochMilliseconds()
        val memoPayload = e2eeRepository.prepareMemoSyncPayload(
            userId = event.userId,
            eventId = event.localId,
            localDate = event.localDate,
            memo = memo
        ).getOrThrow()
        val updatedEvent = event.copy(
            voidedAtEpochMs = voidedAtEpochMs,
            memo = memo,
            volumeMl = volumeMl.normalizedVolumeMl(),
            urgency = urgency.normalizedUrgency(),
            hasIncontinence = hasIncontinence,
            isNocturia = isNocturia,
            memoCiphertext = memoPayload.memoCiphertext,
            memoEncryption = memoPayload.memoEncryption,
            syncState = SyncState.PENDING_CREATE,
            updatedAtEpochMs = nowEpochMs
        )
        val queueItem = SyncQueueEntity(
            queueId = UUID.randomUUID().toString(),
            eventLocalId = event.localId,
            action = SyncAction.CREATE,
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

private fun Instant.toLocalDate(): String =
    this.toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()

private fun LocalDate.toEpochMilliseconds(hour: Int, minute: Int): Long =
    LocalDateTime(this, LocalTime(hour = hour, minute = minute))
        .toInstant(TimeZone.currentSystemDefault())
        .toEpochMilliseconds()

private fun Throwable?.isJwtExpired(): Boolean {
    val text = this?.message?.lowercase() ?: return false
    return text.contains("jwt expired") || text.contains("pgrst303")
}

private fun Int?.normalizedVolumeMl(): Int? = this?.takeIf { it > 0 }

private fun Int?.normalizedUrgency(): Int? = this?.takeIf { it in 1..5 }

private fun repoTrace(message: String, throwable: Throwable? = null) {
    println("[VoidingRepository] $message")
    throwable?.printStackTrace()
}
