@file:Suppress("ktlint:standard:function-naming")

package io.github.chalicechore.moduletemplate.ui.component

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Checkbox
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.chalicechore.moduletemplate.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class PickableApp(
    val packageName: String,
    val label: String,
    val system: Boolean,
)

/** Launchable apps (visible through the manifest's launcher-intent query), sorted by name. */
private fun loadApps(context: Context): List<PickableApp> {
    val pm = context.packageManager
    return pm
        .queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
        .map { it.activityInfo.applicationInfo }
        .distinctBy { it.packageName }
        .filter { it.packageName != context.packageName }
        .map {
            PickableApp(
                packageName = it.packageName,
                label = it.loadLabel(pm).toString(),
                system = it.flags and ApplicationInfo.FLAG_SYSTEM != 0,
            )
        }.sortedBy { it.label.lowercase() }
}

/**
 * #14 Pick one app: a search field, app icons with labels, "Installed" and "System" groups, and
 * the current choice pinned at the top.
 */
@Composable
fun AppPickerDialog(
    title: String,
    selected: String?,
    onPick: (PickableApp) -> Unit,
    onDismiss: () -> Unit,
) {
    val pinned = remember { setOfNotNull(selected) }
    AppPickerFrame(
        title = title,
        pinned = pinned,
        onDismiss = onDismiss,
        row = { app -> AppRow(app, checked = app.packageName == selected, multi = false) { onPick(app) } },
        confirm = null,
    )
}

/**
 * #14b Pick several apps with checkboxes. Apps checked when the dialog opens are listed first;
 * the order then stays put while boxes are ticked, so rows never jump under the finger.
 */
@Composable
fun AppMultiPickerDialog(
    title: String,
    selected: Set<String>,
    onDone: (List<PickableApp>) -> Unit,
    onDismiss: () -> Unit,
) {
    val pinned = remember { selected }
    var checked by rememberSaveable { mutableStateOf(ArrayList(selected)) }
    var all by remember { mutableStateOf<List<PickableApp>>(emptyList()) }
    AppPickerFrame(
        title = title,
        pinned = pinned,
        onDismiss = onDismiss,
        onLoaded = { all = it },
        row = { app ->
            val on = app.packageName in checked
            AppRow(app, checked = on, multi = true) {
                checked = ArrayList(if (on) checked - app.packageName else checked + app.packageName)
            }
        },
        confirm = {
            TextButton(onClick = { onDone(all.filter { it.packageName in checked }) }) {
                Text(stringResource(R.string.dialog_done))
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AppPickerFrame(
    title: String,
    pinned: Set<String>,
    onDismiss: () -> Unit,
    row: @Composable (PickableApp) -> Unit,
    confirm: (@Composable () -> Unit)?,
    onLoaded: (List<PickableApp>) -> Unit = {},
) {
    val context = LocalContext.current
    val apps by produceState<List<PickableApp>?>(null) {
        value = withContext(Dispatchers.IO) { loadApps(context) }.also(onLoaded)
    }
    var query by rememberSaveable { mutableStateOf("") }
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Column(Modifier.padding(top = 24.dp, bottom = 8.dp)) {
                Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(horizontal = 24.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    leadingIcon = { Icon(Icons.Outlined.Search, null) },
                    placeholder = { Text(stringResource(R.string.app_picker_search)) },
                    singleLine = true,
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                )
                Box(Modifier.fillMaxWidth().height(420.dp)) {
                    val list = apps
                    if (list == null) {
                        LoadingIndicator(Modifier.align(Alignment.Center))
                    } else {
                        val filtered =
                            remember(list, query) {
                                list.filter { query.isBlank() || it.label.contains(query, true) || it.packageName.contains(query, true) }
                            }
                        val current = filtered.filter { it.packageName in pinned }
                        val user = filtered.filter { !it.system && it.packageName !in pinned }
                        val system = filtered.filter { it.system && it.packageName !in pinned }
                        LazyColumn(Modifier.fillMaxSize()) {
                            fun group(
                                header: Int,
                                items: List<PickableApp>,
                            ) {
                                if (items.isEmpty()) return
                                item(key = "h$header") { PickerHeader(stringResource(header)) }
                                items(items, key = { it.packageName }) { app -> row(app) }
                            }
                            group(R.string.app_picker_selected, current)
                            group(R.string.app_picker_installed, user)
                            group(R.string.app_picker_system, system)
                            if (filtered.isEmpty()) {
                                item { PickerHeader(stringResource(R.string.app_picker_none)) }
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) }
                    confirm?.invoke()
                }
            }
        }
    }
}

@Composable
private fun PickerHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 4.dp),
    )
}

@Composable
private fun AppRow(
    app: PickableApp,
    checked: Boolean,
    multi: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .then(
                    if (multi) {
                        Modifier.toggleable(checked, role = Role.Checkbox) { onClick() }
                    } else {
                        Modifier.selectable(checked, role = Role.RadioButton, onClick = onClick)
                    },
                )
                .heightIn(min = 56.dp)
                .padding(horizontal = 24.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        AppIcon(size = 40.dp, packageName = app.packageName)
        Column(Modifier.weight(1f)) {
            Text(app.label, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
            Text(app.packageName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
        if (multi) Checkbox(checked = checked, onCheckedChange = null) else RadioButton(selected = checked, onClick = null)
    }
}
