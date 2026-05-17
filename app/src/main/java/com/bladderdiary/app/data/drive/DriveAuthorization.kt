package com.bladderdiary.app.data.drive

import android.app.PendingIntent
import android.content.Intent

const val DRIVE_APP_DATA_SCOPE = "https://www.googleapis.com/auth/drive.appdata"

data class DriveAuthorizationToken(val accessToken: String, val expiresAtEpochMs: Long? = null)

sealed class DriveAuthorizationResult {
    data class Authorized(val token: DriveAuthorizationToken) : DriveAuthorizationResult()
    data class RequiresUserResolution(
        val pendingIntent: PendingIntent,
        val reason: String = "Google Drive 권한 동의가 필요합니다."
    ) : DriveAuthorizationResult()

    data class Unavailable(val reason: String) : DriveAuthorizationResult()
    data object Denied : DriveAuthorizationResult()
}

interface DriveAuthorizationClient {
    suspend fun authorizeDriveAppData(): DriveAuthorizationResult
    fun parseAuthorizationResult(data: Intent?): DriveAuthorizationResult
    suspend fun clearCachedToken()
}
