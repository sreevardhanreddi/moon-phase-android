package com.vardev.moon_phase.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vardev.moon_phase.R
import com.vardev.moon_phase.data.MoonPhaseCalculator
import com.vardev.moon_phase.model.MoonPhaseData
import com.vardev.moon_phase.ui.components.MoonDatePickerDialog
import com.vardev.moon_phase.ui.components.MoonInfoCard
import com.vardev.moon_phase.ui.components.MoonInfoRow
import com.vardev.moon_phase.ui.components.MoonPhaseView
import com.vardev.moon_phase.ui.theme.NamingMode
import com.vardev.moon_phase.ui.theme.ThemeMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    themeMode: ThemeMode = ThemeMode.DARK,
    onThemeToggle: () -> Unit = {},
    namingMode: NamingMode = NamingMode.ENGLISH,
    onNamingModeToggle: () -> Unit = {},
    selectedDate: LocalDate = LocalDate.now(),
    onDateSelected: (LocalDate) -> Unit = {}
) {
    var showDatePicker by remember { mutableStateOf(false) }

    val moonData = remember(selectedDate) {
        MoonPhaseCalculator.calculate(selectedDate)
    }

    val isDarkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    if (showDatePicker) {
        MoonDatePickerDialog(
            selectedDate = selectedDate,
            onDateSelected = { date ->
                onDateSelected(date)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Bar with Theme and Naming Toggle
        TopBar(
            themeMode = themeMode,
            onThemeToggle = onThemeToggle,
            namingMode = namingMode,
            onNamingModeToggle = onNamingModeToggle
        )

        // Date Header
        DateHeader(
            date = selectedDate,
            onDateClick = { showDatePicker = true }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Moon Visualization
        MoonPhaseView(
            phase = moonData.phase,
            illumination = moonData.illumination,
            isWaxing = MoonPhaseCalculator.isWaxing(moonData.tithi.lunarDay),
            isDarkTheme = isDarkTheme
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Tithi Day Label (e.g., "Eleventh (Waxing)" or "Ekadashi (Shukla)")
        val tithiDayLabel = getTithiDayLabel(moonData, namingMode)
        Text(
            text = tithiDayLabel,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        // Illumination with Phase Name
        val displayPhaseName = getDisplayPhaseName(moonData, namingMode)
        Text(
            text = "${moonData.illuminationPercent}% Illuminated ($displayPhaseName)",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Hindu Calendar Section
        HinduCalendarSection(moonData = moonData, namingMode = namingMode)

        Spacer(modifier = Modifier.height(16.dp))

        // Additional Info
        AdditionalInfoSection(moonData = moonData, namingMode = namingMode)
    }
}

private fun getDisplayPhaseName(moonData: MoonPhaseData, namingMode: NamingMode): String {
    return when (namingMode) {
        NamingMode.ENGLISH -> moonData.phaseName
        NamingMode.HINDU -> {
            when {
                moonData.phaseName == "New Moon" -> "Amavasya"
                moonData.phaseName == "Full Moon" -> "Purnima"
                else -> moonData.phaseName // Keep Waxing/Waning Gibbous, Crescent, etc.
            }
        }
    }
}

private fun getTithiDayLabel(moonData: MoonPhaseData, namingMode: NamingMode): String {
    val lunarDay = moonData.tithi.lunarDay
    val dayInPaksha = if (lunarDay <= 15) lunarDay else lunarDay - 15
    val isWaxing = lunarDay <= 15
    val tithiName = moonData.tithi.tithiName.split(" ")[0]

    return when (namingMode) {
        NamingMode.ENGLISH -> {
            // For Full Moon (15th of Shukla) and New Moon (30th/Amavasya)
            when {
                dayInPaksha == 15 && isWaxing -> "Full Moon"
                lunarDay == 30 || tithiName == "Amavasya" -> "New Moon"
                else -> {
                    val ordinal = getOrdinalName(dayInPaksha)
                    val phase = if (isWaxing) "Waxing" else "Waning"
                    "$ordinal ($phase)"
                }
            }
        }
        NamingMode.HINDU -> {
            val paksha = if (isWaxing) "Shukla" else "Krishna"
            // For Purnima and Amavasya, don't show paksha
            if (tithiName == "Purnima" || tithiName == "Amavasya") tithiName else "$tithiName ($paksha)"
        }
    }
}

private fun getOrdinalName(day: Int): String {
    return when (day) {
        1 -> "First"
        2 -> "Second"
        3 -> "Third"
        4 -> "Fourth"
        5 -> "Fifth"
        6 -> "Sixth"
        7 -> "Seventh"
        8 -> "Eighth"
        9 -> "Ninth"
        10 -> "Tenth"
        11 -> "Eleventh"
        12 -> "Twelfth"
        13 -> "Thirteenth"
        14 -> "Fourteenth"
        15 -> "Fifteenth"
        else -> "${day}th"
    }
}

@Composable
private fun TopBar(
    themeMode: ThemeMode,
    onThemeToggle: () -> Unit,
    namingMode: NamingMode,
    onNamingModeToggle: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onNamingModeToggle) {
            Icon(
                painter = painterResource(
                    id = when (namingMode) {
                        NamingMode.ENGLISH -> R.drawable.ic_language_english
                        NamingMode.HINDU -> R.drawable.ic_language_hindu
                    }
                ),
                contentDescription = "Toggle naming (current: ${namingMode.name.lowercase()})",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        IconButton(onClick = onThemeToggle) {
            Icon(
                painter = painterResource(
                    id = when (themeMode) {
                        ThemeMode.LIGHT -> R.drawable.ic_theme_light
                        ThemeMode.DARK -> R.drawable.ic_theme_dark
                    }
                ),
                contentDescription = "Toggle theme (current: ${themeMode.name.lowercase()})",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun DateHeader(
    date: LocalDate,
    onDateClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onDateClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
        IconButton(onClick = onDateClick) {
            Icon(
                imageVector = Icons.Default.DateRange,
                contentDescription = "Select date"
            )
        }
    }
}

@Composable
private fun HinduCalendarSection(moonData: MoonPhaseData, namingMode: NamingMode) {
    val sectionTitle = when (namingMode) {
        NamingMode.ENGLISH -> "Lunar Calendar"
        NamingMode.HINDU -> "Hindu Calendar"
    }

    val lunarDay = moonData.tithi.lunarDay
    val dayInPaksha = if (lunarDay <= 15) lunarDay else lunarDay - 15
    val isWaxing = lunarDay <= 15

    val (tithiTitle, tithiValue, tithiSubtitle) = when (namingMode) {
        NamingMode.ENGLISH -> Triple(
            "Lunar Day",
            "${getOrdinalName(dayInPaksha)} Day",
            if (isWaxing) "Waxing Phase" else "Waning Phase"
        )
        NamingMode.HINDU -> Triple(
            "Tithi",
            moonData.tithi.displayTithi,
            moonData.tithi.paksha
        )
    }

    val specialDayValue = when (namingMode) {
        NamingMode.ENGLISH -> getEnglishSpecialDay(moonData.tithi.specialDayType)
        NamingMode.HINDU -> moonData.tithi.specialDayType
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = sectionTitle,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        MoonInfoCard(
            title = tithiTitle,
            value = tithiValue,
            subtitle = tithiSubtitle,
            highlighted = moonData.tithi.isSpecialDay
        )

        // Special day card hidden
        // if (moonData.tithi.isSpecialDay && specialDayValue != null) {
        //     MoonInfoCard(
        //         title = "Special Day",
        //         value = specialDayValue,
        //         highlighted = true
        //     )
        // }
    }
}

private fun getEnglishSpecialDay(specialDayType: String?): String? {
    return when (specialDayType) {
        "Purnima" -> "Full Moon"
        "Amavasya" -> "New Moon"
        "Ekadashi" -> "Eleventh Day (Fasting)"
        "Chaturthi" -> "Fourth Day"
        "Chaturdashi" -> "Fourteenth Day"
        else -> specialDayType
    }
}

@Composable
private fun AdditionalInfoSection(moonData: MoonPhaseData, namingMode: NamingMode) {
    val nextNewMoonLabel = when (namingMode) {
        NamingMode.ENGLISH -> "Next New Moon"
        NamingMode.HINDU -> "Next Amavasya"
    }
    val nextFullMoonLabel = when (namingMode) {
        NamingMode.ENGLISH -> "Next Full Moon"
        NamingMode.HINDU -> "Next Purnima"
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Moon Information",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        MoonInfoCard(
            title = "Moon Age",
            value = "%.1f days".format(moonData.moonAge),
            subtitle = "into lunar cycle"
        )

        MoonInfoRow(
            items = listOf(
                nextNewMoonLabel to moonData.nextNewMoon.format(
                    DateTimeFormatter.ofPattern("MMM d")
                ),
                nextFullMoonLabel to moonData.nextFullMoon.format(
                    DateTimeFormatter.ofPattern("MMM d")
                )
            )
        )

        // Distance card hidden
        // MoonInfoCard(
        //     title = "Distance from Earth",
        //     value = moonData.formattedDistance
        // )
    }
}
