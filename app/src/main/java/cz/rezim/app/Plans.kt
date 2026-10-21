package cz.rezim.app

enum class UnitType { KG, ADDED_KG, REPS }
data class Exercise(val key: String, val name: String, val sets: Int, val type: UnitType,
    val partnerKey: String? = null, val note: String = "", val supersetId: String? = null)
data class DayPlan(val key: String, val short: String, val name: String, val exercises: List<Exercise>)

object Plans {
    val days = listOf(
        DayPlan("mon", "PO", "Upper A", listOf(
            Exercise("barbell_row", "Barbell Row", 4, UnitType.KG),
            Exercise("pullups_mon", "Pull-ups", 4, UnitType.ADDED_KG),
            Exercise("flat_db_bench", "Flat DB Bench Press", 4, UnitType.KG),
            Exercise("pushups_mon", "Push-ups (po benchi)", 1, UnitType.REPS),
            Exercise("ab_wheel_mon", "Ab Wheel", 4, UnitType.REPS),
            Exercise("lateral_mon", "Lateral Raises", 4, UnitType.KG),
            Exercise("skull_mon", "EZ Skull Crushers", 4, UnitType.KG)
        )),
        DayPlan("tue", "ÚT", "Lower", listOf(
            Exercise("front_squat", "Front Squat", 4, UnitType.KG),
            Exercise("rdl", "Romanian Deadlift", 4, UnitType.KG),
            Exercise("hip_thrust", "Hip Thrust", 4, UnitType.KG),
            Exercise("calf_tue", "Calf Raises", 4, UnitType.KG),
            Exercise("leg_curls", "Leg Curls", 4, UnitType.KG),
            Exercise("leg_extensions", "Leg Extensions", 4, UnitType.KG),
            Exercise("hip_abduction", "Banded Hip Abduction", 4, UnitType.REPS)
        )),
        DayPlan("wed", "ST", "Upper B", listOf(
            Exercise("chinups", "Chin-ups", 4, UnitType.ADDED_KG),
            Exercise("ez_curls", "EZ Curls", 4, UnitType.KG),
            Exercise("neutral_db_ohp", "Neutral-grip DB OHP", 4, UnitType.KG),
            Exercise("hanging_leg_raises", "Hanging Leg Raises", 4, UnitType.REPS),
            Exercise("rear_delt", "Chest-supported Rear-delt Flyes", 4, UnitType.KG),
            Exercise("side_bends", "Weighted Side Bends", 4, UnitType.KG)
        )),
        DayPlan("fri", "PÁ", "Upper C", listOf(
            Exercise("yates_row", "Yates Row", 4, UnitType.KG),
            Exercise("pullups_fri", "Pull-ups", 4, UnitType.ADDED_KG),
            Exercise("incline_db_bench", "Incline DB Bench Press", 4, UnitType.KG),
            Exercise("ab_wheel_fri", "Ab Wheel", 4, UnitType.REPS),
            Exercise("lateral_fri", "Lateral Raises", 4, UnitType.KG),
            Exercise("skull_fri", "EZ Skull Crushers", 4, UnitType.KG)
        )),
        DayPlan("sun", "NE", "Mini Lower", listOf(
            Exercise("step_downs", "Step-downs (na nohu)", 2, UnitType.REPS),
            Exercise("tibialis", "Tibialis Raises", 3, UnitType.REPS),
            Exercise("seated_calf", "Seated Calf Raises", 3, UnitType.KG),
            Exercise("finger_extension", "TrainMax Finger Extensors", 1, UnitType.REPS),
            Exercise("wrist_extension_left", "Wrist Extension (levá)", 1, UnitType.KG)
        ))
    )
    fun day(key: String) = days.first { it.key == key }
    fun exercise(day: String, key: String) = day(day).exercises.first { it.key == key }
    // Default supersets stay editable; front squats and RDL remain separate.
    val initialPartners = mapOf(
        "hip_thrust" to "calf_tue", "calf_tue" to "hip_thrust",
        "leg_curls" to "leg_extensions", "leg_extensions" to "leg_curls"
    )
}
