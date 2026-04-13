package com.vardev.moon_phase.data

import com.vardev.moon_phase.model.MoonPhaseData
import com.vardev.moon_phase.model.TithiData
import org.shredzone.commons.suncalc.MoonIllumination
import org.shredzone.commons.suncalc.MoonPhase
import org.shredzone.commons.suncalc.MoonPosition
import org.shredzone.commons.suncalc.MoonTimes
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.floor

/**
 * Moon phase calculator using commons-suncalc library for accurate astronomical calculations.
 * Uses Delhi, India coordinates for location-based calculations.
 */
object MoonPhaseCalculator {

    // Delhi, India coordinates
    private const val LATITUDE = 28.6139
    private const val LONGITUDE = 77.2090

    // India Standard Time zone
    private val IST_ZONE = ZoneId.of("Asia/Kolkata")

    // Average synodic month for tithi calculations
    private const val SYNODIC_MONTH = 29.53058867

    fun calculate(date: LocalDate): MoonPhaseData {
        val dateTime = date.atStartOfDay(IST_ZONE)

        // Get moon illumination data (phase, fraction, angle)
        val illumination = MoonIllumination.compute()
            .on(dateTime)
            .execute()

        // Get moon position (distance, altitude, azimuth)
        val position = MoonPosition.compute()
            .on(dateTime)
            .at(LATITUDE, LONGITUDE)
            .execute()

        // Get next new moon
        val nextNewMoonTime = MoonPhase.compute()
            .phase(MoonPhase.Phase.NEW_MOON)
            .on(dateTime)
            .execute()

        // Get next full moon
        val nextFullMoonTime = MoonPhase.compute()
            .phase(MoonPhase.Phase.FULL_MOON)
            .on(dateTime)
            .execute()

        // Get last new moon to calculate moon age
        val lastNewMoonTime = MoonPhase.compute()
            .phase(MoonPhase.Phase.NEW_MOON)
            .on(dateTime.minusDays(30))
            .execute()

        // Calculate moon age (days since last new moon)
        val lastNewMoon = lastNewMoonTime.time
        val moonAgeMillis = dateTime.toInstant().toEpochMilli() - lastNewMoon.toInstant().toEpochMilli()
        val moonAge = moonAgeMillis.toDouble() / (1000.0 * 60.0 * 60.0 * 24.0)

        // Convert phase from library range (-180..180 degrees) to 0..1
        // -180 = new moon (0.0), 0 = full moon (0.5), 180 = new moon (1.0)
        val phase = (illumination.phase + 180.0) / 360.0

        // Illumination fraction (0-1)
        val illuminationFraction = illumination.fraction

        // Distance in km
        val distanceKm = position.distance

        // Get phase name
        val phaseName = getPhaseName(phase)

        // Calculate tithi (lunar day 1-30)
        val tithiDuration = SYNODIC_MONTH / 30.0
        val lunarDay = minOf(30, floor(moonAge / tithiDuration).toInt() + 1)
        val tithi = calculateTithi(lunarDay)

        // Convert next phase times to LocalDate
        val nextNewMoon = nextNewMoonTime.time.toLocalDate()
        val nextFullMoon = nextFullMoonTime.time.toLocalDate()

        return MoonPhaseData(
            date = date,
            moonAge = moonAge,
            phase = phase,
            illumination = illuminationFraction,
            phaseName = phaseName,
            distanceKm = distanceKm,
            nextNewMoon = nextNewMoon,
            nextFullMoon = nextFullMoon,
            tithi = tithi
        )
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

    /**
     * Get moon rise and set times for a given date
     */
    fun getMoonTimes(date: LocalDate): Pair<ZonedDateTime?, ZonedDateTime?> {
        val dateTime = date.atStartOfDay(IST_ZONE)
        val times = MoonTimes.compute()
            .on(dateTime)
            .at(LATITUDE, LONGITUDE)
            .execute()

        return Pair(times.rise, times.set)
    }
}
