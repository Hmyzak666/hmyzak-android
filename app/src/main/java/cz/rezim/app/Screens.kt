package cz.rezim.app

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
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
    val mode=LocalHmyzakDesign.current
    val shape=RoundedCornerShape(when(mode){0->12.dp;2->22.dp;else->16.dp})
    val surface=when(mode){0->Color(0xFF0C0C0C);2->Color(0xFF141917);else->Panel}
    Column(modifier.fillMaxWidth().clip(shape).background(surface)
        .then(if(mode==2) Modifier.border(1.dp,Color(0xFF27382F),shape) else Modifier)
        .padding(if(mode==0) 11.dp else if(mode==2) 17.dp else 14.dp),
        verticalArrangement=Arrangement.spacedBy(if(mode==0) 6.dp else 9.dp),content=content)
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
private fun optionalMinutesOK(text:String):Boolean = text.isBlank() || (text.toIntOrNull()?.let{it in 0..240} ?: false)
private fun optionalResistanceOK(text:String):Boolean = text.isBlank() || (text.toIntOrNull()?.let{it in 0..999} ?: false)
private fun optionalDistanceOK(text:String):Boolean = text.isBlank() || (text.asNumber()?.let{it>0.0 && it<=500.0} ?: false)
// Intervals = work repeated N times, with (N-1) pauses between intervals.
private fun estimateHiitMinutes(warmup:Int,cooldown:Int,rounds:Int,work:Int,rest:Int):Int =
    (warmup*60L + cooldown*60L + rounds.toLong()*work + (rounds-1).coerceAtLeast(0).toLong()*rest)
        .let{((it+59)/60).coerceAtMost(600).toInt()}

@Composable
fun HomeScreen(vm:RezimState,onExport:()->Unit,onImport:()->Unit,onShare:()->Unit,onCopy:()->Unit,onShareWeek:()->Unit,onCopyWeek:()->Unit,onShareAll:()->Unit) {
    val view=LocalView.current
    LazyColumn(contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        item {
            Column(verticalArrangement=Arrangement.spacedBy(4.dp)) {
                Text("Tvůj trénink. Bez rušení.",fontSize=23.sp,fontWeight=FontWeight.Bold)
                Text("Čisté rozhraní, výkony na jednom místě.",fontSize=13.sp,color=SecondaryText)
            }
        }
        item {
            SectionCard {
                MutedHeading("Vzhled Hmyzáka")
                Text("Tři varianty. Všechny zachovávají tmavý AMOLED styl a rychlý zápis.",fontSize=12.sp,color=SecondaryText)
                Row(horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                    listOf("Kompaktní","Vyvážený","Studio").forEachIndexed{ i,name ->
                        FilterChip(selected=vm.designMode==i,onClick={vm.setDesign(i)},
                            modifier=Modifier.weight(1f),label={Text(name,fontSize=11.sp,maxLines=1)})
                    }
                }
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                    Text("Vibrace po odkliknutí série",color=SecondaryText,fontSize=12.sp,
                        modifier=Modifier.weight(1f))
                    Switch(checked=vm.hapticEnabled,onCheckedChange=vm::setHaptic)
                }
                if(vm.hapticEnabled) {
                    Row(horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick={
                            vm.tell(when(performSetHaptic(view.context,test=true)) {
                                HapticResult.REQUESTED -> "Zkušební impulz odeslán. Pokud ho necítíš, zkontroluj intenzitu systémových vibrací."
                                HapticResult.SYSTEM_DISABLED -> "V telefonu jsou vypnuté vibrace dotyku. Zapni je v nastavení zvuku a vibrací."
                                HapticResult.NO_VIBRATOR -> "Telefon nehlásí dostupný vibrátor."
                                HapticResult.UNAVAILABLE -> "Vibraci se nepodařilo spustit. Zkontroluj nastavení zvuku a vibrací."
                            })
                        }) {Text("Vyzkoušet vibraci",fontSize=12.sp)}
                        TextButton(onClick={
                            if(!openHapticSettings(view.context)) vm.tell("Otevři v telefonu Nastavení → Zvuky a vibrace.")
                        }) {Text("Nastavení vibrací",fontSize=12.sp)}
                    }
                }
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                    Text("Automaticky posouvat na další sérii",color=SecondaryText,fontSize=12.sp,
                        modifier=Modifier.weight(1f))
                    Switch(checked=vm.autoScrollEnabled,onCheckedChange=vm::setAutoScroll)
                }
            }
        }
        if(vm.session!=null) item {
            SectionCard {
                Text("Máš rozpracovaný trénink",fontWeight=FontWeight.SemiBold,color=StrengthGreen)
                Text("Všechny zadané série jsou průběžně uložené.",color=SecondaryText,fontSize=12.sp)
                MainButton("Pokračovat v tréninku",modifier=Modifier.fillMaxWidth()) {vm.page=Page.STRENGTH}
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
                vm.recentWorkouts.take(12).forEach { record ->
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(vm.days.firstOrNull{it.key==record.day}?.name ?: record.day,fontSize=14.sp)
                            Text(java.time.Instant.ofEpochMilli(record.finished?:record.created)
                                .atZone(java.time.ZoneId.systemDefault()).toLocalDate().toString()+
                                " · ${record.elapsedSeconds()/60} min",
                                color=SecondaryText,fontSize=12.sp)
                        }
                        TextButton(onClick={vm.beginEditSavedWorkout(record)}){Text("Opravit",fontSize=12.sp)}
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
                MutedHeading("Pondělní týdenní kontrola")
                OutlinedButton(onClick=onCopyWeek,modifier=Modifier.fillMaxWidth()) {
                    Text("Kopírovat minulý týden",color=StrengthGreen)
                }
                var moreOptions by remember {mutableStateOf(false)}
                Box {
                    TextButton(onClick={moreOptions=true}) {Text("Další možnosti sdílení a kopírování ▾",color=SecondaryText)}
                    DropdownMenu(expanded=moreOptions,onDismissRequest={moreOptions=false}) {
                        DropdownMenuItem(text={Text("Sdílet minulý týden (Po–Ne)")},onClick={moreOptions=false;onShareWeek()})
                        DropdownMenuItem(text={Text("Sdílet poslední výkony")},onClick={moreOptions=false;onShare()})
                        DropdownMenuItem(text={Text("Kopírovat poslední výkony")},onClick={moreOptions=false;onCopy()})
                        DropdownMenuItem(text={Text("Sdílet celou historii jako soubor")},onClick={moreOptions=false;onShareAll()})
                    }
                }
                Text("Sdílení pouze po tvém potvrzení. Kopírování uloží výpis do schránky telefonu.",color=SecondaryText,fontSize=11.sp)
                Text("Zálohu ukládej bezpečně: exportovaný JSON není šifrovaný.",color=SecondaryText,fontSize=11.sp)
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
private fun CompactCell(value:String,onValue:(String)->Unit,width:Int,decimal:Boolean=false,
    enabled:Boolean=true,onLongPress:(()->Unit)?=null) {
    val requester=remember {BringIntoViewRequester()}
    val scope=rememberCoroutineScope()
    val focus=LocalFocusManager.current
    var focused by remember {mutableStateOf(false)}
    var field by remember {mutableStateOf(TextFieldValue(value))}
    val longPressAction by rememberUpdatedState(onLongPress)
    // Synchronise when + or - changes the underlying number without opening keyboard.
    LaunchedEffect(value) {
        if(field.text!=value) field=TextFieldValue(value,selection=TextRange(value.length))
    }
    val density=LocalDensity.current
    val imeHeight=WindowInsets.ime.getBottom(density)
    LaunchedEffect(focused,imeHeight) {
        if(focused && imeHeight>0) {delay(120);requester.bringIntoView()}
    }
    // Intercept the long press before the text field's own selection handler.
    // This Box covers the whole visible cell, including its padding. Ordinary taps,
    // cursor handling and scrolling keep going to the text field / surrounding list.
    val weightGesture=if(enabled && onLongPress!=null) Modifier
        .pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(pass=PointerEventPass.Initial)
                val finishedBeforeTimeout=withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                    waitForUpOrCancellation(pass=PointerEventPass.Initial)
                    true
                }
                if(finishedBeforeTimeout==null) {
                    longPressAction?.invoke()
                    do {
                        val event=awaitPointerEvent(PointerEventPass.Initial)
                        event.changes.forEach {it.consume()}
                    } while(event.changes.any {it.pressed})
                }
            }
        }
        .semantics {
            onLongClick(label="Rychlá změna váhy") {longPressAction?.invoke();true}
        }
        else Modifier
    Box(Modifier.width(width.dp).heightIn(min=44.dp).bringIntoViewRequester(requester).then(weightGesture)) {
    BasicTextField(
        value=field,onValueChange={ input ->
            val filtered=input.text.filter { it.isDigit() || (decimal && (it=='.'||it==',')) }.take(8)
            val next=input.copy(text=filtered,selection=TextRange(input.selection.start.coerceIn(0,filtered.length),
                input.selection.end.coerceIn(0,filtered.length)))
            field=next;onValue(filtered)
        },enabled=enabled,
        modifier=Modifier.fillMaxWidth().heightIn(min=44.dp)
            .onFocusChanged { state ->
                if(state.isFocused && !focused) {
                    field=field.copy(selection=TextRange(0,field.text.length))
                    scope.launch{delay(160);requester.bringIntoView()}
                }
                focused=state.isFocused
            }
            .clip(RoundedCornerShape(8.dp)).background(PanelLight)
            .padding(horizontal=3.dp,vertical=9.dp),
        textStyle=TextStyle(color=StrongText,fontSize=18.sp,fontWeight=FontWeight.SemiBold,
            textAlign=TextAlign.Center),
        singleLine=true,cursorBrush=SolidColor(StrengthGreen),
        keyboardOptions=KeyboardOptions(keyboardType=if(decimal) KeyboardType.Decimal else KeyboardType.Number,
            imeAction=ImeAction.Next),
        keyboardActions=KeyboardActions(onNext={focus.moveFocus(FocusDirection.Next)})
    )
    }
}

@Composable
private fun ExerciseCard(ex:Exercise,entries:List<SetEntry>,vm:RezimState,
    setRequesters:MutableMap<Long,BringIntoViewRequester>,onComplete:(SetEntry)->Unit) {
    var quickWeight by remember(ex.key){mutableStateOf<SetEntry?>(null)}
    var showHistory by remember(ex.key){mutableStateOf(false)}
    val keyboard=LocalSoftwareKeyboardController.current
    val focusManager=LocalFocusManager.current
    SectionCard {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text(ex.name,fontWeight=FontWeight.SemiBold,fontSize=15.sp)
                if(ex.note.isNotBlank()) Text(ex.note,color=SecondaryText,fontSize=12.sp)
                Text(when(ex.type) {
                    UnitType.KG -> "Hmotnost: kg (u jednoruček jedna činka)"
                    UnitType.ADDED_KG -> "Shyby: pouze přidaná zátěž"
                    UnitType.REPS -> "Vlastní váha / opakování (0 kg bez zátěže)"
                },fontSize=11.sp,color=SecondaryText)
            }
            val done=entries.count {it.done}
            Text("$done / ${entries.size}",color=if(done==entries.size) StrengthGreen else SecondaryText,fontSize=12.sp)
        }
        HorizontalDivider(color=DarkBorder)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(3.dp)) {
            Text("S",Modifier.width(17.dp),fontSize=10.sp,color=SecondaryText,textAlign=TextAlign.Center)
            Text("Minule",Modifier.width(53.dp),fontSize=10.sp,color=SecondaryText,textAlign=TextAlign.Center)
            Text(if(ex.type==UnitType.ADDED_KG) "+kg" else "kg",Modifier.width(65.dp),fontSize=10.sp,color=SecondaryText,textAlign=TextAlign.Center)
            Text("Opak.",Modifier.width(59.dp),fontSize=10.sp,color=SecondaryText,textAlign=TextAlign.Center)
            Text("−",Modifier.width(29.dp),fontSize=10.sp,color=SecondaryText,textAlign=TextAlign.Center)
            Text("+",Modifier.width(29.dp),fontSize=10.sp,color=SecondaryText,textAlign=TextAlign.Center)
            Spacer(Modifier.width(10.dp))
            Text("✓",Modifier.width(39.dp),fontSize=11.sp,color=SecondaryText,textAlign=TextAlign.Center)
        }
        entries.forEach { entry ->
            val setRequester=remember(entry.id) {BringIntoViewRequester()}
            DisposableEffect(entry.id,setRequesters) {
                setRequesters[entry.id]=setRequester
                onDispose {setRequesters.remove(entry.id)}
            }
            val prev=vm.previous["${ex.key}:${entry.number}"]
            Row(Modifier.fillMaxWidth().bringIntoViewRequester(setRequester)
                .horizontalScroll(rememberScrollState()),verticalAlignment=Alignment.CenterVertically,
                horizontalArrangement=Arrangement.spacedBy(3.dp)) {
                Text(entry.number.toString(),Modifier.width(17.dp),fontWeight=FontWeight.Bold,fontSize=13.sp,
                    color=if(entry.done)StrengthGreen else SecondaryText,textAlign=TextAlign.Center)
                val old=if(prev!=null) {
                    (if(ex.type==UnitType.REPS && prev.weight=="0") "" else "${prev.weight}×")+prev.reps
                } else "–"
                Text(old,Modifier.width(53.dp),fontSize=10.sp,color=SecondaryText,maxLines=1,textAlign=TextAlign.Center)
                CompactCell(entry.weight,onValue={vm.changeSet(entry,weight=it)},width=65,decimal=true,
                    onLongPress={
                        keyboard?.hide();focusManager.clearFocus()
                        if(entry.done) vm.tell("Nejdřív vrať sérii mezi nedokončené.")
                        else quickWeight=entry
                    })
                CompactCell(entry.reps,onValue={vm.changeSet(entry,reps=it)},width=59)
                Box(Modifier.size(width=29.dp,height=41.dp).clip(RoundedCornerShape(8.dp))
                    .background(PanelLight).clickable(enabled=!entry.done){vm.minusOneRep(entry)},
                    contentAlignment=Alignment.Center) {
                    Text("−",fontSize=21.sp,fontWeight=FontWeight.Bold,color=if(entry.done)SecondaryText else StrongText)
                }
                Box(Modifier.size(width=29.dp,height=41.dp).clip(RoundedCornerShape(8.dp))
                    .background(PanelLight).clickable(enabled=!entry.done){vm.plusOneRep(entry)},
                    contentAlignment=Alignment.Center) {
                    Text("+",fontSize=21.sp,fontWeight=FontWeight.Bold,color=if(entry.done)SecondaryText else StrengthGreen)
                }
                Spacer(Modifier.width(10.dp))
                Box(Modifier.size(width=39.dp,height=41.dp).clip(RoundedCornerShape(9.dp))
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
        TextButton(onClick={showHistory=!showHistory},contentPadding=PaddingValues(2.dp)) {
            Text(if(showHistory) "Skrýt historii ▴" else "Poslední 3 výkony ▾",color=StrengthGreen,fontSize=12.sp)
        }
        if(showHistory) {
            val history=vm.exerciseHistory(ex.key).takeLast(3).asReversed()
            if(history.isEmpty()) Text("Zatím žádné dokončené výkony.",color=SecondaryText,fontSize=12.sp)
            history.forEach { perf ->
                Text("${shortDate(perf.date)} · ${fmtNumber(perf.maxKg)} kg · ${perf.totalReps} op. / ${perf.sets} sérií",
                    color=SecondaryText,fontSize=12.sp)
            }
            Text(vm.exerciseRecordText(ex.key),fontSize=11.sp,color=StrengthGreen)
            Text("Odhad 1RM je orientační; pro cviky s vlastní vahou se nepoužívá.",fontSize=10.sp,color=SecondaryText)
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End,verticalAlignment=Alignment.CenterVertically) {
            TextButton(onClick={vm.removeWorkoutSet(ex)},enabled=entries.size>1,
                contentPadding=PaddingValues(horizontal=8.dp)) {Text("− Série",fontSize=12.sp)}
            TextButton(onClick={vm.addWorkoutSet(ex)},enabled=entries.size<15,
                contentPadding=PaddingValues(horizontal=8.dp)) {Text("+ Série",fontSize=12.sp,color=StrengthGreen)}
        }
        if(ex.partnerKey!=null) Text(if(vm.autoScrollEnabled) "SUPERSET · po ✓ přejdeš na další sérii" else "SUPERSET · ruční posouvání",color=SecondaryText,fontSize=10.sp)
        Text("Podrž hmotnost pro rychlou změnu o ±2,5 kg.",color=SecondaryText,fontSize=10.sp)
    }
    quickWeight?.let{ entry ->
        AlertDialog(onDismissRequest={quickWeight=null},title={Text("Rychlá změna váhy")},
            text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text("${ex.name} · série ${entry.number}",color=SecondaryText)
                Text("Aktuálně ${vm.workoutSets.firstOrNull{it.id==entry.id}?.weight ?: entry.weight} kg")
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick={vm.adjustWeight(entry,-2.5)},modifier=Modifier.weight(1f).heightIn(min=48.dp)){Text("− 2,5 kg")}
                    OutlinedButton(onClick={vm.adjustWeight(entry,2.5)},modifier=Modifier.weight(1f).heightIn(min=48.dp)){Text("+ 2,5 kg")}
                }
            }},
            confirmButton={TextButton(onClick={quickWeight=null}){Text("Hotovo")}})
    }
}

@Composable
fun StrengthScreen(vm:RezimState,onShareRecord:(String,String,String)->Unit) {
    val listState=rememberLazyListState()
    val scope=rememberCoroutineScope()
    var scrollJob by remember {mutableStateOf<Job?>(null)}
    val setRequesters=remember(vm.session?.id) {mutableMapOf<Long,BringIntoViewRequester>()}
    val plan=vm.plan(vm.selectedDay)
    var edit by remember { mutableStateOf(false) }
    var newDay by remember { mutableStateOf(false) }
    var newDayName by remember { mutableStateOf("") }
    var confirmDiscard by remember {mutableStateOf(false)}
    if(vm.editingSavedWorkout!=null) {
        SavedWorkoutEditor(vm)
        return
    }
    val keyboard=LocalSoftwareKeyboardController.current
    val view=LocalView.current
    val focusManager=androidx.compose.ui.platform.LocalFocusManager.current
    LaunchedEffect(listState) {
        listState.interactionSource.interactions.collect { interaction ->
            if(interaction is DragInteraction.Start) scrollJob?.cancel()
        }
    }
    LazyColumn(state=listState,contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item(key="strength-header") {
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
        if(edit && vm.session==null) item(key="strength-plan-editor") { PlanEditor(vm,plan) }
        item(key="strength-session") {
            SectionCard {
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                    Column {
                        Text(plan.name,fontSize=18.sp,fontWeight=FontWeight.Bold)
                        Text(if(vm.session==null) "Připraveno k tréninku" else "Právě probíhá",fontSize=12.sp,color=SecondaryText)
                    }
                    if(vm.session==null) MainButton("Zahájit") {vm.start()}
                    else Column(horizontalAlignment=Alignment.End) {
                        MainButton("Uložit trénink",color=StrengthGreen) {vm.finish()}
                        TextButton(onClick={confirmDiscard=true}){Text("Zahodit",color=HiitOrange)}
                    }
                }
                if(vm.session!=null) {
                    val completed=vm.workoutSets.count { it.done }
                    val total=vm.workoutSets.size
                    Text("Hotovo $completed / $total pracovních sérií",fontSize=12.sp,color=SecondaryText)
                    val fraction=if(total==0)0f else completed.toFloat()/total
                    val smoothProgress by animateFloatAsState(targetValue=fraction,label="Tréninkový progres")
                    LinearProgressIndicator(progress={smoothProgress},modifier=Modifier.fillMaxWidth(),color=StrengthGreen,trackColor=PanelLight)
                } else Text("Nový trénink předvyplní váhy a opakování podle naposledy dokončených sérií. Při shodném výkonu stačí klepnout na ✓ vpravo.",fontSize=12.sp,color=SecondaryText)
            }
        }
        if(vm.session!=null) {
            items(plan.exercises,key={"exercise:${it.key}"}) { ex ->
                val entries=vm.workoutSets.filter {it.exercise==ex.key}
                ExerciseCard(ex,entries,vm,setRequesters) { set ->
                    // Cancel a pending jump even on undo or a rapid second tap.
                    scrollJob?.cancel()
                    val completed=vm.toggle(set,ex.type)
                    if(completed!=null) {
                        if(vm.hapticEnabled) performSetHaptic(view.context)
                        if(completed) {keyboard?.hide();focusManager.clearFocus()}
                        if(completed && vm.autoScrollEnabled) {
                            val sessionId=vm.session?.id
                            scrollJob=scope.launch {
                                delay(300)
                                if(!vm.autoScrollEnabled || vm.session?.id!=sessionId) return@launch
                                val target=nextWorkoutTarget(plan.exercises,vm.workoutSets.toList(),set.id)
                                    ?:return@launch
                                // Keys describe the exact active list; an old plan-editor flag
                                // cannot shift indices while a workout is running.
                                val activeKeys=listOf("strength-header","strength-session")+
                                    plan.exercises.map {"exercise:${it.key}"}+"strength-save"
                                val targetKey=when(target) {
                                    is WorkoutScrollTarget.NextSet -> "exercise:${target.exerciseKey}"
                                    WorkoutScrollTarget.SaveWorkout -> "strength-save"
                                }
                                val index=activeKeys.indexOf(targetKey)
                                if(index>=0) {
                                    listState.animateScrollToItem(index)
                                    if(target is WorkoutScrollTarget.NextSet) {
                                        withFrameNanos { }
                                        setRequesters[target.setId]?.bringIntoView()
                                    }
                                }
                            }
                        }
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
                    if(ex.note.isNotBlank()) Text(ex.note,color=SecondaryText,fontSize=12.sp)
                }
            }
        }
        if(vm.session!=null) item(key="strength-save") {
            SectionCard {
                MainButton("Uložit trénink",modifier=Modifier.fillMaxWidth()) {vm.finish()}
                OutlinedButton(onClick={confirmDiscard=true},modifier=Modifier.fillMaxWidth()) {
                    Text("Zahodit rozpracovaný trénink",color=HiitOrange)
                }
            }
        }
        if(vm.session==null && plan.exercises.isNotEmpty()) item {
            var graphEx by remember(vm.selectedDay) {mutableStateOf(plan.exercises.first().key)}
            var graphMetric by remember(vm.selectedDay) {mutableIntStateOf(if(plan.exercises.first().type==UnitType.REPS) 1 else 0)}
            var graphMonths by remember(vm.selectedDay) {mutableIntStateOf(6)}
            SectionCard {
                MutedHeading("Progres cviku")
                var expanded by remember{mutableStateOf(false)}
                Box {
                    OutlinedButton(onClick={expanded=true}) {
                        Text(plan.exercises.firstOrNull{it.key==graphEx}?.name ?: "Vybrat cvik")
                    }
                    DropdownMenu(expanded=expanded,onDismissRequest={expanded=false}) {
                        plan.exercises.forEach {ex ->
                            DropdownMenuItem(text={Text(ex.name)},onClick={graphEx=ex.key;graphMetric=if(ex.type==UnitType.REPS)1 else 0;expanded=false})
                        }
                    }
                }
                Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                    listOf("Max kg","Opakování","Odhad 1RM").forEachIndexed { metric,label ->
                        FilterChip(selected=graphMetric==metric,onClick={graphMetric=metric},label={Text(label,fontSize=11.sp)})
                    }
                }
                Row(horizontalArrangement=Arrangement.spacedBy(7.dp)) {
                    listOf(1,3,6,12).forEach {months ->
                        FilterChip(selected=graphMonths==months,onClick={graphMonths=months},
                            label={Text("${months} m",fontSize=11.sp)})
                    }
                }
                val trend=vm.exerciseTrend(graphEx,graphMetric,graphMonths)
                val units=if(graphMetric==1) "op." else "kg"
                if(trend.size>1) {
                    SparkLine(trend.map{it.second},StrengthGreen)
                    Text("${shortDate(trend.first().first)} → ${shortDate(trend.last().first)}  ·  ${fmtNumber(trend.first().second)} → ${fmtNumber(trend.last().second)} $units",
                        color=SecondaryText,fontSize=12.sp)
                } else Text("Graf se zobrazí po dvou dokončených trénincích v daném období.",color=SecondaryText,fontSize=12.sp)
                Text(vm.exerciseRecordText(graphEx),color=StrengthGreen,fontSize=12.sp)
                if(vm.isLatestRecord(graphEx)) Text("◆ Nový osobní rekord",color=StrengthGreen,fontWeight=FontWeight.Bold,fontSize=12.sp)
                Text("1RM je přibližný výpočet ze zátěže a opakování; u 0 kg se nepočítá.",color=SecondaryText,fontSize=11.sp)
                OutlinedButton(onClick={
                    val name=plan.exercises.firstOrNull{it.key==graphEx}?.name?:graphEx
                    onShareRecord(name, if(vm.isLatestRecord(graphEx)) "OSOBNÍ REKORD" else "PŘEHLED VÝKONŮ", vm.exerciseRecordText(graphEx))
                },enabled=vm.exerciseHistory(graphEx).isNotEmpty(),modifier=Modifier.fillMaxWidth()) {
                    Text("Sdílet grafickou kartu výkonu")
                }
            }
        }
        item { Spacer(Modifier.height(220.dp)) }
    }
    if(confirmDiscard) AlertDialog(onDismissRequest={confirmDiscard=false},
        title={Text("Zahodit rozpracovaný trénink?")},
        text={Text("Zadané série v tomto rozpracovaném tréninku budou trvale odstraněny. Starší dokončené tréninky zůstanou zachované.")},
        confirmButton={TextButton(onClick={vm.discardWorkout();confirmDiscard=false}){Text("Zahodit",color=HiitOrange)}},
        dismissButton={TextButton(onClick={confirmDiscard=false}){Text("Pokračovat")}}
    )
    if(newDay) AlertDialog(
        onDismissRequest={newDay=false}, title={Text("Nový tréninkový den")},
        text={OutlinedTextField(value=newDayName,onValueChange={newDayName=it},label={Text("Název, např. Upper D")})},
        confirmButton={TextButton(onClick={vm.createDay(newDayName);newDayName="";newDay=false;edit=true}){Text("Přidat")}},
        dismissButton={TextButton(onClick={newDay=false}){Text("Zrušit")}}
    )
}

@Composable
private fun SavedWorkoutEditor(vm:RezimState) {
    val record=vm.editingSavedWorkout?:return
    val plan=vm.days.firstOrNull{it.key==record.day}
    var durationText by remember(record.id){mutableStateOf((record.elapsedSeconds()/60).toString())}
    LazyColumn(contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item {
            Text("Oprava uloženého tréninku",fontWeight=FontWeight.Bold,fontSize=21.sp)
            Text("${plan?.name?:record.day} · změny se projeví až po Uložit opravy.",color=SecondaryText,fontSize=12.sp)
        }
        val groups=vm.editingSavedSets.groupBy{it.exercise}.toList()
        items(groups.size) { i ->
            val (key,entries)=groups[i]
            val exercise=plan?.exercises?.firstOrNull{it.key==key}
            SectionCard {
                Text(exercise?.name?:key,fontWeight=FontWeight.SemiBold)
                entries.forEach {entry ->
                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,
                        horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                        Text("${entry.number}.",Modifier.width(25.dp),color=SecondaryText)
                        if(exercise?.type==UnitType.REPS) {
                            Text("—",Modifier.width(63.dp),textAlign=TextAlign.Center,color=SecondaryText)
                        } else {
                            CompactCell(entry.weight,onValue={vm.setSavedEntry(entry,weight=it)},width=63,decimal=true)
                        }
                        CompactCell(entry.reps,onValue={vm.setSavedEntry(entry,reps=it)},width=63)
                        Spacer(Modifier.weight(1f))
                        Checkbox(checked=entry.done,onCheckedChange={vm.setSavedEntry(entry,done=it)})
                    }
                }
            }
        }
        item {
            SectionCard {
                DataInput("Délka tréninku v minutách (lze opravit přerušení)",durationText,
                    {durationText=it},Modifier.fillMaxWidth(),KeyboardType.Number)
            }
            MainButton("Uložit opravy",modifier=Modifier.fillMaxWidth()) {
                val value=durationText.toIntOrNull()
                if(value==null || value !in 1..2880) vm.tell("Zadej platnou délku 1 až 2880 minut.")
                else {vm.setWorkoutDuration(record.id,value);vm.commitSavedEdit()}
            }
            OutlinedButton(onClick={vm.cancelSavedEdit()},modifier=Modifier.fillMaxWidth()) {Text("Zrušit bez uložení")}
        }
    }
}

@Composable
private fun PlanEditor(vm:RezimState,plan:DayPlan) {
    var name by remember(plan.key) {mutableStateOf("")}
    var setCount by remember(plan.key){mutableStateOf("4")}
    var unit by remember(plan.key){mutableStateOf(UnitType.KG)}
    var chooseUnit by remember{mutableStateOf(false)}
    var pendingRemoval by remember{mutableStateOf<String?>(null)}
    var editedExercise by remember(plan.key){mutableStateOf<Exercise?>(null)}
    var deleteDay by remember{mutableStateOf(false)}
    SectionCard {
        MutedHeading("Pořadí, supersérie a nové cviky")
        Text("Změny se použijí při příštím tréninku. Starší uložené výkony zůstávají v historii.",color=SecondaryText,fontSize=11.sp)
        plan.exercises.forEachIndexed { index,ex ->
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                Text(ex.name,Modifier.weight(1f),fontSize=12.sp)
                TextButton(onClick={editedExercise=ex},contentPadding=PaddingValues(3.dp)){Text("✎")}
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
    editedExercise?.let {ex ->
        var editedName by remember(ex.key){mutableStateOf(ex.name)}
        var editedNote by remember(ex.key){mutableStateOf(ex.note)}
        var editedType by remember(ex.key){mutableStateOf(ex.type)}
        AlertDialog(onDismissRequest={editedExercise=null},
            title={Text("Název a trvalá poznámka")},
            text={Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value=editedName,onValueChange={editedName=it},label={Text("Název cviku")},singleLine=true)
                OutlinedTextField(value=editedNote,onValueChange={editedNote=it},label={Text("Poznámka – vždy viditelná")},minLines=3,maxLines=6)
                Text("Zátěž cviku (0 kg lze zadat u každého typu)",fontSize=12.sp,color=SecondaryText)
                var chooseEditedType by remember(ex.key){mutableStateOf(false)}
                Box {
                    OutlinedButton(onClick={chooseEditedType=true}) {Text(when(editedType){
                        UnitType.KG->"Váha (kg)";UnitType.ADDED_KG->"Přidaná váha (+kg)";UnitType.REPS->"Vlastní váha / opakování"
                    })}
                    DropdownMenu(expanded=chooseEditedType,onDismissRequest={chooseEditedType=false}) {
                        UnitType.entries.forEach { t -> DropdownMenuItem(
                            text={Text(when(t){UnitType.KG->"Váha (kg)";UnitType.ADDED_KG->"Přidaná váha (+kg)";UnitType.REPS->"Vlastní váha / opakování"})},
                            onClick={editedType=t;chooseEditedType=false}) }
                    }
                }
            }},
            confirmButton={TextButton(onClick={
                if(editedName.isNotBlank()) {
                    vm.updateExerciseDetails(ex.key,editedName,editedNote)
                    if(editedType!=ex.type) vm.changeExerciseUnit(ex.key,editedType)
                    editedExercise=null
                }
                else vm.tell("Název cviku nesmí být prázdný.")
            }){Text("Uložit")}},
            dismissButton={TextButton(onClick={editedExercise=null}){Text("Zrušit")}}
        )
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
private fun NotesInput(label:String,value:String,onValue:(String)->Unit) {
    OutlinedTextField(value=value,onValueChange=onValue,
        label={Text(label,fontSize=12.sp)},modifier=Modifier.fillMaxWidth(),
        minLines=2,maxLines=5,textStyle=TextStyle(fontSize=14.sp))
}

@Composable
private fun DataInput(label:String,value:String,onValue:(String)->Unit,modifier:Modifier=Modifier,keyboard:KeyboardType=KeyboardType.Text) {
    var field by remember {mutableStateOf(TextFieldValue(value))}
    var focused by remember{mutableStateOf(false)}
    val focus=LocalFocusManager.current
    LaunchedEffect(value) {if(field.text!=value) field=TextFieldValue(value,selection=TextRange(value.length))}
    OutlinedTextField(value=field,onValueChange={field=it;onValue(it.text)},label={Text(label,fontSize=12.sp)},
        modifier=modifier.onFocusChanged{ state ->
            if(state.isFocused && !focused) field=field.copy(selection=TextRange(0,field.text.length))
            focused=state.isFocused
        },singleLine=true,textStyle=TextStyle(fontSize=16.sp,fontWeight=FontWeight.Medium),
        keyboardOptions=KeyboardOptions(keyboardType=keyboard,imeAction=ImeAction.Next),
        keyboardActions=KeyboardActions(onNext={focus.moveFocus(FocusDirection.Next)}),
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
    var editing by remember {mutableStateOf<RunEntry?>(null)}
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
            var distanceGroup by remember {mutableIntStateOf(0)}
            val chosen=when(distanceGroup){
                1 -> vm.runs.filter{it.km in 4.5..5.5}
                2 -> vm.runs.filter{it.km in 9.0..11.0}
                else -> vm.runs
            }
            SectionCard {
                MutedHeading("Vývoj běžeckého tempa · min/km")
                Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                    listOf("Vše","5 km","10 km").forEachIndexed {index,label ->
                        FilterChip(selected=distanceGroup==index,onClick={distanceGroup=index},label={Text(label)})
                    }
                }
                if(chosen.size>=2) {
                    SparkLine(chosen.asReversed().map{it.seconds/60.0/it.km},RunningBlue)
                    Text("${chosen.size} běhů · nižší tempo v min/km znamená rychlejší běh.",fontSize=11.sp,color=SecondaryText)
                } else Text("Pro tento rozsah zatím nejsou dva běhy.",fontSize=12.sp,color=SecondaryText)
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
                TextButton(onClick={editing=r}) {Text("Opravit záznam")}
            }
        }
        if(vm.runs.isEmpty()) item {Text("Zatím žádné běhy.",color=SecondaryText)}
        item {Spacer(Modifier.height(15.dp))}
    }
    editing?.let {record ->
        var d by remember(record.id){mutableStateOf(record.date)}
        var dist by remember(record.id){mutableStateOf(fmtNumber(record.km))}
        var duration by remember(record.id){mutableStateOf(minutesAndSeconds(record.seconds))}
        var a by remember(record.id){mutableStateOf(record.avg?.toString().orEmpty())}
        var m by remember(record.id){mutableStateOf(record.max?.toString().orEmpty())}
        var n by remember(record.id){mutableStateOf(record.note)}
        AlertDialog(onDismissRequest={editing=null},title={Text("Opravit běh")},
            text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(7.dp)) {
                DataInput("Datum RRRR-MM-DD",d,{d=it},Modifier.fillMaxWidth())
                DataInput("Vzdálenost (km)",dist,{dist=it},Modifier.fillMaxWidth(),KeyboardType.Decimal)
                DataInput("Čas mm:ss",duration,{duration=it},Modifier.fillMaxWidth())
                DataInput("Průměrný tep",a,{a=it},Modifier.fillMaxWidth(),KeyboardType.Number)
                DataInput("Max. tep",m,{m=it},Modifier.fillMaxWidth(),KeyboardType.Number)
                DataInput("Poznámka",n,{n=it},Modifier.fillMaxWidth())
            }},
            confirmButton={TextButton(onClick={
                val kmValue=dist.asNumber();val sec=parseSeconds(duration)
                if(!validDate(d)||kmValue==null||kmValue<=0||kmValue>300||sec==null||sec<=0||!heartOK(a)||!heartOK(m)) vm.tell("Zkontroluj opravené hodnoty.")
                else {vm.updateRun(record.copy(date=d,km=kmValue,seconds=sec,avg=optionalHeart(a),max=optionalHeart(m),note=n));editing=null}
            }){Text("Uložit")}},
            dismissButton={TextButton(onClick={editing=null}){Text("Zrušit")}}
        )
    }
}

@Composable
fun HiitScreen(vm:RezimState) {
    var editing by remember {mutableStateOf<HiitEntry?>(null)}
    var form by remember(vm.hiitFormRequest) {mutableStateOf(vm.hiitFormRequest>0)}
    var date by remember {mutableStateOf(currentDate())}
    var minutes by remember {mutableStateOf("")}
    var rounds by remember {mutableStateOf("")}
    var work by remember {mutableStateOf("")}
    var rest by remember {mutableStateOf("")}
    var avg by remember {mutableStateOf("")}
    var max by remember {mutableStateOf("")}
    var km by remember {mutableStateOf("")}
    var warmup by remember {mutableStateOf("")}
    var cooldown by remember {mutableStateOf("")}
    var workResistance by remember {mutableStateOf(vm.hiit.firstOrNull()?.workResistance?.toString().orEmpty())}
    var restResistance by remember {mutableStateOf(vm.hiit.firstOrNull()?.restResistance?.toString().orEmpty())}
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
                    DataInput("Warm-up (min)",warmup,{warmup=it},Modifier.weight(1f),KeyboardType.Number)
                    DataInput("Cool-down (min)",cooldown,{cooldown=it},Modifier.weight(1f),KeyboardType.Number)
                }
                TextButton(onClick={
                    val r=rounds.toIntOrNull();val w=work.toIntOrNull();val p=rest.toIntOrNull()
                    val wu=warmup.toIntOrNull()?:0;val cd=cooldown.toIntOrNull()?:0
                    if(r!=null && r in 1..1000 && w!=null && w>0 && p!=null && p>=0 && optionalMinutesOK(warmup) && optionalMinutesOK(cooldown))
                        minutes=estimateHiitMinutes(wu,cd,r,w,p).toString()
                    else vm.tell("Nejdřív vyplň počet intervalů, práci a pauzu.")
                }){Text("Dopočítat celkový čas (zaokrouhleno nahoru)",color=HiitOrange,fontSize=12.sp)}
                DataInput("Vzdálenost (km, nepovinné)",km,{km=it},Modifier.fillMaxWidth(),KeyboardType.Decimal)
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    DataInput("Odpor – práce",workResistance,{workResistance=it},Modifier.weight(1f),KeyboardType.Number)
                    DataInput("Odpor – pauza",restResistance,{restResistance=it},Modifier.weight(1f),KeyboardType.Number)
                }
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    DataInput("Ø tep",avg,{avg=it},Modifier.weight(1f),KeyboardType.Number)
                    DataInput("Max tep",max,{max=it},Modifier.weight(1f),KeyboardType.Number)
                }
                NotesInput("Volná poznámka",note,{note=it})
                Text("Odpor, zahřátí, zklidnění a vzdálenost jsou nepovinné. Délka zahrnuje celý trénink.",fontSize=11.sp,color=SecondaryText)
                MainButton("Uložit HIIT",HiitOrange,Modifier.fillMaxWidth()) {
                    val m=minutes.toIntOrNull();val r=rounds.toIntOrNull();val w=work.toIntOrNull();val p=rest.toIntOrNull()
                    if(!validDate(date)||m==null||m !in 1..600||r==null||r !in 1..1000||w==null||w !in 1..3600||p==null||p !in 0..3600||
                        !heartOK(avg)||!heartOK(max)||!optionalDistanceOK(km)||!optionalMinutesOK(warmup)||!optionalMinutesOK(cooldown)||
                        !optionalResistanceOK(workResistance)||!optionalResistanceOK(restResistance)) vm.tell("Zkontroluj datum, intervaly, čas a nepovinné hodnoty.")
                    else {
                        vm.addHiit(HiitEntry(0,date,m,r,w,p,optionalHeart(avg),optionalHeart(max),note,
                            km.asNumber(),warmup.toIntOrNull(),cooldown.toIntOrNull(),workResistance.toIntOrNull(),restResistance.toIntOrNull()))
                        minutes="";rounds="";work="";rest="";avg="";max="";km="";warmup="";cooldown="";note="";form=false
                    }
                }
            }
        }
        if(vm.hiit.count{it.avg!=null}>=2) item {
            SectionCard {MutedHeading("Průměrný tep během HIIT");SparkLine(vm.hiit.asReversed().mapNotNull{it.avg?.toDouble()},HiitOrange)}
        }
        items(vm.hiit,key={it.id}) { r ->
            SectionCard {
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                    Text(shortDate(r.date),fontSize=12.sp,color=SecondaryText)
                    Text("${r.minutes} min",color=HiitOrange,fontWeight=FontWeight.Bold)
                }
                Text("${r.rounds} intervalů · ${r.work}s práce / ${r.rest}s pauza",fontSize=14.sp)
                Text("Tep Ø ${r.avg?:"–"} · Max ${r.max?:"–"} bpm",color=SecondaryText,fontSize=12.sp)
                if(r.km!=null) Text("Vzdálenost ${fmtNumber(r.km)} km",color=SecondaryText,fontSize=12.sp)
                if(r.warmup!=null || r.cooldown!=null) Text("Warm-up ${r.warmup?:"–"} min · Cool-down ${r.cooldown?:"–"} min",color=SecondaryText,fontSize=12.sp)
                if(r.workResistance!=null || r.restResistance!=null) Text("Odpor: práce ${r.workResistance?:"–"} · pauza ${r.restResistance?:"–"}",color=SecondaryText,fontSize=12.sp)
                if(r.note.isNotBlank()) Text(r.note,color=SecondaryText,fontSize=12.sp)
                TextButton(onClick={editing=r}) {Text("Opravit záznam")}
            }
        }
        if(vm.hiit.isEmpty()) item {SectionCard {Text("Zatím žádné záznamy. Přidej svůj příští HIIT na elipticalu.",color=SecondaryText,fontSize=13.sp)}}
        item {Spacer(Modifier.height(15.dp))}
    }
    editing?.let {record ->
        var d by remember(record.id){mutableStateOf(record.date)}
        var mins by remember(record.id){mutableStateOf(record.minutes.toString())}
        var rounds by remember(record.id){mutableStateOf(record.rounds.toString())}
        var work by remember(record.id){mutableStateOf(record.work.toString())}
        var rest by remember(record.id){mutableStateOf(record.rest.toString())}
        var a by remember(record.id){mutableStateOf(record.avg?.toString().orEmpty())}
        var m by remember(record.id){mutableStateOf(record.max?.toString().orEmpty())}
        var n by remember(record.id){mutableStateOf(record.note)}
        var distance by remember(record.id){mutableStateOf(record.km?.let(::fmtNumber).orEmpty())}
        var wu by remember(record.id){mutableStateOf(record.warmup?.toString().orEmpty())}
        var cd by remember(record.id){mutableStateOf(record.cooldown?.toString().orEmpty())}
        var workRes by remember(record.id){mutableStateOf(record.workResistance?.toString().orEmpty())}
        var restRes by remember(record.id){mutableStateOf(record.restResistance?.toString().orEmpty())}
        AlertDialog(onDismissRequest={editing=null},title={Text("Opravit HIIT")},
            text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(7.dp)) {
                DataInput("Datum",d,{d=it},Modifier.fillMaxWidth())
                DataInput("Celkem minut",mins,{mins=it},Modifier.fillMaxWidth(),KeyboardType.Number)
                DataInput("Intervalů",rounds,{rounds=it},Modifier.fillMaxWidth(),KeyboardType.Number)
                DataInput("Práce (s)",work,{work=it},Modifier.fillMaxWidth(),KeyboardType.Number)
                DataInput("Odpočinek (s)",rest,{rest=it},Modifier.fillMaxWidth(),KeyboardType.Number)
                DataInput("Warm-up (min)",wu,{wu=it},Modifier.fillMaxWidth(),KeyboardType.Number)
                DataInput("Cool-down (min)",cd,{cd=it},Modifier.fillMaxWidth(),KeyboardType.Number)
                DataInput("Vzdálenost (km)",distance,{distance=it},Modifier.fillMaxWidth(),KeyboardType.Decimal)
                DataInput("Odpor – práce",workRes,{workRes=it},Modifier.fillMaxWidth(),KeyboardType.Number)
                DataInput("Odpor – pauza",restRes,{restRes=it},Modifier.fillMaxWidth(),KeyboardType.Number)
                DataInput("Průměrný tep",a,{a=it},Modifier.fillMaxWidth(),KeyboardType.Number)
                DataInput("Max. tep",m,{m=it},Modifier.fillMaxWidth(),KeyboardType.Number)
                NotesInput("Volná poznámka",n,{n=it})
            }},
            confirmButton={TextButton(onClick={
                val minValue=mins.toIntOrNull();val roundValue=rounds.toIntOrNull()
                val workValue=work.toIntOrNull();val restValue=rest.toIntOrNull()
                if(!validDate(d)||minValue==null||minValue !in 1..600||roundValue==null||roundValue !in 1..1000||
                    workValue==null||workValue !in 1..3600||restValue==null||restValue !in 0..3600||
                    !heartOK(a)||!heartOK(m)||!optionalDistanceOK(distance)||!optionalMinutesOK(wu)||!optionalMinutesOK(cd)||
                    !optionalResistanceOK(workRes)||!optionalResistanceOK(restRes)) vm.tell("Zkontroluj opravené hodnoty.")
                else {
                    vm.updateHiit(record.copy(date=d,minutes=minValue,rounds=roundValue,work=workValue,rest=restValue,
                        avg=optionalHeart(a),max=optionalHeart(m),note=n,km=distance.asNumber(),
                        warmup=wu.toIntOrNull(),cooldown=cd.toIntOrNull(),workResistance=workRes.toIntOrNull(),restResistance=restRes.toIntOrNull()))
                    editing=null
                }
            }){Text("Uložit")}},
            dismissButton={TextButton(onClick={editing=null}){Text("Zrušit")}}
        )
    }
}

@Composable
fun BodyScreen(vm:RezimState) {
    var editing by remember {mutableStateOf<MeasureEntry?>(null)}
    var form by remember {mutableStateOf(false)}
    var date by remember {mutableStateOf(currentDate())}
    var kg by remember {mutableStateOf("")}
    var waist by remember {mutableStateOf("")}
    val waistValues=vm.measures.asReversed().mapNotNull{it.waist}
    LazyColumn(contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item {
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                Column {Text("Tělesné míry",fontSize=23.sp,fontWeight=FontWeight.Bold);Text("Denní váha · břicho volitelně",color=SecondaryText,fontSize=12.sp)}
                MainButton(if(form) "Zavřít" else "+ Měření",BodyPurple) {if(!form) date=currentDate();form=!form}
            }
        }
        if(form) item {
            SectionCard {
                MutedHeading("Nové měření")
                DataInput("Datum (RRRR-MM-DD)",date,{date=it},Modifier.fillMaxWidth())
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    DataInput("Hmotnost (kg)",kg,{kg=it},Modifier.weight(1f),KeyboardType.Decimal)
                    DataInput("Břicho (cm, volitelné)",waist,{waist=it},Modifier.weight(1f),KeyboardType.Decimal)
                }
                Text("Váhu můžeš zapisovat každý den. Břicho nech prázdné, když ho neměříš.",fontSize=11.sp,color=SecondaryText)
                MainButton("Uložit měření",BodyPurple,Modifier.fillMaxWidth()) {
                    val values=parseBodyMeasurement(kg,waist)
                    if(!validDate(date)||values==null) vm.tell("Zkontroluj datum a hmotnost. Obvod břicha může zůstat prázdný; vyplněný musí být větší než 0 a nejvýše 300 cm.")
                    else {vm.addMeasure(date,values.kg,values.waist);kg="";waist="";form=false}
                }
            }
        }
        if(vm.measures.size>=2) item {
            SectionCard {MutedHeading("Trend hmotnosti · kg");SparkLine(vm.measures.asReversed().map{it.kg},BodyPurple)}
        }
        if(waistValues.size>=2) item {
            SectionCard {MutedHeading("Trend obvodu břicha · cm");SparkLine(waistValues,BodyPurple)}
        }
        items(vm.measures,key={it.id}) { e ->
            SectionCard {
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                    Text(shortDate(e.date),color=SecondaryText,fontSize=12.sp)
                    Text("${fmtNumber(e.kg)} kg",color=BodyPurple,fontWeight=FontWeight.Bold)
                }
                Text(e.waist?.let{"Obvod břicha ${fmtNumber(it)} cm"}?:"Obvod břicha nezadán",fontSize=13.sp,
                    color=if(e.waist==null) SecondaryText else MaterialTheme.colorScheme.onSurface)
                TextButton(onClick={editing=e}){Text("Opravit záznam")}
            }
        }
        if(vm.measures.isEmpty()) item {SectionCard {Text("Zatím žádná měření. Přidej dnešní váhu; obvod břicha je volitelný.",color=SecondaryText,fontSize=13.sp)}}
        item {Spacer(Modifier.height(15.dp))}
    }
    editing?.let {record ->
        var d by remember(record.id){mutableStateOf(record.date)}
        var weight by remember(record.id){mutableStateOf(fmtNumber(record.kg))}
        var waist by remember(record.id){mutableStateOf(record.waist?.let{fmtNumber(it)}?:"")}
        AlertDialog(onDismissRequest={editing=null},title={Text("Opravit měření")},
            text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                DataInput("Datum",d,{d=it},Modifier.fillMaxWidth())
                DataInput("Hmotnost (kg)",weight,{weight=it},Modifier.fillMaxWidth(),KeyboardType.Decimal)
                DataInput("Obvod břicha (cm, volitelné)",waist,{waist=it},Modifier.fillMaxWidth(),KeyboardType.Decimal)
            }},
            confirmButton={TextButton(onClick={
                val values=parseBodyMeasurement(weight,waist)
                if(!validDate(d)||values==null) vm.tell("Zkontroluj opravené hodnoty. Obvod břicha může zůstat prázdný.")
                else {vm.updateMeasure(record.copy(date=d,kg=values.kg,waist=values.waist));editing=null}
            }){Text("Uložit")}},
            dismissButton={TextButton(onClick={editing=null}){Text("Zrušit")}}
        )
    }
}
