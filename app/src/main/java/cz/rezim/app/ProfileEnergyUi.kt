package cz.rezim.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.Instant
import java.time.ZoneId
import java.time.DayOfWeek
import kotlin.math.roundToInt

@Composable internal fun ProfileCard(vm:RezimState) {
    val c=LocalContext.current
    var year by rememberSaveable(vm.profile) {mutableStateOf(vm.profile.birthYear?.toString().orEmpty())}
    var height by rememberSaveable(vm.profile) {mutableStateOf(vm.profile.heightCm?.let(::fmtNumber).orEmpty())}
    var sex by rememberSaveable(vm.profile) {mutableStateOf(vm.profile.sex)}
    SectionCard {
        Text(c.tr(R.string.profile_title))
        DataInput(c.tr(R.string.profile_year),year,{year=it},Modifier.fillMaxWidth().testTag("profile-year"),KeyboardType.Number)
        DataInput(c.tr(R.string.profile_height),height,{height=it},Modifier.fillMaxWidth(),KeyboardType.Decimal)
        var expanded by remember {mutableStateOf(false)}
        val labels=mapOf("" to R.string.profile_unspecified,"FEMALE" to R.string.profile_female,"MALE" to R.string.profile_male)
        Box {
            OutlinedButton(onClick={expanded=true}) {Text(c.tr(R.string.profile_sex)+": "+c.tr(labels.getValue(sex)))}
            DropdownMenu(expanded,{expanded=false}) {labels.forEach {(key,label)->DropdownMenuItem(text={Text(c.tr(label))},onClick={sex=key;expanded=false})}}
        }
        vm.profile.ageRange()?.let {Text(c.tr(R.string.profile_age,it.first,it.last))}
        val weight=vm.measures.firstOrNull {it.kg!=null && it.date<=currentDate()}
        Text(c.tr(R.string.ui_posledni_hmotnost_d6b32)+": "+(weight?.let {"${fmtDisplay(it.kg!!)} kg · ${it.date}"} ?: "–"))
        Text(c.tr(R.string.profile_note),style=MaterialTheme.typography.bodySmall)
        MainButton(c.tr(R.string.ui_ulozit_85e82),Modifier.testTag("save-profile")) {
            try {
                vm.saveProfile(PersonalProfile(if(year.isBlank()) null else requireNotNull(year.toIntOrNull()),
                    if(height.isBlank()) null else requireNotNull(height.asNumber()),sex))
                vm.tell(c.tr(R.string.profile_saved))
            } catch(_:IllegalArgumentException) {vm.tell(c.tr(R.string.profile_invalid))}
        }
    }
}
internal data class EnergyRow(val key:String,val date:String,val name:String,val activity:String,val seconds:Int,val km:Double?)
internal fun energyRow(record:RunEntry,c:android.content.Context)=EnergyRow("cardio:${record.id}",record.date,cardioName(c,record),record.activity,record.seconds,record.km)
internal fun energyRow(record:HiitEntry)=EnergyRow("hiit:${record.id}",record.date,"HIIT","HIIT",record.totalSeconds ?: record.minutes*60,record.km)
internal fun energyRow(record:Session,c:android.content.Context)=EnergyRow("strength:${record.id}",
    Instant.ofEpochMilli(record.created).atZone(ZoneId.systemDefault()).toLocalDate().toString(),
    c.tr(R.string.ui_sila_c50bc),"STRENGTH",record.elapsedSeconds(),null)
internal fun energyRows(vm:RezimState,c:android.content.Context):List<EnergyRow> = buildList {
    vm.cardio.forEach {add(energyRow(it,c))}
    vm.hiit.forEach {add(energyRow(it))}
    vm.trainingSnapshots.filter {it.session.finished!=null}.forEach {add(energyRow(it.session,c))}
}.sortedByDescending {it.date}
@Composable internal fun ActivityEnergy(vm:RezimState,record:RunEntry)=ActivityEnergy(vm,energyRow(record,LocalContext.current))
@Composable internal fun ActivityEnergy(vm:RezimState,record:HiitEntry)=ActivityEnergy(vm,energyRow(record))
@Composable internal fun ActivityEnergy(vm:RezimState,record:Session)=ActivityEnergy(vm,energyRow(record,LocalContext.current))
@Composable internal fun ActivityEnergy(vm:RezimState,row:EnergyRow) {
    val c=LocalContext.current
    Column(Modifier.testTag("activity-energy:${row.key}")) {
        Text(c.tr(R.string.energy_title),style=MaterialTheme.typography.labelMedium)
        EnergyValues(energyFor(row,vm,vm.energyOptions()[row.key].orEmpty()))
    }
}
internal fun energyFor(row:EnergyRow,vm:RezimState,intensity:String):EnergyEstimate? {
    val kg=vm.measures.firstOrNull {it.kg!=null && it.date<=row.date}?.kg
    return EnergyCalculator.estimate(row.activity,row.seconds,kg,row.km,intensity)
}
internal fun energyRange(lo:Double,hi:Double):String=if(lo.roundToInt()==hi.roundToInt()) "${lo.roundToInt()}" else "${lo.roundToInt()}–${hi.roundToInt()}"
@Composable internal fun EnergyValues(value:EnergyEstimate?) {
    val c=LocalContext.current
    if(value==null) Text(c.tr(R.string.energy_missing)) else {
        Text(c.tr(R.string.energy_active)+": "+energyRange(value.activeLow,value.activeHigh)+" kcal")
        Text(c.tr(R.string.energy_total)+": "+energyRange(value.totalLow,value.totalHigh)+" kcal")
    }
}
@Composable internal fun EnergyOverviewButton(vm:RezimState) {
    val c=LocalContext.current
    var show by rememberSaveable {mutableStateOf(false)}
    OutlinedButton(onClick={show=true},modifier=Modifier.fillMaxWidth().testTag("open-energy")) {Text(c.tr(R.string.energy_title))}
    if(show) androidx.compose.ui.window.Dialog(onDismissRequest={show=false},properties=androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth=false)) {
        Surface(Modifier.fillMaxSize(),color=MaterialTheme.colorScheme.background) {Column {
            TextButton(onClick={show=false}) {Text(c.tr(R.string.ui_zavrit_ae288))}
            EnergyScreen(vm)
        }}
    }
}
@Composable internal fun EnergyScreen(vm:RezimState) {
    val c=LocalContext.current
    var options by remember {mutableStateOf(vm.energyOptions())}
    val rows=energyRows(vm,c)
    val estimates=rows.associate {it.key to energyFor(it,vm,options[it.key].orEmpty())}
    var period by rememberSaveable {mutableIntStateOf(0)}
    val today=LocalDate.now()
    val start=when(period) {1->today.with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));2->today.withDayOfMonth(1);else->today}
    val selected=rows.filter {it.date>=start.toString() && it.date<=today.toString()}
    val values=selected.mapNotNull {estimates[it.key]}
    LazyColumn(Modifier.testTag("energy-screen"),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item {Text(c.tr(R.string.energy_title));Text(c.tr(R.string.energy_method),style=MaterialTheme.typography.bodySmall)}
        item {Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {listOf(R.string.energy_day,R.string.energy_week,R.string.energy_month).forEachIndexed {i,label->FilterChip(period==i,{period=i},label={Text(c.tr(label))})}}}
        item {SectionCard {
            Text(c.tr(R.string.energy_coverage,values.size,selected.size))
            if(values.isEmpty()) Text(c.tr(R.string.energy_missing)) else EnergyValues(EnergyEstimate(values.sumOf {it.totalLow},values.sumOf {it.totalHigh},values.sumOf {it.activeLow},values.sumOf {it.activeHigh},"",0.0,false))
        }}
        items(selected,key={it.key}) {row->SectionCard {
            Text("${row.date} · ${row.name} · ${durationText(row.seconds)}")
            EnergyValues(estimates[row.key])
            estimates[row.key]?.let {Text("${fmtDisplay(it.weightKg)} kg · MET ${it.codes}",style=MaterialTheme.typography.bodySmall)}
            if(row.activity in setOf("ELLIPTICAL","ROWING","HIIT","STRENGTH") || row.activity=="CYCLING" && (row.km ?: 0.0)<=0) {
                var expanded by remember {mutableStateOf(false)}
                val labels=mapOf("" to R.string.energy_unknown,"MODERATE" to R.string.energy_moderate,"VIGOROUS" to R.string.energy_vigorous)
                Box {
                    OutlinedButton(onClick={expanded=true}) {Text(c.tr(R.string.energy_intensity)+": "+c.tr(labels.getValue(options[row.key].orEmpty())))}
                    DropdownMenu(expanded,{expanded=false}) {labels.forEach {(key,label)->DropdownMenuItem(text={Text(c.tr(label))},onClick={vm.setEnergyIntensity(row.key,key);options=vm.energyOptions();expanded=false})}}
                }
            }
        }}
    }
}
