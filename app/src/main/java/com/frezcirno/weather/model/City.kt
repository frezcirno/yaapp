package com.frezcirno.weather.model

import java.io.Serializable

class City : Serializable {
    val id = 0
    val name = ""
    val coord: Coordinates = Coordinates()
    val country = ""
    val population = 0
    val timezone = 0
}