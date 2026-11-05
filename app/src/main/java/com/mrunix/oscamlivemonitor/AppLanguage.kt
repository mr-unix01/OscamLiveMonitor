package com.mrunix.oscamlivemonitor

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

internal object AppLanguage {
    const val SYSTEM = "system"
    const val ITALIAN = "it"
    const val ENGLISH = "en-GB"

    private const val PREFERENCES = "oscam_monitor"
    private const val KEY = "lingua_app"

    fun selected(context: Context): String {
        val tag = if (Build.VERSION.SDK_INT >= 33) {
            context.getSystemService(LocaleManager::class.java)
                .applicationLocales
                .get(0)
                ?.toLanguageTag()
                .orEmpty()
        } else {
            context.getSharedPreferences(
                PREFERENCES,
                Context.MODE_PRIVATE
            ).getString(KEY, SYSTEM).orEmpty()
        }

        return when {
            tag == ITALIAN || tag.startsWith("it-") -> ITALIAN
            tag == ENGLISH || tag == "en" ||
                tag.startsWith("en-") -> ENGLISH
            else -> SYSTEM
        }
    }

    fun localizedBaseContext(base: Context): Context {
        if (Build.VERSION.SDK_INT >= 33) return base

        val language = selected(base)
        if (language == SYSTEM) return base

        val locale = Locale.forLanguageTag(language)
        val configuration = Configuration(base.resources.configuration)

        configuration.setLocale(locale)
        configuration.setLayoutDirection(locale)

        return base.createConfigurationContext(configuration)
    }

    fun change(activity: Activity, language: String) {
        require(
            language == SYSTEM ||
                language == ITALIAN ||
                language == ENGLISH
        ) {
            "Unsupported application language"
        }

        if (selected(activity) == language) return

        if (Build.VERSION.SDK_INT >= 33) {
            val locales = if (language == SYSTEM) {
                LocaleList.getEmptyLocaleList()
            } else {
                LocaleList.forLanguageTags(language)
            }

            activity.getSystemService(LocaleManager::class.java)
                .applicationLocales = locales
        } else {
            activity.getSharedPreferences(
                PREFERENCES,
                Context.MODE_PRIVATE
            ).edit()
                .putString(KEY, language)
                .apply()

            activity.recreate()
        }
    }
}
