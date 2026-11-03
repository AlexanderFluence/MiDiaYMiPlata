package com.midia.organizador.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.midia.organizador.alarmas.Scheduler
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.YearMonth

/** Guarda todos los datos en el celular (sin internet). */
object Store {
    private lateinit var prefs: SharedPreferences
    lateinit var app: Context
        private set
    private var cargado = false
    private var nextId = 1L

    val actividades = mutableStateListOf<Actividad>()
    val movimientos = mutableStateListOf<Movimiento>()
    val pagos = mutableStateListOf<PagoFijo>()
    val metas = mutableStateListOf<Meta>()
    val habilidades = mutableStateListOf<Habilidad>()
    var config by mutableStateOf(Config())
        private set

    fun nuevoId(): Long = nextId++

    fun cargar(ctx: Context) {
        if (cargado) return
        app = ctx.applicationContext
        prefs = app.getSharedPreferences("datos", Context.MODE_PRIVATE)
        val s = prefs.getString("data", null)
        if (s == null) {
            porDefecto()
            cargado = true
            guardar()
        } else {
            try {
                leer(JSONObject(s))
            } catch (e: Exception) {
                porDefecto()
            }
            cargado = true
        }
    }

    fun guardar() {
        prefs.edit().putString("data", aJson().toString()).apply()
    }

    private fun guardarYProgramar() {
        guardar()
        Scheduler.programarTodo(app)
    }

    private fun <T> upsert(lista: SnapshotStateList<T>, item: T, idDe: (T) -> Long) {
        val i = lista.indexOfFirst { idDe(it) == idDe(item) }
        if (i >= 0) lista[i] = item else lista.add(item)
    }

    // ---------- Agenda ----------
    fun guardarActividad(a: Actividad) { upsert(actividades, a) { it.id }; guardarYProgramar() }
    fun borrarActividad(id: Long) { actividades.removeAll { it.id == id }; guardarYProgramar() }

    // ---------- Dinero ----------
    fun agregarMov(m: Movimiento) { upsert(movimientos, m) { it.id }; guardar() }
    fun borrarMov(id: Long) { movimientos.removeAll { it.id == id }; guardar() }

    fun guardarPago(p: PagoFijo) { upsert(pagos, p) { it.id }; guardarYProgramar() }
    fun borrarPago(id: Long) { pagos.removeAll { it.id == id }; guardarYProgramar() }

    fun marcarPagado(p: PagoFijo) {
        agregarMov(Movimiento(nuevoId(), TipoMov.GASTO, p.cat, p.monto, LocalDate.now().toEpochDay(), p.nombre))
        guardarPago(p.copy(pagadoMes = YearMonth.now().toString()))
    }

    fun desmarcarPagado(p: PagoFijo) = guardarPago(p.copy(pagadoMes = null))

    fun guardarMeta(m: Meta) { upsert(metas, m) { it.id }; guardar() }
    fun borrarMeta(id: Long) { metas.removeAll { it.id == id }; guardar() }

    fun abonar(meta: Meta, monto: Double) {
        guardarMeta(meta.copy(ahorrado = meta.ahorrado + monto))
        agregarMov(Movimiento(nuevoId(), TipoMov.AHORRO, meta.nombre, monto, LocalDate.now().toEpochDay(), "Abono a ${meta.nombre}"))
    }

    fun movsDelMes(ym: YearMonth): List<Movimiento> =
        movimientos.filter { YearMonth.from(LocalDate.ofEpochDay(it.fecha)) == ym }

    fun resumen(ym: YearMonth): Resumen {
        val ms = movsDelMes(ym)
        return Resumen(
            ms.filter { it.tipo == TipoMov.INGRESO }.sumOf { it.monto },
            ms.filter { it.tipo == TipoMov.GASTO }.sumOf { it.monto },
            ms.filter { it.tipo == TipoMov.AHORRO }.sumOf { it.monto }
        )
    }

    fun guardarConfig(c: Config) { config = c; guardarYProgramar() }

    // ---------- Aprendizaje ----------
    fun guardarHabilidad(h: Habilidad) { upsert(habilidades, h) { it.id }; guardar() }
    fun borrarHabilidad(id: Long) { habilidades.removeAll { it.id == id }; guardar() }

    fun registrarTiempo(h: Habilidad, hrs: Double) {
        val hoy = LocalDate.now().toEpochDay()
        val racha = when (h.ultimoDia) {
            hoy -> h.racha
            hoy - 1 -> h.racha + 1
            else -> 1
        }
        guardarHabilidad(h.copy(horas = h.horas + hrs, racha = racha, ultimoDia = hoy))
    }

    fun rachaActual(h: Habilidad): Int =
        if (h.ultimoDia >= LocalDate.now().toEpochDay() - 1) h.racha else 0

    fun reiniciar() {
        actividades.clear(); movimientos.clear(); pagos.clear(); metas.clear(); habilidades.clear()
        config = Config()
        porDefecto()
        guardarYProgramar()
    }

    // ---------- Datos iniciales ----------
    private fun porDefecto() {
        nextId = 1
        val ls = (1..6).toSet()
        fun act(t: String, c: Cat, d: Set<Int>, i: Int, f: Int, av: Int, p: Boolean = false, al: Boolean = true) =
            Actividad(nuevoId(), t, c, d, i, f, av, p, al)
        actividades.addAll(
            listOf(
                act("Trabajo", Cat.TRABAJO, ls, 8 * 60, 16 * 60, 30),
                act("Descanso y almuerzo", Cat.DESCANSO, ls, 16 * 60, 17 * 60, 0),
                act("Inglés (30 min)", Cat.APRENDIZAJE, ls, 17 * 60, 17 * 60 + 30, 5),
                act("Tiempo libre y pendientes", Cat.DESCANSO, ls, 17 * 60 + 30, 19 * 60 + 30, 0, al = false),
                act("Clases EPE", Cat.ESTUDIO, setOf(1, 3, 4, 5), 20 * 60, 23 * 60, 15, p = true),
                act("Software: ruta de aprendizaje", Cat.APRENDIZAJE, setOf(2), 20 * 60, 22 * 60, 15),
                act("Práctica de software", Cat.APRENDIZAJE, setOf(7), 9 * 60, 11 * 60, 15),
                act("Repaso de la universidad", Cat.ESTUDIO, setOf(7), 15 * 60, 17 * 60, 10),
                act("Revisión de finanzas", Cat.OTRO, setOf(7), 17 * 60, 17 * 60 + 30, 0),
                act("Desconectarse y dormir", Cat.DESCANSO, setOf(1, 2, 3, 4, 5), 23 * 60 + 15, 23 * 60 + 30, 0)
            )
        )
        pagos.addAll(
            listOf(
                PagoFijo(nuevoId(), "Alquiler del departamento", "Departamento", 0.0, 5),
                PagoFijo(nuevoId(), "Internet y celular", "Servicios", 0.0, 10),
                PagoFijo(nuevoId(), "Pensión universidad", "Universidad", 0.0, 15),
                PagoFijo(nuevoId(), "Luz", "Servicios", 0.0, 20),
                PagoFijo(nuevoId(), "Agua", "Servicios", 0.0, 20)
            )
        )
        metas.add(Meta(nuevoId(), "Fondo de emergencia", 9000.0, 0.0))
        listOf(
            "Inglés", "AutoCAD", "Excel avanzado", "MATLAB", "S10", "Civil 3D",
            "Revit", "SAP2000 / ETABS", "MS Project"
        ).forEach { habilidades.add(Habilidad(nuevoId(), it, 0, 0.0, 0, -1)) }
    }

    // ---------- JSON ----------
    private fun JSONArray.objs(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }

    private fun aJson(): JSONObject {
        val o = JSONObject()
        o.put("nextId", nextId)
        o.put("actividades", JSONArray(actividades.map {
            JSONObject().put("id", it.id).put("titulo", it.titulo).put("cat", it.cat.name)
                .put("dias", JSONArray(it.dias.toList())).put("inicio", it.inicio).put("fin", it.fin)
                .put("aviso", it.avisoAntes).put("pausas", it.pausas).put("alertas", it.alertas)
        }))
        o.put("movimientos", JSONArray(movimientos.map {
            JSONObject().put("id", it.id).put("tipo", it.tipo.name).put("cat", it.cat)
                .put("monto", it.monto).put("fecha", it.fecha).put("nota", it.nota)
        }))
        o.put("pagos", JSONArray(pagos.map {
            JSONObject().put("id", it.id).put("nombre", it.nombre).put("cat", it.cat)
                .put("monto", it.monto).put("dia", it.dia).put("pagadoMes", it.pagadoMes ?: "")
        }))
        o.put("metas", JSONArray(metas.map {
            JSONObject().put("id", it.id).put("nombre", it.nombre).put("objetivo", it.objetivo).put("ahorrado", it.ahorrado)
        }))
        o.put("habilidades", JSONArray(habilidades.map {
            JSONObject().put("id", it.id).put("nombre", it.nombre).put("progreso", it.progreso)
                .put("horas", it.horas).put("racha", it.racha).put("ultimoDia", it.ultimoDia)
        }))
        val c = config
        val pres = JSONObject()
        c.presupuestos.forEach { (k, v) -> pres.put(k, v) }
        o.put("config", JSONObject().put("sueldoFijo", c.sueldoFijo).put("diaPago", c.diaPago)
            .put("pctFijo", c.ahorroPctFijo).put("pctExtra", c.ahorroPctExtra).put("presupuestos", pres))
        return o
    }

    private fun leer(o: JSONObject) {
        nextId = o.optLong("nextId", 1000)
        actividades.addAll(o.getJSONArray("actividades").objs().map { j ->
            val d = j.getJSONArray("dias")
            Actividad(
                j.getLong("id"), j.getString("titulo"), Cat.valueOf(j.getString("cat")),
                (0 until d.length()).map { d.getInt(it) }.toSet(),
                j.getInt("inicio"), j.getInt("fin"), j.getInt("aviso"),
                j.optBoolean("pausas", false), j.optBoolean("alertas", true)
            )
        })
        movimientos.addAll(o.getJSONArray("movimientos").objs().map { j ->
            Movimiento(j.getLong("id"), TipoMov.valueOf(j.getString("tipo")), j.getString("cat"),
                j.getDouble("monto"), j.getLong("fecha"), j.optString("nota", ""))
        })
        pagos.addAll(o.getJSONArray("pagos").objs().map { j ->
            PagoFijo(j.getLong("id"), j.getString("nombre"), j.getString("cat"), j.getDouble("monto"),
                j.getInt("dia"), j.optString("pagadoMes", "").ifEmpty { null })
        })
        metas.addAll(o.getJSONArray("metas").objs().map { j ->
            Meta(j.getLong("id"), j.getString("nombre"), j.getDouble("objetivo"), j.getDouble("ahorrado"))
        })
        habilidades.addAll(o.getJSONArray("habilidades").objs().map { j ->
            Habilidad(j.getLong("id"), j.getString("nombre"), j.getInt("progreso"), j.getDouble("horas"),
                j.getInt("racha"), j.getLong("ultimoDia"))
        })
        val c = o.getJSONObject("config")
        val p = c.getJSONObject("presupuestos")
        config = Config(
            c.getDouble("sueldoFijo"), c.getInt("diaPago"), c.getInt("pctFijo"), c.getInt("pctExtra"),
            p.keys().asSequence().associateWith { p.getDouble(it) }
        )
    }
}
