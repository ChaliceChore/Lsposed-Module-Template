@file:Suppress("ktlint:standard:function-naming")

package io.github.chalicechore.moduletemplate.ui.screen

import android.os.Build
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.outlined.ArrowOutward
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import io.github.chalicechore.moduletemplate.HomeLayout
import io.github.chalicechore.moduletemplate.ModuleConfig
import io.github.chalicechore.moduletemplate.TryItAction
import io.github.chalicechore.moduletemplate.ui.ApplyPhase
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.chalicechore.moduletemplate.BuildConfig
import io.github.chalicechore.moduletemplate.R
import io.github.chalicechore.moduletemplate.prefs.Appearance
import io.github.chalicechore.moduletemplate.prefs.FeatureCategory
import io.github.chalicechore.moduletemplate.prefs.FeatureSettings
import io.github.chalicechore.moduletemplate.prefs.FeatureSwitch
import io.github.chalicechore.moduletemplate.prefs.ThemeMode
import io.github.chalicechore.moduletemplate.ui.UiState
import io.github.chalicechore.moduletemplate.ui.component.NavigationRow
import io.github.chalicechore.moduletemplate.ui.component.ReadOnlyRow
import io.github.chalicechore.moduletemplate.ui.component.SectionHeader
import io.github.chalicechore.moduletemplate.ui.component.StatusCard
import io.github.chalicechore.moduletemplate.ui.component.centeredContentWidth
import io.github.chalicechore.moduletemplate.ui.component.listBottomPadding
import io.github.chalicechore.moduletemplate.ui.component.rememberLauncherIconVisible
import io.github.chalicechore.moduletemplate.ui.component.shapeForPosition
import io.github.chalicechore.moduletemplate.ui.icon
import io.github.chalicechore.moduletemplate.ui.theme.Tokens
import io.github.chalicechore.moduletemplate.ui.titleRes

/**
 * Landing screen: large collapsing title, status card, then grouped rows.
 *
 * TEMPLATE: sections are "Always on" (read-only info), "Features" (one row per category)
 * and "App" (appearance, about). "Always on" is optional: ModuleConfig.ALWAYS_ON.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen(
    state: UiState.Ready,
    onOpenCategory: (FeatureCategory) -> Unit,
    onOpenAppearance: () -> Unit,
    onOpenAbout: () -> Unit,
    onModuleEnabledChange: (Boolean) -> Unit,
    onOpenHealth: () -> Unit,
    onApply: () -> Unit,
    onOpenSearch: (() -> Unit)? = null,
    onOpenComponents: (() -> Unit)? = null,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val bottomPadding = listBottomPadding()
    val direction = LocalLayoutDirection.current
    val launcherIconVisible by rememberLauncherIconVisible()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val targetLabel = stringResource(R.string.target_label)
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            LargeFlexibleTopAppBar(
                title = { TitleWithIcon(stringResource(R.string.app_name)) },
                actions = {
                    if (onOpenSearch != null) {
                        IconButton(onClick = onOpenSearch) {
                            Icon(Icons.Outlined.Search, contentDescription = stringResource(R.string.search_hint))
                        }
                    }
                    ModuleConfig.tryIt?.let { action ->
                        TryItButton(
                            action = action,
                            state = state,
                            onApply = onApply,
                            onCannotOpen = {
                                scope.launch {
                                    snackbar.showSnackbar(context.getString(R.string.try_it_failed, targetLabel))
                                }
                            },
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.centeredContentWidth(),
            contentPadding =
                PaddingValues(
                    start = innerPadding.calculateStartPadding(direction),
                    top = innerPadding.calculateTopPadding(),
                    end = innerPadding.calculateEndPadding(direction),
                    bottom = bottomPadding,
                ),
        ) {
            item(key = "status", contentType = "status") {
                StatusCard(
                    state = state,
                    onModuleEnabledChange = onModuleEnabledChange,
                    onOpenHealth = onOpenHealth,
                    modifier = Modifier.padding(top = Tokens.ScreenMargin),
                )
            }
            item(key = "versions", contentType = "footnote") {
                VersionsFooter()
            }
            if (ModuleConfig.ALWAYS_ON) {
                item(key = "always_on_header", contentType = "header") {
                    SectionHeader(stringResource(R.string.section_always_on))
                }
                item(key = "always_on", contentType = "read_only") {
                    ReadOnlyRow(
                        shape = shapeForPosition(1, 0),
                        icon = Icons.Outlined.Shield,
                        title = stringResource(R.string.always_on_title),
                        summary = stringResource(R.string.always_on_summary),
                        trailingIcon = Icons.Outlined.CheckCircle,
                        modifier = Modifier.padding(horizontal = Tokens.ScreenMargin),
                    )
                }
            }
            item(key = "features_header", contentType = "header") {
                SectionHeader(stringResource(R.string.section_features))
            }
            val categories = FeatureCategory.entries
            val featuresOn = state.desired.moduleEnabled
            if (state.appearance.homeLayout == HomeLayout.GRID) {
                val rows = categories.chunked(2)
                items(rows.size, key = { "grid$it" }, contentType = { "grid" }) { index ->
                    CategoryGridRow(
                        categories = rows[index],
                        settings = state.desired,
                        enabled = featuresOn,
                        onOpenCategory = onOpenCategory,
                        modifier =
                            Modifier
                                .padding(horizontal = Tokens.ScreenMargin)
                                .padding(top = if (index == 0) 0.dp else 8.dp),
                    )
                }
            } else {
                items(
                    categories.size,
                    key = { categories[it].name },
                    contentType = { "category" },
                ) { index ->
                    val category = categories[index]
                    NavigationRow(
                        shape = shapeForPosition(categories.size, index),
                        icon = category.icon,
                        title = stringResource(category.titleRes),
                        summary = categorySummary(category, state.desired),
                        onClick = { onOpenCategory(category) },
                        enabled = featuresOn,
                        modifier =
                            Modifier
                                .padding(horizontal = Tokens.ScreenMargin)
                                .padding(top = if (index == 0) 0.dp else Tokens.RowGap),
                    )
                }
            }
            if (onOpenComponents != null) {
                item(key = "components", contentType = "category") {
                    NavigationRow(
                        shape = shapeForPosition(1, 0),
                        icon = Icons.Outlined.Widgets,
                        title = stringResource(R.string.category_components),
                        summary = stringResource(R.string.category_components_summary),
                        onClick = onOpenComponents,
                        modifier = Modifier.padding(horizontal = Tokens.ScreenMargin).padding(top = 24.dp),
                    )
                }
            }
            item(key = "app_header", contentType = "header") {
                SectionHeader(stringResource(R.string.section_app))
            }
            item(key = "appearance", contentType = "category") {
                NavigationRow(
                    shape = shapeForPosition(2, 0),
                    icon = Icons.Outlined.Palette,
                    title = stringResource(R.string.appearance_title),
                    summary = appearanceSummary(state.appearance, launcherIconVisible),
                    onClick = onOpenAppearance,
                    modifier = Modifier.padding(horizontal = Tokens.ScreenMargin),
                )
            }
            item(key = "about", contentType = "category") {
                NavigationRow(
                    shape = shapeForPosition(2, 1),
                    icon = Icons.Outlined.Info,
                    title = stringResource(R.string.about_title),
                    summary = stringResource(R.string.about_summary),
                    onClick = onOpenAbout,
                    modifier =
                        Modifier
                            .padding(horizontal = Tokens.ScreenMargin)
                            .padding(top = Tokens.RowGap),
                )
            }
        }
    }
}

@Composable
private fun categorySummary(
    category: FeatureCategory,
    settings: FeatureSettings,
): String {
    val total = FeatureSwitch.entries.count { it.category == category }
    val on = settings.switches.count { it.category == category }
    return when {
        on == 0 -> stringResource(R.string.category_summary_off)
        total == 1 -> stringResource(R.string.category_summary_on_single)
        else -> stringResource(R.string.category_summary_on, on, total)
    }
}

@Composable
private fun appearanceSummary(
    appearance: Appearance,
    launcherIconVisible: Boolean,
): String {
    val theme =
        stringResource(
            when (appearance.themeMode) {
                ThemeMode.SYSTEM -> R.string.appearance_summary_system
                ThemeMode.LIGHT -> R.string.appearance_summary_light
                ThemeMode.DARK -> R.string.appearance_summary_dark
            },
        )
    val dynamic =
        stringResource(R.string.appearance_summary_dynamic_color)
            .takeIf { appearance.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S }
    val iconHidden =
        stringResource(R.string.appearance_summary_icon_hidden).takeUnless { launcherIconVisible }
    return listOfNotNull(theme, dynamic, iconHidden).joinToString(", ")
}

/** "<Target> v15.2.1 · <Module> v1.4.0", for support requests and screenshots. */
@Composable
private fun VersionsFooter() {
    val context = LocalContext.current
    val targetVersion =
        remember {
            runCatching {
                context.packageManager.getPackageInfo(BuildConfig.TARGET_PACKAGE, 0).versionName
            }.getOrNull()
        }
    val target = stringResource(R.string.target_label)
    val module = stringResource(R.string.app_name)
    Box(Modifier.fillMaxWidth().padding(horizontal = Tokens.ScreenMargin, vertical = 12.dp), contentAlignment = Alignment.Center) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainer) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    Icons.Outlined.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text =
                        if (targetVersion != null) {
                            stringResource(R.string.versions_footer, target, targetVersion, module, BuildConfig.VERSION_NAME)
                        } else {
                            stringResource(R.string.versions_footer_no_target, target, module, BuildConfig.VERSION_NAME)
                        },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/**
 * The app name after the launcher glyph alone (no adaptive-icon background), tinted with the
 * theme's primary colour: the wallpaper colour with Dynamic color on, otherwise the brand seed.
 */
@Composable
private fun TitleWithIcon(title: String) {
    val iconSize = with(LocalDensity.current) { (LocalTextStyle.current.fontSize * 1.05f).toDp() }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            // The glyph sits in the 66 dp safe zone of a 108 dp canvas; crop to that zone.
            modifier =
                Modifier.size(iconSize).graphicsLayer {
                    scaleX = LAUNCHER_SAFE_ZONE_ZOOM
                    scaleY = LAUNCHER_SAFE_ZONE_ZOOM
                },
        )
        Spacer(Modifier.width(10.dp))
        Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/**
 * "Try it" opens the screen the module changes. Disabled while the module isn't active or has
 * been turned off. With unapplied changes it first asks whether to apply them.
 */
@Composable
private fun TryItButton(
    action: TryItAction,
    state: UiState.Ready,
    onApply: () -> Unit,
    onCannotOpen: () -> Unit,
) {
    val context = LocalContext.current
    var ask by rememberSaveable { mutableStateOf(false) }
    var tryAfterApply by rememberSaveable { mutableStateOf(false) }
    fun launch() {
        val intent = action.intent(context)?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val opened = intent != null && runCatching { context.startActivity(intent) }.isSuccess
        if (!opened) onCannotOpen()
    }
    LaunchedEffect(tryAfterApply, state.phase, state.pendingCount) {
        if (!tryAfterApply || state.phase.isRunning) return@LaunchedEffect
        if (state.phase is ApplyPhase.Failed) {
            tryAfterApply = false
        } else if (state.pendingCount == 0) {
            tryAfterApply = false
            launch()
        }
    }
    val enabled =
        state.framework != null && state.appliedOrDefault.moduleEnabled && !state.phase.isRunning
    FilledTonalButton(
        onClick = { if (state.pendingCount > 0) ask = true else launch() },
        enabled = enabled,
        contentPadding = PaddingValues(start = 16.dp, end = 12.dp),
        modifier = Modifier.padding(end = 8.dp),
    ) {
        Text(stringResource(action.labelRes))
        Spacer(Modifier.width(6.dp))
        Icon(Icons.Outlined.ArrowOutward, contentDescription = null, modifier = Modifier.size(18.dp))
    }
    if (ask) {
        AlertDialog(
            onDismissRequest = { ask = false },
            title = { Text(stringResource(R.string.try_it_pending_title)) },
            text = { Text(stringResource(R.string.try_it_pending_body)) },
            confirmButton = {
                TextButton(onClick = {
                    ask = false
                    tryAfterApply = true
                    onApply()
                }) { Text(stringResource(R.string.try_it_apply_and_try)) }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { ask = false }) { Text(stringResource(R.string.dialog_cancel)) }
                    TextButton(onClick = {
                        ask = false
                        launch()
                    }) { Text(stringResource(R.string.try_it_anyway)) }
                }
            },
        )
    }
}

/** Two category cards side by side, both as tall as the taller one. */
@Composable
private fun CategoryGridRow(
    categories: List<FeatureCategory>,
    settings: FeatureSettings,
    enabled: Boolean,
    onOpenCategory: (FeatureCategory) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        categories.forEach { category ->
            CategoryCard(
                category = category,
                summary = categorySummary(category, settings),
                enabled = enabled,
                onClick = { onOpenCategory(category) },
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
        }
        if (categories.size == 1) Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun CategoryCard(
    category: FeatureCategory,
    summary: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(Tokens.OuterRowCorner),
        color = colors.surfaceContainerHigh,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.alpha(if (enabled) 1f else DISABLED_ALPHA).padding(Tokens.RowPadding),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(44.dp)
                        .background(colors.secondaryContainer, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(category.icon, contentDescription = null, tint = colors.onSecondaryContainer)
            }
            Spacer(Modifier.height(8.dp))
            Text(stringResource(category.titleRes), style = MaterialTheme.typography.titleMedium)
            Text(summary, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        }
    }
}

private const val DISABLED_ALPHA = 0.38f
private const val LAUNCHER_SAFE_ZONE_ZOOM = 108f / 66f
