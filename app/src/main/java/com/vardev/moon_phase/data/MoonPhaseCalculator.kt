package com.vardev.moon_phase.data

import com.vardev.moon_phase.model.MoonPhaseData
import com.vardev.moon_phase.model.TithiData
import java.time.LocalDate
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

/**
 * Moon phase calculator using simplified synodic month approximation.
 * 
 * DISCLAIMER: These calculations are APPROXIMATIONS based on the average
 * synodic month cycle (29.53 days). Results may differ by ±1 day from
 * precise astronomical calculations which account for orbital perturbations,
 * lunar anomaly, and other factors.
 * 
 * For religious observances, please consult a traditional Panchang or
 * local calendar for accurate Tithi timings.
 */
object MoonPhaseCalculator {

    // Average synodic month (time between new moons) - approximation
    private const val SYNODIC_MONTH = 29.53058867
    // Reference new moon: January 6, 2000 (Julian Date)
    private const val REFERENCE_NEW_MOON_JD = 2451549.5
    // Average Earth-Moon distance (approximate)
    private const val AVG_DISTANCE_KM = 384400.0
    // Distance variation (simplified sine approximation)
    private const val DISTANCE_VARIATION_KM = 25000.0

    fun calculate(date: LocalDate): MoonPhaseData {
        val julianDate = calculateJulianDate(date.year, date.monthValue, date.dayOfMonth)
        val daysSinceNew = julianDate - REFERENCE_NEW_MOON_JD
        val moonAge = ((daysSinceNew % SYNODIC_MONTH) + SYNODIC_MONTH) % SYNODIC_MONTH
        val phase = moonAge / SYNODIC_MONTH
        val illumination = (1 - cos(phase * 2 * PI)) / 2
        val phaseName = getPhaseName(phase)
        val distance = AVG_DISTANCE_KM + sin(phase * 2 * PI) * DISTANCE_VARIATION_KM
        val lunarDay = (floor(moonAge).toInt() % 30) + 1
        val tithi = calculateTithi(lunarDay)
        val nextNewMoon = calculateNextPhase(phase, 0.0, date)
        val nextFullMoon = calculateNextPhase(phase, 0.5, date)

        return MoonPhaseData(
            date = date,
            moonAge = moonAge,
            phase = phase,
            illumination = illumination,
            phaseName = phaseName,
            distanceKm = distance,
            nextNewMoon = nextNewMoon,
            nextFullMoon = nextFullMoon,
            tithi = tithi
        )
    }

    private fun calculateJulianDate(year: Int, month: Int, day: Int): Double {
        return 367.0 * year -
                floor(7.0 * (year + floor((month + 9.0) / 12.0)) / 4.0) +
                floor(275.0 * month / 9.0) +
                day + 1721013.5
    }

    private fun getPhaseName(phase: Double): String {
        return when {
            phase < 0.033 || phase > 0.967 -> "New Moon"
            phase < 0.216 -> "Waxing Crescent"
            phase < 0.283 -> "First Quarter"
            phase < 0.466 -> "Waxing Gibbous"
            phase < 0.533 -> "Full Moon"
            phase < 0.716 -> "Waning Gibbous"
            phase < 0.783 -> "Last Quarter"
            else -> "Waning Crescent"
        }
    }

    private fun calculateNextPhase(currentPhase: Double, targetPhase: Double, fromDate: LocalDate): LocalDate {
        val daysUntil = if (targetPhase > currentPhase) {
            (targetPhase - currentPhase) * SYNODIC_MONTH
        } else {
            (1 - currentPhase + targetPhase) * SYNODIC_MONTH
        }
        return fromDate.plusDays(daysUntil.toLong())
    }

    private fun calculateTithi(lunarDay: Int): TithiData {
        val tithiNames = mapOf(
            1 to "Prathama (Padyami)",
            2 to "Dwitiya (Vidiya)",
            3 to "Tritiya (Tadiya)",
            4 to "Chaturthi (Chavithi)",
            5 to "Panchami",
            6 to "Shashthi",
            7 to "Saptami",
            8 to "Ashtami",
            9 to "Navami",
            10 to "Dashami",
            11 to "Ekadashi",
            12 to "Dwadashi",
            13 to "Trayodashi",
            14 to "Chaturdashi",
            15 to "Purnima (Pournami)",
            16 to "Prathama (Padyami)",
            17 to "Dwitiya (Vidiya)",
            18 to "Tritiya (Tadiya)",
            19 to "Chaturthi (Chavithi)",
            20 to "Panchami",
            21 to "Shashthi",
            22 to "Saptami",
            23 to "Ashtami",
            24 to "Navami",
            25 to "Dashami",
            26 to "Ekadashi",
            27 to "Dwadashi",
            28 to "Trayodashi",
            29 to "Chaturdashi",
            30 to "Amavasya"
        )

        val paksha = if (lunarDay <= 15) "Shukla Paksha" else "Krishna Paksha"
        val tithiName = tithiNames[lunarDay] ?: "Unknown"

        val (isSpecialDay, specialDayType) = when {
            lunarDay == 11 || lunarDay == 26 -> true to "Ekadashi"
            lunarDay == 4 || lunarDay == 19 -> (lunarDay == 19) to if (lunarDay == 19) "Chaturthi" else null
            lunarDay == 14 || lunarDay == 29 -> true to "Chaturdashi"
            lunarDay == 15 -> true to "Purnima"
            lunarDay == 30 -> true to "Amavasya"
            else -> false to null
        }

        return TithiData(
            lunarDay = lunarDay,
            tithiName = tithiName,
            paksha = paksha,
            isSpecialDay = isSpecialDay,
            specialDayType = specialDayType
        )
    }

    fun isWaxing(lunarDay: Int): Boolean = lunarDay <= 15
}
