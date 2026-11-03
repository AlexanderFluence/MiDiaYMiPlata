@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.midia.organizador.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.midia.organizador.data.Actividad
import com.midia.organizador.data.Cat
import com.midia.organizador.data.DIAS_CORTOS
import com.midia.organizador.data.DIAS_LARGOS
import com.midia.organizador.data.Store
import com.midia.organizador.data.hhmm
import com.midia.organizador.data.horas
import java.time.LocalDate

@Composable
fun AgendaScreen() {
    var diaSel by rememberSaveable { mutableIntStateOf(LocalDate.now().dayOfWeek.value) }
    var editando by remember { mutableStateOf<Actividad?>(null) }
    var nueva by remember { mutableStateOf(false) }
    val delDia = Store.actividades.filter { diaSel in it.dias }.sortedBy { it.inicio }

    Box(Modifier.fillMaxSize()) {
        LazyColumn {
            item { Encabezado("Agenda", "Toca una actividad para editarla") }
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    DIAS_CORTOS.forEachIndexed { i, d ->
                        FilterChip(selected = diaSel == i + 1, onClick = { diaSel = i + 1 }, label = { Text(d) })
                    }
                }
            }
            item {
                Text(DIAS_LARGOS[diaSel - 1], Modifier.padding(start = 20.dp, top = 8.dp, bottom = 4.dp),
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            if (delDia.isEmpty()) {
                item { Tarjeta { Text("Día libre. Toca + para agregar algo.") } }
            }
            items(delDia, key = { it.id }) { a ->
                Tarjeta(modifier = Modifier.clickable { editando = a }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.width(6.dp).height(44.dp).clip(RoundedCornerShape(3.dp)).background(a.cat.color))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("${a.cat.emoji} ${a.titulo}", fontWeight = FontWeight.SemiBold)
                            Text("${hhmm(a.inicio)} – ${hhmm(a.fin)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(
                            when {
                                !a.alertas -> "🔕"
                                a.avisoAntes > 0 -> "🔔 ${a.avisoAntes}′"
                                else -> "🔔"
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            item {
                Tarjeta("Horas por semana") {
                    Cat.entries.forEach { c ->
                        val min = Store.actividades.filter { it.cat == c }.sumOf { (it.fin - it.inicio) * it.dias.size }
                        if (min > 0) {
                            Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                                Punto(c.color)
                                Spacer(Modifier.width(8.dp))
                                Text("${c.emoji} ${c.label}", Modifier.weight(1f))
                                Text(horas(min / 60.0), fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(90.dp))
            }
        }
        Fab(Modifier.align(Alignment.BottomEnd)) { nueva = true }
    }

    if (nueva) ActividadDialog(null, diaSel) { nueva = false }
    editando?.let { a -> ActividadDialog(a, diaSel) { editando = null } }
}

@Composable
fun ActividadDialog(inicial: Actividad?, diaDefecto: Int, onCerrar: () -> Unit) {
    val ctx = LocalContext.current
    var titulo by remember { mutableStateOf(inicial?.titulo ?: "") }
    var cat by remember { mutableStateOf(inicial?.cat ?: Cat.ESTUDIO) }
    var dias by remember { mutableStateOf(inicial?.dias ?: setOf(diaDefecto)) }
    var inicio by remember { mutableIntStateOf(inicial?.inicio ?: (9 * 60)) }
    var fin by remember { mutableIntStateOf(inicial?.fin ?: (10 * 60)) }
    var aviso by remember { mutableIntStateOf(inicial?.avisoAntes ?: 15) }
    var pausas by remember { mutableStateOf(inicial?.pausas ?: false) }
    var alertas by remember { mutableStateOf(inicial?.alertas ?: true) }
    val valido = titulo.isNotBlank() && dias.isNotEmpty() && fin > inicio

    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text(if (inicial == null) "Nueva actividad" else "Editar actividad") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                CampoTexto(titulo, { titulo = it }, "Nombre")
                Text("Categoría", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Cat.entries.forEach { c ->
                        FilterChip(selected = cat == c, onClick = { cat = c }, label = { Text("${c.emoji} ${c.label}") })
                    }
                }
                Text("Días", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    DIAS_CORTOS.forEachIndexed { i, d ->
                        val n = i + 1
                        FilterChip(selected = n in dias, onClick = { dias = if (n in dias) dias - n else dias + n }, label = { Text(d) })
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { elegirHora(ctx, inicio) { inicio = it } }, Modifier.weight(1f)) { Text("Inicio ${hhmm(inicio)}") }
                    OutlinedButton(onClick = { elegirHora(ctx, fin) { fin = it } }, Modifier.weight(1f)) { Text("Fin ${hhmm(fin)}") }
                }
                if (fin <= inicio) Text("La hora de fin debe ser después del inicio.", color = MaterialTheme.colorScheme.error)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Alertas activadas", Modifier.weight(1f))
                    Switch(checked = alertas, onCheckedChange = { alertas = it })
                }
                if (alertas) {
                    Text("Avisar antes", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(0, 5, 10, 15, 30, 60).forEach { m ->
                            FilterChip(selected = aviso == m, onClick = { aviso = m },
                                label = { Text(if (m == 0) "Solo al empezar" else "$m min") })
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Pausas cada hora")
                            Text("Te recuerda descansar 5 min", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = pausas, onCheckedChange = { pausas = it })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = valido, onClick = {
                Store.guardarActividad(
                    Actividad(inicial?.id ?: Store.nuevoId(), titulo.trim(), cat, dias, inicio, fin, aviso, pausas, alertas)
                )
                onCerrar()
            }) { Text("Guardar") }
        },
        dismissButton = {
            Row {
                if (inicial != null) {
                    TextButton(onClick = { Store.borrarActividad(inicial.id); onCerrar() }) {
                        Text("Eliminar", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onCerrar) { Text("Cancelar") }
            }
        }
    )
}
