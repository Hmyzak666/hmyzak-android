package cz.rezim.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun TemplatePicker(onSelect:(TrainingTemplate)->Unit,onDismiss:()->Unit,append:Boolean=false) {
    val context=LocalContext.current
    AlertDialog(onDismissRequest=onDismiss,title={Text(context.tr(R.string.setup_choose))},
        text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text(context.tr(if(append) R.string.template_append_info else R.string.template_info),fontSize=13.sp)
            TrainingTemplates.all.forEach {template ->
                OutlinedButton(onClick={onSelect(template)},modifier=Modifier.fillMaxWidth().testTag("template-${template.id}")) {
                    Column(Modifier.fillMaxWidth().padding(vertical=8.dp)) {
                        Text(context.tr(template.name),fontWeight=FontWeight.Bold)
                        Text(context.tr(template.detail),fontSize=12.sp)
                    }
                }
            }
        }},confirmButton={},dismissButton={TextButton(onClick=onDismiss) {Text(context.tr(R.string.ui_zrusit_9bd5e))}})
}
@Composable
internal fun OnboardingScreen(vm:RezimState,onRestore:()->Unit) {
    val context=LocalContext.current
    var choose by rememberSaveable {mutableStateOf(false)}
    var failure by remember {mutableStateOf<String?>(null)}
    if(choose) TemplatePicker(onSelect={template ->
        runCatching {vm.completeSetup(template)}.onFailure {failure=it.localizedMessage}
    },onDismiss={choose=false})
    Surface(color=PitchBlack,modifier=Modifier.fillMaxSize().testTag("onboarding")) {
        Box(Modifier.fillMaxSize().safeDrawingPadding(),contentAlignment=Alignment.Center) {
            LazyColumn(Modifier.widthIn(max=640.dp).fillMaxWidth(),contentPadding=PaddingValues(24.dp),
                verticalArrangement=Arrangement.spacedBy(18.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                item {Image(painterResource(R.mipmap.ic_launcher),contentDescription=null,modifier=Modifier.size(112.dp))}
                item {Text(context.tr(R.string.setup_welcome),fontSize=29.sp,fontWeight=FontWeight.Bold)}
                item {Text(context.tr(R.string.setup_tagline),color=StrengthGreen,fontSize=18.sp)}
                item {Text(context.tr(R.string.setup_intro),fontSize=14.sp,color=SecondaryText)}
                item {Button(onClick={runCatching {vm.completeSetup()}.onFailure {failure=it.localizedMessage}},
                    modifier=Modifier.fillMaxWidth().testTag("start-empty")) {Text(context.tr(R.string.setup_empty))}}
                item {OutlinedButton(onClick={choose=true},modifier=Modifier.fillMaxWidth().testTag("choose-template")) {Text(context.tr(R.string.setup_choose))}}
                item {TextButton(onClick=onRestore,modifier=Modifier.testTag("setup-restore")) {Text(context.tr(R.string.ui_obnovit_zalohu_f0146))}}
                failure?.let {item {Text(it,color=HiitOrange)}}
                item {LanguageSettings()}
            }
        }
    }
}
@Composable
internal fun CreateDayDialog(vm:RezimState,onDismiss:()->Unit) {
    val context=LocalContext.current
    var name by rememberSaveable {mutableStateOf("")}
    AlertDialog(onDismissRequest=onDismiss,title={Text(context.tr(R.string.ui_treninkovy_den_ef7ee))},
        text={OutlinedTextField(value=name,onValueChange={name=it},label={Text(context.tr(R.string.day_name))},singleLine=true)},
        confirmButton={TextButton(onClick={vm.createDay(name);onDismiss()},enabled=name.isNotBlank()) {Text(context.tr(R.string.ui_pridat_2cf8c))}},
        dismissButton={TextButton(onClick=onDismiss) {Text(context.tr(R.string.ui_zrusit_9bd5e))}})
}
@Composable
internal fun EmptyPlansScreen(vm:RezimState) {
    val context=LocalContext.current
    var create by rememberSaveable {mutableStateOf(false)}
    var templates by rememberSaveable {mutableStateOf(false)}
    if(create) CreateDayDialog(vm) {create=false}
    if(templates) TemplatePicker(onSelect={if(vm.appendTemplate(it)) templates=false},onDismiss={templates=false},append=true)
    LazyColumn(Modifier.fillMaxSize().testTag("empty-plans"),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        item {Text(context.tr(R.string.empty_plans),fontSize=23.sp,fontWeight=FontWeight.Bold)}
        item {Text(context.tr(R.string.empty_plans_detail),color=SecondaryText)}
        item {Button(onClick={create=true}) {Text(context.tr(R.string.ui_treninkovy_den_ef7ee))}}
        item {OutlinedButton(onClick={templates=true}) {Text(context.tr(R.string.setup_choose))}}
    }
}
@Composable
internal fun HiitActivityPicker(activity:String,onChange:(String)->Unit) {
    val context=LocalContext.current
    var expanded by remember {mutableStateOf(false)}
    Box {
        OutlinedButton(onClick={expanded=true}) {
            val label=HiitActivity.entries.firstOrNull {it.name==activity}?.label?:R.string.activity_other
            Text(context.tr(R.string.activity_label)+": "+context.tr(label))
        }
        DropdownMenu(expanded=expanded,onDismissRequest={expanded=false}) {
            HiitActivity.entries.forEach {type -> DropdownMenuItem(text={Text(context.tr(type.label))},
                onClick={onChange(type.name);expanded=false})}
        }
    }
}
