package com.midia.organizador.alarmas

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.midia.organizador.data.Store
import com.midia.organizador.data.hhmm
import com.midia.organizador.data.soles
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId

/** Calcula y programa todas las alertas de la app en el AlarmManager de Android. */
object Scheduler {
    private const val ACCION = "com.midia.organizador.ALERTA"

    data class Alerta(val key: String, val titulo: String, val texto: String, val cuando: LocalDateTime)

    /** Próxima fecha/hora para un día de la semana (1 = lunes) y minuto del día. */
    private fun proximo(ahora: LocalDateTime, dia: Int, minuto: Int): LocalDateTime {
        var fecha = ahora.toLocalDate()
        repeat(15) {
            if (fecha.dayOfWeek.value == dia) {
                val t = fecha.atStartOfDay().plusMinutes(minuto.toLong())
                if (t.isAfter(ahora)) return t
            }
            fecha = fecha.plusDays(1)
        }
        return fecha.atStartOfDay().plusMinutes(minuto.toLong())
    }

    /** Próxima vez que llega un día del mes (si el mes no tiene ese día, usa el último). */
    private fun proximoDiaMes(ahora: LocalDateTime, dia: Int, minuto: Int, saltarMes: String? = null): LocalDateTime {
        var ym = YearMonth.from(ahora)
        repeat(4) {
            val t = ym.atDay(minOf(dia, ym.lengthOfMonth())).atStartOfDay().plusMinutes(minuto.toLong())
            if (t.isAfter(ahora) && ym.toString() != saltarMes) return t
            ym = ym.plusMonths(1)
        }
        return ym.atDay(1).atStartOfDay()
    }

    fun alertas(ahora: LocalDateTime): List<Alerta> {
        val out = mutableListOf<Alerta>()
        for (a in Store.actividades) {
            if (!a.alertas) continue
            val rango = "${hhmm(a.inicio)} – ${hhmm(a.fin)}"
            for (d in a.dias) {
                if (a.avisoAntes > 0) {
                    out += Alerta("a${a.id}-$d-pre", "${a.cat.emoji} En ${a.avisoAntes} min: ${a.titulo}",
                        rango, proximo(ahora, d, a.inicio - a.avisoAntes))
                }
                out += Alerta("a${a.id}-$d-ini", "${a.cat.emoji} Ahora: ${a.titulo}", rango, proximo(ahora, d, a.inicio))
                if (a.pausas) {
                    var m = a.inicio + 60
                    while (m < a.fin) {
                        val h = (m - a.inicio) / 60
                        out += Alerta("a${a.id}-$d-p$m", "🧘 Pausa de 5 minutos",
                            "Llevas $h h en ${a.titulo}. Estírate y toma agua.", proximo(ahora, d, m))
                        m += 60
                    }
                }
            }
        }
        val c = Store.config
        out += Alerta(
            "sueldo", "💰 ¡Día de pago!",
            "Registra tu sueldo y separa ${soles(c.sueldoFijo * c.ahorroPctFijo / 100)} para tu ahorro.",
            proximoDiaMes(ahora, c.diaPago, 8 * 60)
        )
        for (p in Store.pagos) {
            val f = proximoDiaMes(ahora, p.dia, 9 * 60, p.pagadoMes)
            val montoTxt = if (p.monto > 0) soles(p.monto) else "Revisa el monto en la app"
            out += Alerta("pf${p.id}-hoy", "🧾 Hoy vence: ${p.nombre}", montoTxt, f)
            val antes = f.minusDays(2)
            if (antes.isAfter(ahora)) out += Alerta("pf${p.id}-pre", "🧾 En 2 días vence: ${p.nombre}", montoTxt, antes)
        }
        return out
    }

    private fun pendiente(ctx: Context, code: Int, a: Alerta?): PendingIntent {
        val i = Intent(ctx, AlarmReceiver::class.java).setAction(ACCION)
        if (a != null) {
            i.putExtra("key", a.key).putExtra("titulo", a.titulo).putExtra("texto", a.texto)
        }
        return PendingIntent.getBroadcast(ctx, code, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun programar(ctx: Context, am: AlarmManager, a: Alerta) {
        val t = a.cuando.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val pi = pendiente(ctx, a.key.hashCode(), a)
        if (puedeExactas(ctx)) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t, pi)
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t, pi)
        }
    }

    fun puedeExactas(ctx: Context): Boolean {
        val am = ctx.getSystemService(AlarmManager::class.java)
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
    }

    /** Cancela todo y vuelve a programar. Devuelve cuántas alertas quedaron activas. */
    fun programarTodo(ctx: Context): Int {
        Store.cargar(ctx)
        val am = ctx.getSystemService(AlarmManager::class.java)
        val prefs = ctx.getSharedPreferences("alarmas", Context.MODE_PRIVATE)
        prefs.getStringSet("codes", emptySet())?.forEach { am.cancel(pendiente(ctx, it.toInt(), null)) }
        val lista = alertas(LocalDateTime.now())
        lista.forEach { programar(ctx, am, it) }
        prefs.edit().putStringSet("codes", lista.map { it.key.hashCode().toString() }.toSet()).apply()
        return lista.size
    }

    /** Tras sonar una alerta, programa su siguiente repetición. */
    fun reprogramarUna(ctx: Context, key: String) {
        Store.cargar(ctx)
        val a = alertas(LocalDateTime.now().plusSeconds(90)).firstOrNull { it.key == key } ?: return
        programar(ctx, ctx.getSystemService(AlarmManager::class.java), a)
    }

    fun prueba(ctx: Context) {
        programar(ctx, ctx.getSystemService(AlarmManager::class.java),
            Alerta("prueba", "✅ Prueba de alerta", "¡Las alertas funcionan en tu celular!", LocalDateTime.now().plusSeconds(10)))
    }
}
