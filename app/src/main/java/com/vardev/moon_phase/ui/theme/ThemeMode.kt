package com.vardev.moon_phase.ui.theme

import androidx.compose.runtime.saveable.Saver

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK;

    fun next(): ThemeMode = when (this) {
        SYSTEM -> LIGHT
        LIGHT -> DARK
        DARK -> SYSTEM
    }

    companion object {
        val Saver: Saver<ThemeMode, String> = Saver(
            save = { it.name },
            restore = { ThemeMode.valueOf(it) }
        )
    }
}
