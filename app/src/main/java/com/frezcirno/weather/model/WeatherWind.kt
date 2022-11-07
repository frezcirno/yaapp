package com.frezcirno.weather.model

import com.google.gson.annotations.SerializedName

class WeatherWind {
    val speed = 0f

    @SerializedName("deg")
    private val direction = 0f

    fun getDirectionInt(): Int {
        return direction.toInt()
    }
}