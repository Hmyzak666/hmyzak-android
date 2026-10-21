package cz.rezim.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun SettingsScreen(vm:RezimState) {
    val view=LocalView.current
    LazyColumn(contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        item { Text("Nastavení",fontSize=23.sp,fontWeight=FontWeight.Bold) }
        item {
            SectionCard {
                MutedHeading("Vzhled a ovládání")
                Text("Tři varianty. Všechny zachovávají tmavý AMOLED styl a rychlý zápis.",fontSize=12.sp,color=SecondaryText)
                Row(horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                    listOf("Kompaktní","Vyvážený","Studio").forEachIndexed{ i,name ->
                        FilterChip(selected=vm.designMode==i,onClick={vm.setDesign(i)},
                            modifier=Modifier.weight(1f),label={Text(name,fontSize=11.sp,maxLines=1)})
                    }
                }
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                    Text("Vibrace po odkliknutí série",color=SecondaryText,fontSize=12.sp,
                        modifier=Modifier.weight(1f))
                    Switch(checked=vm.hapticEnabled,onCheckedChange=vm::setHaptic)
                }
                if(vm.hapticEnabled) {
                    Row(horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick={
                            vm.tell(when(performSetHaptic(view.context,test=true)) {
                                HapticResult.REQUESTED -> "Zkušební impulz odeslán. Pokud ho necítíš, zkontroluj intenzitu systémových vibrací."
                                HapticResult.SYSTEM_DISABLED -> "V telefonu jsou vypnuté vibrace dotyku. Zapni je v nastavení zvuku a vibrací."
                                HapticResult.NO_VIBRATOR -> "Telefon nehlásí dostupný vibrátor."
                                HapticResult.UNAVAILABLE -> "Vibraci se nepodařilo spustit. Zkontroluj nastavení zvuku a vibrací."
                            })
                        }) {Text("Vyzkoušet vibraci",fontSize=12.sp)}
                        TextButton(onClick={
                            if(!openHapticSettings(view.context)) vm.tell("Otevři v telefonu Nastavení → Zvuky a vibrace.")
                        }) {Text("Nastavení vibrací",fontSize=12.sp)}
                    }
                }
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                    Text("Automaticky posouvat na další sérii",color=SecondaryText,fontSize=12.sp,
                        modifier=Modifier.weight(1f))
                    Switch(checked=vm.autoScrollEnabled,onCheckedChange=vm::setAutoScroll)
                }
            }
        }
        item {
            SectionCard {
                MutedHeading("Pauzy při tréninku")
                RestTimerPreferences(vm)
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    Text("Vibrace při skončení pauzy",modifier=Modifier.weight(1f),fontSize=12.sp)
                    Switch(checked=vm.restHapticEnabled,onCheckedChange=vm::setRestHaptic)
                }
                Text("Bez zvuku. Vibrace respektují nastavení telefonu. Časovač běží i po zhasnutí displeje; restart telefonu jej zruší.",color=SecondaryText,fontSize=12.sp)
                TextButton(onClick={vm.tell(when(performRestHaptic(view.context)) {
                    HapticResult.REQUESTED -> "Zkušební vibrace konce pauzy odeslána."
                    HapticResult.NO_VIBRATOR -> "Telefon nehlásí dostupný vibrátor."
                    else -> "Vibraci se nepodařilo spustit. Zkontroluj nastavení telefonu."
                })}) {Text("Vyzkoušet konec pauzy")}
                TimerNotificationAccess()
            }
        }
        item { Spacer(Modifier.height(32.dp)) }
    }
}
