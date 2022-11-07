package com.frezcirno.weather.model

class DataResult {
    @JvmField
    var daily = DailyResponse()
    @JvmField
    var fort = ForecastResponse()
}