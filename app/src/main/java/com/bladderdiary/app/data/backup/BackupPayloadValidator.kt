package com.bladderdiary.app.data.backup

import kotlinx.datetime.LocalDate

object BackupPayloadValidator {
    fun validate(payload: BackupPlainPayloadV1, targetUserId: String) {
        if (payload.userId != targetUserId) {
            throw BackupValidationException("현재 사용자와 다른 백업 파일입니다.")
        }
        if (payload.sourceDatabaseVersion <= 0) {
            throw BackupValidationException("백업 데이터베이스 버전이 올바르지 않습니다.")
        }
        payload.records.forEach { record ->
            validateRecord(record)
        }
    }

    private fun validateRecord(record: BackupRecordV1) {
        if (record.localId.isBlank()) {
            throw BackupValidationException("백업 기록 식별자가 비어 있습니다.")
        }
        runCatching { LocalDate.parse(record.localDate) }.getOrElse {
            throw BackupValidationException("백업 기록 날짜 형식이 올바르지 않습니다.")
        }
        if (record.volumeMl != null && record.volumeMl <= 0) {
            throw BackupValidationException("백업 기록의 배뇨량이 올바르지 않습니다.")
        }
        if (record.urgency != null && record.urgency !in 1..5) {
            throw BackupValidationException("백업 기록의 절박감 값이 올바르지 않습니다.")
        }
    }
}
