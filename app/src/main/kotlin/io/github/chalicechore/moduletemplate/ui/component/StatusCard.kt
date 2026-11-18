@file:Suppress("ktlint:standard:function-naming")

package io.github.chalicechore.moduletemplate.ui.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PriorityHigh
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.toPath
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import io.github.chalicechore.moduletemplate.R
import io.github.chalicechore.moduletemplate.BuildConfig
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.foundation.combinedClickable
import io.github.chalicechore.moduletemplate.ui.ApplyPhase
import io.github.chalicechore.moduletemplate.ui.UiState
import io.github.chalicechore.moduletemplate.ui.theme.Tokens
import io.github.chalicechore.moduletemplate.ui.theme.isReducedMotion

/** What the status card shows. Each kind has its own shape, colour and icon. */
enum class StatusKind {
    Active,
    Pending,
    Warning,
    Off,
    NotActive,
    Busy,
}

private data class StatusContent(
    val kind: StatusKind,
    val title: String,
    val body: String,
)

private val BadgeSize = 48.dp
private val BadgeIconSize = 24.dp
private const val MORPH_MILLIS = 450
private const val SPIN_MILLIS = 6_000

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun StatusKind.polygon(): RoundedPolygon =
    when (this) {
        StatusKind.Active -> MaterialShapes.Cookie9Sided
        StatusKind.Pending -> MaterialShapes.Clover4Leaf
        StatusKind.Warning -> MaterialShapes.Sunny
        StatusKind.Off -> MaterialShapes.Square
        StatusKind.NotActive -> MaterialShapes.SoftBurst
        StatusKind.Busy -> MaterialShapes.Cookie12Sided
    }.normalized()

private fun StatusKind.icon(): ImageVector? =
    when (this) {
        StatusKind.Active -> Icons.Rounded.Check
        StatusKind.Pending -> Icons.Rounded.Sync
        StatusKind.Warning -> Icons.Rounded.PriorityHigh
        StatusKind.Off -> Icons.Rounded.Pause
        StatusKind.NotActive -> BoldCross
        StatusKind.Busy -> null
    }

@Composable
private fun StatusKind.colors(): Pair<Color, Color> {
    val c = MaterialTheme.colorScheme
    return when (this) {
        StatusKind.Active, StatusKind.Busy -> c.primaryContainer to c.onPrimaryContainer
        StatusKind.Pending -> c.secondaryContainer to c.onSecondaryContainer
        StatusKind.Warning -> c.tertiaryContainer to c.onTertiaryContainer
        StatusKind.Off -> c.secondaryContainer to c.onSecondaryContainer
        // errorContainer is pale on a pale background in light mode, so use the solid pair there.
        StatusKind.NotActive ->
            if (c.surface.luminance() > 0.5f) c.error to c.onError else c.errorContainer to c.onErrorContainer
    }
}

/** A heavier ✕ than Icons.Rounded.Close, so it reads at badge size. */
private val BoldCross: ImageVector by lazy {
    ImageVector
        .Builder("BoldCross", 24.dp, 24.dp, 24f, 24f)
        .addPath(
            pathData = PathParser().parsePathString("M6.5,6.5L17.5,17.5M17.5,6.5L6.5,17.5").toNodes(),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 3.2f,
            strokeLineCap = StrokeCap.Round,
        ).build()
}

/** Debug builds: long-press the status side to step through these instead of the real state. */
@Composable
private fun previewContents(): List<StatusContent> {
    val target = stringResource(R.string.target_label)
    return listOf(
        StatusContent(
            StatusKind.Active,
            stringResource(R.string.status_active_title),
            stringResource(R.string.status_framework, "LSPosed", "1.10.2", 101),
        ),
        StatusContent(
            StatusKind.Pending,
            stringResource(R.string.status_pending_title),
            stringResource(R.string.status_pending_body, target),
        ),
        StatusContent(
            StatusKind.Warning,
            stringResource(R.string.status_not_loaded_title),
            stringResource(R.string.status_not_loaded_body, target),
        ),
        StatusContent(
            StatusKind.Warning,
            stringResource(R.string.status_partly_title),
            pluralStringResource(R.plurals.status_partly_body, 2, 2),
        ),
        StatusContent(
            StatusKind.Off,
            stringResource(R.string.status_off_title),
            stringResource(R.string.status_off_body, target),
        ),
        StatusContent(
            StatusKind.NotActive,
            stringResource(R.string.status_inactive_title),
            stringResource(R.string.status_inactive_body),
        ),
        StatusContent(
            StatusKind.Busy,
            stringResource(R.string.status_applying_title),
            stringResource(R.string.status_step_restarting, target),
        ),
    )
}

@Composable
private fun statusContent(state: UiState.Ready): StatusContent {
    val target = stringResource(R.string.target_label)
    val framework = state.framework
    val failed = state.failedFeatures.size
    return when {
        state.phase == ApplyPhase.Saving -> {
            StatusContent(
                StatusKind.Busy,
                stringResource(R.string.status_applying_title),
                stringResource(R.string.status_step_saving),
            )
        }

        state.phase == ApplyPhase.Restarting -> {
            StatusContent(
                StatusKind.Busy,
                stringResource(R.string.status_applying_title),
                stringResource(R.string.status_step_restarting, target),
            )
        }

        framework == null -> {
            StatusContent(
                StatusKind.NotActive,
                stringResource(R.string.status_inactive_title),
                stringResource(R.string.status_inactive_body),
            )
        }

        state.restartNeeded -> {
            StatusContent(
                StatusKind.Pending,
                stringResource(R.string.status_pending_title),
                stringResource(R.string.status_pending_body, target),
            )
        }

        !state.appliedOrDefault.moduleEnabled -> {
            StatusContent(
                StatusKind.Off,
                stringResource(R.string.status_off_title),
                stringResource(R.string.status_off_body, target),
            )
        }

        state.targetNeedsRestart -> {
            StatusContent(
                StatusKind.Warning,
                stringResource(R.string.status_not_loaded_title),
                stringResource(R.string.status_not_loaded_body, target),
            )
        }

        failed > 0 -> {
            StatusContent(
                StatusKind.Warning,
                stringResource(R.string.status_partly_title),
                pluralStringResource(R.plurals.status_partly_body, failed, failed),
            )
        }

        else -> {
            StatusContent(
                StatusKind.Active,
                stringResource(R.string.status_active_title),
                stringResource(
                    R.string.status_framework,
                    framework.name,
                    framework.versionName,
                    framework.apiVersion,
                ),
            )
        }
    }
}

/**
 * Home screen header, split like an Android Settings preference with its own switch: status on
 * the left, a divider, and the module's master toggle on the right.
 *
 * @param onOpenHealth opens Feature health; the status side is tappable while features failed.
 */
@Composable
fun StatusCard(
    state: UiState.Ready,
    onModuleEnabledChange: (Boolean) -> Unit,
    onOpenHealth: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Debug builds only: -1 shows the real state, 0+ an entry of previewContents().
    var preview by remember { mutableIntStateOf(-1) }
    val previews = if (BuildConfig.DEBUG) previewContents() else emptyList()
    val content = previews.getOrNull(preview) ?: statusContent(state)
    val colors = MaterialTheme.colorScheme
    val effects = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
    val haptics = LocalHapticFeedback.current
    val checked = state.desired.moduleEnabled
    val busy = state.phase.isRunning
    val healthTappable = state.failedFeatures.isNotEmpty() && content.kind == StatusKind.Warning
    Surface(
        modifier = modifier.fillMaxWidth().padding(horizontal = Tokens.ScreenMargin),
        shape = RoundedCornerShape(Tokens.OuterRowCorner),
        color = colors.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier.height(IntrinsicSize.Min).heightIn(min = 88.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .then(
                            if (BuildConfig.DEBUG) {
                                Modifier.combinedClickable(
                                    onClick = { if (healthTappable) onOpenHealth() },
                                    onLongClick = {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        preview = if (preview + 1 >= previews.size) -1 else preview + 1
                                    },
                                )
                            } else if (healthTappable) {
                                Modifier.clickable(onClick = onOpenHealth)
                            } else {
                                Modifier
                            },
                        )
                        .padding(Tokens.RowPadding),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Tokens.RowPadding),
            ) {
                StatusBadge(content.kind)
                AnimatedContent(
                    targetState = content,
                    transitionSpec = {
                        fadeIn(effects) togetherWith fadeOut(effects) using SizeTransform(clip = false)
                    },
                    modifier = Modifier.weight(1f),
                    label = "statusText",
                ) { shown ->
                    Column(
                        modifier =
                            Modifier.semantics(mergeDescendants = true) {
                                liveRegion = LiveRegionMode.Polite
                            },
                    ) {
                        Text(
                            text = if (preview >= 0) "${shown.title} (preview)" else shown.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.onSurface,
                        )
                        Text(
                            text = shown.body,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant,
                        )
                    }
                }
            }
            VerticalDivider(
                modifier = Modifier.height(40.dp),
                color = colors.outlineVariant,
            )
            val label = stringResource(R.string.master_switch_title)
            Box(
                modifier =
                    Modifier
                        .fillMaxHeight()
                        .toggleable(value = checked, enabled = !busy, role = Role.Switch) { on ->
                            haptics.performHapticFeedback(
                                if (on) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff,
                            )
                            onModuleEnabledChange(on)
                        }.semantics { this.contentDescription = label }
                        .padding(horizontal = Tokens.RowPadding),
                contentAlignment = Alignment.Center,
            ) {
                IconSwitch(checked = checked, onCheckedChange = null, enabled = !busy)
            }
        }
    }
}

/**
 * The status icon inside a shape that stands for the state. The shape morphs only when the
 * state changes, spins slowly while applying, and swaps instantly with reduced motion.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun StatusBadge(kind: StatusKind) {
    val reduced = isReducedMotion()
    var from by remember { mutableStateOf(kind) }
    var to by remember { mutableStateOf(kind) }
    val progress = remember { Animatable(1f) }
    LaunchedEffect(kind) {
        if (kind == to) return@LaunchedEffect
        from = to
        to = kind
        if (reduced) {
            progress.snapTo(1f)
        } else {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(MORPH_MILLIS))
        }
    }
    val morph = remember(from, to) { Morph(from.polygon(), to.polygon()) }
    val (container, content) = kind.colors()
    val fill by animateColorAsState(container, MaterialTheme.motionScheme.defaultEffectsSpec(), label = "badge")
    val rotation =
        if (kind == StatusKind.Busy && !reduced) {
            rememberInfiniteTransition(label = "badgeSpin")
                .animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(tween(SPIN_MILLIS, easing = LinearEasing)),
                    label = "badgeRotation",
                ).value
        } else {
            remember { mutableFloatStateOf(0f) }.floatValue
        }
    val path = remember { Path() }
    val matrix = remember { Matrix() }
    Box(
        modifier =
            Modifier
                .size(BadgeSize)
                .drawBehind {
                    val shape = morph.toPath(progress.value, path)
                    fit(shape, size, matrix, rotation)
                    drawPath(shape, fill)
                },
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = kind.icon(),
            transitionSpec = {
                (fadeIn() + scaleIn(initialScale = 0.6f)) togetherWith fadeOut()
            },
            label = "badgeIcon",
        ) { icon ->
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(BadgeIconSize))
            }
        }
    }
}

/** Scales the unit-sized morph path to [size], centres it, and rotates it by [degrees]. */
private fun fit(
    path: Path,
    size: Size,
    matrix: Matrix,
    degrees: Float,
) {
    matrix.reset()
    matrix.scale(size.width, size.height)
    path.transform(matrix)
    path.translate(size.center - path.getBounds().center)
    if (degrees != 0f) {
        matrix.reset()
        matrix.translate(size.width / 2, size.height / 2)
        matrix.rotateZ(degrees)
        matrix.translate(-size.width / 2, -size.height / 2)
        path.transform(matrix)
    }
}
