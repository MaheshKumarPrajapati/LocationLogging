package com.example.locationdemo.geocoordinateconverter

import android.util.Log
import java.util.*


internal class MGRS2LatLon : UTM2LatLon() {
    fun convertMGRSToLatLong(mgrsString: String): DoubleArray {
        var mgrsString = mgrsString
        mgrsString = mgrsString.replace("\\s+".toRegex(), "").toUpperCase() // remove whitespace
        val length = mgrsString.length
        val utmZoneCharIdx = if (Character.isDigit(mgrsString[1])) 2 else 1
        val utmZoneNumber = mgrsString.substring(0, utmZoneCharIdx).toInt()
        val utmZoneChar = mgrsString[utmZoneCharIdx]
        val eastingID = mgrsString[utmZoneCharIdx + 1]
        val northingID = mgrsString[utmZoneCharIdx + 2]
        val set = get100kSetForZone(utmZoneNumber)
        val east100k = getEastingFromChar(eastingID, set)
        var north100k = getNorthingFromChar(northingID, set).toDouble()

        // We have a bug where the northing may be 2000000 too low.
        // How do we know when to roll over?
        while (north100k < getMinNorthing(utmZoneChar)) {
            north100k += 2000000.0
        }

        // calculate the char index for easting/northing separator
        val i = utmZoneCharIdx + 3
        val remainder = length - i
        if (remainder % 2 != 0) {
            // error
            Log.e("LunaSolCal", "Unexpected remainder ")
        }
        val sep = remainder / 2
        var sepEasting = 0.0
        var sepNorthing = 0.0
        if (sep > 0) {
            val accuracyBonus = 100000f / Math.pow(10.0, sep.toDouble())
            val sepEastingString = mgrsString.substring(i, i + sep)
            sepEasting = sepEastingString.toDouble() * accuracyBonus
            val sepNorthingString = mgrsString.substring(i + sep)
            sepNorthing = sepNorthingString.toDouble() * accuracyBonus
        }
        easting = sepEasting + east100k
        northing = sepNorthing + north100k
        return UTM2LatLon().convertUTMToLatLong(String.format(Locale.getDefault(), "%d %c %d %d",
                utmZoneNumber, utmZoneChar, easting.toInt(), northing.toInt()))
    }

    private fun get100kSetForZone(i: Int): Int {
        var set = i % NUM_100K_SETS
        if (set == 0) {
            set = NUM_100K_SETS
        }
        return set
    }

    private fun getEastingFromChar(e: Char, set: Int): Double {
        val baseCol = SET_ORIGIN_COLUMN_LETTERS
        var curCol = baseCol[set - 1]
        var eastingValue = 100000f
        var rewindMarker = false
        while (curCol != e.toInt()) {
            curCol++
            if (curCol == 'I'.toInt()) {
                curCol++
            }
            if (curCol == 'O'.toInt()) {
                curCol++
            }
            if (curCol > 'Z'.toInt()) {
                if (rewindMarker) {
                    throw NumberFormatException("Bad character: $e")
                }
                curCol = 'A'.toInt()
                rewindMarker = true
            }
            eastingValue += 100000f
        }
        return eastingValue.toDouble()
    }

    private fun getNorthingFromChar(n: Char, set: Int): Float {
        if (n > 'V') {
            throw NumberFormatException("MGRSPoint given invalid Northing $n")
        }
        val baseRow = SET_ORIGIN_ROW_LETTERS
        // rowOrigin is the letter at the origin of the set for the
        // column
        var curRow = baseRow[set - 1]
        var northingValue = 0f
        var rewindMarker = false
        while (curRow != n.toInt()) {
            curRow++
            if (curRow == 'I'.toInt()) {
                curRow++
            }
            if (curRow == 'O'.toInt()) {
                curRow++
            }

            // fixing a bug making whole application hang in this loop
            // when 'n' is a wrong character
            if (curRow > 'V'.toInt()) {
                if (rewindMarker) { // making sure that this loop ends
                    throw NumberFormatException("Bad character: $n")
                }
                curRow = 'A'.toInt()
                rewindMarker = true
            }
            northingValue += 100000f
        }
        return northingValue
    }

    @Throws(NumberFormatException::class)
    private fun getMinNorthing(zoneLetter: Char): Double {
        val northing: Double
        northing = when (zoneLetter) {
            'C' -> 1100000.0
            'D' -> 2000000.0
            'E' -> 2800000.0
            'F' -> 3700000.0
            'G' -> 4600000.0
            'H' -> 5500000.0
            'J' -> 6400000.0
            'K' -> 7300000.0
            'L' -> 8200000.0
            'M' -> 9100000.0
            'N' -> 0.0
            'P' -> 800000.0
            'Q' -> 1700000.0
            'R' -> 2600000.0
            'S' -> 3500000.0
            'T' -> 4400000.0
            'U' -> 5300000.0
            'V' -> 6200000.0
            'W' -> 7000000.0
            'X' -> 7900000.0
            else -> -1.0
        }
        return if (northing >= 0.0) {
            northing
        } else {
            throw NumberFormatException("Invalid zone letter: $zoneLetter")
        }
    }

    companion object {
        private val SET_ORIGIN_COLUMN_LETTERS = intArrayOf('A'.toInt(), 'J'.toInt(), 'S'.toInt(), 'A'.toInt(), 'J'.toInt(), 'S'.toInt())
        private val SET_ORIGIN_ROW_LETTERS = intArrayOf('A'.toInt(), 'F'.toInt(), 'A'.toInt(), 'F'.toInt(), 'A'.toInt(), 'F'.toInt())
        private const val NUM_100K_SETS = 6
    }
}
