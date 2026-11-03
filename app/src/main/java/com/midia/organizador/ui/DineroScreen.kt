@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.midia.organizador.ui

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.midia.organizador.data.CATS_GASTO
import com.midia.organizador.data.CATS_INGRESO
import com.midia.organizador.data.LOCALE_ES
import com.midia.organizador.data.Meta
import com.midia.organizador.data.Movimiento
import com.midia.organizador.data.PagoFijo
import com.midia.organizador.data.Store
import com.midia.organizador.data.TipoMov
import com.midia.organizador.data.colorGasto
import com.midia.organizador.data.emojiGasto
import com.midia.organizador.data.soles
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
fun DineroScreen() {
    var sub by rememberSaveable { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        Encabezado("Dinero y ahorro")
        TabRow(selectedTabIndex = sub) {
            listOf("Movimientos", "Pagos fijos", "Ahorro").forEachIndexed { i, t ->
                Tab(selected = sub == i, onClick = { sub = i }, text = { Text(t) })
            }
        }
        Box(Modifier.weight(1f)) {
            when (sub) {
                0 -> MovimientosTab()
                1 -> PagosTab()
                else -> AhorroTab()
            }
        }
    }
}

// ------------------------------------------------------------------ Movimientos

@Composable
private fun MovimientosTab() {
    var ym by remember { mutableStateOf(YearMonth.now()) }
    var nuevo by remember { mutableStateOf(false) }
    var borrar by remember { mutableStateOf<Movimiento?>(null) }
    var sugerencia by remember { mutableStateOf<Movimiento?>(null) }
    val res = Store.resumen(ym)
    val movs = Store.movsDelMes(ym).sortedWith(compareByDescending<Movimiento> { it.fecha }.thenByDescending { it.id })
    val gastosCat = CATS_GASTO.associateWith { c -> movs.filter { it.tipo == TipoMov.GASTO && it.cat == c }.sumOf { it.monto } }
    val cfg = Store.config

    Box(Modifier.fillMaxSize()) {
        LazyColumn {
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { ym = ym.minusMonths(1) }) { Text("◀") }
                    Text(ym.format(DateTimeFormatter.ofPattern("MMMM yyyy", LOCALE_ES)).capitalizar(),
                        Modifier.weight(1f), fontWeight = FontWeight.SemiBold, fontSize = 17.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    TextButton(onClick = { ym = ym.plusMonths(1) }) { Text("▶") }
                }
            }
            item {
                Tarjeta("Resumen del mes") {
                    FilaDato("Ingresos", soles(res.ingresos), color = Verde)
                    FilaDato("Gastos", soles(res.gastos), color = Rojo)
                    FilaDato("Enviado a ahorro", soles(res.ahorro), color = Morado)
                    FilaDato("Disponible", soles(res.saldo), negrita = true)
                    Spacer(Modifier.height(8.dp))
                    FilledTonalButton(onClick = {
                        val m = Movimiento(Store.nuevoId(), TipoMov.INGRESO, "Sueldo fijo", cfg.sueldoFijo, LocalDate.now().toEpochDay(), "Sueldo")
                        Store.agregarMov(m)
                        sugerencia = m
                    }, Modifier.fillMaxWidth()) { Text("💵 Registrar sueldo ${soles(cfg.sueldoFijo)}") }
                }
            }
            item {
                Tarjeta("¿En qué se va tu plata?") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Dona(gastosCat.filterValues { it > 0 }.map { colorGasto(it.key) to it.value }, Modifier.size(110.dp))
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            if (res.gastos == 0.0) Text("Aún no registras gastos este mes.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            gastosCat.filterValues { it > 0 }.forEach { (c, v) ->
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                                    Punto(colorGasto(c), 9)
                                    Spacer(Modifier.width(6.dp))
                                    Text(c, Modifier.weight(1f), fontSize = 13.sp)
                                    Text("${(v / res.gastos * 100).toInt()}%", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                    val conPres = CATS_GASTO.filter { (cfg.presupuestos[it] ?: 0.0) > 0 }
                    if (conPres.isNotEmpty()) {
                        Spacer(Modifier.height(14.dp))
                        Text("Presupuesto", style = MaterialTheme.typography.labelLarge)
                        conPres.forEach { c ->
                            val lim = cfg.presupuestos[c] ?: 0.0
                            val g = gastosCat[c] ?: 0.0
                            val frac = (g / lim).toFloat()
                            Spacer(Modifier.height(6.dp))
                            Row {
                                Text("${emojiGasto(c)} $c", Modifier.weight(1f), fontSize = 13.sp)
                                Text("${soles(g)} / ${soles(lim)}", fontSize = 13.sp,
                                    color = if (frac >= 1f) Rojo else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Barra(frac, if (frac >= 0.9f) Rojo else colorGasto(c))
                        }
                        if (conPres.any { (gastosCat[it] ?: 0.0) >= (cfg.presupuestos[it] ?: 0.0) * 0.9 }) {
                            Spacer(Modifier.height(6.dp))
                            Text("⚠️ Estás cerca o pasaste el límite en alguna categoría.", color = Rojo, fontSize = 13.sp)
                        }
                    }
                }
            }
            item {
                Text("Movimientos", Modifier.padding(start = 20.dp, top = 10.dp, bottom = 2.dp),
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            if (movs.isEmpty()) item { Tarjeta { Text("Sin movimientos. Toca + para registrar uno.") } }
            items(movs, key = { it.id }) { m ->
                val (signo, color) = when (m.tipo) {
                    TipoMov.INGRESO -> "+" to Verde
                    TipoMov.GASTO -> "−" to Rojo
                    TipoMov.AHORRO -> "→" to Morado
                }
                Tarjeta(modifier = Modifier.clickable { borrar = m }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (m.tipo == TipoMov.AHORRO) "🐷" else emojiGasto(m.cat), fontSize = 22.sp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(m.nota.ifBlank { m.cat }, fontWeight = FontWeight.Medium)
                            Text("${m.cat} · ${LocalDate.ofEpochDay(m.fecha).format(DateTimeFormatter.ofPattern("d MMM", LOCALE_ES))}",
                                fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("$signo ${soles(m.monto)}", color = color, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            item { Spacer(Modifier.height(90.dp)) }
        }
        Fab(Modifier.align(Alignment.BottomEnd)) { nuevo = true }
    }

    if (nuevo) MovimientoDialog(onCerrar = { nuevo = false }) { m -> if (m.tipo == TipoMov.INGRESO) sugerencia = m }
    borrar?.let { m ->
        AlertDialog(
            onDismissRequest = { borrar = null },
            title = { Text("¿Eliminar movimiento?") },
            text = { Text("${m.nota.ifBlank { m.cat }} · ${soles(m.monto)}" +
                    if (m.tipo == TipoMov.AHORRO) "\n\nOjo: el monto no se descuenta de la meta automáticamente; edítala en la pestaña Ahorro." else "") },
            confirmButton = { TextButton(onClick = { Store.borrarMov(m.id); borrar = null }) { Text("Eliminar", color = Rojo) } },
            dismissButton = { TextButton(onClick = { borrar = null }) { Text("Cancelar") } }
        )
    }
    sugerencia?.let { m -> SugerenciaAhorro(m) { sugerencia = null } }
}

@Composable
private fun SugerenciaAhorro(m: Movimiento, onCerrar: () -> Unit) {
    val cfg = Store.config
    val pct = if (m.cat == "Sueldo fijo") cfg.ahorroPctFijo else cfg.ahorroPctExtra
    val monto = m.monto * pct / 100
    var meta by remember { mutableStateOf(Store.metas.firstOrNull()) }
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("💡 ¡Ingreso registrado!") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Tu plan dice guardar el $pct% de este ingreso:")
                Text(soles(monto), fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Morado)
                if (Store.metas.isEmpty()) {
                    Text("Crea una meta en la pestaña Ahorro para poder guardar.")
                } else {
                    Text("¿En qué meta?", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Store.metas.forEach { mt ->
                            FilterChip(selected = meta?.id == mt.id, onClick = { meta = mt }, label = { Text(mt.nombre) })
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = meta != null, onClick = {
                val mt = Store.metas.firstOrNull { it.id == meta?.id }
                if (mt != null) Store.abonar(mt, monto)
                onCerrar()
            }) { Text("Guardar en ahorro") }
        },
        dismissButton = { TextButton(onClick = onCerrar) { Text("Ahora no") } }
    )
}

@Composable
private fun MovimientoDialog(onCerrar: () -> Unit, onGuardado: (Movimiento) -> Unit) {
    val ctx = LocalContext.current
    var tipo by remember { mutableStateOf(TipoMov.GASTO) }
    var cat by remember { mutableStateOf(CATS_GASTO.first()) }
    var monto by remember { mutableStateOf("") }
    var nota by remember { mutableStateOf("") }
    var fecha by remember { mutableStateOf(LocalDate.now()) }
    val valor = monto.toDoubleOrNull()

    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Nuevo movimiento") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(TipoMov.GASTO, TipoMov.INGRESO).forEach { t ->
                        FilterChip(selected = tipo == t, onClick = {
                            tipo = t
                            cat = if (t == TipoMov.INGRESO) CATS_INGRESO.first() else CATS_GASTO.first()
                        }, label = { Text(if (t == TipoMov.GASTO) "📤 Gasto" else "📥 Ingreso") })
                    }
                }
                Text("Categoría", style = MaterialTheme.typography.labelLarge)
                Chips(if (tipo == TipoMov.INGRESO) CATS_INGRESO else CATS_GASTO, cat) { cat = it }
                CampoNumero(monto, { monto = it }, "Monto (S/)")
                CampoTexto(nota, { nota = it }, "Descripción (opcional)")
                OutlinedButton(onClick = { elegirFecha(ctx, fecha) { fecha = it } }) {
                    Text("📆 ${if (fecha == LocalDate.now()) "Hoy" else fecha.format(DateTimeFormatter.ofPattern("d MMM yyyy", LOCALE_ES))}")
                }
            }
        },
        confirmButton = {
            TextButton(enabled = valor != null && valor > 0, onClick = {
                val m = Movimiento(Store.nuevoId(), tipo, cat, valor ?: 0.0, fecha.toEpochDay(), nota.trim())
                Store.agregarMov(m)
                onCerrar()
                onGuardado(m)
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onCerrar) { Text("Cancelar") } }
    )
}

// ------------------------------------------------------------------ Pagos fijos

@Composable
private fun PagosTab() {
    var editando by remember { mutableStateOf<PagoFijo?>(null) }
    var nuevo by remember { mutableStateOf(false) }
    val mes = YearMonth.now().toString()
    val lista = Store.pagos.sortedBy { it.dia }

    Box(Modifier.fillMaxSize()) {
        LazyColumn {
            item {
                Tarjeta {
                    FilaDato("Total de pagos fijos al mes", soles(lista.sumOf { it.monto }), negrita = true)
                    FilaDato("Pagados este mes", "${lista.count { it.pagadoMes == mes }} de ${lista.size}")
                    Text("Te aviso 2 días antes y el día del vencimiento. Toca un pago para editar el monto.",
                        fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            items(lista, key = { it.id }) { p ->
                val pagado = p.pagadoMes == mes
                Tarjeta(modifier = Modifier.clickable { editando = p }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(emojiGasto(p.cat), fontSize = 22.sp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(p.nombre, fontWeight = FontWeight.SemiBold)
                            Text("Vence el día ${p.dia} · ${if (p.monto > 0) soles(p.monto) else "Monto por definir"}",
                                fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (pagado) {
                            TextButton(onClick = { Store.desmarcarPagado(p) }) { Text("✅ Pagado", color = Verde) }
                        } else {
                            Button(onClick = { Store.marcarPagado(p) }, enabled = p.monto > 0) { Text("Pagar") }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(90.dp)) }
        }
        Fab(Modifier.align(Alignment.BottomEnd)) { nuevo = true }
    }
    if (nuevo) PagoDialog(null) { nuevo = false }
    editando?.let { PagoDialog(it) { editando = null } }
}

@Composable
private fun PagoDialog(inicial: PagoFijo?, onCerrar: () -> Unit) {
    var nombre by remember { mutableStateOf(inicial?.nombre ?: "") }
    var cat by remember { mutableStateOf(inicial?.cat ?: "Servicios") }
    var monto by remember { mutableStateOf(inicial?.monto?.takeIf { it > 0 }?.toString() ?: "") }
    var dia by remember { mutableStateOf(inicial?.dia?.toString() ?: "") }
    val d = dia.toIntOrNull()
    val valido = nombre.isNotBlank() && d != null && d in 1..31

    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text(if (inicial == null) "Nuevo pago fijo" else "Editar pago fijo") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                CampoTexto(nombre, { nombre = it }, "Nombre (ej. Alquiler)")
                Text("Categoría", style = MaterialTheme.typography.labelLarge)
                Chips(CATS_GASTO, cat) { cat = it }
                CampoNumero(monto, { monto = it }, "Monto mensual (S/)")
                CampoNumero(dia, { dia = it.filter { c -> c.isDigit() }.take(2) }, "Día de vencimiento (1-31)")
            }
        },
        confirmButton = {
            TextButton(enabled = valido, onClick = {
                Store.guardarPago(
                    PagoFijo(inicial?.id ?: Store.nuevoId(), nombre.trim(), cat, monto.toDoubleOrNull() ?: 0.0, d ?: 1, inicial?.pagadoMes)
                )
                onCerrar()
            }) { Text("Guardar") }
        },
        dismissButton = {
            Row {
                if (inicial != null) TextButton(onClick = { Store.borrarPago(inicial.id); onCerrar() }) { Text("Eliminar", color = Rojo) }
                TextButton(onClick = onCerrar) { Text("Cancelar") }
            }
        }
    )
}

// ------------------------------------------------------------------ Ahorro

@Composable
private fun AhorroTab() {
    var nueva by remember { mutableStateOf(false) }
    var editando by remember { mutableStateOf<Meta?>(null) }
    var abonando by remember { mutableStateOf<Meta?>(null) }
    val cfg = Store.config
    val ym = YearMonth.now()
    val movsMes = Store.movsDelMes(ym)
    val extrasMes = movsMes.filter { it.tipo == TipoMov.INGRESO && it.cat == "Ingreso extra" }.sumOf { it.monto }
    val objetivoMes = cfg.sueldoFijo * cfg.ahorroPctFijo / 100 + extrasMes * cfg.ahorroPctExtra / 100
    val ahorradoMes = movsMes.filter { it.tipo == TipoMov.AHORRO }.sumOf { it.monto }

    Box(Modifier.fillMaxSize()) {
        LazyColumn {
            item {
                Tarjeta("Tu plan de ahorro") {
                    FilaDato("${cfg.ahorroPctFijo}% del sueldo fijo", soles(cfg.sueldoFijo * cfg.ahorroPctFijo / 100))
                    FilaDato("${cfg.ahorroPctExtra}% de extras del mes (${soles(extrasMes)})", soles(extrasMes * cfg.ahorroPctExtra / 100))
                    FilaDato("Meta de este mes", soles(objetivoMes), negrita = true)
                    Spacer(Modifier.height(8.dp))
                    Barra(if (objetivoMes > 0) (ahorradoMes / objetivoMes).toFloat() else 0f, Morado)
                    Spacer(Modifier.height(4.dp))
                    Text("Llevas ${soles(ahorradoMes)} este mes" + if (ahorradoMes >= objetivoMes && objetivoMes > 0) " 🎉 ¡Cumpliste!" else "",
                        fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item {
                Tarjeta {
                    FilaDato("Total ahorrado en metas", soles(Store.metas.sumOf { it.ahorrado }), color = Morado, negrita = true)
                }
            }
            items(Store.metas.toList(), key = { it.id }) { m ->
                val frac = if (m.objetivo > 0) (m.ahorrado / m.objetivo).toFloat() else 0f
                Tarjeta(modifier = Modifier.clickable { editando = m }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (frac >= 1f) "🏆" else "🐷", fontSize = 24.sp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(m.nombre, fontWeight = FontWeight.SemiBold)
                            Text("${soles(m.ahorrado)} de ${soles(m.objetivo)}", fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("${(frac * 100).toInt().coerceAtMost(999)}%", fontWeight = FontWeight.Bold, color = Morado)
                    }
                    Spacer(Modifier.height(8.dp))
                    Barra(frac, Morado)
                    if (objetivoMes > 0 && m.ahorrado < m.objetivo) {
                        val meses = kotlin.math.ceil((m.objetivo - m.ahorrado) / objetivoMes).toInt()
                        Text("A tu ritmo del plan, la logras en unos $meses meses.", fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                    }
                    Spacer(Modifier.height(6.dp))
                    FilledTonalButton(onClick = { abonando = m }, Modifier.fillMaxWidth()) { Text("➕ Abonar") }
                }
            }
            item { Spacer(Modifier.height(90.dp)) }
        }
        Fab(Modifier.align(Alignment.BottomEnd)) { nueva = true }
    }

    if (nueva) MetaDialog(null) { nueva = false }
    editando?.let { MetaDialog(it) { editando = null } }
    abonando?.let { m ->
        var monto by remember(m.id) { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { abonando = null },
            title = { Text("Abonar a ${m.nombre}") },
            text = { CampoNumero(monto, { monto = it }, "Monto (S/)") },
            confirmButton = {
                TextButton(enabled = (monto.toDoubleOrNull() ?: 0.0) > 0, onClick = {
                    Store.abonar(m, monto.toDouble()); abonando = null
                }) { Text("Abonar") }
            },
            dismissButton = { TextButton(onClick = { abonando = null }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun MetaDialog(inicial: Meta?, onCerrar: () -> Unit) {
    var nombre by remember { mutableStateOf(inicial?.nombre ?: "") }
    var objetivo by remember { mutableStateOf(inicial?.objetivo?.toString() ?: "") }
    var ahorrado by remember { mutableStateOf(inicial?.ahorrado?.toString() ?: "0") }
    val obj = objetivo.toDoubleOrNull()
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text(if (inicial == null) "Nueva meta" else "Editar meta") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                CampoTexto(nombre, { nombre = it }, "Nombre (ej. Viaje, Graduación)")
                CampoNumero(objetivo, { objetivo = it }, "Monto objetivo (S/)")
                CampoNumero(ahorrado, { ahorrado = it }, "Ya ahorrado (S/)")
            }
        },
        confirmButton = {
            TextButton(enabled = nombre.isNotBlank() && obj != null && obj > 0, onClick = {
                Store.guardarMeta(Meta(inicial?.id ?: Store.nuevoId(), nombre.trim(), obj ?: 0.0, ahorrado.toDoubleOrNull() ?: 0.0))
                onCerrar()
            }) { Text("Guardar") }
        },
        dismissButton = {
            Row {
                if (inicial != null) TextButton(onClick = { Store.borrarMeta(inicial.id); onCerrar() }) { Text("Eliminar", color = Rojo) }
                TextButton(onClick = onCerrar) { Text("Cancelar") }
            }
        }
    )
}
