@file:Suppress("ktlint:standard:function-naming")

package io.github.chalicechore.moduletemplate.ui.component

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.chalicechore.moduletemplate.R
import io.github.chalicechore.moduletemplate.ui.ApplyFailure
import io.github.chalicechore.moduletemplate.ui.theme.Tokens
import kotlinx.coroutines.flow.first

/** Height the apply sheet covers at the bottom; lists add it as bottom padding. */
val LocalApplySheetInset = compositionLocalOf { 0.dp }

/** One unapplied change, as listed in the sheet. */
data class PendingChange(
    @param:StringRes val titleRes: Int,
    @param:StringRes val areaRes: Int,
    val value: String,
    val highlighted: Boolean,
)

sealed interface ApplySheetContent {
    data class Pending(
        val canApply: Boolean,
        val changes: List<PendingChange>,
    ) : ApplySheetContent

    data class Failure(
        val reason: ApplyFailure,
    ) : ApplySheetContent
}

class FailureAction(
    @param:StringRes val label: Int,
    val onClick: () -> Unit,
)

private val CompactWindowHeight = 480.dp
private const val SUMMARY_NAMES_SHOWN = 2

@OptIn(ExperimentalMaterial3Api::class)
private fun ApplySheetContent?.sheetTarget(): SheetValue =
    when (this) {
        null -> SheetValue.Hidden
        is ApplySheetContent.Pending -> SheetValue.PartiallyExpanded
        is ApplySheetContent.Failure -> SheetValue.Expanded
    }

/**
 * Wraps the whole app. Peeks from the bottom while changes are unapplied (drag up to list
 * them), expands on failure, and hides when everything is applied.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplySheetScaffold(
    content: ApplySheetContent?,
    onApply: () -> Unit,
    onOpenTargetInfo: () -> Unit,
    modifier: Modifier = Modifier,
    body: @Composable () -> Unit,
) {
    var lastContent by remember { mutableStateOf(content) }
    SideEffect {
        if (content != null) lastContent = content
    }
    val shown by rememberUpdatedState(content != null)
    val keepShown = remember { { value: SheetValue -> value != SheetValue.Hidden || !shown } }
    val sheetState =
        rememberBottomSheetState(
            initialValue = content.sheetTarget(),
            confirmValueChange = keepShown,
        )
    var handleHeightPx by remember { mutableIntStateOf(0) }
    var peekBlockHeightPx by remember { mutableIntStateOf(0) }
    val peekHeight = with(LocalDensity.current) { (handleHeightPx + peekBlockHeightPx).toDp() }
    val target = content.sheetTarget()
    LaunchedEffect(target) {
        when (target) {
            SheetValue.Hidden -> {
                sheetState.hide()
            }

            SheetValue.PartiallyExpanded -> {
                snapshotFlow { peekBlockHeightPx }.first { it > 0 }
                sheetState.partialExpand()
            }

            SheetValue.Expanded -> {
                sheetState.expand()
            }
        }
    }
    val sideBySide =
        with(LocalDensity.current) {
            LocalWindowInfo.current.containerSize.height
                .toDp() < CompactWindowHeight
        }
    val failureMaxHeight =
        with(LocalDensity.current) {
            (
                LocalWindowInfo.current.containerSize.height -
                    WindowInsets.safeDrawing.getTop(this) - handleHeightPx
            ).coerceAtLeast(0)
                .toDp()
        }
    val bodyMotionScheme = MaterialTheme.motionScheme
    MaterialTheme(motionScheme = MotionScheme.expressive()) {
        BottomSheetScaffold(
            modifier = modifier,
            scaffoldState = rememberBottomSheetScaffoldState(bottomSheetState = sheetState),
            sheetPeekHeight = peekHeight,
            sheetDragHandle = {
                Box(Modifier.onSizeChanged { handleHeightPx = it.height }) {
                    BottomSheetDefaults.DragHandle()
                }
            },
            sheetContent = {
                when (val shownContent = content ?: lastContent) {
                    is ApplySheetContent.Pending -> {
                        PendingSheet(
                            content = shownContent,
                            sideBySide = sideBySide,
                            onApply = onApply,
                            onPeekBlockHeight = { peekBlockHeightPx = it },
                        )
                    }

                    is ApplySheetContent.Failure -> {
                        FailureSheet(
                            reason = shownContent.reason,
                            onTryAgain = onApply,
                            onOpenTargetInfo = onOpenTargetInfo,
                            maxHeight = failureMaxHeight,
                            onPeekBlockHeight = { peekBlockHeightPx = it },
                        )
                    }

                    null -> {}
                }
            },
        ) {
            val sheetVisible = content != null || sheetState.currentValue != SheetValue.Hidden
            val inset = if (sheetVisible) peekHeight else 0.dp
            MaterialTheme(motionScheme = bodyMotionScheme) {
                CompositionLocalProvider(LocalApplySheetInset provides inset, content = body)
            }
        }
    }
}

@Composable
private fun SheetMessage(
    title: String,
    body: String,
    modifier: Modifier,
) {
    Column(modifier = modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun Modifier.peekBlock(onHeight: (Int) -> Unit): Modifier =
    fillMaxWidth()
        .onSizeChanged { onHeight(it.height) }
        .navigationBarsPadding()
        .padding(start = Tokens.RowPadding, end = Tokens.RowPadding, bottom = Tokens.RowPadding)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ColumnScope.PendingSheet(
    content: ApplySheetContent.Pending,
    sideBySide: Boolean,
    onApply: () -> Unit,
    onPeekBlockHeight: (Int) -> Unit,
) {
    val count = content.changes.size
    val title = pluralStringResource(R.plurals.pending_changes, count, count)
    val summary =
        if (content.canApply) {
            changedAreasSummary(content.changes)
        } else {
            stringResource(R.string.pending_saved_on_device)
        }
    val applyButton: @Composable (Modifier) -> Unit = { buttonModifier ->
        val height = ButtonDefaults.MediumContainerHeight
        Button(
            onClick = onApply,
            enabled = content.canApply,
            shapes = ButtonDefaults.shapes(),
            modifier = buttonModifier.heightIn(min = height),
            contentPadding = ButtonDefaults.contentPaddingFor(height),
        ) {
            Icon(
                imageVector = Icons.Outlined.RestartAlt,
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.iconSizeFor(height)),
            )
            Spacer(Modifier.width(ButtonDefaults.iconSpacingFor(height)))
            Text(stringResource(R.string.apply_and_restart, stringResource(R.string.target_label)))
        }
    }
    if (sideBySide) {
        Row(
            modifier = Modifier.peekBlock(onPeekBlockHeight),
            horizontalArrangement = Arrangement.spacedBy(Tokens.RowPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SheetMessage(title, summary, Modifier.weight(1f))
            applyButton(Modifier)
        }
    } else {
        Column(
            modifier = Modifier.peekBlock(onPeekBlockHeight),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SheetMessage(title, summary, Modifier.fillMaxWidth())
            applyButton(Modifier.fillMaxWidth())
        }
    }
    HorizontalDivider(modifier = Modifier.padding(horizontal = Tokens.RowPadding))
    Column(
        modifier =
            Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(vertical = 8.dp),
    ) {
        content.changes.forEach { change -> PendingChangeRow(change) }
    }
}

@Composable
private fun PendingChangeRow(change: PendingChange) {
    ListItem(
        supportingContent = { Text(stringResource(change.areaRes)) },
        trailingContent = {
            Text(
                text = change.value,
                style = MaterialTheme.typography.labelLarge,
                color =
                    if (change.highlighted) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    ) {
        Text(stringResource(change.titleRes))
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun FailureSheet(
    reason: ApplyFailure,
    onTryAgain: () -> Unit,
    onOpenTargetInfo: () -> Unit,
    maxHeight: Dp,
    onPeekBlockHeight: (Int) -> Unit,
) {
    val target = stringResource(R.string.target_label)
    val actions =
        listOfNotNull(
            FailureAction(R.string.apply_app_info, onOpenTargetInfo).takeIf {
                reason == ApplyFailure.RestartFailed
            },
            FailureAction(R.string.apply_try_again, onTryAgain),
        )
    val (title, body) =
        when (reason) {
            ApplyFailure.NotSent -> {
                stringResource(R.string.apply_not_sent_title) to
                    stringResource(R.string.apply_not_sent_body)
            }

            ApplyFailure.RestartFailed -> {
                stringResource(R.string.apply_restart_failed_title, target) to
                    stringResource(R.string.apply_restart_failed_body, target)
            }
        }
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(max = maxHeight)
                .onSizeChanged { onPeekBlockHeight(it.height) }
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(
                        WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom,
                    ),
                ).padding(
                    start = Tokens.RowPadding,
                    end = Tokens.RowPadding,
                    bottom = Tokens.RowPadding,
                ),
        verticalArrangement = Arrangement.spacedBy(Tokens.RowPadding),
    ) {
        SheetMessage(title = title, body = body, modifier = Modifier.fillMaxWidth())
        AdaptiveButtonRow(items = actions) { _, action, _, buttonModifier ->
            FilledTonalButton(
                onClick = action.onClick,
                shapes = ButtonDefaults.shapes(),
                modifier = buttonModifier,
            ) {
                Text(stringResource(action.label))
            }
        }
    }
}

@Composable
private fun changedAreasSummary(changes: List<PendingChange>): String {
    val names = changes.map { it.areaRes }.distinct().map { stringResource(it) }
    if (names.size <= SUMMARY_NAMES_SHOWN) return names.joinToString(", ")
    val shown = names.take(SUMMARY_NAMES_SHOWN).joinToString(", ")
    return stringResource(R.string.pending_areas_more, shown, names.size - SUMMARY_NAMES_SHOWN)
}
