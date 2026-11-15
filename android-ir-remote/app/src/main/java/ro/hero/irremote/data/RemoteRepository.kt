package ro.hero.irremote.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/** Biblioteca de telecomenzi, salvată local ca JSON. */
class RemoteRepository(context: Context) {
    private val file = File(context.filesDir, "library.json")
    private val prefs = context.getSharedPreferences("app", Context.MODE_PRIVATE)

    private val _remotes = MutableStateFlow(load())
    val remotes: StateFlow<List<Remote>> = _remotes.asStateFlow()

    init {
        // La prima pornire adăugăm direct telecomanda pentru plafoniera Novecento.
        if (!prefs.getBoolean("seeded", false)) {
            if (_remotes.value.isEmpty()) upsert(Templates.NOVECENTO_PLAFONIERA.instantiate())
            prefs.edit().putBoolean("seeded", true).apply()
        }
    }

    private fun load(): List<Remote> = try {
        if (file.exists()) json.decodeFromString<Library>(file.readText()).remotes else emptyList()
    } catch (_: Exception) {
        emptyList()
    }

    @Synchronized
    private fun save(list: List<Remote>) {
        _remotes.value = list
        val tmp = File(file.parentFile, "library.json.tmp")
        tmp.writeText(json.encodeToString(Library(list)))
        tmp.renameTo(file)
    }

    fun get(id: String): Remote? = _remotes.value.firstOrNull { it.id == id }

    fun upsert(remote: Remote) {
        val list = _remotes.value.toMutableList()
        val i = list.indexOfFirst { it.id == remote.id }
        if (i >= 0) list[i] = remote else list += remote
        save(list)
    }

    fun delete(id: String) = save(_remotes.value.filterNot { it.id == id })

    fun updateButton(remoteId: String, button: RemoteButton) {
        val r = get(remoteId) ?: return
        val buttons = r.buttons.toMutableList()
        val i = buttons.indexOfFirst { it.id == button.id }
        if (i >= 0) buttons[i] = button else buttons += button
        upsert(r.copy(buttons = buttons))
    }

    fun deleteButton(remoteId: String, buttonId: String) {
        val r = get(remoteId) ?: return
        upsert(r.copy(buttons = r.buttons.filterNot { it.id == buttonId }))
    }

    fun moveButton(remoteId: String, buttonId: String, delta: Int) {
        val r = get(remoteId) ?: return
        val b = r.buttons.toMutableList()
        val i = b.indexOfFirst { it.id == buttonId }
        val j = i + delta
        if (i < 0 || j !in b.indices) return
        b.add(j, b.removeAt(i))
        upsert(r.copy(buttons = b))
    }

    /** Exportă o telecomandă (sau toată biblioteca) ca JSON pentru backup/partajare. */
    fun exportJson(remotes: List<Remote>): String = json.encodeToString(Library(remotes))

    /** Importă JSON exportat de aplicație; primește ID-uri noi ca să nu suprascrie nimic. */
    fun importJson(text: String): Int {
        val lib = json.decodeFromString<Library>(text)
        lib.remotes.forEach { r ->
            upsert(r.copy(id = newId(), buttons = r.buttons.map { it.copy(id = newId()) }))
        }
        return lib.remotes.size
    }

    companion object {
        val json = Json { ignoreUnknownKeys = true; prettyPrint = true; encodeDefaults = true }
    }
}
