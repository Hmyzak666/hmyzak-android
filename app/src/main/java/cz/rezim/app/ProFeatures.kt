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
        ProSamplePreview(tag)
        Text(context.tr(R.string.pro_analytics_benefits),fontSize=13.sp,color=SecondaryText)
        Text(context.tr(R.string.pro_lifetime),fontSize=12.sp,color=SecondaryText)
        OutlinedButton(onClick=onUpgrade) {Text(vm.billing.price?.let {context.tr(R.string.pro_buy_price,it)} ?: context.tr(R.string.pro_unlock_lifetime))}
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
            ProSamplePreview("exercise-progress")
            ProSamplePreview("body-progress")
            ProSamplePreview("period-stats")
            Text(context.tr(R.string.pro_analytics_benefits),fontSize=13.sp,color=SecondaryText)
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
            ProFeatureContent(vm,onUpgrade,"period-stats") {}
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

// Display-only fixtures: never read from or written to the user's records or backups.
@Composable
internal fun ProSamplePreview(kind:String) {
    val context=LocalContext.current
    Column(Modifier.fillMaxWidth().testTag("pro-sample:$kind"),verticalArrangement=Arrangement.spacedBy(6.dp)) {
        Text(context.tr(R.string.pro_sample_label),fontSize=12.sp,color=SecondaryText)
        when(kind) {
            "body-progress" -> {
                Text(context.tr(R.string.pro_sample_weight))
                SparkLine(listOf(80.0,79.8,80.1,79.5,79.3,79.0),BodyPurple,"pro-sample-chart")
            }
            "running-pace" -> {
                Text(context.tr(R.string.pro_sample_pace))
                SparkLine(listOf(6.5,6.3,6.4,6.1,6.0,5.9),RunningBlue,"pro-sample-chart")
            }
            "running-heart-rate", "hiit-heart-rate" -> {
                Text(context.tr(R.string.pro_sample_heart_rate))
                SparkLine(listOf(150.0,148.0,151.0,146.0,145.0,143.0),if(kind=="hiit-heart-rate") HiitOrange else RunningBlue,"pro-sample-chart")
            }
            "period-stats", "workout-sharing" -> {
                Text(context.tr(R.string.pro_sample_period))
                Text(context.tr(R.string.ui_treninky_s_drive_s_9d9b0,12,10))
                Text(context.tr(R.string.ui_nazvedano_s_kg_drive_s_kg_06c0b,fmtDisplay(26400.0),fmtDisplay(22000.0)))
                Text(context.tr(R.string.ui_serie_s_drive_s_19274,144,120))
            }
            else -> {
                Text(context.tr(R.string.pro_sample_exercise))
                SparkLine(listOf(40.0,42.5,42.5,45.0,47.5,50.0),StrengthGreen,"pro-sample-chart")
            }
        }
    }
}
