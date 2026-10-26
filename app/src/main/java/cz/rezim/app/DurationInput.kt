package cz.rezim.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.focus.onFocusChanged

// Explicit field count, never infer hours from the number of typed digits.
internal fun durationText(seconds:Int,hours:Boolean=true):String = if(hours)
    "%02d:%02d:%02d".format(java.util.Locale.US,seconds/3600,seconds/60%60,seconds%60)
    else "%02d:%02d".format(java.util.Locale.US,seconds/60,seconds%60)
internal fun durationSeconds(raw:String,hours:Boolean=true):Int? {
    val parts=raw.split(':')
    if(parts.size!=if(hours) 3 else 2) return null
    val n=parts.map {if(it.length>2 || it.any {c->c !in '0'..'9'}) return null else it.toIntOrNull() ?: 0}
    if(n.drop(1).any {it>59}) return null
    return if(hours) n[0]*3600+n[1]*60+n[2] else n[0]*60+n[1]
}
@Composable
internal fun DurationInput(label:String,value:String,onChange:(String)->Unit,hours:Boolean=true,tag:String="duration") {
    val context=LocalContext.current
    val size=if(hours) 3 else 2
    val latestValue by rememberUpdatedState(value)
    val latestChange by rememberUpdatedState(onChange)
    val fields=value.split(':').takeIf {it.size==size} ?: List(size){""}
    val labels=if(hours) listOf(R.string.time_hours,R.string.time_minutes,R.string.time_seconds) else listOf(R.string.time_minutes,R.string.time_seconds)
    Column(verticalArrangement=Arrangement.spacedBy(3.dp)) {
        Text("$label (${if(hours) "HH:MM:SS" else "MM:SS"})")
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(4.dp)) {
            fields.forEachIndexed {i,part ->
                if(i>0) Text(":")
                var input by remember {mutableStateOf(TextFieldValue(part))}
                if(input.text!=part) input=TextFieldValue(part,selection=TextRange(part.length))
                OutlinedTextField(value=input,onValueChange={nextInput->
                    val v=nextInput.text
                    if(v.length<=2 && v.all {it in '0'..'9'}) {
                        input=nextInput
                        if(v!=part) {
                            val next=(latestValue.split(':').takeIf {it.size==size} ?: List(size){""}).toMutableList()
                            next[i]=v;latestChange(next.joinToString(":"))
                        }
                    }
                },modifier=Modifier.weight(1f).testTag("$tag:$i").onFocusChanged {if(it.isFocused) input=input.copy(selection=TextRange(0,input.text.length))},singleLine=true,
                    label={Text(context.tr(labels[i]))},placeholder={Text("00")},
                    keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),
                    isError=part.toIntOrNull()?.let {i>0 && it>59}==true)
            }
        }
    }
}
