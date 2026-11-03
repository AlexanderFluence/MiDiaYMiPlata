@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.midia.organizador.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate

private val Claro = lightColorScheme(
    primary = Color(0xFF0F766E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCCFBF1),
    onPrimaryContainer = Color(0xFF042F2E),
    secondary = Color(0xFF8B5CF6),
    background = Color(0xFFF6F7F9),
    surface = Color(0xFFF6F7F9),
    surfaceContainer = Color.White,
    surfaceContainerHighest = Color.White,
    error = Color(0xFFDC2626)
)

private val Oscuro = darkColorScheme(
    primary = Color(0xFF5EEAD4),
    onPrimary = Color(0xFF042F2E),
    primaryContainer = Color(0xFF115E59),
    onPrimaryContainer = Color(0xFFCCFBF1),
    secondary = Color(0xFFC4B5FD),
    background = Color(0xFF0F1115),
    surface = Color(0xFF0F1115),
    surfaceContainer = Color(0xFF1A1D23),
    surfaceContainerHighest = Color(0xFF1A1D23),
    error = Color(0xFFF87171)
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Oscuro else Claro, content = content)
}

@Composable
fun Encabezado(titulo: String, sub: String? = null) {
    Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp)) {
        Text(titulo, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        if (sub != null) Text(sub, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun Tarjeta(titulo: String? = null, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            if (titulo != null) {
                Text(titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(10.dp))
            }
            content()
        }
    }
}

@Composable
fun Fab(modifier: Modifier = Modifier, onClick: () -> Unit) {
    FloatingActionButton(onClick = onClick, modifier = modifier.padding(16.dp)) {
        Text("+", fontSize = 28.sp)
    }
}

@Composable
fun Punto(color: Color, tam: Int = 10) {
    Box(Modifier.size(tam.dp).clip(CircleShape).background(color))
}

@Composable
fun Barra(progreso: Float, color: Color, modifier: Modifier = Modifier) {
    LinearProgressIndicator(
        progress = { progreso.coerceIn(0f, 1f) },
        modifier = modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
        color = color,
        trackColor = color.copy(alpha = 0.18f),
        drawStopIndicator = {}
    )
}

@Composable
fun CampoNumero(valor: String, onChange: (String) -> Unit, label: String, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = valor,
        onValueChange = { t -> onChange(t.filter { it.isDigit() || it == '.' }) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
fun CampoTexto(valor: String, onChange: (String) -> Unit, label: String) {
    OutlinedTextField(value = valor, onValueChange = onChange, label = { Text(label) }, singleLine = true, modifier = Modifier.fillMaxWidth())
}

@Composable
fun Chips(opciones: List<String>, seleccion: String, onSel: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        opciones.forEach { o -> FilterChip(selected = o == seleccion, onClick = { onSel(o) }, label = { Text(o) }) }
    }
}

@Composable
fun FilaDato(etiqueta: String, valor: String, color: Color = MaterialTheme.colorScheme.onSurface, negrita: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(etiqueta, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(valor, color = color, fontWeight = if (negrita) FontWeight.Bold else FontWeight.Medium)
    }
}

@Composable
fun Dona(partes: List<Pair<Color, Double>>, modifier: Modifier = Modifier) {
    val total = partes.sumOf { it.second }
    val vacio = MaterialTheme.colorScheme.outlineVariant
    Canvas(modifier) {
        val grosor = size.minDimension * 0.2f
        val diam = size.minDimension - grosor
        val esquina = Offset((size.width - diam) / 2f, (size.height - diam) / 2f)
        val tam = Size(diam, diam)
        if (total <= 0.0) {
            drawArc(vacio, 0f, 360f, false, esquina, tam, style = Stroke(grosor))
        } else {
            var inicio = -90f
            partes.forEach { (c, v) ->
                val barrido = (v / total * 360.0).toFloat()
                drawArc(c, inicio, barrido, false, esquina, tam, style = Stroke(grosor))
                inicio += barrido
            }
        }
    }
}

fun elegirHora(ctx: Context, inicial: Int, onOk: (Int) -> Unit) {
    TimePickerDialog(ctx, { _, h, m -> onOk(h * 60 + m) }, inicial / 60, inicial % 60, false).show()
}

fun elegirFecha(ctx: Context, inicial: LocalDate, onOk: (LocalDate) -> Unit) {
    DatePickerDialog(ctx, { _, y, mo, d -> onOk(LocalDate.of(y, mo + 1, d)) }, inicial.year, inicial.monthValue - 1, inicial.dayOfMonth).show()
}

fun String.capitalizar(): String = replaceFirstChar { it.uppercase() }

val Verde = Color(0xFF16A34A)
val Rojo = Color(0xFFDC2626)
val Morado = Color(0xFF8B5CF6)
