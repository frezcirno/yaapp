package com.frezcirno.weather.preferences

import android.app.AlarmManager
import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.frezcirno.weather.utils.Constants

class MyPreference(context: Context) {
    companion object {
        private lateinit var sharedPreferences: SharedPreferences
    }

    init {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    }

    var city: String?
        get() = sharedPreferences.getString(Constants.CITY, null)
        set(city) {
            val prefsEditor = sharedPreferences.edit()
            prefsEditor.putString(Constants.CITY, city)
            prefsEditor.apply()
        }

    fun setLaunched() {
        sharedPreferences.edit().putBoolean(Constants.FIRST, true).apply()
    }
    val launched: Boolean
        get() = sharedPreferences.getBoolean(Constants.FIRST, false)
    val language: String
        get() = sharedPreferences.getString(Constants.PREF_DISPLAY_LANGUAGE, "en")!!
    var lastCity: String
        get() = sharedPreferences.getString(Constants.LASTCITY, null)!!
        set(city) {
            sharedPreferences.edit().putString(Constants.LASTCITY, city).apply()
        }
    var latitude: Float
        get() = sharedPreferences.getFloat(Constants.LATITUDE, 0f)
        set(lat) {
            sharedPreferences.edit().putFloat(Constants.LATITUDE, lat).apply()
        }
    var longitude: Float
        get() = sharedPreferences.getFloat(Constants.LONGITUDE, 0f)
        set(lon) {
            sharedPreferences.edit().putFloat(Constants.LONGITUDE, lon).apply()
        }
    var units: String?
        get() = sharedPreferences.getString(Constants.UNITS, Constants.METRIC)
        set(string) {
            sharedPreferences.edit().putString(Constants.UNITS, string).apply()
        }

    fun setv3TargetShown(bool: Boolean) {
        sharedPreferences.edit().putBoolean(Constants.V3TUTORIAL, bool).apply()
    }

    fun getv3TargetShown(): Boolean {
        return sharedPreferences.getBoolean(Constants.V3TUTORIAL, false)
    }

    var weatherKey: String?
        get() = sharedPreferences.getString(Constants.PREF_OWM_KEY, Constants.OWM_APP_ID)
        set(str) {
            sharedPreferences.edit().putString(Constants.PREF_OWM_KEY, str).apply()
        }
    var temperature: Double
        get() = java.lang.Double.longBitsToDouble(
            sharedPreferences.getLong(
                Constants.LARGE_WIDGET_TEMPERATURE,
                0
            )
        )
        set(temp) {
            sharedPreferences.edit().putLong(
                Constants.LARGE_WIDGET_TEMPERATURE,
                java.lang.Double.doubleToLongBits(temp)
            ).apply()
        }
    var pressure: Double
        get() = java.lang.Double.longBitsToDouble(
            sharedPreferences.getLong(
                Constants.LARGE_WIDGET_PRESSURE,
                0
            )
        )
        set(pressure) {
            sharedPreferences.edit().putLong(
                Constants.LARGE_WIDGET_PRESSURE,
                java.lang.Double.doubleToLongBits(pressure)
            ).apply()
        }
    var humidity: Int
        get() = sharedPreferences.getInt(Constants.LARGE_WIDGET_HUMIDITY, 0)
        set(humidity) {
            sharedPreferences.edit().putInt(Constants.LARGE_WIDGET_HUMIDITY, humidity).apply()
        }
    var speed: Float
        get() = sharedPreferences.getFloat(Constants.LARGE_WIDGET_WIND_SPEED, 0f)
        set(speed) {
            sharedPreferences.edit().putFloat(Constants.LARGE_WIDGET_WIND_SPEED, speed).apply()
        }
    var icon: Int
        get() = sharedPreferences.getInt(Constants.LARGE_WIDGET_ICON, 500)
        set(id) {
            sharedPreferences.edit().putInt(Constants.LARGE_WIDGET_ICON, id).apply()
        }
    var description: String?
        get() = sharedPreferences.getString(Constants.LARGE_WIDGET_DESCRIPTION, "Moderate Rain")
        set(description) {
            sharedPreferences.edit().putString(Constants.LARGE_WIDGET_DESCRIPTION, description)
                .apply()
        }
    var country: String?
        get() = sharedPreferences.getString(Constants.LARGE_WIDGET_COUNTRY, "IN")
        set(country) {
            sharedPreferences.edit().putString(Constants.LARGE_WIDGET_COUNTRY, country).apply()
        }

    val isTimeFormat24Hours: Boolean
        get() = sharedPreferences.getBoolean(Constants.PREF_TIME_FORMAT, false)

}