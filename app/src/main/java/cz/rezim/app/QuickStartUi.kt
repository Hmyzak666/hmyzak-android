package cz.rezim.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
internal fun shortcutLabel(vm:RezimState,s:QuickShortcut):String {
    val c=LocalContext.current
    return s.name.ifBlank {when(s.kind) {
        "STRENGTH" -> vm.days.firstOrNull {it.key==s.target}?.name ?: c.tr(R.string.shortcut_missing_plan)
        "CARDIO" -> c.tr(CardioActivity.valueOf(s.target).label)
        "HIIT" -> "HIIT · ${s.rounds} × ${durationText(s.work,false)} / ${durationText(s.rest,false)}"
        else -> c.tr(R.string.ui_mereni_d2fc6)
    }}
}
@Composable
internal fun QuickStartContent(vm:RezimState) {
    val c=LocalContext.current
    var manager by rememberSaveable {mutableStateOf(false)}
    var editId by rememberSaveable {mutableStateOf<String?>(null)}
    var adding by rememberSaveable {mutableStateOf(false)}
    FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        vm.quickShortcuts.forEach {s->
            val label=shortcutLabel(vm,s)
            OutlinedButton(onClick={vm.launchShortcut(s)},modifier=Modifier.testTag(if(s.kind=="BODY") "quick-measurement" else "shortcut:${s.id}")) {Text(label)}
        }
    }
    TextButton(onClick={manager=true},modifier=Modifier.testTag("edit-shortcuts")) {Text(c.tr(R.string.shortcut_edit))}
    if(manager) AlertDialog(onDismissRequest={manager=false},title={Text(c.tr(R.string.shortcut_edit))},
        text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            vm.quickShortcuts.forEachIndexed {i,s->
                Column {
                    Text(shortcutLabel(vm,s))
                    FlowRow(horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick={editId=s.id}) {Text(c.tr(R.string.shortcut_change))}
                        TextButton(enabled=i>0,onClick={vm.moveShortcut(i,-1)}) {Text(c.tr(R.string.shortcut_up))}
                        TextButton(enabled=i<vm.quickShortcuts.lastIndex,onClick={vm.moveShortcut(i,1)}) {Text(c.tr(R.string.shortcut_down))}
                        TextButton(onClick={vm.saveShortcuts(vm.quickShortcuts.filterNot {it.id==s.id})}) {Text(c.tr(R.string.shortcut_remove))}
                    }
                    HorizontalDivider()
                }
            }
            TextButton(onClick={adding=true},enabled=vm.quickShortcuts.size<100) {Text(c.tr(R.string.shortcut_add))}
        }},confirmButton={TextButton(onClick={manager=false}) {Text(c.tr(R.string.ui_zavrit_ae288))}})
    if(adding || editId!=null) ShortcutEditor(vm,vm.quickShortcuts.firstOrNull {it.id==editId}) {adding=false;editId=null}
}
internal fun RezimState.moveShortcut(index:Int,offset:Int) {
    val list=quickShortcuts.toMutableList();val next=index+offset
    if(index !in list.indices || next !in list.indices) return
    val item=list.removeAt(index);list.add(next,item);saveShortcuts(list)
}
@Composable
private fun ShortcutEditor(vm:RezimState,old:QuickShortcut?,onClose:()->Unit) {
    val c=LocalContext.current
    var kind by rememberSaveable(old?.id) {mutableStateOf(old?.kind ?: "CARDIO")}
    var target by rememberSaveable(old?.id) {mutableStateOf(old?.target ?: "RUNNING")}
    var name by rememberSaveable(old?.id) {mutableStateOf(old?.name.orEmpty())}
    var rounds by rememberSaveable(old?.id) {mutableStateOf((old?.rounds ?: 5).toString())}
    var work by rememberSaveable(old?.id) {mutableStateOf(durationText(old?.work ?: 60,false))}
    var rest by rememberSaveable(old?.id) {mutableStateOf(durationText(old?.rest ?: 120,false))}
    AlertDialog(onDismissRequest=onClose,title={Text(c.tr(R.string.shortcut_add))},
        text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            FlowRow(horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                listOf("STRENGTH" to R.string.ui_sila_c50bc,"CARDIO" to R.string.cardio_title,"HIIT" to R.string.ui_hiit_199e5,"BODY" to R.string.ui_mereni_d2fc6).forEach {(key,label)->
                    FilterChip(selected=kind==key,onClick={kind=key;target=if(key=="STRENGTH") vm.days.firstOrNull()?.key.orEmpty() else "RUNNING"},label={Text(c.tr(label))})
                }
            }
            if(kind=="STRENGTH") {
                if(vm.days.isEmpty()) Text(c.tr(R.string.empty_plans_detail))
                vm.days.forEach {d->FilterChip(selected=target==d.key,onClick={target=d.key},label={Text(d.name)})}
            }
            if(kind=="CARDIO") CardioPicker(target) {target=it}
            if(kind=="HIIT") {
                DataInput(c.tr(R.string.ui_pocet_intervalu_11b22),rounds,{rounds=it},Modifier.fillMaxWidth(),KeyboardType.Number)
                DurationInput(c.tr(R.string.time_work),work,{work=it},hours=false,tag="preset-work")
                DurationInput(c.tr(R.string.time_rest),rest,{rest=it},hours=false,tag="preset-rest")
            }
            DataInput(c.tr(R.string.shortcut_name),name,{name=it},Modifier.fillMaxWidth())
        }},confirmButton={TextButton(onClick={
            try {
                if(kind=="STRENGTH") require(vm.days.any {it.key==target})
                val s=QuickShortcut(id=old?.id ?: java.util.UUID.randomUUID().toString(),kind=kind,target=target,name=name.trim(),
                    rounds=requireNotNull(rounds.toIntOrNull()),work=requireNotNull(durationSeconds(work,false)),rest=requireNotNull(durationSeconds(rest,false)))
                vm.saveShortcuts(if(old==null) vm.quickShortcuts+s else vm.quickShortcuts.map {if(it.id==old.id) s else it})
                onClose()
            } catch(_:IllegalArgumentException) {vm.tell(c.tr(R.string.cardio_invalid))}
        }) {Text(c.tr(R.string.ui_ulozit_85e82))}},dismissButton={TextButton(onClick=onClose) {Text(c.tr(R.string.ui_zrusit_9bd5e))}})
}
