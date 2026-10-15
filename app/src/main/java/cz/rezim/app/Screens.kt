package cz.rezim.app

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import kotlinx.coroutines.delay
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.roundToInt

@Composable
internal fun SectionCard(modifier:Modifier=Modifier, content:@Composable ColumnScope.()->Unit) {
    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Panel).padding(14.dp),
        verticalArrangement=Arrangement.spacedBy(9.dp),content=content)
}
@Composable
internal fun MainButton(label:String, color:Color=StrengthGreen, modifier:Modifier=Modifier, action:()->Unit) {
    Button(onClick=action,modifier=modifier,shape=RoundedCornerShape(11.dp),
        colors=ButtonDefaults.buttonColors(containerColor=color,contentColor=PitchBlack)) {
        Text(label,fontWeight=FontWeight.Bold)
    }
}
@Composable
internal fun MutedHeading(text:String) { Text(text.uppercase(),color=SecondaryText,fontSize=11.sp,fontWeight=FontWeight.SemiBold,letterSpacing=1.sp) }
@Composable
internal fun BigMetric(label:String,value:String,color:Color=StrengthGreen,modifier:Modifier=Modifier) {
    SectionCard(modifier) {
        Text(label,color=SecondaryText,fontSize=12.sp)
        Text(value,color=color,fontSize=23.sp,fontWeight=FontWeight.Bold,maxLines=1)
    }
}

private fun minutesAndSeconds(seconds:Int):String = "%d:%02d".format(java.util.Locale.US, seconds/60, seconds%60)
private fun pace(seconds:Int,km:Double):String = if(km<=0) "–" else minutesAndSeconds((seconds/km).roundToInt())+" /km"
private fun shortDate(date:String):String = try {
    val d=LocalDate.parse(date); "${d.dayOfMonth}.${d.monthValue}.${d.year}"
} catch (_:Exception) { date }
private fun parseSeconds(value:String):Int? {
    val parts=value.trim().split(":")
    val nums=parts.map { it.toIntOrNull() ?: return null }
    if(nums.any { it<0 }) return null
    return when(nums.size) {
        2 -> if(nums[1]<60) nums[0]*60+nums[1] else null
        3 -> if(nums[1]<60 && nums[2]<60) nums[0]*3600+nums[1]*60+nums[2] else null
        else -> null
    }
}
private fun validDate(d:String) = try {LocalDate.parse(d);true} catch (_:Exception){false}
private fun optionalHeart(text:String):Int? = if(text.isBlank()) null else text.toIntOrNull()
private fun heartOK(text:String):Boolean = text.isBlank() || (text.toIntOrNull()?.let { it in 30..240 } ?: false)

@Composable
fun HomeScreen(vm:RezimState,onExport:()->Unit,onImport:()->Unit,onShare:()->Unit,onCopy:()->Unit) {
    LazyColumn(contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        item {
            Column(verticalArrangement=Arrangement.spacedBy(4.dp)) {
                Text("Tvůj trénink. Bez rušení.",fontSize=23.sp,fontWeight=FontWeight.Bold)
                Text("Čisté rozhraní, výkony na jednom místě.",fontSize=13.sp,color=SecondaryText)
            }
        }
        item {
            Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                BigMetric("Silové tréninky",vm.totalWorkouts.toString(),StrengthGreen,Modifier.weight(1f))
                BigMetric("Zaznamenané běhy",vm.runs.size.toString(),RunningBlue,Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                BigMetric("HIIT záznamy",vm.hiit.size.toString(),HiitOrange,Modifier.weight(1f))
                BigMetric("Poslední hmotnost",vm.measures.firstOrNull()?.let {fmtNumber(it.kg)+" kg"}?:"–",BodyPurple,Modifier.weight(1f))
            }
        }
        item {
            SectionCard {
                MutedHeading("Rychlé spuštění")
                Text("Otevři libovolný trénink. Všechny cviky jsou rovnou rozbalené.",fontSize=13.sp,color=SecondaryText)
                Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                    vm.days.filter{it.key in listOf("mon","tue","wed","fri")}.forEach { day ->
                        OutlinedButton(onClick={vm.selectDay(day.key);vm.page=Page.STRENGTH},
                            modifier=Modifier.weight(1f),contentPadding=PaddingValues(horizontal=1.dp),
                            shape=RoundedCornerShape(10.dp)) {Text(day.short,fontSize=13.sp)}
                    }
                }
                Row(horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                    vm.days.firstOrNull{it.key=="sun"}?.let { day ->
                        OutlinedButton(onClick={vm.selectDay(day.key);vm.page=Page.STRENGTH},
                            modifier=Modifier.weight(1.5f),contentPadding=PaddingValues(horizontal=4.dp)) {
                            Text("NE mini lower",fontSize=11.sp,maxLines=1)
                        }
                    }
                    OutlinedButton(onClick=vm::startRunForm,modifier=Modifier.weight(1f),contentPadding=PaddingValues(horizontal=3.dp)) { Text("+ Běh",fontSize=12.sp,color=RunningBlue) }
                    OutlinedButton(onClick=vm::startHiitForm,modifier=Modifier.weight(1f),contentPadding=PaddingValues(horizontal=3.dp)) { Text("+ HIIT",fontSize=12.sp,color=HiitOrange) }
                }
                val others=vm.days.filter{it.key !in setOf("mon","tue","wed","fri","sun")}
                if(others.isNotEmpty()) Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                    others.forEach{day->OutlinedButton(onClick={vm.selectDay(day.key);vm.page=Page.STRENGTH}){Text(day.name,maxLines=1)}}
                }
            }
        }
        if(vm.runs.isNotEmpty()) item {
            SectionCard {
                MutedHeading("Poslední běh")
                val last=vm.runs.first()
                Row(horizontalArrangement=Arrangement.SpaceBetween,modifier=Modifier.fillMaxWidth()) {
                    Text(shortDate(last.date),color=SecondaryText,fontSize=12.sp)
                    Text("${fmtNumber(last.km)} km",color=RunningBlue,fontWeight=FontWeight.Bold)
                }
                Row(horizontalArrangement=Arrangement.spacedBy(22.dp)) {
                    Text(pace(last.seconds,last.km),fontWeight=FontWeight.SemiBold)
                    Text("Ø ${last.avg?.toString() ?: "–"} bpm",color=SecondaryText)
                }
            }
        }
        if(vm.recentWorkouts.isNotEmpty()) item {
            SectionCard {
                MutedHeading("Poslední silové tréninky")
                vm.recentWorkouts.take(4).forEach { record ->
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                        Text(vm.days.firstOrNull{it.key==record.day}?.name ?: record.day,fontSize=14.sp)
                        Text(java.time.Instant.ofEpochMilli(record.finished?:record.created)
                            .atZone(java.time.ZoneId.systemDefault()).toLocalDate().toString(),
                            color=SecondaryText,fontSize=12.sp)
                    }
                }
            }
        }
        item {
            SectionCard {
                MutedHeading("Soukromí a zálohy")
                Text("Všechna data jsou uložená pouze v tomto telefonu. Aplikace nemá oprávnění k internetu.",color=SecondaryText,fontSize=13.sp)
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick=onExport,modifier=Modifier.weight(1f),contentPadding=PaddingValues(horizontal=5.dp)) {Text("Záloha JSON",fontSize=12.sp)}
                    OutlinedButton(onClick=onImport,modifier=Modifier.weight(1f),contentPadding=PaddingValues(horizontal=5.dp)) {Text("Obnovit",fontSize=12.sp)}
                }
                OutlinedButton(onClick=onShare,modifier=Modifier.fillMaxWidth()) {
                    Text("Sdílet výkony s ChatGPT",color=StrengthGreen)
                }
                OutlinedButton(onClick=onCopy,modifier=Modifier.fillMaxWidth()) { Text("Kopírovat výpis",color=SecondaryText) }
                Text("Otevře systémové sdílení textového souhrnu. Pokud ChatGPT není v nabídce, použij Kopírovat výpis. Aplikace sama nic neodesílá.",color=SecondaryText,fontSize=11.sp)
                Text("Zálohu ukládej bezpečně: exportovaný JSON není šifrovaný.",color=SecondaryText,fontSize=11.sp)
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
private fun CompactCell(value:String,onValue:(String)->Unit,width:Int,decimal:Boolean=false,enabled:Boolean=true) {
    val requester=remember { BringIntoViewRequester() }
    val scope=rememberCoroutineScope()
    var focused by remember { mutableStateOf(false) }
    val density=LocalDensity.current
    val imeHeight=WindowInsets.ime.getBottom(density)
    LaunchedEffect(focused,imeHeight) {
        if(focused && imeHeight>0) { delay(120);requester.bringIntoView() }
    }
    BasicTextField(
        value=value,onValueChange={ input ->
            val filtered=input.filter { it.isDigit() || (decimal && (it=='.'||it==',')) }
            if(filtered.length<=8) onValue(filtered)
        },enabled=enabled,
        modifier=Modifier.width(width.dp).heightIn(min=44.dp).bringIntoViewRequester(requester)
            .onFocusChanged { focused=it.isFocused; if(focused) scope.launch {delay(180);requester.bringIntoView()} }
            .clip(RoundedCornerShape(8.dp))
            .background(PanelLight).padding(horizontal=4.dp,vertical=10.dp),
        textStyle=TextStyle(color=StrongText,fontSize=14.sp,fontWeight=FontWeight.Medium,textAlign=TextAlign.Center),
        singleLine=true,cursorBrush=SolidColor(StrengthGreen),
        keyboardOptions=KeyboardOptions(keyboardType=if(decimal) KeyboardType.Decimal else KeyboardType.Number)
    )
}

@Composable
private fun ExerciseCard(ex:Exercise,entries:List<SetEntry>,vm:RezimState, onComplete:(SetEntry)->Unit) {
    SectionCard {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text(ex.name,fontWeight=FontWeight.SemiBold,fontSize=15.sp)
                Text(when(ex.type) {
                    UnitType.KG -> "Hmotnost: kg (u jednoruček jedna činka)"
                    UnitType.ADDED_KG -> "Shyby: pouze přidaná zátěž"
                    UnitType.REPS -> "Vlastní váha / opakování"
                },fontSize=11.sp,color=SecondaryText)
            }
            val done=entries.count {it.done}
            Text("$done / ${entries.size}",color=if(done==entries.size) StrengthGreen else SecondaryText,fontSize=12.sp)
        }
        HorizontalDivider(color=DarkBorder)
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(5.dp)) {
            Text("S",Modifier.width(22.dp),fontSize=11.sp,color=SecondaryText,textAlign=TextAlign.Center)
            Text("Minule",Modifier.width(77.dp),fontSize=11.sp,color=SecondaryText,textAlign=TextAlign.Center)
            Text(if(ex.type==UnitType.ADDED_KG) "+kg" else if(ex.type==UnitType.REPS) "–" else "kg",Modifier.width(63.dp),fontSize=11.sp,color=SecondaryText,textAlign=TextAlign.Center)
            Text("Opak.",Modifier.width(63.dp),fontSize=11.sp,color=SecondaryText,textAlign=TextAlign.Center)
            Spacer(Modifier.weight(1f))
            Text("✓",Modifier.width(42.dp),fontSize=11.sp,color=SecondaryText,textAlign=TextAlign.Center)
        }
        entries.forEach { entry ->
            val prev=vm.previous["${ex.key}:${entry.number}"]
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                Text(entry.number.toString(),Modifier.width(22.dp),fontWeight=FontWeight.Bold,fontSize=13.sp,
                    color=if(entry.done)StrengthGreen else SecondaryText,textAlign=TextAlign.Center)
                val old=if(prev!=null) {
                    (if(ex.type==UnitType.REPS) "" else "${prev.weight}×")+prev.reps
                } else "–"
                Text(old,Modifier.width(77.dp),fontSize=12.sp,color=SecondaryText,maxLines=1,textAlign=TextAlign.Center)
                if(ex.type==UnitType.REPS) Box(Modifier.width(63.dp),contentAlignment=Alignment.Center) {
                    Text("—",color=SecondaryText)
                } else CompactCell(entry.weight,onValue={vm.changeSet(entry,weight=it)},width=63,decimal=true)
                CompactCell(entry.reps,onValue={vm.changeSet(entry,reps=it)},width=63)
                Spacer(Modifier.weight(1f))
                Box(Modifier.size(width=42.dp,height=41.dp).clip(RoundedCornerShape(9.dp))
                    .background(if(entry.done) StrengthGreen else PanelLight)
                    .clickable {onComplete(entry)},contentAlignment=Alignment.Center) {
                    Text("✓",fontSize=20.sp,fontWeight=FontWeight.Bold,color=if(entry.done) PitchBlack else SecondaryText)
                }
            }
            if(entry.done && prev!=null && entry.weight==prev.weight && (entry.reps.toIntOrNull()?:0)>(prev.reps.toIntOrNull()?:Int.MAX_VALUE)) {
                Text("+${(entry.reps.toIntOrNull()?:0)-(prev.reps.toIntOrNull()?:0)} opakování oproti minule",
                    color=StrengthGreen,fontSize=11.sp,modifier=Modifier.padding(start=28.dp))
            }
        }
        if(ex.partnerKey!=null) Text("SUPERSET · po ✓ přejdeš na druhý cvik",color=SecondaryText,fontSize=10.sp)
    }
}

@Composable
fun StrengthScreen(vm:RezimState) {
    val listState=rememberLazyListState()
    val scope=rememberCoroutineScope()
    val plan=vm.plan(vm.selectedDay)
    var edit by remember { mutableStateOf(false) }
    var newDay by remember { mutableStateOf(false) }
    var newDayName by remember { mutableStateOf("") }
    val keyboard=LocalSoftwareKeyboardController.current
    val focusManager=androidx.compose.ui.platform.LocalFocusManager.current
    LazyColumn(state=listState,contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item {
            Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
                Text("Silový trénink",fontSize=23.sp,fontWeight=FontWeight.Bold)
                Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    vm.days.forEach {day ->
                        FilterChip(selected=vm.selectedDay==day.key,onClick={vm.selectDay(day.key)},label={Text("${day.short} · ${day.name}")})
                    }
                }
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(9.dp)) {
                    OutlinedButton(onClick={if(vm.session==null) edit=!edit else vm.tell("Nejdřív ulož rozpracovaný trénink.")}) {
                        Text(if(edit) "Hotovo" else "Upravit plán")
                    }
                    OutlinedButton(onClick={newDay=true}) {Text("+ Tréninkový den")}
                }
            }
        }
        if(edit && vm.session==null) item { PlanEditor(vm,plan) }
        item {
            SectionCard {
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                    Column {
                        Text(plan.name,fontSize=18.sp,fontWeight=FontWeight.Bold)
                        Text(if(vm.session==null) "Připraveno k tréninku" else "Právě probíhá",fontSize=12.sp,color=SecondaryText)
                    }
                    if(vm.session==null) MainButton("Zahájit") {vm.start()}
                    else MainButton("Uložit trénink",color=StrengthGreen) {vm.finish()}
                }
                if(vm.session!=null) {
                    val completed=vm.workoutSets.count { it.done }
                    val total=vm.workoutSets.size
                    Text("Hotovo $completed / $total pracovních sérií",fontSize=12.sp,color=SecondaryText)
                    LinearProgressIndicator(progress={ if(total==0) 0f else completed.toFloat()/total },modifier=Modifier.fillMaxWidth(),color=StrengthGreen,trackColor=PanelLight)
                } else Text("Nový trénink předvyplní váhy a opakování podle naposledy dokončených sérií. Při shodném výkonu stačí klepnout na ✓ vpravo.",fontSize=12.sp,color=SecondaryText)
            }
        }
        if(vm.session!=null) {
            itemsIndexed(plan.exercises,key={_,e->e.key}) { i,ex ->
                val entries=vm.workoutSets.filter {it.exercise==ex.key}
                ExerciseCard(ex,entries,vm) { set ->
                    if(vm.toggle(set,ex.type)) {
                        keyboard?.hide();focusManager.clearFocus()
                        val idx=plan.exercises.indexOfFirst {it.key==ex.partnerKey}
                        if(idx>=0) scope.launch {listState.animateScrollToItem(idx+2+(if(edit)1 else 0))}
                    }
                }
            }
        } else {
            items(plan.exercises,key={it.key}) { ex ->
                SectionCard {
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                        Text(ex.name,fontWeight=FontWeight.Medium)
                        Text("${ex.sets} série",fontSize=12.sp,color=SecondaryText)
                    }
                }
            }
        }
        if(vm.session==null && plan.exercises.isNotEmpty()) item {
            var graphEx by remember(vm.selectedDay) {mutableStateOf(plan.exercises.first().key)}
            var weightGraph by remember(vm.selectedDay) {mutableStateOf(plan.exercises.first().type!=UnitType.REPS)}
            SectionCard {
                MutedHeading("Graf síly / opakování")
                var expanded by remember{mutableStateOf(false)}
                Box {
                    OutlinedButton(onClick={expanded=true}) {
                        Text(plan.exercises.firstOrNull{it.key==graphEx}?.name ?: "Vybrat cvik")
                    }
                    DropdownMenu(expanded=expanded,onDismissRequest={expanded=false}) {
                        plan.exercises.forEach {ex ->
                            DropdownMenuItem(text={Text(ex.name)},onClick={graphEx=ex.key;weightGraph=ex.type!=UnitType.REPS;expanded=false})
                        }
                    }
                }
                if(plan.exercises.firstOrNull{it.key==graphEx}?.type != UnitType.REPS) {
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected=weightGraph,onClick={weightGraph=true},label={Text("Max. kg")})
                        FilterChip(selected=!weightGraph,onClick={weightGraph=false},label={Text("Opakování")})
                    }
                }
                val trend=vm.exerciseTrend(graphEx,weightGraph)
                if(trend.size>1) {
                    SparkLine(trend.map{it.second},StrengthGreen)
                    Text("${trend.first().first} → ${trend.last().first} | ${fmtNumber(trend.first().second)} → ${fmtNumber(trend.last().second)} ${if(weightGraph) "kg" else "op."}",color=SecondaryText,fontSize=12.sp)
                    Text(if(weightGraph) "Zobrazuje nejvyšší zadanou váhu v dokončené sérii." else "Zobrazuje součet opakování ve všech dokončených sériích.",color=SecondaryText,fontSize=11.sp)
                } else Text("Graf se objeví po dvou dokončených trénincích tohoto cviku.",color=SecondaryText,fontSize=12.sp)
            }
        }
        item { Spacer(Modifier.height(220.dp)) }
    }
    if(newDay) AlertDialog(
        onDismissRequest={newDay=false}, title={Text("Nový tréninkový den")},
        text={OutlinedTextField(value=newDayName,onValueChange={newDayName=it},label={Text("Název, např. Upper D")})},
        confirmButton={TextButton(onClick={vm.createDay(newDayName);newDayName="";newDay=false;edit=true}){Text("Přidat")}},
        dismissButton={TextButton(onClick={newDay=false}){Text("Zrušit")}}
    )
}

@Composable
private fun PlanEditor(vm:RezimState,plan:DayPlan) {
    var name by remember(plan.key) {mutableStateOf("")}
    var setCount by remember(plan.key){mutableStateOf("4")}
    var unit by remember(plan.key){mutableStateOf(UnitType.KG)}
    var chooseUnit by remember{mutableStateOf(false)}
    var pendingRemoval by remember{mutableStateOf<String?>(null)}
    var deleteDay by remember{mutableStateOf(false)}
    SectionCard {
        MutedHeading("Pořadí, supersérie a nové cviky")
        Text("Změny se použijí při příštím tréninku. Starší uložené výkony zůstávají v historii.",color=SecondaryText,fontSize=11.sp)
        plan.exercises.forEachIndexed { index,ex ->
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                Text(ex.name,Modifier.weight(1f),fontSize=12.sp)
                TextButton(onClick={vm.moveExercise(ex.key,-1)},enabled=index>0,contentPadding=PaddingValues(3.dp)) {Text("↑")}
                TextButton(onClick={vm.moveExercise(ex.key,1)},enabled=index<plan.exercises.lastIndex,contentPadding=PaddingValues(3.dp)) {Text("↓")}
                TextButton(onClick={pendingRemoval=ex.key},contentPadding=PaddingValues(3.dp)) {Text("×",color=HiitOrange)}
            }
            var expanded by remember {mutableStateOf(false)}
            Box {
                OutlinedButton(onClick={expanded=true},contentPadding=PaddingValues(horizontal=10.dp,vertical=3.dp)) {
                    val buddy=plan.exercises.firstOrNull{it.key==ex.partnerKey}?.name
                    Text("Supersérie: ${buddy ?: "žádná"}",fontSize=11.sp)
                }
                DropdownMenu(expanded=expanded,onDismissRequest={expanded=false}) {
                    DropdownMenuItem(text={Text("Bez supersérie")},onClick={vm.updatePair(ex.key,null);expanded=false})
                    plan.exercises.filter{it.key!=ex.key}.forEach{other ->
                        DropdownMenuItem(text={Text(other.name)},onClick={vm.updatePair(ex.key,other.key);expanded=false})
                    }
                }
            }
            HorizontalDivider(color=DarkBorder)
        }
        OutlinedTextField(value=name,onValueChange={name=it},label={Text("Nový cvik")},modifier=Modifier.fillMaxWidth(),singleLine=true)
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.CenterVertically) {
            OutlinedTextField(value=setCount,onValueChange={setCount=it.filter{ch->ch.isDigit()}.take(2)},label={Text("Série")},modifier=Modifier.width(85.dp),singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number))
            Box {
                OutlinedButton(onClick={chooseUnit=true}) {Text(unit.name)}
                DropdownMenu(expanded=chooseUnit,onDismissRequest={chooseUnit=false}) {
                    UnitType.entries.forEach{t-> DropdownMenuItem(text={Text(t.name)},onClick={unit=t;chooseUnit=false})}
                }
            }
            MainButton("Přidat",modifier=Modifier.weight(1f)) {
                vm.addExercise(name,setCount.toIntOrNull()?:0,unit);name=""
            }
        }
        Text("KG = váha; ADDED_KG = přidaná zátěž při shybech; REPS = jen opakování.",color=SecondaryText,fontSize=11.sp)
        TextButton(onClick={deleteDay=true}) {Text("Odstranit tento tréninkový den",color=HiitOrange)}
    }
    if(pendingRemoval!=null) AlertDialog(onDismissRequest={pendingRemoval=null},
        title={Text("Odebrat cvik ze šablony?")},text={Text("Historické výkony se nemažou.")},
        confirmButton={TextButton(onClick={vm.removeExercise(pendingRemoval!!);pendingRemoval=null}){Text("Odebrat")}},
        dismissButton={TextButton(onClick={pendingRemoval=null}){Text("Zrušit")}})
    if(deleteDay) AlertDialog(onDismissRequest={deleteDay=false},
        title={Text("Smazat tréninkovou šablonu?")},text={Text("Uložené tréninky zůstanou v historii.")},
        confirmButton={TextButton(onClick={vm.deleteDay();deleteDay=false}){Text("Odstranit")}},
        dismissButton={TextButton(onClick={deleteDay=false}){Text("Zrušit")}})
}

@Composable
private fun DataInput(label:String,value:String,onValue:(String)->Unit,modifier:Modifier=Modifier,keyboard:KeyboardType=KeyboardType.Text) {
    OutlinedTextField(value=value,onValueChange=onValue,label={Text(label,fontSize=12.sp)},modifier=modifier,
        singleLine=true,textStyle=TextStyle(fontSize=14.sp),keyboardOptions=KeyboardOptions(keyboardType=keyboard),
        shape=RoundedCornerShape(10.dp))
}
@Composable
private fun SparkLine(values:List<Double>,color:Color) {
    val data=values.takeLast(14)
    if(data.size<2) return
    Canvas(Modifier.fillMaxWidth().height(100.dp)) {
        val lo=data.minOrNull()?:0.0; val hi=data.maxOrNull()?:1.0
        val span=(hi-lo).coerceAtLeast(0.001)
        val points=data.mapIndexed {i,v ->
            val x=10f+(size.width-20f)*i/(data.size-1)
            val y=(size.height-15f)-(size.height-30f)*((v-lo)/span).toFloat()
            Offset(x,y)
        }
        drawLine(color=DarkBorder,start=Offset(10f,size.height-9f),end=Offset(size.width-10f,size.height-9f),strokeWidth=1.dp.toPx())
        for(i in 0 until points.lastIndex) drawLine(color,points[i],points[i+1],strokeWidth=2.5.dp.toPx())
        points.forEach {drawCircle(color,radius=3.dp.toPx(),center=it)}
    }
}

@Composable
fun RunScreen(vm:RezimState) {
    var form by remember(vm.runFormRequest) { mutableStateOf(vm.runFormRequest>0) }
    var date by remember {mutableStateOf(currentDate())}
    var km by remember {mutableStateOf("")}
    var time by remember {mutableStateOf("")}
    var avg by remember {mutableStateOf("")}
    var max by remember {mutableStateOf("")}
    var note by remember {mutableStateOf("")}
    LazyColumn(contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item {
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                Column {Text("Běhání",fontSize=23.sp,fontWeight=FontWeight.Bold);Text("Vzdálenost, tempo a tep",color=SecondaryText,fontSize=12.sp)}
                MainButton(if(form) "Zavřít" else "+ Běh",RunningBlue) {form=!form}
            }
        }
        if(form) item {
            SectionCard {
                MutedHeading("Nový běh")
                DataInput("Datum (RRRR-MM-DD)",date,{date=it},Modifier.fillMaxWidth())
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    DataInput("Km",km,{km=it},Modifier.weight(1f),KeyboardType.Decimal)
                    DataInput("Čas (mm:ss / h:mm:ss)",time,{time=it},Modifier.weight(1.4f))
                }
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    DataInput("Ø tep",avg,{avg=it},Modifier.weight(1f),KeyboardType.Number)
                    DataInput("Max tep",max,{max=it},Modifier.weight(1f),KeyboardType.Number)
                }
                DataInput("Poznámka (nepovinná)",note,{note=it},Modifier.fillMaxWidth())
                MainButton("Uložit běh",RunningBlue,Modifier.fillMaxWidth()) {
                    val distance=km.asNumber(); val sec=parseSeconds(time)
                    if(!validDate(date)||distance==null||distance<=0.0||distance>300||sec==null||sec<=0||!heartOK(avg)||!heartOK(max)) vm.tell("Zkontroluj datum, vzdálenost, čas a tepy.")
                    else {
                        vm.addRun(date,distance,sec,optionalHeart(avg),optionalHeart(max),note)
                        km="";time="";avg="";max="";note="";form=false
                    }
                }
            }
        }
        if(vm.runs.size>=2) item {
            SectionCard {
                MutedHeading("Vývoj tempa · min/km (níže = rychleji)")
                SparkLine(vm.runs.asReversed().map {it.seconds/60.0/it.km},RunningBlue)
            }
        }
        if(vm.runs.count{it.avg!=null}>=2) item {
            SectionCard {MutedHeading("Vývoj průměrného tepu · bpm"); SparkLine(vm.runs.asReversed().mapNotNull{it.avg?.toDouble()},RunningBlue)}
        }
        items(vm.runs,key={it.id}) { r ->
            SectionCard {
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                    Text(shortDate(r.date),color=SecondaryText,fontSize=12.sp)
                    Text("${fmtNumber(r.km)} km",color=RunningBlue,fontWeight=FontWeight.Bold)
                }
                Row(horizontalArrangement=Arrangement.spacedBy(14.dp)) {
                    Text(minutesAndSeconds(r.seconds),fontSize=15.sp,fontWeight=FontWeight.Medium)
                    Text(pace(r.seconds,r.km),fontSize=14.sp,color=SecondaryText)
                }
                Text("Tep Ø ${r.avg?.toString() ?: "–"} · Max ${r.max?.toString() ?: "–"} bpm",fontSize=12.sp,color=SecondaryText)
                if(r.note.isNotEmpty())Text(r.note,fontSize=11.sp,color=SecondaryText)
            }
        }
        if(vm.runs.isEmpty()) item {Text("Zatím žádné běhy.",color=SecondaryText)}
        item {Spacer(Modifier.height(15.dp))}
    }
}

@Composable
fun HiitScreen(vm:RezimState) {
    var form by remember(vm.hiitFormRequest) { mutableStateOf(vm.hiitFormRequest>0) }
    var date by remember {mutableStateOf(currentDate())}
    var minutes by remember {mutableStateOf("")}
    var rounds by remember {mutableStateOf("")}
    var work by remember {mutableStateOf("")}
    var rest by remember {mutableStateOf("")}
    var avg by remember {mutableStateOf("")}
    var max by remember {mutableStateOf("")}
    var note by remember {mutableStateOf("")}
    LazyColumn(contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item {
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                Column {Text("HIIT",fontSize=23.sp,fontWeight=FontWeight.Bold);Text("Eliptical a intervaly",fontSize=12.sp,color=SecondaryText)}
                MainButton(if(form) "Zavřít" else "+ HIIT",HiitOrange) {form=!form}
            }
        }
        if(form) item {
            SectionCard {
                MutedHeading("Nový intervalový trénink")
                DataInput("Datum (RRRR-MM-DD)",date,{date=it},Modifier.fillMaxWidth())
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    DataInput("Celkem minut",minutes,{minutes=it},Modifier.weight(1f),KeyboardType.Number)
                    DataInput("Počet intervalů",rounds,{rounds=it},Modifier.weight(1f),KeyboardType.Number)
                }
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    DataInput("Práce (s)",work,{work=it},Modifier.weight(1f),KeyboardType.Number)
                    DataInput("Pauza (s)",rest,{rest=it},Modifier.weight(1f),KeyboardType.Number)
                }
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    DataInput("Ø tep",avg,{avg=it},Modifier.weight(1f),KeyboardType.Number)
                    DataInput("Max tep",max,{max=it},Modifier.weight(1f),KeyboardType.Number)
                }
                DataInput("Poznámka (odpor apod.)",note,{note=it},Modifier.fillMaxWidth())
                MainButton("Uložit HIIT",HiitOrange,Modifier.fillMaxWidth()) {
                    val m=minutes.toIntOrNull();val r=rounds.toIntOrNull();val w=work.toIntOrNull();val p=rest.toIntOrNull()
                    if(!validDate(date)||m==null||m<=0||r==null||r<=0||w==null||w<=0||p==null||p<0||!heartOK(avg)||!heartOK(max)) vm.tell("Zkontroluj datum, intervaly a tepové hodnoty.")
                    else {vm.addHiit(date,m,r,w,p,optionalHeart(avg),optionalHeart(max),note)
                        minutes="";rounds="";work="";rest="";avg="";max="";note="";form=false}
                }
            }
        }
        if(vm.hiit.count{it.avg!=null}>=2) item {
            SectionCard {
                MutedHeading("Průměrný tep během HIIT")
                SparkLine(vm.hiit.asReversed().mapNotNull{it.avg?.toDouble()},HiitOrange)
            }
        }
        items(vm.hiit,key={it.id}) { r ->
            SectionCard {
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                    Text(shortDate(r.date),fontSize=12.sp,color=SecondaryText)
                    Text("${r.minutes} min",color=HiitOrange,fontWeight=FontWeight.Bold)
                }
                Text("${r.rounds} intervalů · ${r.work}s práce / ${r.rest}s pauza",fontSize=14.sp)
                Text("Tep Ø ${r.avg?:"–"} · Max ${r.max?:"–"} bpm",color=SecondaryText,fontSize=12.sp)
                if(r.note.isNotBlank()) Text(r.note,color=SecondaryText,fontSize=12.sp)
            }
        }
        if(vm.hiit.isEmpty()) item {SectionCard {Text("Zatím žádné záznamy. Přidej svůj příští HIIT na elipticalu.",color=SecondaryText,fontSize=13.sp)}}
        item {Spacer(Modifier.height(15.dp))}
    }
}

@Composable
fun BodyScreen(vm:RezimState) {
    var form by remember {mutableStateOf(false)}
    var date by remember {mutableStateOf(currentDate())}
    var kg by remember {mutableStateOf("")}
    var waist by remember {mutableStateOf("")}
    LazyColumn(contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item {
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                Column {Text("Tělesné míry",fontSize=23.sp,fontWeight=FontWeight.Bold);Text("Hmotnost a obvod břicha",color=SecondaryText,fontSize=12.sp)}
                MainButton(if(form) "Zavřít" else "+ Měření",BodyPurple) {form=!form}
            }
        }
        if(form) item {
            SectionCard {
                MutedHeading("Nové měření")
                DataInput("Datum (RRRR-MM-DD)",date,{date=it},Modifier.fillMaxWidth())
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    DataInput("Hmotnost (kg)",kg,{kg=it},Modifier.weight(1f),KeyboardType.Decimal)
                    DataInput("Břicho (cm)",waist,{waist=it},Modifier.weight(1f),KeyboardType.Decimal)
                }
                Text("Měř břicho vždy na stejném, nejširším místě.",fontSize=11.sp,color=SecondaryText)
                MainButton("Uložit měření",BodyPurple,Modifier.fillMaxWidth()) {
                    val weight=kg.asNumber();val size=waist.asNumber()
                    if(!validDate(date)||weight==null||weight<=0||weight>400||size==null||size<=0||size>300) vm.tell("Zkontroluj datum a hodnoty měření.")
                    else {vm.addMeasure(date,weight,size);kg="";waist="";form=false}
                }
            }
        }
        if(vm.measures.size>=2) item {
            SectionCard {MutedHeading("Trend hmotnosti · kg");SparkLine(vm.measures.asReversed().map{it.kg},BodyPurple)}
        }
        if(vm.measures.size>=2) item {
            SectionCard {MutedHeading("Trend obvodu břicha · cm");SparkLine(vm.measures.asReversed().map{it.waist},BodyPurple)}
        }
        items(vm.measures,key={it.id}) { e ->
            SectionCard {
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                    Text(shortDate(e.date),color=SecondaryText,fontSize=12.sp)
                    Text("${fmtNumber(e.kg)} kg",color=BodyPurple,fontWeight=FontWeight.Bold)
                }
                Text("Obvod břicha ${fmtNumber(e.waist)} cm",fontSize=13.sp)
            }
        }
        if(vm.measures.isEmpty()) item {SectionCard {Text("Zatím žádná měření. Přidej pondělní kontrolu během lean bulku.",color=SecondaryText,fontSize=13.sp)}}
        item {Spacer(Modifier.height(15.dp))}
    }
}
