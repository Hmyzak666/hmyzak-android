package cz.rezim.app

import android.content.ContentValues
import androidx.core.database.sqlite.transaction

// Test-only setup for historical regression cases. Production never calls this seed.
internal fun LocalStore.withLegacyTestPlans():LocalStore {
    if(plans().isNotEmpty()) return this
    val db=writableDatabase
    db.transaction {
        Plans.days.forEachIndexed {index,day ->
            db.insertOrThrow("plan_days",null,ContentValues().apply {
                put("id",day.key);put("short",day.short);put("name",day.name);put("position",index)
            })
            day.exercises.forEachIndexed {pos,ex -> db.insertOrThrow("plan_exercises",null,ContentValues().apply {
                put("id",ex.key);put("day_id",day.key);put("name",ex.name);put("sets",ex.sets)
                put("unit",ex.type.name);put("position",pos)
                val partner=Plans.initialPartners[ex.key]
                put("partner_id",partner)
                if(partner!=null) put("superset_id","legacy_"+minOf(ex.key,partner))
                put("load_multiplier",if(ex.key in setOf("flat_db_bench","incline_db_bench","neutral_db_ohp","lateral_mon","lateral_fri","rear_delt"))2 else 1)
            })}
        }
        completeSetup()
    }
    return this
}
