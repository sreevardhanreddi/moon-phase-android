package com.vardev.moon_phase.widget

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.updateAll
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

private const val TAG = "WidgetUpdateWorker"

class WidgetUpdateWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "doWork: Starting widget update")
        return try {
            MoonPhaseWidget().updateAll(applicationContext)
            Log.d(TAG, "doWork: Widget update successful")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "doWork: Widget update failed", e)
            Result.retry()
        }
    }

    companion object {
        private const val PERIODIC_WORK_NAME = "moon_phase_widget_periodic_update"
        private const val IMMEDIATE_WORK_NAME = "moon_phase_widget_immediate_update"

        fun schedule(context: Context) {
            Log.d(TAG, "schedule: Scheduling widget updates")
            
            // First, trigger an immediate update
            scheduleImmediateUpdate(context)
            
            // Then schedule periodic updates every hour
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(false) // Allow updates even on low battery
                .build()
            
            val periodicWorkRequest = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(
                1, TimeUnit.HOURS,
                15, TimeUnit.MINUTES // Flex interval for better scheduling
            )
                .setConstraints(constraints)
                .addTag("moon_phase_widget")
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP, // Keep existing work to avoid resetting schedule
                periodicWorkRequest
            )
            
            Log.d(TAG, "schedule: Periodic work scheduled")
        }
        
        fun scheduleImmediateUpdate(context: Context) {
            Log.d(TAG, "scheduleImmediateUpdate: Triggering immediate widget update")
            
            val immediateWorkRequest = OneTimeWorkRequestBuilder<WidgetUpdateWorker>()
                .addTag("moon_phase_widget_immediate")
                .build()
            
            WorkManager.getInstance(context).enqueueUniqueWork(
                IMMEDIATE_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                immediateWorkRequest
            )
        }
        
        fun cancelAll(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_WORK_NAME)
            WorkManager.getInstance(context).cancelUniqueWork(IMMEDIATE_WORK_NAME)
        }
    }
}
