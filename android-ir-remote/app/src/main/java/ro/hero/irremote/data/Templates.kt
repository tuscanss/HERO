package ro.hero.irremote.data

/**
 * Șabloane de telecomenzi: aranjamentul butoanelor este gata, codurile se
 * „învață” (clonează) de pe telecomanda originală sau se găsesc cu Căutătorul de coduri.
 *
 * Notă: producătorul nu publică codurile IR pentru plafonierele Novecento, de aceea
 * șablonul vine fără coduri — trebuie clonate de pe telecomanda ta.
 */
data class RemoteTemplate(
    val title: String,
    val brand: String,
    val category: String,
    val description: String,
    val buttons: List<Pair<String, String>>, // eticheta to iconiță
) {
    fun instantiate(name: String = title) = Remote(
        name = name,
        brand = brand,
        category = category,
        notes = description,
        buttons = buttons.map { (label, icon) -> RemoteButton(label = label, icon = icon) },
    )
}

object Templates {
    val NOVECENTO_PLAFONIERA = RemoteTemplate(
        title = "Plafonieră Novecento",
        brand = "Novecento",
        category = "Iluminat",
        description = "Plafonieră LED cu telecomandă (CCT: alb cald → alb rece, dimabilă). " +
            "Clonează fiecare buton de pe telecomanda originală.",
        buttons = listOf(
            "Pornit" to "power_on",
            "Oprit" to "power_off",
            "Luminozitate +" to "bright_up",
            "Luminozitate −" to "bright_down",
            "Alb cald" to "warm",
            "Alb rece" to "cold",
            "Neutru 4000K" to "neutral",
            "Lumină de noapte" to "night",
            "100% maxim" to "max",
            "Temporizator" to "timer",
            "Memorie" to "memory",
            "Mod / CCT" to "mode",
        ),
    )

    val GENERIC_LIGHT = RemoteTemplate(
        title = "Lustră / bandă LED (generic)",
        brand = "",
        category = "Iluminat",
        description = "Telecomandă generică pentru corpuri de iluminat.",
        buttons = listOf(
            "Pornit" to "power_on", "Oprit" to "power_off",
            "Luminozitate +" to "bright_up", "Luminozitate −" to "bright_down",
            "Alb cald" to "warm", "Alb rece" to "cold", "Noapte" to "night",
        ),
    )

    val TV = RemoteTemplate(
        title = "Televizor",
        brand = "",
        category = "TV",
        description = "Telecomandă TV de bază.",
        buttons = listOf(
            "Power" to "power", "Sursă" to "input", "Volum +" to "vol_up", "Volum −" to "vol_down",
            "Mut" to "mute", "Canal +" to "ch_up", "Canal −" to "ch_down", "OK" to "ok",
            "Sus" to "up", "Jos" to "down", "Stânga" to "left", "Dreapta" to "right",
            "Înapoi" to "back", "Home" to "home", "Meniu" to "menu",
        ),
    )

    val AC = RemoteTemplate(
        title = "Aer condiționat",
        brand = "",
        category = "Aer condiționat",
        description = "Notă: telecomenzile AC trimit toată starea la fiecare apăsare; " +
            "clonează fiecare combinație de care ai nevoie (ex. „Rece 22°”).",
        buttons = listOf(
            "Oprit" to "power_off", "Rece 22°" to "cold", "Rece 24°" to "cold",
            "Cald 24°" to "warm", "Ventilație" to "fan", "Swing" to "mode",
        ),
    )

    val FAN = RemoteTemplate(
        title = "Ventilator",
        brand = "",
        category = "Ventilator",
        description = "",
        buttons = listOf(
            "Power" to "power", "Viteză" to "fan", "Oscilare" to "mode", "Temporizator" to "timer",
        ),
    )

    val ALL = listOf(NOVECENTO_PLAFONIERA, GENERIC_LIGHT, TV, AC, FAN)
}
