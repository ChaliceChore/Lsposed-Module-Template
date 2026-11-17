@file:Suppress("ktlint:standard:function-naming")

package io.github.chalicechore.moduletemplate.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import io.github.chalicechore.moduletemplate.R
import io.github.chalicechore.moduletemplate.ui.theme.Tokens
import kotlin.math.roundToInt
import androidx.compose.ui.draw.alpha
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Add
import androidx.compose.foundation.layout.Spacer

/**
 * #8 A "PREVIEW" frame for drawing the target's UI at its real size. The module draws [content];
 * the template provides the frame. Put it first in a category, or pin it at the top of an
 * options sheet (see [OptionsSheet]) so it stays visible while the options scroll.
 */
@Composable
fun PreviewBlock(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(Tokens.OuterRowCorner),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 16.dp, horizontal = Tokens.RowPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(R.string.preview_label).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            content()
        }
    }
}

/**
 * #10 A row whose summary lists what's on ("Alerting, Silent"); tapping it expands in place
 * into one switch per item.
 */
@Composable
fun <T> ExpandableSwitchRow(
    shape: Shape,
    icon: ImageVector,
    title: String,
    items: List<T>,
    label: @Composable (T) -> String,
    isOn: (T) -> Boolean,
    onChange: (T, Boolean) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val on = items.filter(isOn)
    val summary =
        when (on.size) {
            items.size -> stringResource(R.string.expandable_all)
            0 -> stringResource(R.string.expandable_none)
            else -> on.map { label(it) }.joinToString(", ")
        }
    val arrow by animateFloatAsState(if (expanded) 180f else 0f, label = "expandArrow")
    Surface(shape = shape, color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { expanded = !expanded }
                        .heightIn(min = Tokens.RowMinHeight)
                        .padding(horizontal = Tokens.RowPadding, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Column(Modifier.weight(1f).padding(horizontal = Tokens.RowPadding)) {
                    Text(title, style = MaterialTheme.typography.bodyLarge)
                    Text(summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.Outlined.ExpandMore, contentDescription = null, modifier = Modifier.rotate(arrow))
            }
            AnimatedVisibility(expanded) {
                Column(Modifier.padding(start = 56.dp, end = Tokens.RowPadding, bottom = 8.dp)) {
                    items.forEach { item ->
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .toggleable(isOn(item), role = Role.Switch) { onChange(item, it) },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(label(item), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                            Switch(checked = isOn(item), onCheckedChange = null)
                        }
                    }
                }
            }
        }
    }
}

/** #12 A row of filter chips for 2 to 5 fixed choices; an alternative to a radio dialog. */
@Composable
fun <T> ChipSelectorRow(
    shape: Shape,
    title: String,
    options: List<T>,
    label: @Composable (T) -> String,
    selected: T,
    onSelect: (T) -> Unit,
) {
    Surface(shape = shape, color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(Tokens.RowPadding), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { option ->
                    val on = option == selected
                    FilterChip(
                        selected = on,
                        onClick = { onSelect(option) },
                        label = { Text(label(option)) },
                        leadingIcon =
                            if (on) {
                                { Icon(Icons.Outlined.Check, null, Modifier.size(FilterChipDefaults.IconSize)) }
                            } else {
                                null
                            },
                    )
                }
            }
        }
    }
}

/**
 * #13 "Default (4 s)" or a custom value, picked in a dialog with a slider.
 *
 * @param value seconds, or null for the default.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DurationRow(
    shape: Shape,
    icon: ImageVector,
    title: String,
    value: Int?,
    default: Int,
    range: IntRange,
    onChange: (Int?) -> Unit,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    NavigationRow(
        shape = shape,
        icon = icon,
        title = title,
        summary =
            if (value == null) {
                stringResource(R.string.duration_default, default)
            } else {
                pluralStringResource(R.plurals.duration_seconds, value, value)
            },
        onClick = { open = true },
        trailing = RowTrailing.None,
    )
    if (!open) return
    var picked by rememberSaveable { mutableFloatStateOf((value ?: default).toFloat()) }
    val seconds = picked.roundToInt()
    fun nudge(by: Int) {
        picked = (seconds + by).coerceIn(range.first, range.last).toFloat()
    }
    BasicAlertDialog(onDismissRequest = { open = false }) {
        Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 16.dp)) {
                Text(title, style = MaterialTheme.typography.headlineSmall)
                // Big value, with a "Default" tag when it matches the default.
                Column(
                    Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        pluralStringResource(R.plurals.duration_seconds, seconds, seconds),
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        stringResource(R.string.duration_is_default),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.alpha(if (seconds == default) 1f else 0f),
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalIconButton(onClick = { nudge(-1) }, enabled = seconds > range.first) {
                        Icon(Icons.Rounded.Remove, contentDescription = stringResource(R.string.duration_less))
                    }
                    Slider(
                        value = picked,
                        onValueChange = { picked = it },
                        valueRange = range.first.toFloat()..range.last.toFloat(),
                        steps = range.last - range.first - 1,
                        modifier = Modifier.weight(1f),
                    )
                    FilledTonalIconButton(onClick = { nudge(1) }, enabled = seconds < range.last) {
                        Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.duration_more))
                    }
                }
                Row(Modifier.fillMaxWidth().padding(horizontal = 56.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${range.first} s", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${range.last} s", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                // Default only moves the slider; OK saves.
                Row(Modifier.fillMaxWidth().padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { picked = default.toFloat() }, enabled = seconds != default) {
                        Text(stringResource(R.string.duration_reset))
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { open = false }) { Text(stringResource(R.string.dialog_cancel)) }
                    TextButton(onClick = {
                        onChange(if (seconds == default) null else seconds)
                        open = false
                    }) { Text(stringResource(R.string.dialog_ok)) }
                }
            }
        }
    }
}

/**
 * #16 A card with an input and a live result, so users can try what the module does without
 * leaving the app. The module supplies [transform].
 */
@Composable
fun TryOutCard(
    title: String,
    inputLabel: String,
    initial: String,
    transform: (String) -> String,
    modifier: Modifier = Modifier,
) {
    var input by rememberSaveable { mutableStateOf(initial) }
    Surface(
        shape = RoundedCornerShape(Tokens.OuterRowCorner),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(Tokens.RowPadding), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                label = { Text(inputLabel) },
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                transform(input),
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(8.dp))
                        .padding(12.dp),
            )
        }
    }
}
