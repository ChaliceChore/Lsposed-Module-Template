@file:Suppress("ktlint:standard:function-naming")

package io.github.chalicechore.moduletemplate.ui.screen

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.RadioButtonChecked
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import io.github.chalicechore.moduletemplate.R
import io.github.chalicechore.moduletemplate.prefs.FeatureCategory
import io.github.chalicechore.moduletemplate.prefs.FeatureSwitch
import io.github.chalicechore.moduletemplate.prefs.SettingsKeys
import io.github.chalicechore.moduletemplate.ui.ChoiceCategory
import io.github.chalicechore.moduletemplate.ui.component.NavigationRow
import io.github.chalicechore.moduletemplate.ui.component.centeredContentWidth
import io.github.chalicechore.moduletemplate.ui.component.shapeForPosition
import io.github.chalicechore.moduletemplate.ui.icon
import io.github.chalicechore.moduletemplate.ui.summaryRes
import io.github.chalicechore.moduletemplate.ui.theme.Tokens
import io.github.chalicechore.moduletemplate.ui.titleRes

/** One searchable setting: where it lives and which row to highlight there. */
private data class SearchEntry(
    val key: String,
    val category: FeatureCategory,
    val icon: ImageVector,
    val title: String,
    val summary: String,
    val area: String,
)

/**
 * Searches every setting's title and summary, tolerating small typos. Built from the settings
 * catalog, so new settings are searchable with no extra work.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onOpenResult: (FeatureCategory, String) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val entries =
        remember {
            FeatureSwitch.entries.map {
                SearchEntry(
                    key = it.key,
                    category = it.category,
                    icon = it.icon,
                    title = context.getString(it.titleRes),
                    summary = context.getString(it.summaryRes),
                    area = context.getString(it.category.titleRes),
                )
            } +
                SearchEntry(
                    key = SettingsKeys.CHOICE,
                    category = ChoiceCategory,
                    icon = Icons.Outlined.RadioButtonChecked,
                    title = context.getString(R.string.option_choice_title),
                    summary = "",
                    area = context.getString(ChoiceCategory.titleRes),
                )
        }
    var query by rememberSaveable { mutableStateOf("") }
    val results = remember(query) { if (query.isBlank()) emptyList() else entries.filter { matches(it, query) } }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                title = {
                    TextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text(stringResource(R.string.search_hint)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { query = "" }) {
                                    Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.search_clear))
                                }
                            }
                        },
                        colors =
                            TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                            ),
                        modifier = Modifier.fillMaxWidth().focusRequester(focus),
                    )
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.centeredContentWidth().imePadding(),
            contentPadding = PaddingValues(top = padding.calculateTopPadding() + Tokens.ScreenMargin, bottom = 24.dp),
        ) {
            if (query.isNotBlank() && results.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.search_none),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(24.dp),
                    )
                }
            }
            if (query.isBlank()) {
                item {
                    Text(
                        stringResource(R.string.search_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(24.dp),
                    )
                }
            }
            itemsIndexed(results, key = { _, e -> e.key }) { index, entry ->
                NavigationRow(
                    shape = shapeForPosition(results.size, index),
                    icon = entry.icon,
                    title = entry.title,
                    summary = if (entry.summary.isEmpty()) entry.area else "${entry.area} · ${entry.summary}",
                    onClick = { onOpenResult(entry.category, entry.key) },
                    modifier = Modifier.padding(horizontal = Tokens.ScreenMargin).padding(top = if (index == 0) 0.dp else Tokens.RowGap),
                )
            }
        }
    }
}

/** Every query word must appear in the entry, allowing one typo in words of 4+ letters. */
private fun matches(
    entry: SearchEntry,
    query: String,
): Boolean {
    val haystack = "${entry.title} ${entry.summary} ${entry.area}".lowercase()
    val words = haystack.split(Regex("\\W+")).filter { it.isNotEmpty() }
    return query.lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }.all { q ->
        q in haystack || (q.length >= 4 && words.any { w -> closeEnough(q, w) })
    }
}

/** True when [query] is within one edit of [word] or of the start of [word]. */
private fun closeEnough(
    query: String,
    word: String,
): Boolean {
    val target = if (word.length > query.length) word.take(query.length + 1) else word
    return editDistance(query, target) <= 1 || editDistance(query, word.take(query.length)) <= 1
}

private fun editDistance(
    a: String,
    b: String,
): Int {
    var prev = IntArray(b.length + 1) { it }
    for (i in 1..a.length) {
        val cur = IntArray(b.length + 1)
        cur[0] = i
        for (j in 1..b.length) {
            cur[j] = minOf(prev[j] + 1, cur[j - 1] + 1, prev[j - 1] + if (a[i - 1] == b[j - 1]) 0 else 1)
        }
        prev = cur
    }
    return prev[b.length]
}
