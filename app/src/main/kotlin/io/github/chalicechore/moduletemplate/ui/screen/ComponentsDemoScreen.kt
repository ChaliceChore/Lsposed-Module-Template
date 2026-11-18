@file:Suppress("ktlint:standard:function-naming")

package io.github.chalicechore.moduletemplate.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.chalicechore.moduletemplate.R
import io.github.chalicechore.moduletemplate.ui.component.AppPickerDialog
import io.github.chalicechore.moduletemplate.ui.component.AppMultiPickerDialog
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.ui.res.pluralStringResource
import io.github.chalicechore.moduletemplate.ui.component.ChipSelectorRow
import io.github.chalicechore.moduletemplate.ui.component.ColorPickerDialog
import io.github.chalicechore.moduletemplate.ui.component.DetailScaffold
import io.github.chalicechore.moduletemplate.ui.component.DurationRow
import io.github.chalicechore.moduletemplate.ui.component.ExpandableSwitchRow
import io.github.chalicechore.moduletemplate.ui.component.NavigationRow
import io.github.chalicechore.moduletemplate.ui.component.OptionsSheet
import io.github.chalicechore.moduletemplate.ui.component.PreviewBlock
import io.github.chalicechore.moduletemplate.ui.component.RowGroup
import io.github.chalicechore.moduletemplate.ui.component.RowTrailing
import io.github.chalicechore.moduletemplate.ui.component.SectionHeader
import io.github.chalicechore.moduletemplate.ui.component.SplitOptionsRow
import io.github.chalicechore.moduletemplate.ui.component.SwitchRow
import io.github.chalicechore.moduletemplate.ui.component.TryOutCard
import io.github.chalicechore.moduletemplate.ui.component.centeredContentWidth
import io.github.chalicechore.moduletemplate.ui.theme.BrandSeed
import io.github.chalicechore.moduletemplate.ui.theme.Tokens
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.imeAnimationTarget
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.ExperimentalLayoutApi

private val Weights = listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold)
private val WeightNames = listOf(R.string.demo_weight_regular, R.string.demo_weight_medium, R.string.demo_weight_semibold, R.string.demo_weight_bold)
private val Kinds = listOf(R.string.demo_kind_alerting, R.string.demo_kind_silent, R.string.demo_kind_ongoing)

/**
 * TEMPLATE: demo of every optional component (ModuleConfig.DEMO_COMPONENTS). State here is local
 * and not saved. Delete this file and FeatureCategory.COMPONENTS in a real module.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ComponentsDemoScreen(onBack: () -> Unit) {
    var color by rememberSaveable { mutableIntStateOf(BrandSeed.toArgb()) }
    var weight by rememberSaveable { mutableIntStateOf(1) }
    var custom by rememberSaveable { mutableStateOf(true) }
    var rounded by rememberSaveable { mutableStateOf(true) }
    var kinds by rememberSaveable { mutableStateOf(setOf(0, 1)) }
    var seconds by rememberSaveable { mutableStateOf<Int?>(null) }
    var app by rememberSaveable { mutableStateOf<String?>(null) }
    var appLabel by rememberSaveable { mutableStateOf<String?>(null) }
    var sheet by rememberSaveable { mutableStateOf(false) }
    var colorDialog by rememberSaveable { mutableStateOf(false) }
    var appDialog by rememberSaveable { mutableStateOf(false) }
    var apps by rememberSaveable { mutableStateOf(ArrayList<String>()) }
    var appsLabels by rememberSaveable { mutableStateOf(ArrayList<String>()) }
    var appsDialog by rememberSaveable { mutableStateOf(false) }
    val preview: @Composable () -> Unit = {
        DemoPreview(Color(color), Weights[weight], custom, rounded)
    }
    DetailScaffold(title = stringResource(R.string.category_components), onBack = onBack) { contentPadding ->
        Column(
            Modifier
                .centeredContentWidth()
                .verticalScroll(rememberScrollState())
                .padding(contentPadding),
        ) {
            Text(
                stringResource(R.string.demo_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = Tokens.RowPadding + 4.dp, vertical = 12.dp),
            )
            SectionHeader(stringResource(R.string.demo_preview))
            PreviewBlock(Modifier.padding(horizontal = Tokens.ScreenMargin)) { preview() }
            SectionHeader(stringResource(R.string.demo_rows))
            RowGroup(
                rows =
                    listOf<@Composable (Shape) -> Unit>(
                        { shape ->
                            SplitOptionsRow(
                                shape = shape,
                                icon = Icons.Outlined.Tune,
                                title = stringResource(R.string.demo_split_title),
                                summary = stringResource(if (custom) R.string.demo_split_on else R.string.demo_split_off),
                                checked = custom,
                                onCheckedChange = { custom = it },
                                onOpenOptions = { sheet = true },
                            )
                        },
                        { shape ->
                            ExpandableSwitchRow(
                                shape = shape,
                                icon = Icons.Outlined.Category,
                                title = stringResource(R.string.demo_expandable_title),
                                items = Kinds.indices.toList(),
                                label = { stringResource(Kinds[it]) },
                                isOn = { it in kinds },
                                onChange = { i, on -> kinds = if (on) kinds + i else kinds - i },
                            )
                        },
                        { shape ->
                            ChipSelectorRow(
                                shape = shape,
                                title = stringResource(R.string.demo_chips_title),
                                options = Weights.indices.toList(),
                                label = { stringResource(WeightNames[it]) },
                                selected = weight,
                                onSelect = { weight = it },
                            )
                        },
                        { shape ->
                            DurationRow(
                                shape = shape,
                                icon = Icons.Outlined.Timer,
                                title = stringResource(R.string.demo_duration_title),
                                value = seconds,
                                default = 4,
                                range = 2..15,
                                onChange = { seconds = it },
                            )
                        },
                        { shape ->
                            NavigationRow(
                                shape = shape,
                                icon = Icons.Outlined.Palette,
                                title = stringResource(R.string.demo_color_title),
                                summary = "#%06X".format(color and 0xFFFFFF),
                                onClick = { colorDialog = true },
                                trailing = RowTrailing.None,
                            )
                        },
                        { shape ->
                            NavigationRow(
                                shape = shape,
                                icon = Icons.Outlined.Apps,
                                title = stringResource(R.string.demo_app_title),
                                summary = appLabel ?: stringResource(R.string.demo_app_none),
                                onClick = { appDialog = true },
                                trailing = RowTrailing.None,
                            )
                        },
                        { shape ->
                            NavigationRow(
                                shape = shape,
                                icon = Icons.Outlined.Checklist,
                                title = stringResource(R.string.demo_apps_title),
                                summary =
                                    if (apps.isEmpty()) {
                                        stringResource(R.string.demo_apps_none)
                                    } else {
                                        val names =
                                            appsLabels.take(2).joinToString(", ") +
                                                if (appsLabels.size > 2) ", +${appsLabels.size - 2}" else ""
                                        pluralStringResource(R.plurals.demo_apps_summary, apps.size, apps.size, names)
                                    },
                                onClick = { appsDialog = true },
                                trailing = RowTrailing.None,
                            )
                        },
                    ),
            )
            SectionHeader(stringResource(R.string.demo_try_out))
            TryOutCard(
                title = stringResource(R.string.demo_try_out_title),
                inputLabel = stringResource(R.string.demo_try_out_label),
                initial = "Hello, LSPosed",
                transform = { it.reversed() },
                modifier = Modifier.padding(horizontal = Tokens.ScreenMargin).padding(bottom = 24.dp),
            )
            // Room for the keyboard at its final height, reserved once when it starts opening.
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.imeAnimationTarget))
        }
    }
    if (sheet) {
        OptionsSheet(
            title = stringResource(R.string.demo_split_title),
            checked = custom,
            onCheckedChange = { custom = it },
            onDismiss = { sheet = false },
            pinned = { PreviewBlock { preview() } },
        ) { enabled ->
            RowGroup(
                rows =
                    listOf<@Composable (Shape) -> Unit>(
                        { shape ->
                            SwitchRow(
                                shape = shape,
                                icon = Icons.Outlined.Tune,
                                title = stringResource(R.string.demo_rounded_title),
                                summary = stringResource(R.string.demo_rounded_summary),
                                checked = rounded,
                                onCheckedChange = { rounded = it },
                                enabled = enabled,
                            )
                        },
                        { shape ->
                            ChipSelectorRow(
                                shape = shape,
                                title = stringResource(R.string.demo_chips_title),
                                options = Weights.indices.toList(),
                                label = { stringResource(WeightNames[it]) },
                                selected = weight,
                                onSelect = { if (enabled) weight = it },
                            )
                        },
                    ) +
                        // Extra rows so the sheet has something to scroll under the pinned preview.
                        (1..20).map { n ->
                            { shape: Shape ->
                                NavigationRow(
                                    shape = shape,
                                    icon = Icons.Outlined.Category,
                                    title = stringResource(R.string.demo_filler_title, n),
                                    summary = stringResource(R.string.demo_filler_summary),
                                    onClick = {},
                                    trailing = RowTrailing.None,
                                    enabled = enabled,
                                )
                            }
                        },
            )
        }
    }
    if (colorDialog) {
        ColorPickerDialog(
            title = stringResource(R.string.demo_color_title),
            initial = Color(color),
            onPick = {
                color = it.toArgb()
                colorDialog = false
            },
            onDismiss = { colorDialog = false },
        )
    }
    if (appsDialog) {
        AppMultiPickerDialog(
            title = stringResource(R.string.demo_apps_title),
            selected = apps.toSet(),
            onDone = { picked ->
                apps = ArrayList(picked.map { it.packageName })
                appsLabels = ArrayList(picked.map { it.label })
                appsDialog = false
            },
            onDismiss = { appsDialog = false },
        )
    }
    if (appDialog) {
        AppPickerDialog(
            title = stringResource(R.string.demo_app_title),
            selected = app,
            onPick = {
                app = it.packageName
                appLabel = it.label
                appDialog = false
            },
            onDismiss = { appDialog = false },
        )
    }
}

/** A made-up "target UI": a pill button that follows the demo settings. */
@Composable
private fun DemoPreview(
    color: Color,
    weight: FontWeight,
    custom: Boolean,
    rounded: Boolean,
) {
    val fill = if (custom) color else MaterialTheme.colorScheme.secondary
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(
            Modifier
                .width(120.dp)
                .height(48.dp)
                .background(fill, if (custom && !rounded) RoundedCornerShape(8.dp) else RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(stringResource(R.string.demo_preview_button), color = Color.White, fontWeight = if (custom) weight else FontWeight.Medium)
        }
        Box(Modifier.size(40.dp).background(fill.copy(alpha = 0.3f), CircleShape))
    }
}
