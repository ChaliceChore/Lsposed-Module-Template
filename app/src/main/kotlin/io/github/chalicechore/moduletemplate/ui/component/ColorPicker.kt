@file:Suppress("ktlint:standard:function-naming")

package io.github.chalicechore.moduletemplate.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import io.github.chalicechore.moduletemplate.R
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

private val Swatches =
    listOf(
        0xFF0E7C93, 0xFF1565C0, 0xFF3949AB, 0xFF6A1B9A, 0xFFAD1457, 0xFFC62828,
        0xFFEF6C00, 0xFFF9A825, 0xFF2E7D32, 0xFF00897B, 0xFF5D4037, 0xFF546E7A,
        0xFF4DD0E1, 0xFF64B5F6, 0xFF9575CD, 0xFFF06292, 0xFFFFB74D, 0xFFAED581,
        0xFF000000, 0xFF424242, 0xFF9E9E9E, 0xFFE0E0E0, 0xFFF5F5F5, 0xFFFFFFFF,
    ).map { Color(it) }

private enum class PickerMode(
    val labelRes: Int,
) {
    SWATCHES(R.string.color_mode_swatches),
    WHEEL(R.string.color_mode_wheel),
}

/**
 * #11 Colour picker with two modes that share one value: a swatch grid, and a hue/saturation
 * wheel with a brightness slider. A hex field and a preview sit under both.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorPickerDialog(
    title: String,
    initial: Color,
    onPick: (Color) -> Unit,
    onDismiss: () -> Unit,
) {
    val start = FloatArray(3).also { android.graphics.Color.colorToHSV(initial.toArgb(), it) }
    var hue by rememberSaveable { mutableFloatStateOf(start[0]) }
    var sat by rememberSaveable { mutableFloatStateOf(start[1]) }
    var value by rememberSaveable { mutableFloatStateOf(start[2]) }
    var mode by rememberSaveable { mutableIntStateOf(0) }
    val color = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, value)))
    var hex by rememberSaveable { mutableStateOf(toHex(color)) }
    fun set(c: Color) {
        val hsv = FloatArray(3).also { android.graphics.Color.colorToHSV(c.toArgb(), it) }
        hue = hsv[0]
        sat = hsv[1]
        value = hsv[2]
        hex = toHex(c)
    }
    fun setHsv(
        h: Float = hue,
        s: Float = sat,
        v: Float = value,
    ) {
        hue = h
        sat = s
        value = v
        hex = toHex(Color(android.graphics.Color.HSVToColor(floatArrayOf(h, s, v))))
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    PickerMode.entries.forEachIndexed { i, m ->
                        SegmentedButton(
                            selected = mode == i,
                            onClick = { mode = i },
                            shape = SegmentedButtonDefaults.itemShape(i, PickerMode.entries.size),
                            label = { Text(stringResource(m.labelRes), maxLines = 1) },
                        )
                    }
                }
                when (PickerMode.entries[mode]) {
                    PickerMode.SWATCHES -> SwatchGrid(color, ::set)
                    PickerMode.WHEEL -> {
                        HueSatWheel(hue, sat, value) { h, s -> setHsv(h = h, s = s) }
                        LabeledSlider(stringResource(R.string.color_brightness), value, 0f..1f) { setHsv(v = it) }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(48.dp).background(color, RoundedCornerShape(12.dp)).border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)))
                    OutlinedTextField(
                        value = hex,
                        onValueChange = { text ->
                            hex = text.uppercase().take(7)
                            parseHex(hex)?.let { c ->
                                val hsv = FloatArray(3).also { android.graphics.Color.colorToHSV(c.toArgb(), it) }
                                hue = hsv[0]
                                sat = hsv[1]
                                value = hsv[2]
                            }
                        },
                        singleLine = true,
                        isError = parseHex(hex) == null,
                        label = { Text(stringResource(R.string.color_hex)) },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = { onPick(color) }) { Text(stringResource(R.string.dialog_ok)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) } },
    )
}

@Composable
private fun SwatchGrid(
    selected: Color,
    onPick: (Color) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Swatches.chunked(6).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                row.forEach { c ->
                    val isSelected = c.toArgb() == selected.toArgb()
                    Box(
                        modifier =
                            Modifier
                                .size(36.dp)
                                .background(c, CircleShape)
                                // A hairline only on near-white swatches, which vanish on light surfaces.
                                .then(if (c.luminance() > 0.85f) Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape) else Modifier)
                                .clickable { onPick(c) },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isSelected) {
                            Icon(Icons.Rounded.Check, null, tint = if (c.luminance() > 0.5f) Color.Black else Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

/** Hue around the circle, saturation from the centre out. */
@Composable
private fun HueSatWheel(
    hue: Float,
    sat: Float,
    value: Float,
    onChange: (Float, Float) -> Unit,
) {
    val hues = (0..6).map { Color.hsv(it * 60f % 360f, 1f, value) }
    Canvas(
        modifier =
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .pointerInput(Unit) {
                    fun pick(p: Offset) {
                        val c = Offset(size.width / 2f, size.height / 2f)
                        val r = min(size.width, size.height) / 2f
                        val d = p - c
                        val h = ((Math.toDegrees(atan2(d.y, d.x).toDouble()) + 360) % 360).toFloat()
                        onChange(h, (hypot(d.x, d.y) / r).coerceIn(0f, 1f))
                    }
                    detectTapGestures { pick(it) }
                }.pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        val c = Offset(size.width / 2f, size.height / 2f)
                        val r = min(size.width, size.height) / 2f
                        val d = change.position - c
                        val h = ((Math.toDegrees(atan2(d.y, d.x).toDouble()) + 360) % 360).toFloat()
                        onChange(h, (hypot(d.x, d.y) / r).coerceIn(0f, 1f))
                    }
                },
    ) {
        val r = size.minDimension / 2f
        drawCircle(Brush.sweepGradient(hues), r)
        drawCircle(Brush.radialGradient(listOf(Color.hsv(0f, 0f, value), Color.Transparent), radius = r), r)
        val a = Math.toRadians(hue.toDouble())
        val p = center + Offset((cos(a) * r * sat).toFloat(), (sin(a) * r * sat).toFloat())
        drawCircle(Color.White, 10.dp.toPx(), p, style = Stroke(3.dp.toPx()))
    }
}

@Composable
private fun LabeledSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
) {
    Column {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Slider(value = value, onValueChange = onChange, valueRange = range)
    }
}

private fun toHex(c: Color) = "#%06X".format(c.toArgb() and 0xFFFFFF)

private fun parseHex(text: String): Color? =
    Regex("^#?([0-9A-Fa-f]{6})$").find(text)?.groupValues?.get(1)?.toLong(16)?.let { Color(0xFF000000 or it) }
