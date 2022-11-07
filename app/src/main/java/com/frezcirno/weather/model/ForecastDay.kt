package com.frezcirno.weather.model

import java.io.Serializable

class ForecastDay : Serializable {
    val dt = 0
    val sunrise = 0
    val sunset = 0
    val temp = Temp()
    val feels_like = FeelsLike()
    val pressure = 0
    val humidity = 0
    val weather: List<WeatherBrief> = ArrayList()
    val speed = 0.0
    val deg = 0
    val gust = 0.0
    val clouds = 0
    val pop = 0.0
    val rain = 0.0
    val uvi = 0.0
    val snow = 0.0
}