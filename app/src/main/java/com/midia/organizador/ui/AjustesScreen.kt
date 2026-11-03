package com.midia.organizador.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.midia.organizador.alarmas.Scheduler
import com.midia.organizador.data.CATS_GASTO
import com.midia.organizador.data.Store
import com.midia.organizador.data.emojiGasto

private fun Double.limpio(): String = if (this % 1.0 == 0.0) this.toLong().toString() else this.toString()

@Composable
fun AjustesScreen() {
    val ctx = LocalContext.current
    val cfg = Store.config
    var sueldo by remember { mutableStateOf(cfg.sueldoFijo.limpio()) }
    var diaPago by remember { mutableStateOf(cfg.diaPago.toString()) }
    var pctFijo by remember { mutableStateOf(cfg.ahorroPctFijo.toString()) }
    var pctExtra by remember { mutableStateOf(cfg.ahorroPctExtra.toString()) }
    val pres = remember { CATS_GASTO.associateWith { mutableStateOf(cfg.presupuestos[it]?.limpio() ?: "") } }
    var confirmarBorrado by remember { mutableStateOf(false) }

    // Revisa permisos cada vez que vuelves a la app
    var refresco by remember { mutableIntStateOf(0) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) refresco++ }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }
    val notifOk = remember(refresco) { NotificationManagerCompat.from(ctx).areNotificationsEnabled() }
    val exactasOk = remember(refresco) { Scheduler.puedeExactas(ctx) }

    LazyColumn {
        item { Encabezado("Ajustes") }

        item {
            Tarjeta("🔔 Alertas") {
                FilaDato("Notificaciones", if (notifOk) "✅ Activadas" else "❌ Desactivadas", color = if (notifOk) Verde else Rojo)
                FilaDato("Alertas a la hora exacta", if (exactasOk) "✅ Permitidas" else "⚠️ Sin permiso", color = if (exactasOk) Verde else Rojo)
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (!notifOk) OutlinedButton(onClick = {
                        ctx.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName))
                    }, Modifier.fillMaxWidth()) { Text("Activar notificaciones") }
                    if (!exactasOk && Build.VERSION.SDK_INT >= 31) OutlinedButton(onClick = {
                        ctx.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${ctx.packageName}")))
                    }, Modifier.fillMaxWidth()) { Text("Permitir alertas exactas") }
                    OutlinedButton(onClick = {
                        ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}")))
                    }, Modifier.fillMaxWidth()) { Text("🔋 Batería: poner en \"Sin restricciones\"") }
                    FilledTonalButton(onClick = {
                        Scheduler.prueba(ctx)
                        Toast.makeText(ctx, "Llegará una alerta en 10 segundos", Toast.LENGTH_SHORT).show()
                    }, Modifier.fillMaxWidth()) { Text("Probar alerta (10 s)") }
                    FilledTonalButton(onClick = {
                        val n = Scheduler.programarTodo(ctx)
                        Toast.makeText(ctx, "$n alertas programadas", Toast.LENGTH_SHORT).show()
                    }, Modifier.fillMaxWidth()) { Text("Reprogramar todas las alertas") }
                }
                Spacer(Modifier.height(6.dp))
                Text("En Samsung, si las alertas no llegan con la app cerrada, entra a Batería y elige \"Sin restricciones\".",
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        item {
            Tarjeta("💵 Ingresos y ahorro") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CampoNumero(sueldo, { sueldo = it }, "Sueldo fijo mensual (S/)")
                    CampoNumero(diaPago, { diaPago = it.filter { c -> c.isDigit() }.take(2) }, "Día de cobro (1-31)")
                    CampoNumero(pctFijo, { pctFijo = it.filter { c -> c.isDigit() }.take(3) }, "% a ahorrar del sueldo fijo")
                    CampoNumero(pctExtra, { pctExtra = it.filter { c -> c.isDigit() }.take(3) }, "% a ahorrar de ingresos extra")
                }
            }
        }

        item {
            Tarjeta("📊 Presupuesto mensual por categoría") {
                Text("Déjalo vacío si no quieres límite.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CATS_GASTO.forEach { c ->
                        val st = pres.getValue(c)
                        CampoNumero(st.value, { st.value = it }, "${emojiGasto(c)} $c (S/)")
                    }
                }
            }
        }

        item {
            Button(onClick = {
                val d = diaPago.toIntOrNull()?.coerceIn(1, 31) ?: cfg.diaPago
                Store.guardarConfig(
                    cfg.copy(
                        sueldoFijo = sueldo.toDoubleOrNull() ?: cfg.sueldoFijo,
                        diaPago = d,
                        ahorroPctFijo = pctFijo.toIntOrNull()?.coerceIn(0, 100) ?: cfg.ahorroPctFijo,
                        ahorroPctExtra = pctExtra.toIntOrNull()?.coerceIn(0, 100) ?: cfg.ahorroPctExtra,
                        presupuestos = pres.mapValues { it.value.value.toDoubleOrNull() ?: 0.0 }.filterValues { it > 0 }
                    )
                )
                Toast.makeText(ctx, "Ajustes guardados", Toast.LENGTH_SHORT).show()
            }, Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) { Text("Guardar ajustes") }
        }

        item {
            Tarjeta("🗂️ Datos") {
                Text("Tus datos se guardan solo en este celular.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = { confirmarBorrado = true }) { Text("Borrar todo y volver al horario inicial", color = Rojo) }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (confirmarBorrado) {
        AlertDialog(
            onDismissRequest = { confirmarBorrado = false },
            title = { Text("¿Borrar todo?") },
            text = { Text("Se eliminarán tus movimientos, metas y cambios. No se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = { Store.reiniciar(); confirmarBorrado = false }) { Text("Borrar", color = Rojo) }
            },
            dismissButton = { TextButton(onClick = { confirmarBorrado = false }) { Text("Cancelar") } }
        )
    }
}

