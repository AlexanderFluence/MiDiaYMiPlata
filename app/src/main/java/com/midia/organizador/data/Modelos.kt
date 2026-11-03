package com.midia.organizador.data

import androidx.compose.ui.graphics.Color
import java.util.Locale

/** Categorías de tiempo, cada una con su color. */
enum class Cat(val label: String, val emoji: String, val color: Color) {
    TRABAJO("Trabajo", "💼", Color(0xFF3B82F6)),
    ESTUDIO("Estudio", "🎓", Color(0xFF22C55E)),
    DESCANSO("Descanso", "☕", Color(0xFFF59E0B)),
    APRENDIZAJE("Aprendizaje", "💻", Color(0xFF8B5CF6)),
    OTRO("Otro", "📌", Color(0xFF64748B))
}

enum class TipoMov(val label: String) { INGRESO("Ingreso"), GASTO("Gasto"), AHORRO("Ahorro") }

val CATS_GASTO = listOf("Departamento", "Carro", "Servicios", "Universidad", "Casa", "Otros")
val CATS_INGRESO = listOf("Sueldo fijo", "Ingreso extra")

fun colorGasto(cat: String): Color = when (cat) {
    "Departamento" -> Color(0xFF3B82F6)
    "Carro" -> Color(0xFFEF4444)
    "Servicios" -> Color(0xFFF59E0B)
    "Universidad" -> Color(0xFF22C55E)
    "Casa" -> Color(0xFF8B5CF6)
    else -> Color(0xFF64748B)
}

fun emojiGasto(cat: String): String = when (cat) {
    "Departamento" -> "🏢"
    "Carro" -> "🚗"
    "Servicios" -> "💡"
    "Universidad" -> "🎓"
    "Casa" -> "🛒"
    "Sueldo fijo" -> "💵"
    "Ingreso extra" -> "➕"
    else -> "📦"
}

/**
 * Actividad que se repite cada semana.
 * dias: 1 = lunes ... 7 = domingo. inicio/fin en minutos desde medianoche.
 */
data class Actividad(
    val id: Long,
    val titulo: String,
    val cat: Cat,
    val dias: Set<Int>,
    val inicio: Int,
    val fin: Int,
    val avisoAntes: Int,
    val pausas: Boolean = false,
    val alertas: Boolean = true
)

data class Movimiento(
    val id: Long,
    val tipo: TipoMov,
    val cat: String,
    val monto: Double,
    val fecha: Long, // LocalDate.toEpochDay()
    val nota: String
)

data class PagoFijo(
    val id: Long,
    val nombre: String,
    val cat: String,
    val monto: Double,
    val dia: Int,
    val pagadoMes: String? = null // "2026-11" si ya se pagó ese mes
)

data class Meta(val id: Long, val nombre: String, val objetivo: Double, val ahorrado: Double)

data class Habilidad(
    val id: Long,
    val nombre: String,
    val progreso: Int,
    val horas: Double,
    val racha: Int,
    val ultimoDia: Long // epochDay de la última práctica, -1 si nunca
)

data class Config(
    val sueldoFijo: Double = 3600.0,
    val diaPago: Int = 1,
    val ahorroPctFijo: Int = 10,
    val ahorroPctExtra: Int = 50,
    val presupuestos: Map<String, Double> = emptyMap()
)

data class Resumen(val ingresos: Double, val gastos: Double, val ahorro: Double) {
    val saldo: Double get() = ingresos - gastos - ahorro
}

val DIAS_CORTOS = listOf("L", "M", "X", "J", "V", "S", "D")
val DIAS_LARGOS = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")
val LOCALE_ES: Locale = Locale.forLanguageTag("es-PE")

fun soles(x: Double): String = String.format(Locale.US, "S/ %,.2f", x)

fun hhmm(min: Int): String {
    val m = ((min % 1440) + 1440) % 1440
    val h = m / 60
    val h12 = if (h % 12 == 0) 12 else h % 12
    val ap = if (h < 12) "am" else "pm"
    return String.format(Locale.US, "%d:%02d %s", h12, m % 60, ap)
}

fun horas(x: Double): String =
    if (x % 1.0 == 0.0) "${x.toInt()} h" else String.format(Locale.US, "%.1f h", x)
