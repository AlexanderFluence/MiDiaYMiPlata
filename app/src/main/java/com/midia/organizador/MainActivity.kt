package com.midia.organizador

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.midia.organizador.alarmas.Notif
import com.midia.organizador.alarmas.Scheduler
import com.midia.organizador.data.Store
import com.midia.organizador.ui.AgendaScreen
import com.midia.organizador.ui.AjustesScreen
import com.midia.organizador.ui.AppTheme
import com.midia.organizador.ui.AprenderScreen
import com.midia.organizador.ui.DineroScreen
import com.midia.organizador.ui.InicioScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Store.cargar(this)
        Notif.crearCanal(this)
        Scheduler.programarTodo(this)
        enableEdgeToEdge()
        setContent { AppTheme { App() } }
    }
}

@Composable
fun App() {
    val ctx = LocalContext.current
    val permiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) permiso.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf("🏠" to "Inicio", "📅" to "Agenda", "💰" to "Dinero", "📚" to "Aprender", "⚙️" to "Ajustes")
    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { i, (e, l) ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = { Text(e, fontSize = 20.sp) },
                        label = { Text(l) }
                    )
                }
            }
        }
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when (tab) {
                0 -> InicioScreen(irA = { tab = it })
                1 -> AgendaScreen()
                2 -> DineroScreen()
                3 -> AprenderScreen()
                else -> AjustesScreen()
            }
        }
    }
}
