package com.example.lovetester.ui.language

import android.content.res.Resources
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.example.lovetester.R

object SupportedLanguages {

    /** Display order of the Language screen. Names are intentionally NOT translated. */
    val all = listOf(
        Language("en", R.string.language_english, R.drawable.flag_english),
        Language("hi", R.string.language_hindi, R.drawable.flag_hindi),
        Language("es", R.string.language_spanish, R.drawable.flag_spanish),
        Language("fr", R.string.language_french, R.drawable.flag_french),
        Language("pt", R.string.language_portuguese, R.drawable.flag_portuguese),
        Language("id", R.string.language_indonesian, R.drawable.flag_indonesian),
        Language("de", R.string.language_german, R.drawable.flag_german)
    )

    val default: Language = all.first()

    /**
     * Finds the supported language for a language code or tag ("es", "es-MX", "in", "id").
     * Java/Android may report Indonesian with the legacy code "in", so it is normalised to "id".
     */
    fun fromTag(tag: String?): Language? {
        val code = tag
            ?.substringBefore('-')
            ?.substringBefore('_')
            ?.lowercase()
            ?.let { if (it == "in") "id" else it }
        return all.firstOrNull { it.id == code }
    }

    /**
     * The language the app is displaying right now.
     * 1. A language chosen in the app (AppCompat per-app locale), if any.
     * 2. Otherwise the device language when the app supports it.
     * 3. Otherwise English (the default resources).
     */
    fun current(): Language {
        val appLocales = AppCompatDelegate.getApplicationLocales()
        if (!appLocales.isEmpty) {
            return fromTag(appLocales[0]?.language) ?: default
        }
        val systemLocale = Resources.getSystem().configuration.locales[0]
        return fromTag(systemLocale?.language) ?: default
    }

    /** Applies the language app-wide. AppCompat stores it and recreates the visible Activities. */
    fun apply(language: Language) {
        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.forLanguageTags(language.id)
        )
    }
}
