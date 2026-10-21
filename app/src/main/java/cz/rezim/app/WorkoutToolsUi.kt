package cz.rezim.app

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

internal fun parseRestSeconds(raw:String):Int? {
    val parts=raw.trim().split(':')
    val numbers=parts.map { it.toLongOrNull() ?: return null }
    if(numbers.any { it < 0 || it > MAX_REST_SECONDS }) return null
    val seconds=when(numbers.size) {
        1 -> numbers[0]
        2 -> if(numbers[1] < 60) numbers[0]*60+numbers[1] else return null
        else -> return null
    }
    return seconds.takeIf { it in 1..MAX_REST_SECONDS.toLong() }?.toInt()
}

// A fixed display clock also lets previews and UI tests inspect a ticking screen.
// Countdown completion itself always uses the monotonic service clock.
internal data class WorkoutDisplayTime(val wallMillis:Long,val elapsedMillis:Long)
internal val LocalWorkoutDisplayTime=compositionLocalOf<WorkoutDisplayTime?> {null}

@Composable
private fun elapsedTick(monotonic:Boolean=false):Long {
    LocalWorkoutDisplayTime.current?.let {return if(monotonic) it.elapsedMillis else it.wallMillis}
    val lifecycle=LocalLifecycleOwner.current.lifecycle
    fun time() = if(monotonic) SystemClock.elapsedRealtime() else System.currentTimeMillis()
    var now by remember { mutableLongStateOf(time()) }
    DisposableEffect(monotonic,lifecycle) {
        val handler=android.os.Handler(android.os.Looper.getMainLooper())
        val tick=object:Runnable {
            override fun run() {
                now=time()
                if(lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) handler.postDelayed(this,500)
            }
        }
        val observer=LifecycleEventObserver {_,event ->
            when(event) {
                Lifecycle.Event.ON_START -> {handler.removeCallbacks(tick);handler.post(tick)}
                Lifecycle.Event.ON_STOP,Lifecycle.Event.ON_DESTROY -> handler.removeCallbacks(tick)
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {lifecycle.removeObserver(observer);handler.removeCallbacks(tick)}
    }
    return now
}

@Composable
internal fun WorkoutToolbar(vm:RezimState) {
    val session=vm.session ?: return
    val now=elapsedTick()
    var durationDialog by rememberSaveable(session.id) { mutableStateOf(false) }
    var timerDialog by rememberSaveable(session.id) { mutableStateOf(false) }
    val timer=vm.restTimer
    Row(Modifier.fillMaxWidth().padding(horizontal=10.dp),verticalAlignment=Alignment.CenterVertically) {
        TextButton(onClick={durationDialog=true},modifier=Modifier.weight(1f),contentPadding=PaddingValues(4.dp)) {
            Column {
                Text("Trénink · upravit",fontSize=10.sp,color=SecondaryText)
                Text(workoutClock(session.elapsedSeconds(now)),fontWeight=FontWeight.Bold,fontSize=17.sp)
            }
        }
        TextButton(onClick={timerDialog=true},modifier=Modifier.weight(1f),contentPadding=PaddingValues(4.dp)) {
            Column {
                Text("Pauza · nastavit",fontSize=10.sp,color=SecondaryText)
                Text(when(timer.phase) {
                    RestPhase.RUNNING,RestPhase.PAUSED -> workoutClock(restSecondsLeft(timer))
                    RestPhase.FINISHED -> "Hotovo"
                    else -> workoutClock(vm.restSeconds)
                },fontWeight=FontWeight.Bold,fontSize=17.sp)
            }
        }
        Button(onClick={when(timer.phase) {
            RestPhase.RUNNING -> vm.restCommand(RestTimerCommands.PAUSE)
            RestPhase.PAUSED -> vm.restCommand(RestTimerCommands.RESUME)
            else -> vm.startRest()
        }},contentPadding=PaddingValues(horizontal=10.dp)) {
            Text(when(timer.phase) {RestPhase.RUNNING->"Pozastavit";RestPhase.PAUSED->"Pokračovat";else->"Spustit"},fontSize=12.sp)
        }
    }
    if(durationDialog) {
        var minutes by rememberSaveable(session.id) { mutableStateOf((session.elapsedSeconds(now)/60).coerceAtLeast(1).toString()) }
        AlertDialog(onDismissRequest={durationDialog=false},title={Text("Upravit délku tréninku")},
            text={Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
                Text("Zadej dosavadní délku. Od této hodnoty bude čas dál přibývat.",color=SecondaryText)
                DataInput("Dosavadní délka v minutách",minutes,{minutes=it},Modifier.fillMaxWidth(),KeyboardType.Number)
            }},
            confirmButton={TextButton(onClick={if(vm.setWorkoutDuration(session.id,minutes.toIntOrNull() ?: 0)) durationDialog=false}) {Text("Uložit")}},
            dismissButton={TextButton(onClick={durationDialog=false}) {Text("Zrušit")}})
    }
    if(timerDialog) AlertDialog(onDismissRequest={timerDialog=false},title={Text("Časovač pauz")},
        text={Column(Modifier.fillMaxWidth().heightIn(max=440.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            RestTimerControls(vm)
            HorizontalDivider(color=DarkBorder)
            RestTimerPreferences(vm)
            TimerNotificationAccess()
        }},confirmButton={TextButton(onClick={timerDialog=false}) {Text("Hotovo")}})
}

@Composable
internal fun RestTimerControls(vm:RezimState) {
    val now=elapsedTick(monotonic=true)
    val timer=vm.restTimer
    Text(when(timer.phase) {
        RestPhase.RUNNING -> "Zbývá ${workoutClock(restSecondsLeft(timer,now))}"
        RestPhase.PAUSED -> "Pozastaveno · ${workoutClock(restSecondsLeft(timer,now))}"
        RestPhase.FINISHED -> "Pauza skončila. Pokračuj, až budeš připravený."
        RestPhase.IDLE -> "Připraveno · ${workoutClock(vm.restSeconds)}"
    },fontWeight=FontWeight.Bold,color=StrengthGreen)
    if(timer.phase in setOf(RestPhase.RUNNING,RestPhase.PAUSED)) {
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick={vm.restCommand(RestTimerCommands.ADJUST,-30)}) {Text("−30 s")}
            OutlinedButton(onClick={vm.restCommand(RestTimerCommands.ADJUST,30)}) {Text("+30 s")}
        }
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            TextButton(onClick={vm.restCommand(if(timer.phase == RestPhase.RUNNING) RestTimerCommands.PAUSE else RestTimerCommands.RESUME)}) {
                Text(if(timer.phase == RestPhase.RUNNING) "Pozastavit" else "Pokračovat")
            }
            TextButton(onClick=vm::cancelRest) {Text("Zrušit pauzu")}
        }
    }
    MainButton(if(timer.phase in setOf(RestPhase.RUNNING,RestPhase.PAUSED)) "Spustit znovu" else "Spustit pauzu",
        modifier=Modifier.fillMaxWidth()) { vm.startRest() }
}

@Composable
internal fun RestTimerPreferences(vm:RezimState) {
    var text by rememberSaveable(vm.restSeconds) {mutableStateOf(workoutClock(vm.restSeconds))}
    Text("Délka pro další spuštění",fontWeight=FontWeight.SemiBold)
    Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(5.dp)) {
        listOf(30,60,90,120,180).forEach { seconds ->
            FilterChip(selected=vm.restSeconds == seconds,onClick={vm.setRestDuration(seconds)},label={Text(workoutClock(seconds))})
        }
    }
    DataInput("Vlastní délka · m:ss nebo sekundy",text,{text=it},Modifier.fillMaxWidth())
    OutlinedButton(onClick={val value=parseRestSeconds(text)
        if(value == null) vm.tell("Zadej délku od 1 sekundy do 60 minut, např. 1:30 nebo 90.")
        else {vm.setRestDuration(value);vm.tell("Délka příští pauzy uložena.")}
    }) {Text("Uložit délku")}
    Text("Běžící pauzu upravíš pomocí ±30 s. Novou délku můžeš použít tlačítkem Spustit znovu.",fontSize=11.sp,color=SecondaryText)
    Text("Spouštění pauzy",fontWeight=FontWeight.SemiBold)
    RestStartMode.entries.forEach {mode ->
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            RadioButton(selected=vm.restStartMode == mode,onClick={vm.chooseRestStartMode(mode)})
            TextButton(onClick={vm.chooseRestStartMode(mode)}) {Text(when(mode) {
                RestStartMode.MANUAL -> "Ručně"
                RestStartMode.AFTER_SET -> "Po každé sérii"
                RestStartMode.AFTER_ROUND -> "Po celém kole supersérie"
            },fontSize=12.sp)}
        }
    }
    Text("Po poslední sérii celého tréninku se nová pauza nespouští. U samostatného cviku je jedno kolo jedna série.",fontSize=11.sp,color=SecondaryText)
}

@Composable
internal fun TimerNotificationAccess() {
    val context=LocalContext.current
    val manager=context.getSystemService(NotificationManager::class.java)
    var enabled by remember {mutableStateOf(manager.areNotificationsEnabled())}
    val prefs=remember {context.getSharedPreferences("hmyzak_prefs",android.content.Context.MODE_PRIVATE)}
    var asked by remember {mutableStateOf(prefs.getBoolean("timer_notification_asked",false))}
    val launcher=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {enabled=manager.areNotificationsEnabled()}
    val lifecycle=LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer=LifecycleEventObserver {_,event -> if(event==Lifecycle.Event.ON_RESUME) enabled=manager.areNotificationsEnabled()}
        lifecycle.addObserver(observer)
        onDispose {lifecycle.removeObserver(observer)}
    }
    if(!enabled) {
        Text("Povol oznámení pro odpočet na zamčené obrazovce a ovládání z lišty.",fontSize=12.sp,color=SecondaryText)
        TextButton(onClick={
            if(Build.VERSION.SDK_INT >= 33 && !asked && ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                asked=true;prefs.edit {putBoolean("timer_notification_asked",true)}
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,context.packageName))
        }) {Text(if(asked || Build.VERSION.SDK_INT < 33) "Nastavení oznámení" else "Povolit oznámení časovače")}
    }
}

@Composable
internal fun SupersetDialog(vm:RezimState, anchor:Exercise, onDismiss:()->Unit) {
    val exercises=vm.plan(vm.selectedDay).exercises
    var selected by rememberSaveable(anchor.key,stateSaver=listSaver<List<String>,String>(save={it},restore={it})) {
        mutableStateOf(supersetMembers(exercises,anchor.key).map {it.key})
    }
    AlertDialog(onDismissRequest=onDismiss,title={Text("Supersérie · ${anchor.name}")},
        text={Column(Modifier.fillMaxWidth().heightIn(max=440.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(4.dp)) {
            Text("Vyber dva nebo více cviků. Střídají se podle pořadí v plánu. Cvik s menším počtem sérií dokončíš v prvních kolech.",fontSize=12.sp,color=SecondaryText)
            exercises.forEach {exercise ->
                Row(Modifier.fillMaxWidth().toggleable(value=exercise.key in selected,enabled=exercise.key != anchor.key,role=Role.Checkbox,onValueChange={checked ->
                        selected=if(checked) selected+exercise.key else selected-exercise.key
                    }),verticalAlignment=Alignment.CenterVertically) {
                    Checkbox(checked=exercise.key in selected,enabled=exercise.key != anchor.key,onCheckedChange=null)
                    Column(Modifier.weight(1f)) {
                        Text(exercise.name,fontSize=13.sp)
                        Text("${if(vm.session != null) vm.workoutSets.count {it.exercise==exercise.key} else exercise.sets} sérií",fontSize=11.sp,color=SecondaryText)
                    }
                }
            }
            Text("Uloží se pro tento i další tréninky. Odcvičené série zůstanou zachované.",fontSize=11.sp,color=SecondaryText)
            TextButton(onClick={if(vm.updateSuperset(anchor.key,listOf(anchor.key))) onDismiss()}) {Text("Odebrat tento cvik ze supersérie")}
        }},
        confirmButton={TextButton(onClick={if(vm.updateSuperset(anchor.key,exercises.filter {it.key in selected}.map {it.key})) onDismiss()}) {Text("Uložit")}},
        dismissButton={TextButton(onClick=onDismiss) {Text("Zrušit")}})
}

@Composable
internal fun PermanentNoteDialog(vm:RezimState, exercise:Exercise, onDismiss:()->Unit) {
    var note by rememberSaveable(exercise.key) {mutableStateOf(exercise.note)}
    AlertDialog(onDismissRequest=onDismiss,title={Text("Trvalá poznámka")},
        text={Column(Modifier.fillMaxWidth().heightIn(max=440.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Text(exercise.name,fontWeight=FontWeight.SemiBold)
            OutlinedTextField(value=note,onValueChange={note=it.take(4000)},label={Text("Poznámka ke cviku")},minLines=3,maxLines=8,modifier=Modifier.fillMaxWidth())
            Text("Zůstane u cviku i pro další tréninky.",fontSize=12.sp,color=SecondaryText)
        }},confirmButton={TextButton(onClick={if(vm.updatePermanentNote(exercise.key,note)) onDismiss()}) {Text("Uložit")}},
        dismissButton={TextButton(onClick=onDismiss) {Text("Zrušit")}})
}
