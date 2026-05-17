package com.xpotrack.app.data.quick

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.xpotrack.app.XpApp
import java.util.concurrent.TimeUnit

// Best-effort daily sweep of expired quick notes. The on-open sweep in the
// Quick screen VM is the source of truth — this is here so the strip on the
// notes list shows fresh counts even if the user never opens Quick.
class QuickNoteSweepWorker(
    ctx: Context,
    params: WorkerParameters,
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as XpApp
        app.quickNotesRepo.sweepExpired()
        return Result.success()
    }

    companion object {
        private const val NAME = "quick_note_sweep"

        fun enqueue(ctx: Context) {
            val req = PeriodicWorkRequestBuilder<QuickNoteSweepWorker>(24, TimeUnit.HOURS)
                .build()
            WorkManager.getInstance(ctx)
                .enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.KEEP, req)
        }
    }
}
