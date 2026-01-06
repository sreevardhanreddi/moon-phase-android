package com.vardev.moon_phase.data

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.vardev.moon_phase.ui.theme.NamingMode
import com.vardev.moon_phase.ui.theme.ThemeMode
import com.vardev.moon_phase.widget.MoonPhaseWidget
import com.vardev.moon_phase.widget.Widget1x1Receiver
import com.vardev.moon_phase.widget.Widget2x1Receiver
import com.vardev.moon_phase.widget.Widget2x2Receiver
import com.vardev.moon_phase.widget.Widget3x1Receiver
import com.vardev.moon_phase.widget.Widget4x1Receiver
import com.vardev.moon_phase.widget.Widget4x2Receiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object PreferencesManager {
    private const val TAG = "MoonPhasePrefs"
    private const val PREFS_NAME = "moon_phase_prefs"
    private const val KEY_THEME_MODE = "theme_mode"
    private const val KEY_NAMING_MODE = "naming_mode"
    private const val KEY_SELECTED_DATE = "selected_date"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getThemeMode(context: Context): ThemeMode {
        val value = getPrefs(context).getString(KEY_THEME_MODE, ThemeMode.DARK.name)
        return try {
            ThemeMode.valueOf(value ?: ThemeMode.DARK.name)
        } catch (e: IllegalArgumentException) {
            ThemeMode.DARK
        }
    }

    fun setThemeMode(context: Context, mode: ThemeMode) {
        Log.d(TAG, "setThemeMode: Setting theme to ${mode.name}")
        val success = getPrefs(context).edit().putString(KEY_THEME_MODE, mode.name).commit()
        Log.d(TAG, "setThemeMode: Commit success = $success")
        updateWidgets(context)
    }

    fun getNamingMode(context: Context): NamingMode {
        val value = getPrefs(context).getString(KEY_NAMING_MODE, NamingMode.ENGLISH.name)
        return try {
            NamingMode.valueOf(value ?: NamingMode.ENGLISH.name)
        } catch (e: IllegalArgumentException) {
            NamingMode.ENGLISH
        }
    }

    fun setNamingMode(context: Context, mode: NamingMode) {
        Log.d(TAG, "setNamingMode: Setting naming mode to ${mode.name}")
        val success = getPrefs(context).edit().putString(KEY_NAMING_MODE, mode.name).commit()
        Log.d(TAG, "setNamingMode: Commit success = $success")
        updateWidgets(context)
    }

    fun getSelectedDate(context: Context): LocalDate {
        val value = getPrefs(context).getString(KEY_SELECTED_DATE, null)
        return if (value != null) {
            try {
                LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE)
            } catch (e: Exception) {
                LocalDate.now()
            }
        } else {
            LocalDate.now()
        }
    }

    fun setSelectedDate(context: Context, date: LocalDate) {
        getPrefs(context).edit()
            .putString(KEY_SELECTED_DATE, date.format(DateTimeFormatter.ISO_LOCAL_DATE))
            .commit()
        updateWidgets(context)
    }

    // Key to force widget refresh by changing state
    private val REFRESH_KEY = longPreferencesKey("refresh_timestamp")

    /**
     * Force sync all widgets - can be called from the app UI
     */
    fun syncWidgets(context: Context) {
        Log.d(TAG, "syncWidgets: Manual sync triggered")
        updateWidgets(context)
    }

    private fun updateWidgets(context: Context) {
        Log.d(TAG, "updateWidgets: Starting widget update")
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // First, force native Android widget update via broadcast
                forceNativeWidgetUpdate(appContext)

                val widget = MoonPhaseWidget()
                val manager = GlanceAppWidgetManager(appContext)
                val glanceIds = manager.getGlanceIds(MoonPhaseWidget::class.java)
                Log.d(TAG, "updateWidgets: Found ${glanceIds.size} widget instances")

                // Force state change for each widget to trigger refresh
                val timestamp = System.currentTimeMillis()
                glanceIds.forEach { glanceId ->
                    Log.d(TAG, "updateWidgets: Forcing state change for widget $glanceId with timestamp $timestamp")
                    updateAppWidgetState(appContext, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                        prefs.toMutablePreferences().apply {
                            this[REFRESH_KEY] = timestamp
                        }
                    }
                }

                Log.d(TAG, "updateWidgets: Calling updateAll")
                widget.updateAll(appContext)
                Log.d(TAG, "updateWidgets: updateAll completed")
            } catch (e: Exception) {
                Log.e(TAG, "updateWidgets: Error updating widgets", e)
                // Fallback: try simple updateAll
                try {
                    Log.d(TAG, "updateWidgets: Trying simple fallback")
                    MoonPhaseWidget().updateAll(appContext)
                    Log.d(TAG, "updateWidgets: Fallback succeeded")
                } catch (e2: Exception) {
                    Log.e(TAG, "updateWidgets: Fallback also failed", e2)
                }
            }
        }
    }

    /**
     * Force native Android widget update by sending UPDATE broadcasts to all widget receivers
     */
    private fun forceNativeWidgetUpdate(context: Context) {
        Log.d(TAG, "forceNativeWidgetUpdate: Starting native update")
        val appWidgetManager = AppWidgetManager.getInstance(context)

        // List of all widget receiver classes
        val receivers = listOf(
            Widget1x1Receiver::class.java,
            Widget2x1Receiver::class.java,
            Widget3x1Receiver::class.java,
            Widget4x1Receiver::class.java,
            Widget2x2Receiver::class.java,
            Widget4x2Receiver::class.java
        )

        receivers.forEach { receiverClass ->
            try {
                val componentName = ComponentName(context, receiverClass)
                val widgetIds = appWidgetManager.getAppWidgetIds(componentName)
                Log.d(TAG, "forceNativeWidgetUpdate: Found ${widgetIds.size} widgets for ${receiverClass.simpleName}")

                if (widgetIds.isNotEmpty()) {
                    // Send explicit UPDATE broadcast
                    val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE).apply {
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds)
                        component = componentName
                    }
                    context.sendBroadcast(intent)
                    Log.d(TAG, "forceNativeWidgetUpdate: Sent update broadcast for ${receiverClass.simpleName}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "forceNativeWidgetUpdate: Error updating ${receiverClass.simpleName}", e)
            }
        }
    }
}
