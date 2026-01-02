package com.vardev.moon_phase

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.vardev.moon_phase.data.PreferencesManager
import com.vardev.moon_phase.ui.screens.HomeScreen
import com.vardev.moon_phase.ui.theme.MoonphaseTheme
import com.vardev.moon_phase.ui.theme.NamingMode
import com.vardev.moon_phase.ui.theme.ThemeMode
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Load saved preferences
        val savedThemeMode = PreferencesManager.getThemeMode(this)
        val savedNamingMode = PreferencesManager.getNamingMode(this)
        // Always use current date instead of saved date
        // val savedDate = PreferencesManager.getSelectedDate(this)

        setContent {
            var themeMode by rememberSaveable(stateSaver = ThemeMode.Saver) { mutableStateOf(savedThemeMode) }
            var namingMode by rememberSaveable(stateSaver = NamingMode.Saver) { mutableStateOf(savedNamingMode) }
            // Always use current date
            var selectedDate by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }

            MoonphaseTheme(themeMode = themeMode) {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    HomeScreen(
                        modifier = Modifier.padding(innerPadding),
                        themeMode = themeMode,
                        onThemeToggle = {
                            val newMode = themeMode.next()
                            themeMode = newMode
                            PreferencesManager.setThemeMode(this@MainActivity, newMode)
                        },
                        namingMode = namingMode,
                        onNamingModeToggle = {
                            val newMode = namingMode.next()
                            namingMode = newMode
                            PreferencesManager.setNamingMode(this@MainActivity, newMode)
                        },
                        selectedDate = LocalDate.parse(selectedDate),
                        // Arrow navigation for date selection
                        onDateSelected = { date ->
                            selectedDate = date.toString()
                        }
                    )
                }
            }
        }
    }
}
