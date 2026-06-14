package com.example.pablo

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** One nearby radio detected by a signal snapshot. */
data class RadioContact(
    val id: String,
    val label: String,
    val dbm: Int,
    val bearingDegrees: Float
)

/** A fixed "real" radio with a typical signal level and direction from us. */
private data class BaseRadio(
    val id: String,
    val label: String,
    val baseDbm: Int,
    val bearing: Float
)

private val baseRadios = listOf(
    BaseRadio("a", "Alpha", -48, 35f),
    BaseRadio("b", "Bravo", -63, 120f),
    BaseRadio("c", "Charlie", -79, 205f),
    BaseRadio("d", "Delta", -94, 300f)
)

/**
 * Produces one "snapshot" of nearby radios.
 *
 * NOTE: this is SIMULATED data for now — each call jitters the dBm a little so
 * the map looks alive. When real hardware is wired in, this is the ONE function
 * that gets replaced with an actual reading from the SDR; the radar UI stays.
 */
fun sampleNearbyRadios(): List<RadioContact> =
    baseRadios.map { base ->
        RadioContact(
            id = base.id,
            label = base.label,
            dbm = base.baseDbm + Random.nextInt(-5, 6),
            bearingDegrees = base.bearing
        )
    }

/** Strong signal (near 0 dBm) -> 0f (center). Weak (-115 dBm) -> 1f (edge). */
private fun dbmFraction(dbm: Int): Float {
    val fraction = (-40 - dbm) / 75f
    return fraction.coerceIn(0f, 1f)
}

/** Green (strong) -> amber -> orange -> red (weak), based on signal level. */
private fun signalColor(dbm: Int): Color = when {
    dbm >= -55 -> Color(0xFF2E9E5B)
    dbm >= -75 -> Color(0xFFE0A000)
    dbm >= -95 -> Color(0xFFE0701A)
    else -> Color(0xFFC2453F)
}

/**
 * A radar-style map of nearby radios. The user is at the center; each contact
 * is a colored "RF bubble" placed by signal strength (distance) and bearing.
 */
@Composable
fun SignalRadar(
    contacts: List<RadioContact>,
    modifier: Modifier = Modifier
) {
    val ringColor = MaterialTheme.colorScheme.outlineVariant
    val centerColor = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(320.dp)
    ) {
        val maxR = (size.minDimension / 2f) * 0.82f
        val cx = size.width / 2f
        val cy = size.height / 2f
        val center = Offset(cx, cy)

        // Concentric range rings.
        for (i in 1..4) {
            drawCircle(
                color = ringColor,
                radius = maxR * (i / 4f),
                center = center,
                style = Stroke(width = 2f)
            )
        }
        // Cross axes (N-S, E-W).
        drawLine(ringColor, Offset(cx - maxR, cy), Offset(cx + maxR, cy), strokeWidth = 1.5f)
        drawLine(ringColor, Offset(cx, cy - maxR), Offset(cx, cy + maxR), strokeWidth = 1.5f)

        // dBm label for each ring, drawn going straight up from the center.
        val ringDbm = listOf(-50, -70, -90, -110)
        val ringPaint = Paint().apply {
            color = onSurface.copy(alpha = 0.55f).toArgb()
            textSize = 26f
            isAntiAlias = true
        }
        for (i in 1..4) {
            drawContext.canvas.nativeCanvas.drawText(
                "${ringDbm[i - 1]}",
                cx + 6f,
                cy - maxR * (i / 4f) + 24f,
                ringPaint
            )
        }

        // Each nearby radio as a bubble.
        val labelPaint = Paint().apply {
            color = onSurface.toArgb()
            textSize = 32f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        for (c in contacts) {
            val fraction = dbmFraction(c.dbm)
            val dist = maxR * fraction
            val rad = c.bearingDegrees * (PI / 180f)
            val x = cx + dist * cos(rad).toFloat()
            val y = cy - dist * sin(rad).toFloat()
            val bubbleR = 9f + (1f - fraction) * 13f
            val color = signalColor(c.dbm)

            // Translucent "RF bubble" halo, then the solid blip.
            drawCircle(color = color.copy(alpha = 0.18f), radius = bubbleR * 2.2f, center = Offset(x, y))
            drawCircle(color = color, radius = bubbleR, center = Offset(x, y))

            drawContext.canvas.nativeCanvas.drawText(c.label, x, y - bubbleR - 12f, labelPaint)
            drawContext.canvas.nativeCanvas.drawText("${c.dbm} dBm", x, y + bubbleR + 34f, labelPaint)
        }

        // The user, at the center.
        drawCircle(color = centerColor, radius = 12f, center = center)
        val youPaint = Paint().apply {
            color = centerColor.toArgb()
            textSize = 30f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        drawContext.canvas.nativeCanvas.drawText("YOU", cx, cy + 44f, youPaint)
    }
}
