package ro.hero.irremote.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ro.hero.irremote.data.RemoteButton

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RemoteScreen(vm: AppViewModel, remoteId: String, onBack: () -> Unit, onLearn: (String) -> Unit, onWizard: () -> Unit) {
    val remotes by vm.repo.remotes.collectAsState()
    val remote = remotes.firstOrNull { it.id == remoteId }
    if (remote == null) { LaunchedEffect(Unit) { onBack() }; return }

    val haptic = LocalHapticFeedback.current
    var editing by remember { mutableStateOf<RemoteButton?>(null) }
    var editMode by remember { mutableStateOf(false) }
    var showDetails by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }

    val flipperLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        if (uri != null) vm.writeText(uri, vm.flipperExport(remote))
    }
    val jsonLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) vm.writeText(uri, vm.repo.exportJson(listOf(remote)))
    }
    val fileBase = remote.name.replace(Regex("[^A-Za-z0-9]+"), "_")

    AppScaffold(
        vm, remote.name, onBack = onBack,
        actions = {
            IconButton(onClick = { editMode = !editMode }) {
                Icon(if (editMode) Icons.Filled.Check else Icons.Filled.Edit, if (editMode) "Gata" else "Editează")
            }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, "Mai mult") }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Detalii / redenumește") }, leadingIcon = { Icon(Icons.Filled.Info, null) },
                        onClick = { menu = false; showDetails = true })
                    DropdownMenuItem(text = { Text("Exportă pentru Flipper (.ir)") }, leadingIcon = { Icon(Icons.Filled.Share, null) },
                        onClick = { menu = false; flipperLauncher.launch("$fileBase.ir") })
                    DropdownMenuItem(text = { Text("Exportă backup (.json)") }, leadingIcon = { Icon(Icons.Filled.Save, null) },
                        onClick = { menu = false; jsonLauncher.launch("$fileBase.json") })
                    DropdownMenuItem(text = { Text("Șterge telecomanda") }, leadingIcon = { Icon(Icons.Filled.Delete, null) },
                        onClick = { menu = false; confirmDelete = true })
                }
            }
        },
        fab = {
            if (editMode) FloatingActionButton(onClick = { editing = RemoteButton(label = "Buton nou") }) {
                Icon(Icons.Filled.Add, "Adaugă buton")
            }
        },
    ) { pad ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(span = { GridItemSpan(3) }) {
                val hint = when {
                    editMode -> "Mod editare: atinge un buton pentru a-l modifica, sau + pentru unul nou."
                    remote.learnedCount < remote.buttons.size ->
                        "Butoanele cu contur punctat nu au încă un cod. Atinge-le pentru a le clona. Apăsare lungă = editare."
                    else -> "Atinge un buton pentru a trimite comanda. Apăsare lungă = editare."
                }
                Column {
                    if (remote.learnedCount == 0 && !editMode) {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                            Column(Modifier.padding(16.dp)) {
                                Text("Telecomanda nu are încă coduri.", style = MaterialTheme.typography.titleSmall)
                                Text("Fără telecomanda originală? Asistentul găsește codurile încercându-le pe lampă.",
                                    style = MaterialTheme.typography.bodySmall)
                                Spacer(Modifier.height(8.dp))
                                Button(onClick = onWizard) {
                                    Icon(Icons.Filled.AutoFixHigh, null); Spacer(Modifier.width(8.dp)); Text("Găsește codurile automat")
                                }
                            }
                        }
                    }
                    Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            items(remote.buttons, key = { it.id }) { b ->
                val learned = b.learned
                OutlinedCard(
                    modifier = Modifier.aspectRatio(1f).combinedClickable(
                        onClick = {
                            when {
                                editMode -> editing = b
                                learned -> { haptic.performHapticFeedback(HapticFeedbackType.LongPress); vm.send(b.code) }
                                else -> onLearn(b.id)
                            }
                        },
                        onLongClick = { editing = b },
                    ),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(if (learned) 1.dp else 2.dp,
                        if (learned) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.tertiary),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = if (learned) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface),
                ) {
                    Column(Modifier.fillMaxSize().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center) {
                        Icon(iconFor(b.icon) ?: Icons.Filled.RadioButtonChecked, null, Modifier.size(32.dp))
                        Spacer(Modifier.height(6.dp))
                        Text(b.label, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center, maxLines = 2)
                        if (!learned) Text("de clonat", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                    }
                }
            }
            if (remote.notes.isNotBlank()) item(span = { GridItemSpan(3) }) {
                Text(remote.notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    editing?.let { b ->
        ButtonEditorDialog(
            button = b,
            isNew = remote.buttons.none { it.id == b.id },
            onDismiss = { editing = null },
            onSave = { vm.repo.updateButton(remote.id, it); editing = null },
            onLearn = { vm.repo.updateButton(remote.id, it); editing = null; onLearn(it.id) },
            onDelete = { vm.repo.deleteButton(remote.id, b.id); editing = null },
            onMove = { d -> vm.repo.moveButton(remote.id, b.id, d) },
            onTest = { vm.send(b.code) },
        )
    }

    if (showDetails) {
        var name by remember { mutableStateOf(remote.name) }
        var brand by remember { mutableStateOf(remote.brand) }
        var cat by remember { mutableStateOf(remote.category) }
        var notes by remember { mutableStateOf(remote.notes) }
        AlertDialog(
            onDismissRequest = { showDetails = false },
            title = { Text("Detalii telecomandă") },
            text = {
                Column {
                    OutlinedTextField(name, { name = it }, label = { Text("Nume") }, singleLine = true)
                    OutlinedTextField(brand, { brand = it }, label = { Text("Marcă") }, singleLine = true)
                    CategoryPicker(cat) { cat = it }
                    OutlinedTextField(notes, { notes = it }, label = { Text("Note") }, minLines = 2)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.repo.upsert(remote.copy(name = name.ifBlank { remote.name }, brand = brand, category = cat, notes = notes))
                    showDetails = false
                }) { Text("Salvează") }
            },
            dismissButton = { TextButton(onClick = { showDetails = false }) { Text("Anulează") } },
        )
    }

    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text("Ștergi „${remote.name}”?") },
        text = { Text("Toate codurile clonate pentru această telecomandă vor fi pierdute.") },
        confirmButton = { TextButton(onClick = { confirmDelete = false; vm.repo.delete(remote.id); onBack() }) { Text("Șterge") } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Anulează") } },
    )
}

@Composable
private fun ButtonEditorDialog(
    button: RemoteButton,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onSave: (RemoteButton) -> Unit,
    onLearn: (RemoteButton) -> Unit,
    onDelete: () -> Unit,
    onMove: (Int) -> Unit,
    onTest: () -> Unit,
) {
    var label by remember { mutableStateOf(button.label) }
    var icon by remember { mutableStateOf(button.icon) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isNew) "Buton nou" else "Editează butonul") },
        text = {
            Column {
                OutlinedTextField(label, { label = it }, label = { Text("Etichetă") }, singleLine = true)
                Spacer(Modifier.height(8.dp))
                Text("Iconiță", style = MaterialTheme.typography.labelLarge)
                LazyVerticalGrid(GridCells.Adaptive(44.dp), Modifier.heightIn(max = 160.dp)) {
                    items(BUTTON_ICONS) { (key, vec, name) ->
                        IconToggleButton(checked = icon == key, onCheckedChange = { icon = key }) {
                            Icon(vec, name, tint = if (icon == key) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(button.code?.let { "Cod: ${it.description} • ${it.frequency / 1000} kHz" } ?: "Fără cod încă",
                    style = MaterialTheme.typography.bodySmall)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { onLearn(button.copy(label = label, icon = icon)) }) {
                        Icon(Icons.Filled.SettingsRemote, null); Spacer(Modifier.width(4.dp))
                        Text(if (button.learned) "Re-clonează" else "Clonează codul")
                    }
                    if (button.learned) TextButton(onClick = onTest) { Text("Test") }
                }
                if (!isNew) Row {
                    IconButton(onClick = { onMove(-1) }) { Icon(Icons.Filled.ArrowUpward, "Mută înainte") }
                    IconButton(onClick = { onMove(+1) }) { Icon(Icons.Filled.ArrowDownward, "Mută după") }
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, "Șterge butonul", tint = MaterialTheme.colorScheme.error) }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(button.copy(label = label.ifBlank { "Buton" }, icon = icon)) }) { Text("Salvează") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Anulează") } },
    )
}
