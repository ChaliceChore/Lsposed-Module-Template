@file:Suppress("ktlint:standard:function-naming")

package io.github.chalicechore.moduletemplate.ui.screen

import android.content.ActivityNotFoundException
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Balance
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Shape
import io.github.chalicechore.moduletemplate.net.GitHub
import io.github.chalicechore.moduletemplate.net.LatestRelease
import io.github.chalicechore.moduletemplate.net.RepoOwner
import io.github.chalicechore.moduletemplate.ui.component.SectionHeader
import kotlinx.coroutines.launch
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mikepenz.aboutlibraries.ui.compose.android.produceLibraries
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import io.github.chalicechore.moduletemplate.BuildConfig
import io.github.chalicechore.moduletemplate.R
import io.github.chalicechore.moduletemplate.ui.component.AdaptiveButtonRow
import io.github.chalicechore.moduletemplate.ui.component.DetailScaffold
import io.github.chalicechore.moduletemplate.ui.component.MorphingLogo
import io.github.chalicechore.moduletemplate.ui.component.NavigationRow
import io.github.chalicechore.moduletemplate.ui.component.RowGroup
import io.github.chalicechore.moduletemplate.ui.component.RowTrailing
import io.github.chalicechore.moduletemplate.ui.component.centeredContentWidth
import io.github.chalicechore.moduletemplate.ui.component.rememberLogoPulse
import io.github.chalicechore.moduletemplate.ui.theme.Tokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Links come from module.repo / module.license in gradle.properties.
private const val REPO_URL = "https://github.com/${BuildConfig.REPO}"
private const val RELEASES_URL = "$REPO_URL/releases"
private const val CHANGELOG_URL = "$REPO_URL/blob/main/CHANGELOG.md"
private const val FEATURE_REQUEST_URL = "$REPO_URL/issues/new?template=feature_request.yml"
private const val KOFI_URL = "https://ko-fi.com/ChaliceChore"
private const val LICENSE_URL = "$REPO_URL/blob/main/LICENSE"
private const val ADAPTIVE_ICON_ZOOM = 1.425f
private const val ICON_PIXELS = 432

private val LogoSize = 96.dp

@Immutable
private data class AboutInfo(
    val versionName: String,
    val versionCode: Long,
    val icon: ImageBitmap?,
)

@Composable
private fun rememberAboutInfo(): AboutInfo? {
    val context = LocalContext.current
    val info by produceState<AboutInfo?>(null, context) {
        value = withContext(Dispatchers.IO) { loadAboutInfo(context) }
    }
    return info
}

private fun loadAboutInfo(context: Context): AboutInfo {
    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    return AboutInfo(
        versionName = packageInfo.versionName.orEmpty(),
        versionCode = packageInfo.longVersionCode,
        icon = runCatching { launcherArt(context) }.getOrNull(),
    )
}

/** Flattens the adaptive launcher icon so the logo can clip it into morphing shapes. */
private fun launcherArt(context: Context): ImageBitmap? {
    val icon =
        context.packageManager.getApplicationIcon(context.applicationInfo)
            as? AdaptiveIconDrawable ?: return null
    val bitmap = Bitmap.createBitmap(ICON_PIXELS, ICON_PIXELS, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    listOfNotNull<Drawable>(icon.background, icon.foreground).forEach { layer ->
        layer.setBounds(0, 0, ICON_PIXELS, ICON_PIXELS)
        layer.draw(canvas)
    }
    return bitmap.asImageBitmap()
}

private fun UriHandler.openOrIgnore(url: String) {
    try {
        openUri(url)
    } catch (_: ActivityNotFoundException) {
    } catch (_: IllegalArgumentException) {
    }
}

@Composable
fun AboutScreen(
    onOpenLicenses: () -> Unit,
    onOpenMoreModules: () -> Unit,
    onBack: () -> Unit,
    troubleshooting: @Composable () -> Unit = {},
    backup: @Composable () -> Unit = {},
) {
    val about = rememberAboutInfo()
    val uriHandler = LocalUriHandler.current
    val open: (String) -> Unit = { uriHandler.openOrIgnore(it) }
    DetailScaffold(
        title = stringResource(R.string.about_title),
        onBack = onBack,
    ) { contentPadding ->
        Column(
            modifier =
                Modifier
                    .centeredContentWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(contentPadding),
        ) {
            if (about == null) return@Column
            IdentityCard(
                about = about,
                onOpenUrl = open,
                modifier = Modifier.padding(start = Tokens.ScreenMargin, end = Tokens.ScreenMargin, top = Tokens.ScreenMargin),
            )
            LinkRows(
                about = about,
                onOpenUrl = open,
                onOpenLicenses = onOpenLicenses,
                onOpenMoreModules = onOpenMoreModules,
                troubleshooting = troubleshooting,
                backup = backup,
            )
        }
    }
}

@Composable
private fun LinkRows(
    about: AboutInfo,
    onOpenUrl: (String) -> Unit,
    onOpenLicenses: () -> Unit,
    onOpenMoreModules: () -> Unit,
    troubleshooting: @Composable () -> Unit,
    backup: @Composable () -> Unit,
) {
    // TEMPLATE: groups are Updates, Help, Data, Project, Author, Legal. Help and Data are
    // optional and drop out with their ModuleConfig flags.
    SectionHeader(stringResource(R.string.section_updates))
    RowGroup(
        rows =
            listOf(
                { shape -> VersionRow(shape, about.versionName, onOpenUrl) },
                { shape ->
                    NavigationRow(
                        shape = shape,
                        icon = Icons.Outlined.NewReleases,
                        title = stringResource(R.string.about_whats_new),
                        summary = stringResource(R.string.about_whats_new_summary, about.versionName),
                        onClick = { onOpenUrl("$RELEASES_URL/tag/v${about.versionName}") },
                        trailing = RowTrailing.External,
                    )
                },
                { shape ->
                    NavigationRow(
                        shape = shape,
                        icon = Icons.Outlined.History,
                        title = stringResource(R.string.about_changelog),
                        summary = stringResource(R.string.about_changelog_summary),
                        onClick = { onOpenUrl(CHANGELOG_URL) },
                        trailing = RowTrailing.External,
                    )
                },
            ),
    )
    troubleshooting()
    backup()
    SectionHeader(stringResource(R.string.section_project))
    RowGroup(
        rows =
            buildList<@Composable (Shape) -> Unit> {
                add { shape ->
                    NavigationRow(
                        shape = shape,
                        icon = Icons.Outlined.Code,
                        title = stringResource(R.string.about_source_code),
                        summary = BuildConfig.REPO,
                        onClick = { onOpenUrl(REPO_URL) },
                        trailing = RowTrailing.External,
                    )
                }
                add { shape ->
                    NavigationRow(
                        shape = shape,
                        icon = Icons.Outlined.Lightbulb,
                        title = stringResource(R.string.about_request_feature),
                        summary = stringResource(R.string.about_request_feature_summary),
                        onClick = { onOpenUrl(FEATURE_REQUEST_URL) },
                        trailing = RowTrailing.External,
                    )
                }
                if (BuildConfig.TRANSLATE_URL.isNotEmpty()) {
                    add { shape ->
                        NavigationRow(
                            shape = shape,
                            icon = Icons.Outlined.Translate,
                            title = stringResource(R.string.about_translate),
                            summary = stringResource(R.string.about_translate_summary),
                            onClick = { onOpenUrl(BuildConfig.TRANSLATE_URL) },
                            trailing = RowTrailing.External,
                        )
                    }
                }
            },
    )
    SectionHeader(stringResource(R.string.section_author))
    RowGroup(
        rows =
            listOf(
                { shape ->
                    NavigationRow(
                        shape = shape,
                        icon = ImageVector.vectorResource(R.drawable.ic_github),
                        title = stringResource(R.string.about_author),
                        summary = "github.com/$RepoOwner",
                        onClick = { onOpenUrl("https://github.com/$RepoOwner") },
                        trailing = RowTrailing.External,
                    )
                },
                { shape ->
                    NavigationRow(
                        shape = shape,
                        icon = Icons.Outlined.Extension,
                        title = stringResource(R.string.about_more_modules, RepoOwner),
                        summary = stringResource(R.string.about_more_modules_summary),
                        onClick = onOpenMoreModules,
                    )
                },
            ),
    )
    SectionHeader(stringResource(R.string.section_legal))
    RowGroup(
        rows =
            listOf { shape ->
                NavigationRow(
                    shape = shape,
                    icon = Icons.Outlined.Gavel,
                    title = stringResource(R.string.about_licenses),
                    summary = stringResource(R.string.about_licenses_summary),
                    onClick = onOpenLicenses,
                )
            },
        modifier = Modifier.padding(bottom = 24.dp),
    )
}

private sealed interface UpdateCheck {
    data object Idle : UpdateCheck

    data object Checking : UpdateCheck

    data object Latest : UpdateCheck

    data class Available(
        val release: LatestRelease,
    ) : UpdateCheck

    data object Failed : UpdateCheck
}

/** "Version": tap to ask GitHub for the latest release; tap again to open an update. */
@Composable
private fun VersionRow(
    shape: Shape,
    versionName: String,
    onOpenUrl: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var check by remember { mutableStateOf<UpdateCheck>(UpdateCheck.Idle) }
    val installed = "v$versionName"
    val summary =
        when (val c = check) {
            UpdateCheck.Idle -> stringResource(R.string.about_version_tap, installed)
            UpdateCheck.Checking -> stringResource(R.string.about_update_checking)
            UpdateCheck.Latest -> stringResource(R.string.about_update_latest, installed)
            is UpdateCheck.Available -> stringResource(R.string.about_update_available, installed, "v${c.release.version}")
            UpdateCheck.Failed -> stringResource(R.string.about_update_failed, installed)
        }
    NavigationRow(
        shape = shape,
        icon = Icons.Outlined.Info,
        title = stringResource(R.string.about_version_title),
        summary = summary,
        trailing = if (check is UpdateCheck.Available) RowTrailing.External else RowTrailing.None,
        onClick = {
            when (val c = check) {
                is UpdateCheck.Available -> {
                    onOpenUrl(c.release.url)
                }

                UpdateCheck.Checking -> {}

                else -> {
                    check = UpdateCheck.Checking
                    scope.launch {
                        check =
                            runCatching { GitHub.latestRelease() }.fold(
                                onSuccess = { if (isNewer(it.version, versionName)) UpdateCheck.Available(it) else UpdateCheck.Latest },
                                onFailure = { UpdateCheck.Failed },
                            )
                    }
                }
            }
        },
    )
}

/** Compares dotted version numbers ("1.10.0" > "1.9.2"); anything non-numeric counts as 0. */
private fun isNewer(
    candidate: String,
    current: String,
): Boolean {
    fun parts(v: String) = v.substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 }
    val a = parts(candidate)
    val b = parts(current)
    for (i in 0 until maxOf(a.size, b.size)) {
        val diff = a.getOrElse(i) { 0 } - b.getOrElse(i) { 0 }
        if (diff != 0) return diff > 0
    }
    return false
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun IdentityCard(
    about: AboutInfo,
    onOpenUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val meta = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace)
    Surface(
        shape = RoundedCornerShape(Tokens.HeaderCardCorner),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val icon = about.icon
            if (icon != null) {
                val pulse = rememberLogoPulse()
                MorphingLogo(
                    pulse = pulse,
                    modifier =
                        Modifier
                            .size(LogoSize)
                            .pointerInput(pulse) { detectTapGestures { pulse.tap() } },
                ) {
                    Image(
                        bitmap = icon,
                        contentDescription = null,
                        modifier =
                            Modifier.fillMaxSize().graphicsLayer {
                                scaleX = ADAPTIVE_ICON_ZOOM
                                scaleY = ADAPTIVE_ICON_ZOOM
                            },
                    )
                }
            }
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineSmall)
            Text(
                stringResource(R.string.app_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AssistChip(
                    onClick = { onOpenUrl(RELEASES_URL) },
                    label = { Text("v${about.versionName} (${about.versionCode})", style = meta) },
                    leadingIcon = {
                        Icon(Icons.Outlined.Sell, null, Modifier.size(AssistChipDefaults.IconSize))
                    },
                )
                AssistChip(
                    onClick = { onOpenUrl(LICENSE_URL) },
                    label = { Text(BuildConfig.LICENSE, style = meta) },
                    leadingIcon = {
                        Icon(Icons.Outlined.Balance, null, Modifier.size(AssistChipDefaults.IconSize))
                    },
                )
            }
            SupportButtons(onOpenUrl = onOpenUrl, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

/** Two connected buttons that stack vertically when their labels don't fit side by side. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SupportButtons(
    onOpenUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val labels =
        listOf(
            stringResource(R.string.about_star_on_github),
            stringResource(R.string.about_kofi),
        )
    AdaptiveButtonRow(items = labels, modifier = modifier) { index, label, stacked, itemModifier ->
        val buttonModifier = itemModifier.heightIn(min = 52.dp)
        if (index == 0) {
            Button(
                onClick = { onOpenUrl(REPO_URL) },
                shapes =
                    if (stacked) {
                        ButtonDefaults.shapes()
                    } else {
                        ButtonDefaults.shapes(
                            ButtonGroupDefaults.connectedLeadingButtonShape,
                            ButtonGroupDefaults.connectedLeadingButtonPressShape,
                        )
                    },
                modifier = buttonModifier,
            ) {
                Icon(Icons.Outlined.StarOutline, null, Modifier.size(ButtonDefaults.IconSize))
                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                Text(label, maxLines = 1)
            }
        } else {
            FilledTonalButton(
                onClick = { onOpenUrl(KOFI_URL) },
                shapes =
                    if (stacked) {
                        ButtonDefaults.shapes()
                    } else {
                        ButtonDefaults.shapes(
                            ButtonGroupDefaults.connectedTrailingButtonShape,
                            ButtonGroupDefaults.connectedTrailingButtonPressShape,
                        )
                    },
                modifier = buttonModifier,
            ) {
                Icon(Icons.Outlined.Coffee, null, Modifier.size(ButtonDefaults.IconSize))
                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                Text(label, maxLines = 1)
            }
        }
    }
}

@Composable
fun LicensesScreen(onBack: () -> Unit) {
    val libraries by produceLibraries(R.raw.aboutlibraries)
    val listState = rememberLazyListState()
    DetailScaffold(
        title = stringResource(R.string.about_licenses),
        onBack = onBack,
    ) { contentPadding ->
        LibrariesContainer(
            libraries = libraries,
            modifier = Modifier.centeredContentWidth(),
            lazyListState = listState,
            contentPadding = contentPadding,
        )
    }
}
