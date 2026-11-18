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
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.key
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
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
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.SwipeToDismissBoxState
import kotlin.math.abs
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.core.animate
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import kotlin.math.sign

/** Height the apply sheet covers at the bottom; lists add it as bottom padding. */
val LocalApplySheetInset = compositionLocalOf { 0.dp }

/** One unapplied change, as listed in the sheet. [onRevert] undoes only this change. */
data class PendingChange(
    val key: String,
    @param:StringRes val titleRes: Int,
    @param:StringRes val areaRes: Int,
    val value: String,
    val highlighted: Boolean,
    val onRevert: () -> Unit,
)

sealed interface ApplySheetContent {
    /** @property restart applying restarts the target (false: only LIVE changes left). */
    data class Pending(
        val canApply: Boolean,
        val restart: Boolean,
        val changes: List<PendingChange>,
    ) : ApplySheetContent

    /** Nothing pending, but the running target started before the module was enabled. */
    data object RestartToLoad : ApplySheetContent

    data class Failure(
        val reason: ApplyFailure,
    ) : ApplySheetContent
}

class FailureAction(
    @param:StringRes val label: Int,
    val onClick: () -> Unit,
)

private val CompactWindowHeight = 480.dp
private const val SWIPE_REMOVE_FRACTION = 0.4f
private const val SWIPE_FLICK_MIN_FRACTION = 0.25f
private const val ROW_COLLAPSE_MILLIS = 350
private const val SWIPE_OUT_MILLIS = 250
private val SWIPE_FLICK_VELOCITY = 400.dp

/** Expressive motion, with a slightly softer spring so the sheet shows and hides a tad slower. */
private object SheetMotion : MotionScheme by MotionScheme.expressive() {
    override fun <T> defaultSpatialSpec(): FiniteAnimationSpec<T> = spring(dampingRatio = 0.85f, stiffness = 250f)
}
private const val SUMMARY_NAMES_SHOWN = 2

@OptIn(ExperimentalMaterial3Api::class)
private fun ApplySheetContent?.sheetTarget(): SheetValue =
    when (this) {
        null -> SheetValue.Hidden
        is ApplySheetContent.Pending, ApplySheetContent.RestartToLoad -> SheetValue.PartiallyExpanded
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
    onDiscard: () -> Unit,
    onRestartTarget: () -> Unit,
    onDismissRestart: () -> Unit,
    onOpenTargetInfo: () -> Unit,
    modifier: Modifier = Modifier,
    body: @Composable () -> Unit,
) {
    var lastContent by remember { mutableStateOf(content) }
    // Bumped each time the sheet comes back after being empty, so rows from the last showing
    // (e.g. one still swiped away) never come back with stale state.
    var generation by remember { mutableIntStateOf(0) }
    var wasEmpty by remember { mutableStateOf(content == null) }
    SideEffect {
        if (content == null) {
            wasEmpty = true
        } else {
            if (wasEmpty) {
                wasEmpty = false
                generation++
            }
            lastContent = content
        }
    }
    val scope = rememberCoroutineScope()
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
    MaterialTheme(motionScheme = SheetMotion) {
        BottomSheetScaffold(
            modifier = modifier,
            scaffoldState = rememberBottomSheetScaffoldState(bottomSheetState = sheetState),
            sheetPeekHeight = peekHeight,
            sheetContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            sheetShadowElevation = 12.dp,
            sheetDragHandle = {
                // Tap toggles peek <-> expanded; no ripple on the handle.
                val label = stringResource(R.string.sheet_toggle)
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .onSizeChanged { handleHeightPx = it.height }
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                enabled = target == SheetValue.PartiallyExpanded,
                                onClickLabel = label,
                            ) {
                                scope.launch {
                                    if (sheetState.currentValue == SheetValue.Expanded) {
                                        sheetState.partialExpand()
                                    } else {
                                        sheetState.expand()
                                    }
                                }
                            }.padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier
                            .size(width = 36.dp, height = 4.dp)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), CircleShape),
                    )
                }
            },
            sheetContent = {
                when (val shownContent = content ?: lastContent) {
                    is ApplySheetContent.Pending -> {
                        PendingSheet(
                            content = shownContent,
                            generation = generation,
                            collapsed = sheetState.targetValue != SheetValue.Expanded,
                            sideBySide = sideBySide,
                            onApply = onApply,
                            onDiscard = onDiscard,
                            onPeekBlockHeight = { peekBlockHeightPx = it },
                        )
                    }

                    ApplySheetContent.RestartToLoad -> {
                        RestartToLoadSheet(
                            sideBySide = sideBySide,
                            onRestart = onRestartTarget,
                            onNotNow = onDismissRestart,
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
            val inset by animateDpAsState(if (sheetVisible) peekHeight else 0.dp, label = "sheetInset")
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
    generation: Int,
    collapsed: Boolean,
    sideBySide: Boolean,
    onApply: () -> Unit,
    onDiscard: () -> Unit,
    onPeekBlockHeight: (Int) -> Unit,
) {
    val count = content.changes.size
    val title = pluralStringResource(R.plurals.pending_changes, count, count)
    val summary = changedAreasSummary(content.changes)
    val context = LocalContext.current
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }
    val target = stringResource(R.string.target_label)
    SheetActions(
        title = title,
        summary = summary,
        sideBySide = sideBySide,
        onPeekBlockHeight = onPeekBlockHeight,
        secondaryLabel = stringResource(R.string.pending_discard),
        onSecondary = { confirmDiscard = true },
        primaryLabel =
            if (content.restart) {
                stringResource(R.string.apply_and_restart, target)
            } else {
                stringResource(R.string.apply_only)
            },
        primaryIcon = if (content.restart) Icons.Outlined.RestartAlt else Icons.Outlined.Check,
        primaryEnabled = content.canApply,
        onPrimary = onApply,
        // Looks disabled while the module isn't active, but a tap says why.
        onPrimaryDisabledTap = {
            Toast.makeText(context, R.string.pending_saved_on_device, Toast.LENGTH_LONG).show()
        },
    )
    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text(pluralStringResource(R.plurals.discard_title, count, count)) },
            text = { Text(stringResource(R.string.discard_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDiscard = false
                    onDiscard()
                }) { Text(stringResource(R.string.pending_discard)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDiscard = false }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            },
        )
    }
    val listScroll = rememberScrollState()
    // Back at peek, start the list from the top again so the next drag moves the sheet, not the list.
    LaunchedEffect(collapsed) { if (collapsed) listScroll.scrollTo(0) }
    val lineAlpha by animateFloatAsState(if (collapsed) 0f else 1f, label = "sheetLine")
    SubtleDivider(Modifier.padding(horizontal = Tokens.RowPadding).graphicsLayer { alpha = lineAlpha })
    Column(
        modifier =
            Modifier
                .weight(1f, fill = false)
                .verticalScroll(listScroll)
                .navigationBarsPadding()
                .padding(vertical = 8.dp)
                .animateContentSize(tween(ROW_COLLAPSE_MILLIS, easing = FastOutSlowInEasing)),
    ) {
        content.changes.forEach { change -> key(generation, change.key) { PendingChangeRow(change) } }
    }
}

/**
 * One change. The ✕ button, or a sideways swipe, reverts just this change without asking.
 * The swipe is decided on release only: past [SWIPE_REMOVE_FRACTION] of the row (or a flick
 * that already moved [SWIPE_FLICK_MIN_FRACTION]) removes it, anything less springs back.
 * Crossing the removal point shows while dragging: haptic tick, filled bin, red background.
 */
@Composable
private fun PendingChangeRow(change: PendingChange) {
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val revert by rememberUpdatedState(change.onRevert)
    var widthPx by remember { mutableIntStateOf(0) }
    var dragX by remember { mutableFloatStateOf(0f) }
    var leaving by remember { mutableStateOf(false) }
    val armed = widthPx > 0 && abs(dragX) >= widthPx * SWIPE_REMOVE_FRACTION
    LaunchedEffect(armed) {
        if (armed && !leaving) haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
    }
    val colors = MaterialTheme.colorScheme
    val background by animateColorAsState(
        if (armed) colors.errorContainer else colors.surfaceContainerHighest,
        label = "swipeBackground",
    )
    val iconTint by animateColorAsState(
        if (armed) colors.onErrorContainer else colors.onSurfaceVariant,
        label = "swipeIcon",
    )
    val iconScale by animateFloatAsState(if (armed) 1.2f else 1f, label = "swipeIconScale")
    val flickVelocity = with(density) { SWIPE_FLICK_VELOCITY.toPx() }
    Box(
        Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .onSizeChanged { widthPx = it.width },
    ) {
        if (dragX != 0f) {
            Box(
                modifier =
                    Modifier
                        .matchParentSize()
                        .background(background)
                        .padding(horizontal = Tokens.RowPadding),
                contentAlignment = if (dragX > 0) Alignment.CenterStart else Alignment.CenterEnd,
            ) {
                Icon(
                    if (armed) Icons.Filled.Delete else Icons.Outlined.Delete,
                    contentDescription = null,
                    tint = iconTint,
                    modifier =
                        Modifier.graphicsLayer {
                            scaleX = iconScale
                            scaleY = iconScale
                        },
                )
            }
        }
        val dragState = rememberDraggableState { delta -> if (!leaving) dragX += delta }
        Box(
            Modifier
                .offset { IntOffset(dragX.roundToInt(), 0) }
                .draggable(
                    state = dragState,
                    orientation = Orientation.Horizontal,
                    enabled = !leaving,
                    onDragStopped = { velocity ->
                        val width = widthPx.toFloat()
                        val flick =
                            abs(velocity) > flickVelocity &&
                                sign(velocity) == sign(dragX) &&
                                abs(dragX) >= width * SWIPE_FLICK_MIN_FRACTION
                        if (width > 0f && (abs(dragX) >= width * SWIPE_REMOVE_FRACTION || flick)) {
                            leaving = true
                            animate(dragX, sign(dragX) * width, animationSpec = tween(SWIPE_OUT_MILLIS)) { v, _ -> dragX = v }
                            revert()
                        } else {
                            animate(dragX, 0f, animationSpec = spring(dampingRatio = 0.8f)) { v, _ -> dragX = v }
                        }
                    },
                ),
        ) {
        ListItem(
            supportingContent = { Text(stringResource(change.areaRes)) },
            trailingContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
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
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = change.onRevert) {
                        Icon(
                            Icons.Outlined.Close,
                            contentDescription =
                                stringResource(R.string.pending_revert, stringResource(change.titleRes)),
                        )
                    }
                }
            },
            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            Text(stringResource(change.titleRes))
        }
        }
    }
}

@Composable
private fun ColumnScope.RestartToLoadSheet(
    sideBySide: Boolean,
    onRestart: () -> Unit,
    onNotNow: () -> Unit,
    onPeekBlockHeight: (Int) -> Unit,
) {
    val target = stringResource(R.string.target_label)
    SheetActions(
        title = stringResource(R.string.restart_to_load_title, target),
        summary = stringResource(R.string.restart_to_load_body, target),
        sideBySide = sideBySide,
        onPeekBlockHeight = onPeekBlockHeight,
        secondaryLabel = stringResource(R.string.restart_not_now),
        onSecondary = onNotNow,
        primaryLabel = stringResource(R.string.restart_target, target),
        primaryIcon = Icons.Outlined.RestartAlt,
        primaryEnabled = true,
        onPrimary = onRestart,
    )
}

/**
 * The sheet's always-visible block: a message, then a smaller secondary button (Discard,
 * Not now) on the left and the wider primary action on the right.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SheetActions(
    title: String,
    summary: String,
    sideBySide: Boolean,
    onPeekBlockHeight: (Int) -> Unit,
    secondaryLabel: String,
    onSecondary: () -> Unit,
    primaryLabel: String,
    primaryIcon: ImageVector,
    primaryEnabled: Boolean,
    onPrimary: () -> Unit,
    onPrimaryDisabledTap: (() -> Unit)? = null,
) {
    val height = 48.dp
    val buttons: @Composable (Modifier) -> Unit = { rowModifier ->
        Row(
            modifier = rowModifier,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(
                onClick = onSecondary,
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier.heightIn(min = height),
                contentPadding = ButtonDefaults.contentPaddingFor(height),
            ) { Text(secondaryLabel, maxLines = 1) }
            val tapWhenDisabled = !primaryEnabled && onPrimaryDisabledTap != null
            Button(
                onClick = { if (primaryEnabled) onPrimary() else onPrimaryDisabledTap?.invoke() },
                enabled = primaryEnabled || tapWhenDisabled,
                colors =
                    if (tapWhenDisabled) {
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                            contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                        )
                    } else {
                        ButtonDefaults.buttonColors()
                    },
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier.weight(1f, fill = !sideBySide).heightIn(min = height),
                contentPadding = ButtonDefaults.contentPaddingFor(height),
            ) {
                Icon(
                    imageVector = primaryIcon,
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.iconSizeFor(height)),
                )
                Spacer(Modifier.width(ButtonDefaults.iconSpacingFor(height)))
                Text(primaryLabel, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
    if (sideBySide) {
        Row(
            modifier = Modifier.peekBlock(onPeekBlockHeight),
            horizontalArrangement = Arrangement.spacedBy(Tokens.RowPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SheetMessage(title, summary, Modifier.weight(1f))
            buttons(Modifier)
        }
    } else {
        Column(
            modifier = Modifier.peekBlock(onPeekBlockHeight),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SheetMessage(title, summary, Modifier.fillMaxWidth())
            buttons(Modifier.fillMaxWidth())
        }
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
