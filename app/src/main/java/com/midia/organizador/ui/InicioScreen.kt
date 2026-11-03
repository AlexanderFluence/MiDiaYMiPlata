package com.midia.organizador.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.midia.organizador.data.Cat
import com.midia.organizador.data.Store
import com.midia.organizador.data.LOCALE_ES
import com.midia.organizador.data.hhmm
import com.midia.organizador.data.horas
import com.midia.organizador.data.soles
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
fun InicioScreen(irA: (Int) -> Unit) {
    val ahora by produceState(LocalDateTime.now()) {
        while (true) {
            delay(30_000)
            value = LocalDateTime.now()
        }
    }
    val dia = ahora.dayOfWeek.value
    val minAhora = ahora.hour * 60 + ahora.minute
    val hoy = Store.actividades.filter { dia in it.dias }.sortedBy { it.inicio }
    val actual = hoy.firstOrNull { minAhora >= it.inicio && minAhora < it.fin }
    val siguiente = hoy.firstOrNull { it.inicio > minAhora }
    val saludo = when (ahora.hour) {
        in 5..11 -> "Buenos días ☀️"
        in 12..18 -> "Buenas tardes 🌤️"
        else -> "Buenas noches 🌙"
    }
    val fecha = ahora.format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", LOCALE_ES)).capitalizar()
    val ym = YearMonth.from(ahora)
    val res = Store.resumen(ym)
    val mesTxt = ym.format(DateTimeFormatter.ofPattern("MMMM", LOCALE_ES)).capitalizar()

    LazyColumn {
        item { Encabezado(saludo, fecha) }

        item {
            Tarjeta("Ahora") {
                if (actual != null) {
                    BloqueActividad(actual.cat, actual.titulo, "${hhmm(actual.inicio)} – ${hhmm(actual.fin)}",
                        "Faltan ${actual.fin - minAhora} min para terminar")
                } else {
                    Text("Sin actividad programada en este momento.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(12.dp))
                Text("Siguiente", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                if (siguiente != null) {
                    val falta = siguiente.inicio - minAhora
                    val faltaTxt = if (falta >= 60) "En ${falta / 60} h ${falta % 60} min" else "En $falta min"
                    BloqueActividad(siguiente.cat, siguiente.titulo, "${hhmm(siguiente.inicio)} – ${hhmm(siguiente.fin)}", faltaTxt)
                } else {
                    Text("Nada más por hoy 🎉", fontWeight = FontWeight.Medium)
                }
            }
        }

        item {
            Tarjeta("Tu día de hoy") {
                val porCat = Cat.entries.associateWith { c -> hoy.filter { it.cat == c }.sumOf { it.fin - it.inicio } }
                    .filterValues { it > 0 }
                val total = porCat.values.sum()
                if (total == 0) {
                    Text("Hoy es día libre 😌")
                } else {
                    Row(Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(7.dp))) {
                        porCat.forEach { (c, m) ->
                            Box(Modifier.weight(m.toFloat()).fillMaxHeight().background(c.color))
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    porCat.forEach { (c, m) ->
                        Row(Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                            Punto(c.color)
                            Spacer(Modifier.width(8.dp))
                            Text("${c.emoji} ${c.label}", Modifier.weight(1f))
                            Text(horas(m / 60.0), fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }

        item {
            Tarjeta("Dinero de $mesTxt") {
                FilaDato("Ingresos", soles(res.ingresos), color = Verde)
                FilaDato("Gastos", soles(res.gastos), color = Rojo)
                FilaDato("Ahorrado", soles(res.ahorro), color = Morado)
                FilaDato("Disponible", soles(res.saldo), negrita = true)
                TextButton(onClick = { irA(2) }) { Text("Ver detalle →") }
            }
        }

        item {
            val pend = Store.pagos.filter { it.pagadoMes != ym.toString() && it.dia >= ahora.dayOfMonth }.sortedBy { it.dia }
            Tarjeta("Próximos pagos del mes") {
                if (pend.isEmpty()) Text("No tienes pagos pendientes este mes ✅")
                pend.take(4).forEach { p ->
                    Row(Modifier.fillMaxWidth().clickable { irA(2) }.padding(vertical = 4.dp)) {
                        Text("🧾 ${p.nombre}", Modifier.weight(1f))
                        Text("Día ${p.dia}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(10.dp))
                        Text(if (p.monto > 0) soles(p.monto) else "—", fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        item {
            val ingles = Store.habilidades.firstOrNull { it.nombre.startsWith("Ingl") }
            if (ingles != null) {
                Tarjeta(modifier = Modifier.clickable { irA(3) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🇬🇧", fontSize = 30.sp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Inglés", fontWeight = FontWeight.SemiBold)
                            Text("Racha: ${Store.rachaActual(ingles)} días 🔥", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("→", fontSize = 20.sp)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun BloqueActividad(cat: Cat, titulo: String, rango: String, extra: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(6.dp).height(48.dp).clip(RoundedCornerShape(3.dp)).background(cat.color))
        Spacer(Modifier.width(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("${cat.emoji} $titulo", fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
            Text(rango, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(extra, color = cat.color, fontWeight = FontWeight.Medium)
        }
    }
}
