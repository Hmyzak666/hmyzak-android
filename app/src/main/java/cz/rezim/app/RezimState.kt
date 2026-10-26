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

enum class Page(val title: Int, val short: Int) {
    HOME(R.string.ui_prehled_3864e, R.string.ui_domu_e21b9), STRENGTH(R.string.ui_sila_c50bc, R.string.ui_sila_c50bc), RUN(R.string.cardio_title, R.string.cardio_title), HIIT(R.string.ui_hiit_199e5, R.string.ui_hiit_199e5), BODY(R.string.ui_miry_897e8, R.string.ui_miry_897e8), SETTINGS(R.string.ui_nastaveni_55f52, R.string.ui_nast_ae7bb), HISTORY(R.string.workout_history,R.string.workout_history), WORKOUT_DETAIL(R.string.workout_detail,R.string.workout_detail)
}

class RezimState(application: Application) : AndroidViewModel(application) {
    private val context:android.content.Context get()=AppLanguage.wrap(getApplication())
    internal val billing by lazy {ProBilling(application)}
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
    var restWakeScreenEnabled by mutableStateOf(settings.getBoolean("rest_wake_screen",false))
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
    fun setRestWakeScreen(value:Boolean) {restWakeScreenEnabled=value;settings.edit {putBoolean("rest_wake_screen",value)}}
    internal fun reconcileRest() {
        restTimer=restRepository.read()
        if(restTimer.phase in setOf(RestPhase.RUNNING,RestPhase.PAUSED))
            RestTimerCommands.send(getApplication(),RestTimerCommands.SYNC)
    }
    fun startRest(sourceSetId:Long=0) {
        val active=session ?: run {tell(context.tr(R.string.ui_nejdriv_zahaj_trenink_c6bde));return}
        if(!RestTimerCommands.send(getApplication(),RestTimerCommands.START,active.id,restSeconds,sourceSetId))
            tell(context.tr(R.string.ui_casovac_se_nepodarilo_spustit_zkus_ho_1f331))
    }
    internal fun restCommand(action:String,seconds:Int=0) {
        if(!RestTimerCommands.send(getApplication(),action,seconds=seconds)) tell(context.tr(R.string.ui_casovac_se_nepodarilo_zmenit_b41cd))
    }
    fun cancelRest() { RestTimerCommands.cancel(getApplication());restTimer=RestCountdown() }
    var onboardingRequired by mutableStateOf(db.needsOnboarding())
        private set
    internal fun completeSetup(template:TrainingTemplate?=null) {
        db.completeSetup(template);refresh();selectDay(days.firstOrNull()?.key.orEmpty());page=Page.HOME
    }
    internal fun appendTemplate(template:TrainingTemplate):Boolean = try {
        val keys=db.insertTemplate(template);refresh();selectDay(keys.first());page=Page.STRENGTH;true
    } catch(e:Exception) {tell(e.localizedMessage?:context.tr(R.string.setup_error));false}
    fun renameDay(name:String) {if(name.isNotBlank() && session==null) {db.renameDay(selectedDay,name);refresh()}}
    fun setExerciseSets(exercise:String,count:Int) {
        if(session==null && count in 1..15) {db.setExerciseSets(selectedDay,exercise,count);refresh()}
    }
    var page by mutableStateOf(Page.HOME)
    var runFormRequest by mutableIntStateOf(0)
        private set
    var hiitFormRequest by mutableIntStateOf(0)
        private set
    var bodyFormRequest by mutableIntStateOf(0)
        private set
    var days by mutableStateOf<List<DayPlan>>(emptyList())
        private set
    fun plan(key:String):DayPlan = days.first {it.key==key}
    fun partner(key:String):String? = plan(selectedDay).exercises.firstOrNull{it.key==key}?.partnerKey
    var cardioActivity by mutableStateOf("RUNNING")
        private set
    internal var hiitPreset by mutableStateOf<QuickShortcut?>(null)
        private set
    internal var quickShortcuts by mutableStateOf(QuickShortcuts.read(settings))
        private set
    internal fun saveShortcuts(items:List<QuickShortcut>) {
        val json=QuickShortcuts.encode(items)
        check(settings.edit().putString("quick_shortcuts",json).commit())
        quickShortcuts=items
    }
    internal fun launchShortcut(item:QuickShortcut) {
        when(item.kind) {
            "STRENGTH" -> if(days.any {it.key==item.target}) {selectDay(item.target);page=Page.STRENGTH}
                else tell(context.tr(R.string.shortcut_missing_plan))
            "CARDIO" -> startCardioForm(item.target)
            "HIIT" -> {hiitPreset=item;page=Page.HIIT;hiitFormRequest++}
            "BODY" -> startBodyForm()
        }
    }
    fun startRunForm() = startCardioForm("RUNNING")
    fun startCardioForm(activity:String) {cardioActivity=activity;page=Page.RUN;runFormRequest++}
    fun startHiitForm() {hiitPreset=null;page=Page.HIIT;hiitFormRequest++}
    fun startBodyForm() {page=Page.BODY;bodyFormRequest++}
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
    private var savedEditOrigin=Page.STRENGTH
    internal var viewingWorkoutId by mutableStateOf<Long?>(null)
        private set
    private var workoutDetailOrigin=Page.HOME
    internal val completedWorkouts:List<Session> get()=trainingSnapshots.map {it.session}
    internal fun savedWorkoutSets(id:Long):List<SetEntry> =
        if(completedWorkouts.any {it.id==id}) db.sets(id) else emptyList()
    internal fun exerciseName(key:String):String = days.flatMap {it.exercises}.firstOrNull {it.key==key}?.name
        ?: Plans.days.flatMap {it.exercises}.firstOrNull {it.key==key}?.name ?: key
    internal fun openWorkoutHistory() {page=Page.HISTORY}
    internal fun openWorkout(id:Long) {
        if(completedWorkouts.none {it.id==id}) return
        workoutDetailOrigin=if(page==Page.HISTORY) Page.HISTORY else Page.HOME
        viewingWorkoutId=id;page=Page.WORKOUT_DETAIL
    }
    internal fun leaveWorkoutHistory() {
        page=if(page==Page.WORKOUT_DETAIL) workoutDetailOrigin else Page.HOME
        viewingWorkoutId=null
    }
    fun beginEditSavedWorkout(record:Session) {
        if(session!=null) {tell(context.tr(R.string.ui_nejdriv_dokonci_nebo_uloz_rozpracovany_trenink_eaecd));return}
        if(record.finished==null) return
        savedEditOrigin=page
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
    fun cancelSavedEdit() {
        editingSavedWorkout=null;editingSavedSets.clear()
        if(page==Page.STRENGTH) page=savedEditOrigin
    }
    fun commitSavedEdit(minutes:Int?=null) = commitSavedEditSeconds(minutes?.times(60))
    fun commitSavedEditSeconds(seconds:Int?=null) {
        val record=editingSavedWorkout?:return
        try {
            require(seconds==null || seconds in 1..172800) {context.tr(R.string.ui_zadej_platnou_delku_az_minut_c3179)}
            db.saveFinishedWorkout(record.id,editingSavedSets.toList(),seconds)
            cancelSavedEdit();refresh();tell(context.tr(R.string.ui_ulozeny_trenink_opraven_2ae45))
        } catch(e:Exception) {tell(e.localizedMessage?:context.tr(R.string.ui_zmeny_se_nepodarilo_ulozit_3b5f8))}
    }
    fun updateRun(r:RunEntry) {db.updateRun(r);refresh();tell(context.tr(R.string.ui_beh_opraven_5c949))}
    fun updateHiit(r:HiitEntry) {db.updateHiit(r);refresh();tell(context.tr(R.string.ui_hiit_opraven_1f22f))}
    fun updateMeasure(r:MeasureEntry):Boolean = try {
        db.updateMeasure(r);refresh();tell(context.tr(R.string.ui_mereni_opraveno_750d1));true
    } catch(e:Exception) {tell(e.localizedMessage?:context.tr(R.string.ui_mereni_se_nepodarilo_ulozit_a538f));false}

    var cardio by mutableStateOf<List<RunEntry>>(emptyList())
        private set
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
    internal var workoutVolumes by mutableStateOf<Map<Long,Double>>(emptyMap())
        private set
    internal var trainingSnapshots by mutableStateOf<List<TrainingSnapshot>>(emptyList())
        private set
    val totalLiftedKg:Double get()=workoutVolumes.values.sum()
    val activeLiftedKg:Double get()=workoutTotals(workoutSets.toList(),days.flatMap {it.exercises}).liftedKg
    internal fun totals(record:Session):WorkoutTotals = workoutTotals(db.sets(record.id),days.flatMap {it.exercises})
    fun setLoadMultiplier(exercise:String,value:Int) {
        db.setLoadMultiplier(selectedDay,exercise,value);refresh()
    }
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
        days=db.plans();onboardingRequired=db.needsOnboarding()
        cardio=db.runs();runs=cardio.filter {it.isRunning && it.km>0}; hiit=db.hiit(); measures=db.measures();bodyMetrics=db.bodyMetrics()
        recentWorkouts=db.recentSessions(3); totalWorkouts=db.countSessions()
        val exercises=days.flatMap {it.exercises}
        val grouped=db.finishedSets().groupBy {it.session}
        trainingSnapshots=db.allSessions().map {TrainingSnapshot(it,workoutTotals(grouped[it.id].orEmpty(),exercises))}
        workoutVolumes=trainingSnapshots.associate {it.session.id to it.totals.liftedKg}
    }
    fun selectDay(day:String) {
        if(days.none{it.key==day}) {
            if(days.isNotEmpty()) selectDay(days.first().key)
            else {selectedDay="";session=db.firstOpenSession();workoutSets.clear();previous.clear()}
            return
        }
        val open=db.firstOpenSession()
        if(open!=null && open.day!=day) {
            selectedDay=open.day
            session=open
            tell(context.tr(R.string.ui_nejdriv_dokonci_nebo_zahod_rozpracovany_trenink_706cc))
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
        if(days.isEmpty() || plan(selectedDay).exercises.isEmpty()) {tell(context.tr(R.string.ui_nejdriv_pridej_cvik_2f001));return}
        val other=db.firstOpenSession()
        if(other!=null && other.day!=selectedDay) {
            selectDay(other.day); tell(context.tr(R.string.ui_nejprve_dokonci_nebo_zahod_rozpracovany_trenink_408e8)); return
        }
        session=db.beginWorkout(selectedDay); selectDay(selectedDay)
    }
    fun discardWorkout() {
        val open=session ?: return
        cancelRest()
        db.deleteOpenWorkout(open.id)
        selectDay(selectedDay);refresh();tell(context.tr(R.string.ui_rozpracovany_trenink_zahozen_55ade))
    }
    fun updateExerciseDetails(key:String,name:String,note:String) {
        if(session!=null) {tell(context.tr(R.string.ui_upravy_cviku_jsou_dostupne_po_ukonceni_7a8b8));return}
        if(name.isBlank()) {tell(context.tr(R.string.ui_nazev_cviku_nesmi_byt_prazdny_685dc));return}
        db.updateExerciseDetails(selectedDay,key,name,note)
        refresh();selectDay(selectedDay);tell(context.tr(R.string.ui_nazev_a_poznamka_ulozeny_22bf8))
    }
    fun createDay(name:String) {
        if(name.isBlank()) {tell(context.tr(R.string.ui_zadej_nazev_dne_0df1b));return}
        if(days.size>=20) {tell(context.tr(R.string.ui_limit_je_sablon_fe838));return}
        val id=db.addDay(name);refresh();selectDay(id);tell(context.tr(R.string.ui_treninkovy_den_pridan_257b0))
    }
    fun addExercise(name:String,sets:Int,type:UnitType) {
        if(session!=null) {tell(context.tr(R.string.ui_pred_upravou_dokonci_rozepsany_trenink_97895));return}
        if(name.isBlank() || sets !in 1..15) {tell(context.tr(R.string.ui_zadej_nazev_a_serii_b3f2b));return}
        db.addExercise(selectedDay,name,sets,type);refresh();selectDay(selectedDay)
    }
    fun changeExerciseUnit(key:String,type:UnitType) {
        if(session!=null) {tell(context.tr(R.string.ui_pred_upravou_dokonci_rozpracovany_trenink_f44c7));return}
        db.changeExerciseUnit(selectedDay,key,type)
        refresh();selectDay(selectedDay);tell(context.tr(R.string.ui_typ_zateze_byl_upraven_0f8bd))
    }
    fun moveExercise(key:String,offset:Int) {
        if(session!=null) {tell(context.tr(R.string.ui_pred_upravou_dokonci_rozepsany_trenink_97895));return}
        db.moveExercise(selectedDay,key,offset);refresh();selectDay(selectedDay)
    }
    fun removeExercise(key:String) {
        if(session!=null) {tell(context.tr(R.string.ui_pred_upravou_dokonci_rozepsany_trenink_97895));return}
        db.removeExercise(selectedDay,key);refresh();selectDay(selectedDay)
    }
    fun deleteDay() {
        if(session!=null) {tell(context.tr(R.string.ui_pred_odstranenim_dokonci_rozepsany_trenink_7d20e));return}
        val old=selectedDay;db.removeDay(old);refresh();selectDay(days.firstOrNull()?.key.orEmpty())
    }
    fun updatePair(a:String,b:String?) = updateSuperset(a,listOfNotNull(a,b).distinct())
    fun updateSuperset(anchor:String,selected:List<String>,rounds:Map<String,Int> = emptyMap()):Boolean = try {
        db.setSuperset(selectedDay,anchor,selected,rounds)
        days=db.plans();tell(context.tr(R.string.ui_superserie_ulozena_pro_tento_i_dalsi_cafcd));true
    } catch(e:Exception) {tell(e.localizedMessage ?: context.tr(R.string.ui_superserii_se_nepodarilo_ulozit_d26c6));false}
    fun updatePermanentNote(key:String,note:String):Boolean = try {
        db.updateExerciseNote(selectedDay,key,note)
        days=db.plans();tell(context.tr(R.string.ui_trvala_poznamka_ulozena_b6960));true
    } catch(e:Exception) {tell(e.localizedMessage ?: context.tr(R.string.ui_poznamku_se_nepodarilo_ulozit_ffea9));false}
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
    fun exerciseRecordText(key:String,includeEstimate:Boolean=false):String {
        val history=exerciseHistory(key)
        if(history.isEmpty()) return context.tr(R.string.ui_jeste_nejsou_dokoncene_serie_95435)
        val weight=history.maxOf{it.maxKg}
        val reps=history.maxOf{it.maxReps}
        val estimated=history.maxOf{it.bestEstimated1Rm}
        return buildString {
            append(context.tr(R.string.ui_nejvyssi_zatez_s_kg_nejvic_opakovani_757a3 ,fmtNumber(weight),reps))
            if(includeEstimate && estimated>0) append(context.tr(R.string.ui_odhad_rm_s_kg_2bf0b ,fmtNumber(estimated)))
        }
    }
    fun isLatestRecord(key:String):Boolean {
        val latest=db.allSessions().firstOrNull {sess ->db.sets(sess.id).any{it.exercise==key && it.done}}?:return false
        return isRecordInSession(key,latest.id)
    }
    internal fun isRecordInSession(key:String,id:Long):Boolean {
        val ordered=db.allSessions().asReversed()
        val position=ordered.indexOfFirst {it.id==id}
        if(position<0) return false
        val earlier=ordered.take(position).flatMap{db.sets(it.id)}.filter{it.exercise==key && it.done}
        if(earlier.isEmpty()) return false
        return db.sets(id).filter{it.exercise==key && it.done}.any {set ->
            val weight=set.weight.asNumber()?:0.0
            val matching=earlier.filter{(it.weight.asNumber()?:0.0)==weight}
            weight>earlier.maxOf{it.weight.asNumber()?:0.0} ||
                (matching.isNotEmpty() && (set.reps.toIntOrNull()?:0)>matching.maxOf{it.reps.toIntOrNull()?:0})
        }
    }
    internal fun workoutRecords(id:Long):List<String> = savedWorkoutSets(id).filter{it.done}
        .map{it.exercise}.distinct().filter{isRecordInSession(it,id)}.map(::exerciseName)
    fun setWorkoutDuration(id:Long, minutes:Int):Boolean = setWorkoutDurationSeconds(id,minutes*60)
    fun setWorkoutDurationSeconds(id:Long, seconds:Int):Boolean = try {
        require(seconds in 1..172800) { context.tr(R.string.ui_zadej_delku_az_minut_6923b) }
        db.updateWorkoutDuration(id,seconds)
        session=db.firstOpenSession();refresh();tell(context.tr(R.string.ui_delka_treninku_upravena_d87b9));true
    } catch(e:Exception) {tell(e.localizedMessage ?: context.tr(R.string.ui_delku_se_nepodarilo_ulozit_abcb3));false}
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
            ExportScope.RECENT->cardio.take(5)
            ExportScope.PREVIOUS_WEEK->cardio.filter{dateInWeek(it.date)}
            ExportScope.ALL->cardio
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
        appendLine(context.tr(R.string.ui_liftant_treninkovy_vypis_jen_cteni_sdileny_473e9))
        appendLine(context.tr(R.string.ui_datum_exportu_s_78e26 ,today))
        appendLine(when(scope) {
            ExportScope.RECENT -> context.tr(R.string.ui_rozsah_posledni_zaznamy_c08d5)
            ExportScope.PREVIOUS_WEEK -> context.tr(R.string.ui_rozsah_minuly_tyden_s_az_s_b3d73 ,from,until)
            ExportScope.ALL -> context.tr(R.string.ui_rozsah_cela_historie_9a17a)
        })
        appendLine(context.tr(R.string.ui_silove_treninky_s_4a411 ,finishedSessions.size))
        if(finishedSessions.isEmpty()) appendLine(context.tr(R.string.ui_zadny_dokonceny_silovy_trenink_ca87b))
        finishedSessions.forEach {sess ->
            val date=java.time.Instant.ofEpochMilli(sess.finished?:sess.created)
                .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
            appendLine("$date | ${days.firstOrNull{it.key==sess.day}?.name ?: sess.day}")
            appendLine(context.tr(R.string.ui_nazvedana_hmotnost_s_kg_externi_zatez_4dd7d ,fmtNumber(totals(sess).liftedKg)))
            db.sets(sess.id).filter{it.done}.groupBy{it.exercise}.forEach { (key,sets) ->
                val exercise=days.flatMap{it.exercises}.firstOrNull{it.key==key}
                val name=exercise?.name ?: key
                appendLine("- $name: ${sets.joinToString("; "){set ->
                    val w= when {
                        set.weight=="0" && (exercise?.type==UnitType.ADDED_KG || exercise?.type==UnitType.REPS || key.contains("calf",true)) -> context.tr(R.string.ui_kg_vlastni_vaha_b4254)
                        set.weight.isNotBlank() -> "${set.weight} kg × "
                        else -> ""
                    }
                    w+set.reps+" "+context.tr(R.string.ui_op_85765)+if(exercise?.loadMultiplier==2) context.tr(R.string.ui_cinky_vaha_jedne_f4558) else ""
                }}")
            }
        }
        appendLine(context.tr(R.string.cardio_title)+": "+chosenRuns.size)
        chosenRuns.forEach {r->appendLine(cardioName(context,r)+" | "+context.tr(R.string.ui_s_s_km_ss_tep_s_5ab27 ,r.date,fmtNumber(r.km),r.seconds,r.avg?:"–",r.max?:"–",if(r.note.isNotBlank()) context.tr(R.string.ui_pozn_s_db1d9 ,r.note) else ""))}
        appendLine(context.tr(R.string.ui_hiit_s_b5662 ,chosenHiit.size))
        chosenHiit.forEach { r ->
            val extras=buildList {
                r.km?.let{add("${fmtNumber(it)} km")}
                r.warmup?.let{add(context.tr(R.string.ui_zahrati_s_min_4c1cf ,it))}
                r.cooldown?.let{add(context.tr(R.string.ui_zklidneni_s_min_68133 ,it))}
                r.workResistance?.let{add(context.tr(R.string.ui_odpor_prace_s_f4100 ,it))}
                r.restResistance?.let{add(context.tr(R.string.ui_odpor_odpocinek_s_2cb58 ,it))}
            }.joinToString(", ")
            appendLine(context.tr(R.string.ui_s_s_min_celkem_s_kol_fd807 ,r.date,fmtNumber((r.totalSeconds ?: r.minutes*60)/60.0),r.rounds,r.work,r.rest,r.avg?:"–",r.max?:"–",if(extras.isNotBlank()) ", $extras" else "",if(r.note.isNotBlank()) context.tr(R.string.ui_pozn_s_db1d9 ,r.note) else ""))
        }
        appendLine(context.tr(R.string.ui_delka_silovych_treninku_min_114a8))
        finishedSessions.forEach {sess ->appendLine("${sess.day}: ${sess.elapsedSeconds()/60} min")}
        appendLine(context.tr(R.string.ui_mereni_s_6b409 ,chosenMeasures.size))
        chosenMeasures.forEach { m ->
            val values=buildList {
                m.kg?.let { add("${fmtNumber(it)} kg") }
                m.waist?.let { add(context.tr(R.string.ui_bricho_s_cm_9a34f ,fmtNumber(it))) }
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
            if(r==null || r<=0) { tell(context.tr(R.string.ui_nejdriv_zadej_pocet_opakovani_07224)); return null }
            if(type==UnitType.KG && (current.weight.asNumber() ?: -1.0)<0.0) { tell(context.tr(R.string.ui_zadej_zatez_kg_vlastni_vaha_00f5b)); return null }
            if(type==UnitType.ADDED_KG && (current.weight.asNumber() ?: -1.0)<0.0) { tell(context.tr(R.string.ui_zadej_pridanou_zatez_kg_bez_zateze_5a8d0)); return null }
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
        if(current.done) {tell(context.tr(R.string.ui_nejdriv_vrat_serii_mezi_nedokoncene_cf238));return}
        val reps=(current.reps.toIntOrNull()?:0).coerceAtLeast(0)
        changeSet(current,reps=(reps-1).coerceAtLeast(0).toString())
    }
    fun adjustWeight(entry:SetEntry,delta:Double) {
        val current=workoutSets.firstOrNull{it.id==entry.id}?:return
        if(current.done) {tell(context.tr(R.string.ui_nejdriv_vrat_serii_mezi_nedokoncene_cf238));return}
        val next=((current.weight.asNumber()?:0.0)+delta).coerceAtLeast(0.0)
        changeSet(current,weight=fmtNumber(next))
    }
    fun plusOneRep(entry:SetEntry) {
        val current=workoutSets.firstOrNull{it.id==entry.id} ?: return
        if(current.done) {tell(context.tr(R.string.ui_nejdriv_vrat_serii_mezi_nedokoncene_cf238));return}
        val currentReps=current.reps.toIntOrNull() ?: 0
        if(currentReps>=9999) {tell(context.tr(R.string.ui_maximum_je_opakovani_3a1bc));return}
        changeSet(current,reps=(currentReps+1).toString())
    }
    fun addWorkoutSet(exercise:Exercise) {
        val s=session?:return
        val last=workoutSets.filter{it.exercise==exercise.key}.maxByOrNull{it.number}
        try {
            val added=db.addOpenSet(s.id,exercise.key,last)
            workoutSets.add(added)
            previous["${exercise.key}:${added.number}"]=db.previous(selectedDay,exercise.key,added.number)
        } catch(e:Exception){tell(e.localizedMessage?:context.tr(R.string.ui_serii_nelze_pridat_e4c83))}
    }
    fun removeWorkoutSet(exercise:Exercise) {
        val s=session?:return
        val entries=workoutSets.filter{it.exercise==exercise.key}
        if(entries.size<=1){tell(context.tr(R.string.ui_musi_zustat_alespon_jedna_serie_2dbc3));return}
        val last=entries.maxByOrNull{it.number}?:return
        try {
            db.removeLastOpenSet(s.id,last)
            workoutSets.removeAll{it.id==last.id}
            previous.remove("${exercise.key}:${last.number}")
        }catch(e:Exception){tell(e.localizedMessage?:context.tr(R.string.ui_serii_nelze_odebrat_4f967))}
    }
    fun finish() {
        val s=session ?: return
        if(!workoutSets.any { it.done }) { tell(context.tr(R.string.ui_nejdriv_dokonci_alespon_jednu_serii_ce950)); return }
        cancelRest()
        db.finishWorkout(s.id)
        selectDay(selectedDay); refresh()
        val records=workoutRecords(s.id)
        tell(if(records.isEmpty()) context.tr(R.string.ui_trenink_ulozen_a092d)
            else context.tr(R.string.workout_new_records,records.joinToString(", ")))
    }
    fun addCardio(record:RunEntry) {db.addCardio(record);refresh();tell(context.tr(R.string.cardio_saved))}
    fun addRun(date:String,km:Double,seconds:Int,avg:Int?,max:Int?,note:String) {
        db.addRun(date,km,seconds,avg,max,note); refresh(); tell(context.tr(R.string.ui_beh_ulozen_3e5f1))
    }
    fun addHiit(record:HiitEntry) {
        db.addHiit(record);refresh();tell(context.tr(R.string.ui_hiit_ulozen_c8fea))
    }
    internal fun addMeasure(date:String,values:BodyMeasurementInput):Boolean = try {
        db.addMeasure(date,values.kg,values.waist,values.extra);refresh();tell(context.tr(R.string.ui_mereni_ulozeno_0fce9));true
    } catch(e:Exception) {tell(e.localizedMessage?:context.tr(R.string.ui_mereni_se_nepodarilo_ulozit_a538f));false}
    fun saveBodyMetric(name:String,id:String?=null):Boolean = try {
        db.saveBodyMetric(name,id);refresh();tell(context.tr(R.string.ui_vlastni_mira_ulozena_d9e76));true
    } catch(e:Exception) {tell(e.localizedMessage?:context.tr(R.string.ui_miru_se_nepodarilo_ulozit_4df2c));false}
    fun exportWorkoutCsv():String {
        check(billing.hasPro)
        val allExercises=days.flatMap {it.exercises}.associateBy {it.key}
        return buildString {
            append("\uFEFFdate,workout,exercise,set,weight_kg,reps,load_multiplier,volume_kg\r\n")
            db.allSessions().asReversed().forEach {record ->
                val date=Instant.ofEpochMilli(record.finished?:record.created).atZone(ZoneId.systemDefault()).toLocalDate().toString()
                val plan=days.firstOrNull {it.key==record.day}?.name?:record.day
                db.sets(record.id).filter {it.done}.forEach {set ->
                    val exercise=allExercises[set.exercise]
                    val load=(set.weight.asNumber()?:0.0).takeIf {it.isFinite()}?:0.0
                    val volume=workoutTotals(listOf(set),allExercises.values.toList()).liftedKg
                    append(listOf(date,plan,exercise?.name?:set.exercise,set.number.toString(),fmtNumber(load),
                        set.reps,(exercise?.loadMultiplier?:1).toString(),fmtNumber(volume)).joinToString(",") {csvCell(it)})
                    append("\r\n")
                }
            }
        }
    }
    fun exportBackup():String = org.json.JSONObject(db.exportJson()).put("settings",BackupPreferences.export(getApplication())).toString(2)
    fun importBackup(text:String) {
        val root=org.json.JSONObject(text)
        val restoredSettings=BackupPreferences.validate(root.optJSONObject("settings"))
        db.importJson(text)
        // Only validated, non-secret display preferences are restored, after the DB transaction commits.
        BackupPreferences.restore(getApplication(),restoredSettings)
        designMode=settings.getInt("design",2);hapticEnabled=settings.getBoolean("haptic",true)
        autoScrollEnabled=settings.getBoolean("auto_scroll",true);restSeconds=settings.getInt("rest_seconds",90)
        restHapticEnabled=settings.getBoolean("rest_haptic",true)
        restWakeScreenEnabled=settings.getBoolean("rest_wake_screen",false)
        restStartMode=RestStartMode.valueOf(settings.getString("rest_mode","MANUAL")!!)
        quickShortcuts=QuickShortcuts.read(settings)
        cancelSavedEdit();cancelRest();refresh();selectDay(db.firstOpenSession()?.day?:selectedDay)
        tell(context.tr(R.string.ui_zaloha_obnovena_8abf3))
    }
    override fun onCleared() { billing.close();restRepository.removeObserver(restListener);db.close();super.onCleared() }
}
