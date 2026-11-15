package ro.hero.irremote.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import ro.hero.irremote.data.CodeSet

/**
 * Asistent „Ai pierdut telecomanda?”: trimite pe rând comanda de stingere din fiecare
 * telecomandă de iluminat preinstalată, până când lampa reacționează.
 * Progresul și istoricul variantelor trimise se păstrează în ViewModel/preferințe,
 * iar modul „Aprinde-o înapoi” încearcă comanda de pornire a ultimelor variante trimise.
 */
@Composable
fun LampWizardScreen(vm: AppViewModel, onBack: () -> Unit, onDone: (String) -> Unit) {
    val sets by vm.codeSets.collectAsState()
    val index by vm.wizardIndex.collectAsState()
    val history by vm.wizardHistory.collectAsState()
    var started by rememberSaveable { mutableStateOf(history.isNotEmpty()) }
    var auto by remember { mutableStateOf(false) }
    var foundIndex by rememberSaveable { mutableIntStateOf(-1) }
    /** Poziția în istoric când căutăm codul care a stins lampa (-1 = mod normal). */
    var recoverPos by rememberSaveable { mutableIntStateOf(-1) }

    val list = sets
    AppScaffold(vm, "Găsește codurile lămpii", onBack = onBack) { pad ->
        if (list == null) {
            Box(Modifier.padding(pad).fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@AppScaffold
        }

        LaunchedEffect(auto, index) {
            if (auto) {
                vm.wizardProbe()
                delay(4000)
                if (index < list.lastIndex) vm.setWizardIndex(index + 1) else auto = false
            }
        }
        DisposableEffect(Unit) { onDispose { auto = false } }

        fun sendOn(i: Int) { list.getOrNull(i)?.let { s -> (s.onButton ?: s.probeButton)?.let { vm.send(it.code, repeats = 2) } } }

        Column(Modifier.padding(pad).padding(16.dp).fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {

            // ---------- Am găsit setul ----------
            list.getOrNull(foundIndex)?.let { set ->
                Text("Super! Lampa răspunde la codurile:", style = MaterialTheme.typography.titleMedium)
                Text(set.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Testează butoanele (de ex. „On”/„Power” ca s-o aprinzi). Dacă merg, salvează-le.",
                    style = MaterialTheme.typography.bodyMedium)
                CodeButtonsGrid(vm, set)
                Button(onClick = { onDone(vm.applyToNovecento(set)) }, Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Check, null); Spacer(Modifier.width(8.dp)); Text("Salvează ca „Plafonieră Novecento”")
                }
                OutlinedButton(onClick = { foundIndex = -1 }, Modifier.fillMaxWidth()) { Text("Nu merg bine – continuă căutarea") }
                return@Column
            }

            // ---------- Aprinde-o înapoi: încercăm comanda de pornire a ultimelor variante ----------
            if (recoverPos >= 0) {
                val histIdx = history.getOrNull(recoverPos)
                val set = histIdx?.let { list.getOrNull(it) }
                Text("Aprindem lampa înapoi", style = MaterialTheme.typography.titleLarge)
                Text("Încerc comanda de PORNIRE din variantele trimise recent (cea mai recentă prima). " +
                    "Când se aprinde lampa, apasă „S-a aprins!” – aceea e telecomanda ta.")
                if (set == null) {
                    Text("Am încercat toate variantele recente. Aprinde lampa de la întrerupător (stinge, 5 secunde, aprinde) " +
                        "și reia căutarea mai încet, fără modul automat.", color = MaterialTheme.colorScheme.error)
                    OutlinedButton(onClick = { recoverPos = -1 }) { Text("Înapoi la căutare") }
                    return@Column
                }
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Recentă ${recoverPos + 1} din ${history.size}", style = MaterialTheme.typography.labelMedium)
                        Text(set.title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                        Text("Am trimis: „${(set.onButton ?: set.probeButton)?.name ?: "-"}”", style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(onClick = { foundIndex = histIdx; recoverPos = -1; vm.setWizardIndex(histIdx) }) { Text("S-a aprins!") }
                            OutlinedButton(onClick = { recoverPos++; history.getOrNull(recoverPos)?.let(::sendOn) }) { Text("Nu, următoarea") }
                        }
                        TextButton(onClick = { sendOn(histIdx) }) { Icon(Icons.Filled.Replay, null); Spacer(Modifier.width(4.dp)); Text("Trimite din nou") }
                    }
                }
                OutlinedButton(onClick = { recoverPos = -1 }) { Text("Anulează") }
                return@Column
            }

            // ---------- Ecran de start ----------
            if (!started) {
                Text("Nu ai telecomanda? Nicio problemă.", style = MaterialTheme.typography.titleLarge)
                Text("Aplicația are preinstalate ${list.size} telecomenzi IR pentru corpuri de iluminat " +
                    "(${list.count { it.likelyCeiling }} pentru plafoniere / lumină albă reglabilă). Le încercăm pe rând pe lampa ta.")
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Pregătire", fontWeight = FontWeight.Bold)
                        Text("1. Aprinde plafoniera de la întrerupătorul de perete și las-o aprinsă.")
                        Text("2. Stai sub lampă și îndreaptă muchia de sus a telefonului spre centrul ei.")
                        Text("3. La fiecare pas aplicația trimite comanda „Stinge”. Când lampa se stinge (sau clipește), apasă „DA”.")
                        Text("Dacă lampa se stinge și n-ai apucat să apeși: nicio grijă, butonul „S-a stins lampa?” găsește varianta și o reaprinde. " +
                            "Sau aprinde-o de la întrerupător (stinge, 5 secunde, aprinde).", style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (!vm.transmitter.hasEmitter) {
                    Text("⚠️ Telefonul acesta nu are emițător IR, deci nu poate trimite comenzi.", color = MaterialTheme.colorScheme.error)
                }
                Button(onClick = { started = true; vm.wizardProbe() }, Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.PlayArrow, null); Spacer(Modifier.width(8.dp))
                    Text(if (index > 0) "Continuă de la varianta ${index + 1}" else "Începe")
                }
                if (index > 0) TextButton(onClick = { vm.resetWizard(); started = true; vm.wizardProbe() }) { Text("Ia-o de la capăt") }
                return@Column
            }

            val set: CodeSet? = list.getOrNull(index)
            if (set == null) {
                Text("Am încercat toate cele ${list.size} variante.", style = MaterialTheme.typography.titleMedium)
                Text("Lampa nu a reacționat la niciuna. Cel mai probabil plafoniera folosește o telecomandă radio (2.4 GHz). " +
                    "Poți încerca și „Căutătorul de coduri” (scanare NEC completă) sau o telecomandă de schimb Novecento.")
                OutlinedButton(onClick = { vm.resetWizard() }) { Text("Ia-o de la capăt") }
                return@Column
            }

            // Buton mereu vizibil: lampa s-a stins dar nu știu la care variantă
            if (history.isNotEmpty()) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                    modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("S-a stins lampa și n-ai apucat să apeși DA?", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        Button(onClick = { auto = false; recoverPos = 0; sendOn(history.first()) }) { Text("Aprinde-o înapoi") }
                    }
                }
            }

            LinearProgressIndicator(progress = { (index + 1f) / list.size }, Modifier.fillMaxWidth())
            Text("Varianta ${index + 1} din ${list.size} • progresul se salvează", style = MaterialTheme.typography.labelMedium)
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Lightbulb, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    Text(set.title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                    Text("Am trimis: „${set.probeButton?.name ?: "-"}”", style = MaterialTheme.typography.bodySmall)
                    if (set.likelyCeiling) AssistChip(onClick = {}, label = { Text("plafonieră / alb reglabil") })
                    Spacer(Modifier.height(12.dp))
                    Text("A reacționat lampa?", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = { auto = false; foundIndex = index }) { Text("DA") }
                        OutlinedButton(onClick = { auto = false; vm.setWizardIndex(index + 1); vm.wizardProbe() }) { Text("NU, următoarea") }
                    }
                    TextButton(onClick = { vm.wizardProbe() }) { Icon(Icons.Filled.Replay, null); Spacer(Modifier.width(4.dp)); Text("Trimite din nou") }
                }
            }
            if (auto) {
                Button(onClick = { auto = false }, Modifier.fillMaxWidth().height(64.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                    Icon(Icons.Filled.Stop, null); Spacer(Modifier.width(8.dp)); Text("STOP – lampa a reacționat!")
                }
            } else {
                OutlinedButton(onClick = { auto = true }, Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.PlayArrow, null); Spacer(Modifier.width(8.dp)); Text("Automat (o variantă la 4 secunde)")
                }
            }
            if (index > 0) OutlinedButton(onClick = { auto = false; vm.setWizardIndex(index - 1); vm.wizardProbe() }) {
                Icon(Icons.Filled.SkipPrevious, null); Text("Înapoi o variantă")
            }
        }
    }
}

/** Grilă cu toate butoanele unui set; atingerea trimite codul. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CodeButtonsGrid(vm: AppViewModel, set: CodeSet) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        set.codes.forEach { c ->
            FilledTonalButton(onClick = { vm.send(c.code) }) {
                iconFor(CodeSet.guessIcon(c.name))?.let { Icon(it, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)) }
                Text(c.name.replace('_', ' '))
            }
        }
    }
}
