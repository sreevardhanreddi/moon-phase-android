package com.vardev.moon_phase.ui.theme

import androidx.compose.runtime.saveable.Saver

enum class NamingMode {
    ENGLISH,
    HINDU;

    fun next(): NamingMode = when (this) {
        ENGLISH -> HINDU
        HINDU -> ENGLISH
    }

    companion object {
        val Saver: Saver<NamingMode, String> = Saver(
            save = { it.name },
            restore = { NamingMode.valueOf(it) }
        )
    }
}
