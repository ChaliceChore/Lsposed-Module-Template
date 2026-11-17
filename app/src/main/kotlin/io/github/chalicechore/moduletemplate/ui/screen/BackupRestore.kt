@file:Suppress("ktlint:standard:function-naming")

package io.github.chalicechore.moduletemplate.ui.screen

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.chalicechore.moduletemplate.BuildConfig
import io.github.chalicechore.moduletemplate.HomeLayout
import io.github.chalicechore.moduletemplate.R
import io.github.chalicechore.moduletemplate.prefs.Appearance
import io.github.chalicechore.moduletemplate.prefs.FeatureSettings
import io.github.chalicechore.moduletemplate.prefs.SettingsKeys
import io.github.chalicechore.moduletemplate.prefs.ThemeMode
import io.github.chalicechore.moduletemplate.ui.UiState
import io.github.chalicechore.moduletemplate.ui.component.NavigationRow
import io.github.chalicechore.moduletemplate.ui.component.RowGroup
import io.github.chalicechore.moduletemplate.ui.component.RowTrailing
import io.github.chalicechore.moduletemplate.ui.component.SectionHeader
import io.github.chalicechore.moduletemplate.ui.labelRes
import io.github.chalicechore.moduletemplate.ui.titleRes
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.material.icons.outlined.SettingsBackupRestore
import io.github.chalicechore.moduletemplate.ModuleConfig
import io.github.chalicechore.moduletemplate.ui.component.DetailScaffold
import io.github.chalicechore.moduletemplate.ui.component.centeredContentWidth
import io.github.chalicechore.moduletemplate.ui.theme.Tokens

/** A backup's parts. Each can be saved or restored on its own. */
enum class BackupSection(
    @param:StringRes val labelRes: Int,
    @param:StringRes val noteRes: Int,
) {
    SETTINGS(R.string.backup_section_settings, R.string.backup_section_settings_note),
    APPEARANCE(R.string.backup_section_appearance, R.string.backup_section_appearance_note),
}

/** What a backup file holds; a missing section is null. */
data class Backup(
    val settings: FeatureSettings?,
    val appearance: Appearance?,
) {
    fun has(section: BackupSection) =
        when (section) {
            BackupSection.SETTINGS -> settings != null
            BackupSection.APPEARANCE -> appearance != null
        }
}

private const val FORMAT = 1

/** One JSON file, readable by eye: {"module", "version", "format", "settings", "appearance"}. */
private fun encode(
    state: UiState.Ready,
    sections: Set<BackupSection>,
): String {
    val root =
        JSONObject()
            .put("module", BuildConfig.APPLICATION_ID)
            .put("version", BuildConfig.VERSION_NAME)
            .put("format", FORMAT)
    if (BackupSection.SETTINGS in sections) {
        val map = mutableMapOf<String, Any?>()
        val editor = MapEditor(map)
        SettingsKeys.write(state.desired, editor)
        root.put("settings", JSONObject(map))
    }
    if (BackupSection.APPEARANCE in sections) {
        val a = state.appearance
        root.put(
            "appearance",
            JSONObject()
                .put("theme", a.themeMode.name)
                .put("dynamic_color", a.dynamicColor)
                .put("pure_black", a.pureBlack)
                .put("home_layout", a.homeLayout.name),
        )
    }
    return root.toString(2)
}

/** Reads a backup; throws for files that aren't one of this module's backups. */
private fun decode(json: String): Backup {
    val root = JSONObject(json)
    require(root.optString("module") == BuildConfig.APPLICATION_ID) { "Not a backup of this module" }
    val settings =
        root.optJSONObject("settings")?.let { s ->
            SettingsKeys.read(s.keys().asSequence().associateWith { s.get(it) })
        }
    val appearance =
        root.optJSONObject("appearance")?.let { a ->
            Appearance(
                themeMode = ThemeMode.entries.firstOrNull { it.name == a.optString("theme") } ?: ThemeMode.SYSTEM,
                dynamicColor = a.optBoolean("dynamic_color", true),
                pureBlack = a.optBoolean("pure_black", false),
                homeLayout = HomeLayout.entries.firstOrNull { it.name == a.optString("home_layout") } ?: HomeLayout.LIST,
            )
        }
    return Backup(settings, appearance)
}

/** The lines a restore would change, for the diff chip and its dialog. */
@Composable
private fun restoreDiff(
    state: UiState.Ready,
    backup: Backup,
    sections: Set<BackupSection>,
): List<String> {
    val on = stringResource(R.string.pending_change_on)
    val off = stringResource(R.string.pending_change_off)
    val lines = mutableListOf<String>()
    val restored = backup.settings
    if (BackupSection.SETTINGS in sections && restored != null) {
        val now = state.desired
        if (restored.moduleEnabled != now.moduleEnabled) {
            lines += "${stringResource(R.string.master_switch_title)}: ${if (restored.moduleEnabled) on else off}"
        }
        restored.differingSwitches(now).sorted().forEach {
            lines += "${stringResource(it.titleRes)}: ${if (restored.isOn(it)) on else off}"
        }
        if (restored.choice != now.choice) {
            lines += "${stringResource(R.string.option_choice_title)}: ${stringResource(restored.choice.labelRes)}"
        }
    }
    val look = backup.appearance
    if (BackupSection.APPEARANCE in sections && look != null) {
        val now = state.appearance
        if (look.themeMode != now.themeMode) lines += "${stringResource(R.string.appearance_theme)}: ${look.themeMode.name.lowercase()}"
        if (look.dynamicColor != now.dynamicColor) lines += "${stringResource(R.string.dynamic_color_title)}: ${if (look.dynamicColor) on else off}"
        if (look.pureBlack != now.pureBlack) lines += "${stringResource(R.string.pure_black_title)}: ${if (look.pureBlack) on else off}"
        if (look.homeLayout != now.homeLayout) lines += "${stringResource(R.string.home_layout_title)}: ${look.homeLayout.name.lowercase()}"
    }
    return lines
}

/**
 * The Data screen's rows: Back up and Restore (ModuleConfig.BACKUP_RESTORE) and Reset all
 * settings (ModuleConfig.RESET). Restore and reset both turn module settings into pending
 * changes, so they go through the apply sheet and can be undone there.
 */
@Composable
private fun DataGroup(
    state: UiState.Ready,
    onRestoreSettings: (FeatureSettings) -> Unit,
    onRestoreAppearance: (Appearance) -> Unit,
    backupRestore: Boolean = ModuleConfig.BACKUP_RESTORE,
    reset: Boolean = ModuleConfig.RESET,
) {
    if (!backupRestore && !reset) return
    var resetting by remember { mutableStateOf<Backup?>(null) }
    val context = LocalContext.current
    var backupSections by remember { mutableStateOf<Set<BackupSection>?>(null) }
    var pendingSave by remember { mutableStateOf<Set<BackupSection>>(emptySet()) }
    var restoring by remember { mutableStateOf<Backup?>(null) }
    val save =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            if (uri != null) write(context, uri, encode(state, pendingSave))
        }
    val open =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            restoring =
                runCatching {
                    decode(context.contentResolver.openInputStream(uri)!!.bufferedReader().use { it.readText() })
                }.getOrElse {
                    Toast.makeText(context, R.string.restore_invalid, Toast.LENGTH_LONG).show()
                    null
                }
        }
    RowGroup(
        rows =
            buildList<@Composable (Shape) -> Unit> {
                if (backupRestore) {
                    add { shape ->
                    NavigationRow(
                        shape = shape,
                        icon = Icons.Outlined.Backup,
                        title = stringResource(R.string.backup_title),
                        summary = stringResource(R.string.backup_summary),
                        onClick = { backupSections = BackupSection.entries.toSet() },
                        trailing = RowTrailing.None,
                    )
                    }
                    add { shape ->
                    NavigationRow(
                        shape = shape,
                        icon = Icons.Outlined.Restore,
                        title = stringResource(R.string.restore_title),
                        summary = stringResource(R.string.restore_summary),
                        onClick = { open.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
                        trailing = RowTrailing.None,
                    )
                    }
                }
                if (reset) {
                    add { shape ->
                        NavigationRow(
                            shape = shape,
                            icon = Icons.Outlined.SettingsBackupRestore,
                            title = stringResource(R.string.reset_all_title),
                            summary = stringResource(R.string.reset_all_summary),
                            // Defaults, keeping the module switch as it is.
                            onClick = { resetting = Backup(FeatureSettings(moduleEnabled = state.desired.moduleEnabled), Appearance()) },
                            trailing = RowTrailing.None,
                        )
                    }
                }
            },
    )
    resetting?.let { defaults ->
        var selected by rememberSaveable { mutableStateOf(setOf(BackupSection.SETTINGS)) }
        SectionsDialog(
            title = stringResource(R.string.reset_all_title),
            note = stringResource(R.string.reset_all_note),
            available = BackupSection.entries,
            selected = selected,
            onSelectedChange = { selected = it },
            diff = restoreDiff(state, defaults, selected),
            confirm = stringResource(R.string.reset_confirm),
            onConfirm = {
                if (BackupSection.SETTINGS in selected) defaults.settings?.let(onRestoreSettings)
                if (BackupSection.APPEARANCE in selected) defaults.appearance?.let(onRestoreAppearance)
                resetting = null
            },
            onDismiss = { resetting = null },
        )
    }
    backupSections?.let { selected ->
        SectionsDialog(
            title = stringResource(R.string.backup_title),
            note = stringResource(R.string.backup_note),
            available = BackupSection.entries,
            selected = selected,
            onSelectedChange = { backupSections = it },
            diff = null,
            confirm = stringResource(R.string.backup_save),
            onConfirm = {
                pendingSave = selected
                backupSections = null
                val stamp = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date())
                save.launch("${context.getString(R.string.app_name).replace(' ', '-')}-$stamp.json")
            },
            onDismiss = { backupSections = null },
        )
    }
    restoring?.let { backup ->
        var selected by rememberSaveable { mutableStateOf(BackupSection.entries.filter(backup::has).toSet()) }
        SectionsDialog(
            title = stringResource(R.string.restore_title),
            note = stringResource(R.string.restore_note),
            available = BackupSection.entries.filter(backup::has),
            selected = selected,
            onSelectedChange = { selected = it },
            diff = restoreDiff(state, backup, selected),
            confirm = stringResource(R.string.restore_confirm),
            onConfirm = {
                if (BackupSection.SETTINGS in selected) backup.settings?.let(onRestoreSettings)
                if (BackupSection.APPEARANCE in selected) backup.appearance?.let(onRestoreAppearance)
                restoring = null
            },
            onDismiss = { restoring = null },
        )
    }
}

@Composable
private fun SectionsDialog(
    title: String,
    note: String,
    available: List<BackupSection>,
    selected: Set<BackupSection>,
    onSelectedChange: (Set<BackupSection>) -> Unit,
    diff: List<String>?,
    confirm: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    var showDiff by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(note, style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { onSelectedChange(available.toSet()) }) { Text(stringResource(R.string.backup_all)) }
                    TextButton(onClick = { onSelectedChange(emptySet()) }) { Text(stringResource(R.string.backup_none)) }
                }
                available.forEach { section ->
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .toggleable(section in selected, role = Role.Checkbox) { on ->
                                    onSelectedChange(if (on) selected + section else selected - section)
                                },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = section in selected, onCheckedChange = null)
                        Column(Modifier.padding(start = 12.dp)) {
                            Text(stringResource(section.labelRes), style = MaterialTheme.typography.bodyLarge)
                            Text(
                                stringResource(section.noteRes),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                if (diff != null) {
                    AssistChip(
                        onClick = { showDiff = true },
                        enabled = diff.isNotEmpty(),
                        label = {
                            Text(
                                if (diff.isEmpty()) {
                                    stringResource(R.string.restore_no_changes)
                                } else {
                                    pluralStringResource(R.plurals.restore_changes, diff.size, diff.size)
                                },
                            )
                        },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = selected.isNotEmpty()) { Text(confirm) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) } },
    )
    if (showDiff && diff != null) {
        AlertDialog(
            onDismissRequest = { showDiff = false },
            title = { Text(stringResource(R.string.restore_diff_title)) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    diff.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
                }
            },
            confirmButton = { TextButton(onClick = { showDiff = false }) { Text(stringResource(R.string.dialog_ok)) } },
        )
    }
}

private fun write(
    context: Context,
    uri: Uri,
    text: String,
) {
    val ok =
        runCatching {
            context.contentResolver.openOutputStream(uri, "wt")!!.bufferedWriter().use { it.write(text) }
        }.isSuccess
    Toast.makeText(context, if (ok) R.string.backup_done else R.string.backup_failed, Toast.LENGTH_SHORT).show()
}

/** Collects SettingsKeys.write output into a map, so the backup uses the same keys. */
private class MapEditor(
    private val map: MutableMap<String, Any?>,
) : android.content.SharedPreferences.Editor {
    override fun putString(
        key: String,
        value: String?,
    ) = apply { map[key] = value }

    override fun putStringSet(
        key: String,
        values: MutableSet<String>?,
    ) = apply { map[key] = values?.toList() }

    override fun putInt(
        key: String,
        value: Int,
    ) = apply { map[key] = value }

    override fun putLong(
        key: String,
        value: Long,
    ) = apply { map[key] = value }

    override fun putFloat(
        key: String,
        value: Float,
    ) = apply { map[key] = value }

    override fun putBoolean(
        key: String,
        value: Boolean,
    ) = apply { map[key] = value }

    override fun remove(key: String) = apply { map.remove(key) }

    override fun clear() = apply { map.clear() }

    override fun commit() = true

    override fun apply() {}
}

/** "Data" on Home: Back up, Restore and Reset all settings. */
@Composable
fun DataScreen(
    state: UiState.Ready,
    onRestoreSettings: (FeatureSettings) -> Unit,
    onRestoreAppearance: (Appearance) -> Unit,
    onBack: () -> Unit,
) {
    DetailScaffold(title = stringResource(R.string.section_data), onBack = onBack) { contentPadding ->
        Column(
            Modifier
                .centeredContentWidth()
                .verticalScroll(rememberScrollState())
                .padding(contentPadding)
                .padding(top = Tokens.ScreenMargin),
        ) {
            DataGroup(state, onRestoreSettings, onRestoreAppearance)
        }
    }
}
