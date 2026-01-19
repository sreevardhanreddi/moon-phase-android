package com.vardev.moon_phase.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

private const val TAG = "WidgetBootReceiver"

/**
 * BroadcastReceiver to reschedule widget updates after device boot
 * and handle date/time/timezone changes.
 */
class WidgetBootReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive: action=${intent.action}")
        
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            "android.intent.action.QUICKBOOT_POWERON" -> {
                // Device booted or app updated - reschedule periodic work
                Log.d(TAG, "Device boot/app update detected, scheduling widget updates")
                WidgetUpdateWorker.schedule(context)
            }
            
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_DATE_CHANGED -> {
                // Time/date changed - trigger immediate update
                Log.d(TAG, "Time/date change detected, triggering immediate update")
                WidgetUpdateWorker.scheduleImmediateUpdate(context)
            }
        }
    }
}
