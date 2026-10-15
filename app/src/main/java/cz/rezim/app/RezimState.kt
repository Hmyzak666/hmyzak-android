package cz.rezim.app

import android.app.Application
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import java.time.LocalDate
import java.time.DayOfWeek

enum class Page(val title: String, val short: String) {
    HOME("Přehled", "Domů"), STRENGTH("Síla", "Síla"), RUN("Běh", "Běh"), HIIT("HIIT", "HIIT"), BODY("Míry", "Míry")
}

class RezimState(application: Application) : AndroidViewModel(application) {
    private val db=LocalStore(application)
    var page by mutableStateOf(Page.HOME)
    var runFormRequest by mutableIntStateOf(0)
        private set
    var hiitFormRequest by mutableIntStateOf(0)
        private set
    var days by mutableStateOf<List<DayPlan>>(emptyList())
        private set
    fun plan(key:String):DayPlan = days.first {it.key==key}
    fun partner(key:String):String? = plan(selectedDay).exercises.firstOrNull{it.key==key}?.partnerKey
    fun startRunForm() { page=Page.RUN;runFormRequest++ }
    fun startHiitForm() { page=Page.HIIT;hiitFormRequest++ }
    var selectedDay by mutableStateOf(when(LocalDate.now().dayOfWeek) {
        DayOfWeek.TUESDAY -> "tue"
        DayOfWeek.WEDNESDAY -> "wed"
        DayOfWeek.FRIDAY -> "fri"
        DayOfWeek.SUNDAY -> "sun"
        else -> "mon"
    })
        private set
    var session by mutableStateOf<Session?>(null)
        private set
    val workoutSets = mutableStateListOf<SetEntry>()
    val previous = mutableStateMapOf<String,SetEntry?>()
    var runs by mutableStateOf<List<RunEntry>>(emptyList())
        private set
    var hiit by mutableStateOf<List<HiitEntry>>(emptyList())
        private set
    var measures by mutableStateOf<List<MeasureEntry>>(emptyList())
        private set
    var recentWorkouts by mutableStateOf<List<Session>>(emptyList())
        private set
    var totalWorkouts by mutableIntStateOf(0)
        private set
    var message by mutableStateOf<String?>(null)
        private set
    init { refresh(); selectDay(selectedDay) }
    fun clearMessage() { message=null }
    fun tell(text:String) { message=text }
    fun refresh() {
        days=db.plans()
        runs=db.runs(); hiit=db.hiit(); measures=db.measures()
        recentWorkouts=db.recentSessions(); totalWorkouts=db.countSessions()
    }
    fun selectDay(day:String) {
        if(days.none{it.key==day}) { if(days.isNotEmpty()) selectDay(days.first().key); return }
        selectedDay=day; session=db.openSession(day)
        workoutSets.clear()
        session?.let { workoutSets.addAll(db.sets(it.id)) }
        previous.clear()
        for (ex in plan(day).exercises) for (n in 1..ex.sets) {
            previous["${ex.key}:$n"] = db.previous(day,ex.key,n)
        }
    }
    fun start() { if(plan(selectedDay).exercises.isEmpty()) {tell("Nejdřív přidej cvik.");return};session=db.beginWorkout(selectedDay); selectDay(selectedDay) }
    fun createDay(name:String) {
        if(name.isBlank()) {tell("Zadej název dne.");return}
        if(days.size>=20) {tell("Limit je 20 šablon.");return}
        val id=db.addDay(name);refresh();selectDay(id);tell("Tréninkový den přidán.")
    }
    fun addExercise(name:String,sets:Int,type:UnitType) {
        if(session!=null) {tell("Před úpravou dokonči rozepsaný trénink.");return}
        if(name.isBlank() || sets !in 1..15) {tell("Zadej název a 1–15 sérií.");return}
        db.addExercise(selectedDay,name,sets,type);refresh();selectDay(selectedDay)
    }
    fun moveExercise(key:String,offset:Int) {
        if(session!=null) {tell("Před úpravou dokonči rozepsaný trénink.");return}
        db.moveExercise(selectedDay,key,offset);refresh();selectDay(selectedDay)
    }
    fun removeExercise(key:String) {
        if(session!=null) {tell("Před úpravou dokonči rozepsaný trénink.");return}
        db.removeExercise(selectedDay,key);refresh();selectDay(selectedDay)
    }
    fun deleteDay() {
        if(session!=null) {tell("Před odstraněním dokonči rozepsaný trénink.");return}
        if(days.size<=1) {tell("Musí zůstat alespoň jeden tréninkový den.");return}
        val old=selectedDay;db.removeDay(old);refresh();selectDay(days.first().key)
    }
    fun updatePair(a:String,b:String?) {
        if(session!=null) {tell("Před úpravou dokonči rozepsaný trénink.");return}
        db.pair(selectedDay,a,b);refresh();selectDay(selectedDay)
    }
    fun exerciseTrend(key:String,showWeight:Boolean):List<Pair<String,Double>> {
        val sessions=db.recentSessions(400).filter {it.day==selectedDay}.asReversed()
        return sessions.mapNotNull { sess ->
            val sets=db.sets(sess.id).filter {it.exercise==key && it.done}
            if(sets.isEmpty()) null else {
                val max=sets.mapNotNull {it.weight.asNumber()}.maxOrNull() ?: 0.0
                val total=sets.sumOf {it.reps.toIntOrNull() ?: 0}
                val date=java.time.Instant.ofEpochMilli(sess.finished?:sess.created)
                    .atZone(java.time.ZoneId.systemDefault()).toLocalDate().toString()
                date to (if(showWeight) max else total.toDouble())
            }
        }.takeLast(16)
    }
    fun shareSummary():String = buildString {
        appendLine("HMYZÁK · tréninkový výpis (jen čtení, sdílený uživatelem)")
        appendLine("Datum exportu: ${currentDate()}")
        appendLine("Poslední silové tréninky:")
        val sessions=db.recentSessions(7)
        if(sessions.isEmpty()) appendLine("Zatím žádné dokončené silové tréninky.")
        sessions.forEach {s ->
            val date=java.time.Instant.ofEpochMilli(s.finished?:s.created)
                .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
            appendLine("${date} | ${days.firstOrNull{it.key==s.day}?.name ?: s.day}")
            db.sets(s.id).filter{it.done}.groupBy{it.exercise}.forEach { (key,sets) ->
                val name=days.flatMap{it.exercises}.firstOrNull{it.key==key}?.name ?: key
                appendLine("- $name: ${sets.joinToString("; "){it.weight.takeIf{w->w.isNotBlank()}?.let{w->"${w} kg × "}.orEmpty()+it.reps+" op."}}")
            }
        }
        appendLine("Posledních 5 běhů:")
        runs.take(5).forEach {r->appendLine("${r.date}: ${fmtNumber(r.km)} km, ${r.seconds}s, tep Ø ${r.avg?:"–"}, max ${r.max?:"–"}")}
        appendLine("Poslední 3 HIIT:")
        hiit.take(3).forEach{r->appendLine("${r.date}: ${r.minutes} min, ${r.rounds} kol, tep Ø ${r.avg?:"–"}, max ${r.max?:"–"}")}
        appendLine("Poslední 3 měření:")
        measures.take(3).forEach{m->appendLine("${m.date}: ${fmtNumber(m.kg)} kg, pas ${fmtNumber(m.waist)} cm")}
    }
    fun changeSet(entry: SetEntry, weight: String?=null, reps: String?=null) {
        val idx=workoutSets.indexOfFirst { it.id==entry.id }
        if(idx<0) return
        val base=workoutSets[idx]
        val next=base.copy(weight=weight ?: base.weight, reps=reps ?: base.reps)
        workoutSets[idx]=next
        db.updateSet(next)
    }
    fun toggle(entry: SetEntry, type: UnitType):Boolean {
        val idx=workoutSets.indexOfFirst { it.id==entry.id }
        if(idx<0) return false
        val current=workoutSets[idx]
        if (!current.done) {
            val r=current.reps.toIntOrNull()
            if(r==null || r<=0) { tell("Nejdřív zadej počet opakování."); return false }
            if(type==UnitType.KG && (current.weight.asNumber() ?: 0.0)<=0.0) { tell("Zadej hmotnost."); return false }
            if(type==UnitType.ADDED_KG && (current.weight.asNumber() ?: -1.0)<0.0) { tell("Zadej přidanou zátěž (0 kg bez zátěže)."); return false }
        }
        val next=current.copy(done=!current.done)
        workoutSets[idx]=next; db.updateSet(next)
        return next.done
    }
    fun finish() {
        val s=session ?: return
        if(!workoutSets.any { it.done }) { tell("Nejdřív dokonči alespoň jednu sérii."); return }
        db.finishWorkout(s.id)
        selectDay(selectedDay); refresh(); tell("Trénink uložen.")
    }
    fun addRun(date:String,km:Double,seconds:Int,avg:Int?,max:Int?,note:String) {
        db.addRun(date,km,seconds,avg,max,note); refresh(); tell("Běh uložen.")
    }
    fun addHiit(date:String,minutes:Int,rounds:Int,work:Int,rest:Int,avg:Int?,max:Int?,note:String) {
        db.addHiit(date,minutes,rounds,work,rest,avg,max,note); refresh(); tell("HIIT uložen.")
    }
    fun addMeasure(date:String,kg:Double,waist:Double) {
        db.addMeasure(date,kg,waist); refresh(); tell("Měření uloženo.")
    }
    fun exportBackup():String = db.exportJson()
    fun importBackup(text:String) {
        db.importJson(text); refresh(); selectDay(selectedDay)
        tell("Záloha obnovena.")
    }
    override fun onCleared() { db.close(); super.onCleared() }
}
