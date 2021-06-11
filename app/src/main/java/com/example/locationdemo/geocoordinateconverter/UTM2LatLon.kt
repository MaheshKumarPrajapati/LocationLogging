package com.example.locationdemo.geocoordinateconverter

internal open class UTM2LatLon {
    var easting = 0.0
    var northing = 0.0
    private var phi1 = 0.0
    private var fact1 = 0.0
    private var fact2 = 0.0
    private var fact3 = 0.0
    private var fact4 = 0.0
    private var _a3 = 0.0
    private fun getHemisphere(latZone: String): String {
        var hemisphere = "N"
        val southernHemisphere = "ACDEFGHJKLM"
        if (southernHemisphere.contains(latZone)) {
            hemisphere = "S"
        }
        return hemisphere
    }

    fun convertUTMToLatLong(utmString: String): DoubleArray {
        val latlon = doubleArrayOf(0.0, 0.0)
        val latZone: String
        val zone: Int
        val utm = utmString.split(" ").toTypedArray()
        if (utm.size == 4) {
            zone = utm[0].toInt()
            latZone = utm[1]
            easting = utm[2].toDouble()
            northing = utm[3].toDouble()
        } else {
            if (utm.size == 3) {
                val utmZoneCharIdx = if (Character.isDigit(utmString[1])) 2 else 1
                zone = utm[0].substring(0, utmZoneCharIdx).toInt()
                latZone = utm[0].substring(utmZoneCharIdx)
                easting = utm[1].toDouble()
                northing = utm[2].toDouble()
            } else {
                throw IllegalArgumentException("malformed UTM string")
            }
        }
        val hemisphere = getHemisphere(latZone)
        if (hemisphere == "S") {
            northing = 10000000 - northing
        }
        setVariables()
        var latitude = 180 * (phi1 - fact1 * (fact2 + fact3 + fact4)) / Math.PI
        val zoneCM: Double
        zoneCM = if (zone > 0) {
            6 * zone - 183.0
        } else {
            3.0
        }
        val longitude = zoneCM - _a3
        if (hemisphere == "S") {
            latitude = -latitude
        }
        latlon[0] = latitude
        latlon[1] = longitude
        return latlon
    }

    private fun setVariables() {
        val a = 6378137.0
        val e = 0.081819191
        val e1sq = 0.006739497
        val k0 = 0.9996
        val arc = northing / k0
        val mu = (arc
                / (a * (1 - Math.pow(e, 2.0) / 4.0 - 3 * Math.pow(e, 4.0) / 64.0 - 5 * Math.pow(e, 6.0) / 256.0)))
        val ei = ((1 - Math.pow(1 - e * e, 1 / 2.0))
                / (1 + Math.pow(1 - e * e, 1 / 2.0)))
        val ca = 3 * ei / 2 - 27 * Math.pow(ei, 3.0) / 32.0
        val cb = 21 * Math.pow(ei, 2.0) / 16 - 55 * Math.pow(ei, 4.0) / 32
        val cc = 151 * Math.pow(ei, 3.0) / 96
        val cd = 1097 * Math.pow(ei, 4.0) / 512
        phi1 = mu + ca * Math.sin(2 * mu) + cb * Math.sin(4 * mu) + cc * Math.sin(6 * mu) + (cd
                * Math.sin(8 * mu))
        val n0 = a / Math.pow(1 - Math.pow(e * Math.sin(phi1), 2.0), 1 / 2.0)
        val r0 = a * (1 - e * e) / Math.pow(1 - Math.pow(e * Math.sin(phi1), 2.0), 3 / 2.0)
        fact1 = n0 * Math.tan(phi1) / r0
        val _a1 = 500000 - easting
        val dd0 = _a1 / (n0 * k0)
        fact2 = dd0 * dd0 / 2
        val t0 = Math.pow(Math.tan(phi1), 2.0)
        val Q0 = e1sq * Math.pow(Math.cos(phi1), 2.0)
        fact3 = ((5 + 3 * t0 + 10 * Q0 - 4 * Q0 * Q0 - 9 * e1sq) * Math.pow(dd0, 4.0)
                / 24)
        fact4 = 61 + 90 * t0 + 298 * Q0 + 45 * t0 * t0 - 252 * e1sq - (3 * Q0
                * Q0)* Math.pow(dd0, 6.0) / 720

        //
        val lof1 = _a1 / (n0 * k0)
        val lof2 = (1 + 2 * t0 + Q0) * Math.pow(dd0, 3.0) / 6.0
        val lof3 = 5 - 2 * Q0 + 28 * t0 - 3 * Math.pow(Q0, 2.0) + 8 * e1sq + 24 * Math.pow(t0, 2.0)* Math.pow(dd0, 5.0) / 120
        val _a2 = (lof1 - lof2 + lof3) / Math.cos(phi1)
        _a3 = _a2 * 180 / Math.PI
    }
}
