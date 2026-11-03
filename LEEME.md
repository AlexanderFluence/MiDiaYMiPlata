# Mi Día y Mi Plata

App Android para organizar tu tiempo (trabajo, estudio, descanso y aprendizaje) con alertas, y tu dinero (ingresos, gastos, pagos fijos y ahorro).

## Qué trae ya cargado
- **Agenda:** trabajo L–S 8am–4pm, inglés 5–5:30pm, clases EPE L/X/J/V 8–11pm con pausas cada hora, software martes 8–10pm y domingo 9–11am, repaso y revisión de finanzas el domingo.
- **Dinero:** sueldo fijo S/ 3,600 cobrado el día 1, pagos fijos (alquiler, internet, universidad, luz y agua: edita los montos), gráfico de gastos y presupuestos.
- **Ahorro:** plan 10% del sueldo fijo + 50% de ingresos extra, meta "Fondo de emergencia" de S/ 9,000.
- **Aprendizaje:** inglés con racha diaria 🔥 y ruta AutoCAD → Excel → MATLAB → S10 → Civil 3D → Revit → SAP2000/ETABS → MS Project.

## Cómo obtener el .apk (con una computadora)
1. Instala **Android Studio** (gratis): https://developer.android.com/studio
2. Descomprime este .zip.
3. En Android Studio: **File > Open** y elige la carpeta `MiDiaYMiPlata`.
4. Espera a que termine "Gradle sync" (la primera vez descarga cosas, puede tardar 5–10 min). Si te sugiere actualizar algo, acepta.
5. Menú **Build > Build App Bundle(s) / APK(s) > Build APK(s)**.
6. Al terminar, toca **locate**: el archivo es `app/build/outputs/apk/debug/app-debug.apk`.
7. Pásalo a tu Samsung (cable, WhatsApp, Drive) y ábrelo. Si pide permiso para "instalar apps desconocidas", acéptalo.

También puedes conectar el celular por cable con la **depuración USB** activada y presionar ▶ Run en Android Studio.

## Alternativa sin computadora: GitHub
El proyecto incluye `.github/workflows/compilar-apk.yml`. Si subes la carpeta a un repositorio de GitHub, GitHub compila el APK solo: entra a la pestaña **Actions**, abre la última ejecución y descarga **MiDiaYMiPlata-apk**.

## Primera vez en el celular
1. Acepta el permiso de **notificaciones**.
2. Ve a **Ajustes** en la app y toca **Probar alerta (10 s)**.
3. En Samsung: Ajustes del teléfono > Aplicaciones > Mi Día y Mi Plata > **Batería > Sin restricciones**. Así las alertas llegan aunque la app esté cerrada.
4. En **Dinero > Pagos fijos**, toca cada pago y pon su monto real.
