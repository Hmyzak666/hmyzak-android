package cz.rezim.app

import androidx.annotation.StringRes

internal data class TemplateExercise(@StringRes val name:Int,val sets:Int=3,
    val unit:UnitType=UnitType.KG,val multiplier:Int=1)
internal data class TemplateDay(@StringRes val name:Int,val exercises:List<TemplateExercise>)
internal data class TrainingTemplate(val id:String,@StringRes val name:Int,@StringRes val detail:Int,val days:List<TemplateDay>)

// General editable starting points; no personal history, fixed weekdays or prescribed weights.
internal object TrainingTemplates {
    private fun e(name:Int,unit:UnitType=UnitType.KG,multiplier:Int=1)=TemplateExercise(name,3,unit,multiplier)
    private val squat=e(R.string.ex_squat)
    private val hinge=e(R.string.ex_rdl)
    private val bench=e(R.string.ex_bench)
    private val row=e(R.string.ex_row)
    private val pull=e(R.string.ex_pulldown)
    private val press=e(R.string.ex_shoulder_press,multiplier=2)
    private val core=e(R.string.ex_crunch,UnitType.REPS)
    private fun day(name:Int,vararg exercises:TemplateExercise)=TemplateDay(name,exercises.toList())
    val all=listOf(
        TrainingTemplate("full_body",R.string.template_full_body,R.string.template_full_body_detail,listOf(
            day(R.string.day_full_a,squat,bench,row,e(R.string.ex_leg_curl),core),
            day(R.string.day_full_b,hinge,press,pull,e(R.string.ex_split_squat,multiplier=2),core),
            day(R.string.day_full_c,e(R.string.ex_leg_press),e(R.string.ex_incline_bench,multiplier=2),row,e(R.string.ex_hip_thrust),e(R.string.ex_calf))
        )),
        TrainingTemplate("upper_lower",R.string.template_upper_lower,R.string.template_upper_lower_detail,listOf(
            day(R.string.day_upper_a,bench,row,press,pull,e(R.string.ex_curl,multiplier=2)),
            day(R.string.day_lower_a,squat,hinge,e(R.string.ex_leg_curl),e(R.string.ex_calf),core),
            day(R.string.day_upper_b,e(R.string.ex_incline_bench,multiplier=2),pull,row,e(R.string.ex_lateral,multiplier=2),e(R.string.ex_triceps)),
            day(R.string.day_lower_b,e(R.string.ex_leg_press),e(R.string.ex_hip_thrust),e(R.string.ex_leg_extension),e(R.string.ex_calf),core)
        )),
        TrainingTemplate("ppl",R.string.template_ppl,R.string.template_ppl_detail,listOf(
            day(R.string.day_push,bench,press,e(R.string.ex_incline_bench,multiplier=2),e(R.string.ex_lateral,multiplier=2),e(R.string.ex_triceps)),
            day(R.string.day_pull,row,pull,e(R.string.ex_rear_delt,multiplier=2),e(R.string.ex_curl,multiplier=2)),
            day(R.string.day_legs,squat,hinge,e(R.string.ex_leg_curl),e(R.string.ex_calf),core)
        ))
    )
}
internal enum class HiitActivity(@StringRes val label:Int) {
    OTHER(R.string.activity_other),ELLIPTICAL(R.string.activity_elliptical),RUNNING(R.string.activity_running),
    CYCLING(R.string.activity_cycling),ROWING(R.string.activity_rowing)
}
