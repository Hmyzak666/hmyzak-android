package cz.rezim.app

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun LanguageSettings() {
    val context=LocalContext.current
    var expanded by remember {mutableStateOf(false)}
    val current=AppLanguage.selected(context)
    SectionCard {
        MutedHeading(context.tr(R.string.ui_jazyk_aplikace_638e7))
        Box {
            OutlinedButton(onClick={expanded=true},modifier=Modifier.fillMaxWidth()) {
                Text(if(current.isEmpty()) context.tr(R.string.ui_podle_telefonu_d478e) else AppLanguage.names.getOrElse(AppLanguage.supported.indexOf(current)) {current})
            }
            DropdownMenu(expanded=expanded,onDismissRequest={expanded=false}) {
                DropdownMenuItem(text={Text(context.tr(R.string.ui_podle_telefonu_d478e))},onClick={expanded=false;AppLanguage.choose(context,"")})
                AppLanguage.supported.forEachIndexed {i,language ->
                    DropdownMenuItem(text={Text(AppLanguage.names[i])},onClick={expanded=false;AppLanguage.choose(context,language)})
                }
            }
        }
        Text(context.tr(R.string.ui_vlastni_nazvy_cviku_a_poznamky_se_36c3e),fontSize=12.sp,color=SecondaryText)
    }
}
