package ro.hero.irremote.ir

import android.content.Context
import android.hardware.ConsumerIrManager

/** Trimite coduri prin emițătorul IR („IR blaster”) al telefonului. */
class IrTransmitter(context: Context) {
    private val manager = context.getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager

    val hasEmitter: Boolean get() = manager?.hasIrEmitter() == true

    fun frequencyRanges(): List<IntRange> =
        manager?.carrierFrequencies?.map { it.minFrequency..it.maxFrequency }.orEmpty()

    /** Returnează null la succes sau un mesaj de eroare în română. */
    fun send(code: IrCode, repeats: Int = 1): String? {
        val m = manager ?: return "Telefonul nu are serviciu IR."
        if (!m.hasIrEmitter()) return "Telefonul nu are emițător IR (IR blaster)."
        if (!code.isValid()) return "Cod IR invalid."
        val pattern = code.pattern.toIntArray()
        return try {
            repeat(repeats.coerceIn(1, 5)) { m.transmit(code.frequency, pattern) }
            null
        } catch (e: Exception) {
            "Eroare la transmisie: ${e.message}"
        }
    }
}
