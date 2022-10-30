package com.frezcirno.weather.app

import android.app.Application
import android.content.res.Configuration
import android.os.Build
import androidx.preference.PreferenceManager
import com.frezcirno.weather.R
import com.frezcirno.weather.model.Log.i
import com.frezcirno.weather.preferences.Prefs
import com.frezcirno.weather.utils.LanguageUtil
import java.util.*

class MyApplication : Application() {
    private lateinit var locale: Locale

    override fun onCreate() {
        super.onCreate()
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        val config = baseContext.resources.configuration
        val lang = preferences.getString(getString(R.string.pref_language), "en")!!
        locale = Locale(lang)
        config.setLocale(locale)
        i("Locale", lang)
        Locale.setDefault(locale)
        updateConfiguration(config)
        setSystemLocale(config, locale)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) LanguageUtil.setLanguage(
            this,
            Prefs(this).language
        )
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        setSystemLocale(newConfig, locale)
        Locale.setDefault(locale)
        updateConfiguration(newConfig)
    }

    private fun updateConfiguration(config: Configuration) {
        baseContext.createConfigurationContext(config)
    }

    companion object {
        private fun setSystemLocale(config: Configuration, locale: Locale) {
            config.setLocale(locale)
        }
    }
}