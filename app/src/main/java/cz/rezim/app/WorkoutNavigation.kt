package cz.rezim.app

internal sealed interface WorkoutScrollTarget {
    data class NextSet(val exerciseKey: String, val setId: Long) : WorkoutScrollTarget
    data object SaveWorkout : WorkoutScrollTarget
}

// Plan order is also the order within each round. Legacy pairs remain readable.
internal fun supersetMembers(exercises: List<Exercise>, key: String): List<Exercise> {
    val current = exercises.firstOrNull { it.key == key } ?: return emptyList()
    current.supersetId?.let { id -> return exercises.filter { it.supersetId == id } }
    val partner = exercises.firstOrNull { it.key == current.partnerKey && it.key != key }
    return if (partner == null) listOf(current)
        else exercises.filter { it.key == key || it.key == partner.key }
}

internal fun nextWorkoutTarget(
    exercises: List<Exercise>, entries: List<SetEntry>, completedSetId: Long
): WorkoutScrollTarget? {
    val completed = entries.firstOrNull { it.id == completedSetId && it.done } ?: return null
    val members = supersetMembers(exercises, completed.exercise)
    if (members.isEmpty()) return null
    val keys = members.map { it.key }.toSet()
    val pending = entries.filter { it.exercise in keys && !it.done }
    if (pending.isNotEmpty()) {
        // Complete the earliest unfinished round; a one-set member appears in round 1.
        val round = pending.minOf { it.number }
        val current = members.indexOfFirst { it.key == completed.exercise }
        val following = members.drop(current + 1) + members.take(current + 1)
        for (exercise in following) pending.firstOrNull { it.exercise == exercise.key && it.number == round }
            ?.let { return WorkoutScrollTarget.NextSet(exercise.key, it.id) }
    }
    val groupStart = exercises.indexOfFirst { it.key in keys }
    for (exercise in exercises.drop(groupStart + 1) + exercises.take(groupStart)) {
        entries.filter { it.exercise == exercise.key && !it.done }.minByOrNull { it.number }
            ?.let { return WorkoutScrollTarget.NextSet(exercise.key, it.id) }
    }
    return WorkoutScrollTarget.SaveWorkout
}

internal enum class RestStartMode { MANUAL, AFTER_SET, AFTER_ROUND }

internal fun shouldStartRest(
    mode: RestStartMode, exercises: List<Exercise>, entries: List<SetEntry>, completedSetId: Long
): Boolean {
    val completed = entries.firstOrNull { it.id == completedSetId && it.done } ?: return false
    if (mode == RestStartMode.MANUAL || nextWorkoutTarget(exercises, entries, completedSetId) == WorkoutScrollTarget.SaveWorkout)
        return false
    if (mode == RestStartMode.AFTER_SET) return true
    val members = supersetMembers(exercises, completed.exercise).map { it.key }.toSet()
    if (members.isEmpty()) return false
    return entries.none { it.exercise in members && !it.done && it.number <= completed.number }
}
