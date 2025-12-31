package com.vardev.moon_phase.ui.theme

import androidx.compose.runtime.saveable.Saver

enum class ThemeMode {
    LIGHT,
    DARK;

    fun next(): ThemeMode = when (this) {
        LIGHT -> DARK
        DARK -> LIGHT
    }

    companion object {
        val Saver: Saver<ThemeMode, String> = Saver(
            save = { it.name },
            restore = { ThemeMode.valueOf(it) }
        )
    }
}
