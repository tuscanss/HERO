package ro.hero.irremote.data

import kotlinx.serialization.Serializable
import ro.hero.irremote.ir.IrCode
import java.util.UUID

fun newId(): String = UUID.randomUUID().toString()

@Serializable
data class RemoteButton(
    val id: String = newId(),
    val label: String,
    /** Cheie de iconiță, vezi ui/Icons.kt */
    val icon: String = "",
    val code: IrCode? = null,
) {
    val learned: Boolean get() = code != null
}

@Serializable
data class Remote(
    val id: String = newId(),
    val name: String,
    val brand: String = "",
    val category: String = "Altele",
    val notes: String = "",
    val buttons: List<RemoteButton> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
) {
    val learnedCount: Int get() = buttons.count { it.learned }
}

@Serializable
data class Library(val remotes: List<Remote> = emptyList())

val CATEGORIES = listOf("Iluminat", "TV", "Aer condiționat", "Audio", "Ventilator", "Proiector", "Altele")
