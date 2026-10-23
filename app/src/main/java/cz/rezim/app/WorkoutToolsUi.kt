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
import androidx.compose.ui.platform.testTag
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
    val context=androidx.compose.ui.platform.LocalContext.current

    val session=vm.session ?: return
    val now=elapsedTick()
    var durationDialog by rememberSaveable(session.id) { mutableStateOf(false) }
    var timerDialog by rememberSaveable(session.id) { mutableStateOf(false) }
    val timer=vm.restTimer
    Row(Modifier.fillMaxWidth().padding(horizontal=10.dp),verticalAlignment=Alignment.CenterVertically) {
        TextButton(onClick={durationDialog=true},modifier=Modifier.weight(1f),contentPadding=PaddingValues(4.dp)) {
            Column {
                Text(context.tr(R.string.ui_trenink_upravit_06a18),fontSize=10.sp,color=SecondaryText)
                Text(workoutClock(session.elapsedSeconds(now)),fontWeight=FontWeight.Bold,fontSize=17.sp)
            }
        }
        TextButton(onClick={timerDialog=true},modifier=Modifier.weight(1f),contentPadding=PaddingValues(4.dp)) {
            Column {
                Text(context.tr(R.string.ui_pauza_nastavit_6f97a),fontSize=10.sp,color=SecondaryText)
                Text(when(timer.phase) {
                    RestPhase.RUNNING,RestPhase.PAUSED -> workoutClock(restSecondsLeft(timer))
                    RestPhase.FINISHED -> context.tr(R.string.ui_hotovo_09526)
                    else -> workoutClock(vm.restSeconds)
                },fontWeight=FontWeight.Bold,fontSize=17.sp)
            }
        }
        Button(onClick={when(timer.phase) {
            RestPhase.RUNNING -> vm.restCommand(RestTimerCommands.PAUSE)
            RestPhase.PAUSED -> vm.restCommand(RestTimerCommands.RESUME)
            else -> vm.startRest()
        }},contentPadding=PaddingValues(horizontal=10.dp)) {
            Text(when(timer.phase) {RestPhase.RUNNING->context.tr(R.string.ui_pozastavit_0761b);RestPhase.PAUSED->context.tr(R.string.ui_pokracovat_659f1);else->context.tr(R.string.ui_spustit_7e4cd)},fontSize=12.sp)
        }
    }
    if(durationDialog) {
        var minutes by rememberSaveable(session.id) { mutableStateOf((session.elapsedSeconds(now)/60).coerceAtLeast(1).toString()) }
        AlertDialog(onDismissRequest={durationDialog=false},title={Text(context.tr(R.string.ui_upravit_delku_treninku_b2f25))},
            text={Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
                Text(context.tr(R.string.ui_zadej_dosavadni_delku_od_teto_hodnoty_84c3d),color=SecondaryText)
                DataInput(context.tr(R.string.ui_dosavadni_delka_v_minutach_52c7c),minutes,{minutes=it},Modifier.fillMaxWidth(),KeyboardType.Number)
            }},
            confirmButton={TextButton(onClick={if(vm.setWorkoutDuration(session.id,minutes.toIntOrNull() ?: 0)) durationDialog=false}) {Text(context.tr(R.string.ui_ulozit_85e82))}},
            dismissButton={TextButton(onClick={durationDialog=false}) {Text(context.tr(R.string.ui_zrusit_9bd5e))}})
    }
    if(timerDialog) AlertDialog(onDismissRequest={timerDialog=false},title={Text(context.tr(R.string.ui_casovac_pauz_7cfc6))},
        text={Column(Modifier.fillMaxWidth().heightIn(max=440.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            RestTimerControls(vm)
            HorizontalDivider(color=DarkBorder)
            RestTimerPreferences(vm)
            TimerNotificationAccess()
        }},confirmButton={TextButton(onClick={timerDialog=false}) {Text(context.tr(R.string.ui_hotovo_09526))}})
}

@Composable
internal fun RestTimerControls(vm:RezimState) {
    val context=androidx.compose.ui.platform.LocalContext.current

    val now=elapsedTick(monotonic=true)
    val timer=vm.restTimer
    Text(when(timer.phase) {
        RestPhase.RUNNING -> context.tr(R.string.ui_zbyva_s_981b4 ,workoutClock(restSecondsLeft(timer,now)))
        RestPhase.PAUSED -> context.tr(R.string.ui_pozastaveno_s_02b9a ,workoutClock(restSecondsLeft(timer,now)))
        RestPhase.FINISHED -> context.tr(R.string.ui_pauza_skoncila_pokracuj_az_budes_pripraveny_4179e)
        RestPhase.IDLE -> context.tr(R.string.ui_pripraveno_s_f8377 ,workoutClock(vm.restSeconds))
    },fontWeight=FontWeight.Bold,color=StrengthGreen)
    if(timer.phase in setOf(RestPhase.RUNNING,RestPhase.PAUSED)) {
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick={vm.restCommand(RestTimerCommands.ADJUST,-30)}) {Text("−30 s")}
            OutlinedButton(onClick={vm.restCommand(RestTimerCommands.ADJUST,30)}) {Text("+30 s")}
        }
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            TextButton(onClick={vm.restCommand(if(timer.phase == RestPhase.RUNNING) RestTimerCommands.PAUSE else RestTimerCommands.RESUME)}) {
                Text(if(timer.phase == RestPhase.RUNNING) context.tr(R.string.ui_pozastavit_0761b) else context.tr(R.string.ui_pokracovat_659f1))
            }
            TextButton(onClick=vm::cancelRest) {Text(context.tr(R.string.ui_zrusit_pauzu_88798))}
        }
    }
    MainButton(if(timer.phase in setOf(RestPhase.RUNNING,RestPhase.PAUSED)) context.tr(R.string.ui_spustit_znovu_a7f9f) else context.tr(R.string.ui_spustit_pauzu_e7bc8),
        modifier=Modifier.fillMaxWidth()) { vm.startRest() }
}

@Composable
internal fun RestTimerPreferences(vm:RezimState) {
    val context=androidx.compose.ui.platform.LocalContext.current

    var text by rememberSaveable(vm.restSeconds) {mutableStateOf(workoutClock(vm.restSeconds))}
    Text(context.tr(R.string.ui_delka_pro_dalsi_spusteni_f1321),fontWeight=FontWeight.SemiBold)
    Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(5.dp)) {
        listOf(30,60,90,120,180).forEach { seconds ->
            FilterChip(selected=vm.restSeconds == seconds,onClick={vm.setRestDuration(seconds)},label={Text(workoutClock(seconds))})
        }
    }
    DataInput(context.tr(R.string.ui_vlastni_delka_m_ss_nebo_sekundy_0c96e),text,{text=it},Modifier.fillMaxWidth())
    OutlinedButton(onClick={val value=parseRestSeconds(text)
        if(value == null) vm.tell(context.tr(R.string.ui_zadej_delku_od_sekundy_do_minut_6a960))
        else {vm.setRestDuration(value);vm.tell(context.tr(R.string.ui_delka_pristi_pauzy_ulozena_2a3c4))}
    }) {Text(context.tr(R.string.ui_ulozit_delku_6ad22))}
    Text(context.tr(R.string.ui_bezici_pauzu_upravis_pomoci_s_novou_aef24),fontSize=11.sp,color=SecondaryText)
    Text(context.tr(R.string.ui_spousteni_pauzy_28331),fontWeight=FontWeight.SemiBold)
    RestStartMode.entries.forEach {mode ->
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            RadioButton(selected=vm.restStartMode == mode,onClick={vm.chooseRestStartMode(mode)})
            TextButton(onClick={vm.chooseRestStartMode(mode)}) {Text(when(mode) {
                RestStartMode.MANUAL -> context.tr(R.string.ui_rucne_4392e)
                RestStartMode.AFTER_SET -> context.tr(R.string.ui_po_kazde_serii_41108)
                RestStartMode.AFTER_ROUND -> context.tr(R.string.ui_po_celem_kole_superserie_42cbe)
            },fontSize=12.sp)}
        }
    }
    Text(context.tr(R.string.ui_po_posledni_serii_celeho_treninku_se_c9ca4),fontSize=11.sp,color=SecondaryText)
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
        Text(context.tr(R.string.ui_povol_oznameni_pro_odpocet_na_zamcene_81abe),fontSize=12.sp,color=SecondaryText)
        TextButton(onClick={
            if(Build.VERSION.SDK_INT >= 33 && !asked && ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                asked=true;prefs.edit {putBoolean("timer_notification_asked",true)}
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,context.packageName))
        }) {Text(if(asked || Build.VERSION.SDK_INT < 33) context.tr(R.string.ui_nastaveni_oznameni_5404e) else context.tr(R.string.ui_povolit_oznameni_casovace_c19ba))}
    }
}

@Composable
internal fun SupersetDialog(vm:RezimState, anchor:Exercise, onDismiss:()->Unit) {
    val context=androidx.compose.ui.platform.LocalContext.current

    val exercises=vm.plan(vm.selectedDay).exercises
    var selected by rememberSaveable(anchor.key,stateSaver=listSaver<List<String>,String>(save={it},restore={it})) {
        mutableStateOf(supersetMembers(exercises,anchor.key).map {it.key})
    }
    var rounds by rememberSaveable(anchor.key,stateSaver=listSaver<List<Int>,Int>(save={it},restore={it})) {
        mutableStateOf(exercises.map {it.supersetStartRound})
    }
    val members=exercises.mapIndexed {index,ex -> ex.copy(supersetStartRound=rounds[index])}.filter {it.key in selected}
    val entries=if(vm.session != null) vm.workoutSets.toList() else null
    fun count(ex:Exercise)=entries?.filter {it.exercise==ex.key}?.maxOfOrNull {it.number} ?: ex.sets
    val totalRounds=members.maxOfOrNull {count(it)} ?: 1
    AlertDialog(onDismissRequest=onDismiss,title={Text(context.tr(R.string.ui_superserie_s_605ea ,anchor.name))},
        text={Column(Modifier.fillMaxWidth().heightIn(max=440.dp).testTag("superset-options").verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(4.dp)) {
            Text(context.tr(R.string.ui_vyber_cviky_a_jejich_kola_poradi_510bc),fontSize=12.sp,color=SecondaryText)
            exercises.forEachIndexed {index,exercise ->
                Row(Modifier.fillMaxWidth().toggleable(value=exercise.key in selected,enabled=exercise.key != anchor.key,role=Role.Checkbox,onValueChange={checked ->
                        selected=if(checked) selected+exercise.key else selected-exercise.key
                    }),verticalAlignment=Alignment.CenterVertically) {
                    Checkbox(checked=exercise.key in selected,enabled=exercise.key != anchor.key,onCheckedChange=null)
                    Column(Modifier.weight(1f)) {
                        Text(exercise.name,fontSize=13.sp)
                        Text(context.tr(R.string.ui_s_serii_f8eea ,if(vm.session != null) vm.workoutSets.count {it.exercise==exercise.key} else exercise.sets),fontSize=11.sp,color=SecondaryText)
                    }
                }
                if(exercise.key in selected && members.size>1) {
                    val configured=members.first {it.key==exercise.key}
                    SupersetRoundPicker(configured,(totalRounds-count(exercise)+1).coerceAtLeast(1),
                        context.supersetRoundText(configured,members,entries)) {value ->
                        rounds=rounds.toMutableList().apply {this[index]=value}
                    }
                }
            }
            Text(context.tr(R.string.ui_vice_serii_stejneho_cviku_navazuje_v_13f10),fontSize=11.sp,color=SecondaryText)
            Text(context.tr(R.string.ui_ulozi_se_pro_tento_i_dalsi_3821c),fontSize=11.sp,color=SecondaryText)
            TextButton(onClick={if(vm.updateSuperset(anchor.key,listOf(anchor.key))) onDismiss()}) {Text(context.tr(R.string.ui_odebrat_tento_cvik_ze_superserie_13ee0))}
        }},
        confirmButton={TextButton(onClick={if(vm.updateSuperset(anchor.key,members.map {it.key},members.associate {it.key to it.supersetStartRound})) onDismiss()}) {Text(context.tr(R.string.ui_ulozit_85e82))}},
        dismissButton={TextButton(onClick=onDismiss) {Text(context.tr(R.string.ui_zrusit_9bd5e))}})
}

@Composable
private fun SupersetRoundPicker(exercise:Exercise,latest:Int,range:String,onChange:(Int)->Unit) {
    val context=androidx.compose.ui.platform.LocalContext.current

    var expanded by remember {mutableStateOf(false)}
    Column(Modifier.fillMaxWidth().padding(start=36.dp,bottom=8.dp)) {
        Text(context.tr(R.string.ui_zaradit_do_superserie_cf0e5),fontSize=11.sp,color=SecondaryText)
        Box {
            OutlinedButton(onClick={expanded=true},modifier=Modifier.testTag("superset-round:${exercise.key}")) {
                Text(when(exercise.supersetStartRound) {
                    0 -> context.tr(R.string.ui_posledni_kola_41da7)
                    1 -> context.tr(R.string.ui_prvni_kola_9d08c)
                    else -> context.tr(R.string.ui_od_s_kola_52aa5 ,exercise.supersetStartRound.coerceIn(1,latest))
                },fontSize=12.sp)
            }
            DropdownMenu(expanded=expanded,onDismissRequest={expanded=false}) {
                DropdownMenuItem(text={Text(context.tr(R.string.ui_prvni_kola_9d08c))},onClick={onChange(1);expanded=false})
                DropdownMenuItem(text={Text(context.tr(R.string.ui_posledni_kola_41da7))},onClick={onChange(0);expanded=false})
                for(round in 2..latest) DropdownMenuItem(text={Text(context.tr(R.string.ui_od_s_kola_52aa5 ,round))},onClick={onChange(round);expanded=false})
            }
        }
        Text(range,fontSize=11.sp,color=StrengthGreen)
    }
}

@Composable
internal fun PermanentNoteDialog(vm:RezimState, exercise:Exercise, onDismiss:()->Unit) {
    val context=androidx.compose.ui.platform.LocalContext.current

    var note by rememberSaveable(exercise.key) {mutableStateOf(exercise.note)}
    AlertDialog(onDismissRequest=onDismiss,title={Text(context.tr(R.string.ui_trvala_poznamka_3a05a))},
        text={Column(Modifier.fillMaxWidth().heightIn(max=440.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Text(exercise.name,fontWeight=FontWeight.SemiBold)
            OutlinedTextField(value=note,onValueChange={note=it.take(4000)},label={Text(context.tr(R.string.ui_poznamka_ke_cviku_85346))},minLines=3,maxLines=8,modifier=Modifier.fillMaxWidth())
            Text(context.tr(R.string.ui_zustane_u_cviku_i_pro_dalsi_a4736),fontSize=12.sp,color=SecondaryText)
        }},confirmButton={TextButton(onClick={if(vm.updatePermanentNote(exercise.key,note)) onDismiss()}) {Text(context.tr(R.string.ui_ulozit_85e82))}},
        dismissButton={TextButton(onClick=onDismiss) {Text(context.tr(R.string.ui_zrusit_9bd5e))}})
}
