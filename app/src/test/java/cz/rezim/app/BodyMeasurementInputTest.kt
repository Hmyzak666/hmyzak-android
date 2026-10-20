package cz.rezim.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BodyMeasurementInputTest {
    @Test fun dailyWeightWithoutCircumference() {
        assertEquals(BodyMeasurementInput(83.1, null), parseBodyMeasurement("83,1", ""))
        assertEquals(BodyMeasurementInput(83.1, null), parseBodyMeasurement(" 83.1 ", "  "))
    }

    @Test fun measuredCircumferenceRemainsAvailable() {
        assertEquals(BodyMeasurementInput(83.1, 90.5), parseBodyMeasurement("83,1", "90,5"))
    }

    @Test fun invalidSuppliedCircumferenceIsRejectedRatherThanLost() {
        for (waist in listOf("0", "-1", "301", "abc", "NaN", "Infinity")) {
            assertNull(waist, parseBodyMeasurement("83.1", waist))
        }
    }

    @Test fun weightIsStillRequiredAndFinite() {
        for (kg in listOf("", " ", "0", "-1", "401", "abc", "NaN", "Infinity")) {
            assertNull(kg, parseBodyMeasurement(kg, ""))
        }
    }

    @Test fun acceptsUpperBoundsAndSmallPositiveValues() {
        assertEquals(BodyMeasurementInput(400.0, 300.0), parseBodyMeasurement("400", "300"))
        assertEquals(BodyMeasurementInput(0.1, 0.1), parseBodyMeasurement("0.1", "0.1"))
    }
}
