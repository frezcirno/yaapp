package com.frezcirno.weather.model

class DailyResponse {
    val coord = Coordinates()
    val weather: List<WeatherBrief> = ArrayList()
    val base = ""
    val main = WeatherMain()
    val visibility = 0
    val wind = WeatherWind()
    val clouds = WeatherClouds()
    val dt = 0
    val sys = Sys()
    val timezone = 0
    val id = 0
    val name = ""
    val cod = 0
}