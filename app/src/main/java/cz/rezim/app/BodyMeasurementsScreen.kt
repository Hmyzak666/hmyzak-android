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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val measurementError = "Zkontroluj datum a vyplň alespoň jednu hodnotu. Hmotnost musí být větší než 0 a nejvýše 400 kg, obvody nejvýše 300 cm."

@Composable
fun BodyScreen(vm: RezimState) {
    var form by rememberSaveable { mutableStateOf(false) }
    var date by rememberSaveable { mutableStateOf(currentDate()) }
    var kg by rememberSaveable { mutableStateOf("") }
    var waist by rememberSaveable { mutableStateOf("") }
    var extra by rememberSaveable { mutableStateOf(hashMapOf<String, String>()) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var editingId by rememberSaveable { mutableStateOf<Long?>(null) }
    var metricDialog by rememberSaveable { mutableStateOf(false) }
    var metricId by rememberSaveable { mutableStateOf<String?>(null) }
    var metricName by rememberSaveable { mutableStateOf("") }
    var metricError by rememberSaveable { mutableStateOf<String?>(null) }
    var trend by rememberSaveable { mutableStateOf("weight") }
    var chooseTrend by remember { mutableStateOf(false) }
    val trendOptions = listOf("weight" to "Hmotnost · kg", "waist" to "Břicho · cm") +
        vm.bodyMetrics.map { it.id to "${it.name} · cm" }
    val trendLabel = trendOptions.firstOrNull { it.first == trend }?.second ?: trendOptions.first().second
    val trendValues = vm.measures.asReversed().mapNotNull { record ->
        when (trend) { "weight" -> record.kg; "waist" -> record.waist; else -> record.extra[trend] }
    }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Tělesné míry", fontSize = 23.sp, fontWeight = FontWeight.Bold)
                    Text("Zapiš jen to, co dnes měříš", color = SecondaryText, fontSize = 12.sp)
                }
                MainButton(if (form) "Zavřít" else "+ Měření", color = BodyPurple) {
                    if (!form) date = currentDate()
                    error = null; form = !form
                }
            }
        }
        if (form) item {
            SectionCard {
                MutedHeading("Nové měření")
                DataInput("Datum (RRRR-MM-DD)", date, { date = it }, Modifier.fillMaxWidth())
                BodyMeasurementFields(vm.bodyMetrics, kg, { kg = it }, waist, { waist = it }, extra) { id, value ->
                    extra = HashMap(extra).apply { put(id, value) }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
                MainButton("Uložit měření", modifier = Modifier.fillMaxWidth(), color = BodyPurple) {
                    val values = parseBodyMeasurement(kg, waist, extra)
                    if (!validDate(date) || values == null) error = measurementError
                    else if (vm.addMeasure(date, values)) {
                        kg = ""; waist = ""; extra = hashMapOf(); error = null; form = false
                    } else error = vm.message
                }
            }
        }
        item {
            SectionCard {
                MutedHeading("Vlastní míry")
                Text("Přidej si třeba biceps, stehno, hrudník nebo levou a pravou paži zvlášť. Všechny obvody jsou v cm.",
                    color = SecondaryText, fontSize = 12.sp)
                vm.bodyMetrics.forEach { metric ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(metric.name, Modifier.weight(1f), fontSize = 14.sp)
                        TextButton(onClick = {
                            metricId = metric.id; metricName = metric.name; metricError = null; metricDialog = true
                        }) { Text("Přejmenovat", fontSize = 12.sp) }
                    }
                }
                OutlinedButton(onClick = {
                    metricId = null; metricName = ""; metricError = null; metricDialog = true
                }, modifier = Modifier.fillMaxWidth()) { Text("+ Vlastní míra") }
            }
        }
        if (vm.measures.isNotEmpty()) item {
            SectionCard {
                MutedHeading("Vývoj měření")
                Box {
                    OutlinedButton(onClick = { chooseTrend = true }) { Text(trendLabel) }
                    DropdownMenu(expanded = chooseTrend, onDismissRequest = { chooseTrend = false }) {
                        trendOptions.forEach { (id, label) -> DropdownMenuItem(
                            text = { Text(label) }, onClick = { trend = id; chooseTrend = false }) }
                    }
                }
                if (trendValues.size >= 2) SparkLine(trendValues, BodyPurple)
                else Text("Graf se zobrazí po dvou záznamech této míry.", color = SecondaryText, fontSize = 12.sp)
                Text("V grafu jsou jen skutečně vyplněné hodnoty.", color = SecondaryText, fontSize = 11.sp)
            }
        }
        items(vm.measures, key = { it.id }) { record ->
            SectionCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(shortDate(record.date), color = SecondaryText, fontSize = 12.sp)
                    record.kg?.let { Text("${fmtNumber(it)} kg", color = BodyPurple, fontWeight = FontWeight.Bold) }
                }
                record.waist?.let { Text("Břicho ${fmtNumber(it)} cm", fontSize = 13.sp) }
                vm.bodyMetrics.forEach { metric -> record.extra[metric.id]?.let {
                    Text("${metric.name} ${fmtNumber(it)} cm", fontSize = 13.sp)
                } }
                if (record.waist == null && record.extra.isEmpty()) {
                    Text("Pouze hmotnost", fontSize = 12.sp, color = SecondaryText)
                }
                TextButton(onClick = { editingId = record.id }) { Text("Opravit záznam") }
            }
        }
        if (vm.measures.isEmpty()) item {
            SectionCard { Text("Zatím žádná měření. Můžeš přidat samotnou váhu nebo libovolné obvody.",
                color = SecondaryText, fontSize = 13.sp) }
        }
        item { Spacer(Modifier.height(15.dp)) }
    }

    if (metricDialog) AlertDialog(
        onDismissRequest = { metricDialog = false },
        title = { Text(if (metricId == null) "Přidat vlastní míru" else "Přejmenovat míru") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            DataInput("Název míry", metricName, { metricName = it; metricError = null }, Modifier.fillMaxWidth())
            Text("1–40 znaků · hodnoty v cm", color = SecondaryText, fontSize = 12.sp)
            if (metricId == null) LazyRow {
                items(listOf("Biceps", "Stehno", "Hrudník", "Boky", "Lýtko")) { suggestion ->
                    TextButton(onClick = { metricName = suggestion; metricError = null }) { Text(suggestion) }
                }
            }
            metricError?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
        } },
        confirmButton = { TextButton(onClick = {
            if (vm.saveBodyMetric(metricName, metricId)) metricDialog = false else metricError = vm.message
        }) { Text("Uložit") } },
        dismissButton = { TextButton(onClick = { metricDialog = false }) { Text("Zrušit") } }
    )
    vm.measures.firstOrNull { it.id == editingId }?.let { record ->
        var editedDate by rememberSaveable(record.id) { mutableStateOf(record.date) }
        var editedKg by rememberSaveable(record.id) { mutableStateOf(record.kg?.let { fmtNumber(it) } ?: "") }
        var editedWaist by rememberSaveable(record.id) { mutableStateOf(record.waist?.let { fmtNumber(it) } ?: "") }
        var editedExtra by rememberSaveable(record.id) { mutableStateOf(HashMap(record.extra.mapValues { fmtNumber(it.value) })) }
        var editError by rememberSaveable(record.id) { mutableStateOf<String?>(null) }
        AlertDialog(onDismissRequest = { editingId = null }, title = { Text("Opravit měření") },
            text = { Column(Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DataInput("Datum", editedDate, { editedDate = it }, Modifier.fillMaxWidth())
                BodyMeasurementFields(vm.bodyMetrics, editedKg, { editedKg = it }, editedWaist,
                    { editedWaist = it }, editedExtra) { id, value ->
                    editedExtra = HashMap(editedExtra).apply { put(id, value) }
                }
                editError?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
            } },
            confirmButton = { TextButton(onClick = {
                val values = parseBodyMeasurement(editedKg, editedWaist, editedExtra)
                if (!validDate(editedDate) || values == null) editError = measurementError
                else if (vm.updateMeasure(record.copy(date = editedDate, kg = values.kg, waist = values.waist, extra = values.extra))) {
                    editingId = null
                } else editError = vm.message
            }) { Text("Uložit") } },
            dismissButton = { TextButton(onClick = { editingId = null }) { Text("Zrušit") } }
        )
    }
}

@Composable
private fun BodyMeasurementFields(
    metrics: List<BodyMetric>, kg: String, onKg: (String) -> Unit, waist: String, onWaist: (String) -> Unit,
    extra: Map<String, String>, onExtra: (String, String) -> Unit
) {
    DataInput("Hmotnost (kg, volitelné)", kg, onKg, Modifier.fillMaxWidth(), KeyboardType.Decimal)
    DataInput("Břicho (cm, volitelné)", waist, onWaist, Modifier.fillMaxWidth(), KeyboardType.Decimal)
    metrics.forEach { metric ->
        DataInput("${metric.name} (cm)", extra[metric.id] ?: "", { onExtra(metric.id, it) },
            Modifier.fillMaxWidth(), KeyboardType.Decimal)
    }
    Text("Nevyplněná pole se neukládají. Stačí samotná váha nebo jediný obvod.", color = SecondaryText, fontSize = 11.sp)
}
