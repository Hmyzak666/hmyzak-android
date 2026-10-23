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
    val context=androidx.compose.ui.platform.LocalContext.current

    val view=LocalView.current
    LazyColumn(contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        item { Text(context.tr(R.string.ui_nastaveni_55f52),fontSize=23.sp,fontWeight=FontWeight.Bold) }
        item {
            SectionCard {
                MutedHeading(context.tr(R.string.ui_vzhled_a_ovladani_5f2eb))
                Text(context.tr(R.string.ui_tri_varianty_vsechny_zachovavaji_tmavy_amoled_93176),fontSize=12.sp,color=SecondaryText)
                Row(horizontalArrangement=Arrangement.spacedBy(5.dp)) {
                    listOf(context.tr(R.string.ui_kompaktni_ee1b7),context.tr(R.string.ui_vyvazeny_4959b),"Studio").forEachIndexed{ i,name ->
                        FilterChip(selected=vm.designMode==i,onClick={vm.setDesign(i)},
                            modifier=Modifier.weight(1f),label={Text(name,fontSize=11.sp,maxLines=1)})
                    }
                }
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                    Text(context.tr(R.string.ui_vibrace_po_odkliknuti_serie_1b811),color=SecondaryText,fontSize=12.sp,
                        modifier=Modifier.weight(1f))
                    Switch(checked=vm.hapticEnabled,onCheckedChange=vm::setHaptic)
                }
                if(vm.hapticEnabled) {
                    Row(horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick={
                            vm.tell(when(performSetHaptic(view.context,test=true)) {
                                HapticResult.REQUESTED -> context.tr(R.string.ui_zkusebni_impulz_odeslan_pokud_ho_necitis_10092)
                                HapticResult.SYSTEM_DISABLED -> context.tr(R.string.ui_v_telefonu_jsou_vypnute_vibrace_dotyku_be58d)
                                HapticResult.NO_VIBRATOR -> context.tr(R.string.ui_telefon_nehlasi_dostupny_vibrator_6ce85)
                                HapticResult.UNAVAILABLE -> context.tr(R.string.ui_vibraci_se_nepodarilo_spustit_zkontroluj_nastaveni_ca492)
                            })
                        }) {Text(context.tr(R.string.ui_vyzkouset_vibraci_0249a),fontSize=12.sp)}
                        TextButton(onClick={
                            if(!openHapticSettings(view.context)) vm.tell(context.tr(R.string.ui_otevri_v_telefonu_nastaveni_zvuky_a_31c47))
                        }) {Text(context.tr(R.string.ui_nastaveni_vibraci_b759f),fontSize=12.sp)}
                    }
                }
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                    Text(context.tr(R.string.ui_automaticky_posouvat_na_dalsi_serii_e5c5d),color=SecondaryText,fontSize=12.sp,
                        modifier=Modifier.weight(1f))
                    Switch(checked=vm.autoScrollEnabled,onCheckedChange=vm::setAutoScroll)
                }
            }
        }
        item {
            SectionCard {
                MutedHeading(context.tr(R.string.ui_pauzy_pri_treninku_f2df6))
                RestTimerPreferences(vm)
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    Text(context.tr(R.string.ui_vibrace_pri_skonceni_pauzy_25743),modifier=Modifier.weight(1f),fontSize=12.sp)
                    Switch(checked=vm.restHapticEnabled,onCheckedChange=vm::setRestHaptic)
                }
                Text(context.tr(R.string.ui_bez_zvuku_vibrace_respektuji_nastaveni_telefonu_41c1c),color=SecondaryText,fontSize=12.sp)
                TextButton(onClick={vm.tell(when(performRestHaptic(view.context)) {
                    HapticResult.REQUESTED -> context.tr(R.string.ui_zkusebni_vibrace_konce_pauzy_odeslana_60d05)
                    HapticResult.NO_VIBRATOR -> context.tr(R.string.ui_telefon_nehlasi_dostupny_vibrator_6ce85)
                    else -> context.tr(R.string.ui_vibraci_se_nepodarilo_spustit_zkontroluj_nastaveni_81749)
                })}) {Text(context.tr(R.string.ui_vyzkouset_konec_pauzy_581d1))}
                TimerNotificationAccess()
            }
        }
        item { LanguageSettings() }
        item { ProSettings(vm) }
        item { Spacer(Modifier.height(32.dp)) }
    }
}
