package cz.rezim.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
internal fun workoutDateText(record:Session):String = Instant.ofEpochMilli(record.finished?:record.created)
    .atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT))

@Composable
internal fun workoutDurationText(record:Session):String {
    val seconds=record.elapsedSeconds()
    return LocalContext.current.tr(R.string.workout_duration_value,seconds/60,seconds%60)
}

@Composable
internal fun WorkoutHistoryRow(vm:RezimState,record:Session) {
    SectionCard(Modifier.testTag("workout-row:${record.id}").clickable {vm.openWorkout(record.id)}) {
        Text(vm.days.firstOrNull{it.key==record.day}?.name?:record.day,fontWeight=FontWeight.SemiBold)
        Text(workoutDateText(record),color=SecondaryText,fontSize=12.sp)
        Text(workoutDurationText(record),color=SecondaryText,fontSize=12.sp)
        ActivityEnergy(vm,record)
    }
}

@Composable
internal fun WorkoutHistoryScreen(vm:RezimState) {
    val context=LocalContext.current
    LazyColumn(Modifier.testTag("completed-history"),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item {Text(context.tr(R.string.workout_history),fontSize=23.sp,fontWeight=FontWeight.Bold)}
        if(vm.completedWorkouts.isEmpty()) item {Text(context.tr(R.string.workout_history_empty),color=SecondaryText)}
        items(vm.completedWorkouts,key={it.id}) {WorkoutHistoryRow(vm,it)}
    }
}

@Composable
internal fun WorkoutDetailScreen(vm:RezimState) {
    val context=LocalContext.current
    val record=vm.completedWorkouts.firstOrNull {it.id==vm.viewingWorkoutId}
    if(record==null) {
        Text(context.tr(R.string.workout_history_empty),Modifier.padding(16.dp));return
    }
    val groups=vm.savedWorkoutSets(record.id).groupBy{it.exercise}.toList()
    LazyColumn(Modifier.testTag("workout-detail"),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item {
            Text(vm.days.firstOrNull{it.key==record.day}?.name?:record.day,fontSize=23.sp,fontWeight=FontWeight.Bold)
            Text(workoutDateText(record),color=SecondaryText)
            Text(workoutDurationText(record),color=StrengthGreen)
            ActivityEnergy(vm,record)
            Text(context.tr(R.string.workout_read_only),fontSize=12.sp,color=SecondaryText)
            OutlinedButton(onClick={vm.beginEditSavedWorkout(record)},modifier=Modifier.testTag("edit-saved-workout")) {
                Text(context.tr(R.string.workout_edit))
            }
        }
        items(groups,key={it.first}) {(key,entries) ->
            SectionCard {
                Text(vm.exerciseName(key),fontWeight=FontWeight.SemiBold)
                entries.sortedBy{it.number}.forEach {entry ->
                    val weight=if(entry.weight.isBlank()) "—" else "${entry.weight} kg"
                    val reps=entry.reps.ifBlank {"—"}
                    Text(context.tr(R.string.workout_set_value,entry.number,weight,reps),Modifier.testTag("saved-set:${entry.id}"))
                    if(!entry.done) Text(context.tr(R.string.workout_set_incomplete),fontSize=11.sp,color=SecondaryText)
                }
            }
        }
    }
}

@Composable
internal fun ExerciseRecordsCard(vm:RezimState,plan:DayPlan) {
    val context=LocalContext.current
    var selected by remember(plan.key,plan.exercises.map{it.key}) {
        mutableStateOf(plan.exercises.first().key)
    }
    var expanded by remember {mutableStateOf(false)}
    SectionCard(Modifier.testTag("personal-records")) {
        MutedHeading(context.tr(R.string.personal_records))
        Box {
            OutlinedButton(onClick={expanded=true}) {Text(vm.exerciseName(selected))}
            DropdownMenu(expanded=expanded,onDismissRequest={expanded=false}) {
                plan.exercises.forEach {exercise ->
                    DropdownMenuItem(text={Text(exercise.name)},onClick={selected=exercise.key;expanded=false})
                }
            }
        }
        Text(vm.exerciseRecordText(selected),fontSize=13.sp,color=StrengthGreen)
        if(vm.isLatestRecord(selected)) Text(context.tr(R.string.ui_novy_osobni_rekord_d21e4),
            color=StrengthGreen,fontWeight=FontWeight.Bold,modifier=Modifier.testTag("new-personal-record"))
    }
}
