package com.victorious.gps.domain

/** A validated WGS84 coordinate. Construct through [CoordinateValidator]. */
data class Coordinates(val latitude: Double, val longitude: Double)

sealed interface CoordinateResult {
    data class Valid(val coordinates: Coordinates) : CoordinateResult
    data class Invalid(val reason: String) : CoordinateResult
}

object CoordinateValidator {
    const val MIN_LAT = -90.0
    const val MAX_LAT = 90.0
    const val MIN_LNG = -180.0
    const val MAX_LNG = 180.0

    fun isValidLatitude(lat: Double): Boolean = lat.isFinite() && lat in MIN_LAT..MAX_LAT
    fun isValidLongitude(lng: Double): Boolean = lng.isFinite() && lng in MIN_LNG..MAX_LNG

    fun validate(lat: Double, lng: Double): CoordinateResult = when {
        !isValidLatitude(lat) -> CoordinateResult.Invalid("Latitude must be between -90 and 90")
        !isValidLongitude(lng) -> CoordinateResult.Invalid("Longitude must be between -180 and 180")
        else -> CoordinateResult.Valid(Coordinates(lat, lng))
    }

    /** Validates two separate text fields (lat, lng). */
    fun parse(latText: String, lngText: String): CoordinateResult {
        val lat = latText.trim().replace(',', '.').toDoubleOrNull()
            ?: return CoordinateResult.Invalid("Latitude is not a number")
        val lng = lngText.trim().replace(',', '.').toDoubleOrNull()
            ?: return CoordinateResult.Invalid("Longitude is not a number")
        return validate(lat, lng)
    }

    private val pair = Regex("""^\s*\(?\s*(-?\d+(?:\.\d+)?)\s*[,;\s]\s*(-?\d+(?:\.\d+)?)\s*\)?\s*$""")

    /** Parses a single search query such as "40.7128, -74.0060" or "40.7128 -74.0060". */
    fun parseQuery(query: String): CoordinateResult {
        val m = pair.matchEntire(query) ?: return CoordinateResult.Invalid("Enter coordinates as \"lat, lng\"")
        return validate(m.groupValues[1].toDouble(), m.groupValues[2].toDouble())
    }
}
