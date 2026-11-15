package ro.hero.irremote.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

/** Iconițe disponibile pentru butoane (cheie -> iconiță + nume afișat). */
val BUTTON_ICONS: List<Triple<String, ImageVector, String>> = listOf(
    Triple("power", Icons.Filled.PowerSettingsNew, "Power"),
    Triple("power_on", Icons.Filled.Lightbulb, "Pornit"),
    Triple("power_off", Icons.Filled.LightbulbCircle, "Oprit"),
    Triple("bright_up", Icons.Filled.BrightnessHigh, "Lumină +"),
    Triple("bright_down", Icons.Filled.BrightnessLow, "Lumină −"),
    Triple("warm", Icons.Filled.WbIncandescent, "Cald"),
    Triple("cold", Icons.Filled.AcUnit, "Rece"),
    Triple("neutral", Icons.Filled.WbSunny, "Neutru"),
    Triple("night", Icons.Filled.Bedtime, "Noapte"),
    Triple("max", Icons.Filled.Brightness7, "Maxim"),
    Triple("timer", Icons.Filled.Timer, "Timer"),
    Triple("memory", Icons.Filled.Bookmark, "Memorie"),
    Triple("mode", Icons.Filled.Tune, "Mod"),
    Triple("vol_up", Icons.AutoMirrored.Filled.VolumeUp, "Volum +"),
    Triple("vol_down", Icons.AutoMirrored.Filled.VolumeDown, "Volum −"),
    Triple("mute", Icons.AutoMirrored.Filled.VolumeOff, "Mut"),
    Triple("ch_up", Icons.Filled.KeyboardArrowUp, "Canal +"),
    Triple("ch_down", Icons.Filled.KeyboardArrowDown, "Canal −"),
    Triple("up", Icons.Filled.ArrowUpward, "Sus"),
    Triple("down", Icons.Filled.ArrowDownward, "Jos"),
    Triple("left", Icons.AutoMirrored.Filled.ArrowBack, "Stânga"),
    Triple("right", Icons.AutoMirrored.Filled.ArrowForward, "Dreapta"),
    Triple("ok", Icons.Filled.CheckCircle, "OK"),
    Triple("back", Icons.Filled.Undo, "Înapoi"),
    Triple("home", Icons.Filled.Home, "Home"),
    Triple("menu", Icons.Filled.Menu, "Meniu"),
    Triple("input", Icons.Filled.Input, "Sursă"),
    Triple("fan", Icons.Filled.Air, "Ventilator"),
    Triple("play", Icons.Filled.PlayArrow, "Play"),
    Triple("star", Icons.Filled.Star, "Favorit"),
)

fun iconFor(key: String): ImageVector? = BUTTON_ICONS.firstOrNull { it.first == key }?.second

fun categoryIcon(category: String): ImageVector = when (category) {
    "Iluminat" -> Icons.Filled.Lightbulb
    "TV" -> Icons.Filled.Tv
    "Aer condiționat" -> Icons.Filled.AcUnit
    "Audio" -> Icons.Filled.Speaker
    "Ventilator" -> Icons.Filled.Air
    "Proiector" -> Icons.Filled.Videocam
    else -> Icons.Filled.SettingsRemote
}
