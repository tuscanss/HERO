package ro.hero.irremote.usb

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import com.hoho.android.usbserial.util.SerialInputOutputManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import ro.hero.irremote.ir.IrCode
import ro.hero.irremote.ir.IrProtocols

/**
 * Telefoanele Android au doar EMIȚĂTOR IR, nu și receptor. Pentru a „asculta”
 * telecomanda originală folosim un receptor IR ieftin (Arduino/ESP32 + TSOP38238)
 * conectat prin cablu USB‑OTG. Firmware-ul din /hardware/ir_learner trimite linii:
 *
 *     IR <frecventa_Hz> <durata1> <durata2> ...
 *
 * (durate în microsecunde, alternând puls/pauză).
 */
class UsbIrLearner(private val context: Context) {

    data class State(
        val connected: Boolean = false,
        val status: String = "Neconectat",
        val lastCapture: IrCode? = null,
        val captureCount: Int = 0,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    private var port: UsbSerialPort? = null
    private var io: SerialInputOutputManager? = null
    private val buffer = StringBuilder()
    private var receiverRegistered = false

    private val permissionReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, intent: Intent) {
            if (intent.action == ACTION_PERMISSION) {
                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) connect()
                else _state.value = _state.value.copy(status = "Permisiune USB refuzată")
            }
        }
    }

    fun connect() {
        if (port != null) return
        val drivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)
        if (drivers.isEmpty()) {
            _state.value = State(status = "Niciun receptor USB detectat. Conectează-l prin adaptor OTG.")
            return
        }
        val driver = drivers.first()
        if (!usbManager.hasPermission(driver.device)) {
            if (!receiverRegistered) {
                ContextCompat.registerReceiver(
                    context, permissionReceiver, IntentFilter(ACTION_PERMISSION), ContextCompat.RECEIVER_NOT_EXPORTED,
                )
                receiverRegistered = true
            }
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
            val pi = PendingIntent.getBroadcast(
                context, 0, Intent(ACTION_PERMISSION).setPackage(context.packageName), flags,
            )
            usbManager.requestPermission(driver.device, pi)
            _state.value = State(status = "Aprobă accesul la dispozitivul USB…")
            return
        }
        try {
            val connection = usbManager.openDevice(driver.device)
                ?: throw IllegalStateException("Nu pot deschide dispozitivul")
            val p = driver.ports.first()
            p.open(connection)
            p.setParameters(115_200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)
            try { p.dtr = true } catch (_: Exception) {}
            port = p
            io = SerialInputOutputManager(p, object : SerialInputOutputManager.Listener {
                override fun onNewData(data: ByteArray) = onBytes(data)
                override fun onRunError(e: Exception) {
                    disconnect()
                    _state.value = State(status = "Receptor deconectat: ${e.message}")
                }
            }).also { it.start() }
            _state.value = State(connected = true, status = "Conectat la ${driver.device.productName ?: "receptor"}. Apasă un buton pe telecomanda originală, îndreptată spre receptor.")
        } catch (e: Exception) {
            disconnect()
            _state.value = State(status = "Eroare conectare: ${e.message}")
        }
    }

    private fun onBytes(data: ByteArray) {
        synchronized(buffer) {
            buffer.append(String(data, Charsets.US_ASCII))
            while (true) {
                val nl = buffer.indexOf("\n")
                if (nl < 0) break
                val line = buffer.substring(0, nl).trim()
                buffer.delete(0, nl + 1)
                handleLine(line)
            }
            if (buffer.length > 20_000) buffer.clear()
        }
    }

    private fun handleLine(line: String) {
        if (!line.startsWith("IR ")) return
        val nums = line.removePrefix("IR ").split(' ').mapNotNull { it.toIntOrNull() }
        if (nums.size < 8) return
        val code = IrProtocols.normalizeCapture(nums[0], nums.drop(1))
        val s = _state.value
        _state.value = s.copy(lastCapture = code, captureCount = s.captureCount + 1, status = "Semnal capturat: ${code.description}")
    }

    fun clearCapture() { _state.value = _state.value.copy(lastCapture = null) }

    fun disconnect() {
        try { io?.stop() } catch (_: Exception) {}
        try { port?.close() } catch (_: Exception) {}
        io = null
        port = null
        _state.value = _state.value.copy(connected = false, status = "Neconectat")
    }

    companion object {
        private const val ACTION_PERMISSION = "ro.hero.irremote.USB_PERMISSION"
    }
}
