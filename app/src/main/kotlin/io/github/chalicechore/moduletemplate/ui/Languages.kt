package io.github.chalicechore.moduletemplate.ui

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

/**
 * TEMPLATE: the app's languages, as BCP 47 tags. Keep in sync with `localeFilters` in
 * app/build.gradle.kts and the res/values-xx folders. The Language row hides itself when there's
 * only one.
 */
val AppLanguages =
    listOf("en", "es", "pt-BR", "fr", "de", "ru", "zh-CN", "zh-TW", "ja", "id", "tr", "vi", "ar")

/** A language's name written in that language, e.g. "Español", "日本語". */
fun languageName(tag: String): String {
    val locale = Locale.forLanguageTag(tag)
    return locale.getDisplayName(locale).replaceFirstChar { it.titlecase(locale) }
}

/** The tag picked in the app, or null for "System default". */
fun currentAppLanguage(): String? {
    val locales = AppCompatDelegate.getApplicationLocales()
    if (locales.isEmpty) return null
    val picked = locales[0] ?: return null
    // Match on language plus script/region where the list has it (zh-CN vs zh-TW).
    return AppLanguages.firstOrNull { Locale.forLanguageTag(it) == picked }
        ?: AppLanguages.firstOrNull { Locale.forLanguageTag(it).language == picked.language }
}

/** Applies [tag] (null = follow the system). The activity is recreated in the new language. */
fun setAppLanguage(tag: String?) {
    AppCompatDelegate.setApplicationLocales(
        if (tag == null) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(tag),
    )
}
