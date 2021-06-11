package com.example.locationdemo.geocoordinateconverter

import java.util.*


internal class LatLon2MGRS : LatLon2UTM() {
    private val digraphArrayE: CharArray
    fun convertLatLonToMGRS(latitude: Double, longitude: Double): String {
        verifyLatLon(latitude, longitude)
        convert(latitude, longitude)
        val digraph = calcDigraph()
        val eastingStr = formatIngValue(eastingValue)
        val northingStr = formatIngValue(northingValue)
        return String.format(Locale.getDefault(), "%d%c%s %s %s",
                longitudeZoneValue,
                digraphArrayN[latitudeZoneValue],
                digraph,
                eastingStr,
                northingStr)
    }

    private fun calcDigraph(): String {
        var letter = Math.floor((longitudeZoneValue - 1) * 8 + eastingValue / 100000.0).toInt()
        var letterIdx = (letter % 24 + 23) % 24
        val digraph = digraphArrayE[letterIdx]
        letter = Math.floor(northingValue / 100000.0).toInt()
        if (longitudeZoneValue / 2.0 == Math.floor(longitudeZoneValue / 2.0)) {
            letter = letter + 5
        }
        letterIdx = letter - (20 * Math.floor(letter / 20.0)).toInt()
        return String.format("%c%c", digraph, digraphArrayN[letterIdx])
    }

    private fun formatIngValue(value: Double): String {
        var str = String.format(Locale.getDefault(), "%d", Math.round(value - 100000 * Math.floor(value / 100000)).toInt())
        if (str.length < 5) {
            str = String.format("00000%s", str)
        }
        return str.substring(str.length - 5)
    }

    init {
        digraphArrayE = charArrayOf(
                'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'J', 'K', 'L', 'M', 'N', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'
        )
    }
}
