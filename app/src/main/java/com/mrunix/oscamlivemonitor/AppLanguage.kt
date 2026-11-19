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

    private const val FIRST_LAUNCH_KEY =
        "lingua_primo_avvio_gestita"

    fun initializeFirstLaunch(base: Context) {
        val prefs = base.getSharedPreferences(
            PREFERENCES,
            Context.MODE_PRIVATE
        )

        if (prefs.contains(FIRST_LAUNCH_KEY)) return

        // Solo nuove installazioni senza preferenze precedenti.
        val nuovaInstallazione = prefs.all.isEmpty()

        if (nuovaInstallazione) {
            if (Build.VERSION.SDK_INT >= 33) {
                val manager = base.getSystemService(
                    LocaleManager::class.java
                )

                if (manager.applicationLocales
                        .toLanguageTags().isEmpty()) {
                    manager.applicationLocales =
                        LocaleList.forLanguageTags(ENGLISH)
                }
            } else {
                prefs.edit()
                    .putString(KEY, ENGLISH)
                    .apply()
            }
        }

        prefs.edit()
            .putBoolean(FIRST_LAUNCH_KEY, true)
            .apply()
    }

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
