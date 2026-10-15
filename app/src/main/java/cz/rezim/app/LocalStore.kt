package cz.rezim.app

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.time.LocalDate
import java.time.format.DateTimeFormatter

internal fun currentDate(): String = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
internal fun String.asNumber(): Double? = trim().replace(',', '.').toDoubleOrNull()
internal fun fmtNumber(n: Double): String = if (n % 1.0 == 0.0) "${n.toLong()}" else "%.2f".format(java.util.Locale.US, n).trimEnd('0').trimEnd('.')

data class Session(val id: Long, val day: String, val created: Long, val finished: Long?)
data class SetEntry(val id: Long, val session: Long, val exercise: String, val number: Int,
                             val weight: String, val reps: String, val done: Boolean)
data class RunEntry(val id: Long, val date: String, val km: Double, val seconds: Int,
                             val avg: Int?, val max: Int?, val note: String)
data class HiitEntry(val id: Long, val date: String, val minutes: Int, val rounds: Int,
                              val work: Int, val rest: Int, val avg: Int?, val max: Int?, val note: String)
data class MeasureEntry(val id: Long, val date: String, val kg: Double, val waist: Double)

internal class LocalStore(context: Context) : SQLiteOpenHelper(context, "rezim_local_v1.db", null, 2) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE sessions (id INTEGER PRIMARY KEY AUTOINCREMENT, day TEXT NOT NULL, created INTEGER NOT NULL, finished INTEGER)")
        db.execSQL("CREATE TABLE workout_sets (id INTEGER PRIMARY KEY AUTOINCREMENT, session INTEGER NOT NULL, exercise TEXT NOT NULL, set_number INTEGER NOT NULL, weight TEXT NOT NULL DEFAULT '', reps TEXT NOT NULL DEFAULT '', done INTEGER NOT NULL DEFAULT 0, FOREIGN KEY(session) REFERENCES sessions(id) ON DELETE CASCADE)")
        db.execSQL("CREATE INDEX sets_session ON workout_sets(session)")
        db.execSQL("CREATE TABLE runs (id INTEGER PRIMARY KEY AUTOINCREMENT, date TEXT NOT NULL, km REAL NOT NULL, seconds INTEGER NOT NULL, avg_hr INTEGER, max_hr INTEGER, note TEXT NOT NULL DEFAULT '')")
        db.execSQL("CREATE TABLE hiit (id INTEGER PRIMARY KEY AUTOINCREMENT, date TEXT NOT NULL, minutes INTEGER NOT NULL, rounds INTEGER NOT NULL, work_seconds INTEGER NOT NULL, rest_seconds INTEGER NOT NULL, avg_hr INTEGER, max_hr INTEGER, note TEXT NOT NULL DEFAULT '')")
        db.execSQL("CREATE TABLE measures (id INTEGER PRIMARY KEY AUTOINCREMENT, date TEXT NOT NULL, kg REAL NOT NULL, waist REAL NOT NULL)")
        createPlanTables(db)
        seedPlans(db)
        // Only runs whose exact values the user already recorded in chat. No invented strength records.
        val knownRuns = listOf(
            arrayOf("2026-08-14", 9.70, 3642, 143, 155),
            arrayOf("2026-08-20", 10.03, 3619, 146, 162),
            arrayOf("2026-10-08", 5.37, 1906, 148, 160),
            arrayOf("2026-10-10", 7.16, 2644, 146, 160)
        )
        for (r in knownRuns) {
            val v = ContentValues().apply {
                put("date", r[0] as String); put("km", r[1] as Double)
                put("seconds", r[2] as Int); put("avg_hr", r[3] as Int)
                put("max_hr", r[4] as Int); put("note", "Dřívější záznam z konverzace")
            }
            db.insertOrThrow("runs", null, v)
        }
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if(oldVersion < 2) {
            createPlanTables(db)
            seedPlans(db)
        }
    }
    private fun createPlanTables(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS plan_days (id TEXT PRIMARY KEY NOT NULL, short TEXT NOT NULL, name TEXT NOT NULL, position INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS plan_exercises (id TEXT PRIMARY KEY NOT NULL, day_id TEXT NOT NULL, name TEXT NOT NULL, sets INTEGER NOT NULL, unit TEXT NOT NULL, position INTEGER NOT NULL, partner_id TEXT)")
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
                "SELECT id,name,sets,unit,partner_id FROM plan_exercises WHERE day_id=? ORDER BY position,id",arrayOf(id)
            ).use { e -> buildList {
                while(e.moveToNext()) add(Exercise(e.getString(0),e.getString(1),e.getInt(2),
                    runCatching { UnitType.valueOf(e.getString(3)) }.getOrDefault(UnitType.KG),
                    if(e.isNull(4)) null else e.getString(4)))
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
    fun moveExercise(day:String,exercise:String,offset:Int) {
        val ids=readableDatabase.rawQuery("SELECT id FROM plan_exercises WHERE day_id=? ORDER BY position,id",arrayOf(day))
            .use { c->buildList { while(c.moveToNext()) add(c.getString(0)) } }
        val i=ids.indexOf(exercise);val to=i+offset
        if(i<0 || to !in ids.indices) return
        val reordered=ids.toMutableList().apply { val id=removeAt(i);add(to,id) }
        writableDatabase.beginTransaction()
        try {
            reordered.forEachIndexed {index,key ->
                writableDatabase.update("plan_exercises",ContentValues().apply{put("position",index)},"id=?",arrayOf(key))
            }
            writableDatabase.setTransactionSuccessful()
        } finally { writableDatabase.endTransaction() }
    }
    fun removeExercise(day:String,exercise:String) {
        val db=writableDatabase;db.beginTransaction()
        try {
            db.delete("plan_exercises","id=? AND day_id=?",arrayOf(exercise,day))
            db.execSQL("UPDATE plan_exercises SET partner_id=NULL WHERE partner_id=?",arrayOf(exercise))
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }
    fun removeDay(day:String) {
        val db=writableDatabase;db.beginTransaction()
        try {
            db.delete("plan_exercises","day_id=?",arrayOf(day))
            db.delete("plan_days","id=?",arrayOf(day))
            db.setTransactionSuccessful()
        } finally {db.endTransaction()}
    }
    fun pair(day:String,a:String,b:String?) {
        val db=writableDatabase; db.beginTransaction()
        try {
            val former=plans().firstOrNull{it.key==day}?.exercises?.firstOrNull{it.key==a}?.partnerKey
            db.execSQL("UPDATE plan_exercises SET partner_id=NULL WHERE day_id=? AND (id=? OR partner_id=?)",arrayOf(day,a,a))
            if(former!=null) db.execSQL("UPDATE plan_exercises SET partner_id=NULL WHERE day_id=? AND id=?",arrayOf(day,former))
            if(b!=null && b!=a) {
                db.execSQL("UPDATE plan_exercises SET partner_id=NULL WHERE day_id=? AND (id=? OR partner_id=?)",arrayOf(day,b,b))
                db.execSQL("UPDATE plan_exercises SET partner_id=? WHERE day_id=? AND id=?",arrayOf(b,day,a))
                db.execSQL("UPDATE plan_exercises SET partner_id=? WHERE day_id=? AND id=?",arrayOf(a,day,b))
            }
            db.setTransactionSuccessful()
        } finally {db.endTransaction()}
    }
    private fun Cursor.longOrNull(col: String): Long? { val p = getColumnIndexOrThrow(col); return if (isNull(p)) null else getLong(p) }
    private fun Cursor.intOrNull(col: String): Int? { val p = getColumnIndexOrThrow(col); return if (isNull(p)) null else getInt(p) }

    fun openSession(day: String): Session? = readableDatabase.rawQuery(
        "SELECT * FROM sessions WHERE day=? AND finished IS NULL ORDER BY id DESC LIMIT 1", arrayOf(day)
    ).use { c -> if (c.moveToFirst()) session(c) else null }
    private fun session(c: Cursor) = Session(c.getLong(c.getColumnIndexOrThrow("id")),
        c.getString(c.getColumnIndexOrThrow("day")), c.getLong(c.getColumnIndexOrThrow("created")), c.longOrNull("finished"))
    fun recentSessions(limit: Int = 15): List<Session> = readableDatabase.rawQuery(
        "SELECT * FROM sessions WHERE finished IS NOT NULL ORDER BY finished DESC LIMIT $limit", null
    ).use { c -> buildList { while(c.moveToNext()) add(session(c)) } }
    fun previous(day: String, exercise: String, no: Int): SetEntry? = readableDatabase.rawQuery(
        """SELECT w.* FROM workout_sets w JOIN sessions s ON s.id=w.session
           WHERE s.day=? AND s.finished IS NOT NULL AND w.exercise=? AND w.set_number=? AND w.done=1
           ORDER BY s.finished DESC,w.id DESC LIMIT 1""".trimIndent(),
        arrayOf(day, exercise, no.toString())
    ).use { c -> if(c.moveToFirst()) set(c) else null }

    fun beginWorkout(day: String): Session {
        openSession(day)?.let { return it }
        val db = writableDatabase
        db.beginTransaction()
        try {
            val v = ContentValues().apply { put("day", day); put("created", System.currentTimeMillis()) }
            val id = db.insertOrThrow("sessions", null, v)
            for (e in plans().first {it.key==day}.exercises) for (n in 1..e.sets) {
                val prev = previous(day, e.key, n)
                val row = ContentValues().apply {
                    put("session", id); put("exercise", e.key); put("set_number", n)
                    put("weight", prev?.weight ?: if (e.type == UnitType.ADDED_KG) "0" else "")
                    put("reps", prev?.reps ?: ""); put("done", 0)
                }
                db.insertOrThrow("workout_sets", null, row)
            }
            db.setTransactionSuccessful()
            return Session(id, day, System.currentTimeMillis(), null)
        } finally { db.endTransaction() }
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
    fun finishWorkout(id: Long) {
        writableDatabase.execSQL("UPDATE sessions SET finished=? WHERE id=? AND finished IS NULL", arrayOf(System.currentTimeMillis(), id))
    }
    fun completedSets(): List<Pair<Session,SetEntry>> {
        val sessions=recentSessions(2000)
        return sessions.flatMap { session ->
            sets(session.id).filter {it.done}.map {session to it}
        }
    }
    fun countSessions(): Int = readableDatabase.rawQuery("SELECT count(*) FROM sessions WHERE finished IS NOT NULL", null).use { c -> c.moveToFirst(); c.getInt(0) }

    fun addRun(date: String, km: Double, seconds: Int, avg: Int?, max: Int?, note: String) {
        val v = ContentValues().apply { put("date",date); put("km",km); put("seconds",seconds); put("avg_hr",avg); put("max_hr",max); put("note",note) }
        writableDatabase.insertOrThrow("runs",null,v)
    }
    fun runs(): List<RunEntry> = readableDatabase.rawQuery("SELECT * FROM runs ORDER BY date DESC,id DESC", null).use { c -> buildList {
        while(c.moveToNext()) add(RunEntry(c.getLong(0),c.getString(1),c.getDouble(2),c.getInt(3),c.intOrNull("avg_hr"),c.intOrNull("max_hr"),c.getString(6)))
    } }
    fun addHiit(date: String, minutes: Int, rounds: Int, work: Int, rest: Int, avg: Int?, max: Int?, note: String) {
        val v = ContentValues().apply { put("date",date); put("minutes",minutes); put("rounds",rounds); put("work_seconds",work); put("rest_seconds",rest); put("avg_hr",avg); put("max_hr",max); put("note",note) }
        writableDatabase.insertOrThrow("hiit",null,v)
    }
    fun hiit(): List<HiitEntry> = readableDatabase.rawQuery("SELECT * FROM hiit ORDER BY date DESC,id DESC", null).use { c -> buildList {
        while(c.moveToNext()) add(HiitEntry(c.getLong(0),c.getString(1),c.getInt(2),c.getInt(3),c.getInt(4),c.getInt(5),c.intOrNull("avg_hr"),c.intOrNull("max_hr"),c.getString(8)))
    } }
    fun addMeasure(date:String,kg:Double,waist:Double) {
        val v=ContentValues().apply { put("date",date); put("kg",kg); put("waist",waist) }
        writableDatabase.insertOrThrow("measures",null,v)
    }
    fun measures():List<MeasureEntry> = readableDatabase.rawQuery("SELECT * FROM measures ORDER BY date DESC,id DESC",null).use { c -> buildList {
        while(c.moveToNext()) add(MeasureEntry(c.getLong(0),c.getString(1),c.getDouble(2),c.getDouble(3)))
    } }

    private val tables = linkedMapOf(
        "sessions" to listOf("id","day","created","finished"),
        "workout_sets" to listOf("id","session","exercise","set_number","weight","reps","done"),
        "runs" to listOf("id","date","km","seconds","avg_hr","max_hr","note"),
        "hiit" to listOf("id","date","minutes","rounds","work_seconds","rest_seconds","avg_hr","max_hr","note"),
        "measures" to listOf("id","date","kg","waist"),
        "plan_days" to listOf("id","short","name","position"),
        "plan_exercises" to listOf("id","day_id","name","sets","unit","position","partner_id")
    )
    fun exportJson():String {
        val root=JSONObject().put("format","rezim-local-backup").put("schemaVersion",2)
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
        return root.toString(2)
    }
    fun importJson(raw:String) {
        val root=JSONObject(raw)
        val schema=root.optInt("schemaVersion")
        require(root.optString("format")=="rezim-local-backup" && schema in 1..2) { "Nepodporovaný soubor zálohy." }
        // Old backups have no editable plans; restored plans then use the built-in defaults.
        val imported=tables.mapValues { (t,_) ->
            if(schema==1 && t.startsWith("plan_")) JSONArray() else root.getJSONArray(t)
        }
        val db=writableDatabase
        db.beginTransaction()
        try {
            for (t in tables.keys.reversed()) db.delete(t,null,null)
            for ((table,cols) in tables) {
                val arr=imported.getValue(table)
                for (i in 0 until arr.length()) {
                    val obj=arr.getJSONObject(i)
                    val values=ContentValues()
                    for (col in cols) {
                        if (obj.isNull(col)) continue
                        val v=obj.get(col)
                        when(v) {
                            is Int -> values.put(col,v)
                            is Long -> values.put(col,v)
                            is Double -> values.put(col,v)
                            is String -> values.put(col,v)
                            else -> error("Neplatná položka $col")
                        }
                    }
                    db.insertOrThrow(table,null,values)
                }
            }
            if(schema==1) seedPlans(db)
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }
}
