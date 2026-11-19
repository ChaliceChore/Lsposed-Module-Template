@file:Suppress("ktlint:standard:function-naming")

package io.github.chalicechore.moduletemplate.ui.theme

import android.content.ContentResolver
import android.database.ContentObserver
import android.graphics.Color.TRANSPARENT
import android.graphics.Color.argb
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.LocalActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Typography
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.chalicechore.moduletemplate.R
import io.github.chalicechore.moduletemplate.prefs.Appearance
import io.github.chalicechore.moduletemplate.prefs.ThemeMode
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.dynamiccolor.DynamicScheme
import com.materialkolor.hct.Hct
import com.materialkolor.scheme.SchemeTonalSpot
import me.zhanghai.compose.preference.ProvidePreferenceTheme
import me.zhanghai.compose.preference.preferenceTheme

// TEMPLATE: shared spacing and shape tokens. Keep them identical across modules.
object Tokens {
    val ScreenMargin = 8.dp
    val RowGap = 2.dp
    val RowPadding = 16.dp
    val RowMinHeight = 72.dp
    val OuterRowCorner = 24.dp
    val InnerRowCorner = 4.dp
    val HeaderCardCorner = 32.dp
    val ContentMaxWidth = 640.dp
}

private val GoogleSansFlex =
    FontFamily(
        listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold).map {
            Font(
                resId = R.font.google_sans_flex,
                weight = it,
                variationSettings =
                    FontVariation.Settings(
                        FontVariation.weight(it.weight),
                        FontVariation.Setting("ROND", 0f),
                    ),
            )
        },
    )

private val SettingsTypography = Typography(fontFamily = GoogleSansFlex)

private val LightNavigationScrim = argb(0xE6, 0xFF, 0xFF, 0xFF)
private val DarkNavigationScrim = argb(0x80, 0x1B, 0x1B, 0x1B)

val LocalReducedMotion = staticCompositionLocalOf { false }

@Composable
fun isReducedMotion(): Boolean = LocalReducedMotion.current

@Composable
private fun ProvideReducedMotion(content: @Composable () -> Unit) {
    val resolver = LocalContext.current.contentResolver
    var reduced by remember(resolver) { mutableStateOf(readReducedMotion(resolver)) }
    DisposableEffect(resolver) {
        val observer =
            object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    reduced = readReducedMotion(resolver)
                }
            }
        resolver.registerContentObserver(
            Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE),
            false,
            observer,
        )
        reduced = readReducedMotion(resolver)
        onDispose { resolver.unregisterContentObserver(observer) }
    }
    CompositionLocalProvider(LocalReducedMotion provides reduced, content = content)
}

private fun readReducedMotion(resolver: ContentResolver): Boolean =
    Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f

@Composable
private fun SystemBarStyles(darkTheme: Boolean) {
    val activity = LocalActivity.current as? ComponentActivity ?: return
    DisposableEffect(activity, darkTheme) {
        activity.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(TRANSPARENT, TRANSPARENT) { darkTheme },
            navigationBarStyle =
                SystemBarStyle.auto(LightNavigationScrim, DarkNavigationScrim) { darkTheme },
        )
        onDispose {}
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ModuleTheme(
    appearance: Appearance,
    content: @Composable () -> Unit,
) {
    val darkTheme =
        when (appearance.themeMode) {
            ThemeMode.SYSTEM -> isSystemInDarkTheme()
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
        }
    val context = LocalContext.current
    val colorScheme =
        when {
            appearance.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }

            else -> {
                remember(darkTheme) { seedColorScheme(BrandSeed, darkTheme) }
            }
        }.let { if (darkTheme && appearance.pureBlack) it.pureBlack() else it }
    SystemBarStyles(darkTheme)
    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = MotionScheme.standard(),
        typography = SettingsTypography,
    ) {
        ProvideReducedMotion {
            ProvidePreferenceTheme(theme = preferenceTheme(), content = content)
        }
    }
}

/**
 * Brand colour shared by all ChaliceChore modules: Deep Cerulean Teal. Used when dynamic color is
 * off or unavailable, and as the accent in launcher icons. Keep it the same in every module.
 */
val BrandSeed = Color(0xFF187B97)

/**
 * Material's own tonal-spot scheme generated from [seed], the same algorithm Android uses.
 * Uses the 2025 colour spec, the one Android 16 and later use for system dynamic colours.
 */
private fun seedColorScheme(
    seed: Color,
    dark: Boolean,
): ColorScheme {
    val s: DynamicScheme = SchemeTonalSpot(
            Hct.fromInt(seed.toArgb()),
            isDark = dark,
            contrastLevel = 0.0,
            specVersion = ColorSpec.SpecVersion.SPEC_2025,
        )
    fun c(argb: Int) = Color(argb)
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = c(s.primary),
        onPrimary = c(s.onPrimary),
        primaryContainer = c(s.primaryContainer),
        onPrimaryContainer = c(s.onPrimaryContainer),
        inversePrimary = c(s.inversePrimary),
        secondary = c(s.secondary),
        onSecondary = c(s.onSecondary),
        secondaryContainer = c(s.secondaryContainer),
        onSecondaryContainer = c(s.onSecondaryContainer),
        tertiary = c(s.tertiary),
        onTertiary = c(s.onTertiary),
        tertiaryContainer = c(s.tertiaryContainer),
        onTertiaryContainer = c(s.onTertiaryContainer),
        error = c(s.error),
        onError = c(s.onError),
        errorContainer = c(s.errorContainer),
        onErrorContainer = c(s.onErrorContainer),
        background = c(s.background),
        onBackground = c(s.onBackground),
        surface = c(s.surface),
        onSurface = c(s.onSurface),
        surfaceVariant = c(s.surfaceVariant),
        onSurfaceVariant = c(s.onSurfaceVariant),
        surfaceTint = c(s.surfaceTint),
        inverseSurface = c(s.inverseSurface),
        inverseOnSurface = c(s.inverseOnSurface),
        outline = c(s.outline),
        outlineVariant = c(s.outlineVariant),
        scrim = c(s.scrim),
        surfaceBright = c(s.surfaceBright),
        surfaceDim = c(s.surfaceDim),
        surfaceContainerLowest = c(s.surfaceContainerLowest),
        surfaceContainerLow = c(s.surfaceContainerLow),
        surfaceContainer = c(s.surfaceContainer),
        surfaceContainerHigh = c(s.surfaceContainerHigh),
        surfaceContainerHighest = c(s.surfaceContainerHighest),
    )
}

/**
 * Pure black: the screen background becomes #000000. Rows and cards keep their tinted container
 * colours, so they still stand out from the background.
 */
private fun ColorScheme.pureBlack(): ColorScheme =
    copy(
        background = Color.Black,
        surface = Color.Black,
        surfaceDim = Color.Black,
        surfaceContainerLowest = Color.Black,
    )
