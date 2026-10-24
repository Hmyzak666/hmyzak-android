package cz.rezim.app

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.core.database.sqlite.transaction
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.time.LocalDate
import java.time.format.DateTimeFormatter

internal fun currentDate(): String = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
internal fun String.asNumber(): Double? = trim().replace(',', '.').toDoubleOrNull()
internal fun fmtNumber(n: Double): String = if (n % 1.0 == 0.0) "${n.toLong()}" else "%.2f".format(java.util.Locale.US, n).trimEnd('0').trimEnd('.')
internal fun fmtDisplay(n:Double):String = java.text.NumberFormat.getNumberInstance(java.util.Locale.getDefault()).apply {
    isGroupingUsed=false;maximumFractionDigits=2
}.format(n)


data class Session(val id: Long, val day: String, val created: Long, val finished: Long?, val durationSeconds: Int? = null,
    val elapsedOffsetSeconds: Int = 0) {
    fun elapsedSeconds(now: Long = System.currentTimeMillis()): Int = durationSeconds ?: (
        ((finished ?: now) - created).coerceAtLeast(0L) / 1000L + elapsedOffsetSeconds
    ).coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
}
data class SetEntry(val id: Long, val session: Long, val exercise: String, val number: Int,
                             val weight: String, val reps: String, val done: Boolean)
data class RunEntry(val id: Long, val date: String, val km: Double, val seconds: Int,
                             val avg: Int?, val max: Int?, val note: String)
data class HiitEntry(val id: Long, val date: String, val minutes: Int, val rounds: Int,
                              val work: Int, val rest: Int, val avg: Int?, val max: Int?, val note: String,
                              val km: Double? = null, val warmup: Int? = null, val cooldown: Int? = null,
                              val workResistance: Int? = null, val restResistance: Int? = null,
                              val activity: String = "OTHER")
data class BodyMetric(val id: String, val name: String)
data class MeasureEntry(val id: Long, val date: String, val kg: Double?, val waist: Double?,
                        val extra: Map<String, Double> = emptyMap())

internal class LocalStore(private val baseContext: Context) : SQLiteOpenHelper(baseContext, "rezim_local_v1.db", null, 11) {
    private val context:Context get()=AppLanguage.wrap(baseContext)
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE sessions (id INTEGER PRIMARY KEY AUTOINCREMENT, day TEXT NOT NULL, created INTEGER NOT NULL, finished INTEGER, duration_seconds INTEGER)")
        db.execSQL("CREATE TABLE workout_sets (id INTEGER PRIMARY KEY AUTOINCREMENT, session INTEGER NOT NULL, exercise TEXT NOT NULL, set_number INTEGER NOT NULL, weight TEXT NOT NULL DEFAULT '', reps TEXT NOT NULL DEFAULT '', done INTEGER NOT NULL DEFAULT 0, FOREIGN KEY(session) REFERENCES sessions(id) ON DELETE CASCADE)")
        db.execSQL("CREATE INDEX sets_session ON workout_sets(session)")
        db.execSQL("CREATE TABLE runs (id INTEGER PRIMARY KEY AUTOINCREMENT, date TEXT NOT NULL, km REAL NOT NULL, seconds INTEGER NOT NULL, avg_hr INTEGER, max_hr INTEGER, note TEXT NOT NULL DEFAULT '')")
        db.execSQL("CREATE TABLE hiit (id INTEGER PRIMARY KEY AUTOINCREMENT, date TEXT NOT NULL, minutes INTEGER NOT NULL, rounds INTEGER NOT NULL, work_seconds INTEGER NOT NULL, rest_seconds INTEGER NOT NULL, avg_hr INTEGER, max_hr INTEGER, note TEXT NOT NULL DEFAULT '', km REAL, warmup_minutes INTEGER, cooldown_minutes INTEGER, work_resistance INTEGER, rest_resistance INTEGER)")
        db.execSQL("CREATE TABLE measures (id INTEGER PRIMARY KEY AUTOINCREMENT, date TEXT NOT NULL, kg REAL, waist REAL)")
        createBodyMetricTables(db)
        createPlanTables(db)
        addWorkoutToolsColumns(db)
        addSupersetRoundColumn(db)
        addLoadMultiplierColumn(db)
        initializeDumbbellFactors(db)
        addPublicReleaseSchema(db, false)
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if(oldVersion < 2) {
            createPlanTables(db)
            seedPlans(db)
        }
        if(oldVersion == 2) {
            db.execSQL("ALTER TABLE plan_exercises ADD COLUMN note TEXT NOT NULL DEFAULT ''")
        }
        if(oldVersion < 4) {
            // Only additive columns; no existing records are removed.
            db.execSQL("ALTER TABLE hiit ADD COLUMN km REAL")
            db.execSQL("ALTER TABLE hiit ADD COLUMN warmup_minutes INTEGER")
            db.execSQL("ALTER TABLE hiit ADD COLUMN cooldown_minutes INTEGER")
            db.execSQL("ALTER TABLE hiit ADD COLUMN work_resistance INTEGER")
            db.execSQL("ALTER TABLE hiit ADD COLUMN rest_resistance INTEGER")
        }
        if(oldVersion < 5) {
            // Optional override when the workout was interrupted for a long time.
            db.execSQL("ALTER TABLE sessions ADD COLUMN duration_seconds INTEGER")
        }
        if(oldVersion < 6) {
            // SQLiteOpenHelper wraps the upgrade in a transaction. Preserve all IDs and values;
            // only the optional circumference loses its NOT NULL constraint.
            db.execSQL("CREATE TABLE measures_optional_waist (id INTEGER PRIMARY KEY AUTOINCREMENT, date TEXT NOT NULL, kg REAL NOT NULL, waist REAL)")
            db.execSQL("INSERT INTO measures_optional_waist (id,date,kg,waist) SELECT id,date,kg,waist FROM measures")
            db.execSQL("DROP TABLE measures")
            db.execSQL("ALTER TABLE measures_optional_waist RENAME TO measures")
        }
        if(oldVersion < 7) {
            // Preserve every existing ID, weight and optional waist. Other body measures
            // can now be recorded on their own, without inventing a weight for that day.
            db.execSQL("CREATE TABLE measures_optional_weight (id INTEGER PRIMARY KEY AUTOINCREMENT, date TEXT NOT NULL, kg REAL, waist REAL)")
            db.execSQL("INSERT INTO measures_optional_weight (id,date,kg,waist) SELECT id,date,kg,waist FROM measures")
            db.execSQL("DROP TABLE measures")
            db.execSQL("ALTER TABLE measures_optional_weight RENAME TO measures")
            createBodyMetricTables(db)
        }
        if(oldVersion < 8) addWorkoutToolsColumns(db)
        if(oldVersion < 9) addSupersetRoundColumn(db)
        if(oldVersion < 10) { addLoadMultiplierColumn(db);initializeDumbbellFactors(db) }
        if(oldVersion < 11) addPublicReleaseSchema(db, true)
    }
    private fun addPublicReleaseSchema(db: SQLiteDatabase, existing: Boolean) {
        db.execSQL("ALTER TABLE hiit ADD COLUMN activity TEXT NOT NULL DEFAULT 'OTHER' CHECK(activity IN ('OTHER','ELLIPTICAL','RUNNING','CYCLING','ROWING'))")
        db.execSQL("CREATE TABLE app_state (id TEXT PRIMARY KEY NOT NULL, value TEXT NOT NULL)")
        db.execSQL("INSERT INTO app_state VALUES ('setup_complete',?)", arrayOf(if(existing) "1" else "0"))
    }
    fun needsOnboarding(): Boolean = readableDatabase.rawQuery(
        "SELECT value FROM app_state WHERE id='setup_complete'", null
    ).use { it.moveToFirst() && it.getString(0) != "1" } &&
        listOf("plan_days","sessions","runs","hiit","measures","body_metrics").none { table ->
            readableDatabase.rawQuery("SELECT 1 FROM $table LIMIT 1",null).use {it.moveToFirst()}
        }
    private fun markSetupComplete(db: SQLiteDatabase) {
        db.execSQL("INSERT OR REPLACE INTO app_state VALUES ('setup_complete','1')")
    }
    fun completeSetup(template: TrainingTemplate? = null) {
        val db=writableDatabase
        db.transaction {
            if(!needsOnboarding()) {markSetupComplete(db);return@transaction}
            if(template!=null) insertTemplate(template)
            markSetupComplete(db)
        }
    }
    fun insertTemplate(template: TrainingTemplate): List<String> {
        val db=writableDatabase
        return db.transaction {
            require(plans().size+template.days.size<=20) {context.tr(R.string.ui_limit_je_sablon_fe838)}
            template.days.map {day ->
                val key=addDay(context.tr(day.name))
                day.exercises.forEach {ex ->
                    addExercise(key,context.tr(ex.name),ex.sets,ex.unit)
                    if(ex.multiplier==2) {
                        val id=plans().first {it.key==key}.exercises.last().key
                        setLoadMultiplier(key,id,2)
                    }
                }
                key
            }.also {markSetupComplete(db)}
        }
    }
    fun renameDay(day: String, name: String) {
        require(name.trim().isNotEmpty())
        writableDatabase.update("plan_days",ContentValues().apply {
            put("name",name.trim());put("short",name.trim().take(3).uppercase())
        },"id=?",arrayOf(day))
    }
    fun setExerciseSets(day:String, exercise:String, count:Int) {
        require(count in 1..15)
        writableDatabase.update("plan_exercises",ContentValues().apply {put("sets",count)},
            "day_id=? AND id=?",arrayOf(day,exercise))
    }
    private fun addLoadMultiplierColumn(db: SQLiteDatabase) {
        db.execSQL("ALTER TABLE plan_exercises ADD COLUMN load_multiplier INTEGER NOT NULL DEFAULT 1 CHECK(load_multiplier IN (1,2))")
    }
    private fun initializeDumbbellFactors(db: SQLiteDatabase) {
        // Built-in dumbbell exercises have always requested the weight of ONE dumbbell.
        // Only this new interpretation field changes; recorded sets remain byte-for-byte intact.
        val keys = arrayOf("flat_db_bench","incline_db_bench","neutral_db_ohp","lateral_mon","lateral_fri","rear_delt")
        keys.forEach { key -> db.execSQL("UPDATE plan_exercises SET load_multiplier=2 WHERE id=? AND unit='KG'",arrayOf(key)) }
    }
    fun setLoadMultiplier(day:String,exercise:String,value:Int) {
        require(value in 1..2)
        check(writableDatabase.update("plan_exercises",ContentValues().apply {put("load_multiplier",value)},
            "id=? AND day_id=?",arrayOf(exercise,day)) == 1)
    }
    private fun addSupersetRoundColumn(db: SQLiteDatabase) {
        db.execSQL("ALTER TABLE plan_exercises ADD COLUMN superset_start_round INTEGER NOT NULL DEFAULT 1 CHECK(superset_start_round BETWEEN 0 AND 15)")
    }
    private fun addWorkoutToolsColumns(db: SQLiteDatabase) {
        db.execSQL("ALTER TABLE plan_exercises ADD COLUMN superset_id TEXT")
        db.execSQL("ALTER TABLE sessions ADD COLUMN elapsed_offset_seconds INTEGER NOT NULL DEFAULT 0")
        migrateLegacySupersets(db)
    }
    private fun migrateLegacySupersets(db: SQLiteDatabase) {
        val rows = db.rawQuery("SELECT id,day_id,partner_id,superset_id FROM plan_exercises", null).use { c ->
            buildList { while(c.moveToNext()) add(listOf(c.getString(0),c.getString(1),
                if(c.isNull(2)) null else c.getString(2), if(c.isNull(3)) null else c.getString(3))) }
        }
        val byId = rows.associateBy { it[0] }
        for (row in rows) {
            val other = byId[row[2]] ?: continue
            if (row[3] == null && other[3] == null && other[2] == row[0] && other[1] == row[1] && other[0] != row[0]) {
                val group = "legacy_" + minOf(row[0]!!, other[0]!!)
                db.execSQL("UPDATE plan_exercises SET superset_id=? WHERE id IN (?,?)", arrayOf(group,row[0],other[0]))
            }
        }
    }
    private fun createBodyMetricTables(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE body_metrics (id TEXT PRIMARY KEY NOT NULL, name TEXT NOT NULL, position INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE body_metric_values (id INTEGER PRIMARY KEY AUTOINCREMENT, measurement_id INTEGER NOT NULL, metric_id TEXT NOT NULL, cm REAL NOT NULL CHECK(cm > 0 AND cm <= 300), UNIQUE(measurement_id,metric_id), FOREIGN KEY(measurement_id) REFERENCES measures(id) ON DELETE CASCADE, FOREIGN KEY(metric_id) REFERENCES body_metrics(id))")
    }
    private fun createPlanTables(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS plan_days (id TEXT PRIMARY KEY NOT NULL, short TEXT NOT NULL, name TEXT NOT NULL, position INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS plan_exercises (id TEXT PRIMARY KEY NOT NULL, day_id TEXT NOT NULL, name TEXT NOT NULL, sets INTEGER NOT NULL, unit TEXT NOT NULL, position INTEGER NOT NULL, partner_id TEXT, note TEXT NOT NULL DEFAULT '')")
        db.execSQL("CREATE INDEX IF NOT EXISTS exercises_day_position ON plan_exercises(day_id, position)")
    }
    private fun seedPlans(db: SQLiteDatabase) {
        Plans.days.forEachIndexed { index, day ->
            db.insertOrThrow("plan_days", null, ContentValues().apply {
                put("id",day.key); put("short",day.short);put("name",day.name);put("position",index)
            })
            day.exercises.forEachIndexed { pos, ex ->
                db.insertOrThrow("plan_exercises",null,ContentValues().apply {
                    put("id",ex.key);put("day_id",day.key);put("name",ex.name)
                    put("sets",ex.sets);put("unit",ex.type.name);put("position",pos)
                    put("partner_id", Plans.initialPartners[ex.key])
                })
            }
        }
    }
    fun plans(): List<DayPlan> = readableDatabase.rawQuery(
        "SELECT id,short,name FROM plan_days ORDER BY position,id",null
    ).use { c -> buildList {
        while(c.moveToNext()) {
            val id=c.getString(0)
            val exercises=readableDatabase.rawQuery(
                "SELECT id,name,sets,unit,partner_id,note,superset_id,superset_start_round,load_multiplier FROM plan_exercises WHERE day_id=? ORDER BY position,id",arrayOf(id)
            ).use { e -> buildList {
                while(e.moveToNext()) add(Exercise(e.getString(0),e.getString(1),e.getInt(2),
                    runCatching { UnitType.valueOf(e.getString(3)) }.getOrDefault(UnitType.KG),
                    if(e.isNull(4)) null else e.getString(4), e.getString(5) ?: "", if(e.isNull(6)) null else e.getString(6),e.getInt(7),e.getInt(8)))
            } }
            add(DayPlan(id,c.getString(1),c.getString(2),exercises))
        }
    } }
    fun addDay(name:String): String {
        val key="day_"+UUID.randomUUID().toString().replace("-", "")
        val pos=readableDatabase.rawQuery("SELECT COALESCE(MAX(position),-1)+1 FROM plan_days",null)
            .use { c -> c.moveToFirst();c.getInt(0) }
        writableDatabase.insertOrThrow("plan_days",null,ContentValues().apply {
            put("id",key);put("name",name.trim());put("short",name.trim().take(3).uppercase());put("position",pos)
        })
        markSetupComplete(writableDatabase)
        return key
    }
    fun addExercise(day:String,name:String,sets:Int,type:UnitType) {
        val pos=readableDatabase.rawQuery("SELECT COALESCE(MAX(position),-1)+1 FROM plan_exercises WHERE day_id=?", arrayOf(day))
            .use { c->c.moveToFirst();c.getInt(0) }
        writableDatabase.insertOrThrow("plan_exercises",null,ContentValues().apply {
            put("id","custom_"+UUID.randomUUID().toString().replace("-", ""))
            put("day_id",day);put("name",name.trim());put("sets",sets);put("unit",type.name);put("position",pos)
        })
    }
    // Names and permanent notes belong to the stable exercise ID, so history and trends remain linked.
    fun updateExerciseDetails(day:String, exercise:String, name:String, note:String) {
        require(name.trim().isNotEmpty()) { context.tr(R.string.ui_nazev_cviku_nesmi_byt_prazdny_685dc) }
        writableDatabase.update("plan_exercises", ContentValues().apply {
            put("name",name.trim()); put("note",note.trim())
        }, "id=? AND day_id=?", arrayOf(exercise,day))
    }

    fun changeExerciseUnit(day:String, exercise:String, type:UnitType) {
        writableDatabase.update("plan_exercises",ContentValues().apply{put("unit",type.name)},
            "id=? AND day_id=?",arrayOf(exercise,day))
    }

    fun moveExercise(day:String,exercise:String,offset:Int) {
        val ids=readableDatabase.rawQuery("SELECT id FROM plan_exercises WHERE day_id=? ORDER BY position,id",arrayOf(day))
            .use { c->buildList { while(c.moveToNext()) add(c.getString(0)) } }
        val i=ids.indexOf(exercise);val to=i+offset
        if(i<0 || to !in ids.indices) return
        val reordered=ids.toMutableList().apply { val id=removeAt(i);add(to,id) }
        val db=writableDatabase
        db.transaction {
            reordered.forEachIndexed {index,key ->
                db.update("plan_exercises",ContentValues().apply{put("position",index)},"id=?",arrayOf(key))
            }
        }
    }
    fun removeExercise(day:String,exercise:String) {
        val db=writableDatabase
        db.transaction {
            setSuperset(day,exercise,listOf(exercise))
            db.delete("plan_exercises","id=? AND day_id=?",arrayOf(exercise,day))
            db.execSQL("UPDATE plan_exercises SET partner_id=NULL WHERE partner_id=?",arrayOf(exercise))
        }
    }
    fun removeDay(day:String) {
        val db=writableDatabase
        db.transaction {
            db.delete("plan_exercises","day_id=?",arrayOf(day))
            db.delete("plan_days","id=?",arrayOf(day))
        }
    }
    fun pair(day:String,a:String,b:String?) = setSuperset(day,a,listOfNotNull(a,b).distinct())

    // Change group membership without touching saved sets, notes, exercise IDs or plan order.
    fun setSuperset(day:String, anchor:String, selected:List<String>, rounds:Map<String,Int> = emptyMap()) {
        val db = writableDatabase
        db.transaction {
            val exercises = plans().firstOrNull { it.key == day }?.exercises ?: error(context.tr(R.string.ui_den_nebyl_nalezen_1f6e3))
            val known = exercises.map { it.key }.toSet()
            require(anchor in known && anchor in selected && selected.distinct().size == selected.size && selected.all { it in known }) {
                context.tr(R.string.ui_vyber_cviky_ze_stejneho_treninkoveho_dne_c8a72)
            }
            require(rounds.keys.all { it in selected } && rounds.values.all { it in 0..15 }) {
                context.tr(R.string.ui_zvol_prvni_posledni_nebo_konkretni_kolo_231ae)
            }
            val moving = selected.toSet()
            val assignments = exercises.associate { it.key to it.supersetId }.toMutableMap()
            exercises.mapNotNull { it.supersetId }.distinct().forEach { group ->
                val remaining = exercises.filter { it.supersetId == group && it.key !in moving }
                exercises.filter { it.supersetId == group }.forEach { assignments[it.key] = null }
                if(remaining.size >= 2) remaining.forEach { assignments[it.key] = group }
            }
            selected.forEach { assignments[it] = null }
            if(selected.size >= 2) {
                val group = exercises.first { it.key == anchor }.supersetId
                    ?.takeIf { id -> assignments.values.none { it == id } }
                    ?: "superset_" + UUID.randomUUID().toString().replace("-", "")
                selected.forEach { assignments[it] = group }
            }
            for(exercise in exercises) {
                val group = assignments[exercise.key]
                val members = exercises.filter { group != null && assignments[it.key] == group }
                val next = if(members.size >= 2) members[(members.indexOfFirst { it.key == exercise.key } + 1) % members.size].key else null
                db.update("plan_exercises",ContentValues().apply {
                    put("superset_id",group);put("partner_id",next)
                    rounds[exercise.key]?.let {put("superset_start_round",it)}
                },"id=? AND day_id=?",arrayOf(exercise.key,day))
            }
        }
    }
    fun updateExerciseNote(day:String, exercise:String, note:String) {
        require(note.length <= 4000) { context.tr(R.string.ui_poznamka_muze_mit_nejvyse_znaku_14730) }
        check(writableDatabase.update("plan_exercises",ContentValues().apply { put("note",note.trim()) },
            "id=? AND day_id=?",arrayOf(exercise,day)) == 1) { context.tr(R.string.ui_poznamku_se_nepodarilo_ulozit_ffea9) }
    }
    private fun Cursor.longOrNull(col: String): Long? { val p = getColumnIndexOrThrow(col); return if (isNull(p)) null else getLong(p) }
    private fun Cursor.intOrNull(col: String): Int? { val p = getColumnIndexOrThrow(col); return if (isNull(p)) null else getInt(p) }

    fun openSession(day: String): Session? = readableDatabase.rawQuery(
        "SELECT * FROM sessions WHERE day=? AND finished IS NULL ORDER BY id DESC LIMIT 1", arrayOf(day)
    ).use { c -> if (c.moveToFirst()) session(c) else null }
    private fun session(c: Cursor) = Session(c.getLong(c.getColumnIndexOrThrow("id")),
        c.getString(c.getColumnIndexOrThrow("day")), c.getLong(c.getColumnIndexOrThrow("created")), c.longOrNull("finished"), c.intOrNull("duration_seconds"),c.getInt(c.getColumnIndexOrThrow("elapsed_offset_seconds")))
    fun recentSessions(limit: Int = 15): List<Session> = readableDatabase.rawQuery(
        "SELECT * FROM sessions WHERE finished IS NOT NULL ORDER BY finished DESC LIMIT $limit", null
    ).use { c -> buildList { while(c.moveToNext()) add(session(c)) } }
    fun allSessions():List<Session> = readableDatabase.rawQuery(
        "SELECT * FROM sessions WHERE finished IS NOT NULL ORDER BY finished DESC",null
    ).use{c->buildList{while(c.moveToNext()) add(session(c))}}
    fun previous(day: String, exercise: String, no: Int): SetEntry? = readableDatabase.rawQuery(
        """SELECT w.* FROM workout_sets w JOIN sessions s ON s.id=w.session
           WHERE s.day=? AND s.finished IS NOT NULL AND w.exercise=? AND w.set_number=? AND w.done=1
           ORDER BY s.finished DESC,w.id DESC LIMIT 1""".trimIndent(),
        arrayOf(day, exercise, no.toString())
    ).use { c -> if(c.moveToFirst()) set(c) else null }

    fun beginWorkout(day: String): Session {
        openSession(day)?.let { return it }
        val db = writableDatabase
        return db.transaction {
            val created = System.currentTimeMillis()
            val v = ContentValues().apply { put("day", day); put("created", created) }
            val id = db.insertOrThrow("sessions", null, v)
            for (e in plans().first {it.key==day}.exercises) for (n in 1..e.sets) {
                val prev = previous(day, e.key, n)
                val row = ContentValues().apply {
                    put("session", id); put("exercise", e.key); put("set_number", n)
                    put("weight", prev?.weight?.takeIf{it.isNotBlank()} ?: if (e.type != UnitType.KG || e.key.contains("calf",true)) "0" else "")
                    put("reps", prev?.reps ?: ""); put("done", 0)
                }
                db.insertOrThrow("workout_sets", null, row)
            }
            Session(id, day, created, null)
        }
    }
    private fun set(c: Cursor) = SetEntry(
        c.getLong(c.getColumnIndexOrThrow("id")), c.getLong(c.getColumnIndexOrThrow("session")),
        c.getString(c.getColumnIndexOrThrow("exercise")), c.getInt(c.getColumnIndexOrThrow("set_number")),
        c.getString(c.getColumnIndexOrThrow("weight")), c.getString(c.getColumnIndexOrThrow("reps")),
        c.getInt(c.getColumnIndexOrThrow("done")) == 1
    )
    fun sets(session: Long): List<SetEntry> = readableDatabase.rawQuery(
        "SELECT * FROM workout_sets WHERE session=? ORDER BY id", arrayOf(session.toString())
    ).use { c -> buildList { while(c.moveToNext()) add(set(c)) } }
    fun updateSet(s: SetEntry) {
        val v = ContentValues().apply { put("weight", s.weight); put("reps", s.reps); put("done", if(s.done) 1 else 0) }
        writableDatabase.update("workout_sets", v, "id=?", arrayOf(s.id.toString()))
    }
    fun addOpenSet(sessionId:Long, exercise:String, last:SetEntry?):SetEntry {
        val db=writableDatabase
        return db.transaction {
            check(db.rawQuery("SELECT id FROM sessions WHERE id=? AND finished IS NULL",arrayOf(sessionId.toString()))
                .use{it.moveToFirst()}) { context.tr(R.string.ui_trenink_uz_neni_rozpracovany_06f93) }
            val number=db.rawQuery(
                "SELECT COALESCE(MAX(set_number),0)+1 FROM workout_sets WHERE session=? AND exercise=?",
                arrayOf(sessionId.toString(),exercise)
            ).use{it.moveToFirst();it.getInt(0)}
            require(number in 1..15) { context.tr(R.string.ui_maximum_je_serii_na_cvik_7afb3) }
            val id=db.insertOrThrow("workout_sets",null,ContentValues().apply{
                put("session",sessionId);put("exercise",exercise);put("set_number",number)
                put("weight",last?.weight ?: "0");put("reps",last?.reps ?: "");put("done",0)
            })
            SetEntry(id,sessionId,exercise,number,last?.weight ?: "0",last?.reps ?: "",false)
        }
    }
    fun removeLastOpenSet(sessionId:Long, entry:SetEntry) {
        require(!entry.done) { context.tr(R.string.ui_nejdriv_odskrtni_posledni_serii_bbdb6) }
        val deleted=writableDatabase.delete("workout_sets",
            "id=? AND session=? AND done=0 AND session IN (SELECT id FROM sessions WHERE id=? AND finished IS NULL) AND set_number=(SELECT MAX(set_number) FROM workout_sets WHERE session=? AND exercise=?)",
            arrayOf(entry.id.toString(),sessionId.toString(),sessionId.toString(),sessionId.toString(),entry.exercise))
        require(deleted==1) { context.tr(R.string.ui_serii_se_nepodarilo_odebrat_6a6b9) }
    }
    fun deleteOpenWorkout(id: Long) {
        val db=writableDatabase
        db.transaction {
            // Deleting is intentionally restricted to unfinished sessions.
            val open=db.rawQuery("SELECT id FROM sessions WHERE id=? AND finished IS NULL",arrayOf(id.toString()))
                .use {it.moveToFirst()}
            if(open) {
                db.delete("workout_sets","session=?",arrayOf(id.toString()))
                db.delete("sessions","id=? AND finished IS NULL",arrayOf(id.toString()))
            }
        }
    }
    fun firstOpenSession():Session? = readableDatabase.rawQuery(
        "SELECT * FROM sessions WHERE finished IS NULL ORDER BY id DESC LIMIT 1",null
    ).use {c->if(c.moveToFirst()) session(c) else null}
    fun finishWorkout(id: Long) {
        writableDatabase.execSQL("UPDATE sessions SET finished=? WHERE id=? AND finished IS NULL", arrayOf(System.currentTimeMillis(), id))
    }
    fun updateWorkoutDuration(id: Long, seconds: Int, now:Long = System.currentTimeMillis()) {
        require(seconds in 60..(48*3600)) { context.tr(R.string.ui_delka_treninku_musi_byt_minuta_az_15d3d) }
        val db = writableDatabase
        db.transaction {
            val record = db.rawQuery("SELECT * FROM sessions WHERE id=?",arrayOf(id.toString()))
                .use { c -> if(c.moveToFirst()) session(c) else null } ?: error(context.tr(R.string.ui_trenink_nebyl_nalezen_141ea))
            val values = ContentValues().apply {
                if(record.finished != null) put("duration_seconds",seconds)
                else {
                    val elapsed = ((now-record.created).coerceAtLeast(0L)/1000L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
                    put("elapsed_offset_seconds",seconds-elapsed)
                    putNull("duration_seconds")
                }
            }
            check(db.update("sessions",values,"id=?",arrayOf(id.toString())) == 1) { context.tr(R.string.ui_trenink_nebyl_nalezen_141ea) }
        }
    }
    fun completedSets(): List<Pair<Session,SetEntry>> {
        val sessions=recentSessions(2000)
        return sessions.flatMap { session ->
            sets(session.id).filter {it.done}.map {session to it}
        }
    }
    fun saveFinishedWorkout(id:Long, changes:List<SetEntry>) {
        val db=writableDatabase
        db.transaction {
            val finished=db.rawQuery("SELECT id FROM sessions WHERE id=? AND finished IS NOT NULL",arrayOf(id.toString())).use{it.moveToFirst()}
            check(finished) { context.tr(R.string.ui_upravovat_lze_pouze_ulozeny_trenink_3293b) }
            val known=sets(id).associateBy{it.id}
            require(changes.size==known.size && changes.all{known[it.id]!=null && it.session==id}) { context.tr(R.string.ui_nesouhlasi_seznam_serii_5a053) }
            changes.forEach { s ->
                if(s.done) {
                    require((s.reps.toIntOrNull()?:0)>0) { context.tr(R.string.ui_dokoncena_serie_musi_mit_opakovani_3c7ab) }
                    require(s.weight.isBlank() || (s.weight.asNumber()?:-1.0)>=0.0) { context.tr(R.string.ui_zatez_nesmi_byt_zaporna_4ed49) }
                }
                db.update("workout_sets",ContentValues().apply{
                    put("weight",s.weight);put("reps",s.reps);put("done",if(s.done)1 else 0)
                },"id=? AND session=?",arrayOf(s.id.toString(),id.toString()))
            }
        }
    }
    fun countSessions(): Int = readableDatabase.rawQuery("SELECT count(*) FROM sessions WHERE finished IS NOT NULL", null).use { c -> c.moveToFirst(); c.getInt(0) }

    fun addRun(date: String, km: Double, seconds: Int, avg: Int?, max: Int?, note: String) {
        val v = ContentValues().apply { put("date",date); put("km",km); put("seconds",seconds); put("avg_hr",avg); put("max_hr",max); put("note",note) }
        writableDatabase.insertOrThrow("runs",null,v)
    }
    fun updateRun(r:RunEntry) {
        writableDatabase.update("runs",ContentValues().apply{
            put("date",r.date);put("km",r.km);put("seconds",r.seconds)
            put("avg_hr",r.avg);put("max_hr",r.max);put("note",r.note)
        },"id=?",arrayOf(r.id.toString()))
    }
    fun runs(): List<RunEntry> = readableDatabase.rawQuery("SELECT * FROM runs ORDER BY date DESC,id DESC", null).use { c -> buildList {
        while(c.moveToNext()) add(RunEntry(c.getLong(0),c.getString(1),c.getDouble(2),c.getInt(3),c.intOrNull("avg_hr"),c.intOrNull("max_hr"),c.getString(6)))
    } }
    fun addHiit(r:HiitEntry) {
        val v = ContentValues().apply {
            put("date",r.date);put("minutes",r.minutes);put("rounds",r.rounds)
            put("work_seconds",r.work);put("rest_seconds",r.rest)
            put("avg_hr",r.avg);put("max_hr",r.max);put("note",r.note)
            put("km",r.km);put("warmup_minutes",r.warmup);put("cooldown_minutes",r.cooldown)
            put("work_resistance",r.workResistance);put("rest_resistance",r.restResistance);put("activity",r.activity)
        }
        writableDatabase.insertOrThrow("hiit",null,v)
    }
    fun updateHiit(r:HiitEntry) {
        writableDatabase.update("hiit",ContentValues().apply{
            put("date",r.date);put("minutes",r.minutes);put("rounds",r.rounds)
            put("work_seconds",r.work);put("rest_seconds",r.rest)
            put("avg_hr",r.avg);put("max_hr",r.max);put("note",r.note)
            put("km",r.km);put("warmup_minutes",r.warmup);put("cooldown_minutes",r.cooldown)
            put("work_resistance",r.workResistance);put("rest_resistance",r.restResistance);put("activity",r.activity)
        },"id=?",arrayOf(r.id.toString()))
    }
    fun hiit(): List<HiitEntry> = readableDatabase.rawQuery("SELECT * FROM hiit ORDER BY date DESC,id DESC", null).use { c -> buildList {
        while(c.moveToNext()) add(HiitEntry(c.getLong(0),c.getString(1),c.getInt(2),c.getInt(3),c.getInt(4),c.getInt(5),
            c.intOrNull("avg_hr"),c.intOrNull("max_hr"),c.getString(c.getColumnIndexOrThrow("note")),
            c.getColumnIndexOrThrow("km").let{if(c.isNull(it)) null else c.getDouble(it)},
            c.intOrNull("warmup_minutes"),c.intOrNull("cooldown_minutes"),
            c.intOrNull("work_resistance"),c.intOrNull("rest_resistance"),c.getString(c.getColumnIndexOrThrow("activity"))))
    } }
    fun bodyMetrics(): List<BodyMetric> = readableDatabase.rawQuery(
        "SELECT id,name FROM body_metrics ORDER BY position,id", null
    ).use { c -> buildList { while(c.moveToNext()) add(BodyMetric(c.getString(0),c.getString(1))) } }

    fun saveBodyMetric(name:String, id:String?=null) {
        val clean=cleanBodyMetricName(name)
        require(validBodyMetricName(clean)) { context.tr(R.string.ui_zadej_vlastni_nazev_miry_znaku_vaha_d034f) }
        require(bodyMetrics().none { it.id!=id && bodyMetricNameKey(it.name)==bodyMetricNameKey(clean) }) {
            context.tr(R.string.ui_mira_s_timto_nazvem_uz_existuje_5aa6a)
        }
        val db=writableDatabase
        if(id==null) {
            val position=db.rawQuery("SELECT COALESCE(MAX(position),-1)+1 FROM body_metrics",null)
                .use { it.moveToFirst();it.getInt(0) }
            db.insertOrThrow("body_metrics",null,ContentValues().apply {
                put("id","metric_"+UUID.randomUUID().toString().replace("-",""));put("name",clean);put("position",position)
            })
        } else check(db.update("body_metrics",ContentValues().apply { put("name",clean) },"id=?",arrayOf(id))==1) {
            context.tr(R.string.ui_mira_nebyla_nalezena_a5058)
        }
    }

    private fun validateMeasure(date:String,kg:Double?,waist:Double?,extra:Map<String,Double>) {
        require(runCatching { LocalDate.parse(date) }.isSuccess) { context.tr(R.string.ui_zkontroluj_datum_mereni_aeecf) }
        require(kg==null || validBodyValue(kg,400.0)) { context.tr(R.string.ui_zkontroluj_hmotnost_vice_nez_nejvyse_kg_fa920) }
        require(waist==null || validBodyValue(waist,300.0)) { context.tr(R.string.ui_zkontroluj_obvod_bricha_vice_nez_nejvyse_ab794) }
        val known=bodyMetrics().map { it.id }.toSet()
        require(extra.all { (id,value) -> id in known && validBodyValue(value,300.0) }) { context.tr(R.string.ui_zkontroluj_vlastni_miry_f67f1) }
        require(kg!=null || waist!=null || extra.isNotEmpty()) { context.tr(R.string.ui_vypln_alespon_jednu_miru_0c287) }
    }

    private fun writeBodyValues(db:SQLiteDatabase,id:Long,extra:Map<String,Double>) {
        db.delete("body_metric_values","measurement_id=?",arrayOf(id.toString()))
        extra.forEach { (metric,value) -> db.insertOrThrow("body_metric_values",null,ContentValues().apply {
            put("measurement_id",id);put("metric_id",metric);put("cm",value)
        }) }
    }

    fun addMeasure(date:String,kg:Double?,waist:Double?,extra:Map<String,Double> = emptyMap()) {
        validateMeasure(date,kg,waist,extra)
        val db=writableDatabase
        db.transaction {
            val id=db.insertOrThrow("measures",null,ContentValues().apply { put("date",date);put("kg",kg);put("waist",waist) })
            writeBodyValues(db,id,extra)
        }
    }
    fun updateMeasure(r:MeasureEntry) {
        validateMeasure(r.date,r.kg,r.waist,r.extra)
        val db=writableDatabase
        db.transaction {
            check(db.update("measures",ContentValues().apply {
                put("date",r.date);put("kg",r.kg);put("waist",r.waist)
            },"id=?",arrayOf(r.id.toString()))==1) { context.tr(R.string.ui_mereni_nebylo_nalezeno_7d290) }
            writeBodyValues(db,r.id,r.extra)
        }
    }
    fun measures():List<MeasureEntry> {
        val extra=readableDatabase.rawQuery("SELECT measurement_id,metric_id,cm FROM body_metric_values ORDER BY id",null)
            .use { c -> buildMap<Long,MutableMap<String,Double>> {
                while(c.moveToNext()) getOrPut(c.getLong(0)) { linkedMapOf() }[c.getString(1)]=c.getDouble(2)
            } }
        return readableDatabase.rawQuery("SELECT id,date,kg,waist FROM measures ORDER BY date DESC,id DESC",null).use { c -> buildList {
            while(c.moveToNext()) add(MeasureEntry(c.getLong(0),c.getString(1),
                if(c.isNull(2)) null else c.getDouble(2),if(c.isNull(3)) null else c.getDouble(3),extra[c.getLong(0)]?.toMap()?:emptyMap()))
        } }
    }

    private val tables = linkedMapOf(
        "sessions" to listOf("id","day","created","finished","duration_seconds","elapsed_offset_seconds"),
        "workout_sets" to listOf("id","session","exercise","set_number","weight","reps","done"),
        "runs" to listOf("id","date","km","seconds","avg_hr","max_hr","note"),
        "hiit" to listOf("id","date","minutes","rounds","work_seconds","rest_seconds","avg_hr","max_hr","note","km","warmup_minutes","cooldown_minutes","work_resistance","rest_resistance","activity"),
        "measures" to listOf("id","date","kg","waist"),
        "plan_days" to listOf("id","short","name","position"),
        "plan_exercises" to listOf("id","day_id","name","sets","unit","position","partner_id","note","superset_id","superset_start_round","load_multiplier"),
        "body_metrics" to listOf("id","name","position"),
        "body_metric_values" to listOf("id","measurement_id","metric_id","cm")
    )
    fun finishedSets():List<SetEntry> = readableDatabase.rawQuery(
        "SELECT w.id,w.session,w.exercise,w.set_number,w.weight,w.reps,w.done FROM workout_sets w JOIN sessions s ON s.id=w.session WHERE s.finished IS NOT NULL ORDER BY w.session,w.id",null
    ).use { c -> buildList {while(c.moveToNext()) add(SetEntry(c.getLong(0),c.getLong(1),c.getString(2),c.getInt(3),c.getString(4),c.getString(5),c.getInt(6)!=0))} }
    fun exportJson():String = readableDatabase.transaction {
        val root=JSONObject().put("format","rezim-local-backup").put("schemaVersion",11)
        for ((table,cols) in tables) {
            val rows=JSONArray()
            readableDatabase.rawQuery("SELECT * FROM $table ORDER BY id",null).use { c ->
                while(c.moveToNext()) {
                    val row=JSONObject()
                    for (col in cols) {
                        val idx=c.getColumnIndexOrThrow(col)
                        val data:Any=when(c.getType(idx)) {
                            Cursor.FIELD_TYPE_INTEGER -> c.getLong(idx)
                            Cursor.FIELD_TYPE_FLOAT -> c.getDouble(idx)
                            Cursor.FIELD_TYPE_STRING -> c.getString(idx)
                            else -> JSONObject.NULL
                        }
                        row.put(col,data)
                    }
                    rows.put(row)
                }
            }
            root.put(table,rows)
        }
        root.toString(2)
    }
    fun importJson(raw:String) {
        val root=JSONObject(raw)
        val schema=root.optInt("schemaVersion")
        require(root.optString("format")=="rezim-local-backup" && schema in 1..11) { context.tr(R.string.ui_nepodporovany_soubor_zalohy_c34e9) }
        // Old backups have no editable plans; restored plans then use the built-in defaults.
        val imported=tables.mapValues { (t,_) ->
            if((schema==1 && t.startsWith("plan_")) || (schema<7 && t.startsWith("body_"))) JSONArray()
            else root.getJSONArray(t)
        }
        val db=writableDatabase
        db.transaction {
            for (t in tables.keys.reversed()) db.delete(t,null,null)
            for ((table,cols) in tables) {
                val arr=imported.getValue(table)
                for (i in 0 until arr.length()) {
                    val obj=arr.getJSONObject(i)
                    val values=ContentValues()
                    for (col in cols) {
                        if(schema < 11 && col == "activity") continue
                        if(schema < 8 && col in setOf("superset_id","elapsed_offset_seconds")) continue
                        if(schema < 9 && col == "superset_start_round") continue
                        if(schema < 10 && col == "load_multiplier") continue
                        if(col == "load_multiplier" && obj.has(col)) require((obj.get(col) is Int || obj.get(col) is Long) && obj.getLong(col) in 1L..2L) { context.tr(R.string.ui_neplatny_pocet_cinek_e6c37) }
                        if (!obj.has(col) || obj.isNull(col)) continue
                        val v=obj.get(col)
                        if(col=="superset_start_round") require((v is Int || v is Long) && (v as Number).toLong() in 0L..15L) { context.tr(R.string.ui_neplatne_kolo_superserie_d3357) }
                        if(col=="elapsed_offset_seconds") require((v is Int || v is Long) && (v as Number).toLong() in -2147483647L..2147483647L) { context.tr(R.string.ui_neplatna_oprava_delky_treninku_2f7c7) }
                        if(col=="superset_id") require(v is String && v.isNotBlank() && v.length<=128) { context.tr(R.string.ui_neplatne_oznaceni_superserie_8dcdb) }
                        when(v) {
                            is Int -> values.put(col,v)
                            is Long -> values.put(col,v)
                            is Double -> values.put(col,v)
                            is String -> values.put(col,v)
                            else -> error(context.tr(R.string.ui_neplatna_polozka_s_58b5f ,col))
                        }
                    }
                    db.insertOrThrow(table,null,values)
                }
            }
            if(schema==1) seedPlans(db)
            if(schema<8) migrateLegacySupersets(db)
            if(schema<10) initializeDumbbellFactors(db)
            val plans = plans()
            val grouped = plans.flatMap { day -> day.exercises.mapNotNull { ex -> ex.supersetId?.let { it to day.key } } }.groupBy({it.first},{it.second})
            require(grouped.values.all { it.size >= 2 && it.distinct().size == 1 }) { context.tr(R.string.ui_superserie_ma_neplatne_cleny_59611) }
            require(!db.rawQuery("SELECT id FROM sessions WHERE ABS(elapsed_offset_seconds)>2147483647 OR (duration_seconds IS NOT NULL AND (duration_seconds<0 OR duration_seconds>172800)) LIMIT 1",null)
                .use { it.moveToFirst() }) { context.tr(R.string.ui_neplatna_delka_treninku_83b78) }
            if(schema>=7) {
                val metrics=bodyMetrics()
                require(metrics.all { validBodyMetricName(it.name) } &&
                    metrics.map { bodyMetricNameKey(it.name) }.distinct().size==metrics.size) { context.tr(R.string.ui_neplatne_nazvy_vlastnich_mer_3f4d8) }
                require(!db.rawQuery("SELECT v.id FROM body_metric_values v LEFT JOIN measures m ON m.id=v.measurement_id LEFT JOIN body_metrics b ON b.id=v.metric_id WHERE m.id IS NULL OR b.id IS NULL LIMIT 1",null)
                    .use { it.moveToFirst() }) { context.tr(R.string.ui_vlastni_miry_maji_neplatne_vazby_48e9a) }
            }
            val records=measures()
            if(schema<7) require(records.all { it.kg!=null }) { context.tr(R.string.ui_starsi_zaloha_obsahuje_mereni_bez_hmotnosti_d5ac7) }
            records.forEach { validateMeasure(it.date,it.kg,it.waist,it.extra) }
            markSetupComplete(db)
        }
    }
}
