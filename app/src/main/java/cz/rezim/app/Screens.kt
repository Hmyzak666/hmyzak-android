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
import androidx.compose.ui.platform.testTag
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
internal fun MainButton(label:String, modifier:Modifier=Modifier, color:Color=StrengthGreen, action:()->Unit) {
    Button(onClick=action,modifier=modifier,shape=RoundedCornerShape(11.dp),
        colors=ButtonDefaults.buttonColors(containerColor=color,contentColor=PitchBlack)) {
        Text(label,fontWeight=FontWeight.Bold)
    }
}
@Composable
internal fun MutedHeading(text:String) {
    val locale=androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    Text(text.uppercase(locale),color=SecondaryText,fontSize=11.sp,fontWeight=FontWeight.SemiBold,letterSpacing=1.sp)
}
@Composable
internal fun BigMetric(label:String,value:String,modifier:Modifier=Modifier,color:Color=StrengthGreen) {
    SectionCard(modifier) {
        Text(label,color=SecondaryText,fontSize=12.sp)
        Text(value,color=color,fontSize=23.sp,fontWeight=FontWeight.Bold,maxLines=1)
    }
}

private fun minutesAndSeconds(seconds:Int):String = "%d:%02d".format(java.util.Locale.US, seconds/60, seconds%60)
private fun pace(seconds:Int,km:Double):String = if(km<=0) "–" else minutesAndSeconds((seconds/km).roundToInt())+" /km"
internal fun shortDate(date:String):String = try {
    val d=LocalDate.parse(date); d.format(java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.SHORT))
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
internal fun validDate(d:String) = try {LocalDate.parse(d);true} catch (_:Exception){false}
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
fun HomeScreen(vm:RezimState,onExport:()->Unit,onImport:()->Unit,onShare:()->Unit,onCopy:()->Unit,onShareWeek:()->Unit,onCopyWeek:()->Unit,onShareAll:()->Unit,onSettings:()->Unit,onCsv:()->Unit,onUpgrade:()->Unit=onSettings) {
    val context=androidx.compose.ui.platform.LocalContext.current

    LazyColumn(modifier=Modifier.testTag("home-screen"),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        item {
            Column(verticalArrangement=Arrangement.spacedBy(4.dp)) {
                Text(context.tr(R.string.ui_tvuj_trenink_bez_ruseni_82245),fontSize=23.sp,fontWeight=FontWeight.Bold)
                Text(context.tr(R.string.ui_ciste_rozhrani_vykony_na_jednom_miste_be6c0),fontSize=13.sp,color=SecondaryText)
            }
        }
        item {
            OutlinedButton(onClick=onSettings,modifier=Modifier.fillMaxWidth()) { Text(context.tr(R.string.ui_nastaveni_aplikace_b064e)) }
        }
        if(vm.session!=null) item {
            SectionCard {
                Text(context.tr(R.string.ui_mas_rozpracovany_trenink_013f8),fontWeight=FontWeight.SemiBold,color=StrengthGreen)
                Text(context.tr(R.string.ui_vsechny_zadane_serie_jsou_prubezne_ulozene_b1255),color=SecondaryText,fontSize=12.sp)
                MainButton(context.tr(R.string.ui_pokracovat_v_treninku_54e08),modifier=Modifier.fillMaxWidth()) {vm.page=Page.STRENGTH}
            }
        }
        item {
            Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                BigMetric(context.tr(R.string.ui_silove_treninky_800b3),vm.totalWorkouts.toString(),modifier=Modifier.weight(1f),color=StrengthGreen)
                BigMetric(context.tr(R.string.cardio_title),vm.cardio.size.toString(),modifier=Modifier.weight(1f),color=RunningBlue)
            }
        }
        item {
            Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                BigMetric(context.tr(R.string.ui_hiit_zaznamy_2ed22),vm.hiit.size.toString(),modifier=Modifier.weight(1f),color=HiitOrange)
                BigMetric(context.tr(R.string.ui_posledni_hmotnost_d6b32),vm.measures.firstNotNullOfOrNull {it.kg}?.let {fmtDisplay(it)+" kg"}?:"–",modifier=Modifier.weight(1f),color=BodyPurple)
            }
        }
        item {
            SectionCard(Modifier.testTag("quick-start-section")) {
                MutedHeading(context.tr(R.string.ui_rychle_spusteni_8742b))
                Text(context.tr(R.string.ui_otevri_libovolny_trenink_vsechny_cviky_jsou_ad32f),fontSize=13.sp,color=SecondaryText)
                QuickStartContent(vm)
            }
        }
        item { BigMetric(context.tr(R.string.ui_celkem_nazvedano_fccfb),fmtDisplay(vm.totalLiftedKg)+" kg",color=StrengthGreen) }
        item { ProStatsCard(vm,onCsv,onUpgrade) }
        if(vm.runs.isNotEmpty()) item {
            SectionCard {
                MutedHeading(context.tr(R.string.ui_posledni_beh_abc85))
                val last=vm.runs.first()
                Row(horizontalArrangement=Arrangement.SpaceBetween,modifier=Modifier.fillMaxWidth()) {
                    Text(shortDate(last.date),color=SecondaryText,fontSize=12.sp)
                    Text("${fmtDisplay(last.km)} km",color=RunningBlue,fontWeight=FontWeight.Bold)
                }
                Row(horizontalArrangement=Arrangement.spacedBy(22.dp)) {
                    Text(pace(last.seconds,last.km),fontWeight=FontWeight.SemiBold)
                    Text("Ø ${last.avg?.toString() ?: "–"} bpm",color=SecondaryText)
                }
            }
        }
        item {
            SectionCard {
                MutedHeading(context.tr(R.string.ui_posledni_silove_treninky_78869))
                if(vm.recentWorkouts.isEmpty()) Text(context.tr(R.string.workout_history_empty),color=SecondaryText,fontSize=12.sp)
                vm.recentWorkouts.forEach {WorkoutHistoryRow(vm,it)}
                OutlinedButton(onClick=vm::openWorkoutHistory,modifier=Modifier.fillMaxWidth().testTag("all-workout-history")) {
                    Text(context.tr(R.string.workout_history_all))
                }
            }
        }
        item {
            SectionCard {
                MutedHeading(context.tr(R.string.ui_soukromi_a_zalohy_f8da6))
                Text(context.tr(R.string.ui_vsechna_data_jsou_ulozena_pouze_v_f06f1),color=SecondaryText,fontSize=13.sp)
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick=onExport,modifier=Modifier.weight(1f),contentPadding=PaddingValues(horizontal=5.dp)) {Text(context.tr(R.string.ui_zaloha_json_5ac9b),fontSize=12.sp)}
                    OutlinedButton(onClick=onImport,modifier=Modifier.weight(1f),contentPadding=PaddingValues(horizontal=5.dp)) {Text(context.tr(R.string.ui_obnovit_4fe15),fontSize=12.sp)}
                }
                MutedHeading(context.tr(R.string.ui_pondelni_tydenni_kontrola_f72d6))
                OutlinedButton(onClick=onCopyWeek,modifier=Modifier.fillMaxWidth()) {
                    Text(context.tr(R.string.ui_kopirovat_tyden_pro_ai_ae56b),color=StrengthGreen)
                }
                var moreOptions by remember {mutableStateOf(false)}
                Box {
                    TextButton(onClick={moreOptions=true}) {Text(context.tr(R.string.ui_dalsi_moznosti_sdileni_a_kopirovani_9fa11),color=SecondaryText)}
                    DropdownMenu(expanded=moreOptions,onDismissRequest={moreOptions=false}) {
                        DropdownMenuItem(text={Text(context.tr(R.string.ui_sdilet_minuly_tyden_pone_1c878))},onClick={moreOptions=false;onShareWeek()})
                        DropdownMenuItem(text={Text(context.tr(R.string.ui_sdilet_posledni_vykony_e269f))},onClick={moreOptions=false;onShare()})
                        DropdownMenuItem(text={Text(context.tr(R.string.ui_kopirovat_posledni_vykony_pro_ai_2f610))},onClick={moreOptions=false;onCopy()})
                        DropdownMenuItem(text={Text(context.tr(R.string.ui_sdilet_celou_historii_jako_soubor_fa34e))},onClick={moreOptions=false;onShareAll()})
                    }
                }
                Text(context.tr(R.string.ui_sdileni_pouze_po_tvem_potvrzeni_kopirovani_fbc5d),color=SecondaryText,fontSize=11.sp)
                Text(context.tr(R.string.ui_zalohu_ukladej_bezpecne_exportovany_json_neni_9ce6b),color=SecondaryText,fontSize=11.sp)
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
private fun CompactCell(value:String,onValue:(String)->Unit,width:Int,decimal:Boolean=false,
    enabled:Boolean=true,onLongPress:(()->Unit)?=null) {
    val context=androidx.compose.ui.platform.LocalContext.current

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
            onLongClick(label=context.tr(R.string.ui_rychla_zmena_vahy_a9a55)) {longPressAction?.invoke();true}
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
    val context=androidx.compose.ui.platform.LocalContext.current

    var quickWeight by remember(ex.key){mutableStateOf<SetEntry?>(null)}
    var showHistory by remember(ex.key){mutableStateOf(false)}
    var editNote by androidx.compose.runtime.saveable.rememberSaveable(ex.key){mutableStateOf(false)}
    var editSuperset by androidx.compose.runtime.saveable.rememberSaveable(ex.key){mutableStateOf(false)}
    val keyboard=LocalSoftwareKeyboardController.current
    val focusManager=LocalFocusManager.current
    SectionCard {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text(ex.name,fontWeight=FontWeight.SemiBold,fontSize=15.sp)
                if(ex.note.isNotBlank()) Text(ex.note,color=SecondaryText,fontSize=12.sp)
                Text(when(ex.type) {
                    UnitType.KG -> context.tr(R.string.ui_hmotnost_kg_u_jednorucek_jedna_cinka_5e308)
                    UnitType.ADDED_KG -> context.tr(R.string.ui_shyby_pouze_pridana_zatez_0393b)
                    UnitType.REPS -> context.tr(R.string.ui_vlastni_vaha_opakovani_kg_bez_zateze_5374b)
                },fontSize=11.sp,color=SecondaryText)
            }
            val done=entries.count {it.done}
            Text("$done / ${entries.size}",color=if(done==entries.size) StrengthGreen else SecondaryText,fontSize=12.sp)
        }
        LoadConventionPicker(vm,ex)
        HorizontalDivider(color=DarkBorder)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(3.dp)) {
            Text(context.tr(R.string.ui_s_02aa6),Modifier.width(17.dp),fontSize=10.sp,color=SecondaryText,textAlign=TextAlign.Center)
            Text(context.tr(R.string.ui_minule_8d48b),Modifier.width(53.dp),fontSize=10.sp,color=SecondaryText,textAlign=TextAlign.Center)
            Text(if(ex.type==UnitType.ADDED_KG) "+kg" else context.tr(R.string.ui_kg_13898),Modifier.width(65.dp),fontSize=10.sp,color=SecondaryText,textAlign=TextAlign.Center)
            Text(context.tr(R.string.ui_opak_2a39d),Modifier.width(59.dp),fontSize=10.sp,color=SecondaryText,textAlign=TextAlign.Center)
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
                        if(entry.done) vm.tell(context.tr(R.string.ui_nejdriv_vrat_serii_mezi_nedokoncene_cf238))
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
                Text(context.tr(R.string.ui_s_opakovani_oproti_minule_10488 ,(entry.reps.toIntOrNull()?:0)-(prev.reps.toIntOrNull()?:0)),
                    color=StrengthGreen,fontSize=11.sp,modifier=Modifier.padding(start=28.dp))
            }
        }
        TextButton(onClick={showHistory=!showHistory},contentPadding=PaddingValues(2.dp)) {
            Text(if(showHistory) context.tr(R.string.ui_skryt_historii_8c853) else context.tr(R.string.ui_posledni_vykony_a1ce0),color=StrengthGreen,fontSize=12.sp)
        }
        if(showHistory) {
            val history=vm.exerciseHistory(ex.key).takeLast(3).asReversed()
            if(history.isEmpty()) Text(context.tr(R.string.ui_zatim_zadne_dokoncene_vykony_b2111),color=SecondaryText,fontSize=12.sp)
            history.forEach { perf ->
                Text(context.tr(R.string.ui_s_s_kg_s_op_s_d39e2 ,shortDate(perf.date),fmtDisplay(perf.maxKg),perf.totalReps,perf.sets),
                    color=SecondaryText,fontSize=12.sp)
            }
            Text(vm.exerciseRecordText(ex.key),fontSize=11.sp,color=StrengthGreen)
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End,verticalAlignment=Alignment.CenterVertically) {
            TextButton(onClick={vm.removeWorkoutSet(ex)},enabled=entries.size>1,
                contentPadding=PaddingValues(horizontal=8.dp)) {Text(context.tr(R.string.ui_serie_ac13f),fontSize=12.sp)}
            TextButton(onClick={vm.addWorkoutSet(ex)},enabled=entries.size<15,
                contentPadding=PaddingValues(horizontal=8.dp)) {Text(context.tr(R.string.ui_serie_ef6c4),fontSize=12.sp,color=StrengthGreen)}
        }
        val members=supersetMembers(vm.plan(vm.selectedDay).exercises,ex.key)
        if(members.size>1) {
            Text(context.tr(R.string.ui_superserie_06027)+members.joinToString(" → "){it.name},color=StrengthGreen,fontSize=11.sp)
            Text(context.supersetRoundText(ex,members,vm.workoutSets.toList()),color=SecondaryText,fontSize=11.sp)
        }
        Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            TextButton(onClick={editNote=true}) {Text(context.tr(R.string.ui_trvala_poznamka_3a05a),fontSize=12.sp)}
            TextButton(onClick={editSuperset=true}) {Text(context.tr(R.string.ui_superserie_f4a01),fontSize=12.sp)}
        }
        Text(context.tr(R.string.ui_podrz_hmotnost_pro_rychlou_zmenu_o_f5cb9),color=SecondaryText,fontSize=10.sp)
    }
    if(editNote) PermanentNoteDialog(vm,ex) {editNote=false}
    if(editSuperset) SupersetDialog(vm,ex) {editSuperset=false}
    quickWeight?.let{ entry ->
        AlertDialog(onDismissRequest={quickWeight=null},title={Text(context.tr(R.string.ui_rychla_zmena_vahy_a9a55))},
            text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text(context.tr(R.string.ui_s_serie_s_d1551 ,ex.name,entry.number),color=SecondaryText)
                Text(context.tr(R.string.ui_aktualne_s_kg_e14b5 ,vm.workoutSets.firstOrNull{it.id==entry.id}?.weight ?: entry.weight))
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick={vm.adjustWeight(entry,-2.5)},modifier=Modifier.weight(1f).heightIn(min=48.dp)){Text("− 2,5 kg")}
                    OutlinedButton(onClick={vm.adjustWeight(entry,2.5)},modifier=Modifier.weight(1f).heightIn(min=48.dp)){Text("+ 2,5 kg")}
                }
            }},
            confirmButton={TextButton(onClick={quickWeight=null}){Text(context.tr(R.string.ui_hotovo_09526))}})
    }
}

@Composable
fun StrengthScreen(vm:RezimState,onShareRecord:(String,String,String)->Unit,onShareWorkout:(Session,Boolean)->Unit,onUpgrade:()->Unit={vm.page=Page.SETTINGS}) {
    val context=androidx.compose.ui.platform.LocalContext.current

    var finishedSummary by remember {mutableStateOf<Session?>(null)}
    finishedSummary?.let {record -> WorkoutSummaryDialog(vm,record,onShareWorkout,onUpgrade) {finishedSummary=null}}
    fun saveWorkout() {
        val id=vm.session?.id ?: return
        vm.finish()
        if(vm.session==null) finishedSummary=vm.recentWorkouts.firstOrNull {it.id==id}
    }

    val listState=rememberLazyListState()
    val scope=rememberCoroutineScope()
    var scrollJob by remember {mutableStateOf<Job?>(null)}
    val setRequesters=remember(vm.session?.id) {mutableMapOf<Long,BringIntoViewRequester>()}
    if(vm.editingSavedWorkout!=null) {SavedWorkoutEditor(vm);return}
    if(vm.days.isEmpty()) {EmptyPlansScreen(vm);return}
    val plan=vm.days.firstOrNull {it.key==vm.selectedDay}?:vm.days.first()
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
    LazyColumn(modifier=Modifier.testTag("strength-workout"),state=listState,contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item(key="strength-header") {
            Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
                Text(context.tr(R.string.ui_silovy_trenink_0c35f),fontSize=23.sp,fontWeight=FontWeight.Bold)
                Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    vm.days.forEach {day ->
                        FilterChip(selected=vm.selectedDay==day.key,onClick={vm.selectDay(day.key)},label={Text("${day.short} · ${day.name}")})
                    }
                }
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(9.dp)) {
                    OutlinedButton(onClick={if(vm.session==null) edit=!edit else vm.tell(context.tr(R.string.ui_nejdriv_uloz_rozpracovany_trenink_595c2))}) {
                        Text(if(edit) context.tr(R.string.ui_hotovo_09526) else context.tr(R.string.ui_upravit_plan_19a83))
                    }
                    OutlinedButton(onClick={newDay=true}) {Text(context.tr(R.string.ui_treninkovy_den_ef7ee))}
                }
            }
        }
        if(edit && vm.session==null) item(key="strength-plan-editor") { PlanEditor(vm,plan) }
        item(key="strength-session") {
            SectionCard {
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                    Column {
                        Text(plan.name,fontSize=18.sp,fontWeight=FontWeight.Bold)
                        Text(if(vm.session==null) context.tr(R.string.ui_pripraveno_k_treninku_e9904) else context.tr(R.string.ui_prave_probiha_8d3e9),fontSize=12.sp,color=SecondaryText)
                    }
                    if(vm.session==null) MainButton(context.tr(R.string.ui_zahajit_5a2d0)) {vm.start()}
                    else Column(horizontalAlignment=Alignment.End) {
                        MainButton(context.tr(R.string.ui_ulozit_trenink_f10b8),color=StrengthGreen) {
                            saveWorkout()
                        }
                        TextButton(onClick={confirmDiscard=true}){Text(context.tr(R.string.ui_zahodit_379d6),color=HiitOrange)}
                    }
                }
                if(vm.session!=null) {
                    val completed=vm.workoutSets.count { it.done }
                    val total=vm.workoutSets.size
                    Text(context.tr(R.string.ui_hotovo_s_s_pracovnich_serii_dc9d5 ,completed,total),fontSize=12.sp,color=SecondaryText)
                    Text(context.tr(R.string.ui_nazvedano_s_kg_d79c5 ,fmtDisplay(vm.activeLiftedKg)),fontSize=14.sp,color=StrengthGreen)
                    val fraction=if(total==0)0f else completed.toFloat()/total
                    val smoothProgress by animateFloatAsState(targetValue=fraction,label=context.tr(R.string.ui_treninkovy_progres_ac106))
                    LinearProgressIndicator(progress={smoothProgress},modifier=Modifier.fillMaxWidth(),color=StrengthGreen,trackColor=PanelLight)
                } else Text(context.tr(R.string.ui_novy_trenink_predvyplni_vahy_a_opakovani_536dc),fontSize=12.sp,color=SecondaryText)
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
                        Text(context.quantity(R.plurals.set_count,ex.sets),fontSize=12.sp,color=SecondaryText)
                    }
                    if(ex.note.isNotBlank()) Text(ex.note,color=SecondaryText,fontSize=12.sp)
                }
            }
        }
        if(vm.session!=null) item(key="strength-save") {
            SectionCard {
                MainButton(context.tr(R.string.ui_ulozit_trenink_f10b8),modifier=Modifier.fillMaxWidth().testTag("finish-workout-bottom")) {saveWorkout()}
                OutlinedButton(onClick={confirmDiscard=true},modifier=Modifier.fillMaxWidth()) {
                    Text(context.tr(R.string.ui_zahodit_rozpracovany_trenink_0a949),color=HiitOrange)
                }
            }
        }
        if(vm.session==null && plan.exercises.isNotEmpty()) item {
            ExerciseRecordsCard(vm,plan)
        }
        if(vm.session==null && plan.exercises.isNotEmpty()) item {
            var graphEx by remember(vm.selectedDay) {mutableStateOf(plan.exercises.first().key)}
            var graphMetric by remember(vm.selectedDay) {mutableIntStateOf(if(plan.exercises.first().type==UnitType.REPS) 1 else 0)}
            var graphMonths by remember(vm.selectedDay) {mutableIntStateOf(6)}
            SectionCard {
                MutedHeading(context.tr(R.string.ui_progres_cviku_2d278))
                ProFeatureContent(vm,onUpgrade,"exercise-progress") {
                    var expanded by remember{mutableStateOf(false)}
                    Box {
                        OutlinedButton(onClick={expanded=true}) {
                            Text(plan.exercises.firstOrNull{it.key==graphEx}?.name ?: context.tr(R.string.ui_vybrat_cvik_9737d))
                        }
                        DropdownMenu(expanded=expanded,onDismissRequest={expanded=false}) {
                            plan.exercises.forEach {ex ->
                                DropdownMenuItem(text={Text(ex.name)},onClick={graphEx=ex.key;graphMetric=if(ex.type==UnitType.REPS)1 else 0;expanded=false})
                            }
                        }
                    }
                    Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                        listOf(context.tr(R.string.ui_max_kg_8b405),context.tr(R.string.ui_opakovani_307fe),context.tr(R.string.ui_odhad_rm_67857)).forEachIndexed { metric,label ->
                            FilterChip(selected=graphMetric==metric,onClick={graphMetric=metric},label={Text(label,fontSize=11.sp)})
                        }
                    }
                    Row(horizontalArrangement=Arrangement.spacedBy(7.dp)) {
                        listOf(1,3,6,12).forEach {months ->
                            FilterChip(selected=graphMonths==months,onClick={graphMonths=months},
                                label={Text(context.quantity(R.plurals.period_months,months),fontSize=11.sp)})
                        }
                    }
                    val trend=vm.exerciseTrend(graphEx,graphMetric,graphMonths)
                    val units=if(graphMetric==1) context.tr(R.string.ui_op_85765) else context.tr(R.string.ui_kg_13898)
                    if(trend.size>1) {
                        SparkLine(trend.map{it.second},StrengthGreen)
                        Text("${shortDate(trend.first().first)} → ${shortDate(trend.last().first)}  ·  ${fmtDisplay(trend.first().second)} → ${fmtDisplay(trend.last().second)} $units",
                            color=SecondaryText,fontSize=12.sp)
                    } else Text(context.tr(R.string.ui_graf_se_zobrazi_po_dvou_dokoncenych_c95b8),color=SecondaryText,fontSize=12.sp)
                    Text(vm.exerciseRecordText(graphEx,includeEstimate=true),color=StrengthGreen,fontSize=12.sp)
                    Text(context.tr(R.string.ui_rm_je_priblizny_vypocet_ze_zateze_ec9a1),color=SecondaryText,fontSize=11.sp)
                    OutlinedButton(onClick={
                        val name=plan.exercises.firstOrNull{it.key==graphEx}?.name?:graphEx
                        onShareRecord(name, if(vm.isLatestRecord(graphEx)) context.tr(R.string.ui_osobni_rekord_bbb68) else context.tr(R.string.ui_prehled_vykonu_7dbb2), vm.exerciseRecordText(graphEx,includeEstimate=true))
                    },enabled=vm.exerciseHistory(graphEx).isNotEmpty(),modifier=Modifier.fillMaxWidth()) {
                        Text(context.tr(R.string.ui_sdilet_grafickou_kartu_vykonu_c3675))
                    }
                }
            }
        }
        item { Spacer(Modifier.height(220.dp)) }
    }
    if(confirmDiscard) AlertDialog(onDismissRequest={confirmDiscard=false},
        title={Text(context.tr(R.string.ui_zahodit_rozpracovany_trenink_066f7))},
        text={Text(context.tr(R.string.ui_zadane_serie_v_tomto_rozpracovanem_treninku_b0989))},
        confirmButton={TextButton(onClick={vm.discardWorkout();confirmDiscard=false}){Text(context.tr(R.string.ui_zahodit_379d6),color=HiitOrange)}},
        dismissButton={TextButton(onClick={confirmDiscard=false}){Text(context.tr(R.string.ui_pokracovat_659f1))}}
    )
    if(newDay) AlertDialog(
        onDismissRequest={newDay=false}, title={Text(context.tr(R.string.ui_novy_treninkovy_den_2e7db))},
        text={OutlinedTextField(value=newDayName,onValueChange={newDayName=it},label={Text(context.tr(R.string.ui_nazev_napr_upper_d_8a5d0))})},
        confirmButton={TextButton(onClick={vm.createDay(newDayName);newDayName="";newDay=false;edit=true}){Text(context.tr(R.string.ui_pridat_2cf8c))}},
        dismissButton={TextButton(onClick={newDay=false}){Text(context.tr(R.string.ui_zrusit_9bd5e))}}
    )
}

@Composable
private fun SavedWorkoutEditor(vm:RezimState) {
    val context=androidx.compose.ui.platform.LocalContext.current

    val record=vm.editingSavedWorkout?:return
    val plan=vm.days.firstOrNull{it.key==record.day}
    val originalMinutes=durationText(record.elapsedSeconds())
    var durationText by remember(record.id){mutableStateOf(originalMinutes)}
    LazyColumn(contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item {
            Text(context.tr(R.string.ui_oprava_ulozeneho_treninku_4f780),fontWeight=FontWeight.Bold,fontSize=21.sp)
            Text(context.tr(R.string.ui_s_zmeny_se_projevi_az_po_d0d50 ,plan?.name?:record.day),color=SecondaryText,fontSize=12.sp)
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
                DurationInput(context.tr(R.string.time_duration),durationText,{durationText=it},tag="saved-workout-time")
            }
            MainButton(context.tr(R.string.ui_ulozit_opravy_f3e43),modifier=Modifier.fillMaxWidth()) {
                val value=durationSeconds(durationText)
                if(durationText==originalMinutes) vm.commitSavedEdit()
                else if(value==null || value !in 1..172800) vm.tell(context.tr(R.string.ui_zadej_platnou_delku_az_minut_c3179))
                else vm.commitSavedEditSeconds(value)
            }
            OutlinedButton(onClick={vm.cancelSavedEdit()},modifier=Modifier.fillMaxWidth()) {Text(context.tr(R.string.ui_zrusit_bez_ulozeni_cc84e))}
        }
    }
}

@Composable
private fun PlanEditor(vm:RezimState,plan:DayPlan) {
    val context=androidx.compose.ui.platform.LocalContext.current

    var name by remember(plan.key) {mutableStateOf("")}
    var setCount by remember(plan.key){mutableStateOf("4")}
    var unit by remember(plan.key){mutableStateOf(UnitType.KG)}
    var chooseUnit by remember{mutableStateOf(false)}
    var pendingRemoval by remember{mutableStateOf<String?>(null)}
    var editedExercise by remember(plan.key){mutableStateOf<Exercise?>(null)}
    var deleteDay by remember{mutableStateOf(false)}
    var dayName by remember(plan.key,plan.name){mutableStateOf(plan.name)}
    SectionCard {
        OutlinedTextField(value=dayName,onValueChange={dayName=it},label={Text(context.tr(R.string.day_name))},modifier=Modifier.fillMaxWidth())
        TextButton(onClick={vm.renameDay(dayName)},enabled=dayName.isNotBlank() && dayName!=plan.name) {Text(context.tr(R.string.ui_ulozit_85e82))}
        MutedHeading(context.tr(R.string.ui_poradi_superserie_a_nove_cviky_877e4))
        Text(context.tr(R.string.ui_zmeny_se_pouziji_pri_pristim_treninku_bd4e3),color=SecondaryText,fontSize=11.sp)
        plan.exercises.forEachIndexed { index,ex ->
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                Text(ex.name,Modifier.weight(1f),fontSize=12.sp)
                TextButton(onClick={editedExercise=ex},contentPadding=PaddingValues(3.dp)){Text("✎")}
                TextButton(onClick={vm.moveExercise(ex.key,-1)},enabled=index>0,contentPadding=PaddingValues(3.dp)) {Text("↑")}
                TextButton(onClick={vm.moveExercise(ex.key,1)},enabled=index<plan.exercises.lastIndex,contentPadding=PaddingValues(3.dp)) {Text("↓")}
                TextButton(onClick={pendingRemoval=ex.key},contentPadding=PaddingValues(3.dp)) {Text("×",color=HiitOrange)}
            }
            Row(verticalAlignment=Alignment.CenterVertically) {
                Text(context.tr(R.string.ui_serie_74f19)+": ${ex.sets}",modifier=Modifier.weight(1f),fontSize=12.sp)
                TextButton(onClick={vm.setExerciseSets(ex.key,ex.sets-1)},enabled=ex.sets>1) {Text("−")}
                TextButton(onClick={vm.setExerciseSets(ex.key,ex.sets+1)},enabled=ex.sets<15) {Text("+")}
            }
            var editSuperset by androidx.compose.runtime.saveable.rememberSaveable(ex.key) {mutableStateOf(false)}
            val members=supersetMembers(plan.exercises,ex.key)
            OutlinedButton(onClick={editSuperset=true}) {
                Text(if(members.size>1) context.tr(R.string.ui_superserie_s_cviky_2a630 ,members.size) else context.tr(R.string.ui_nastavit_superserii_4d318),fontSize=11.sp)
            }
            if(members.size>1) Text(context.supersetRoundText(ex,members),fontSize=11.sp,color=SecondaryText)
            if(editSuperset) SupersetDialog(vm,ex) {editSuperset=false}
            HorizontalDivider(color=DarkBorder)
        }
        OutlinedTextField(value=name,onValueChange={name=it},label={Text(context.tr(R.string.ui_novy_cvik_fbc52))},modifier=Modifier.fillMaxWidth(),singleLine=true)
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.CenterVertically) {
            OutlinedTextField(value=setCount,onValueChange={setCount=it.filter{ch->ch.isDigit()}.take(2)},label={Text(context.tr(R.string.ui_serie_74f19))},modifier=Modifier.width(85.dp),singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number))
            Box {
                OutlinedButton(onClick={chooseUnit=true}) {Text(unit.name)}
                DropdownMenu(expanded=chooseUnit,onDismissRequest={chooseUnit=false}) {
                    UnitType.entries.forEach{t-> DropdownMenuItem(text={Text(t.name)},onClick={unit=t;chooseUnit=false})}
                }
            }
            MainButton(context.tr(R.string.ui_pridat_2cf8c),modifier=Modifier.weight(1f)) {
                vm.addExercise(name,setCount.toIntOrNull()?:0,unit);name=""
            }
        }
        Text(context.tr(R.string.ui_kg_vaha_added_kg_pridana_zatez_bacbe),color=SecondaryText,fontSize=11.sp)
        TextButton(onClick={deleteDay=true}) {Text(context.tr(R.string.ui_odstranit_tento_treninkovy_den_384e2),color=HiitOrange)}
    }
    editedExercise?.let {ex ->
        var editedName by remember(ex.key){mutableStateOf(ex.name)}
        var editedNote by remember(ex.key){mutableStateOf(ex.note)}
        var editedType by remember(ex.key){mutableStateOf(ex.type)}
        AlertDialog(onDismissRequest={editedExercise=null},
            title={Text(context.tr(R.string.ui_nazev_a_trvala_poznamka_f8cd5))},
            text={Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value=editedName,onValueChange={editedName=it},label={Text(context.tr(R.string.ui_nazev_cviku_2c98e))},singleLine=true)
                OutlinedTextField(value=editedNote,onValueChange={editedNote=it},label={Text(context.tr(R.string.ui_poznamka_vzdy_viditelna_bd543))},minLines=3,maxLines=6)
                LoadConventionPicker(vm,vm.plan(plan.key).exercises.first {it.key==ex.key})
                Text(context.tr(R.string.ui_zatez_cviku_kg_lze_zadat_u_79844),fontSize=12.sp,color=SecondaryText)
                var chooseEditedType by remember(ex.key){mutableStateOf(false)}
                Box {
                    OutlinedButton(onClick={chooseEditedType=true}) {Text(when(editedType){
                        UnitType.KG->context.tr(R.string.ui_vaha_kg_1115d);UnitType.ADDED_KG->context.tr(R.string.ui_pridana_vaha_kg_2a13d);UnitType.REPS->context.tr(R.string.ui_vlastni_vaha_opakovani_5c213)
                    })}
                    DropdownMenu(expanded=chooseEditedType,onDismissRequest={chooseEditedType=false}) {
                        UnitType.entries.forEach { t -> DropdownMenuItem(
                            text={Text(when(t){UnitType.KG->context.tr(R.string.ui_vaha_kg_1115d);UnitType.ADDED_KG->context.tr(R.string.ui_pridana_vaha_kg_2a13d);UnitType.REPS->context.tr(R.string.ui_vlastni_vaha_opakovani_5c213)})},
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
                else vm.tell(context.tr(R.string.ui_nazev_cviku_nesmi_byt_prazdny_685dc))
            }){Text(context.tr(R.string.ui_ulozit_85e82))}},
            dismissButton={TextButton(onClick={editedExercise=null}){Text(context.tr(R.string.ui_zrusit_9bd5e))}}
        )
    }
    if(pendingRemoval!=null) AlertDialog(onDismissRequest={pendingRemoval=null},
        title={Text(context.tr(R.string.ui_odebrat_cvik_ze_sablony_4732e))},text={Text(context.tr(R.string.ui_historicke_vykony_se_nemazou_f9a62))},
        confirmButton={TextButton(onClick={vm.removeExercise(pendingRemoval!!);pendingRemoval=null}){Text(context.tr(R.string.ui_odebrat_744ed))}},
        dismissButton={TextButton(onClick={pendingRemoval=null}){Text(context.tr(R.string.ui_zrusit_9bd5e))}})
    if(deleteDay) AlertDialog(onDismissRequest={deleteDay=false},
        title={Text(context.tr(R.string.ui_smazat_treninkovou_sablonu_5598c))},text={Text(context.tr(R.string.ui_ulozene_treninky_zustanou_v_historii_2b6c1))},
        confirmButton={TextButton(onClick={vm.deleteDay();deleteDay=false}){Text(context.tr(R.string.ui_odstranit_be8a0))}},
        dismissButton={TextButton(onClick={deleteDay=false}){Text(context.tr(R.string.ui_zrusit_9bd5e))}})
}

@Composable
internal fun NotesInput(label:String,value:String,onValue:(String)->Unit) {
    OutlinedTextField(value=value,onValueChange=onValue,
        label={Text(label,fontSize=12.sp)},modifier=Modifier.fillMaxWidth(),
        minLines=2,maxLines=5,textStyle=TextStyle(fontSize=14.sp))
}

@Composable
internal fun DataInput(label:String,value:String,onValue:(String)->Unit,modifier:Modifier=Modifier,keyboard:KeyboardType=KeyboardType.Text) {
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
internal fun SparkLine(values:List<Double>,color:Color) {
    val data=values
    if(data.size<2) return
    Canvas(Modifier.fillMaxWidth().height(100.dp).testTag("analytics-chart")) {
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
fun HiitScreen(vm:RezimState,onUpgrade:()->Unit={vm.page=Page.SETTINGS}) {
    val context=androidx.compose.ui.platform.LocalContext.current

    var editing by remember {mutableStateOf<HiitEntry?>(null)}
    var form by rememberSaveable(vm.hiitFormRequest) {mutableStateOf(vm.hiitFormRequest>0)}
    var date by rememberSaveable {mutableStateOf(currentDate())}
    var minutes by rememberSaveable(vm.hiitFormRequest) {mutableStateOf(durationText(0))}
    var rounds by rememberSaveable(vm.hiitFormRequest) {mutableStateOf(vm.hiitPreset?.rounds?.toString().orEmpty())}
    var work by rememberSaveable(vm.hiitFormRequest) {mutableStateOf(durationText(vm.hiitPreset?.work ?: 0,false))}
    var rest by rememberSaveable(vm.hiitFormRequest) {mutableStateOf(durationText(vm.hiitPreset?.rest ?: 0,false))}
    var avg by rememberSaveable {mutableStateOf("")}
    var max by rememberSaveable {mutableStateOf("")}
    var km by rememberSaveable {mutableStateOf("")}
    var warmup by rememberSaveable {mutableStateOf("")}
    var cooldown by rememberSaveable {mutableStateOf("")}
    var workResistance by rememberSaveable {mutableStateOf(vm.hiit.firstOrNull()?.workResistance?.toString().orEmpty())}
    var restResistance by rememberSaveable {mutableStateOf(vm.hiit.firstOrNull()?.restResistance?.toString().orEmpty())}
    var activity by rememberSaveable {mutableStateOf("OTHER")}
    var note by rememberSaveable {mutableStateOf("")}
    LazyColumn(contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item {
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                Column {Text(context.tr(R.string.ui_hiit_199e5),fontSize=23.sp,fontWeight=FontWeight.Bold);Text(context.tr(R.string.ui_eliptical_a_intervaly_e752c),fontSize=12.sp,color=SecondaryText)}
                MainButton(if(form) context.tr(R.string.ui_zavrit_ae288) else "+ HIIT",color=HiitOrange) {form=!form}
            }
        }
        if(form) item {
            SectionCard {
                MutedHeading(context.tr(R.string.ui_novy_intervalovy_trenink_55f17))
                HiitActivityPicker(activity) {activity=it}
                DataInput(context.tr(R.string.ui_datum_rrrr_mm_dd_2ac3f),date,{date=it},Modifier.fillMaxWidth())
                DurationInput(context.tr(R.string.time_duration),minutes,{minutes=it},tag="hiit-total")
                DataInput(context.tr(R.string.ui_pocet_intervalu_11b22),rounds,{rounds=it},Modifier.fillMaxWidth(),KeyboardType.Number)
                DurationInput(context.tr(R.string.time_work),work,{work=it},hours=false,tag="hiit-work")
                DurationInput(context.tr(R.string.time_rest),rest,{rest=it},hours=false,tag="hiit-rest")
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    DataInput(context.tr(R.string.ui_warm_up_min_81975),warmup,{warmup=it},Modifier.weight(1f),KeyboardType.Number)
                    DataInput(context.tr(R.string.ui_cool_down_min_dcde5),cooldown,{cooldown=it},Modifier.weight(1f),KeyboardType.Number)
                }
                TextButton(onClick={
                    val r=rounds.toIntOrNull();val w=durationSeconds(work,false);val p=durationSeconds(rest,false)
                    val wu=warmup.toIntOrNull()?:0;val cd=cooldown.toIntOrNull()?:0
                    if(r!=null && r in 1..1000 && w!=null && w>0 && p!=null && p>=0 && optionalMinutesOK(warmup) && optionalMinutesOK(cooldown))
                        minutes=durationText((wu*60L+cd*60L+r.toLong()*w+(r-1).toLong()*p).coerceAtMost(36000L).toInt())
                    else vm.tell(context.tr(R.string.ui_nejdriv_vypln_pocet_intervalu_praci_a_ecb32))
                }){Text(context.tr(R.string.time_calculate),color=HiitOrange,fontSize=12.sp)}
                DataInput(context.tr(R.string.ui_vzdalenost_km_nepovinne_f2c65),km,{km=it},Modifier.fillMaxWidth(),KeyboardType.Decimal)
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    DataInput(context.tr(R.string.ui_odpor_prace_83e4f),workResistance,{workResistance=it},Modifier.weight(1f),KeyboardType.Number)
                    DataInput(context.tr(R.string.ui_odpor_pauza_d6a1d),restResistance,{restResistance=it},Modifier.weight(1f),KeyboardType.Number)
                }
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    DataInput(context.tr(R.string.ui_tep_38219),avg,{avg=it},Modifier.weight(1f),KeyboardType.Number)
                    DataInput(context.tr(R.string.ui_max_tep_cf81e),max,{max=it},Modifier.weight(1f),KeyboardType.Number)
                }
                NotesInput(context.tr(R.string.ui_volna_poznamka_c1cc9),note,{note=it})
                Text(context.tr(R.string.ui_odpor_zahrati_zklidneni_a_vzdalenost_jsou_0fae8),fontSize=11.sp,color=SecondaryText)
                MainButton(context.tr(R.string.ui_ulozit_hiit_f93a8),modifier=Modifier.fillMaxWidth(),color=HiitOrange) {
                    val exact=durationSeconds(minutes);val m=exact?.let {(it+59)/60};val r=rounds.toIntOrNull();val w=durationSeconds(work,false);val p=durationSeconds(rest,false)
                    if(!validDate(date)||exact==null||exact !in 1..36000||m==null||m !in 1..600||r==null||r !in 1..1000||w==null||w !in 1..3600||p==null||p !in 0..3600||
                        !heartOK(avg)||!heartOK(max)||!optionalDistanceOK(km)||!optionalMinutesOK(warmup)||!optionalMinutesOK(cooldown)||
                        !optionalResistanceOK(workResistance)||!optionalResistanceOK(restResistance)) vm.tell(context.tr(R.string.ui_zkontroluj_datum_intervaly_cas_a_nepovinne_2f7a9))
                    else {
                        vm.addHiit(HiitEntry(0,date,m,r,w,p,optionalHeart(avg),optionalHeart(max),note,
                            km.asNumber(),warmup.toIntOrNull(),cooldown.toIntOrNull(),workResistance.toIntOrNull(),restResistance.toIntOrNull(),activity,exact))
                        minutes="";rounds="";work="";rest="";avg="";max="";km="";warmup="";cooldown="";note="";form=false
                    }
                }
            }
        }
        if(vm.hiit.count{it.avg!=null}>=2) item {
            SectionCard {MutedHeading(context.tr(R.string.ui_prumerny_tep_behem_hiit_81059));ProFeatureContent(vm,onUpgrade,"hiit-heart-rate") {SparkLine(vm.hiit.asReversed().mapNotNull{it.avg?.toDouble()},HiitOrange)}}
        }
        items(vm.hiit,key={it.id}) { r ->
            SectionCard {
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                    Text(shortDate(r.date),fontSize=12.sp,color=SecondaryText)
                    Text(durationText(r.totalSeconds ?: r.minutes*60),color=HiitOrange,fontWeight=FontWeight.Bold)
                }
                Text(context.tr(HiitActivity.entries.firstOrNull {it.name==r.activity}?.label?:R.string.activity_other),color=HiitOrange,fontSize=12.sp)
                Text(context.tr(R.string.ui_s_intervalu_ss_prace_ss_pauza_ae54a ,r.rounds,r.work,r.rest),fontSize=14.sp)
                Text(context.tr(R.string.ui_tep_s_max_s_bpm_e66a2 ,r.avg?:"–",r.max?:"–"),color=SecondaryText,fontSize=12.sp)
                if(r.km!=null) Text(context.tr(R.string.ui_vzdalenost_s_km_62bde ,fmtDisplay(r.km)),color=SecondaryText,fontSize=12.sp)
                if(r.warmup!=null || r.cooldown!=null) Text(context.tr(R.string.ui_warm_up_s_min_cool_down_755fc ,r.warmup?:"–",r.cooldown?:"–"),color=SecondaryText,fontSize=12.sp)
                if(r.workResistance!=null || r.restResistance!=null) Text(context.tr(R.string.ui_odpor_prace_s_pauza_s_baa24 ,r.workResistance?:"–",r.restResistance?:"–"),color=SecondaryText,fontSize=12.sp)
                if(r.note.isNotBlank()) Text(r.note,color=SecondaryText,fontSize=12.sp)
                TextButton(onClick={editing=r}) {Text(context.tr(R.string.ui_opravit_zaznam_932c9))}
            }
        }
        if(vm.hiit.isEmpty()) item {SectionCard {Text(context.tr(R.string.ui_zatim_zadne_zaznamy_pridej_svuj_pristi_8083a),color=SecondaryText,fontSize=13.sp)}}
        item {Spacer(Modifier.height(15.dp))}
    }
    editing?.let {record ->
        var d by rememberSaveable(record.id){mutableStateOf(record.date)}
        var mins by rememberSaveable(record.id){mutableStateOf(durationText(record.totalSeconds ?: record.minutes*60))}
        var rounds by rememberSaveable(record.id){mutableStateOf(record.rounds.toString())}
        var work by rememberSaveable(record.id){mutableStateOf(durationText(record.work,false))}
        var rest by rememberSaveable(record.id){mutableStateOf(durationText(record.rest,false))}
        var a by rememberSaveable(record.id){mutableStateOf(record.avg?.toString().orEmpty())}
        var m by rememberSaveable(record.id){mutableStateOf(record.max?.toString().orEmpty())}
        var n by rememberSaveable(record.id){mutableStateOf(record.note)}
        var distance by rememberSaveable(record.id){mutableStateOf(record.km?.let(::fmtNumber).orEmpty())}
        var wu by rememberSaveable(record.id){mutableStateOf(record.warmup?.toString().orEmpty())}
        var cd by rememberSaveable(record.id){mutableStateOf(record.cooldown?.toString().orEmpty())}
        var workRes by rememberSaveable(record.id){mutableStateOf(record.workResistance?.toString().orEmpty())}
        var restRes by rememberSaveable(record.id){mutableStateOf(record.restResistance?.toString().orEmpty())}
        var editedActivity by rememberSaveable(record.id){mutableStateOf(record.activity)}
        AlertDialog(onDismissRequest={editing=null},title={Text(context.tr(R.string.ui_opravit_hiit_d99ea))},
            text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(7.dp)) {
                HiitActivityPicker(editedActivity) {editedActivity=it}
                DataInput(context.tr(R.string.ui_datum_df5c3),d,{d=it},Modifier.fillMaxWidth())
                DurationInput(context.tr(R.string.time_duration),mins,{mins=it},tag="edit-hiit-total")
                DataInput(context.tr(R.string.ui_intervalu_33e96),rounds,{rounds=it},Modifier.fillMaxWidth(),KeyboardType.Number)
                DurationInput(context.tr(R.string.time_work),work,{work=it},hours=false,tag="edit-hiit-work")
                DurationInput(context.tr(R.string.time_rest),rest,{rest=it},hours=false,tag="edit-hiit-rest")
                DataInput(context.tr(R.string.ui_warm_up_min_81975),wu,{wu=it},Modifier.fillMaxWidth(),KeyboardType.Number)
                DataInput(context.tr(R.string.ui_cool_down_min_dcde5),cd,{cd=it},Modifier.fillMaxWidth(),KeyboardType.Number)
                DataInput(context.tr(R.string.ui_vzdalenost_km_1e8bf),distance,{distance=it},Modifier.fillMaxWidth(),KeyboardType.Decimal)
                DataInput(context.tr(R.string.ui_odpor_prace_83e4f),workRes,{workRes=it},Modifier.fillMaxWidth(),KeyboardType.Number)
                DataInput(context.tr(R.string.ui_odpor_pauza_d6a1d),restRes,{restRes=it},Modifier.fillMaxWidth(),KeyboardType.Number)
                DataInput(context.tr(R.string.ui_prumerny_tep_8efa6),a,{a=it},Modifier.fillMaxWidth(),KeyboardType.Number)
                DataInput(context.tr(R.string.ui_max_tep_21910),m,{m=it},Modifier.fillMaxWidth(),KeyboardType.Number)
                NotesInput(context.tr(R.string.ui_volna_poznamka_c1cc9),n,{n=it})
            }},
            confirmButton={TextButton(onClick={
                val exact=durationSeconds(mins);val minValue=exact?.let {(it+59)/60};val roundValue=rounds.toIntOrNull()
                val workValue=durationSeconds(work,false);val restValue=durationSeconds(rest,false)
                if(!validDate(d)||exact==null||exact !in 1..36000||minValue==null||minValue !in 1..600||roundValue==null||roundValue !in 1..1000||
                    workValue==null||workValue !in 1..3600||restValue==null||restValue !in 0..3600||
                    !heartOK(a)||!heartOK(m)||!optionalDistanceOK(distance)||!optionalMinutesOK(wu)||!optionalMinutesOK(cd)||
                    !optionalResistanceOK(workRes)||!optionalResistanceOK(restRes)) vm.tell(context.tr(R.string.ui_zkontroluj_opravene_hodnoty_33180))
                else {
                    vm.updateHiit(record.copy(date=d,minutes=minValue,rounds=roundValue,work=workValue,rest=restValue,
                        avg=optionalHeart(a),max=optionalHeart(m),note=n,km=distance.asNumber(),
                        warmup=wu.toIntOrNull(),cooldown=cd.toIntOrNull(),workResistance=workRes.toIntOrNull(),restResistance=restRes.toIntOrNull(),activity=editedActivity,totalSeconds=exact))
                    editing=null
                }
            }){Text(context.tr(R.string.ui_ulozit_85e82))}},
            dismissButton={TextButton(onClick={editing=null}){Text(context.tr(R.string.ui_zrusit_9bd5e))}}
        )
    }
}
