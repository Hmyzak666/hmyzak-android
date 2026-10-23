package cz.rezim.app

internal data class WorkoutTotals(val sets:Int, val reps:Long, val liftedKg:Double)

// External load only: body mass, band resistance and unfinished sets are never invented.
// Multiplier 2 means the recorded load is the weight of one of TWO dumbbells.
internal fun workoutTotals(entries:List<SetEntry>, exercises:List<Exercise>):WorkoutTotals {
    val factors=exercises.associate {it.key to it.loadMultiplier.coerceIn(1,2)}
    val completed=entries.filter {it.done}
    val volume=completed.sumOf { entry ->
        val kg=entry.weight.asNumber()
        val reps=entry.reps.toIntOrNull()
        if(kg==null || !kg.isFinite() || kg<=0 || reps==null || reps<=0) 0.0
        else (kg*reps*(factors[entry.exercise]?:1)).takeIf {it.isFinite()} ?: 0.0
    }
    return WorkoutTotals(completed.size,completed.sumOf {(it.reps.toIntOrNull()?:0).coerceAtLeast(0).toLong()},volume)
}
