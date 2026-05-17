package com.bladderdiary.app.data.drive

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class AndroidDriveAuthorizationClient(context: Context) : DriveAuthorizationClient {
    private val authorizationClient = Identity.getAuthorizationClient(context.applicationContext)

    override suspend fun authorizeDriveAppData(): DriveAuthorizationResult = runCatching {
        authorizationClient.authorize(appDataAuthorizationRequest()).awaitTask()
    }.fold(
        onSuccess = ::toDriveAuthorizationResult,
        onFailure = { error -> error.toDriveAuthorizationFailure() }
    )

    override fun parseAuthorizationResult(data: Intent?): DriveAuthorizationResult {
        if (data == null) return DriveAuthorizationResult.Denied
        return runCatching {
            authorizationClient.getAuthorizationResultFromIntent(data)
        }.fold(
            onSuccess = ::toDriveAuthorizationResult,
            onFailure = { error -> error.toDriveAuthorizationFailure() }
        )
    }

    override suspend fun clearCachedToken() = Unit

    private fun toDriveAuthorizationResult(result: AuthorizationResult): DriveAuthorizationResult {
        val accessToken = result.accessToken
        Log.d(
            TAG,
            "AuthorizationResult hasToken=${!accessToken.isNullOrBlank()} " +
                "hasResolution=${result.hasResolution()}"
        )
        return when {
            !accessToken.isNullOrBlank() -> DriveAuthorizationResult.Authorized(
                DriveAuthorizationToken(accessToken)
            )

            result.hasResolution() && result.pendingIntent != null -> {
                DriveAuthorizationResult.RequiresUserResolution(
                    pendingIntent = result.pendingIntent!!
                )
            }

            else -> DriveAuthorizationResult.Unavailable(
                reason = "Google Drive 권한 토큰을 받을 수 없습니다."
            )
        }
    }

    private fun Throwable.toDriveAuthorizationFailure(): DriveAuthorizationResult {
        Log.d(TAG, "Authorization failed: $message", this)
        if (this is ApiException) {
            if (statusCode == USER_DENIED_STATUS_CODE) {
                return DriveAuthorizationResult.Denied
            }
            if (statusCode == UNREGISTERED_ON_API_CONSOLE_STATUS_CODE &&
                message?.contains(UNREGISTERED_ON_API_CONSOLE_REASON) == true
            ) {
                return DriveAuthorizationResult.Unavailable(
                    reason = "Google Drive 백업 설정을 확인할 수 없습니다. 앱 업데이트 후 다시 시도해 주세요."
                )
            }
        }
        return DriveAuthorizationResult.Unavailable(
            reason = message ?: "Google Drive 권한 확인 중 오류가 발생했습니다."
        )
    }

    private fun appDataAuthorizationRequest(): AuthorizationRequest = AuthorizationRequest.builder()
        .setRequestedScopes(listOf(Scope(DRIVE_APP_DATA_SCOPE)))
        .build()

    private companion object {
        private const val TAG = "DriveAuthorization"
        private const val USER_DENIED_STATUS_CODE = 16
        private const val UNREGISTERED_ON_API_CONSOLE_STATUS_CODE = 8
        private const val UNREGISTERED_ON_API_CONSOLE_REASON = "UNREGISTERED_ON_API_CONSOLE"
    }
}

private suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { result ->
        if (continuation.isActive) {
            continuation.resume(result)
        }
    }
    addOnFailureListener { error ->
        if (continuation.isActive) {
            continuation.resumeWithException(error)
        }
    }
    addOnCanceledListener {
        continuation.cancel()
    }
}
