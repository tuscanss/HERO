package ro.hero.irremote.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ro.hero.irremote.data.RemoteButton

/**
 * Căutător de coduri: trimite pe rând comenzile unui protocol (implicit NEC)
 * pentru a găsi codurile unei lămpi fără a avea receptor IR.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CodeFinderScreen(vm: AppViewModel, remoteId: String?, buttonId: String?, onBack: () -> Unit) {
    val s by vm.scan.collectAsState()
    val remotes by vm.repo.remotes.collectAsState()
    val target = remotes.firstOrNull { it.id == remoteId }?.buttons?.firstOrNull { it.id == buttonId }
    var addrText by remember(s.protocol) { mutableStateOf(s.address.toString(16).uppercase().padStart(if (s.protocol == "NECext") 4 else 2, '0')) }
    var pickTarget by remember { mutableStateOf(false) }
    DisposableEffect(Unit) { onDispose { vm.stopAutoScan() } }

    AppScaffold(vm, "Căutător de coduri", onBack = onBack) { pad ->
        Column(Modifier.padding(pad).padding(16.dp).verticalScroll(rememberScrollState())) {
            Text(target?.let { "Caut codul pentru „${it.label}”." } ?: "Găsește codurile unei telecomenzi pierdute sau necunoscute.",
                style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            SimpleDropdown("Protocol", s.protocol, listOf("NEC", "NECext", "Samsung32")) {
                vm.stopAutoScan(); vm.updateScan(s.copy(protocol = it, address = 0, command = 0))
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(addrText, {
                addrText = it
                it.trim().removePrefix("0x").toIntOrNull(16)?.let { a -> vm.updateScan(s.copy(address = a)) }
            }, label = { Text("Adresă dispozitiv (hex)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Text("Adrese întâlnite des la lămpi LED generice — încearcă-le pe rând:", style = MaterialTheme.typography.labelSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val common = if (s.protocol == "NECext") listOf(0x0000, 0xEF00, 0xFF00, 0x7F80, 0x0D80) else listOf(0x00, 0x01, 0x80, 0xEF, 0xFF, 0x04, 0x86)
                common.forEach { a ->
                    val txt = a.toString(16).uppercase().padStart(if (s.protocol == "NECext") 4 else 2, '0')
                    FilterChip(selected = s.address == a, onClick = { addrText = txt; vm.updateScan(s.copy(address = a, command = 0)) },
                        label = { Text("0x$txt") })
                }
            }
            Spacer(Modifier.height(16.dp))

            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Comanda curentă", style = MaterialTheme.typography.labelMedium)
                    Text("0x" + s.command.toString(16).uppercase().padStart(if (s.protocol == "NECext") 4 else 2, '0'),
                        style = MaterialTheme.typography.displayMedium, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    Text("${s.command + 1} / ${s.maxCommand + 1}", style = MaterialTheme.typography.labelSmall)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        FilledTonalIconButton(onClick = { vm.stopAutoScan(); vm.scanStep(-1) }) { Icon(Icons.Filled.SkipPrevious, "Anterior") }
                        Button(onClick = { vm.send(s.code) }) { Icon(Icons.Filled.Send, null); Spacer(Modifier.width(4.dp)); Text("Trimite") }
                        FilledTonalIconButton(onClick = { vm.stopAutoScan(); vm.scanStep(+1) }) { Icon(Icons.Filled.SkipNext, "Următor") }
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { vm.toggleAutoScan() },
                        colors = if (s.running) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error) else ButtonDefaults.buttonColors(),
                    ) {
                        Icon(if (s.running) Icons.Filled.Stop else Icons.Filled.PlayArrow, null)
                        Spacer(Modifier.width(4.dp))
                        Text(if (s.running) "STOP – lampa a reacționat!" else "Scanare automată")
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Interval: ${s.intervalMs} ms", style = MaterialTheme.typography.labelSmall)
                    Slider(value = s.intervalMs.toFloat(), onValueChange = { vm.updateScan(s.copy(intervalMs = it.toLong())) },
                        valueRange = 400f..3000f)
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("Când lampa reacționează: apasă STOP, apoi folosește ◀ / Trimite ca să confirmi exact comanda (scanarea poate fi trecut deja la următoarea).",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            Button(onClick = {
                if (target != null && remoteId != null) {
                    vm.repo.updateButton(remoteId, target.copy(code = s.code))
                    vm.toast("Cod ${s.code.description} salvat pe „${target.label}”")
                    onBack()
                } else pickTarget = true
            }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Check, null); Spacer(Modifier.width(4.dp))
                Text(target?.let { "Salvează pe „${it.label}”" } ?: "Salvează pe un buton…")
            }
        }
    }

    if (pickTarget) AlertDialog(
        onDismissRequest = { pickTarget = false },
        title = { Text("Alege butonul") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                remotes.forEach { r ->
                    Text(r.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
                    r.buttons.forEach { b ->
                        Text("• ${b.label}" + if (b.learned) " (are cod)" else "",
                            Modifier.fillMaxWidth().clickable {
                                vm.repo.updateButton(r.id, b.copy(code = s.code))
                                vm.toast("Salvat pe „${b.label}”")
                                pickTarget = false
                            }.padding(vertical = 6.dp))
                    }
                    Text("+ Buton nou în „${r.name}”", color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth().clickable {
                            vm.repo.updateButton(r.id, RemoteButton(label = "Cod ${s.code.description}", code = s.code))
                            vm.toast("Buton nou adăugat")
                            pickTarget = false
                        }.padding(vertical = 6.dp))
                }
            }
        },
        confirmButton = { TextButton(onClick = { pickTarget = false }) { Text("Închide") } },
    )
}
