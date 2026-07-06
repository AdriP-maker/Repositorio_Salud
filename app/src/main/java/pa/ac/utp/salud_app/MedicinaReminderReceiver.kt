package pa.ac.utp.salud_app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Se dispara cuando el AlarmManager llega a la hora programada de un medicamento.
 * Solo se encarga de mostrar la notificación; no toca la base de datos.
 */
class MedicinaReminderReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_MEDICINA_ID = "extra_medicina_id"
        const val EXTRA_NOMBRE = "extra_nombre"
        const val EXTRA_DOSIS = "extra_dosis"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val medicinaId = intent.getIntExtra(EXTRA_MEDICINA_ID, -1)
        val nombre = intent.getStringExtra(EXTRA_NOMBRE) ?: "Medicamento"
        val dosis = intent.getStringExtra(EXTRA_DOSIS) ?: ""

        if (medicinaId == -1) return

        NotificationHelper.crearCanalNotificaciones(context)
        NotificationHelper.mostrarNotificacionMedicina(context, medicinaId, nombre, dosis)
    }
}
