package pa.ac.utp.salud_app

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/**
 * Centraliza todo lo relacionado a notificaciones locales de recordatorio de medicamentos.
 */
object NotificationHelper {

    const val CANAL_MEDICAMENTOS = "canal_medicamentos"

    /** Debe llamarse una vez (Application/primera Activity) antes de mostrar notificaciones. */
    fun crearCanalNotificaciones(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CANAL_MEDICAMENTOS,
                "Recordatorios de medicamentos",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Avisos para tomar tus medicamentos a la hora indicada"
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    /** Devuelve true si la app tiene permiso para mostrar notificaciones (Android 13+). */
    fun tienePermisoNotificaciones(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // En versiones anteriores no se requiere permiso explícito
        }
    }

    /** Muestra la notificación de "hora de tomar tu medicamento". */
    fun mostrarNotificacionMedicina(context: Context, medicinaId: Int, nombre: String, dosis: String) {
        if (!tienePermisoNotificaciones(context)) return

        val notificacion = NotificationCompat.Builder(context, CANAL_MEDICAMENTOS)
            .setSmallIcon(R.drawable.ic_medicina)
            .setContentTitle("Hora de tu medicamento 💊")
            .setContentText("$nombre — $dosis")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$nombre — $dosis"))
            .build()

        NotificationManagerCompat.from(context).notify(medicinaId, notificacion)
    }
}
