@file:Suppress("ktlint:standard:function-naming")

package io.github.chalicechore.moduletemplate.ui.tour

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateValueAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import io.github.chalicechore.moduletemplate.R
import io.github.chalicechore.moduletemplate.ui.theme.isReducedMotion

/** Parts of Home a tour step can point at. Tag them with [tourTarget]. */
enum class TourTarget {
    STATUS,
    FEATURES,
    SEARCH,
    TRY_IT,
    APP,
}

/** Where Home should scroll before a step is shown. */
enum class TourScroll {
    NONE,
    TOP,
    BOTTOM,
}

/**
 * One step of the first-launch tour.
 *
 * @property target what the spotlight frames, or null for a centred card with no spotlight.
 * @property bodyInactiveRes text to use instead of [bodyRes] while the module isn't active.
 * Strings may use %1$s for the target app's label.
 */
data class TourStep(
    val target: TourTarget?,
    @param:StringRes val titleRes: Int,
    @param:StringRes val bodyRes: Int,
    @param:StringRes val bodyInactiveRes: Int? = null,
    val scroll: TourScroll = TourScroll.NONE,
)

/** Which step is showing (-1: none) and where each tagged target is on screen. */
@Stable
class TourState {
    var index by mutableIntStateOf(-1)
        private set

    val active: Boolean get() = index >= 0

    // Target → (part → bounds in root). A target can span several rows; its spotlight is the union.
    private val parts = mutableStateMapOf<TourTarget, Map<String, Rect>>()

    fun start() {
        index = 0
    }

    fun next(total: Int) {
        index = if (index + 1 >= total) -1 else index + 1
    }

    fun back() {
        if (index > 0) index--
    }

    fun finish() {
        index = -1
    }

    fun boundsOf(target: TourTarget): Rect? =
        parts[target]?.values?.reduceOrNull { a, b ->
            Rect(minOf(a.left, b.left), minOf(a.top, b.top), maxOf(a.right, b.right), maxOf(a.bottom, b.bottom))
        }

    internal fun report(
        target: TourTarget,
        part: String,
        bounds: Rect?,
    ) {
        val current = parts[target].orEmpty()
        parts[target] = if (bounds == null) current - part else current + (part to bounds)
    }
}

val LocalTourState = staticCompositionLocalOf<TourState?> { null }

/** Marks this element (or one [part] of a larger area) as a tour target. */
fun Modifier.tourTarget(
    target: TourTarget,
    part: String = "",
): Modifier =
    composed {
        val tour = LocalTourState.current ?: return@composed this
        DisposableEffect(tour, target, part) { onDispose { tour.report(target, part, null) } }
        onGloballyPositioned { tour.report(target, part, it.boundsInRoot()) }
    }

private val SpotlightPadding = 8.dp
private val SpotlightCorner = 24.dp
private val CardGap = 16.dp
private const val SCRIM_ALPHA = 0.62f

/**
 * Dims the screen, cuts a rounded spotlight around the current step's target, and shows a
 * tip card with Back / Next / Skip. Taps outside the card are swallowed; the back key steps
 * back (or skips on the first step).
 */
@Composable
fun TourOverlay(
    state: TourState,
    steps: List<TourStep>,
    active: Boolean,
    onFinish: () -> Unit,
) {
    val step = steps.getOrNull(state.index) ?: return
    val density = LocalDensity.current
    val pad = with(density) { SpotlightPadding.toPx() }
    val target = step.target?.let(state::boundsOf)?.let { Rect(it.left - pad, it.top - pad, it.right + pad, it.bottom + pad) }
    val reduced = isReducedMotion()
    val spotlight by animateValueAsState(
        targetValue = target ?: Rect.Zero,
        typeConverter = Rect.VectorConverter,
        animationSpec = if (reduced) snap() else spring(dampingRatio = 0.85f, stiffness = 300f),
        label = "spotlight",
    )
    val finish = {
        state.finish()
        onFinish()
    }
    BackHandler {
        if (state.index > 0) state.back() else finish()
    }
    Box(
        Modifier
            .fillMaxSize()
            // Swallow every touch outside the card, so the app underneath can't be used mid-tour.
            .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent().changes.forEach { it.consume() } } },
    ) {
        val corner = with(density) { SpotlightCorner.toPx() }
        Canvas(Modifier.fillMaxSize().graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }) {
            drawRect(Color.Black.copy(alpha = SCRIM_ALPHA))
            if (target != null && spotlight != Rect.Zero) {
                drawRoundRect(
                    color = Color.Black,
                    topLeft = Offset(spotlight.left, spotlight.top),
                    size = spotlight.size,
                    cornerRadius = CornerRadius(corner),
                    blendMode = BlendMode.Clear,
                )
            }
        }
        val gap = with(density) { CardGap.roundToPx() }
        // Card goes below the spotlight when the target is in the top half, above it otherwise,
        // and in the middle when there is no target.
        Layout(
            content = {
                TipCard(
                    step = step,
                    number = state.index + 1,
                    total = steps.size,
                    active = active,
                    onBack = state::back,
                    onNext = { if (state.index + 1 >= steps.size) finish() else state.next(steps.size) },
                    onSkip = finish,
                )
            },
            modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 16.dp),
        ) { measurables, constraints ->
            val card = measurables.first().measure(Constraints(maxWidth = constraints.maxWidth, maxHeight = constraints.maxHeight))
            layout(constraints.maxWidth, constraints.maxHeight) {
                val x = (constraints.maxWidth - card.width) / 2
                // The spotlight is in root coordinates; this layout is inset by the safe area.
                val origin = coordinates?.let { it.localToRoot(Offset.Zero) } ?: Offset.Zero
                val y =
                    if (target == null) {
                        (constraints.maxHeight - card.height) / 2
                    } else if (target.center.y - origin.y < constraints.maxHeight / 2f) {
                        (target.bottom - origin.y).toInt() + gap
                    } else {
                        (target.top - origin.y).toInt() - gap - card.height
                    }
                card.place(x, y.coerceIn(0, (constraints.maxHeight - card.height).coerceAtLeast(0)))
            }
        }
    }
}

@Composable
private fun TipCard(
    step: TourStep,
    number: Int,
    total: Int,
    active: Boolean,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit,
) {
    val target = stringResource(R.string.target_label)
    val app = stringResource(R.string.app_name)
    val title = stringResource(step.titleRes, app)
    val body = stringResource(if (!active && step.bodyInactiveRes != null) step.bodyInactiveRes else step.bodyRes, target)
    val last = number == total
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 6.dp,
        modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth(),
    ) {
        Column(
            modifier =
                Modifier
                    .padding(start = 24.dp, end = 16.dp, top = 20.dp, bottom = 12.dp)
                    .semantics(mergeDescendants = false) {
                        paneTitle = title
                        liveRegion = LiveRegionMode.Polite
                    },
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                stringResource(R.string.tour_step, number, total),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(end = 8.dp))
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 8.dp),
            )
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (!last) TextButton(onClick = onSkip) { Text(stringResource(R.string.tour_skip)) }
                Spacer(Modifier.weight(1f))
                if (number > 1) TextButton(onClick = onBack) { Text(stringResource(R.string.tour_back)) }
                Button(onClick = onNext) { Text(stringResource(if (last) R.string.tour_done else R.string.tour_next)) }
            }
        }
    }
}
