package com.bladderdiary.app.export

import com.bladderdiary.app.domain.model.VoidingEvent
import kotlinx.datetime.LocalDate

data class VoidingPdfExportParams(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val includeMemo: Boolean
)

data class VoidingPdfShareFile(val uriString: String, val fileName: String)

data class VoidingPdfReport(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val generatedAtEpochMs: Long,
    val includeMemo: Boolean,
    val totalCount: Int,
    val totalVolumeMl: Int?,
    val volumeEntryCount: Int,
    val averageDailyCount: Double,
    val dailySummaries: List<VoidingPdfDailySummary>,
    val details: List<VoidingPdfDetail>,
    val footerNote: String
)

data class VoidingPdfDailySummary(val localDate: String, val count: Int, val totalVolumeMl: Int?)

data class VoidingPdfDetail(
    val localDate: String,
    val voidedAtEpochMs: Long,
    val volumeMl: Int?,
    val urgency: Int?,
    val hasIncontinence: Boolean,
    val memo: String?
)

interface VoidingPdfExporter {
    suspend fun export(
        params: VoidingPdfExportParams,
        events: List<VoidingEvent>
    ): Result<VoidingPdfShareFile>
}
