package com.vardev.moon_phase.data

import android.content.Context
import android.content.SharedPreferences
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import com.vardev.moon_phase.ui.theme.NamingMode
import com.vardev.moon_phase.ui.theme.ThemeMode
import com.vardev.moon_phase.widget.MoonPhaseWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object PreferencesManager {
    private const val PREFS_NAME = "moon_phase_prefs"
    private const val KEY_THEME_MODE = "theme_mode"
    private const val KEY_NAMING_MODE = "naming_mode"
    private const val KEY_SELECTED_DATE = "selected_date"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getThemeMode(context: Context): ThemeMode {
        val value = getPrefs(context).getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name)
        return try {
            ThemeMode.valueOf(value ?: ThemeMode.SYSTEM.name)
        } catch (e: IllegalArgumentException) {
            ThemeMode.SYSTEM
        }
    }

    fun setThemeMode(context: Context, mode: ThemeMode) {
        getPrefs(context).edit().putString(KEY_THEME_MODE, mode.name).apply()
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
        getPrefs(context).edit().putString(KEY_NAMING_MODE, mode.name).apply()
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
            .apply()
        updateWidgets(context)
    }

    private fun updateWidgets(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            MoonPhaseWidget().updateAll(context)
        }
    }
}
