package com.datecount.widget.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.await
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

class DateChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                val pendingResult = goAsync()
                val appContext = context.applicationContext
                CoroutineScope(Dispatchers.Default).launch {
                    try {
                        withTimeout(8_000L) {
                            WidgetRefreshWorker.rescheduleAfterClockChange(appContext).await()
                        }
                    } catch (error: TimeoutCancellationException) {
                        Log.w("DateChangeReceiver", "Scheduling timed out", error)
                    } catch (error: CancellationException) {
                        throw error
                    } catch (error: Exception) {
                        Log.e("DateChangeReceiver", "Unable to schedule daily refresh", error)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }
}
