package pa.ac.utp.salud_app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * AlarmManager pierde todas las alarmas programadas cuando el teléfono se apaga.
 * Este receiver escucha BOOT_COMPLETED y vuelve a programar los recordatorios
 * de los medicamentos que el usuario dejó activos, leyendo la base de datos.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val dbHelper = DatabaseHelper.getInstance(context)
        // SELECT * FROM medicina WHERE recordatorio_activo = 1
        val medicamentosConRecordatorio = dbHelper.obtenerMedicinasConRecordatorioActivo()

        for (medicamento in medicamentosConRecordatorio) {
            RecordatorioScheduler.programar(
                context,
                medicamento.id,
                medicamento.nombre,
                medicamento.dosis,
                medicamento.horaRecordatorio ?: continue
            )
        }
    }
}
