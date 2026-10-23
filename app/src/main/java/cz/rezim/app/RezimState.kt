package cz.rezim.app

import android.app.Application
import androidx.core.content.edit
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import java.time.LocalDate
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import kotlin.math.roundToInt

enum class Page(val title: String, val short: String) {
    HOME("Přehled", "Domů"), STRENGTH("Síla", "Síla"), RUN("Běh", "Běh"), HIIT("HIIT", "HIIT"), BODY("Míry", "Míry"), SETTINGS("Nastavení", "Nast.")
}

class RezimState(application: Application) : AndroidViewModel(application) {
    private val db=LocalStore(application)
    private val settings=application.getSharedPreferences("hmyzak_prefs",android.content.Context.MODE_PRIVATE)
    // 0 = compact, 1 = balanced, 2 = studio. Saved locally, no account required.
    var designMode by mutableIntStateOf(settings.getInt("design",2).coerceIn(0,2))
        private set
    var hapticEnabled by mutableStateOf(settings.getBoolean("haptic",true))
        private set
    var autoScrollEnabled by mutableStateOf(settings.getBoolean("auto_scroll",true))
        private set
    fun setDesign(value:Int) {designMode=value.coerceIn(0,2);settings.edit { putInt("design",designMode) }}
    fun setHaptic(value:Boolean) {hapticEnabled=value;settings.edit { putBoolean("haptic",value) }}
    fun setAutoScroll(value:Boolean) {autoScrollEnabled=value;settings.edit { putBoolean("auto_scroll",value) }}
    private val restRepository = RestTimerRepository(application)
    internal var restTimer by mutableStateOf(restRepository.read())
        private set
    var restSeconds by mutableIntStateOf(settings.getInt("rest_seconds",90).coerceIn(1,MAX_REST_SECONDS))
        private set
    internal var restStartMode by mutableStateOf(runCatching {
        RestStartMode.valueOf(settings.getString("rest_mode",null) ?: "MANUAL")
    }.getOrDefault(RestStartMode.MANUAL))
        private set
    var restHapticEnabled by mutableStateOf(settings.getBoolean("rest_haptic",true))
        private set
    private val restListener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _,_ ->
        restTimer = restRepository.read()
        restRepository.error()?.let { tell(it) }
    }
    fun setRestDuration(value:Int) {
        restSeconds=value.coerceIn(1,MAX_REST_SECONDS)
        settings.edit { putInt("rest_seconds",restSeconds) }
    }
    internal fun chooseRestStartMode(value:RestStartMode) {
        restStartMode=value;settings.edit { putString("rest_mode",value.name) }
    }
    fun setRestHaptic(value:Boolean) { restHapticEnabled=value;settings.edit {putBoolean("rest_haptic",value)} }
    fun startRest(sourceSetId:Long=0) {
        val active=session ?: run {tell("Nejdřív zahaj trénink.");return}
        if(!RestTimerCommands.send(getApplication(),RestTimerCommands.START,active.id,restSeconds,sourceSetId))
            tell("Časovač se nepodařilo spustit. Zkus ho znovu otevřít v aplikaci.")
    }
    internal fun restCommand(action:String,seconds:Int=0) {
        if(!RestTimerCommands.send(getApplication(),action,seconds=seconds)) tell("Časovač se nepodařilo změnit.")
    }
    fun cancelRest() { RestTimerCommands.cancel(getApplication());restTimer=RestCountdown() }
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
    var editingSavedWorkout by mutableStateOf<Session?>(null)
        private set
    val editingSavedSets = mutableStateListOf<SetEntry>()
    fun beginEditSavedWorkout(record:Session) {
        if(session!=null) {tell("Nejdřív dokonči nebo ulož rozpracovaný trénink.");return}
        if(record.finished==null) return
        editingSavedWorkout=record
        editingSavedSets.clear();editingSavedSets.addAll(db.sets(record.id))
        page=Page.STRENGTH
    }
    fun setSavedEntry(entry:SetEntry,weight:String?=null,reps:String?=null,done:Boolean?=null) {
        val i=editingSavedSets.indexOfFirst{it.id==entry.id}
        if(i<0) return
        val old=editingSavedSets[i]
        editingSavedSets[i]=old.copy(weight=weight?:old.weight,reps=reps?:old.reps,done=done?:old.done)
    }
    fun cancelSavedEdit() {editingSavedWorkout=null;editingSavedSets.clear()}
    fun commitSavedEdit() {
        val record=editingSavedWorkout?:return
        try {
            db.saveFinishedWorkout(record.id,editingSavedSets.toList())
            cancelSavedEdit();refresh();tell("Uložený trénink opraven.")
        } catch(e:Exception) {tell(e.localizedMessage?:"Změny se nepodařilo uložit.")}
    }
    fun updateRun(r:RunEntry) {db.updateRun(r);refresh();tell("Běh opraven.")}
    fun updateHiit(r:HiitEntry) {db.updateHiit(r);refresh();tell("HIIT opraven.")}
    fun updateMeasure(r:MeasureEntry):Boolean = try {
        db.updateMeasure(r);refresh();tell("Měření opraveno.");true
    } catch(e:Exception) {tell(e.localizedMessage?:"Měření se nepodařilo uložit.");false}

    var runs by mutableStateOf<List<RunEntry>>(emptyList())
        private set
    var hiit by mutableStateOf<List<HiitEntry>>(emptyList())
        private set
    var measures by mutableStateOf<List<MeasureEntry>>(emptyList())
        private set
    var bodyMetrics by mutableStateOf<List<BodyMetric>>(emptyList())
        private set
    var recentWorkouts by mutableStateOf<List<Session>>(emptyList())
        private set
    var totalWorkouts by mutableIntStateOf(0)
        private set
    var message by mutableStateOf<String?>(null)
        private set
    init {
        refresh()
        // Restore an unfinished session after a process kill or a phone reboot.
        selectDay(db.firstOpenSession()?.day ?: selectedDay)
        if(restTimer.sessionId != session?.id) cancelRest()
        restRepository.observe(restListener)
    }
    fun clearMessage() { message=null }
    fun tell(text:String) { message=text }
    fun refresh() {
        days=db.plans()
        runs=db.runs(); hiit=db.hiit(); measures=db.measures();bodyMetrics=db.bodyMetrics()
        recentWorkouts=db.recentSessions(); totalWorkouts=db.countSessions()
    }
    fun selectDay(day:String) {
        if(days.none{it.key==day}) { if(days.isNotEmpty()) selectDay(days.first().key); return }
        val open=db.firstOpenSession()
        if(open!=null && open.day!=day) {
            selectedDay=open.day
            session=open
            tell("Nejdřív dokonči nebo zahoď rozpracovaný trénink.")
            return
        }
        selectedDay=day; session=db.openSession(day)
        workoutSets.clear()
        session?.let { workoutSets.addAll(db.sets(it.id)) }
        previous.clear()
        for (ex in plan(day).exercises) for (n in 1..ex.sets) {
            previous["${ex.key}:$n"] = db.previous(day,ex.key,n)
        }
    }
    fun start() {
        if(plan(selectedDay).exercises.isEmpty()) {tell("Nejdřív přidej cvik.");return}
        val other=db.firstOpenSession()
        if(other!=null && other.day!=selectedDay) {
            selectDay(other.day); tell("Nejprve dokonči nebo zahoď rozpracovaný trénink."); return
        }
        session=db.beginWorkout(selectedDay); selectDay(selectedDay)
    }
    fun discardWorkout() {
        val open=session ?: return
        cancelRest()
        db.deleteOpenWorkout(open.id)
        selectDay(selectedDay);refresh();tell("Rozpracovaný trénink zahozen.")
    }
    fun updateExerciseDetails(key:String,name:String,note:String) {
        if(session!=null) {tell("Úpravy cviků jsou dostupné po ukončení tréninku.");return}
        if(name.isBlank()) {tell("Název cviku nesmí být prázdný.");return}
        db.updateExerciseDetails(selectedDay,key,name,note)
        refresh();selectDay(selectedDay);tell("Název a poznámka uloženy.")
    }
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
    fun changeExerciseUnit(key:String,type:UnitType) {
        if(session!=null) {tell("Před úpravou dokonči rozpracovaný trénink.");return}
        db.changeExerciseUnit(selectedDay,key,type)
        refresh();selectDay(selectedDay);tell("Typ zátěže byl upraven.")
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
    fun updatePair(a:String,b:String?) = updateSuperset(a,listOfNotNull(a,b).distinct())
    fun updateSuperset(anchor:String,selected:List<String>,rounds:Map<String,Int> = emptyMap()):Boolean = try {
        db.setSuperset(selectedDay,anchor,selected,rounds)
        days=db.plans();tell("Supersérie uložena pro tento i další tréninky.");true
    } catch(e:Exception) {tell(e.localizedMessage ?: "Supersérii se nepodařilo uložit.");false}
    fun updatePermanentNote(key:String,note:String):Boolean = try {
        db.updateExerciseNote(selectedDay,key,note)
        days=db.plans();tell("Trvalá poznámka uložena.");true
    } catch(e:Exception) {tell(e.localizedMessage ?: "Poznámku se nepodařilo uložit.");false}
    data class ExercisePerformance(val date:String, val maxKg:Double, val totalReps:Int,
        val bestEstimated1Rm:Double, val maxReps:Int, val sets:Int)
    // All calculations are done locally from completed sets (no Internet or AI access).
    fun exerciseHistory(key:String):List<ExercisePerformance> = db.allSessions().asReversed().mapNotNull { sess ->
        val sets=db.sets(sess.id).filter {it.exercise==key && it.done}
        if(sets.isEmpty()) null else {
            val date=Instant.ofEpochMilli(sess.finished?:sess.created)
                .atZone(ZoneId.systemDefault()).toLocalDate().toString()
            val best=sets.mapNotNull {set ->
                val kg=set.weight.asNumber()?:return@mapNotNull null
                val reps=set.reps.toIntOrNull()?:return@mapNotNull null
                val type=days.flatMap{it.exercises}.firstOrNull{it.key==key}?.type
                if(type!=UnitType.KG || kg<=0.0 || reps !in 1..30) null else kg*(1.0+reps/30.0)
            }.maxOrNull()?:0.0
            ExercisePerformance(date,sets.mapNotNull{it.weight.asNumber()}.maxOrNull()?:0.0,
                sets.sumOf{it.reps.toIntOrNull()?:0},best,
                sets.mapNotNull{it.reps.toIntOrNull()}.maxOrNull()?:0,sets.size)
        }
    }
    fun exerciseTrend(key:String,metric:Int=0,months:Int=6):List<Pair<String,Double>> {
        val threshold=LocalDate.now().minusMonths(months.toLong())
        return exerciseHistory(key).filter {runCatching {LocalDate.parse(it.date)>=threshold}.getOrDefault(false)}
            .map{ it.date to when(metric) {1->it.totalReps.toDouble();2->it.bestEstimated1Rm;else->it.maxKg} }
    }
    fun exerciseRecordText(key:String):String {
        val history=exerciseHistory(key)
        if(history.isEmpty()) return "Ještě nejsou dokončené série."
        val weight=history.maxOf{it.maxKg}
        val reps=history.maxOf{it.maxReps}
        val estimated=history.maxOf{it.bestEstimated1Rm}
        return buildString {
            append("Nejvyšší zátěž: ${fmtNumber(weight)} kg · Nejvíc opakování: $reps")
            if(estimated>0) append(" · Odhad 1RM: ${fmtNumber(estimated)} kg")
        }
    }
    fun isLatestRecord(key:String):Boolean {
        val h=exerciseHistory(key)
        if(h.size<2) return false
        val latest=h.last();val older=h.dropLast(1)
        if(latest.maxKg>older.maxOf{it.maxKg} ||
            (latest.bestEstimated1Rm>0 && latest.bestEstimated1Rm>older.maxOf{it.bestEstimated1Rm})) return true
        // Repetition PRs are comparable only at the same external load.
        val previousSessions=db.allSessions().asReversed().filter{sess ->
            db.sets(sess.id).any{it.exercise==key && it.done}
        }
        val latestSession=previousSessions.lastOrNull()?:return false
        val earlier=previousSessions.dropLast(1).flatMap{db.sets(it.id)}
            .filter{it.exercise==key && it.done}
        return db.sets(latestSession.id).filter{it.exercise==key && it.done}.any {set ->
            val reps=set.reps.toIntOrNull()?:0
            val weight=set.weight.asNumber()?:0.0
            val matching=earlier.filter{it.weight.asNumber()==weight}
            matching.isNotEmpty() && reps>(matching.mapNotNull{it.reps.toIntOrNull()}.maxOrNull()?:0)
        }
    }
    fun setWorkoutDuration(id:Long, minutes:Int):Boolean = try {
        require(minutes in 1..2880) { "Zadej délku 1 až 2880 minut." }
        db.updateWorkoutDuration(id,minutes*60)
        session=db.firstOpenSession();refresh();tell("Délka tréninku upravena.");true
    } catch(e:Exception) {tell(e.localizedMessage ?: "Délku se nepodařilo uložit.");false}
    enum class ExportScope { RECENT, PREVIOUS_WEEK, ALL }

    // Monday morning export: previous Mon-Sun workouts, plus current Monday's check-in.
    fun shareSummary(scope:ExportScope=ExportScope.RECENT):String = buildString {
        val today=LocalDate.now()
        val thisMonday=today.minusDays((today.dayOfWeek.value-1).toLong())
        val from=thisMonday.minusDays(7)
        val until=thisMonday.minusDays(1)
        val finishedSessions=when(scope) {
            ExportScope.RECENT -> db.recentSessions(7)
            ExportScope.PREVIOUS_WEEK -> db.allSessions().filter {
                val date=java.time.Instant.ofEpochMilli(it.finished?:it.created)
                    .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                !date.isBefore(from) && !date.isAfter(until)
            }
            ExportScope.ALL -> db.allSessions()
        }
        fun dateInWeek(date:String):Boolean = runCatching {
            val d=LocalDate.parse(date);!d.isBefore(from) && !d.isAfter(until)
        }.getOrDefault(false)
        val chosenRuns=when(scope) {
            ExportScope.RECENT->runs.take(5)
            ExportScope.PREVIOUS_WEEK->runs.filter{dateInWeek(it.date)}
            ExportScope.ALL->runs
        }
        val chosenHiit=when(scope) {
            ExportScope.RECENT->hiit.take(3)
            ExportScope.PREVIOUS_WEEK->hiit.filter{dateInWeek(it.date)}
            ExportScope.ALL->hiit
        }
        val chosenMeasures=when(scope) {
            ExportScope.RECENT->measures.take(3)
            ExportScope.PREVIOUS_WEEK->measures.filter {dateInWeek(it.date) || (today.dayOfWeek==DayOfWeek.MONDAY && it.date==today.toString())}
            ExportScope.ALL->measures
        }
        appendLine("HMYZÁK · tréninkový výpis (jen čtení, sdílený uživatelem)")
        appendLine("Datum exportu: $today")
        appendLine(when(scope) {
            ExportScope.RECENT -> "Rozsah: poslední záznamy"
            ExportScope.PREVIOUS_WEEK -> "Rozsah: minulý týden $from až $until; plus pondělní měření, pokud existuje"
            ExportScope.ALL -> "Rozsah: celá historie"
        })
        appendLine("Silové tréninky (${finishedSessions.size}):")
        if(finishedSessions.isEmpty()) appendLine("Žádný dokončený silový trénink.")
        finishedSessions.forEach {sess ->
            val date=java.time.Instant.ofEpochMilli(sess.finished?:sess.created)
                .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
            appendLine("$date | ${days.firstOrNull{it.key==sess.day}?.name ?: sess.day}")
            db.sets(sess.id).filter{it.done}.groupBy{it.exercise}.forEach { (key,sets) ->
                val exercise=days.flatMap{it.exercises}.firstOrNull{it.key==key}
                val name=exercise?.name ?: key
                appendLine("- $name: ${sets.joinToString("; "){set ->
                    val w= when {
                        set.weight=="0" && (exercise?.type==UnitType.ADDED_KG || exercise?.type==UnitType.REPS || key.contains("calf",true)) -> "0 kg (vlastní váha) × "
                        set.weight.isNotBlank() -> "${set.weight} kg × "
                        else -> ""
                    }
                    w+set.reps+" op."
                }}")
            }
        }
        appendLine("Běhy (${chosenRuns.size}):")
        chosenRuns.forEach {r->appendLine("${r.date}: ${fmtNumber(r.km)} km, ${r.seconds}s, tep Ø ${r.avg?:"–"}, max ${r.max?:"–"}${if(r.note.isNotBlank()) ", pozn.: ${r.note}" else ""}")}
        appendLine("HIIT (${chosenHiit.size}):")
        chosenHiit.forEach { r ->
            val extras=buildList {
                r.km?.let{add("${fmtNumber(it)} km")}
                r.warmup?.let{add("zahřátí ${it} min")}
                r.cooldown?.let{add("zklidnění ${it} min")}
                r.workResistance?.let{add("odpor práce $it")}
                r.restResistance?.let{add("odpor odpočinek $it")}
            }.joinToString(", ")
            appendLine("${r.date}: ${r.minutes} min celkem, ${r.rounds} kol, ${r.work}s práce / ${r.rest}s odpočinek, tep Ø ${r.avg?:"–"}, max ${r.max?:"–"}${if(extras.isNotBlank()) ", $extras" else ""}${if(r.note.isNotBlank()) ", pozn.: ${r.note}" else ""}")
        }
        appendLine("Délka silových tréninků (min):")
        finishedSessions.forEach {sess ->appendLine("${sess.day}: ${sess.elapsedSeconds()/60} min")}
        appendLine("Měření (${chosenMeasures.size}):")
        chosenMeasures.forEach { m ->
            val values=buildList {
                m.kg?.let { add("${fmtNumber(it)} kg") }
                m.waist?.let { add("břicho ${fmtNumber(it)} cm") }
                bodyMetrics.forEach { metric -> m.extra[metric.id]?.let { add("${metric.name} ${fmtNumber(it)} cm") } }
            }
            appendLine("${m.date}: ${values.joinToString(", ")}")
        }
    }
    fun changeSet(entry: SetEntry, weight: String?=null, reps: String?=null) {
        val idx=workoutSets.indexOfFirst { it.id==entry.id }
        if(idx<0) return
        val base=workoutSets[idx]
        val next=base.copy(weight=weight ?: base.weight, reps=reps ?: base.reps)
        workoutSets[idx]=next
        db.updateSet(next)
    }
    // null = validation failed; false = successfully undone; true = completed.
    fun toggle(entry: SetEntry, type: UnitType):Boolean? {
        val idx=workoutSets.indexOfFirst { it.id==entry.id }
        if(idx<0) return null
        val current=workoutSets[idx]
        if (!current.done) {
            val r=current.reps.toIntOrNull()
            if(r==null || r<=0) { tell("Nejdřív zadej počet opakování."); return null }
            if(type==UnitType.KG && (current.weight.asNumber() ?: -1.0)<0.0) { tell("Zadej zátěž (0 kg = vlastní váha)."); return null }
            if(type==UnitType.ADDED_KG && (current.weight.asNumber() ?: -1.0)<0.0) { tell("Zadej přidanou zátěž (0 kg bez zátěže)."); return null }
        }
        val next=current.copy(done=!current.done)
        db.updateSet(next);workoutSets[idx]=next
        if(next.done) {
            cancelRest()
            if(shouldStartRest(restStartMode,plan(selectedDay).exercises,workoutSets.toList(),next.id)) startRest(next.id)
        } else if(restTimer.sourceSetId == next.id) cancelRest()
        return next.done
    }
    fun minusOneRep(entry:SetEntry) {
        val current=workoutSets.firstOrNull{it.id==entry.id}?:return
        if(current.done) {tell("Nejdřív vrať sérii mezi nedokončené.");return}
        val reps=(current.reps.toIntOrNull()?:0).coerceAtLeast(0)
        changeSet(current,reps=(reps-1).coerceAtLeast(0).toString())
    }
    fun adjustWeight(entry:SetEntry,delta:Double) {
        val current=workoutSets.firstOrNull{it.id==entry.id}?:return
        if(current.done) {tell("Nejdřív vrať sérii mezi nedokončené.");return}
        val next=((current.weight.asNumber()?:0.0)+delta).coerceAtLeast(0.0)
        changeSet(current,weight=fmtNumber(next))
    }
    fun plusOneRep(entry:SetEntry) {
        val current=workoutSets.firstOrNull{it.id==entry.id} ?: return
        if(current.done) {tell("Nejdřív vrať sérii mezi nedokončené.");return}
        val currentReps=current.reps.toIntOrNull() ?: 0
        if(currentReps>=9999) {tell("Maximum je 9999 opakování.");return}
        changeSet(current,reps=(currentReps+1).toString())
    }
    fun addWorkoutSet(exercise:Exercise) {
        val s=session?:return
        val last=workoutSets.filter{it.exercise==exercise.key}.maxByOrNull{it.number}
        try {
            val added=db.addOpenSet(s.id,exercise.key,last)
            workoutSets.add(added)
            previous["${exercise.key}:${added.number}"]=db.previous(selectedDay,exercise.key,added.number)
        } catch(e:Exception){tell(e.localizedMessage?:"Sérii nelze přidat.")}
    }
    fun removeWorkoutSet(exercise:Exercise) {
        val s=session?:return
        val entries=workoutSets.filter{it.exercise==exercise.key}
        if(entries.size<=1){tell("Musí zůstat alespoň jedna série.");return}
        val last=entries.maxByOrNull{it.number}?:return
        try {
            db.removeLastOpenSet(s.id,last)
            workoutSets.removeAll{it.id==last.id}
            previous.remove("${exercise.key}:${last.number}")
        }catch(e:Exception){tell(e.localizedMessage?:"Sérii nelze odebrat.")}
    }
    fun finish() {
        val s=session ?: return
        if(!workoutSets.any { it.done }) { tell("Nejdřív dokonči alespoň jednu sérii."); return }
        cancelRest()
        db.finishWorkout(s.id)
        selectDay(selectedDay); refresh(); tell("Trénink uložen.")
    }
    fun addRun(date:String,km:Double,seconds:Int,avg:Int?,max:Int?,note:String) {
        db.addRun(date,km,seconds,avg,max,note); refresh(); tell("Běh uložen.")
    }
    fun addHiit(record:HiitEntry) {
        db.addHiit(record);refresh();tell("HIIT uložen.")
    }
    internal fun addMeasure(date:String,values:BodyMeasurementInput):Boolean = try {
        db.addMeasure(date,values.kg,values.waist,values.extra);refresh();tell("Měření uloženo.");true
    } catch(e:Exception) {tell(e.localizedMessage?:"Měření se nepodařilo uložit.");false}
    fun saveBodyMetric(name:String,id:String?=null):Boolean = try {
        db.saveBodyMetric(name,id);refresh();tell("Vlastní míra uložena.");true
    } catch(e:Exception) {tell(e.localizedMessage?:"Míru se nepodařilo uložit.");false}
    fun exportBackup():String = db.exportJson()
    fun importBackup(text:String) {
        db.importJson(text);cancelRest();refresh();selectDay(selectedDay)
        tell("Záloha obnovena.")
    }
    override fun onCleared() { restRepository.removeObserver(restListener);db.close();super.onCleared() }
}
