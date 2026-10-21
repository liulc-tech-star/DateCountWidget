package com.datecount.widget.widget

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Operation
import androidx.work.WorkerParameters
import androidx.work.WorkManager
import androidx.work.await
import com.datecount.widget.asDateCountApp
import kotlinx.coroutines.CancellationException
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

class WidgetRefreshWorker(context: Context, parameters: WorkerParameters) :
    CoroutineWorker(context, parameters) {

    override suspend fun doWork(): Result {
        if (DateCountWidgetReceiver.widgetIds(applicationContext).isEmpty()) return Result.success()
        val refreshed = try {
            DateCountWidgetReceiver.refreshAllNow(applicationContext)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Log.e(TAG, "Unable to refresh widgets", error)
            false
        }

        if (refreshed) {
            scheduleNextMidnight()
            return Result.success()
        }
        if (shouldRetryRefresh(runAttemptCount)) return Result.retry()

        // Out of retries. The next midnight is appended as a continuation of this work,
        // and WorkManager fails every continuation of a failed work — so this attempt has
        // to report success, otherwise the daily chain dies with it.
        Log.w(TAG, "Refresh failed after $runAttemptCount attempts; resuming at next midnight")
        scheduleNextMidnight()
        return Result.success()
    }

    private suspend fun scheduleNextMidnight() {
        try {
            // Append the next midnight; replacing this work here would cancel the current worker.
            schedule(applicationContext, ExistingWorkPolicy.APPEND_OR_REPLACE).await()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Log.e(TAG, "Unable to schedule the next midnight refresh", error)
        }
    }

    companion object {
        private const val TAG = "WidgetRefreshWorker"
        private const val WORK_NAME = "date_count_widget_midnight_refresh"
        private const val LEGACY_PERIODIC_WORK_NAME = "date_count_widget_refresh"

        fun schedule(
            context: Context,
            policy: ExistingWorkPolicy = ExistingWorkPolicy.KEEP,
            immediate: Boolean = false
        ): Operation {
            if (DateCountWidgetReceiver.widgetIds(context).isEmpty()) return cancel(context)
            val delayMillis = if (immediate) 0L else nextMidnightDelayMillis(ZonedDateTime.now())
            val request = OneTimeWorkRequestBuilder<WidgetRefreshWorker>()
                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                .build()
            return WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                WORK_NAME,
                policy,
                request
            )
        }

        fun rescheduleAfterClockChange(context: Context): Operation {
            val bindings = context.asDateCountApp().widgetBindings
            val today = LocalDate.now()
            val changedDate = DateCountWidgetReceiver.widgetIds(context).any {
                shouldRefreshWidget(bindings.lastRefreshDayFor(it), today)
            }
            return schedule(context, ExistingWorkPolicy.REPLACE, immediate = changedDate)
        }

        fun cancel(context: Context): Operation =
            WorkManager.getInstance(context.applicationContext).cancelUniqueWork(WORK_NAME)

        fun cancelLegacyPeriodicWork(context: Context) {
            WorkManager.getInstance(context.applicationContext)
                .cancelUniqueWork(LEGACY_PERIODIC_WORK_NAME)
        }
    }
}
