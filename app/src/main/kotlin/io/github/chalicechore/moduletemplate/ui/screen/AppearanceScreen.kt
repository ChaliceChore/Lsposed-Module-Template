@file:Suppress("ktlint:standard:function-naming")

package io.github.chalicechore.moduletemplate.ui.screen

import android.os.Build
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material3.RadioButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import io.github.chalicechore.moduletemplate.HomeLayout
import io.github.chalicechore.moduletemplate.ui.AppLanguages
import io.github.chalicechore.moduletemplate.ui.component.NavigationRow
import io.github.chalicechore.moduletemplate.ui.component.RowTrailing
import io.github.chalicechore.moduletemplate.ui.currentAppLanguage
import io.github.chalicechore.moduletemplate.ui.languageName
import io.github.chalicechore.moduletemplate.ui.setAppLanguage
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
import io.github.chalicechore.moduletemplate.ui.TranslatedLanguages
import androidx.compose.ui.draw.alpha
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.runtime.remember
import java.text.Collator
import java.util.Locale

@Composable
fun AppearanceScreen(
    appearance: Appearance,
    onThemeModeChange: (ThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onPureBlackChange: (Boolean) -> Unit,
    onHomeLayoutChange: (HomeLayout) -> Unit,
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
                        SegmentedChoiceRow(
                            shape = shape,
                            title = stringResource(R.string.appearance_theme),
                            options = ThemeModes,
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
            SectionHeader(stringResource(R.string.section_layout_language))
            RowGroup(
                rows =
                    buildList<@Composable (Shape) -> Unit> {
                        if (AppLanguages.size > 1) add { shape -> LanguageRow(shape) }
                        add { shape ->
                            SegmentedChoiceRow(
                                shape = shape,
                                title = stringResource(R.string.home_layout_title),
                                options = HomeLayouts,
                                selected = appearance.homeLayout,
                                onSelect = onHomeLayoutChange,
                            )
                        }
                    },
            )
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

private val HomeLayouts =
    listOf(
        Triple(HomeLayout.LIST, Icons.Outlined.ViewAgenda, R.string.home_layout_list),
        Triple(HomeLayout.GRID, Icons.Outlined.GridView, R.string.home_layout_grid),
    )

/** A row with a title over a group of connected toggle buttons, one per option. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun <T> SegmentedChoiceRow(
    shape: Shape,
    title: String,
    options: List<Triple<T, ImageVector, Int>>,
    selected: T,
    onSelect: (T) -> Unit,
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
                text = title,
                style = MaterialTheme.typography.bodyLarge,
            )
            AdaptiveButtonRow(
                items = options,
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

                            index == options.lastIndex -> {
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

/**
 * "Language": System default, then the translated languages, then a "Not translated yet" group
 * (greyed out). Each group is sorted by the languages' own names, and each row also shows the
 * name in the current language ("Deutsch · German") so it is easy to find.
 */
@Composable
private fun LanguageRow(shape: Shape) {
    var open by rememberSaveable { mutableStateOf(false) }
    val current = currentAppLanguage()
    val systemDefault = stringResource(R.string.language_system_default)
    NavigationRow(
        shape = shape,
        icon = Icons.Outlined.Language,
        title = stringResource(R.string.language_title),
        summary = current?.let(::languageName) ?: systemDefault,
        onClick = { open = true },
        trailing = RowTrailing.None,
    )
    if (!open) return
    val haptics = LocalHapticFeedback.current
    val uiLocale = LocalConfiguration.current.locales[0]
    val collator = remember(uiLocale) { Collator.getInstance(uiLocale) }
    val (translated, untranslated) =
        remember(collator) {
            AppLanguages.sortedWith(compareBy(collator) { languageName(it) }).partition { it in TranslatedLanguages }
        }
    @Composable
    fun option(tag: String?) {
        val available = tag == null || tag in TranslatedLanguages
        val own = tag?.let(::languageName) ?: systemDefault
        val local = tag?.let { Locale.forLanguageTag(it).getDisplayName(uiLocale) }?.takeIf { it != own }
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .alpha(if (available) 1f else 0.38f)
                    .selectable(
                        selected = tag == current,
                        enabled = available,
                        role = Role.RadioButton,
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                            open = false
                            if (tag != current) setAppLanguage(tag)
                        },
                    ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = tag == current, onClick = null, enabled = available)
            Spacer(Modifier.width(16.dp))
            Column {
                Text(own, style = MaterialTheme.typography.bodyLarge)
                if (local != null) {
                    Text(local, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
    AlertDialog(
        onDismissRequest = { open = false },
        title = { Text(stringResource(R.string.language_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()).selectableGroup()) {
                option(null)
                HorizontalDivider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
                translated.forEach { option(it) }
                if (untranslated.isNotEmpty()) {
                    Text(
                        stringResource(R.string.language_not_translated),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
                    )
                    untranslated.forEach { option(it) }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { open = false }) { Text(stringResource(R.string.dialog_cancel)) }
        },
    )
}
