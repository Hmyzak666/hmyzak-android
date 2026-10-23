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

// 0 follows the final rounds as sets are added/removed; positive values select a first round.
// Actual workout sets determine the available rounds during training, not the template counts.
internal fun supersetRoundStarts(members: List<Exercise>, entries: List<SetEntry>? = null): Map<String,Int> {
    val counts = members.associate { exercise -> exercise.key to
        (entries?.filter { it.exercise == exercise.key }?.maxOfOrNull { it.number }
            ?: exercise.sets).coerceAtLeast(1) }
    val rounds = counts.values.maxOrNull() ?: 1
    return members.associate { exercise ->
        val latest = rounds - counts.getValue(exercise.key) + 1
        exercise.key to if(exercise.supersetStartRound == 0) latest
            else exercise.supersetStartRound.coerceIn(1,latest)
    }
}

internal fun supersetRoundLabel(exercise: Exercise, members: List<Exercise>, entries: List<SetEntry>? = null): String {
    val start = supersetRoundStarts(members,entries)[exercise.key] ?: 1
    val count = entries?.filter { it.exercise == exercise.key }?.maxOfOrNull { it.number } ?: exercise.sets
    return if(count <= 1) "Kolo $start" else "Kola $start–${start + count - 1}"
}

internal fun nextWorkoutTarget(
    exercises: List<Exercise>, entries: List<SetEntry>, completedSetId: Long
): WorkoutScrollTarget? {
    val completed = entries.firstOrNull { it.id == completedSetId && it.done } ?: return null
    val members = supersetMembers(exercises, completed.exercise)
    if (members.isEmpty()) return null
    val keys = members.map { it.key }.toSet()
    val starts = supersetRoundStarts(members,entries)
    fun round(entry:SetEntry) = entry.number + starts.getValue(entry.exercise) - 1
    val pending = entries.filter { it.exercise in keys && !it.done }
    if (pending.isNotEmpty()) {
        val nextRound = pending.minOf { round(it) }
        val current = members.indexOfFirst { it.key == completed.exercise }
        val following = members.drop(current + 1) + members.take(current + 1)
        for (exercise in following) pending.firstOrNull { it.exercise == exercise.key && round(it) == nextRound }
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
    val members = supersetMembers(exercises, completed.exercise)
    if (members.isEmpty()) return false
    val starts = supersetRoundStarts(members,entries)
    val completedRound = completed.number + starts.getValue(completed.exercise) - 1
    return entries.none { it.exercise in starts && !it.done &&
        it.number + starts.getValue(it.exercise) - 1 <= completedRound }
}
