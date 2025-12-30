package com.vardev.moon_phase.model

import java.time.LocalDate

data class MoonPhaseData(
    val date: LocalDate,
    val moonAge: Double,
    val phase: Double,
    val illumination: Double,
    val phaseName: String,
    val distanceKm: Double,
    val nextNewMoon: LocalDate,
    val nextFullMoon: LocalDate,
    val tithi: TithiData
) {
    val illuminationPercent: Int
        get() = (illumination * 100).toInt()

    val formattedDistance: String
        get() = "%,.0f km".format(distanceKm)
}

data class TithiData(
    val lunarDay: Int,
    val tithiName: String,
    val paksha: String,
    val isSpecialDay: Boolean,
    val specialDayType: String?
) {
    val displayTithi: String
        get() = if (lunarDay <= 15) {
            "$tithiName (Day $lunarDay)"
        } else {
            "$tithiName (Day ${lunarDay - 15})"
        }
}
