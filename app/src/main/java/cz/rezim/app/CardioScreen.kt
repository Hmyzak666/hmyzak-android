package cz.rezim.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun CardioPicker(selected:String,onSelect:(String)->Unit) {
    val c=LocalContext.current
    var expanded by remember {mutableStateOf(false)}
    Box {
        OutlinedButton(onClick={expanded=true},modifier=Modifier.fillMaxWidth().testTag("cardio-type")) {Text(c.tr(CardioActivity.valueOf(selected).label))}
        DropdownMenu(expanded,onDismissRequest={expanded=false}) {
            CardioActivity.entries.forEach {a->DropdownMenuItem(text={Text(c.tr(a.label))},onClick={expanded=false;onSelect(a.name)})}
        }
    }
}
@Composable
internal fun CardioForm(vm:RezimState,record:RunEntry?,activity:String,onClose:()->Unit) {
    val c=LocalContext.current
    val key=record?.id ?: -vm.runFormRequest.toLong()
    var type by rememberSaveable(key) {mutableStateOf(record?.activity ?: activity)}
    var custom by rememberSaveable(key) {mutableStateOf(record?.customName.orEmpty())}
    var date by rememberSaveable(key) {mutableStateOf(record?.date ?: currentDate())}
    var km by rememberSaveable(key) {mutableStateOf(record?.km?.takeIf {it>0}?.let(::fmtNumber).orEmpty())}
    var time by rememberSaveable(key) {mutableStateOf(durationText(record?.seconds ?: 0))}
    var avg by rememberSaveable(key) {mutableStateOf(record?.avg?.toString().orEmpty())}
    var max by rememberSaveable(key) {mutableStateOf(record?.max?.toString().orEmpty())}
    var resistance by rememberSaveable(key) {mutableStateOf(record?.resistance?.let(::fmtNumber).orEmpty())}
    var incline by rememberSaveable(key) {mutableStateOf(record?.incline?.let(::fmtNumber).orEmpty())}
    var note by rememberSaveable(key) {mutableStateOf(record?.note.orEmpty())}
    Column(Modifier.testTag("cardio-form"),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        CardioPicker(type) {type=it}
        if(type=="OTHER") DataInput(c.tr(R.string.cardio_custom_name),custom,{custom=it},Modifier.fillMaxWidth())
        DataInput(c.tr(R.string.ui_datum_rrrr_mm_dd_2ac3f),date,{date=it},Modifier.fillMaxWidth())
        DurationInput(c.tr(R.string.time_duration),time,{time=it},tag="cardio-time")
        DataInput(c.tr(R.string.ui_vzdalenost_km_nepovinne_f2c65),km,{km=it},Modifier.fillMaxWidth(),KeyboardType.Decimal)
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            DataInput(c.tr(R.string.ui_prumerny_tep_8efa6),avg,{avg=it},Modifier.weight(1f),KeyboardType.Number)
            DataInput(c.tr(R.string.ui_max_tep_21910),max,{max=it},Modifier.weight(1f),KeyboardType.Number)
        }
        if(type in setOf("ELLIPTICAL","STATIONARY_BIKE","ROWING","OTHER"))
            DataInput(c.tr(R.string.cardio_resistance),resistance,{resistance=it},Modifier.fillMaxWidth(),KeyboardType.Decimal)
        if(type in setOf("TREADMILL","WALKING","RUNNING","OTHER"))
            DataInput(c.tr(R.string.cardio_incline),incline,{incline=it},Modifier.fillMaxWidth(),KeyboardType.Decimal)
        NotesInput(c.tr(R.string.ui_volna_poznamka_c1cc9),note,{note=it})
        MainButton(c.tr(R.string.ui_ulozit_85e82),Modifier.fillMaxWidth().testTag("save-cardio"),RunningBlue) {
            try {
                fun optionalNumber(raw:String):Double? = if(raw.isBlank()) null else requireNotNull(raw.asNumber())
                fun optionalInt(raw:String):Int? = if(raw.isBlank()) null else requireNotNull(raw.toIntOrNull())
                val entry=RunEntry(record?.id ?: 0,date,optionalNumber(km) ?: 0.0,requireNotNull(durationSeconds(time)),
                    optionalInt(avg),optionalInt(max),note,type,custom.trim(),optionalNumber(resistance),optionalNumber(incline))
                validateCardio(entry)
                if(record==null) vm.addCardio(entry) else vm.updateRun(entry)
                onClose()
            } catch(_:IllegalArgumentException) {vm.tell(c.tr(R.string.cardio_invalid))}
        }
    }
}
@Composable
fun RunScreen(vm:RezimState,onUpgrade:()->Unit={vm.page=Page.SETTINGS}) {
    val c=LocalContext.current
    var form by rememberSaveable(vm.runFormRequest) {mutableStateOf(vm.runFormRequest>0)}
    var editId by rememberSaveable {mutableStateOf<Long?>(null)}
    LazyColumn(Modifier.testTag("cardio-screen"),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item {
            Text(c.tr(R.string.cardio_title),fontSize=23.sp)
            MainButton(if(form) c.tr(R.string.ui_zavrit_ae288) else c.tr(R.string.cardio_add),color=RunningBlue) {form=!form}
        }
        item {GpsCard(vm)}
        if(form) item {SectionCard {CardioForm(vm,null,vm.cardioActivity) {form=false}}}
        if(vm.runs.size>=2) item {
            var group by rememberSaveable {mutableIntStateOf(0)}
            val chosen=vm.runs.filter {when(group) {1->it.km in 4.5..5.5;2->it.km in 9.0..11.0;else->true}}
            SectionCard {
                MutedHeading(c.tr(R.string.ui_vyvoj_bezeckeho_tempa_min_km_52d11))
                ProFeatureContent(vm,onUpgrade,"running-pace") {
                    Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                        listOf(c.tr(R.string.ui_vse_8807d),"5 km","10 km").forEachIndexed {i,label->FilterChip(selected=group==i,onClick={group=i},label={Text(label)})}
                    }
                    if(chosen.size>=2) SparkLine(chosen.asReversed().map {it.seconds/60.0/it.km},RunningBlue)
                    else Text(c.tr(R.string.ui_pro_tento_rozsah_zatim_nejsou_dva_29045))
                }
            }
        }
        if(vm.runs.count {it.avg!=null}>=2) item {SectionCard {
            MutedHeading(c.tr(R.string.ui_vyvoj_prumerneho_tepu_bpm_2ba32))
            ProFeatureContent(vm,onUpgrade,"running-heart-rate") {SparkLine(vm.runs.asReversed().mapNotNull {it.avg?.toDouble()},RunningBlue)}
        }}
        items(vm.cardio,key={it.id}) {r->SectionCard {
            Text(cardioName(c,r),color=RunningBlue)
            Text(shortDate(r.date)+" · "+durationText(r.seconds))
            if(r.km>0) Text("${fmtDisplay(r.km)} km"+if(r.isRunning) " · "+runningPace(r.seconds,r.km) else "")
            Text(c.tr(R.string.ui_tep_s_max_s_bpm_e66a2,r.avg ?: "–",r.max ?: "–"))
            r.resistance?.let {Text(c.tr(R.string.cardio_resistance)+": "+fmtDisplay(it))}
            r.incline?.let {Text(c.tr(R.string.cardio_incline)+": "+fmtDisplay(it))}
            if(r.note.isNotBlank()) Text(r.note)
            ActivityEnergy(vm,r)
            SavedRoute(r.id)
            TextButton(onClick={editId=r.id}) {Text(c.tr(R.string.ui_opravit_zaznam_932c9))}
        }}
        if(vm.cardio.isEmpty()) item {Text(c.tr(R.string.cardio_empty))}
    }
    vm.cardio.firstOrNull {it.id==editId}?.let {record->
        AlertDialog(onDismissRequest={editId=null},title={Text(c.tr(R.string.cardio_title))},
            text={Column(Modifier.verticalScroll(rememberScrollState())) {CardioForm(vm,record,record.activity) {editId=null}}},
            confirmButton={},dismissButton={TextButton(onClick={editId=null}) {Text(c.tr(R.string.ui_zrusit_9bd5e))}})
    }
}
internal fun runningPace(seconds:Int,km:Double):String {
    if(km<=0) return "–"
    val n=kotlin.math.round(seconds/km).toInt()
    return durationText(n,false)+" /km"
}
