@file:Suppress("ktlint:standard:function-naming")

package io.github.chalicechore.moduletemplate.ui.screen

import android.os.Build
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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
 * and "App" (appearance, about). Drop "Always on" if the module has nothing fixed.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen(
    state: UiState.Ready,
    onOpenCategory: (FeatureCategory) -> Unit,
    onOpenAppearance: () -> Unit,
    onOpenAbout: () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val bottomPadding = listBottomPadding()
    val direction = LocalLayoutDirection.current
    val launcherIconVisible by rememberLauncherIconVisible()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
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
                    modifier = Modifier.padding(vertical = Tokens.ScreenMargin),
                )
            }
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
            item(key = "features_header", contentType = "header") {
                SectionHeader(stringResource(R.string.section_features))
            }
            val categories = FeatureCategory.entries
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
                    modifier =
                        Modifier
                            .padding(horizontal = Tokens.ScreenMargin)
                            .padding(top = if (index == 0) 0.dp else Tokens.RowGap),
                )
            }
            item(key = "features_footnote", contentType = "footnote") {
                Text(
                    text =
                        stringResource(
                            R.string.features_footnote,
                            stringResource(R.string.target_label),
                        ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier =
                        Modifier.padding(
                            horizontal = Tokens.RowPadding + 4.dp,
                            vertical = 12.dp,
                        ),
                )
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
    val on = settings.enabled.count { it.category == category }
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
