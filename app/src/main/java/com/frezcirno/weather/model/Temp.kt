package com.frezcirno.weather.model

import java.io.Serializable

class Temp : Serializable {
    val day = 0.0
    val min = 0.0
    val max = 0.0
    val night = 0.0
    val eve = 0.0
    val morn = 0.0

    fun getDayInt(): Int {
        return day.toInt()
    }

    fun getNightInt(): Int {
        return night.toInt()
    }

    fun getMornInt(): Int {
        return morn.toInt()
    }

    fun getEveInt(): Int {
        return eve.toInt()
    }
}