package ro.hero.irremote.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ro.hero.irremote.data.Remote
import ro.hero.irremote.data.RemoteButton
import ro.hero.irremote.data.RemoteRepository
import ro.hero.irremote.ir.IrCode
import ro.hero.irremote.ir.IrFormats
import ro.hero.irremote.ir.IrProtocols
import ro.hero.irremote.ir.IrTransmitter
import ro.hero.irremote.usb.UsbIrLearner

class AppViewModel(app: Application) : AndroidViewModel(app) {
    val repo = RemoteRepository(app)
    val transmitter = IrTransmitter(app)
    val learner = UsbIrLearner(app)

    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()
    fun toast(msg: String?) { _message.value = msg }
    fun consumeMessage() { _message.value = null }

    fun send(code: IrCode?, repeats: Int = 1): Boolean {
        if (code == null) { toast("Butonul nu are încă un cod. Clonează-l mai întâi."); return false }
        val err = transmitter.send(code, repeats)
        if (err != null) toast(err)
        return err == null
    }

    // ---------------- Căutătorul de coduri (scanare NEC) ----------------
    data class ScanState(
        val protocol: String = "NEC",
        val address: Int = 0x00,
        val command: Int = 0x00,
        val running: Boolean = false,
        val intervalMs: Long = 1200,
    ) {
        val code: IrCode get() = IrProtocols.encode(protocol, address, command)
        val maxCommand: Int get() = if (protocol == "NECext") 0xFFFF else 0xFF
    }

    private val _scan = MutableStateFlow(ScanState())
    val scan = _scan.asStateFlow()
    private var scanJob: Job? = null

    fun updateScan(s: ScanState) { _scan.value = s }

    fun scanStep(delta: Int, sendNow: Boolean = true) {
        val s = _scan.value
        val next = (s.command + delta).mod(s.maxCommand + 1)
        _scan.value = s.copy(command = next)
        if (sendNow) send(_scan.value.code)
    }

    fun toggleAutoScan() {
        if (scanJob?.isActive == true) { stopAutoScan(); return }
        _scan.value = _scan.value.copy(running = true)
        scanJob = viewModelScope.launch {
            send(_scan.value.code)
            while (true) {
                delay(_scan.value.intervalMs)
                scanStep(+1)
            }
        }
    }

    fun stopAutoScan() {
        scanJob?.cancel()
        scanJob = null
        _scan.value = _scan.value.copy(running = false)
    }

    // ---------------- Import / export ----------------
    fun importFile(uri: Uri, displayName: String?) {
        try {
            val text = getApplication<Application>().contentResolver.openInputStream(uri)!!.bufferedReader().readText()
            if (text.trimStart().startsWith("{")) {
                val n = repo.importJson(text)
                toast("Am importat $n telecomenzi")
            } else {
                val codes = IrFormats.parseFlipper(text)
                if (codes.isEmpty()) { toast("Nu am găsit coduri IR în fișier"); return }
                val name = displayName?.substringBeforeLast('.')?.replace('_', ' ') ?: "Telecomandă importată"
                repo.upsert(Remote(name = name, category = "Altele", notes = "Importată din fișier .ir",
                    buttons = codes.map { RemoteButton(label = it.name.replace('_', ' '), code = it.code) }))
                toast("Am importat ${codes.size} butoane în „$name”")
            }
        } catch (e: Exception) {
            toast("Import eșuat: ${e.message}")
        }
    }

    fun writeText(uri: Uri, text: String) {
        try {
            getApplication<Application>().contentResolver.openOutputStream(uri)!!.use { it.write(text.toByteArray()) }
            toast("Fișier salvat")
        } catch (e: Exception) {
            toast("Salvare eșuată: ${e.message}")
        }
    }

    fun flipperExport(remote: Remote): String = IrFormats.toFlipper(
        remote.buttons.mapNotNull { b -> b.code?.let { IrFormats.NamedCode(b.label, it) } },
    )

    override fun onCleared() {
        learner.disconnect()
        super.onCleared()
    }
}
