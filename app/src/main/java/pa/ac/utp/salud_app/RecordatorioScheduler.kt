package pa.ac.utp.salud_app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

/**
 * Encapsula el uso de AlarmManager para programar y cancelar los recordatorios
 * diarios de medicamentos. Se usa tanto desde ModuloMedicina (al crear/borrar un
 * medicamento) como desde BootReceiver (al reiniciar el teléfono).
 */
object RecordatorioScheduler {

    /**
     * Programa (o reprograma) una alarma diaria para el medicamento indicado.
     * @param horaMinuto hora en formato "HH:mm", ej. "08:30"
     */
    fun programar(context: Context, medicinaId: Int, nombre: String, dosis: String, horaMinuto: String) {
        val partes = horaMinuto.split(":")
        if (partes.size != 2) return
        val hora = partes[0].toIntOrNull() ?: return
        val minuto = partes[1].toIntOrNull() ?: return

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hora)
            set(Calendar.MINUTE, minuto)
            set(Calendar.SECOND, 0)
            // Si la hora de hoy ya pasó, se programa a partir de mañana
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = crearPendingIntent(context, medicinaId, nombre, dosis)

        // Alarma diaria repetitiva (inexacta), no requiere el permiso especial de alarmas exactas
        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    fun cancelar(context: Context, medicinaId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = crearPendingIntent(context, medicinaId, "", "")
        alarmManager.cancel(pendingIntent)
    }

    private fun crearPendingIntent(context: Context, medicinaId: Int, nombre: String, dosis: String): PendingIntent {
        val intent = Intent(context, MedicinaReminderReceiver::class.java).apply {
            putExtra(MedicinaReminderReceiver.EXTRA_MEDICINA_ID, medicinaId)
            putExtra(MedicinaReminderReceiver.EXTRA_NOMBRE, nombre)
            putExtra(MedicinaReminderReceiver.EXTRA_DOSIS, dosis)
        }
        // requestCode = medicinaId para que cada medicamento tenga su propia alarma independiente
        return PendingIntent.getBroadcast(
            context,
            medicinaId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
