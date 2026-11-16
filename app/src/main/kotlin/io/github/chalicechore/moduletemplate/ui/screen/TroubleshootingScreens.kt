@file:Suppress("ktlint:standard:function-naming")

package io.github.chalicechore.moduletemplate.ui.screen

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import io.github.chalicechore.moduletemplate.BuildConfig
import io.github.chalicechore.moduletemplate.R
import io.github.chalicechore.moduletemplate.prefs.FeatureHealth
import io.github.chalicechore.moduletemplate.prefs.FeatureSwitch
import io.github.chalicechore.moduletemplate.prefs.Troubleshooting
import io.github.chalicechore.moduletemplate.ui.UiState
import io.github.chalicechore.moduletemplate.ui.component.DetailScaffold
import io.github.chalicechore.moduletemplate.ui.component.NavigationRow
import io.github.chalicechore.moduletemplate.ui.component.ReadOnlyRow
import io.github.chalicechore.moduletemplate.ui.component.RowGroup
import io.github.chalicechore.moduletemplate.ui.component.RowTrailing
import io.github.chalicechore.moduletemplate.ui.component.SectionHeader
import io.github.chalicechore.moduletemplate.ui.component.SwitchRow
import io.github.chalicechore.moduletemplate.ui.component.centeredContentWidth
import io.github.chalicechore.moduletemplate.ui.icon
import io.github.chalicechore.moduletemplate.ui.titleRes
import io.github.chalicechore.moduletemplate.ui.theme.Tokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import java.text.DateFormat
import java.util.Date

private const val MAX_ISSUE_URL = 8_000
private const val LOG_LINES = 200

/** About's "Troubleshooting" group: Feature health, Debug logging and Report a problem. */
@Composable
fun TroubleshootingGroup(
    state: UiState.Ready,
    troubleshooting: Troubleshooting,
    onOpenHealth: () -> Unit,
    onDebugLoggingChange: (Boolean) -> Unit,
) {
    var showWhat by rememberSaveable { mutableStateOf(false) }
    var showReport by rememberSaveable { mutableStateOf(false) }
    val failed = state.failedFeatures.size
    SectionHeader(stringResource(R.string.section_troubleshooting))
    RowGroup(
        rows =
            listOf<@Composable (Shape) -> Unit>(
                { shape ->
                    NavigationRow(
                        shape = shape,
                        icon = Icons.Outlined.HealthAndSafety,
                        title = stringResource(R.string.health_title),
                        summary =
                            if (failed > 0) {
                                pluralStringResource(R.plurals.status_partly_body, failed, failed)
                            } else {
                                stringResource(R.string.health_summary_ok, stringResource(R.string.target_label))
                            },
                        onClick = onOpenHealth,
                    )
                },
                { shape ->
                    val until = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(troubleshooting.debugUntil))
                    SwitchRow(
                        shape = shape,
                        icon = Icons.Outlined.Terminal,
                        title = stringResource(R.string.debug_title),
                        summary =
                            if (troubleshooting.debugOn) {
                                stringResource(R.string.debug_on_summary, until)
                            } else {
                                stringResource(R.string.debug_off_summary)
                            },
                        checked = troubleshooting.debugOn,
                        onCheckedChange = onDebugLoggingChange,
                    )
                },
                { shape ->
                    NavigationRow(
                        shape = shape,
                        icon = Icons.Outlined.HelpOutline,
                        title = stringResource(R.string.debug_what_title),
                        summary = null,
                        onClick = { showWhat = true },
                        trailing = RowTrailing.None,
                    )
                },
                { shape ->
                    NavigationRow(
                        shape = shape,
                        icon = Icons.Outlined.BugReport,
                        title = stringResource(R.string.report_title),
                        summary = stringResource(R.string.report_summary),
                        onClick = { showReport = true },
                        trailing = RowTrailing.None,
                    )
                },
            ),
    )
    if (showWhat) {
        AlertDialog(
            onDismissRequest = { showWhat = false },
            title = { Text(stringResource(R.string.debug_what_title)) },
            text = { Text(stringResource(R.string.debug_what_body)) },
            confirmButton = { TextButton(onClick = { showWhat = false }) { Text(stringResource(R.string.dialog_ok)) } },
        )
    }
    if (showReport) ReportDialog(state, troubleshooting, onDismiss = { showReport = false })
}

/** Shows the whole report before anything leaves the phone, then opens the GitHub issue form. */
@Composable
private fun ReportDialog(
    state: UiState.Ready,
    troubleshooting: Troubleshooting,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val report by produceState<Report?>(null) {
        value = withContext(Dispatchers.IO) { buildReport(context, state, troubleshooting) }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.report_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(stringResource(R.string.report_note), style = MaterialTheme.typography.bodyMedium)
                Text(stringResource(R.string.report_contains_title), style = MaterialTheme.typography.titleSmall)
                Text(stringResource(R.string.report_contains), style = MaterialTheme.typography.bodySmall)
                Text(stringResource(R.string.report_never_title), style = MaterialTheme.typography.titleSmall)
                Text(stringResource(R.string.report_never), style = MaterialTheme.typography.bodySmall)
                Text(stringResource(R.string.report_preview), style = MaterialTheme.typography.titleSmall)
                SelectionContainer {
                    Text(
                        report?.fullText ?: "…",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = report != null,
                onClick = {
                    val r = report ?: return@TextButton
                    val url = r.issueUrl()
                    if (url.length > MAX_ISSUE_URL) {
                        copy(context, r.fullText)
                        Toast.makeText(context, R.string.report_copied, Toast.LENGTH_LONG).show()
                        runCatching { uriHandler.openUri(r.issueUrl(withFields = false)) }
                    } else {
                        runCatching { uriHandler.openUri(url) }
                    }
                    onDismiss()
                },
            ) { Text(stringResource(R.string.report_open)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) }
        },
    )
}

/** Field ids match .github/ISSUE_TEMPLATE/bug_report.yml. */
private class Report(
    val device: String,
    val versions: String,
    val framework: String,
    val health: String,
    val logs: String?,
) {
    val fullText: String
        get() =
            buildString {
                appendLine("Device: $device")
                appendLine("Versions: $versions")
                appendLine("Framework: $framework")
                appendLine("Feature health:")
                appendLine(health)
                if (logs != null) {
                    appendLine("Debug log:")
                    append(logs)
                }
            }.trim()

    fun issueUrl(withFields: Boolean = true): String {
        val base = "https://github.com/${BuildConfig.REPO}/issues/new?template=bug_report.yml"
        if (!withFields) return base
        fun field(
            id: String,
            value: String,
        ) = "&$id=" + URLEncoder.encode(value, "UTF-8")
        return base + field("device", device) + field("versions", versions) + field("framework", framework) +
            field("health", health) + (logs?.let { field("logs", it) } ?: "")
    }
}

private fun buildReport(
    context: Context,
    state: UiState.Ready,
    troubleshooting: Troubleshooting,
): Report {
    val targetVersion =
        runCatching { context.packageManager.getPackageInfo(BuildConfig.TARGET_PACKAGE, 0).versionName }
            .getOrNull() ?: "not installed"
    val health =
        FeatureSwitch.entries.joinToString("\n") { feature ->
            val result =
                when (val h = state.hookStatus.features[feature]) {
                    FeatureHealth.Ok -> "ok"
                    is FeatureHealth.Failed -> "FAILED: ${h.reason}"
                    null -> "not checked"
                }
            "- ${feature.key}: $result"
        }
    return Report(
        device = "${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}), ${Build.DISPLAY}",
        versions = "${BuildConfig.TARGET_PACKAGE} v$targetVersion · ${BuildConfig.APPLICATION_ID} v${BuildConfig.VERSION_NAME}",
        framework = state.framework?.let { "${it.name} ${it.versionName} (API ${it.apiVersion})" } ?: "not active",
        health = health,
        logs = if (troubleshooting.debugOn) rootLogs() else null,
    )
}

/** The hook's own log lines (its tag only), read with root; null when root isn't available. */
private fun rootLogs(): String? =
    runCatching {
        val process =
            ProcessBuilder("su", "-c", "logcat -d -v time -t $LOG_LINES -s ModuleTemplate:*")
                .redirectErrorStream(true)
                .start()
        process.inputStream.bufferedReader().use { it.readText() }.takeIf { process.waitFor() == 0 }
    }.getOrNull()?.trim()?.takeIf { it.isNotEmpty() }

private fun copy(
    context: Context,
    text: String,
) {
    context
        .getSystemService(ClipboardManager::class.java)
        .setPrimaryClip(ClipData.newPlainText(context.getString(R.string.report_title), text))
}

/** Every feature with what its hook did at the target's last start, plus failure alerts. */
@Composable
fun FeatureHealthScreen(
    state: UiState.Ready,
    troubleshooting: Troubleshooting,
    onHealthAlertsChange: (Boolean) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val permission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            onHealthAlertsChange(granted)
        }
    val target = stringResource(R.string.target_label)
    DetailScaffold(title = stringResource(R.string.health_title), onBack = onBack) { contentPadding ->
        Column(
            modifier =
                Modifier
                    .centeredContentWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(contentPadding),
        ) {
            val checkedAt = state.hookStatus.loadedAt
            Text(
                text =
                    if (checkedAt == null) {
                        stringResource(R.string.health_never, target)
                    } else {
                        stringResource(
                            R.string.health_checked,
                            target,
                            state.hookStatus.targetVersion ?: "?",
                            DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(checkedAt)),
                        )
                    },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(horizontal = Tokens.RowPadding + 4.dp, vertical = 12.dp),
            )
            RowGroup(
                rows =
                    FeatureSwitch.entries.map { feature ->
                        { shape ->
                            val health = state.hookStatus.features[feature]
                            ReadOnlyRow(
                                shape = shape,
                                icon = feature.icon,
                                title = stringResource(feature.titleRes),
                                summary =
                                    when (health) {
                                        FeatureHealth.Ok -> stringResource(R.string.health_ok)
                                        is FeatureHealth.Failed -> stringResource(R.string.health_failed, health.reason)
                                        null -> stringResource(R.string.health_not_checked)
                                    },
                                trailingIcon =
                                    when (health) {
                                        FeatureHealth.Ok -> Icons.Outlined.CheckCircle
                                        is FeatureHealth.Failed -> Icons.Outlined.ErrorOutline
                                        null -> Icons.Outlined.RemoveCircleOutline
                                    },
                            )
                        }
                    },
            )
            SectionHeader(stringResource(R.string.health_alerts_section))
            RowGroup(
                rows =
                    listOf { shape ->
                        SwitchRow(
                            shape = shape,
                            icon = Icons.Outlined.NotificationsActive,
                            title = stringResource(R.string.health_alerts_title),
                            summary = stringResource(R.string.health_alerts_summary, target),
                            checked = troubleshooting.healthAlerts && notificationsAllowed(context),
                            onCheckedChange = { on ->
                                when {
                                    !on -> onHealthAlertsChange(false)
                                    notificationsAllowed(context) -> onHealthAlertsChange(true)
                                    // The switch only stays on if the permission is granted.
                                    else -> permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            },
                        )
                    },
            )
        }
    }
}

private fun notificationsAllowed(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
