package cz.rezim.app

import android.content.Context

enum class CardioActivity(val label:Int) {
    RUNNING(R.string.cardio_running), TREADMILL(R.string.cardio_treadmill), WALKING(R.string.cardio_walking),
    ELLIPTICAL(R.string.cardio_elliptical), CYCLING(R.string.cardio_cycling), STATIONARY_BIKE(R.string.cardio_bike),
    ROWING(R.string.cardio_rowing), OTHER(R.string.cardio_other)
}
internal fun cardioName(context:Context,r:RunEntry):String = if(r.activity=="OTHER") r.customName else
    context.tr(CardioActivity.valueOf(r.activity).label)
internal fun validateCardio(r:RunEntry) {
    require(validDate(r.date) && r.seconds in 1..359999)
    require(r.km.isFinite() && r.km in 0.0..500.0)
    require(CardioActivity.entries.any {it.name==r.activity})
    require(r.customName.length<=80 && (r.activity!="OTHER" || r.customName.isNotBlank()))
    require(listOf(r.avg,r.max).all {it==null || it in 30..240})
    require(r.avg==null || r.max==null || r.avg<=r.max)
    require(r.resistance==null || (r.resistance.isFinite() && r.resistance in 0.0..999.0))
    require(r.incline==null || (r.incline.isFinite() && r.incline in -100.0..100.0))
}
