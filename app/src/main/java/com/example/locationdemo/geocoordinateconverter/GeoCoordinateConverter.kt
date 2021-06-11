package com.example.locationdemo.geocoordinateconverter



class GeoCoordinateConverter {
    fun utm2LatLon(UTM: String?): DoubleArray {
        val c = UTM2LatLon()
        return c.convertUTMToLatLong(UTM!!)
    }

    fun latLon2UTM(latitude: Double, longitude: Double): String {
        val c = LatLon2UTM()
        return c.convertLatLonToUTM(latitude, longitude)
    }

    fun latLon2MGRS(latitude: Double, longitude: Double): String {
        val c = LatLon2MGRS()
        return c.convertLatLonToMGRS(latitude, longitude)
    }

    fun mgrs2LatLon(MGRS: String?): DoubleArray {
        val c = MGRS2LatLon()
        return c.convertMGRSToLatLong(MGRS!!)
    }

    fun degreeToRadian(degree: Double): Double {
        return degree * Math.PI / 180
    }

    fun radianToDegree(radian: Double): Double {
        return radian * 180 / Math.PI
    }

    fun latLon2UTMForCSV(latitude: Double, longitude: Double): String {
        val c = LatLon2UTM()
        return c.convertLatLonToUTMForCSV(latitude, longitude)
    }

    companion object {
        private var sharedConverter: GeoCoordinateConverter? = null
        val instance: GeoCoordinateConverter?
            get() {
                if (sharedConverter == null) {
                    sharedConverter = GeoCoordinateConverter()
                }
                return sharedConverter
            }
    }
}
