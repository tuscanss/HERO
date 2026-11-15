package ro.hero.irremote.ir

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Generatoare de semnal pentru protocoalele IR uzuale și un decodor NEC
 * (cel mai folosit protocol la plafonierele LED cu telecomandă IR).
 */
object IrProtocols {

    val SUPPORTED = listOf("NEC", "NECext", "Samsung32", "RC5", "RC5X", "RC6", "SIRC", "SIRC15", "SIRC20")

    fun encode(protocol: String, address: Int, command: Int): IrCode = when (protocol.uppercase()) {
        "NEC" -> nec(address and 0xFF, command and 0xFF)
        "NECEXT" -> necExt(address and 0xFFFF, command and 0xFFFF)
        "SAMSUNG32" -> samsung32(address and 0xFF, command and 0xFF)
        "RC5" -> rc5(address and 0x1F, command and 0x3F)
        "RC5X" -> rc5(address and 0x1F, (command and 0x3F) or 0x40)
        "RC6" -> rc6(address and 0xFF, command and 0xFF)
        "SIRC" -> sirc(command and 0x7F, address and 0x1F, 5)
        "SIRC15" -> sirc(command and 0x7F, address and 0xFF, 8)
        "SIRC20" -> sirc(command and 0x7F, address and 0x1FFF, 13)
        else -> throw IllegalArgumentException("Protocol nesuportat: $protocol")
    }

    private fun hex(v: Int, digits: Int = 2) = "0x" + v.toString(16).uppercase().padStart(digits, '0')

    // ---------------- NEC ----------------
    private const val NEC_HDR_MARK = 9000
    private const val NEC_HDR_SPACE = 4500
    private const val NEC_BIT_MARK = 560
    private const val NEC_ONE_SPACE = 1690
    private const val NEC_ZERO_SPACE = 560

    private fun necFrame(bits: Long, nbits: Int, hdrMark: Int, hdrSpace: Int): MutableList<Int> {
        val p = mutableListOf(hdrMark, hdrSpace)
        for (i in 0 until nbits) {
            p += NEC_BIT_MARK
            p += if ((bits shr i) and 1L == 1L) NEC_ONE_SPACE else NEC_ZERO_SPACE
        }
        p += NEC_BIT_MARK
        return p
    }

    fun nec(address: Int, command: Int): IrCode {
        val bits = (address.toLong()) or
            ((address.inv() and 0xFF).toLong() shl 8) or
            (command.toLong() shl 16) or
            ((command.inv() and 0xFF).toLong() shl 24)
        return IrCode(38_000, necFrame(bits, 32, NEC_HDR_MARK, NEC_HDR_SPACE), "NEC ${hex(address)}/${hex(command)}")
    }

    /** NEC extins: adresă pe 16 biți, comandă pe 16 biți (ca la Flipper „NECext”). */
    fun necExt(address: Int, command: Int): IrCode {
        val bits = (address.toLong() and 0xFFFF) or ((command.toLong() and 0xFFFF) shl 16)
        return IrCode(38_000, necFrame(bits, 32, NEC_HDR_MARK, NEC_HDR_SPACE), "NECext ${hex(address, 4)}/${hex(command, 4)}")
    }

    fun samsung32(address: Int, command: Int): IrCode {
        val bits = address.toLong() or (address.toLong() shl 8) or
            (command.toLong() shl 16) or ((command.inv() and 0xFF).toLong() shl 24)
        return IrCode(38_000, necFrame(bits, 32, 4500, 4500), "Samsung32 ${hex(address)}/${hex(command)}")
    }

    // ---------------- Manchester helpers ----------------
    /** Transformă o listă de niveluri (true = puls) cu durate în pattern alternant. */
    private fun levelsToPattern(levels: List<Pair<Boolean, Int>>): List<Int> {
        val out = mutableListOf<Int>()
        var current: Boolean? = null
        for ((on, d) in levels) {
            if (out.isEmpty() && !on) continue // pattern-ul trebuie să înceapă cu puls
            if (on == current) out[out.lastIndex] += d else { out += d; current = on }
        }
        if (current == false) out.removeAt(out.lastIndex)
        return out
    }

    fun rc5(address: Int, command: Int, toggle: Boolean = false): IrCode {
        val t = 889
        val field = if (command and 0x40 != 0) 0 else 1 // RC5X: bitul 6 al comenzii, inversat
        val bits = mutableListOf(1, field, if (toggle) 1 else 0)
        for (i in 4 downTo 0) bits += (address shr i) and 1
        for (i in 5 downTo 0) bits += (command shr i) and 1
        val levels = mutableListOf<Pair<Boolean, Int>>()
        for (b in bits) {
            // RC5: 1 = pauză apoi puls, 0 = puls apoi pauză
            if (b == 1) { levels += false to t; levels += true to t } else { levels += true to t; levels += false to t }
        }
        return IrCode(36_000, levelsToPattern(levels), "RC5 ${hex(address)}/${hex(command)}")
    }

    fun rc6(address: Int, command: Int, toggle: Boolean = false): IrCode {
        val t = 444
        val levels = mutableListOf<Pair<Boolean, Int>>(true to 6 * t, false to 2 * t)
        fun bit(b: Int, w: Int = 1) {
            // RC6: 1 = puls apoi pauză, 0 = pauză apoi puls
            if (b == 1) { levels += true to t * w; levels += false to t * w } else { levels += false to t * w; levels += true to t * w }
        }
        bit(1)            // bit de start
        bit(0); bit(0); bit(0) // mod 0
        bit(if (toggle) 1 else 0, 2) // bit toggle, durată dublă
        for (i in 7 downTo 0) bit((address shr i) and 1)
        for (i in 7 downTo 0) bit((command shr i) and 1)
        return IrCode(36_000, levelsToPattern(levels), "RC6 ${hex(address)}/${hex(command)}")
    }

    fun sirc(command: Int, address: Int, addressBits: Int): IrCode {
        val frame = mutableListOf(2400, 600)
        val value = command.toLong() or (address.toLong() shl 7)
        val n = 7 + addressBits
        for (i in 0 until n) {
            frame += if ((value shr i) and 1L == 1L) 1200 else 600
            frame += 600
        }
        // Sony cere repetarea cadrului (minim 3), cu perioada de 45 ms.
        val frameLen = frame.sum()
        frame[frame.lastIndex] = 600 + (45_000 - frameLen).coerceAtLeast(600)
        val p = mutableListOf<Int>()
        repeat(3) { p += frame }
        p.removeAt(p.lastIndex)
        return IrCode(40_000, p, "SIRC${if (addressBits == 5) "" else (7 + addressBits)} ${hex(address)}/${hex(command)}")
    }

    // ---------------- Decodare ----------------
    private fun near(v: Int, target: Int, tol: Double = 0.35) = abs(v - target) <= target * tol

    data class Decoded(val protocol: String, val address: Int, val command: Int)

    /**
     * Încearcă să recunoască un cadru NEC/NECext/Samsung32 într-o captură brută.
     * Receptoarele IR (TSOP) lungesc pulsurile cu ~100 µs, de aceea toleranța e generoasă.
     */
    fun decode(pattern: List<Int>): Decoded? {
        if (pattern.size < 67) return null
        val samsung = near(pattern[0], 4500) && near(pattern[1], 4500)
        val nec = near(pattern[0], 9000) && near(pattern[1], 4500)
        if (!nec && !samsung) return null
        var bits = 0L
        for (i in 0 until 32) {
            val mark = pattern[2 + 2 * i]
            val space = pattern[3 + 2 * i]
            if (!near(mark, 560, 0.5)) return null
            bits = bits or when {
                near(space, 1690, 0.4) -> 1L shl i
                near(space, 560, 0.6) -> 0L
                else -> return null
            }
        }
        val a = (bits and 0xFF).toInt()
        val na = ((bits shr 8) and 0xFF).toInt()
        val c = ((bits shr 16) and 0xFF).toInt()
        val nc = ((bits shr 24) and 0xFF).toInt()
        return when {
            samsung && a == na && c == (nc.inv() and 0xFF) -> Decoded("Samsung32", a, c)
            nec && a == (na.inv() and 0xFF) && c == (nc.inv() and 0xFF) -> Decoded("NEC", a, c)
            nec -> Decoded("NECext", (bits and 0xFFFF).toInt(), ((bits shr 16) and 0xFFFF).toInt())
            else -> null
        }
    }

    /**
     * Normalizează o captură: dacă e un protocol cunoscut regenerează semnalul
     * „curat”, altfel păstrează datele brute (rotunjite, fără pauza finală).
     */
    fun normalizeCapture(frequency: Int, raw: List<Int>): IrCode {
        val trimmed = raw.toMutableList()
        if (trimmed.size % 2 == 0 && trimmed.isNotEmpty()) trimmed.removeAt(trimmed.lastIndex)
        decode(trimmed)?.let { return encode(it.protocol, it.address, it.command) }
        val freq = if (frequency in 15_000..100_000) frequency else 38_000
        return IrCode(freq, trimmed.map { (it / 10.0).roundToInt() * 10 }.map { it.coerceAtLeast(10) }, "RAW (${trimmed.size} durate)")
    }
}
