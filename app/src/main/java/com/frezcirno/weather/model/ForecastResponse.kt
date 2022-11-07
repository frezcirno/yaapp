package com.frezcirno.weather.model

import java.io.Serializable

class ForecastResponse : Serializable {
    val city = City()
    val cod = 0
    val message = 0.0
    val cnt = 0
    val list: List<ForecastDay> = ArrayList()
}