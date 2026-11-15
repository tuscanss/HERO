package ro.hero.irremote.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Biblioteca de coduri preinstalate (Flipper-IRDB, iluminat). */
@Composable
fun CodeDatabaseScreen(vm: AppViewModel, onBack: () -> Unit, openSet: (Int) -> Unit, openWizard: () -> Unit) {
    val sets by vm.codeSets.collectAsState()
    var query by remember { mutableStateOf("") }
    var onlyCeiling by remember { mutableStateOf(false) }
    AppScaffold(vm, "Coduri preinstalate", onBack = onBack) { pad ->
        val list = sets
        if (list == null) {
            Box(Modifier.padding(pad).fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@AppScaffold
        }
        Column(Modifier.padding(pad).fillMaxSize()) {
            Card(Modifier.padding(16.dp).fillMaxWidth().clickable(onClick = openWizard),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.AutoFixHigh, null)
                    Spacer(Modifier.width(12.dp))
                    Text("Nu știi care e? Asistentul le încearcă automat pe lampa ta.", Modifier.weight(1f))
                }
            }
            OutlinedTextField(query, { query = it }, Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                placeholder = { Text("Caută marcă sau model…") }, leadingIcon = { Icon(Icons.Filled.Search, null) }, singleLine = true)
            Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                FilterChip(selected = onlyCeiling, onClick = { onlyCeiling = !onlyCeiling },
                    label = { Text("Doar plafoniere / alb reglabil") })
            }
            val filtered = list.withIndex().filter { (_, s) ->
                (!onlyCeiling || s.likelyCeiling) && (query.isBlank() || s.title.contains(query, true))
            }
            LazyColumn(contentPadding = PaddingValues(16.dp, 0.dp, 16.dp, 24.dp)) {
                itemsIndexed(filtered, key = { _, it -> it.value.path }) { _, (i, s) ->
                    ListItem(
                        headlineContent = { Text(s.title) },
                        supportingContent = { Text("${s.codes.size} butoane" + if (s.likelyCeiling) " • plafonieră / alb reglabil" else "") },
                        leadingContent = { Icon(Icons.Filled.Lightbulb, null) },
                        modifier = Modifier.clickable { openSet(i) },
                    )
                    HorizontalDivider()
                }
            }
            Text("Sursa: Flipper-IRDB (domeniu public, CC0).", Modifier.padding(16.dp),
                style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun CodeSetScreen(vm: AppViewModel, index: Int, onBack: () -> Unit, openRemote: (String) -> Unit) {
    val sets by vm.codeSets.collectAsState()
    val set = sets?.getOrNull(index)
    if (set == null) { LaunchedEffect(Unit) { onBack() }; return }
    AppScaffold(vm, set.title, onBack = onBack) { pad ->
        Column(Modifier.padding(pad).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Atinge un buton ca să-l testezi pe lampă.", style = MaterialTheme.typography.bodyMedium)
            CodeButtonsGrid(vm, set)
            Button(onClick = { openRemote(vm.applyToNovecento(set)) }, Modifier.fillMaxWidth()) {
                Text("Folosește pentru Plafoniera Novecento")
            }
            OutlinedButton(onClick = { openRemote(vm.addCodeSet(set)) }, Modifier.fillMaxWidth()) {
                Text("Adaugă ca telecomandă separată")
            }
        }
    }
}
