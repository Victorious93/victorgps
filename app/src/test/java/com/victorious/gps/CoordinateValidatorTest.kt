package com.victorious.gps

import com.victorious.gps.domain.CoordinateResult
import com.victorious.gps.domain.CoordinateValidator
import com.victorious.gps.domain.Coordinates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CoordinateValidatorTest {
    @Test fun acceptsBoundaries() {
        assertTrue(CoordinateValidator.validate(90.0, 180.0) is CoordinateResult.Valid)
        assertTrue(CoordinateValidator.validate(-90.0, -180.0) is CoordinateResult.Valid)
        assertTrue(CoordinateValidator.validate(0.0, 0.0) is CoordinateResult.Valid)
    }

    @Test fun rejectsOutOfRange() {
        assertTrue(CoordinateValidator.validate(90.0001, 0.0) is CoordinateResult.Invalid)
        assertTrue(CoordinateValidator.validate(-91.0, 0.0) is CoordinateResult.Invalid)
        assertTrue(CoordinateValidator.validate(0.0, 180.5) is CoordinateResult.Invalid)
        assertTrue(CoordinateValidator.validate(0.0, -181.0) is CoordinateResult.Invalid)
    }

    @Test fun rejectsNonFinite() {
        assertFalse(CoordinateValidator.isValidLatitude(Double.NaN))
        assertFalse(CoordinateValidator.isValidLongitude(Double.POSITIVE_INFINITY))
        assertTrue(CoordinateValidator.validate(Double.NaN, 0.0) is CoordinateResult.Invalid)
    }

    @Test fun parsesFields() {
        assertEquals(
            CoordinateResult.Valid(Coordinates(40.7128, -74.006)),
            CoordinateValidator.parse(" 40.7128 ", "-74.006"),
        )
        assertEquals(
            CoordinateResult.Valid(Coordinates(48.8566, 2.3522)),
            CoordinateValidator.parse("48,8566", "2,3522"),
        )
        assertTrue(CoordinateValidator.parse("abc", "1") is CoordinateResult.Invalid)
        assertTrue(CoordinateValidator.parse("", "") is CoordinateResult.Invalid)
    }

    @Test fun parsesQueries() {
        val ny = CoordinateResult.Valid(Coordinates(40.7128, -74.006))
        assertEquals(ny, CoordinateValidator.parseQuery("40.7128, -74.006"))
        assertEquals(ny, CoordinateValidator.parseQuery("40.7128 -74.006"))
        assertEquals(ny, CoordinateValidator.parseQuery("(40.7128,-74.006)"))
        assertEquals(ny, CoordinateValidator.parseQuery("40.7128; -74.006"))
        assertTrue(CoordinateValidator.parseQuery("Times Square") is CoordinateResult.Invalid)
        assertTrue(CoordinateValidator.parseQuery("95, 10") is CoordinateResult.Invalid)
        assertTrue(CoordinateValidator.parseQuery("40.7") is CoordinateResult.Invalid)
    }
}
