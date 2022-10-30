package com.frezcirno.weather.internet

import android.content.Context
import com.frezcirno.weather.model.Log.e
import com.frezcirno.weather.model.WeatherInfo
import com.frezcirno.weather.preferences.Prefs
import com.frezcirno.weather.utils.Constants
import com.frezcirno.weather.utils.Utils
import com.google.gson.GsonBuilder
import java.io.IOException
import java.io.InputStreamReader
import java.io.Reader
import java.net.HttpURLConnection
import java.net.URL

class Request(var context: Context) {
    @Throws(IOException::class)
    private fun gsonWeather(url: URL): WeatherInfo? {
        val connection1 = url.openConnection() as HttpURLConnection
        connection1.addRequestProperty("x-api-key", Prefs(context).weatherKey)
        val content = connection1.inputStream
        try {
            //Read the server response and attempt to parse it as JSON
            val reader: Reader = InputStreamReader(content)
            val gsonBuilder = GsonBuilder()
            gsonBuilder.setDateFormat("M/d/yy hh:mm a")
            val gson = gsonBuilder.create()
            val posts = gson.fromJson(reader, WeatherInfo::class.java)
            println(gson.toJson(posts))
            content.close()
            return posts
        } catch (ex: Exception) {
            e("FetchWeather", "Failed to parse JSON due to: $ex")
        }
        return null
    }

    @Throws(IOException::class)
    fun getItems(city: String?, units: String?): WeatherInfo? {
        return gsonWeather(
            Utils.getWeatherForecastUrl(
                Constants.OPEN_WEATHER_MAP_DAILY_API,
                city,
                units
            )
        )
    }
}