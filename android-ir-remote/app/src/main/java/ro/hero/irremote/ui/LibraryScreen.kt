package ro.hero.irremote.ui

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ro.hero.irremote.data.CATEGORIES
import ro.hero.irremote.data.Remote
import ro.hero.irremote.data.Templates

@Composable
fun LibraryScreen(vm: AppViewModel, openRemote: (String) -> Unit, openHelp: () -> Unit, openFinder: () -> Unit) {
    val remotes by vm.repo.remotes.collectAsState()
    var query by remember { mutableStateOf("") }
    var showNew by remember { mutableStateOf(false) }
    var exportAll by remember { mutableStateOf(false) }
    val ctx = LocalContext.current

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            val name = ctx.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
            vm.importFile(uri, name)
        }
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) vm.writeText(uri, vm.repo.exportJson(remotes))
    }
    LaunchedEffect(exportAll) { if (exportAll) { exportAll = false; exportLauncher.launch("telecomenzi_backup.json") } }

    var menu by remember { mutableStateOf(false) }
    AppScaffold(
        vm, "Telecomenzile mele",
        actions = {
            IconButton(onClick = openFinder) { Icon(Icons.Filled.ManageSearch, "Căutător de coduri") }
            IconButton(onClick = openHelp) { Icon(Icons.Filled.HelpOutline, "Ajutor") }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, "Mai mult") }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Importă fișier (.ir Flipper / .json)") },
                        leadingIcon = { Icon(Icons.Filled.FileOpen, null) },
                        onClick = { menu = false; importLauncher.launch(arrayOf("*/*")) })
                    DropdownMenuItem(text = { Text("Backup bibliotecă (.json)") },
                        leadingIcon = { Icon(Icons.Filled.Save, null) },
                        onClick = { menu = false; exportAll = true })
                }
            }
        },
        fab = {
            ExtendedFloatingActionButton(onClick = { showNew = true }, icon = { Icon(Icons.Filled.Add, null) }, text = { Text("Telecomandă nouă") })
        },
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            if (!vm.transmitter.hasEmitter) {
                Card(
                    Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth().clickable { openHelp() },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Warning, null)
                        Spacer(Modifier.width(12.dp))
                        Text("Acest telefon nu are emițător IR. Poți organiza biblioteca, dar ai nevoie de un telefon cu IR blaster (ex. Xiaomi/Redmi/POCO) pentru a trimite comenzi. Atinge pentru detalii.",
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            OutlinedTextField(
                value = query, onValueChange = { query = it },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(),
                placeholder = { Text("Caută în bibliotecă…") },
                leadingIcon = { Icon(Icons.Filled.Search, null) }, singleLine = true,
            )
            val filtered = remotes.filter {
                query.isBlank() || it.name.contains(query, true) || it.brand.contains(query, true) || it.category.contains(query, true)
            }
            if (filtered.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Biblioteca e goală. Adaugă o telecomandă cu butonul +", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                val grouped = filtered.groupBy { it.category }.toSortedMap(compareBy { CATEGORIES.indexOf(it).let { i -> if (i < 0) 99 else i } })
                LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp)) {
                    grouped.forEach { (cat, list) ->
                        item(key = "h_$cat") {
                            Text(cat, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
                        }
                        items(list, key = { it.id }) { r -> RemoteCard(r) { openRemote(r.id) } }
                    }
                }
            }
        }
    }

    if (showNew) NewRemoteDialog(onDismiss = { showNew = false }) { remote ->
        showNew = false
        vm.repo.upsert(remote)
        openRemote(remote.id)
    }
}

@Composable
private fun RemoteCard(r: Remote, onClick: () -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(onClick = onClick)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(categoryIcon(r.category), null, Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(r.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (r.brand.isNotBlank()) Text(r.brand, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(6.dp))
                val total = r.buttons.size.coerceAtLeast(1)
                LinearProgressIndicator(progress = { r.learnedCount / total.toFloat() }, modifier = Modifier.fillMaxWidth())
                Text("${r.learnedCount} din ${r.buttons.size} butoane clonate", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun NewRemoteDialog(onDismiss: () -> Unit, onCreate: (Remote) -> Unit) {
    var name by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Iluminat") }
    var template by remember { mutableStateOf<Int?>(0) } // implicit: Novecento
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Telecomandă nouă") },
        text = {
            Column {
                Text("Pornește de la un șablon:", style = MaterialTheme.typography.labelLarge)
                Templates.ALL.forEachIndexed { i, t ->
                    Row(Modifier.fillMaxWidth().clickable { template = i; category = t.category; if (brand.isBlank()) brand = t.brand },
                        verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = template == i, onClick = { template = i; category = t.category; if (brand.isBlank()) brand = t.brand })
                        Text(t.title)
                    }
                }
                Row(Modifier.fillMaxWidth().clickable { template = null }, verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = template == null, onClick = { template = null })
                    Text("Goală (adaug eu butoanele)")
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(name, { name = it }, label = { Text("Nume (ex. Plafonieră dormitor)") }, singleLine = true)
                OutlinedTextField(brand, { brand = it }, label = { Text("Marcă") }, singleLine = true)
                CategoryPicker(category) { category = it }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val t = template?.let { Templates.ALL[it] }
                val base = t?.instantiate() ?: Remote(name = "Telecomandă")
                onCreate(base.copy(
                    name = name.ifBlank { base.name },
                    brand = brand.ifBlank { base.brand },
                    category = category,
                ))
            }) { Text("Creează") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Anulează") } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryPicker(value: String, onChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = value, onValueChange = {}, readOnly = true, label = { Text("Categorie") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            CATEGORIES.forEach { c -> DropdownMenuItem(text = { Text(c) }, onClick = { onChange(c); expanded = false }) }
        }
    }
}
