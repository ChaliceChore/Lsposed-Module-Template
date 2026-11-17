@file:Suppress("ktlint:standard:function-naming")

package io.github.chalicechore.moduletemplate.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.chalicechore.moduletemplate.ui.theme.Tokens

/**
 * #9 Split row: tapping the left side opens [OptionsSheet] with related options; the switch on
 * the right turns the whole group on or off (off = default behaviour, whatever the options say).
 */
@Composable
fun SplitOptionsRow(
    shape: Shape,
    icon: ImageVector,
    title: String,
    summary: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onOpenOptions: () -> Unit,
) {
    Surface(shape = shape, color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.height(IntrinsicSize.Min).heightIn(min = Tokens.RowMinHeight), verticalAlignment = Alignment.CenterVertically) {
            Row(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(onClick = onOpenOptions)
                        .padding(horizontal = Tokens.RowPadding, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Column(Modifier.padding(start = Tokens.RowPadding)) {
                    Text(title, style = MaterialTheme.typography.bodyLarge)
                    Text(summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            VerticalDivider(Modifier.height(32.dp), color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                Modifier
                    .fillMaxHeight()
                    .toggleable(checked, role = Role.Switch, onValueChange = onCheckedChange)
                    .padding(horizontal = Tokens.RowPadding),
                verticalAlignment = Alignment.CenterVertically,
            ) { IconSwitch(checked = checked, onCheckedChange = null) }
        }
    }
}

/**
 * The options sheet behind a [SplitOptionsRow]. Its header holds the title and the group's
 * switch, and [pinned] (usually a [PreviewBlock]) sits under it; both stay in place while
 * [options] scroll. With the group off, the options are dimmed and disabled.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptionsSheet(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    pinned: (@Composable ColumnScope.() -> Unit)? = null,
    options: @Composable ColumnScope.(enabled: Boolean) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        val scroll = rememberScrollState()
        val sheetColor = MaterialTheme.colorScheme.surfaceContainerLow
        // Sticky header: title and the group switch in one line, outside the scrolling part.
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .toggleable(checked, role = Role.Switch, onValueChange = onCheckedChange)
                    .padding(horizontal = Tokens.RowPadding + 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            IconSwitch(checked = checked, onCheckedChange = null)
        }
        if (pinned != null) {
            Column(Modifier.padding(horizontal = Tokens.ScreenMargin, vertical = 8.dp)) { pinned() }
        }
        // The header's lower edge: a soft shadow that fades in once the options scroll under it.
        val edge by animateFloatAsState(if (scroll.canScrollBackward) 1f else 0f, label = "sheetEdge")
        Box(Modifier.weight(1f, fill = false)) {
            Column(
                modifier =
                    Modifier
                        .background(sheetColor)
                        .verticalScroll(scroll)
                        .navigationBarsPadding()
                        .padding(top = 8.dp, bottom = Tokens.ScreenMargin)
                        .alpha(if (checked) 1f else 0.38f),
            ) { options(checked) }
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(8.dp)
                    .graphicsLayer { alpha = edge }
                    .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.14f), Color.Transparent))),
            )
        }
    }
}
