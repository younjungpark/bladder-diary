package com.bladderdiary.app.data.drive

const val DRIVE_APP_DATA_SCOPE = "https://www.googleapis.com/auth/drive.appdata"

data class DriveAuthorizationToken(val accessToken: String, val expiresAtEpochMs: Long? = null)

sealed class DriveAuthorizationResult {
    data class Authorized(val token: DriveAuthorizationToken) : DriveAuthorizationResult()
    data class RequiresUserResolution(val reason: String) : DriveAuthorizationResult()
    data class Unavailable(val reason: String) : DriveAuthorizationResult()
    data object Denied : DriveAuthorizationResult()
}

interface DriveAuthorizationClient {
    suspend fun authorizeDriveAppData(): DriveAuthorizationResult
    suspend fun clearCachedToken()
}
