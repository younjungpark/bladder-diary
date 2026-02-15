package com.bladderdiary.app.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.bladderdiary.app.core.AppGraph

class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val result = AppGraph.voidingRepository.syncPending()
        return result.fold(
            onSuccess = { report ->
                if (report.failCount > 0) {
                    Result.retry()
                } else {
                    Result.success()
                }
            },
            onFailure = { Result.retry() }
        )
    }

    companion object {
        const val WORK_NAME = "voiding_sync_work"
    }
}
