@file:Suppress("ktlint:standard:function-naming")

package io.github.chalicechore.moduletemplate.ui.screen

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.RadioButtonChecked
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.chalicechore.moduletemplate.R
import io.github.chalicechore.moduletemplate.prefs.Choice
import io.github.chalicechore.moduletemplate.prefs.FeatureCategory
import io.github.chalicechore.moduletemplate.prefs.FeatureHealth
import io.github.chalicechore.moduletemplate.prefs.FeatureSettings
import io.github.chalicechore.moduletemplate.prefs.FeatureSwitch
import io.github.chalicechore.moduletemplate.prefs.SettingsKeys
import io.github.chalicechore.moduletemplate.ui.ChoiceCategory
import io.github.chalicechore.moduletemplate.ui.component.DetailScaffold
import io.github.chalicechore.moduletemplate.ui.component.NavigationRow
import io.github.chalicechore.moduletemplate.ui.component.RowGroup
import io.github.chalicechore.moduletemplate.ui.component.RowTrailing
import io.github.chalicechore.moduletemplate.ui.component.SectionHeader
import io.github.chalicechore.moduletemplate.ui.component.SwitchRow
import io.github.chalicechore.moduletemplate.ui.component.centeredContentWidth
import io.github.chalicechore.moduletemplate.ui.icon
import io.github.chalicechore.moduletemplate.ui.labelRes
import io.github.chalicechore.moduletemplate.ui.summaryRes
import io.github.chalicechore.moduletemplate.ui.theme.Tokens
import io.github.chalicechore.moduletemplate.ui.titleRes
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.SettingsBackupRestore
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.foundation.layout.Arrangement

/** One feature category: its switches, plus the single-choice option where it belongs. */
@Composable
fun CategoryScreen(
    category: FeatureCategory,
    settings: FeatureSettings,
    onSwitchChange: (FeatureSwitch, Boolean) -> Unit,
    onChoiceChange: (Choice) -> Unit,
    onBack: () -> Unit,
    failed: Map<FeatureSwitch, FeatureHealth.Failed> = emptyMap(),
    highlight: String? = null,
    onReset: (() -> Unit)? = null,
) {
    val flash = rememberHighlightFlash(highlight)
    var reason by rememberSaveable { mutableStateOf<String?>(null) }
    val target = stringResource(R.string.target_label)
    var showChoiceDialog by rememberSaveable { mutableStateOf(false) }
    val switches = remember(category) { FeatureSwitch.entries.filter { it.category == category } }
    val resetTo = remember(settings, category) { settings.resetCategory(category, category == ChoiceCategory) }
    var menu by remember { mutableStateOf(false) }
    var confirmReset by rememberSaveable { mutableStateOf(false) }
    DetailScaffold(
        title = stringResource(category.titleRes),
        onBack = onBack,
        actions = {
            if (onReset != null) {
                Box {
                    IconButton(onClick = { menu = true }) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = stringResource(R.string.more_options))
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.reset_category)) },
                            leadingIcon = { Icon(Icons.Outlined.SettingsBackupRestore, null) },
                            enabled = resetTo != settings,
                            onClick = {
                                menu = false
                                confirmReset = true
                            },
                        )
                    }
                }
            }
        },
    ) { contentPadding ->
        Column(
            modifier =
                Modifier
                    .centeredContentWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(contentPadding),
        ) {
            if (switches.any { it in failed }) BrokenBanner(target)
            RowGroup(
                rows =
                    switches.map { switch ->
                        { shape ->
                            SwitchRow(
                                shape = shape,
                                icon = switch.icon,
                                title = stringResource(switch.titleRes),
                                summary = stringResource(switch.summaryRes),
                                checked = settings.isOn(switch),
                                onCheckedChange = { onSwitchChange(switch, it) },
                                modifier = Modifier.highlightFlash(shape, flash.takeIf { switch.key == highlight }, MaterialTheme.colorScheme.primary),
                                below =
                                    failed[switch]?.let { f ->
                                        {
                                            BrokenChip(target) { reason = f.reason }
                                        }
                                    },
                            )
                        }
                    },
                modifier = Modifier.padding(top = Tokens.ScreenMargin),
            )
            if (category == ChoiceCategory) {
                SectionHeader(stringResource(R.string.section_options))
                RowGroup(
                    rows =
                        listOf { shape ->
                            NavigationRow(
                                shape = shape,
                                icon = Icons.Outlined.RadioButtonChecked,
                                title = stringResource(R.string.option_choice_title),
                                summary = stringResource(settings.choice.labelRes),
                                onClick = { showChoiceDialog = true },
                                trailing = RowTrailing.None,
                                modifier = Modifier.highlightFlash(shape, flash.takeIf { highlight == SettingsKeys.CHOICE }, MaterialTheme.colorScheme.primary),
                            )
                        },
                )
            }
        }
    }
    if (confirmReset && onReset != null) {
        val on = stringResource(R.string.pending_change_on)
        val off = stringResource(R.string.pending_change_off)
        val lines =
            settings.differingSwitches(resetTo).sorted().map {
                "${stringResource(it.titleRes)}: ${if (resetTo.isOn(it)) on else off}"
            } +
                listOfNotNull(
                    if (resetTo.choice != settings.choice) {
                        "${stringResource(R.string.option_choice_title)}: ${stringResource(resetTo.choice.labelRes)}"
                    } else {
                        null
                    },
                )
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            icon = { Icon(Icons.Outlined.SettingsBackupRestore, null) },
            title = { Text(stringResource(R.string.reset_category_title, stringResource(category.titleRes))) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.reset_category_body))
                    lines.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmReset = false
                    onReset()
                }) { Text(stringResource(R.string.reset_confirm)) }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text(stringResource(R.string.dialog_cancel)) } },
        )
    }
    reason?.let { text ->
        AlertDialog(
            onDismissRequest = { reason = null },
            title = { Text(stringResource(R.string.broken_reason_title)) },
            text = { Text(text) },
            confirmButton = { TextButton(onClick = { reason = null }) { Text(stringResource(R.string.dialog_ok)) } },
        )
    }
    if (showChoiceDialog) {
        ChoiceDialog(
            current = settings.choice,
            onConfirm = {
                onChoiceChange(it)
                showChoiceDialog = false
            },
            onDismiss = { showChoiceDialog = false },
        )
    }
}

@Composable
private fun ChoiceDialog(
    current: Choice,
    onConfirm: (Choice) -> Unit,
    onDismiss: () -> Unit,
) {
    var selected by rememberSaveable { mutableStateOf(current) }
    val haptics = LocalHapticFeedback.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.option_choice_title)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()).selectableGroup()) {
                Choice.entries.forEach { choice ->
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .selectable(
                                    selected = selected == choice,
                                    role = Role.RadioButton,
                                    onClick = {
                                        if (selected != choice) {
                                            haptics.performHapticFeedback(
                                                HapticFeedbackType.SegmentTick,
                                            )
                                            selected = choice
                                        }
                                    },
                                ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = selected == choice, onClick = null)
                        Spacer(Modifier.width(16.dp))
                        Text(
                            text = stringResource(choice.labelRes),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selected) }) {
                Text(stringResource(R.string.dialog_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) }
        },
    )
}

/** Red "Doesn't work in this <target> version" label; tapping it shows the reason. */
@Composable
private fun BrokenChip(
    target: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier.padding(top = 6.dp),
    ) {
        Text(
            stringResource(R.string.broken_chip, target),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

@Composable
private fun BrokenBanner(target: String) {
    Surface(
        shape = RoundedCornerShape(Tokens.OuterRowCorner),
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier.fillMaxWidth().padding(horizontal = Tokens.ScreenMargin).padding(top = Tokens.ScreenMargin),
    ) {
        Row(Modifier.padding(Tokens.RowPadding), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
            Spacer(Modifier.width(12.dp))
            Text(
                stringResource(R.string.broken_banner, target),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
}

/** 0..1 strength of the "you searched for this" flash; fades out over about two seconds. */
@Composable
private fun rememberHighlightFlash(highlight: String?): Animatable<Float, *> {
    val flash = remember { Animatable(0f) }
    LaunchedEffect(highlight) {
        if (highlight == null) return@LaunchedEffect
        repeat(2) {
            flash.animateTo(1f, tween(250))
            flash.animateTo(0.2f, tween(450))
        }
        flash.animateTo(0f, tween(600))
    }
    return flash
}

private fun Modifier.highlightFlash(
    shape: Shape,
    flash: Animatable<Float, *>?,
    color: Color,
): Modifier =
    if (flash == null) {
        this
    } else {
        this.clip(shape).drawWithContent {
            drawContent()
            drawRect(color.copy(alpha = 0.24f * flash.value))
        }
    }
