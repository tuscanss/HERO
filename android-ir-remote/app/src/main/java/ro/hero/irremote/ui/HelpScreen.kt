package ro.hero.irremote.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun HelpScreen(vm: AppViewModel, onBack: () -> Unit) {
    AppScaffold(vm, "Ajutor", onBack = onBack) { pad ->
        Column(Modifier.padding(pad).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val has = vm.transmitter.hasEmitter
            Section(if (has) Icons.Filled.CheckCircle else Icons.Filled.Error, "Telefonul tău") {
                Text(if (has) "✅ Are emițător IR. Frecvențe suportate: " +
                    vm.transmitter.frequencyRanges().joinToString { "${it.first / 1000}–${it.last / 1000} kHz" }
                else "❌ Nu are emițător IR. Telefoane cu IR: majoritatea Xiaomi / Redmi / POCO, unele Huawei/Honor, Realme, Vivo, Oppo. " +
                    "Alternativ: un emițător IR care se conectează în mufa USB‑C.")
            }
            Section(Icons.Filled.Lightbulb, "Plafoniera Novecento: IR sau radio?") {
                Text("Înainte de toate verifică dacă telecomanda e în infraroșu:")
                Text("1. Deschide camera telefonului (de preferat camera frontală).\n" +
                    "2. Îndreaptă LED-ul din vârful telecomenzii spre cameră și apasă un buton.\n" +
                    "3. Dacă vezi pe ecran o lumină mov/albă care clipește → e IR și o poți clona cu aplicația.\n" +
                    "4. Dacă nu vezi nimic (sau telecomanda funcționează și fără să o îndrepți spre lampă, prin obiecte) → " +
                    "este radio (2.4 GHz / 433 MHz). Telefoanele nu pot emite aceste semnale; ai nevoie de un modul radio separat.")
                Text("Multe plafoniere LED moderne cu „telecomandă” folosesc radio 2.4 GHz, deci testul de mai sus contează.",
                    style = MaterialTheme.typography.bodySmall)
            }
            Section(Icons.Filled.SettingsRemote, "Cum clonez o telecomandă IR") {
                Text("Telefoanele au doar emițător IR, nu și receptor. Ai trei variante:")
                Text("• Receptor USB (cel mai sigur): Arduino Nano sau ESP32 + senzor TSOP38238 (VS1838B), conectat cu adaptor OTG. " +
                    "Firmware-ul e în proiect: hardware/ir_learner/ir_learner.ino. Apeși butonul pe telecomanda originală, aplicația îl copiază.")
                Text("• Căutătorul de coduri: aplicația încearcă pe rând comenzile NEC până reacționează lampa. Nu necesită hardware.")
                Text("• Cod manual: dacă găsești codul online (Pronto, NEC adresă/comandă, fișier Flipper .ir).")
            }
            Section(Icons.Filled.FolderOpen, "Biblioteca") {
                Text("Toate telecomenzile se salvează local pe telefon. Poți face backup (.json), poți importa fișiere .ir " +
                    "(format Flipper Zero – există colecții publice cu mii de telecomenzi) și exporta telecomenzile tale în același format.")
            }
            Section(Icons.Filled.TipsAndUpdates, "Sfaturi") {
                Text("• Ține telefonul îndreptat spre receptorul lămpii (de obicei în centrul plafonierei), la 1–4 m.\n" +
                    "• Emițătorul IR al telefonului e de obicei pe muchia de sus.\n" +
                    "• Dacă un buton merge doar uneori, re-clonează-l sau încearcă din nou mai aproape.")
            }
        }
    }
}

@Composable
private fun Section(icon: ImageVector, title: String, content: @Composable ColumnScope.() -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleMedium)
            }
            content()
        }
    }
}
