package com.midia.organizador.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.midia.organizador.data.Habilidad
import com.midia.organizador.data.Store
import com.midia.organizador.data.horas
import java.time.LocalDate

@Composable
fun AprenderScreen() {
    var nueva by remember { mutableStateOf(false) }
    var editando by remember { mutableStateOf<Habilidad?>(null) }
    val ingles = Store.habilidades.firstOrNull { it.nombre.startsWith("Ingl") }
    val ruta = Store.habilidades.filter { it.id != ingles?.id }
    val hoy = LocalDate.now().toEpochDay()

    Box(Modifier.fillMaxSize()) {
        LazyColumn {
            item { Encabezado("Aprendizaje", "Softwares de tu carrera e inglés") }

            if (ingles != null) item {
                val racha = Store.rachaActual(ingles)
                val hecho = ingles.ultimoDia == hoy
                Tarjeta {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🇬🇧", fontSize = 34.sp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Inglés diario", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("${horas(ingles.horas)} acumuladas", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🔥 $racha", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            Text("días", fontSize = 12.sp)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = { Store.registrarTiempo(ingles, 0.5) }, enabled = !hecho, modifier = Modifier.fillMaxWidth()) {
                        Text(if (hecho) "✅ Hoy ya practicaste" else "Registré mis 30 min de hoy")
                    }
                    if (hecho) {
                        TextButton(onClick = { Store.registrarTiempo(ingles, 0.5) }) { Text("+30 min extra") }
                    }
                    Text("Tip: escucha podcasts en inglés en tus traslados.", fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            item {
                Text("Ruta de softwares", Modifier.padding(start = 20.dp, top = 12.dp, bottom = 2.dp), style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold)
            }

            itemsIndexed(ruta, key = { _, h -> h.id }) { i, h ->
                var prog by remember(h.id, h.progreso) { mutableFloatStateOf(h.progreso.toFloat()) }
                Tarjeta(modifier = Modifier.clickable { editando = h }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${i + 1}.", fontWeight = FontWeight.Bold, color = Morado, fontSize = 18.sp)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(h.nombre, fontWeight = FontWeight.SemiBold)
                            Text("${horas(h.horas)} practicadas" + if (h.ultimoDia == hoy) " · hoy ✅" else "",
                                fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("${prog.toInt()}%", fontWeight = FontWeight.Bold, color = Morado)
                    }
                    Slider(
                        value = prog,
                        onValueChange = { prog = it },
                        onValueChangeFinished = { Store.guardarHabilidad(h.copy(progreso = prog.toInt())) },
                        valueRange = 0f..100f,
                        steps = 19
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { Store.registrarTiempo(h, 0.5) }) { Text("+30 min") }
                        OutlinedButton(onClick = { Store.registrarTiempo(h, 1.0) }) { Text("+1 h") }
                        OutlinedButton(onClick = { Store.registrarTiempo(h, 2.0) }) { Text("+2 h") }
                    }
                }
            }
            item {
                Tarjeta {
                    Text("Total practicado: ${horas(Store.habilidades.sumOf { it.horas })}", fontWeight = FontWeight.SemiBold)
                    Text("Sesiones programadas: martes 8–10 pm y domingo 9–11 am.", fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(90.dp))
            }
        }
        Fab(Modifier.align(Alignment.BottomEnd)) { nueva = true }
    }

    if (nueva) HabilidadDialog(null) { nueva = false }
    editando?.let { HabilidadDialog(it) { editando = null } }
}

@Composable
private fun HabilidadDialog(inicial: Habilidad?, onCerrar: () -> Unit) {
    var nombre by remember { mutableStateOf(inicial?.nombre ?: "") }
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text(if (inicial == null) "Agregar a la ruta" else "Editar") },
        text = { CampoTexto(nombre, { nombre = it }, "Software o tema") },
        confirmButton = {
            TextButton(enabled = nombre.isNotBlank(), onClick = {
                Store.guardarHabilidad(inicial?.copy(nombre = nombre.trim()) ?: Habilidad(Store.nuevoId(), nombre.trim(), 0, 0.0, 0, -1))
                onCerrar()
            }) { Text("Guardar") }
        },
        dismissButton = {
            Row {
                if (inicial != null) TextButton(onClick = { Store.borrarHabilidad(inicial.id); onCerrar() }) { Text("Eliminar", color = Rojo) }
                TextButton(onClick = onCerrar) { Text("Cancelar") }
            }
        }
    )
}

