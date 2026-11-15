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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import ro.hero.irremote.ir.IrCode
import ro.hero.irremote.ir.IrFormats
import ro.hero.irremote.ir.IrProtocols

@Composable
fun LearnScreen(vm: AppViewModel, remoteId: String, buttonId: String, onBack: () -> Unit, openFinder: () -> Unit) {
    val remotes by vm.repo.remotes.collectAsState()
    val remote = remotes.firstOrNull { it.id == remoteId }
    val button = remote?.buttons?.firstOrNull { it.id == buttonId }
    if (remote == null || button == null) { LaunchedEffect(Unit) { onBack() }; return }

    var tab by rememberSaveable { mutableIntStateOf(0) }
    var candidate by remember { mutableStateOf<IrCode?>(null) }

    fun save(code: IrCode) {
        vm.repo.updateButton(remoteId, button.copy(code = code))
        vm.toast("Cod salvat pe „${button.label}”")
        onBack()
    }

    AppScaffold(vm, "Clonează: ${button.label}", onBack = onBack) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            TabRow(selectedTabIndex = tab) {
                Tab(tab == 0, { tab = 0 }, text = { Text("Receptor USB") }, icon = { Icon(Icons.Filled.Usb, null) })
                Tab(tab == 1, { tab = 1 }, text = { Text("Cod manual") }, icon = { Icon(Icons.Filled.Keyboard, null) })
                Tab(tab == 2, { tab = 2 }, text = { Text("Căutare") }, icon = { Icon(Icons.Filled.ManageSearch, null) })
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp)) {
                when (tab) {
                    0 -> UsbLearnTab(vm) { candidate = it }
                    1 -> ManualTab { candidate = it }
                    2 -> FinderIntro(openFinder)
                }
            }
            candidate?.let { c ->
                Surface(tonalElevation = 3.dp) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Text("Cod pregătit: ${c.description}", style = MaterialTheme.typography.titleSmall)
                        Text("${c.frequency} Hz • ${c.pattern.size} durate • ${c.totalMicros / 1000} ms",
                            style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { vm.send(c) }) { Icon(Icons.Filled.Send, null); Spacer(Modifier.width(4.dp)); Text("Testează") }
                            Button(onClick = { save(c) }) { Icon(Icons.Filled.Check, null); Spacer(Modifier.width(4.dp)); Text("Salvează") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UsbLearnTab(vm: AppViewModel, onCandidate: (IrCode) -> Unit) {
    val st by vm.learner.state.collectAsState()
    LaunchedEffect(st.lastCapture, st.captureCount) { st.lastCapture?.let(onCandidate) }
    DisposableEffect(Unit) { onDispose { vm.learner.disconnect() } }

    Text("Telefonul poate doar EMITE infraroșu, nu poate recepționa. Pentru clonare conectează prin cablu OTG " +
        "un receptor IR (Arduino Nano/ESP32 + senzor TSOP38238, ~25 lei) cu firmware-ul din folderul „hardware/ir_learner”.",
        style = MaterialTheme.typography.bodyMedium)
    Spacer(Modifier.height(16.dp))
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(if (st.connected) Icons.Filled.UsbOff else Icons.Filled.Usb, null)
                Spacer(Modifier.width(8.dp))
                Text(st.status, Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            if (st.connected) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Text("Ascult… Îndreaptă telecomanda originală spre receptor (5–10 cm) și apasă scurt butonul.",
                    style = MaterialTheme.typography.bodySmall)
                if (st.captureCount > 0) Text("Capturi primite: ${st.captureCount}", style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { vm.learner.disconnect() }) { Text("Deconectează") }
            } else {
                Button(onClick = { vm.learner.connect() }) { Text("Conectează receptorul") }
            }
        }
    }
    Spacer(Modifier.height(12.dp))
    Text("Sfat: dacă la „Testează” lampa nu reacționează, capturează din nou butonul – uneori prima captură e incompletă.",
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun ManualTab(onCandidate: (IrCode) -> Unit) {
    val formats = IrProtocols.SUPPORTED + listOf("Pronto HEX", "RAW (durate µs)")
    var format by rememberSaveable { mutableStateOf("NEC") }
    var address by rememberSaveable { mutableStateOf("00") }
    var command by rememberSaveable { mutableStateOf("00") }
    var text by rememberSaveable { mutableStateOf("") }
    var freq by rememberSaveable { mutableStateOf("38000") }
    var error by remember { mutableStateOf<String?>(null) }

    Text("Introdu un cod găsit online (ex. baze de date IR, fișiere Flipper, LIRC) sau notat de pe alt dispozitiv.",
        style = MaterialTheme.typography.bodyMedium)
    Spacer(Modifier.height(12.dp))
    SimpleDropdown("Format", format, formats) { format = it }
    Spacer(Modifier.height(8.dp))
    val isProto = format in IrProtocols.SUPPORTED
    if (isProto) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(address, { address = it }, Modifier.weight(1f), label = { Text("Adresă (hex)") }, singleLine = true)
            OutlinedTextField(command, { command = it }, Modifier.weight(1f), label = { Text("Comandă (hex)") }, singleLine = true)
        }
    } else {
        OutlinedTextField(text, { text = it }, Modifier.fillMaxWidth(), minLines = 4,
            label = { Text(if (format.startsWith("Pronto")) "0000 006D 0022 0002 0156 00AB …" else "9000 4500 560 560 560 1690 …") },
            textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace))
        if (format.startsWith("RAW")) OutlinedTextField(freq, { freq = it }, label = { Text("Frecvență (Hz)") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
    }
    error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
    Spacer(Modifier.height(8.dp))
    Button(onClick = {
        error = null
        try {
            val code = when {
                isProto -> IrProtocols.encode(format, address.trim().removePrefix("0x").toInt(16), command.trim().removePrefix("0x").toInt(16))
                format.startsWith("Pronto") -> IrFormats.parsePronto(text)
                else -> IrFormats.parseRaw(text, freq.toIntOrNull() ?: 38_000)
            }
            onCandidate(code)
        } catch (e: Exception) {
            error = "Cod invalid: ${e.message}"
        }
    }) { Text("Generează codul") }
}

@Composable
private fun FinderIntro(openFinder: () -> Unit) {
    Text("Fără receptor? Multe plafoniere LED cu telecomandă IR folosesc protocolul NEC. Căutătorul trimite pe rând " +
        "toate cele 256 de comenzi pentru o adresă, iar tu oprești când lampa reacționează.", style = MaterialTheme.typography.bodyMedium)
    Spacer(Modifier.height(8.dp))
    Text("Durează câteva minute; ține telefonul îndreptat spre lampă, de la 1–3 m.", style = MaterialTheme.typography.bodySmall)
    Spacer(Modifier.height(16.dp))
    Button(onClick = openFinder) { Icon(Icons.Filled.ManageSearch, null); Spacer(Modifier.width(8.dp)); Text("Deschide căutătorul de coduri") }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleDropdown(label: String, value: String, options: List<String>, onChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(value, {}, readOnly = true, label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth())
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { o -> DropdownMenuItem(text = { Text(o) }, onClick = { onChange(o); expanded = false }) }
        }
    }
}
