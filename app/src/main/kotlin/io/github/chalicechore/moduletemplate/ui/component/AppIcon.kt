@file:Suppress("ktlint:standard:function-naming")

package io.github.chalicechore.moduletemplate.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.core.graphics.drawable.toBitmap

/**
 * The app's real launcher icon, drawn from its adaptive icon layers with the system's icon mask.
 * Replacing the launcher icon in res/ updates it everywhere, with no extra setup.
 */
@Composable
fun AppIcon(
    size: Dp,
    modifier: Modifier = Modifier,
    packageName: String = LocalContext.current.packageName,
) {
    val context = LocalContext.current
    val px = with(LocalDensity.current) { size.roundToPx() }.coerceAtLeast(1)
    val bitmap: ImageBitmap? =
        remember(packageName, px) {
            runCatching {
                context.packageManager
                    .getApplicationIcon(packageName)
                    .toBitmap(px, px)
                    .asImageBitmap()
            }.getOrNull()
        }
    if (bitmap != null) {
        Image(bitmap = bitmap, contentDescription = null, modifier = modifier.size(size))
    }
}
