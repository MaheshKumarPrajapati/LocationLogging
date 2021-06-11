package com.example.locationdemo.geocoordinateconverter

import java.util.*


internal open class LatLon2UTM {
    private val a: Double
    private val e: Double
    private val esq: Double
    private val e0sq: Double
    val digraphArrayN: CharArray
    var longitudeZoneValue = 0
    var latitudeZoneValue = 0
    var eastingValue = 0.0
    var northingValue = 0.0
    fun convertLatLonToUTM(latitude: Double, longitude: Double): String {
        verifyLatLon(latitude, longitude)
        convert(latitude, longitude)
        // (int) Math.round( northingValue )
        return String.format(Locale.getDefault(), "UTM Zone- %d%c\nEasting- %.04f\nNorthing- %.04f",
                longitudeZoneValue,
                digraphArrayN[latitudeZoneValue],
                eastingValue,
                northingValue)
    }

    fun convert(latitude: Double, longitude: Double) {
        val latRad = latitude * Math.PI / 180.0
        val utmz = 1 + Math.floor((longitude + 180) / 6) // utm zone
        val zcm = 3 + 6 * (utmz - 1) - 180 // central meridian of a zone
        var latz = 0.0 // zone A-B for below 80S

        // convert latitude to latitude zone
        if (latitude > -80 && latitude < 72) {
            latz = Math.floor((latitude + 80) / 8) + 2 // zones C-W
        } else {
            if (latitude > 72 && latitude < 84) {
                latz = 21.0 // zone X
            } else {
                if (latitude > 84) {
                    latz = 23.0 // zones Y-Z
                }
            }
        }
        val N = a / Math.sqrt(1 - Math.pow(e * Math.sin(latRad), 2.0))
        val T = Math.pow(Math.tan(latRad), 2.0)
        val C = e0sq * Math.pow(Math.cos(latRad), 2.0)
        val A = (longitude - zcm) * Math.PI / 180.0 * Math.cos(latRad)

        // calculate M (USGS style)
        var M = latRad * (1.0 - esq * (1.0 / 4.0 + esq * (3.0 / 64.0 + 5.0 * esq / 256.0)))
        M = M - Math.sin(2.0 * latRad) * (esq * (3.0 / 8.0 + esq * (3.0 / 32.0 + 45.0 * esq / 1024.0)))
        M = M + Math.sin(4.0 * latRad) * (esq * esq * (15.0 / 256.0 + esq * 45.0 / 1024.0))
        M = M - Math.sin(6.0 * latRad) * (esq * esq * esq * (35.0 / 3072.0))
        M = M * a //Arc length along standard meridian

        // calculate easting
        val k0 = 0.9996
        var x = k0 * N * A * (1.0 + A * A * ((1.0 - T + C) / 6.0 + A * A * (5.0 - 18.0 * T + T * T + 72.0 * C - 58.0 * e0sq) / 120.0)) //Easting relative to CM
        x = x + 500000 // standard easting

        // calculate northing
        var y = k0 * (M + N * Math.tan(latRad) * (A * A * (1.0 / 2.0 + A * A * ((5.0 - T + 9.0 * C + 4.0 * C * C) / 24.0 + A * A * (61.0 - 58.0 * T + T * T + 600.0 * C - 330.0 * e0sq) / 720.0)))) // from the equator
        if (y < 0) {
            y = 10000000 + y // add in false northing if south of the equator
        }
        longitudeZoneValue = utmz.toInt()
        latitudeZoneValue = latz.toInt()
        eastingValue = x
        northingValue = y
    }

    fun verifyLatLon(latitude: Double, longitude: Double) {
        require(!(latitude < -90.0 || latitude > 90.0 || longitude < -180.0 || longitude >= 180.0)) { "Legal ranges: latitude [-90,90], longitude [-180,180)." }
    }

    fun convertLatLonToUTMForCSV(latitude: Double, longitude: Double): String {
        verifyLatLon(latitude, longitude)
        convert(latitude, longitude)
        // (int) Math.round( northingValue )
        return String.format(Locale.getDefault(), "%d%c,%.04f,%.04f",
                longitudeZoneValue,
                digraphArrayN[latitudeZoneValue],
                eastingValue,
                northingValue)
    }

    init {
        val equatorialRadius = 6378137.0
        val flattening = 298.2572236
        a = equatorialRadius
        val f = 1 / flattening
        val b = a * (1 - f) // polar radius
        e = Math.sqrt(1 - Math.pow(b, 2.0) / Math.pow(a, 2.0))
        esq = 1 - b / a * (b / a)
        e0sq = e * e / (1 - Math.pow(e, 2.0))
        digraphArrayN = charArrayOf(
                'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'J', 'K', 'L', 'M', 'N', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'
        )
    }
}