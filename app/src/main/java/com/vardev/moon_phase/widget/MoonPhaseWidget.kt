package com.vardev.moon_phase.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.updateAll
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.vardev.moon_phase.MainActivity
import com.vardev.moon_phase.data.MoonPhaseCalculator
import com.vardev.moon_phase.data.PreferencesManager
import com.vardev.moon_phase.model.MoonPhaseData
import com.vardev.moon_phase.ui.theme.NamingMode
import com.vardev.moon_phase.ui.theme.ThemeMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.abs

private const val TAG = "MoonPhaseWidget"

class MoonPhaseWidget : GlanceAppWidget() {

    companion object {
        private val SIZE_1x1 = DpSize(40.dp, 40.dp)
        private val SIZE_2x1 = DpSize(110.dp, 40.dp)
        private val SIZE_3x1 = DpSize(180.dp, 40.dp)
        private val SIZE_4x1 = DpSize(250.dp, 40.dp)
        private val SIZE_2x2 = DpSize(110.dp, 110.dp)
        private val SIZE_4x2 = DpSize(250.dp, 110.dp)
    }

    override val sizeMode = SizeMode.Responsive(
        setOf(SIZE_1x1, SIZE_2x1, SIZE_3x1, SIZE_4x1, SIZE_2x2, SIZE_4x2)
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        Log.d(TAG, "provideGlance: Called for widget $id")
        // Always use current date for widget
        val currentDate = LocalDate.now()
        val namingMode = PreferencesManager.getNamingMode(context)
        val themeMode = PreferencesManager.getThemeMode(context)
        Log.d(TAG, "provideGlance: themeMode=$themeMode, namingMode=$namingMode, date=$currentDate")
        val moonData = MoonPhaseCalculator.calculate(currentDate)

        val isDarkTheme = when (themeMode) {
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
        }

        provideContent {
            GlanceTheme {
                val size = LocalSize.current
                when {
                    // 2x2 or larger square/tall widgets
                    size.height >= 100.dp && size.width < 200.dp -> Widget2x2Content(moonData, namingMode, isDarkTheme)
                    // 4x2 large widget
                    size.height >= 100.dp && size.width >= 200.dp -> Widget4x2Content(moonData, namingMode, isDarkTheme)
                    // 4x1 wide widget
                    size.width >= 220.dp -> Widget4x1Content(moonData, namingMode, isDarkTheme)
                    // 3x1 medium horizontal widget
                    size.width >= 150.dp -> Widget3x1Content(moonData, namingMode, isDarkTheme)
                    // 2x1 small widget
                    size.width >= 80.dp -> Widget2x1Content(moonData, namingMode, isDarkTheme)
                    // 1x1 tiny widget
                    else -> Widget1x1Content(moonData, namingMode, isDarkTheme)
                }
            }
        }
    }
}

// Key to force widget refresh by changing state
private val REFRESH_KEY = longPreferencesKey("refresh_timestamp")

// Action callback to refresh the widget when clicked
class RefreshWidgetAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        Log.d(TAG, "RefreshWidgetAction: onAction called for widget $glanceId")
        try {
            // Force state change to trigger refresh
            val timestamp = System.currentTimeMillis()
            Log.d(TAG, "RefreshWidgetAction: Forcing state change with timestamp $timestamp")
            updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                prefs.toMutablePreferences().apply {
                    this[REFRESH_KEY] = timestamp
                }
            }

            // Update all widget instances (updateAll triggers provideGlance for each)
            MoonPhaseWidget().updateAll(context)
            Log.d(TAG, "RefreshWidgetAction: Widget update completed")
        } catch (e: Exception) {
            Log.e(TAG, "RefreshWidgetAction: Error updating widget", e)
        }
    }
}

// Widget theme colors
private fun getWidgetBackgroundColor(isDarkTheme: Boolean): Color =
    if (isDarkTheme) Color.Black else Color.White

private fun getWidgetTextColor(isDarkTheme: Boolean): Color =
    if (isDarkTheme) Color.White else Color.Black

private fun getWidgetAccentColor(isDarkTheme: Boolean): Color =
    if (isDarkTheme) Color(0xFFFFD700) else Color(0xFFB8860B) // Gold / DarkGoldenrod

private fun getWidgetSubduedTextColor(isDarkTheme: Boolean): Color =
    if (isDarkTheme) Color(0xFF888888) else Color(0xFF666666) // Muted gray

// 1x1 Widget - Tiny (moon icon with tithi number)
@Composable
fun Widget1x1Content(moonData: MoonPhaseData, namingMode: NamingMode, isDarkTheme: Boolean) {
    val bitmap = createMoonBitmap(moonData, 64, isDarkTheme)
    val lunarDay = moonData.tithi.lunarDay
    val dayInPaksha = if (lunarDay <= 15) lunarDay else lunarDay - 15
    val isWaxing = lunarDay <= 15
    val tithiName = moonData.tithi.tithiName.split(" ")[0]
    val isPurnimaOrAmavasya = tithiName == "Purnima" || tithiName == "Amavasya"

    val displayText = if (isPurnimaOrAmavasya) {
        // Show short name for Purnima/Amavasya without paksha
        if (tithiName == "Purnima") "Purn" else "Amav"
    } else {
        val ordinal = getOrdinalWithSuperscript(dayInPaksha)
        val pakshaInitial = when (namingMode) {
            NamingMode.ENGLISH -> if (isWaxing) "W" else "Wn"
            NamingMode.HINDU -> if (isWaxing) "S" else "K"
        }
        "$ordinal ($pakshaInitial)"
    }

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(getWidgetBackgroundColor(isDarkTheme))
            .clickable(actionStartActivity<MainActivity>())
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            provider = ImageProvider(bitmap),
            contentDescription = "Moon phase - tap to refresh",
            modifier = GlanceModifier
                .size(36.dp)
                .clickable(actionRunCallback<RefreshWidgetAction>())
        )
        Text(
            text = displayText,
            style = TextStyle(
                color = ColorProvider(getWidgetTextColor(isDarkTheme)),
                fontWeight = FontWeight.Medium
            )
        )
    }
}

// 2x1 Widget - Compact
@Composable
fun Widget2x1Content(moonData: MoonPhaseData, namingMode: NamingMode, isDarkTheme: Boolean) {
    val bitmap = createMoonBitmap(moonData, 48, isDarkTheme)
    val lunarDay = moonData.tithi.lunarDay
    val dayInPaksha = if (lunarDay <= 15) lunarDay else lunarDay - 15
    val isWaxing = lunarDay <= 15
    val (tithiDisplay, pakshaDisplay) = getWidgetTithiPaksha(moonData, namingMode, isWaxing)
    val baseText = if (pakshaDisplay != null) "$tithiDisplay ($pakshaDisplay)" else tithiDisplay
    val tithiText = "$baseText - ${getOrdinalWithSuperscript(dayInPaksha)}"
    // val moonAgeText = "%.1f days".format(moonData.moonAge)

    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(getWidgetBackgroundColor(isDarkTheme))
            .clickable(actionStartActivity<MainActivity>())
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            provider = ImageProvider(bitmap),
            contentDescription = "Moon phase - tap to refresh",
            modifier = GlanceModifier
                .size(40.dp)
                .clickable(actionRunCallback<RefreshWidgetAction>())
        )
        Spacer(modifier = GlanceModifier.width(8.dp))
        Column {
            Text(
                text = tithiText,
                style = TextStyle(
                    color = ColorProvider(getWidgetTextColor(isDarkTheme)),
                    fontWeight = FontWeight.Medium
                )
            )
            // Text(
            //     text = moonAgeText,
            //     style = TextStyle(
            //         color = ColorProvider(getWidgetTextColor(isDarkTheme))
            //     )
            // )
        }
    }
}

// 3x1 Widget - Medium horizontal
@Composable
fun Widget3x1Content(moonData: MoonPhaseData, namingMode: NamingMode, isDarkTheme: Boolean) {
    val bitmap = createMoonBitmap(moonData, 56, isDarkTheme)
    val lunarDay = moonData.tithi.lunarDay
    val dayInPaksha = if (lunarDay <= 15) lunarDay else lunarDay - 15
    val isWaxing = lunarDay <= 15
    val (tithiDisplay, pakshaDisplay) = getWidgetTithiPaksha(moonData, namingMode, isWaxing)
    val baseText = if (pakshaDisplay != null) "$tithiDisplay ($pakshaDisplay)" else tithiDisplay
    val tithiText = "$baseText - ${getOrdinalWithSuperscript(dayInPaksha)}"
    val phaseShort = getWidgetPhaseName(moonData, namingMode, isWaxing)
    // val moonAgeText = "%.1f days".format(moonData.moonAge)

    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(getWidgetBackgroundColor(isDarkTheme))
            .clickable(actionStartActivity<MainActivity>())
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            provider = ImageProvider(bitmap),
            contentDescription = "Moon phase - tap to refresh",
            modifier = GlanceModifier
                .size(44.dp)
                .clickable(actionRunCallback<RefreshWidgetAction>())
        )
        Spacer(modifier = GlanceModifier.width(10.dp))
        Column {
            Text(
                text = tithiText,
                style = TextStyle(
                    color = ColorProvider(getWidgetTextColor(isDarkTheme)),
                    fontWeight = FontWeight.Bold
                )
            )
            // Text(
            //     text = moonAgeText,
            //     style = TextStyle(
            //         color = ColorProvider(getWidgetTextColor(isDarkTheme))
            //     )
            // )
            Text(
                text = "${moonData.illuminationPercent}% ($phaseShort)",
                style = TextStyle(
                    color = ColorProvider(getWidgetSubduedTextColor(isDarkTheme))
                )
            )
        }
    }
}

// 4x1 Widget - Wide horizontal
@Composable
fun Widget4x1Content(moonData: MoonPhaseData, namingMode: NamingMode, isDarkTheme: Boolean) {
    val bitmap = createMoonBitmap(moonData, 64, isDarkTheme)
    val lunarDay = moonData.tithi.lunarDay
    val dayInPaksha = if (lunarDay <= 15) lunarDay else lunarDay - 15
    val isWaxing = lunarDay <= 15
    val (tithiDisplay, pakshaDisplay) = getWidgetTithiPaksha(moonData, namingMode, isWaxing)
    val baseText = if (pakshaDisplay != null) "$tithiDisplay ($pakshaDisplay)" else tithiDisplay
    val tithiText = "$baseText - ${getOrdinalWithSuperscript(dayInPaksha)}"
    val phaseShort = getWidgetPhaseName(moonData, namingMode, isWaxing)
    // val moonAgeText = "%.1f days".format(moonData.moonAge)

    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(getWidgetBackgroundColor(isDarkTheme))
            .clickable(actionStartActivity<MainActivity>())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            provider = ImageProvider(bitmap),
            contentDescription = "Moon phase - tap to refresh",
            modifier = GlanceModifier
                .size(52.dp)
                .clickable(actionRunCallback<RefreshWidgetAction>())
        )
        Spacer(modifier = GlanceModifier.width(12.dp))
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = tithiText,
                style = TextStyle(
                    color = ColorProvider(getWidgetTextColor(isDarkTheme)),
                    fontWeight = FontWeight.Bold
                )
            )
            // Text(
            //     text = moonAgeText,
            //     style = TextStyle(
            //         color = ColorProvider(getWidgetTextColor(isDarkTheme))
            //     )
            // )
            Text(
                text = "${moonData.illuminationPercent}% Illuminated ($phaseShort)",
                style = TextStyle(
                    color = ColorProvider(getWidgetSubduedTextColor(isDarkTheme))
                )
            )
        }
    }
}

// 2x2 Widget - Square
@Composable
fun Widget2x2Content(moonData: MoonPhaseData, namingMode: NamingMode, isDarkTheme: Boolean) {
    val bitmap = createMoonBitmap(moonData, 80, isDarkTheme)
    val isWaxing = moonData.tithi.lunarDay <= 15
    val (tithiDisplay, pakshaDisplay) = getWidgetTithiPaksha(moonData, namingMode, isWaxing)
    val tithiText = if (pakshaDisplay != null) "$tithiDisplay ($pakshaDisplay)" else tithiDisplay
    val phaseShort = getWidgetPhaseName(moonData, namingMode, isWaxing)
    val moonAgeText = "%.1f days".format(moonData.moonAge)

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(getWidgetBackgroundColor(isDarkTheme))
            .clickable(actionStartActivity<MainActivity>())
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            provider = ImageProvider(bitmap),
            contentDescription = "Moon phase - tap to refresh",
            modifier = GlanceModifier
                .size(52.dp)
                .clickable(actionRunCallback<RefreshWidgetAction>())
        )
        Spacer(modifier = GlanceModifier.height(2.dp))
        Text(
            text = tithiText,
            style = TextStyle(
                color = ColorProvider(getWidgetTextColor(isDarkTheme)),
                fontWeight = FontWeight.Bold
            )
        )
        Text(
            text = "${moonData.illuminationPercent}% ($phaseShort)",
            style = TextStyle(
                color = ColorProvider(getWidgetSubduedTextColor(isDarkTheme))
            )
        )
        Text(
            text = moonAgeText,
            style = TextStyle(
                color = ColorProvider(getWidgetSubduedTextColor(isDarkTheme))
            )
        )
    }
}

// 4x2 Widget - Large
@Composable
fun Widget4x2Content(moonData: MoonPhaseData, namingMode: NamingMode, isDarkTheme: Boolean) {
    val bitmap = createMoonBitmap(moonData, 96, isDarkTheme)
    val isWaxing = moonData.tithi.lunarDay <= 15
    val (tithiDisplay, pakshaDisplay) = getWidgetTithiPaksha(moonData, namingMode, isWaxing)
    val tithiText = if (pakshaDisplay != null) "$tithiDisplay ($pakshaDisplay)" else tithiDisplay
    val phaseShort = getWidgetPhaseName(moonData, namingMode, isWaxing)
    val moonAgeText = "%.1f days old".format(moonData.moonAge)
    val dateFormatter = DateTimeFormatter.ofPattern("MMM d")

    val (newMoonLabel, fullMoonLabel) = when (namingMode) {
        NamingMode.ENGLISH -> Pair("New", "Full")
        NamingMode.HINDU -> Pair("Amav", "Purn")
    }

    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(getWidgetBackgroundColor(isDarkTheme))
            .clickable(actionStartActivity<MainActivity>())
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            provider = ImageProvider(bitmap),
            contentDescription = "Moon phase - tap to refresh",
            modifier = GlanceModifier
                .size(80.dp)
                .clickable(actionRunCallback<RefreshWidgetAction>())
        )
        Spacer(modifier = GlanceModifier.width(12.dp))
        Column(
            modifier = GlanceModifier.defaultWeight()
        ) {
            Text(
                text = tithiText,
                style = TextStyle(
                    color = ColorProvider(getWidgetTextColor(isDarkTheme)),
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = "${moonData.illuminationPercent}% ($phaseShort)",
                style = TextStyle(
                    color = ColorProvider(getWidgetSubduedTextColor(isDarkTheme))
                )
            )
            Spacer(modifier = GlanceModifier.height(4.dp))
            Text(
                text = moonAgeText,
                style = TextStyle(
                    color = ColorProvider(getWidgetSubduedTextColor(isDarkTheme))
                )
            )
        }
        // Next moon dates column
        Column(
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = "$newMoonLabel: ${moonData.nextNewMoon.format(dateFormatter)}",
                style = TextStyle(
                    color = ColorProvider(getWidgetSubduedTextColor(isDarkTheme))
                )
            )
            Text(
                text = "$fullMoonLabel: ${moonData.nextFullMoon.format(dateFormatter)}",
                style = TextStyle(
                    color = ColorProvider(getWidgetSubduedTextColor(isDarkTheme))
                )
            )
        }
    }
}

private fun getWidgetTithiPaksha(
    moonData: MoonPhaseData,
    namingMode: NamingMode,
    isWaxing: Boolean
): Pair<String, String?> {
    val lunarDay = moonData.tithi.lunarDay
    val dayInPaksha = if (lunarDay <= 15) lunarDay else lunarDay - 15
    val tithiName = moonData.tithi.tithiName.split(" ")[0]
    val isPurnimaOrAmavasya = tithiName == "Purnima" || tithiName == "Amavasya"

    return when (namingMode) {
        NamingMode.ENGLISH -> {
            // Show Full Moon / New Moon for special days
            when {
                dayInPaksha == 15 && isWaxing -> Pair("Full Moon", null)
                lunarDay == 30 || tithiName == "Amavasya" -> Pair("New Moon", null)
                else -> {
                    val ordinal = getOrdinalName(dayInPaksha)
                    val paksha = if (isWaxing) "Waxing" else "Waning"
                    Pair(ordinal, paksha)
                }
            }
        }
        NamingMode.HINDU -> {
            val paksha = if (isPurnimaOrAmavasya) null else if (isWaxing) "Shukla" else "Krishna"
            Pair(tithiName, paksha)
        }
    }
}

// Version with superscript ordinals (e.g., "12ᵗʰ" instead of "Twelfth")
private fun getWidgetTithiPakshaWithSuperscript(
    moonData: MoonPhaseData,
    namingMode: NamingMode,
    isWaxing: Boolean
): Pair<String, String?> {
    val lunarDay = moonData.tithi.lunarDay
    val dayInPaksha = if (lunarDay <= 15) lunarDay else lunarDay - 15
    val tithiName = moonData.tithi.tithiName.split(" ")[0]
    val isPurnimaOrAmavasya = tithiName == "Purnima" || tithiName == "Amavasya"

    return when (namingMode) {
        NamingMode.ENGLISH -> {
            when {
                dayInPaksha == 15 && isWaxing -> Pair("Full Moon", null)
                lunarDay == 30 || tithiName == "Amavasya" -> Pair("New Moon", null)
                else -> {
                    val ordinal = getOrdinalWithSuperscript(dayInPaksha)
                    val paksha = if (isWaxing) "Waxing" else "Waning"
                    Pair(ordinal, paksha)
                }
            }
        }
        NamingMode.HINDU -> {
            val paksha = if (isPurnimaOrAmavasya) null else if (isWaxing) "Shukla" else "Krishna"
            Pair(tithiName, paksha)
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

private fun getWidgetPhaseName(moonData: MoonPhaseData, namingMode: NamingMode, isWaxing: Boolean): String {
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

private fun getWidgetSpecialDay(specialDayType: String?, namingMode: NamingMode): String? {
    if (specialDayType == null) return null
    return when (namingMode) {
        NamingMode.ENGLISH -> when (specialDayType) {
            "Purnima" -> "Full Moon"
            "Amavasya" -> "New Moon"
            "Ekadashi" -> "Fasting Day"
            "Chaturthi" -> "Fourth Day"
            "Chaturdashi" -> "Fourteenth"
            else -> specialDayType
        }
        NamingMode.HINDU -> specialDayType
    }
}

// Get ordinal number with Unicode superscript suffix (e.g., "12ᵗʰ")
private fun getOrdinalWithSuperscript(day: Int): String {
    val suffix = when {
        day in 11..13 -> "ᵗʰ"
        day % 10 == 1 -> "ˢᵗ"
        day % 10 == 2 -> "ⁿᵈ"
        day % 10 == 3 -> "ʳᵈ"
        else -> "ᵗʰ"
    }
    return "$day$suffix"
}

private fun createMoonBitmap(moonData: MoonPhaseData, sizePx: Int, isDarkTheme: Boolean = true): Bitmap {
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val centerX = sizePx / 2f
    val centerY = sizePx / 2f
    // Make moon slightly smaller to fit glow within bitmap
    val radius = sizePx / 2f * 0.85f

    // Moon colors based on theme
    val moonColor = if (isDarkTheme) 0xFFF5F5DC.toInt() else 0xFFFFE4B5.toInt() // Cream / Moccasin
    val shadowColor = if (isDarkTheme) 0xFF000000.toInt() else 0xFF404040.toInt() // Black / Dark Gray

    // Glow color based on theme
    val glowColor = if (isDarkTheme) 0xFFF5F5DC.toInt() else 0xFF000000.toInt()

    val moonPaint = Paint().apply {
        color = moonColor
        isAntiAlias = true
    }

    val shadowPaint = Paint().apply {
        color = shadowColor
        isAntiAlias = true
    }

    val glowPaint = Paint().apply {
        isAntiAlias = true
    }

    // Draw glow layers (outer to inner)
    val glowLayers = 5
    val maxGlowRadius = radius * 1.15f
    for (i in glowLayers downTo 1) {
        val fraction = i.toFloat() / glowLayers
        val layerRadius = radius + (maxGlowRadius - radius) * fraction
        val alpha = if (isDarkTheme) {
            // Softer glow for dark theme
            (0.15f * (1f - fraction + 0.2f) * 255).toInt()
        } else {
            // Subtle shadow for light theme
            (0.08f * (1f - fraction + 0.2f) * 255).toInt()
        }
        glowPaint.color = (glowColor and 0x00FFFFFF) or (alpha shl 24)
        canvas.drawCircle(centerX, centerY, layerRadius, glowPaint)
    }

    // Draw moon base
    canvas.drawCircle(centerX, centerY, radius, moonPaint)

    val phase = moonData.phase
    val illumination = moonData.illumination
    val isWaxing = MoonPhaseCalculator.isWaxing(moonData.tithi.lunarDay)

    // Full moon - no shadow
    if (phase > 0.48 && phase < 0.52) return bitmap

    // New moon - full shadow
    if (phase < 0.02 || phase > 0.98) {
        canvas.drawCircle(centerX, centerY, radius, shadowPaint)
        return bitmap
    }

    val shadowWidth = radius * abs(1 - 2 * illumination).toFloat()
    val path = Path()

    if (isWaxing) {
        if (illumination < 0.5) {
            path.moveTo(centerX, centerY - radius)
            path.arcTo(
                RectF(centerX - shadowWidth, centerY - radius, centerX + shadowWidth, centerY + radius),
                -90f, 180f
            )
            path.arcTo(
                RectF(centerX - radius, centerY - radius, centerX + radius, centerY + radius),
                90f, 180f
            )
        } else {
            path.moveTo(centerX, centerY - radius)
            path.arcTo(
                RectF(centerX - shadowWidth, centerY - radius, centerX + shadowWidth, centerY + radius),
                -90f, -180f
            )
            path.arcTo(
                RectF(centerX - radius, centerY - radius, centerX + radius, centerY + radius),
                90f, 180f
            )
        }
    } else {
        if (illumination > 0.5) {
            path.moveTo(centerX, centerY - radius)
            path.arcTo(
                RectF(centerX - shadowWidth, centerY - radius, centerX + shadowWidth, centerY + radius),
                -90f, 180f
            )
            path.arcTo(
                RectF(centerX - radius, centerY - radius, centerX + radius, centerY + radius),
                90f, -180f
            )
        } else {
            path.moveTo(centerX, centerY - radius)
            path.arcTo(
                RectF(centerX - shadowWidth, centerY - radius, centerX + shadowWidth, centerY + radius),
                -90f, -180f
            )
            path.arcTo(
                RectF(centerX - radius, centerY - radius, centerX + radius, centerY + radius),
                90f, -180f
            )
        }
    }

    path.close()
    canvas.drawPath(path, shadowPaint)

    return bitmap
}

// Widget Receivers for each size
class Widget1x1Receiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MoonPhaseWidget()

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        Log.d(TAG, "Widget1x1Receiver.onUpdate: Updating ${appWidgetIds.size} widgets")
        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }
}

class Widget2x1Receiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MoonPhaseWidget()

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        Log.d(TAG, "Widget2x1Receiver.onUpdate: Updating ${appWidgetIds.size} widgets")
        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }
}

class Widget3x1Receiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MoonPhaseWidget()

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        Log.d(TAG, "Widget3x1Receiver.onUpdate: Updating ${appWidgetIds.size} widgets")
        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }
}

class Widget4x1Receiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MoonPhaseWidget()

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        Log.d(TAG, "Widget4x1Receiver.onUpdate: Updating ${appWidgetIds.size} widgets")
        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }
}

class Widget2x2Receiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MoonPhaseWidget()

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        Log.d(TAG, "Widget2x2Receiver.onUpdate: Updating ${appWidgetIds.size} widgets")
        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }
}

class Widget4x2Receiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MoonPhaseWidget()

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        Log.d(TAG, "Widget4x2Receiver.onUpdate: Updating ${appWidgetIds.size} widgets")
        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }
}
