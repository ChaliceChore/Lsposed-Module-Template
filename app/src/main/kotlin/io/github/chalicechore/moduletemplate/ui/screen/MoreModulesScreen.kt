@file:Suppress("ktlint:standard:function-naming")

package io.github.chalicechore.moduletemplate.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.chalicechore.moduletemplate.R
import io.github.chalicechore.moduletemplate.net.GitHub
import io.github.chalicechore.moduletemplate.net.ModuleRepo
import io.github.chalicechore.moduletemplate.net.MoreModules
import io.github.chalicechore.moduletemplate.net.RepoOwner
import io.github.chalicechore.moduletemplate.ui.component.DetailScaffold
import io.github.chalicechore.moduletemplate.ui.component.centeredContentWidth
import io.github.chalicechore.moduletemplate.ui.theme.Tokens

private val IconSize = 56.dp

/** The author's other LSPosed modules, one card each. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MoreModulesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repos by produceState<List<ModuleRepo>?>(null) { value = MoreModules(context).load() }
    DetailScaffold(
        title = stringResource(R.string.about_more_modules, RepoOwner),
        onBack = onBack,
    ) { contentPadding ->
        val list = repos
        when {
            list == null -> {
                Box(Modifier.fillMaxSize().padding(contentPadding), contentAlignment = Alignment.Center) {
                    LoadingIndicator()
                }
            }

            list.isEmpty() -> {
                Text(
                    text = stringResource(R.string.more_modules_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(contentPadding).padding(32.dp),
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.centeredContentWidth(),
                    contentPadding =
                        PaddingValues(
                            top = contentPadding.calculateTopPadding() + Tokens.ScreenMargin,
                            bottom = contentPadding.calculateBottomPadding() + 16.dp,
                            start = Tokens.ScreenMargin,
                            end = Tokens.ScreenMargin,
                        ),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(list, key = { it.fullName }) { repo -> ModuleCard(repo) }
                }
            }
        }
    }
}

@Composable
private fun ModuleCard(repo: ModuleRepo) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val icon by produceState<ImageBitmap?>(null, repo.fullName) {
        value = GitHub.icon(context, repo)?.asImageBitmap()
    }
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = { runCatching { uriHandler.openUri(repo.url) } },
        shape = RoundedCornerShape(Tokens.OuterRowCorner),
        color = colors.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(Tokens.RowPadding), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            val shape = RoundedCornerShape(16.dp)
            val bitmap = icon
            if (bitmap != null) {
                Image(bitmap, contentDescription = null, modifier = Modifier.size(IconSize).clip(shape))
            } else {
                Box(
                    Modifier.size(IconSize).background(colors.secondaryContainer, shape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.Extension, contentDescription = null, tint = colors.onSecondaryContainer)
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(repo.name, style = MaterialTheme.typography.titleMedium)
                if (repo.description.isNotEmpty()) {
                    Text(repo.description, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.StarOutline, contentDescription = null, modifier = Modifier.size(16.dp), tint = colors.primary)
                    Spacer(Modifier.width(4.dp))
                    Text(repo.stars.toString(), style = MaterialTheme.typography.labelLarge, color = colors.primary)
                    Spacer(Modifier.width(16.dp))
                    Text(
                        "github.com/${repo.fullName}",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                    )
                    Icon(
                        Icons.AutoMirrored.Outlined.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = colors.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
