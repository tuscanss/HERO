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

/**
 * Asistent „Nu mai am telecomanda”: trimite pe rând comanda de stingere din fiecare
 * telecomandă de iluminat din baza preinstalată, până când lampa reacționează.
 * Seturile pentru plafoniere / lumină albă reglabilă sunt încercate primele.
 */
@Composable
fun LampWizardScreen(vm: AppViewModel, onBack: () -> Unit, onDone: (String) -> Unit) {
    val sets by vm.codeSets.collectAsState()
    var index by rememberSaveable { mutableIntStateOf(0) }
    var started by rememberSaveable { mutableStateOf(false) }
    var auto by remember { mutableStateOf(false) }
    var found by rememberSaveable { mutableStateOf(false) }

    val list = sets
    AppScaffold(vm, "Găsește codurile lămpii", onBack = onBack) { pad ->
        if (list == null) {
            Box(Modifier.padding(pad).fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@AppScaffold
        }
        val set = list.getOrNull(index)
        fun sendProbe() { set?.probeButton?.let { vm.send(it.code, repeats = 2) } }

        LaunchedEffect(auto, index) {
            if (auto) {
                sendProbe()
                delay(2500)
                if (index < list.lastIndex) index++ else auto = false
            }
        }

        Column(Modifier.padding(pad).padding(16.dp).fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (!started) {
                Text("Nu ai telecomanda? Nicio problemă.", style = MaterialTheme.typography.titleLarge)
                Text("Aplicația are preinstalate ${list.size} telecomenzi IR pentru corpuri de iluminat " +
                    "(${list.count { it.likelyCeiling }} dintre ele pentru plafoniere / lumină albă reglabilă). " +
                    "Le încercăm pe rând pe lampa ta.")
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Pregătire", fontWeight = FontWeight.Bold)
                        Text("1. Aprinde plafoniera de la întrerupătorul de perete și las-o aprinsă.")
                        Text("2. Stai sub lampă și îndreaptă muchia de sus a telefonului spre centrul ei.")
                        Text("3. La fiecare pas aplicația trimite comanda „Stinge”. Când lampa se stinge (sau clipește), apasă „DA”.")
                    }
                }
                if (!vm.transmitter.hasEmitter) {
                    Text("⚠️ Telefonul acesta nu are emițător IR, deci nu poate trimite comenzi.",
                        color = MaterialTheme.colorScheme.error)
                }
                Button(onClick = { started = true; sendProbe() }, Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.PlayArrow, null); Spacer(Modifier.width(8.dp)); Text("Începe")
                }
                Text("Dacă nicio variantă nu funcționează, plafoniera folosește probabil o telecomandă radio (2.4 GHz), " +
                    "nu infraroșu – și atunci nu poate fi controlată de un telefon. Soluția ar fi o telecomandă de schimb de la producător.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                return@Column
            }

            if (set == null) {
                Text("Am încercat toate cele ${list.size} variante.", style = MaterialTheme.typography.titleMedium)
                Text("Lampa nu a reacționat la niciuna. Cel mai probabil plafoniera folosește o telecomandă radio (2.4 GHz). " +
                    "Poți încerca și „Căutătorul de coduri” (scanare NEC completă) sau o telecomandă de schimb Novecento.")
                OutlinedButton(onClick = { index = 0 }) { Text("Ia-o de la capăt") }
                return@Column
            }

            if (found) {
                Text("Super! Lampa răspunde la codurile:", style = MaterialTheme.typography.titleMedium)
                Text(set.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Testează butoanele de mai jos. Dacă merg, salvează-le ca telecomanda plafonierei.",
                    style = MaterialTheme.typography.bodyMedium)
                CodeButtonsGrid(vm, set)
                Button(onClick = { onDone(vm.applyToNovecento(set)) }, Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Check, null); Spacer(Modifier.width(8.dp)); Text("Salvează ca „Plafonieră Novecento”")
                }
                OutlinedButton(onClick = { found = false; index++; sendProbe() }, Modifier.fillMaxWidth()) {
                    Text("Nu merg bine – continuă căutarea")
                }
                return@Column
            }

            LinearProgressIndicator(progress = { (index + 1f) / list.size }, Modifier.fillMaxWidth())
            Text("Varianta ${index + 1} din ${list.size}", style = MaterialTheme.typography.labelMedium)
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
                        Button(onClick = { auto = false; found = true }) { Text("DA") }
                        OutlinedButton(onClick = { auto = false; index++; list.getOrNull(index)?.probeButton?.let { vm.send(it.code, 2) } }) {
                            Text("NU, următoarea")
                        }
                    }
                    TextButton(onClick = { sendProbe() }) { Icon(Icons.Filled.Replay, null); Spacer(Modifier.width(4.dp)); Text("Trimite din nou") }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = auto, onCheckedChange = { auto = it })
                Spacer(Modifier.width(8.dp))
                Text("Automat (câte o variantă la 2,5 s). Oprește imediat ce lampa reacționează, apoi verifică cu „Înapoi”.",
                    style = MaterialTheme.typography.bodySmall)
            }
            if (index > 0) OutlinedButton(onClick = { auto = false; index--; list.getOrNull(index)?.probeButton?.let { vm.send(it.code, 2) } }) {
                Icon(Icons.Filled.SkipPrevious, null); Text("Înapoi o variantă")
            }
        }
    }
}

/** Grilă cu toate butoanele unui set; atingerea trimite codul. */
@Composable
fun CodeButtonsGrid(vm: AppViewModel, set: ro.hero.irremote.data.CodeSet) {
    @OptIn(ExperimentalLayoutApi::class)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        set.codes.forEach { c ->
            FilledTonalButton(onClick = { vm.send(c.code) }) {
                iconFor(ro.hero.irremote.data.CodeSet.guessIcon(c.name))?.let { Icon(it, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)) }
                Text(c.name.replace('_', ' '))
            }
        }
    }
}
