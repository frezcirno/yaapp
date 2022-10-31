package com.frezcirno.weather.preferences

import android.app.AlarmManager
import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.frezcirno.weather.model.WeatherInfo
import com.frezcirno.weather.utils.Constants

class Prefs(context: Context?) {
    companion object {
        private lateinit var prefs: SharedPreferences
    }

    init {
        Companion.prefs = PreferenceManager.getDefaultSharedPreferences(
            context!!
        )
    }

    val prefs: SharedPreferences
        get() = Companion.prefs
    var city: String?
        get() = Companion.prefs.getString(Constants.CITY, null)
        set(city) {
            val prefsEditor = Companion.prefs.edit()
            prefsEditor.putString(Constants.CITY, city)
            prefsEditor.apply()
        }

    fun setLaunched() {
        Companion.prefs.edit().putBoolean(Constants.FIRST, true).apply()
    }

    val launched: Boolean
        get() = Companion.prefs.getBoolean(Constants.FIRST, false)
    val time: String
        get() {
            val time = Companion.prefs.getString(
                Constants.PREF_REFRESH_INTERVAL,
                java.lang.Long.toString(AlarmManager.INTERVAL_HOUR / 60000)
            )!!.toLong() * 60000
            return java.lang.Long.toString(time)
        }
    val language: String
        get() = Companion.prefs.getString(Constants.PREF_DISPLAY_LANGUAGE, "en")!!
    var lastCity: String
        get() = Companion.prefs.getString(Constants.LASTCITY, null)!!
        set(city) {
            Companion.prefs.edit().putString(Constants.LASTCITY, city).apply()
        }
    var latitude: Float
        get() = Companion.prefs.getFloat(Constants.LATITUDE, 0f)
        set(lat) {
            Companion.prefs.edit().putFloat(Constants.LATITUDE, lat).apply()
        }
    var longitude: Float
        get() = Companion.prefs.getFloat(Constants.LONGITUDE, 0f)
        set(lon) {
            Companion.prefs.edit().putFloat(Constants.LONGITUDE, lon).apply()
        }
    var units: String?
        get() = Companion.prefs.getString(Constants.UNITS, Constants.METRIC)
        set(string) {
            Companion.prefs.edit().putString(Constants.UNITS, string).apply()
        }
    var notifs: Boolean
        get() = Companion.prefs.getBoolean(Constants.NOTIFICATIONS, false)
        set(bool) {
            Companion.prefs.edit().putBoolean(Constants.NOTIFICATIONS, bool).apply()
        }

    fun setv3TargetShown(bool: Boolean) {
        Companion.prefs.edit().putBoolean(Constants.V3TUTORIAL, bool).apply()
    }

    fun getv3TargetShown(): Boolean {
        return Companion.prefs.getBoolean(Constants.V3TUTORIAL, false)
    }

    var weatherKey: String?
        get() = Companion.prefs.getString(Constants.PREF_OWM_KEY, Constants.OWM_APP_ID)
        set(str) {
            Companion.prefs.edit().putString(Constants.PREF_OWM_KEY, str).apply()
        }
    var temperature: Double
        get() = java.lang.Double.longBitsToDouble(
            Companion.prefs.getLong(
                Constants.LARGE_WIDGET_TEMPERATURE,
                0
            )
        )
        set(temp) {
            Companion.prefs.edit().putLong(
                Constants.LARGE_WIDGET_TEMPERATURE,
                java.lang.Double.doubleToLongBits(temp)
            ).apply()
        }
    var pressure: Double
        get() = java.lang.Double.longBitsToDouble(
            Companion.prefs.getLong(
                Constants.LARGE_WIDGET_PRESSURE,
                0
            )
        )
        set(pressure) {
            Companion.prefs.edit().putLong(
                Constants.LARGE_WIDGET_PRESSURE,
                java.lang.Double.doubleToLongBits(pressure)
            ).apply()
        }
    var humidity: Int
        get() = Companion.prefs.getInt(Constants.LARGE_WIDGET_HUMIDITY, 0)
        set(humidity) {
            Companion.prefs.edit().putInt(Constants.LARGE_WIDGET_HUMIDITY, humidity).apply()
        }
    var speed: Float
        get() = Companion.prefs.getFloat(Constants.LARGE_WIDGET_WIND_SPEED, 0f)
        set(speed) {
            Companion.prefs.edit().putFloat(Constants.LARGE_WIDGET_WIND_SPEED, speed).apply()
        }
    var icon: Int
        get() = Companion.prefs.getInt(Constants.LARGE_WIDGET_ICON, 500)
        set(id) {
            Companion.prefs.edit().putInt(Constants.LARGE_WIDGET_ICON, id).apply()
        }
    var description: String?
        get() = Companion.prefs.getString(Constants.LARGE_WIDGET_DESCRIPTION, "Moderate Rain")
        set(description) {
            Companion.prefs.edit().putString(Constants.LARGE_WIDGET_DESCRIPTION, description)
                .apply()
        }
    var country: String?
        get() = Companion.prefs.getString(Constants.LARGE_WIDGET_COUNTRY, "IN")
        set(country) {
            Companion.prefs.edit().putString(Constants.LARGE_WIDGET_COUNTRY, country).apply()
        }

    fun saveWeather(weather: WeatherInfo) {
        temperature = weather.main.temp
        pressure = weather.main.pressure
        humidity = weather.main.humidity
        speed = weather.wind.speed
        icon = weather.weather[0].id
        country = weather.sys.country
        description = weather.weather[0].description
    }

    val isTimeFormat24Hours: Boolean
        get() = Companion.prefs.getBoolean(Constants.PREF_TIME_FORMAT, false)

}