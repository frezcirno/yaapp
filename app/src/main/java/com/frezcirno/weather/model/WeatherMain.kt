package com.frezcirno.weather.model

class WeatherMain {
    val temp = 0.0
    val feels_like = 0.0
    val temp_min = 0.0
    val temp_max = 0.0
    val pressure = 0
    val humidity = 0
    fun getHumidityInt(): Int {
        return humidity.toInt()
    }
}