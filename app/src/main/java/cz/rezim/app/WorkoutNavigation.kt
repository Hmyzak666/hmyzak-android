package cz.rezim.app

internal sealed interface WorkoutScrollTarget {
    data class NextSet(val exerciseKey: String, val setId: Long) : WorkoutScrollTarget
    data object SaveWorkout : WorkoutScrollTarget
}

// Uses the current, already saved completion states; never completes a set itself.
internal fun nextWorkoutTarget(
    exercises: List<Exercise>,
    entries: List<SetEntry>,
    completedSetId: Long
): WorkoutScrollTarget? {
    val completed = entries.firstOrNull { it.id == completedSetId && it.done } ?: return null
    val currentIndex = exercises.indexOfFirst { it.key == completed.exercise }
    if (currentIndex < 0) return null
    val current = exercises[currentIndex]
    val partnerIndex = exercises.indexOfFirst { it.key == current.partnerKey && it.key != current.key }

    fun pending(exercise: Exercise): WorkoutScrollTarget.NextSet? = entries
        .filter { it.exercise == exercise.key && !it.done }
        .minByOrNull { it.number }
        ?.let { WorkoutScrollTarget.NextSet(exercise.key, it.id) }

    // Alternate the pair while both have work left. Unequal set counts are allowed.
    if (partnerIndex >= 0) pending(exercises[partnerIndex])?.let { return it }
    pending(current)?.let { return it }

    // Continue in plan order after this exercise/pair, then revisit earlier skipped work.
    // Starting at the first member also keeps intervening exercises in non-adjacent pairs.
    val groupStart = if (partnerIndex >= 0) minOf(currentIndex, partnerIndex) else currentIndex
    val following = exercises.drop(groupStart + 1) + exercises.take(groupStart)
    for (exercise in following) pending(exercise)?.let { return it }
    return WorkoutScrollTarget.SaveWorkout
}
