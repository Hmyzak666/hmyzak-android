package cz.rezim.app

internal data class BodyMeasurementInput(
    val kg: Double?, val waist: Double?, val extra: Map<String, Double> = emptyMap()
)

// Empty means unmeasured; a supplied invalid value must never silently disappear.
internal fun parseBodyMeasurement(
    weightText: String, waistText: String, extraTexts: Map<String, String> = emptyMap()
): BodyMeasurementInput? {
    val kg = if (weightText.isBlank()) null else {
        weightText.asNumber()?.takeIf { validBodyValue(it, 400.0) } ?: return null
    }
    val waist = if (waistText.isBlank()) null else {
        waistText.asNumber()?.takeIf { validBodyValue(it, 300.0) } ?: return null
    }
    val extra = buildMap {
        for ((id, text) in extraTexts) if (text.isNotBlank()) {
            val value = text.asNumber()?.takeIf { validBodyValue(it, 300.0) } ?: return null
            put(id, value)
        }
    }
    if (kg == null && waist == null && extra.isEmpty()) return null
    return BodyMeasurementInput(kg, waist, extra)
}

internal fun validBodyValue(value: Double, maximum: Double) = value.isFinite() && value > 0.0 && value <= maximum
internal fun cleanBodyMetricName(name: String) = name.trim().replace(Regex("\\s+"), " ")
internal fun bodyMetricNameKey(name: String) = cleanBodyMetricName(name).lowercase(java.util.Locale.ROOT)
internal fun validBodyMetricName(name: String) = cleanBodyMetricName(name).length in 1..40 &&
    // Keep the original validation contract so older backups and user names stay valid.
    bodyMetricNameKey(name) !in setOf("váha", "hmotnost", "břicho", "obvod břicha", "pas")
