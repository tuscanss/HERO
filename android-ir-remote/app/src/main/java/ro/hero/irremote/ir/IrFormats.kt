package ro.hero.irremote.ir

import kotlin.math.roundToInt

/** Import / export pentru formatele de coduri IR des întâlnite. */
object IrFormats {

    // ---------------- Pronto HEX ----------------
    fun parsePronto(text: String): IrCode {
        val words = text.trim().split(Regex("\\s+")).map { it.toInt(16) }
        require(words.size >= 6 && words[0] == 0) { "Cod Pronto invalid (trebuie să înceapă cu 0000)" }
        val freq = (1_000_000.0 / (words[1] * 0.241246)).roundToInt()
        val once = words[2]
        val rep = words[3]
        require(words.size >= 4 + 2 * (once + rep)) { "Cod Pronto incomplet" }
        val periodUs = 1_000_000.0 / freq
        val start = if (once > 0) 4 else 4 + 2 * once
        val count = if (once > 0) once else rep
        val p = (0 until count * 2).map { (words[start + it] * periodUs).roundToInt().coerceAtLeast(1) }.toMutableList()
        if (p.size % 2 == 0) p.removeAt(p.lastIndex)
        return IrCode(freq, p, "Pronto")
    }

    fun toPronto(code: IrCode): String {
        val fcode = (1_000_000.0 / (code.frequency * 0.241246)).roundToInt()
        val periodUs = 1_000_000.0 / code.frequency
        val p = code.pattern.toMutableList()
        if (p.size % 2 == 1) p += 40_000 // pauză finală
        val words = mutableListOf(0, fcode, p.size / 2, 0) + p.map { (it / periodUs).roundToInt().coerceAtLeast(1) }
        return words.joinToString(" ") { it.toString(16).uppercase().padStart(4, '0') }
    }

    // ---------------- Durate brute ----------------
    /** Acceptă „9000 4500 560 …”, „+9000 -4500 …” sau separate prin virgulă. */
    fun parseRaw(text: String, frequency: Int): IrCode {
        val values = text.split(Regex("[\\s,;]+")).filter { it.isNotBlank() }
            .map { kotlin.math.abs(it.trim().toDouble()).roundToInt() }
        require(values.isNotEmpty()) { "Nu am găsit durate" }
        return IrProtocols.normalizeCapture(frequency, values)
    }

    // ---------------- Flipper Zero .ir ----------------
    data class NamedCode(val name: String, val code: IrCode)

    private fun leHex(s: String): Int {
        val bytes = s.trim().split(Regex("\\s+")).map { it.toInt(16) }
        var v = 0
        bytes.forEachIndexed { i, b -> v = v or (b shl (8 * i)) }
        return v
    }

    /** Citește un fișier .ir (Flipper Zero) — o bază mare de telecomenzi publice folosește acest format. */
    fun parseFlipper(text: String): List<NamedCode> {
        val result = mutableListOf<NamedCode>()
        val blocks = text.split(Regex("(?m)^#.*$"))
        for (block in blocks) {
            val kv = block.lines().mapNotNull { line ->
                val i = line.indexOf(':')
                if (i <= 0) null else line.substring(0, i).trim().lowercase() to line.substring(i + 1).trim()
            }.toMap()
            val name = kv["name"] ?: continue
            try {
                when (kv["type"]) {
                    "parsed" -> {
                        val proto = kv["protocol"] ?: continue
                        val code = IrProtocols.encode(proto, leHex(kv["address"] ?: "00"), leHex(kv["command"] ?: "00"))
                        result += NamedCode(name, code)
                    }
                    "raw" -> {
                        val freq = kv["frequency"]?.toIntOrNull() ?: 38_000
                        val data = kv["data"]?.split(Regex("\\s+"))?.mapNotNull { it.toIntOrNull() } ?: continue
                        val p = data.toMutableList()
                        if (p.size % 2 == 0) p.removeAt(p.lastIndex)
                        result += NamedCode(name, IrCode(freq, p, "RAW Flipper"))
                    }
                }
            } catch (_: Exception) {
                // protocol necunoscut: îl sărim
            }
        }
        return result
    }

    fun toFlipper(codes: List<NamedCode>): String = buildString {
        appendLine("Filetype: IR signals file")
        appendLine("Version: 1")
        for (c in codes) {
            appendLine("# ")
            appendLine("name: ${c.name.replace(' ', '_')}")
            appendLine("type: raw")
            appendLine("frequency: ${c.code.frequency}")
            appendLine("duty_cycle: 0.330000")
            appendLine("data: ${c.code.pattern.joinToString(" ")}")
        }
    }
}
