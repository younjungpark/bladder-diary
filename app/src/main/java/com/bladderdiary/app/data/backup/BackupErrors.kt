package com.bladderdiary.app.data.backup

sealed class BackupException(message: String, cause: Throwable? = null) : Exception(message, cause)

class BackupValidationException(message: String) : BackupException(message)

class BackupCryptoException(cause: Throwable? = null) :
    BackupException("백업 비밀번호가 올바르지 않거나 백업 파일이 손상되었습니다.", cause)

class BackupMissingLocalKeyException : BackupException("자동 백업에 사용할 로컬 백업 키가 없습니다.")

class BackupNotFoundException : BackupException("저장된 백업 파일을 찾을 수 없습니다.")

class BackupPermissionException(message: String = "Google Drive 백업 권한이 필요합니다.") :
    BackupException(message)

class BackupNetworkException(message: String, cause: Throwable? = null) :
    BackupException(message, cause)
