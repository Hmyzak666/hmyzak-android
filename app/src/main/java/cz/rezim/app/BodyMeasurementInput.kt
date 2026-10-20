package cz.rezim.app

internal data class BodyMeasurementInput(val kg: Double, val waist: Double?)

// Empty means unmeasured. A supplied but invalid circumference must not silently disappear.
internal fun parseBodyMeasurement(weightText: String, waistText: String): BodyMeasurementInput? {
    val kg = weightText.asNumber()?.takeIf { it.isFinite() && it > 0.0 && it <= 400.0 } ?: return null
    val waist = if (waistText.isBlank()) null else {
        waistText.asNumber()?.takeIf { it.isFinite() && it > 0.0 && it <= 300.0 } ?: return null
    }
    return BodyMeasurementInput(kg, waist)
}
