package com.bladderdiary.app.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

interface BackupWorkScheduler {
    fun request()
    fun cancel()
}

class BackupScheduler(context: Context) : BackupWorkScheduler {
    private val workManager = WorkManager.getInstance(context)

    override fun request() {
        val request = OneTimeWorkRequestBuilder<BackupWorker>()
            .setInitialDelay(BACKUP_DEBOUNCE_MINUTES, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        workManager.enqueueUniqueWork(
            BackupWorker.WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    override fun cancel() {
        workManager.cancelUniqueWork(BackupWorker.WORK_NAME)
    }

    private companion object {
        private const val BACKUP_DEBOUNCE_MINUTES = 30L
    }
}
