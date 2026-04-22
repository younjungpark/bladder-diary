package com.bladderdiary.app.presentation.main

import com.bladderdiary.app.domain.model.SyncState
import com.bladderdiary.app.domain.model.VoidingEvent
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.junit.Assert.assertEquals
import org.junit.Test

class MainScreenTimeStateTest {
    @Test
    fun `선택일이 오늘이면 현재 시각을 기본값으로 사용한다`() {
        val now = LocalDateTime(
            date = LocalDate(2026, 3, 29),
            time = LocalTime(hour = 9, minute = 17)
        )

        val state = defaultRecordEditorTime(
            selectedDate = LocalDate(2026, 3, 29),
            now = now
        )

        assertEquals(9, state.hour)
        assertEquals(17, state.minute)
        assertEquals("오전 09:17", state.label)
    }

    @Test
    fun `선택일이 오늘이 아니면 12시를 기본값으로 사용한다`() {
        val now = LocalDateTime(
            date = LocalDate(2026, 3, 29),
            time = LocalTime(hour = 9, minute = 17)
        )

        val state = defaultRecordEditorTime(
            selectedDate = LocalDate(2026, 3, 30),
            now = now
        )

        assertEquals(12, state.hour)
        assertEquals(0, state.minute)
        assertEquals("오후 12:00", state.label)
    }

    @Test
    fun `기존 기록 수정 시 저장된 시각이 편집 상태에 반영된다`() {
        val event = VoidingEvent(
            localId = "event-1",
            userId = "user-1",
            voidedAtEpochMs = LocalDateTime(
                date = LocalDate(2026, 3, 29),
                time = LocalTime(hour = 21, minute = 5)
            ).toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds(),
            localDate = "2026-03-29",
            isDeleted = false,
            syncState = SyncState.SYNCED,
            updatedAtEpochMs = Instant.parse("2026-03-29T12:00:00Z").toEpochMilliseconds(),
            memo = null,
            volumeMl = 200,
            urgency = 3,
            hasIncontinence = false
        )

        val state = event.toRecordEditorTimeState()

        assertEquals(21, state.hour)
        assertEquals(5, state.minute)
        assertEquals("오후 09:05", state.label)
    }
}
