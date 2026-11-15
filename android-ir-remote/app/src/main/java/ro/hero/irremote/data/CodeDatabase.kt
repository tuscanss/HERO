package ro.hero.irremote.data

import android.content.res.AssetManager
import ro.hero.irremote.ir.IrFormats

/**
 * Bază de coduri preinstalată: telecomenzi IR pentru corpuri de iluminat din
 * colecția publică Flipper-IRDB (licență CC0), inclusă în assets/irdb.
 */
data class CodeSet(
    val brand: String,
    val model: String,
    val path: String,
    val codes: List<IrFormats.NamedCode>,
    /** Plafonieră / lumină albă reglabilă (cald-rece) – cele mai probabile pentru Novecento. */
    val likelyCeiling: Boolean,
) {
    val title: String get() = if (brand == "Unknown") "Generic – $model" else "$brand – $model"

    /** Butonul folosit de asistent ca să testeze dacă lampa răspunde (preferăm „stinge”). */
    val probeButton: IrFormats.NamedCode?
        get() {
            val order = listOf("off", "power_off", "poweroff", "power", "on/off", "onoff", "on", "power_on")
            for (key in order) codes.firstOrNull { normalize(it.name) == key }?.let { return it }
            return codes.firstOrNull { normalize(it.name).contains("power") || normalize(it.name).contains("off") }
                ?: codes.firstOrNull()
        }

    fun toRemote(name: String, brandOverride: String? = null) = Remote(
        name = name,
        brand = brandOverride ?: if (brand == "Unknown") "" else brand,
        category = "Iluminat",
        notes = "Coduri din baza preinstalată: $title",
        buttons = codes.map { RemoteButton(label = it.name.replace('_', ' '), icon = guessIcon(it.name), code = it.code) },
    )

    companion object {
        fun normalize(s: String) = s.lowercase().replace(' ', '_').trim()

        private val CEILING_HINTS = listOf("ceiling", "decken", "plafon", "soffitto", "warm", "cold", "cool", "cct",
            "neutral", "night", "nacht", "dimm", "brighter", "dimmer", "kelvin", "3000k", "6000k")

        fun guessIcon(label: String): String {
            val n = normalize(label)
            return when {
                n in listOf("off", "power_off", "poweroff") || n.endsWith("_off") -> "power_off"
                n in listOf("on", "power_on", "poweron") || n.endsWith("_on") -> "power_on"
                n.contains("power") -> "power"
                n.contains("warm") -> "warm"
                n.contains("cold") || n.contains("cool") -> "cold"
                n.contains("neutral") || n.contains("4000") -> "neutral"
                n.contains("night") || n.contains("nacht") || n.contains("moon") -> "night"
                n.contains("timer") || Regex("^\\d+h$").matches(n) || n.contains("min") -> "timer"
                n.contains("bright") || n == "up" || n.contains("+") || n.contains("plus") -> "bright_up"
                n.contains("dim") || n == "down" || n.contains("-") || n.contains("minus") -> "bright_down"
                n.contains("mode") || n.contains("auto") -> "mode"
                n.contains("max") || n.contains("100") -> "max"
                else -> ""
            }
        }

        fun isLikelyCeiling(path: String, codes: List<IrFormats.NamedCode>): Boolean {
            val text = (path + " " + codes.joinToString(" ") { it.name }).lowercase()
            return CEILING_HINTS.any { text.contains(it) }
        }
    }
}

object CodeDatabase {
    @Volatile private var cache: List<CodeSet>? = null

    fun load(assets: AssetManager): List<CodeSet> {
        cache?.let { return it }
        val result = mutableListOf<CodeSet>()
        val seen = HashSet<String>()
        fun walk(dir: String) {
            for (name in assets.list(dir).orEmpty()) {
                val path = "$dir/$name"
                if (name.endsWith(".ir")) {
                    try {
                        val text = assets.open(path).bufferedReader().use { it.readText() }
                        val codes = IrFormats.parseFlipper(text)
                        if (codes.isEmpty()) continue
                        // eliminăm seturile identice (aceeași telecomandă sub alt nume)
                        val fingerprint = codes.map { it.code.pattern.hashCode() }.sorted().joinToString(",")
                        if (!seen.add(fingerprint)) continue
                        val parts = path.removePrefix("irdb/LED_Lighting/").split('/')
                        val brand = parts.first().replace('_', ' ')
                        val model = name.removeSuffix(".ir").replace('_', ' ')
                        result += CodeSet(brand, model, path, codes, CodeSet.isLikelyCeiling(path, codes))
                    } catch (_: Exception) {
                    }
                } else if (!name.contains('.')) {
                    walk(path)
                }
            }
        }
        walk("irdb")
        val sorted = result.sortedWith(compareBy<CodeSet>({ !it.likelyCeiling }, { it.brand == "Unknown" }, { it.title }))
        cache = sorted
        return sorted
    }
}
