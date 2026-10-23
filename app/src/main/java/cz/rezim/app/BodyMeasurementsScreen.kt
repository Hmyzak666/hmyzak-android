package cz.rezim.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val measurementError = R.string.ui_zkontroluj_datum_a_vypln_alespon_jednu_0e80f
private val bodyValuesSaver = mapSaver<Map<String, String>>(
    save = { it }, restore = { values -> values.mapValues { (_, value) -> value as String } }
)

@Composable
fun BodyScreen(vm: RezimState) {
    val context=androidx.compose.ui.platform.LocalContext.current

    var form by rememberSaveable { mutableStateOf(false) }
    var date by rememberSaveable { mutableStateOf(currentDate()) }
    var kg by rememberSaveable { mutableStateOf("") }
    var waist by rememberSaveable { mutableStateOf("") }
    var extra by rememberSaveable(stateSaver = bodyValuesSaver) { mutableStateOf(emptyMap<String, String>()) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var editingId by rememberSaveable { mutableStateOf<Long?>(null) }
    var metricDialog by rememberSaveable { mutableStateOf(false) }
    var metricId by rememberSaveable { mutableStateOf<String?>(null) }
    var metricName by rememberSaveable { mutableStateOf("") }
    var metricError by rememberSaveable { mutableStateOf<String?>(null) }
    var trend by rememberSaveable { mutableStateOf("weight") }
    var chooseTrend by remember { mutableStateOf(false) }
    val trendOptions = listOf("weight" to context.tr(R.string.ui_hmotnost_kg_226e2), "waist" to context.tr(R.string.ui_bricho_cm_e2c84)) +
        vm.bodyMetrics.map { it.id to "${it.name} · cm" }
    val trendLabel = trendOptions.firstOrNull { it.first == trend }?.second ?: trendOptions.first().second
    val trendValues = vm.measures.asReversed().mapNotNull { record ->
        when (trend) { "weight" -> record.kg; "waist" -> record.waist; else -> record.extra[trend] }
    }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(context.tr(R.string.ui_telesne_miry_8809a), fontSize = 23.sp, fontWeight = FontWeight.Bold)
                    Text(context.tr(R.string.ui_zapis_jen_to_co_dnes_meris_f715f), color = SecondaryText, fontSize = 12.sp)
                }
                MainButton(if (form) context.tr(R.string.ui_zavrit_ae288) else context.tr(R.string.ui_mereni_d2fc6), color = BodyPurple) {
                    if (!form) date = currentDate()
                    error = null; form = !form
                }
            }
        }
        if (form) item {
            SectionCard {
                MutedHeading(context.tr(R.string.ui_nove_mereni_1ffed))
                DataInput(context.tr(R.string.ui_datum_rrrr_mm_dd_2ac3f), date, { date = it }, Modifier.fillMaxWidth())
                BodyMeasurementFields(vm.bodyMetrics, kg, { kg = it }, waist, { waist = it }, extra) { id, value ->
                    extra = extra + (id to value)
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
                MainButton(context.tr(R.string.ui_ulozit_mereni_d5840), modifier = Modifier.fillMaxWidth(), color = BodyPurple) {
                    val values = parseBodyMeasurement(kg, waist, extra)
                    if (!validDate(date) || values == null) error = context.tr(measurementError)
                    else if (vm.addMeasure(date, values)) {
                        kg = ""; waist = ""; extra = emptyMap(); error = null; form = false
                    } else error = vm.message
                }
            }
        }
        item {
            SectionCard {
                MutedHeading(context.tr(R.string.ui_vlastni_miry_9572f))
                Text(context.tr(R.string.ui_pridej_si_treba_biceps_stehno_hrudnik_76294),
                    color = SecondaryText, fontSize = 12.sp)
                vm.bodyMetrics.forEach { metric ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(metric.name, Modifier.weight(1f), fontSize = 14.sp)
                        TextButton(onClick = {
                            metricId = metric.id; metricName = metric.name; metricError = null; metricDialog = true
                        }) { Text(context.tr(R.string.ui_prejmenovat_f7f30), fontSize = 12.sp) }
                    }
                }
                OutlinedButton(onClick = {
                    metricId = null; metricName = ""; metricError = null; metricDialog = true
                }, modifier = Modifier.fillMaxWidth()) { Text(context.tr(R.string.ui_vlastni_mira_8f96e)) }
            }
        }
        if (vm.measures.isNotEmpty()) item {
            SectionCard {
                MutedHeading(context.tr(R.string.ui_vyvoj_mereni_1aa68))
                Box {
                    OutlinedButton(onClick = { chooseTrend = true }) { Text(trendLabel) }
                    DropdownMenu(expanded = chooseTrend, onDismissRequest = { chooseTrend = false }) {
                        trendOptions.forEach { (id, label) -> DropdownMenuItem(
                            text = { Text(label) }, onClick = { trend = id; chooseTrend = false }) }
                    }
                }
                if (trendValues.size >= 2) SparkLine(trendValues, BodyPurple)
                else Text(context.tr(R.string.ui_graf_se_zobrazi_po_dvou_zaznamech_f88db), color = SecondaryText, fontSize = 12.sp)
                Text(context.tr(R.string.ui_v_grafu_jsou_jen_skutecne_vyplnene_224c5), color = SecondaryText, fontSize = 11.sp)
            }
        }
        items(vm.measures, key = { it.id }) { record ->
            SectionCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(shortDate(record.date), color = SecondaryText, fontSize = 12.sp)
                    record.kg?.let { Text("${fmtDisplay(it)} kg", color = BodyPurple, fontWeight = FontWeight.Bold) }
                }
                record.waist?.let { Text(context.tr(R.string.ui_bricho_s_cm_f5bff ,fmtDisplay(it)), fontSize = 13.sp) }
                vm.bodyMetrics.forEach { metric -> record.extra[metric.id]?.let {
                    Text("${metric.name} ${fmtDisplay(it)} cm", fontSize = 13.sp)
                } }
                if (record.waist == null && record.extra.isEmpty()) {
                    Text(context.tr(R.string.ui_pouze_hmotnost_4b845), fontSize = 12.sp, color = SecondaryText)
                }
                TextButton(onClick = { editingId = record.id }) { Text(context.tr(R.string.ui_opravit_zaznam_932c9)) }
            }
        }
        if (vm.measures.isEmpty()) item {
            SectionCard { Text(context.tr(R.string.ui_zatim_zadna_mereni_muzes_pridat_samotnou_98377),
                color = SecondaryText, fontSize = 13.sp) }
        }
        item { Spacer(Modifier.height(15.dp)) }
    }

    if (metricDialog) AlertDialog(
        onDismissRequest = { metricDialog = false },
        title = { Text(if (metricId == null) context.tr(R.string.ui_pridat_vlastni_miru_8951d) else context.tr(R.string.ui_prejmenovat_miru_10fc7)) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            DataInput(context.tr(R.string.ui_nazev_miry_b87bd), metricName, { metricName = it; metricError = null }, Modifier.fillMaxWidth())
            Text(context.tr(R.string.ui_znaku_hodnoty_v_cm_0f4d5), color = SecondaryText, fontSize = 12.sp)
            if (metricId == null) LazyRow {
                items(listOf(context.tr(R.string.ui_biceps_f8203), context.tr(R.string.ui_stehno_9d802), context.tr(R.string.ui_hrudnik_0380f), context.tr(R.string.ui_boky_1ec96), context.tr(R.string.ui_lytko_cdfb8))) { suggestion ->
                    TextButton(onClick = { metricName = suggestion; metricError = null }) { Text(suggestion) }
                }
            }
            metricError?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
        } },
        confirmButton = { TextButton(onClick = {
            if (vm.saveBodyMetric(metricName, metricId)) metricDialog = false else metricError = vm.message
        }) { Text(context.tr(R.string.ui_ulozit_85e82)) } },
        dismissButton = { TextButton(onClick = { metricDialog = false }) { Text(context.tr(R.string.ui_zrusit_9bd5e)) } }
    )
    vm.measures.firstOrNull { it.id == editingId }?.let { record ->
        var editedDate by rememberSaveable(record.id) { mutableStateOf(record.date) }
        var editedKg by rememberSaveable(record.id) { mutableStateOf(record.kg?.let { fmtDisplay(it) } ?: "") }
        var editedWaist by rememberSaveable(record.id) { mutableStateOf(record.waist?.let { fmtDisplay(it) } ?: "") }
        var editedExtra by rememberSaveable(record.id, stateSaver = bodyValuesSaver) {
            mutableStateOf(record.extra.mapValues { fmtDisplay(it.value) })
        }
        var editError by rememberSaveable(record.id) { mutableStateOf<String?>(null) }
        AlertDialog(onDismissRequest = { editingId = null }, title = { Text(context.tr(R.string.ui_opravit_mereni_3bd7e)) },
            text = { Column(Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DataInput(context.tr(R.string.ui_datum_df5c3), editedDate, { editedDate = it }, Modifier.fillMaxWidth())
                BodyMeasurementFields(vm.bodyMetrics, editedKg, { editedKg = it }, editedWaist,
                    { editedWaist = it }, editedExtra) { id, value ->
                    editedExtra = editedExtra + (id to value)
                }
                editError?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
            } },
            confirmButton = { TextButton(onClick = {
                val values = parseBodyMeasurement(editedKg, editedWaist, editedExtra)
                if (!validDate(editedDate) || values == null) editError = context.tr(measurementError)
                else if (vm.updateMeasure(record.copy(date = editedDate, kg = values.kg, waist = values.waist, extra = values.extra))) {
                    editingId = null
                } else editError = vm.message
            }) { Text(context.tr(R.string.ui_ulozit_85e82)) } },
            dismissButton = { TextButton(onClick = { editingId = null }) { Text(context.tr(R.string.ui_zrusit_9bd5e)) } }
        )
    }
}

@Composable
private fun BodyMeasurementFields(
    metrics: List<BodyMetric>, kg: String, onKg: (String) -> Unit, waist: String, onWaist: (String) -> Unit,
    extra: Map<String, String>, onExtra: (String, String) -> Unit
) {
    val context=androidx.compose.ui.platform.LocalContext.current

    DataInput(context.tr(R.string.ui_hmotnost_kg_volitelne_9b066), kg, onKg, Modifier.fillMaxWidth(), KeyboardType.Decimal)
    DataInput(context.tr(R.string.ui_bricho_cm_volitelne_955b1), waist, onWaist, Modifier.fillMaxWidth(), KeyboardType.Decimal)
    metrics.forEach { metric ->
        DataInput("${metric.name} (cm)", extra[metric.id] ?: "", { onExtra(metric.id, it) },
            Modifier.fillMaxWidth(), KeyboardType.Decimal)
    }
    Text(context.tr(R.string.ui_nevyplnena_pole_se_neukladaji_staci_samotna_bae77), color = SecondaryText, fontSize = 11.sp)
}
