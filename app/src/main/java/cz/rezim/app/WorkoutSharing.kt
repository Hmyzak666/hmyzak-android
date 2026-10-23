package cz.rezim.app

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.*
import androidx.core.content.FileProvider
import androidx.core.graphics.createBitmap
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
internal fun LoadConventionPicker(vm:RezimState,exercise:Exercise) {
    val context=androidx.compose.ui.platform.LocalContext.current

    if(exercise.type==UnitType.REPS) return
    var expanded by remember {mutableStateOf(false)}
    Box {
        TextButton(onClick={expanded=true},contentPadding=PaddingValues(0.dp)) {
            Text(if(exercise.loadMultiplier==2) context.tr(R.string.ui_objem_dve_cinky_zapsana_vaha_5a093) else context.tr(R.string.ui_objem_celkova_zapsana_zatez_9e15f),fontSize=11.sp)
        }
        DropdownMenu(expanded=expanded,onDismissRequest={expanded=false}) {
            DropdownMenuItem(text={Text(context.tr(R.string.ui_zapsana_vaha_je_celkova_2c68d))},onClick={vm.setLoadMultiplier(exercise.key,1);expanded=false})
            DropdownMenuItem(text={Text(context.tr(R.string.ui_zapsana_vaha_je_jedna_ze_dvou_f71bc))},onClick={vm.setLoadMultiplier(exercise.key,2);expanded=false})
        }
    }
}

@Composable
internal fun WorkoutSummaryDialog(vm:RezimState,record:Session,onShare:(Session,Boolean)->Unit,onDismiss:()->Unit) {
    val context=androidx.compose.ui.platform.LocalContext.current

    val totals=vm.totals(record)
    AlertDialog(onDismissRequest=onDismiss,title={Text(context.tr(R.string.ui_trenink_ulozen_3791c))},text={
        Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text(vm.days.firstOrNull {it.key==record.day}?.name?:record.day,fontWeight=FontWeight.Bold)
            Text(context.tr(R.string.ui_s_min_s_serii_s_opakovani_f8b0d ,record.elapsedSeconds()/60,totals.sets,totals.reps))
            Text(context.tr(R.string.ui_nazvedano_s_kg_d79c5 ,fmtDisplay(totals.liftedKg)),fontSize=22.sp,color=StrengthGreen,fontWeight=FontWeight.Bold)
            Text(context.tr(R.string.ui_soucet_dokoncenych_serii_zatez_opakovani_bez_83965),fontSize=12.sp,color=SecondaryText)
            OutlinedButton(onClick={onShare(record,true)},modifier=Modifier.fillMaxWidth()) {Text(context.tr(R.string.ui_sdilet_obrazek_pribeh_a3d8f))}
            OutlinedButton(onClick={onShare(record,false)},modifier=Modifier.fillMaxWidth()) {Text(context.tr(R.string.ui_sdilet_obrazek_ctverec_352f6))}
        }
    },confirmButton={TextButton(onClick=onDismiss) {Text(context.tr(R.string.ui_hotovo_09526))}})
}

internal data class WorkoutCardData(val name:String,val date:String,val duration:String,val totals:WorkoutTotals)

internal fun createWorkoutCard(context:Context,data:WorkoutCardData,story:Boolean):Bitmap {
    val height=if(story)1920 else 1080
    val bitmap=createBitmap(1080,height,Bitmap.Config.ARGB_8888)
    val canvas=Canvas(bitmap)
    val paint=Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    paint.shader=LinearGradient(0f,0f,1080f,height.toFloat(),intArrayOf(Color.rgb(2,8,3),Color.rgb(13,30,18),Color.BLACK),null,Shader.TileMode.CLAMP)
    canvas.drawRect(0f,0f,1080f,height.toFloat(),paint);paint.shader=null
    val top=if(story)250f else 65f
    val bottom=height-top
    paint.color=Color.rgb(94,214,117);paint.style=Paint.Style.STROKE;paint.strokeWidth=2f
    canvas.drawRoundRect(RectF(40f,top,1040f,bottom),42f,42f,paint);paint.style=Paint.Style.FILL
    val icon=BitmapFactory.decodeResource(context.resources,R.drawable.ic_liftant_mascot)
    if(icon!=null) {canvas.drawBitmap(icon,null,RectF(82f,top+40,252f,top+210),paint);icon.recycle()}
    fun line(value:String,y:Float,size:Float,color:Int=Color.WHITE,bold:Boolean=false) {
        paint.color=color;paint.textSize=size;paint.isFakeBoldText=bold
        // Fitted text is measured, never truncated by character count or outside the card.
        while(paint.measureText(value)>890f && paint.textSize>18f) paint.textSize-=1f
        val shown=if(paint.measureText(value)>890f) value.take(paint.breakText(value,true,850f,null))+"…" else value
        canvas.drawText(shown,95f,y,paint)
    }
    paint.color=Color.rgb(153,241,95);paint.textSize=64f;paint.isFakeBoldText=true
    canvas.drawText("LiftAnt",285f,top+145,paint)
    val titleY=top+300f
    line(data.name,titleY,58f,bold=true)
    line(data.date,titleY+55f,29f,Color.rgb(159,181,163))
    val hero=if(story)titleY+240f else titleY+170f
    line(context.tr(R.string.ui_nazvedana_hmotnost_22d2f),hero-72f,30f,Color.rgb(159,181,163))
    val amount=java.text.NumberFormat.getNumberInstance(context.resources.configuration.locales[0]).apply {maximumFractionDigits=1}.format(data.totals.liftedKg)
    line("$amount kg",hero+34f,104f,Color.rgb(153,241,95),true)
    val stats=if(story)hero+210f else hero+130f
    line(data.duration+" · "+data.totals.sets+context.tr(R.string.ui_serii_c95d3),stats,40f)
    line(data.totals.reps.toString()+context.tr(R.string.ui_opakovani_f9bbc),stats+65f,36f)
    line(context.tr(R.string.ui_trenink_ulozen_dalsi_krok_je_na_b3543),bottom-105f,27f,Color.rgb(159,181,163))
    line(context.tr(R.string.ui_liftant_offline_treninkovy_denik_e0f26),bottom-60f,25f,Color.rgb(94,214,117))
    return bitmap
}

internal fun shareWorkoutImage(context:Context,vm:RezimState,record:Session,story:Boolean) {
    try {
        require(record.finished!=null)
        val date=Instant.ofEpochMilli(record.finished).atZone(ZoneId.systemDefault()).toLocalDate()
        val data=WorkoutCardData(vm.days.firstOrNull {it.key==record.day}?.name?:record.day,
            date.format(DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM)),
            "${record.elapsedSeconds()/60} min",vm.totals(record))
        val bitmap=createWorkoutCard(context,data,story)
        val dir=File(context.cacheDir,"shared_exports").apply {mkdirs()}
        val file=File(dir,"liftant-workout-${record.id}-${System.nanoTime()}.png")
        try {file.outputStream().use {bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}} finally {bitmap.recycle()}
        val uri=FileProvider.getUriForFile(context,"${context.packageName}.fileprovider",file)
        val intent=Intent(Intent.ACTION_SEND).apply {
            type="image/png";putExtra(Intent.EXTRA_STREAM,uri)
            clipData=ClipData.newUri(context.contentResolver,"LiftAnt",uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent,context.tr(R.string.ui_sdilet_trenink_4ad92)))
    } catch(e:Exception) {vm.tell(context.tr(R.string.ui_kartu_se_nepodarilo_sdilet_s_428c5 ,e.localizedMessage))}
}
