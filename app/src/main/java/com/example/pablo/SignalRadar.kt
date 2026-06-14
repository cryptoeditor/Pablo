package com.example.pablo

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
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

// How much the ground plane is squashed vertically to fake a 3D tilt.
private const val TILT = 0.5f

/**
 * A pseudo-3D radar map of nearby radios. The ground plane is tilted into
 * perspective; the user sits at the center and each contact is a shaded
 * "sphere" floating above the plane, placed by signal strength and bearing.
 *
 * Drag horizontally to rotate the whole scene.
 */
@Composable
fun SignalRadar(
    contacts: List<RadioContact>,
    modifier: Modifier = Modifier
) {
    val ringColor = MaterialTheme.colorScheme.outlineVariant
    val centerColor = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface

    // Current rotation of the scene, in radians. Updated by dragging.
    var rotation by remember { mutableStateOf(0.6f) }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(340.dp)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    rotation += dragAmount.x * 0.01f
                    change.consume()
                }
            }
    ) {
        val maxR = (size.minDimension / 2f) * 0.8f
        val cx = size.width / 2f
        val cy = size.height / 2f
        val center = Offset(cx, cy)
        val canvas = drawContext.canvas.nativeCanvas

        // Tilted range rings (circles squashed into ellipses).
        for (i in 1..4) {
            val r = maxR * (i / 4f)
            drawOval(
                color = ringColor,
                topLeft = Offset(cx - r, cy - r * TILT),
                size = Size(r * 2f, r * 2f * TILT),
                style = Stroke(width = 2f)
            )
        }
        // Cross axes on the tilted plane.
        drawLine(ringColor, Offset(cx - maxR, cy), Offset(cx + maxR, cy), strokeWidth = 1.5f)
        drawLine(ringColor, Offset(cx, cy - maxR * TILT), Offset(cx, cy + maxR * TILT), strokeWidth = 1.5f)

        // dBm labels up the back of the plane.
        val ringDbm = listOf(-50, -70, -90, -110)
        val ringPaint = Paint().apply {
            color = onSurface.copy(alpha = 0.5f).toArgb()
            textSize = 24f
            isAntiAlias = true
        }
        for (i in 1..4) {
            val r = maxR * (i / 4f)
            canvas.drawText("${ringDbm[i - 1]}", cx + 6f, cy - r * TILT + 20f, ringPaint)
        }

        // The user, at the center of the plane.
        drawSphere(center, 11f, centerColor)
        val youPaint = Paint().apply {
            color = centerColor.toArgb()
            textSize = 28f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("YOU", cx, cy + 40f, youPaint)

        // Project each contact onto the plane, then sort back-to-front so nearer
        // bubbles correctly overlap farther ones.
        val plotted = contacts.map { c ->
            val dist = maxR * dbmFraction(c.dbm)
            val angle = c.bearingDegrees * (PI.toFloat() / 180f) + rotation
            Triple(c, dist * cos(angle), dist * sin(angle))
        }.sortedBy { it.third }

        val labelPaint = Paint().apply {
            color = onSurface.toArgb()
            textSize = 30f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        for ((c, planeX, planeY) in plotted) {
            val groundX = cx + planeX
            val groundY = cy + planeY * TILT
            // Things toward the front (larger planeY) look a bit bigger.
            val depth = 0.85f + ((planeY / maxR) + 1f) / 2f * 0.4f
            val bubbleR = (10f + (1f - dbmFraction(c.dbm)) * 12f) * depth
            val lift = 46f * depth
            val bubbleCenter = Offset(groundX, groundY - lift)
            val color = signalColor(c.dbm)

            // Shadow on the plane.
            drawOval(
                color = Color.Black.copy(alpha = 0.18f),
                topLeft = Offset(groundX - bubbleR, groundY - bubbleR * TILT),
                size = Size(bubbleR * 2f, bubbleR * 2f * TILT)
            )
            // Stem connecting the bubble to its spot on the plane.
            drawLine(
                color = color.copy(alpha = 0.5f),
                start = Offset(groundX, groundY),
                end = bubbleCenter,
                strokeWidth = 2f
            )
            // The shaded sphere.
            drawSphere(bubbleCenter, bubbleR, color)

            canvas.drawText(c.label, bubbleCenter.x, bubbleCenter.y - bubbleR - 10f, labelPaint)
            canvas.drawText("${c.dbm} dBm", groundX, groundY + 24f, labelPaint)
        }
    }
}

/** Draws a circle shaded like a 3D sphere (bright highlight, dark far edge). */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSphere(
    center: Offset,
    radius: Float,
    base: Color
) {
    val brush = Brush.radialGradient(
        colors = listOf(
            lerp(base, Color.White, 0.6f),
            base,
            lerp(base, Color.Black, 0.35f)
        ),
        center = Offset(center.x - radius * 0.35f, center.y - radius * 0.4f),
        radius = radius * 1.4f
    )
    drawCircle(brush = brush, radius = radius, center = center)
}
