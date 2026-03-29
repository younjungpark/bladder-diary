package com.bladderdiary.app.export

import com.bladderdiary.app.domain.model.VoidingEvent
import java.time.LocalDate as JavaLocalDate
import java.time.temporal.ChronoUnit

object VoidingPdfReportBuilder {
    private const val FOOTER_NOTE =
        "본 PDF는 개인 참고용 기록이며 의료적 진단, 치료 또는 응급 판단을 대체하지 않습니다."

    fun build(
        params: VoidingPdfExportParams,
        events: List<VoidingEvent>,
        generatedAtEpochMs: Long = System.currentTimeMillis()
    ): VoidingPdfReport {
        val sortedEvents = events.sortedBy { it.voidedAtEpochMs }
        val groupedByDate = sortedEvents.groupBy { it.localDate }
        val totalCount = sortedEvents.size
        val volumeValues = sortedEvents.mapNotNull { it.volumeMl }
        val inclusiveDays = calculateInclusiveDays(params.startDate, params.endDate)

        return VoidingPdfReport(
            startDate = params.startDate,
            endDate = params.endDate,
            generatedAtEpochMs = generatedAtEpochMs,
            includeMemo = params.includeMemo,
            totalCount = totalCount,
            totalVolumeMl = volumeValues.sum().takeIf { volumeValues.isNotEmpty() },
            volumeEntryCount = volumeValues.size,
            averageDailyCount = if (inclusiveDays <= 0) 0.0 else totalCount.toDouble() / inclusiveDays,
            dailySummaries = groupedByDate.map { (localDate, dayEvents) ->
                VoidingPdfDailySummary(
                    localDate = localDate,
                    count = dayEvents.size,
                    totalVolumeMl = dayEvents.mapNotNull { it.volumeMl }.sum().takeIf {
                        dayEvents.any { event -> event.volumeMl != null }
                    }
                )
            }.sortedBy { it.localDate },
            details = sortedEvents.map { event ->
                VoidingPdfDetail(
                    localDate = event.localDate,
                    voidedAtEpochMs = event.voidedAtEpochMs,
                    volumeMl = event.volumeMl,
                    urgency = event.urgency,
                    hasIncontinence = event.hasIncontinence,
                    memo = if (params.includeMemo) event.memo else null
                )
            },
            footerNote = FOOTER_NOTE
        )
    }

    private fun calculateInclusiveDays(
        startDate: kotlinx.datetime.LocalDate,
        endDate: kotlinx.datetime.LocalDate
    ): Long {
        val start = JavaLocalDate.parse(startDate.toString())
        val end = JavaLocalDate.parse(endDate.toString())
        return ChronoUnit.DAYS.between(start, end) + 1
    }
}
