package ro.hero.irremote.ir

import kotlinx.serialization.Serializable

/**
 * Un semnal IR gata de trimis: frecvența purtătoarei (Hz) și o succesiune de
 * durate în microsecunde, alternând PULS (LED aprins) / PAUZĂ, începând cu puls.
 * Exact formatul cerut de ConsumerIrManager.transmit().
 */
@Serializable
data class IrCode(
    val frequency: Int = 38_000,
    val pattern: List<Int>,
    /** Descriere lizibilă, ex. "NEC 0x00/0x45" sau "RAW". */
    val description: String = "RAW",
) {
    val totalMicros: Long get() = pattern.sumOf { it.toLong() }

    fun isValid(): Boolean =
        pattern.isNotEmpty() && pattern.all { it > 0 } && frequency in 15_000..100_000
}
