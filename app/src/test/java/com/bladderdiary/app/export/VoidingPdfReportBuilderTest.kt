package com.bladderdiary.app.export

import com.bladderdiary.app.domain.model.SyncState
import com.bladderdiary.app.domain.model.VoidingEvent
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VoidingPdfReportBuilderTest {
    @Test
    fun `메모 제외 옵션이면 상세 기록 메모가 제거된다`() {
        val report = VoidingPdfReportBuilder.build(
            params = VoidingPdfExportParams(
                startDate = LocalDate(2026, 3, 1),
                endDate = LocalDate(2026, 3, 3),
                includeMemo = false
            ),
            events = sampleEvents()
        )

        assertEquals(3, report.totalCount)
        assertEquals(550, report.totalVolumeMl)
        assertEquals(2, report.volumeEntryCount)
        assertEquals(2, report.dailySummaries.size)
        assertEquals(1.0, report.averageDailyCount, 0.001)
        report.details.forEach { detail ->
            assertNull(detail.memo)
        }
    }

    @Test
    fun `메모 포함 옵션이면 상세 기록 메모가 유지된다`() {
        val report = VoidingPdfReportBuilder.build(
            params = VoidingPdfExportParams(
                startDate = LocalDate(2026, 3, 1),
                endDate = LocalDate(2026, 3, 3),
                includeMemo = true
            ),
            events = sampleEvents()
        )

        assertEquals("첫 메모", report.details.first().memo)
        assertEquals(1, report.details.first().urgency)
        assertEquals(true, report.details.first().hasIncontinence)
        assertEquals(250, report.dailySummaries.first().totalVolumeMl)
        assertEquals(300, report.dailySummaries.last().totalVolumeMl)
    }

    private fun sampleEvents(): List<VoidingEvent> = listOf(
        event(
            localId = "a",
            voidedAtEpochMs = 1_000L,
            localDate = "2026-03-01",
            memo = "첫 메모",
            volumeMl = 250,
            urgency = 1,
            hasIncontinence = true
        ),
        event(
            localId = "b",
            voidedAtEpochMs = 2_000L,
            localDate = "2026-03-03",
            memo = "둘째 메모",
            volumeMl = 300,
            urgency = 4,
            hasIncontinence = false
        ),
        event(
            localId = "c",
            voidedAtEpochMs = 3_000L,
            localDate = "2026-03-03",
            memo = null,
            volumeMl = null,
            urgency = null,
            hasIncontinence = false
        )
    )

    private fun event(
        localId: String,
        voidedAtEpochMs: Long,
        localDate: String,
        memo: String?,
        volumeMl: Int?,
        urgency: Int?,
        hasIncontinence: Boolean
    ): VoidingEvent = VoidingEvent(
        localId = localId,
        userId = "user-1",
        voidedAtEpochMs = voidedAtEpochMs,
        localDate = localDate,
        isDeleted = false,
        syncState = SyncState.SYNCED,
        updatedAtEpochMs = voidedAtEpochMs,
        memo = memo,
        volumeMl = volumeMl,
        urgency = urgency,
        hasIncontinence = hasIncontinence
    )
}
