package cz.rezim.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

internal data class TrainingSnapshot(val session:Session,val totals:WorkoutTotals)
internal data class PeriodStats(val workouts:Int,val sets:Int,val reps:Long,val liftedKg:Double,val seconds:Long)

internal fun periodStats(records:List<TrainingSnapshot>,from:LocalDate,through:LocalDate):PeriodStats {
    val selected=records.filter {r -> r.session.finished?.let {finished ->
        val d=Instant.ofEpochMilli(finished).atZone(ZoneId.systemDefault()).toLocalDate()
        !d.isBefore(from) && !d.isAfter(through)
    }?:false}
    return PeriodStats(selected.size,selected.sumOf {it.totals.sets},selected.sumOf {it.totals.reps},
        selected.sumOf {it.totals.liftedKg},selected.sumOf {it.session.elapsedSeconds().toLong()})
}

// Quoting handles separators/newlines. A leading apostrophe neutralizes spreadsheet formula cells.
internal fun csvCell(raw:String):String {
    val safe=if(raw.trimStart().firstOrNull() in listOf('=','+','-','@','\t','\r')) "'"+raw else raw
    return "\""+safe.replace("\"","\"\"")+"\""
}

// All advanced views use the existing, reactive Billing entitlement.
@Composable
internal fun ProFeatureContent(vm:RezimState,onUpgrade:()->Unit,tag:String,content:@Composable ()->Unit) {
    val context=LocalContext.current
    if(vm.billing.hasPro) content()
    else Column(Modifier.testTag("pro-locked:$tag"),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Text(context.tr(R.string.pro_analytics_benefits),fontSize=13.sp,color=SecondaryText)
        OutlinedButton(onClick=onUpgrade) {Text(context.tr(R.string.pro_unlock_lifetime))}
        Text(context.tr(R.string.pro_free_records_history),fontSize=12.sp,color=SecondaryText)
    }
}

@Composable
internal fun ProSettings(vm:RezimState) {
    val context=LocalContext.current
    val billing=vm.billing
    SectionCard {
        MutedHeading("LiftAnt Pro")
        Text(context.tr(R.string.ui_pokrocile_statistiky_porovnani_obdobi_a_export_00ed5),fontSize=13.sp)
        Text(context.tr(R.string.ui_jednorazove_odemceni_historie_bezne_zapisovani_casovac_deaf5),fontSize=12.sp,color=SecondaryText)
        if(billing.hasPro) Text(if(billing.testMode) context.tr(R.string.pro_preview) else context.tr(R.string.pro_active),color=StrengthGreen)
        else {
            val activity=context as? android.app.Activity
            Button(onClick={activity?.let {billing.buy(it)}},enabled=activity!=null && billing.price!=null && !billing.busy) {
                Text(billing.price?.let {context.tr(R.string.pro_buy_price,it)}?:context.tr(R.string.pro_store_unavailable))
            }
        }
        TextButton(onClick={billing.refresh()},enabled=billing.playBuild) {Text(context.tr(R.string.ui_obnovit_nakup_3c999))}
        Text(context.tr(R.string.pro_lifetime),fontSize=12.sp,color=SecondaryText)
    }
}

@Composable
internal fun ProStatsCard(vm:RezimState,onCsv:()->Unit,onUpgrade:()->Unit) {
    val context=LocalContext.current
    var length by remember {mutableIntStateOf(30)}
    SectionCard {
        MutedHeading(context.tr(R.string.ui_pokrocile_statistiky_pro_b7a30))
        if(!vm.billing.hasPro) {
            Text(context.tr(R.string.ui_porovnej_objem_serie_a_cas_treninku_37d77),fontSize=13.sp,color=SecondaryText)
            OutlinedButton(onClick=onUpgrade) {Text(context.tr(R.string.pro_show))}
        } else {
            Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(7.dp)) {
                listOf(7,30,90).forEach {days -> FilterChip(selected=length==days,onClick={length=days},label={Text(context.quantity(R.plurals.period_days,days))})}
            }
            val end=LocalDate.now();val start=end.minusDays(length.toLong()-1)
            val oldEnd=start.minusDays(1);val oldStart=start.minusDays(length.toLong())
            val now=periodStats(vm.trainingSnapshots,start,end)
            val old=periodStats(vm.trainingSnapshots,oldStart,oldEnd)
            Text("${shortDate(start.toString())} – ${shortDate(end.toString())}",fontSize=12.sp)
            Text(context.tr(R.string.ui_srovnani_s_s_28f3f ,shortDate(oldStart.toString()),shortDate(oldEnd.toString())),fontSize=11.sp,color=SecondaryText)
            Text(context.tr(R.string.ui_treninky_s_drive_s_9d9b0 ,now.workouts,old.workouts))
            Text(context.tr(R.string.ui_nazvedano_s_kg_drive_s_kg_06c0b ,fmtDisplay(now.liftedKg),fmtDisplay(old.liftedKg)))
            Text(context.tr(R.string.ui_serie_s_drive_s_19274 ,now.sets,old.sets))
            Text(context.tr(R.string.ui_opakovani_s_drive_s_f19e5 ,now.reps,old.reps))
            Text(context.tr(R.string.ui_cas_s_min_drive_s_min_9c13e ,now.seconds/60,old.seconds/60))
            OutlinedButton(onClick=onCsv,modifier=Modifier.fillMaxWidth()) {Text(context.tr(R.string.ui_exportovat_serie_do_csv_201c6))}
        }
    }
}
