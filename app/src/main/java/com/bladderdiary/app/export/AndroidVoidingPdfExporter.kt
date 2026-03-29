package com.bladderdiary.app.export

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.content.FileProvider
import com.bladderdiary.app.R
import com.bladderdiary.app.domain.model.VoidingEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.ceil
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class AndroidVoidingPdfExporter(
    private val context: Context
) : VoidingPdfExporter {
    override suspend fun export(
        params: VoidingPdfExportParams,
        events: List<VoidingEvent>
    ): Result<VoidingPdfShareFile> = withContext(Dispatchers.IO) {
        runCatching {
            val report = VoidingPdfReportBuilder.build(params, events)
            val exportDir = File(context.cacheDir, EXPORT_DIR_NAME).apply { mkdirs() }
            val fileName = buildFileName(params)
            val outputFile = File(exportDir, fileName)
            val document = PdfDocument()

            try {
                VoidingPdfRenderer(
                    appName = context.getString(R.string.app_name)
                ).render(document, report)

                outputFile.outputStream().use(document::writeTo)
            } finally {
                document.close()
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                outputFile
            )

            VoidingPdfShareFile(
                uriString = uri.toString(),
                fileName = fileName
            )
        }
    }

    private fun buildFileName(params: VoidingPdfExportParams): String {
        val memoSuffix = if (params.includeMemo) "-memo" else ""
        return "bladder-diary-${params.startDate}_to_${params.endDate}$memoSuffix.pdf"
    }

    companion object {
        private const val EXPORT_DIR_NAME = "exports"
    }
}

private class VoidingPdfRenderer(
    private val appName: String
) {
    private val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 18f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        color = 0xFF111827.toInt()
    }
    private val sectionPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 14f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        color = 0xFF1F2937.toInt()
    }
    private val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 10f
        color = 0xFF374151.toInt()
    }
    private val smallPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 9f
        color = 0xFF6B7280.toInt()
    }
    private val tableHeaderPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 10f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        color = 0xFF0F172A.toInt()
    }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f
        color = 0xFFE5E7EB.toInt()
    }

    private lateinit var document: PdfDocument
    private lateinit var page: PdfDocument.Page
    private lateinit var canvas: Canvas
    private var pageNumber = 0
    private var cursorY = 0f

    fun render(document: PdfDocument, report: VoidingPdfReport) {
        this.document = document
        startNewPage()

        drawReportHeader(report)
        drawSummary(report)
        drawDailySummaryTable(report)
        drawDetailsTable(report)
        drawFooter(report.footerNote)

        finishCurrentPage()
    }

    private fun drawReportHeader(report: VoidingPdfReport) {
        ensureSpace(90f)
        drawText("배뇨일기 PDF 보고서", PAGE_MARGIN, cursorY, titlePaint)
        cursorY += 24f
        drawText(appName, PAGE_MARGIN, cursorY, bodyPaint)
        cursorY += 16f
        drawText("기간: ${report.startDate} ~ ${report.endDate}", PAGE_MARGIN, cursorY, bodyPaint)
        cursorY += 14f
        drawText(
            "생성 시각: ${report.generatedAtEpochMs.toDateTimeLabel()}",
            PAGE_MARGIN,
            cursorY,
            bodyPaint
        )
        cursorY += 20f
        drawDivider()
        cursorY += 10f
    }

    private fun drawSummary(report: VoidingPdfReport) {
        ensureSpace(108f)
        drawSectionTitle("요약")
        drawMetricRow("총 배뇨 횟수", "${report.totalCount}회")
        drawMetricRow("총 배뇨량", report.totalVolumeMl?.let { "${it} mL" }.orEmpty())
        drawMetricRow("배뇨량 입력 건수", "${report.volumeEntryCount}건")
        drawMetricRow(
            "1일 평균 횟수",
            String.format(Locale.KOREA, "%.1f회/일", report.averageDailyCount)
        )
        cursorY += 8f
    }

    private fun drawDailySummaryTable(report: VoidingPdfReport) {
        drawSectionTitle("일별 요약")
        drawTableHeader(listOf("날짜" to 230f, "횟수" to 90f, "총 배뇨량" to 155f))
        report.dailySummaries.forEach { row ->
            ensureSpace(24f) {
                drawSectionTitle("일별 요약 (계속)")
                drawTableHeader(listOf("날짜" to 230f, "횟수" to 90f, "총 배뇨량" to 155f))
            }
            drawTableRow(
                values = listOf(
                    row.localDate,
                    "${row.count}회",
                    row.totalVolumeMl?.let { "${it} mL" }.orEmpty()
                ),
                widths = listOf(230f, 90f, 155f)
            )
        }
        cursorY += 10f
    }

    private fun drawDetailsTable(report: VoidingPdfReport) {
        drawSectionTitle("상세 기록")
        val columns = listOf("날짜" to 150f, "시각" to 80f, "절박감" to 70f, "요실금" to 70f, "배뇨량" to 145f)
        drawTableHeader(columns)

        report.details.forEach { row ->
            val rowHeight = measureDetailRowHeight(row, report.includeMemo)
            ensureSpace(rowHeight) {
                drawSectionTitle("상세 기록 (계속)")
                drawTableHeader(columns)
            }
            drawDetailRow(row, report.includeMemo, columns, rowHeight)
        }
    }

    private fun drawFooter(footerNote: String) {
        ensureSpace(44f)
        drawDivider()
        cursorY += 10f
        drawWrappedText(
            text = footerNote,
            x = PAGE_MARGIN,
            top = cursorY,
            width = (PAGE_WIDTH - PAGE_MARGIN * 2).toInt(),
            paint = smallPaint,
            maxLines = 3
        )
    }

    private fun drawMetricRow(label: String, value: String) {
        ensureSpace(16f)
        drawText(label, PAGE_MARGIN, cursorY, bodyPaint)
        drawText(value.ifBlank { "-" }, PAGE_MARGIN + 200f, cursorY, bodyPaint)
        cursorY += 16f
    }

    private fun drawSectionTitle(text: String) {
        ensureSpace(22f)
        drawText(text, PAGE_MARGIN, cursorY, sectionPaint)
        cursorY += 18f
    }

    private fun drawTableHeader(columns: List<Pair<String, Float>>) {
        ensureSpace(22f)
        val startX = PAGE_MARGIN
        var currentX = startX
        columns.forEach { (title, width) ->
            drawText(title, currentX + 4f, cursorY, tableHeaderPaint)
            currentX += width
        }
        cursorY += 8f
        canvas.drawLine(PAGE_MARGIN, cursorY, PAGE_WIDTH - PAGE_MARGIN, cursorY, linePaint)
        cursorY += 12f
    }

    private fun drawTableRow(values: List<String>, widths: List<Float>) {
        val startX = PAGE_MARGIN
        var currentX = startX
        values.zip(widths).forEach { (value, width) ->
            drawText(value.ifBlank { "-" }, currentX + 4f, cursorY, bodyPaint)
            currentX += width
        }
        cursorY += 10f
        canvas.drawLine(PAGE_MARGIN, cursorY, PAGE_WIDTH - PAGE_MARGIN, cursorY, linePaint)
        cursorY += 12f
    }

    private fun drawDetailRow(
        row: VoidingPdfDetail,
        includeMemo: Boolean,
        columns: List<Pair<String, Float>>,
        rowHeight: Float
    ) {
        val dateX = PAGE_MARGIN + 4f
        val timeX = PAGE_MARGIN + columns[0].second + 4f
        val urgencyX = PAGE_MARGIN + columns[0].second + columns[1].second + 4f
        val incontinenceX = PAGE_MARGIN + columns[0].second + columns[1].second + columns[2].second + 4f
        val volumeX = PAGE_MARGIN + columns[0].second + columns[1].second + columns[2].second + columns[3].second + 4f
        val topY = cursorY

        drawText(row.localDate, dateX, topY, bodyPaint)
        drawText(row.voidedAtEpochMs.toTimeLabel(), timeX, topY, bodyPaint)
        drawText(row.urgency?.toString().orEmpty().ifBlank { "-" }, urgencyX, topY, bodyPaint)
        drawText(if (row.hasIncontinence) "있음" else "없음", incontinenceX, topY, bodyPaint)
        drawText(row.volumeMl?.let { "${it} mL" }.orEmpty().ifBlank { "-" }, volumeX, topY, bodyPaint)

        if (includeMemo && !row.memo.isNullOrBlank()) {
            drawWrappedText(
                text = "메모: ${row.memo}",
                x = PAGE_MARGIN + DETAIL_MEMO_INDENT,
                top = topY + 8f,
                width = (PAGE_WIDTH - PAGE_MARGIN * 2 - DETAIL_MEMO_INDENT).toInt(),
                paint = smallPaint,
                maxLines = DETAIL_MEMO_MAX_LINES
            )
        }

        cursorY += rowHeight - 12f
        canvas.drawLine(PAGE_MARGIN, cursorY, PAGE_WIDTH - PAGE_MARGIN, cursorY, linePaint)
        cursorY += 12f
    }

    private fun measureDetailRowHeight(
        row: VoidingPdfDetail,
        includeMemo: Boolean
    ): Float {
        if (!includeMemo || row.memo.isNullOrBlank()) return 24f

        val memoHeight = measureWrappedTextHeight(
            text = "메모: ${row.memo}",
            width = (PAGE_WIDTH - PAGE_MARGIN * 2 - DETAIL_MEMO_INDENT).toInt(),
            paint = smallPaint,
            maxLines = DETAIL_MEMO_MAX_LINES
        )
        return 24f + memoHeight + 8f
    }

    private fun ensureSpace(requiredHeight: Float, onPageBreak: (() -> Unit)? = null) {
        if (cursorY + requiredHeight <= PAGE_HEIGHT - PAGE_MARGIN) return
        finishCurrentPage()
        startNewPage()
        onPageBreak?.invoke()
    }

    private fun startNewPage() {
        pageNumber += 1
        page = document.startPage(
            PdfDocument.PageInfo.Builder(PAGE_WIDTH.toInt(), PAGE_HEIGHT.toInt(), pageNumber).create()
        )
        canvas = page.canvas
        cursorY = PAGE_MARGIN
        drawText(appName, PAGE_MARGIN, cursorY, smallPaint)
        drawText("페이지 $pageNumber", PAGE_WIDTH - PAGE_MARGIN - 50f, cursorY, smallPaint)
        cursorY += 20f
    }

    private fun finishCurrentPage() {
        document.finishPage(page)
    }

    private fun drawText(text: String, x: Float, y: Float, paint: TextPaint) {
        canvas.drawText(text, x, y, paint)
    }

    private fun drawDivider() {
        canvas.drawLine(PAGE_MARGIN, cursorY, PAGE_WIDTH - PAGE_MARGIN, cursorY, linePaint)
    }

    private fun drawWrappedText(
        text: String,
        x: Float,
        top: Float,
        width: Int,
        paint: TextPaint,
        maxLines: Int
    ): Int {
        val lines = wrapText(text, width.toFloat(), paint, maxLines)
        val lineHeight = paint.fontSpacing.takeIf { it > 0f }
            ?: (paint.fontMetrics.descent - paint.fontMetrics.ascent)
        var baselineY = top - paint.fontMetrics.ascent
        lines.forEach { line ->
            canvas.drawText(line, x, baselineY, paint)
            baselineY += lineHeight
        }
        return ceil(lines.size * lineHeight).toInt()
    }

    private fun measureWrappedTextHeight(
        text: String,
        width: Int,
        paint: TextPaint,
        maxLines: Int
    ): Float {
        val lines = wrapText(text, width.toFloat(), paint, maxLines)
        val lineHeight = paint.fontSpacing.takeIf { it > 0f }
            ?: (paint.fontMetrics.descent - paint.fontMetrics.ascent)
        return lines.size * lineHeight
    }

    private fun wrapText(
        text: String,
        maxWidth: Float,
        paint: TextPaint,
        maxLines: Int
    ): List<String> {
        if (maxLines <= 0) return emptyList()

        val source = text.ifBlank { "-" }.replace("\r\n", "\n")
        val lines = mutableListOf<String>()
        val paragraphs = source.split('\n')

        paragraphs.forEachIndexed { paragraphIndex, paragraph ->
            if (lines.size >= maxLines) return@forEachIndexed
            if (paragraph.isEmpty()) {
                lines += ""
                return@forEachIndexed
            }

            var remaining = paragraph
            while (remaining.isNotEmpty() && lines.size < maxLines) {
                var count = paint.breakText(remaining, true, maxWidth, null).coerceAtLeast(1)
                var candidate = remaining.substring(0, count)

                if (count < remaining.length) {
                    val softBreak = candidate.lastIndexOfAny(charArrayOf(' ', '\t'))
                    if (softBreak > 0) {
                        count = softBreak + 1
                        candidate = remaining.substring(0, count)
                    }
                }

                val isLastAvailableLine = lines.size == maxLines - 1
                val hasMoreParagraphText = count < remaining.length
                val hasMoreParagraphs = paragraphIndex < paragraphs.lastIndex
                if (isLastAvailableLine && (hasMoreParagraphText || hasMoreParagraphs)) {
                    val tail = if (hasMoreParagraphText) {
                        candidate + remaining.substring(count)
                    } else {
                        candidate
                    }
                    lines += TextUtils.ellipsize(
                        tail.trim(),
                        paint,
                        maxWidth,
                        TextUtils.TruncateAt.END
                    ).toString()
                    break
                }

                lines += candidate.trimEnd()
                remaining = remaining.substring(count).trimStart()
            }
        }

        return lines.ifEmpty { listOf("-") }
    }

    companion object {
        private const val PAGE_WIDTH = 595f
        private const val PAGE_HEIGHT = 842f
        private const val PAGE_MARGIN = 40f
        private const val DETAIL_MEMO_MAX_LINES = 6
        private const val DETAIL_MEMO_INDENT = 16f
    }
}

private fun Long.toDateTimeLabel(): String {
    return Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.KOREA))
}

private fun Long.toTimeLabel(): String {
    return Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .toLocalTime()
        .format(DateTimeFormatter.ofPattern("HH:mm", Locale.KOREA))
}
