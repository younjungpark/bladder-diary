package com.bladderdiary.app.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.bladderdiary.app.core.AppGraph
import com.bladderdiary.app.data.backup.BackupNetworkException

class BackupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val result = AppGraph.backupRepository.runAutomaticBackup()
        return result.fold(
            onSuccess = { Result.success() },
            onFailure = { error ->
                if (error is BackupNetworkException) {
                    Result.retry()
                } else {
                    Result.failure()
                }
            }
        )
    }

    companion object {
        const val WORK_NAME = "google_drive_backup_work"
    }
}
