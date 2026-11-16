@file:Suppress("ktlint:standard:function-naming")

package io.github.chalicechore.moduletemplate.ui.screen

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AppShortcut
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.outlined.Colorize
import androidx.compose.material.icons.outlined.Contrast
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.chalicechore.moduletemplate.R
import io.github.chalicechore.moduletemplate.prefs.Appearance
import io.github.chalicechore.moduletemplate.prefs.ThemeMode
import io.github.chalicechore.moduletemplate.ui.component.AdaptiveButtonRow
import io.github.chalicechore.moduletemplate.ui.component.DetailScaffold
import io.github.chalicechore.moduletemplate.ui.component.RowGroup
import io.github.chalicechore.moduletemplate.ui.component.SectionHeader
import io.github.chalicechore.moduletemplate.ui.component.SwitchRow
import io.github.chalicechore.moduletemplate.ui.component.centeredContentWidth
import io.github.chalicechore.moduletemplate.ui.component.isLauncherIconVisible
import io.github.chalicechore.moduletemplate.ui.component.rememberLauncherIconVisible
import io.github.chalicechore.moduletemplate.ui.component.setLauncherIconVisible
import io.github.chalicechore.moduletemplate.ui.component.shapeForPosition
import io.github.chalicechore.moduletemplate.ui.theme.Tokens

@Composable
fun AppearanceScreen(
    appearance: Appearance,
    onThemeModeChange: (ThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onPureBlackChange: (Boolean) -> Unit,
    onBack: () -> Unit,
) {
    DetailScaffold(
        title = stringResource(R.string.appearance_title),
        onBack = onBack,
    ) { contentPadding ->
        Column(
            modifier =
                Modifier
                    .centeredContentWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(contentPadding),
        ) {
            val rows =
                buildList<@Composable (Shape) -> Unit> {
                    add { shape ->
                        ThemeModeRow(
                            shape = shape,
                            selected = appearance.themeMode,
                            onSelect = onThemeModeChange,
                        )
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        add { shape ->
                            SwitchRow(
                                shape = shape,
                                icon = Icons.Outlined.Colorize,
                                title = stringResource(R.string.dynamic_color_title),
                                summary = stringResource(R.string.dynamic_color_summary),
                                checked = appearance.dynamicColor,
                                onCheckedChange = onDynamicColorChange,
                            )
                        }
                    }
                    add { shape ->
                        SwitchRow(
                            shape = shape,
                            icon = Icons.Outlined.Contrast,
                            title = stringResource(R.string.pure_black_title),
                            summary = stringResource(R.string.pure_black_summary),
                            checked = appearance.pureBlack,
                            onCheckedChange = onPureBlackChange,
                            // Light theme never shows the dark surfaces this changes.
                            enabled = appearance.themeMode != ThemeMode.LIGHT,
                        )
                    }
                }
            RowGroup(rows = rows, modifier = Modifier.padding(top = Tokens.ScreenMargin))
            LauncherIconGroup()
        }
    }
}

@Composable
private fun LauncherIconGroup() {
    val context = LocalContext.current
    var visible by rememberLauncherIconVisible()
    var confirmHide by rememberSaveable { mutableStateOf(false) }
    SectionHeader(stringResource(R.string.section_launcher))
    RowGroup(
        rows =
            listOf { shape ->
                SwitchRow(
                    shape = shape,
                    icon = Icons.Outlined.AppShortcut,
                    title = stringResource(R.string.launcher_icon_title),
                    summary = stringResource(R.string.launcher_icon_summary),
                    checked = visible,
                    onCheckedChange = { show ->
                        if (show) {
                            setLauncherIconVisible(context, true)
                            visible = isLauncherIconVisible(context)
                        } else {
                            confirmHide = true
                        }
                    },
                )
            },
    )
    if (confirmHide) {
        AlertDialog(
            onDismissRequest = { confirmHide = false },
            title = { Text(stringResource(R.string.launcher_icon_hide_title)) },
            text = { Text(stringResource(R.string.launcher_icon_hide_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmHide = false
                        setLauncherIconVisible(context, false)
                        visible = isLauncherIconVisible(context)
                    },
                ) { Text(stringResource(R.string.launcher_icon_hide_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmHide = false }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            },
        )
    }
}

private val ThemeModes =
    listOf(
        Triple(ThemeMode.SYSTEM, Icons.Outlined.BrightnessAuto, R.string.theme_system),
        Triple(ThemeMode.LIGHT, Icons.Outlined.LightMode, R.string.theme_light),
        Triple(ThemeMode.DARK, Icons.Outlined.DarkMode, R.string.theme_dark),
    )

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ThemeModeRow(
    shape: Shape,
    selected: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(Tokens.RowPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.appearance_theme),
                style = MaterialTheme.typography.bodyLarge,
            )
            AdaptiveButtonRow(
                items = ThemeModes,
                modifier = Modifier.selectableGroup(),
            ) { index, (mode, icon, label), stacked, itemModifier ->
                ToggleButton(
                    checked = mode == selected,
                    onCheckedChange = {
                        if (mode != selected) {
                            haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                            onSelect(mode)
                        }
                    },
                    modifier = itemModifier.semantics { role = Role.RadioButton },
                    shapes =
                        when {
                            stacked -> {
                                ToggleButtonDefaults.shapesFor(ButtonDefaults.MinHeight)
                            }

                            index == 0 -> {
                                ButtonGroupDefaults.connectedLeadingButtonShapes()
                            }

                            index == ThemeModes.lastIndex -> {
                                ButtonGroupDefaults
                                    .connectedTrailingButtonShapes()
                            }

                            else -> {
                                ButtonGroupDefaults.connectedMiddleButtonShapes()
                            }
                        },
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(ToggleButtonDefaults.IconSize),
                    )
                    Spacer(Modifier.width(ToggleButtonDefaults.IconSpacing))
                    Text(text = stringResource(label), maxLines = 1)
                }
            }
        }
    }
}
