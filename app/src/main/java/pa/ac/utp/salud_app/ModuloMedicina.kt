package pa.ac.utp.salud_app

import android.Manifest
import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import java.util.Calendar

class ModuloMedicina : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper

    private lateinit var etNombreMedicina: EditText
    private lateinit var etDosis: EditText
    private lateinit var etFrecuencia: EditText
    private lateinit var swRecordatorio: SwitchMaterial
    private lateinit var btnHoraRecordatorio: MaterialButton
    private lateinit var btnAnadirMedicina: Button
    private lateinit var lvMedicamentos: ListView
    private lateinit var barChartMedicina: BarChart

    // Hora elegida en el TimePickerDialog, en formato "HH:mm" (null si no se ha elegido aún)
    private var horaSeleccionada: String? = null

    // Guarda los registros actualmente mostrados (con su id de BD) para poder
    // identificar cuál borrar al hacer long-click sobre la lista.
    private var medicamentosActuales: List<RegistroMedicina> = emptyList()
    private lateinit var adapter: ArrayAdapter<String>

    private val PERMISSION_REQUEST_NOTIFICACIONES = 200

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_modulo_medicina)

        dbHelper = DatabaseHelper.getInstance(this)
        NotificationHelper.crearCanalNotificaciones(this)
        pedirPermisoNotificacionesSiHaceFalta()

        etNombreMedicina = findViewById(R.id.etNombreMedicina)
        etDosis = findViewById(R.id.etDosis)
        etFrecuencia = findViewById(R.id.etFrecuencia)
        swRecordatorio = findViewById(R.id.swRecordatorio)
        btnHoraRecordatorio = findViewById(R.id.btnHoraRecordatorio)
        btnAnadirMedicina = findViewById(R.id.btnAnadirMedicina)
        lvMedicamentos = findViewById(R.id.lvMedicamentos)
        barChartMedicina = findViewById(R.id.barChartMedicina)

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf())
        lvMedicamentos.adapter = adapter

        // El botón de hora solo aparece si el usuario activa el recordatorio
        swRecordatorio.setOnCheckedChangeListener { _, activado ->
            btnHoraRecordatorio.visibility = if (activado) android.view.View.VISIBLE else android.view.View.GONE
            if (activado && horaSeleccionada == null) {
                mostrarSelectorDeHora()
            }
        }

        btnHoraRecordatorio.setOnClickListener {
            mostrarSelectorDeHora()
        }

        btnAnadirMedicina.setOnClickListener {
            anadirMedicamento()
        }

        // Long-click sobre un medicamento para eliminarlo (SQL: DELETE FROM medicina WHERE id=?)
        lvMedicamentos.setOnItemLongClickListener { _, _, position, _ ->
            if (medicamentosActuales.isNotEmpty() && position < medicamentosActuales.size) {
                val medicamento = medicamentosActuales[position]
                confirmarEliminacion(medicamento)
            }
            true
        }

        cargarMedicamentos()
    }

    private fun pedirPermisoNotificacionesSiHaceFalta() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    PERMISSION_REQUEST_NOTIFICACIONES
                )
            }
        }
    }

    private fun mostrarSelectorDeHora() {
        val calendar = Calendar.getInstance()
        val horaActual = calendar.get(Calendar.HOUR_OF_DAY)
        val minutoActual = calendar.get(Calendar.MINUTE)

        TimePickerDialog(this, { _, hora, minuto ->
            horaSeleccionada = "%02d:%02d".format(hora, minuto)
            btnHoraRecordatorio.text = "Recordatorio: $horaSeleccionada"
        }, horaActual, minutoActual, true).show()
    }

    private fun cargarMedicamentos() {
        // SELECT * FROM medicina ORDER BY id ASC
        medicamentosActuales = dbHelper.obtenerMedicinas()

        val textos = medicamentosActuales.map {
            val recordatorio = if (it.recordatorioActivo && it.horaRecordatorio != null) {
                " ⏰ ${it.horaRecordatorio}"
            } else {
                ""
            }
            "${it.nombre} - ${it.dosis} (${it.frecuencia})$recordatorio"
        }.toMutableList()

        if (textos.isEmpty()) {
            textos.add("No hay medicamentos registrados.")
        }

        adapter.clear()
        adapter.addAll(textos)
        adapter.notifyDataSetChanged()

        cargarGrafico(barChartMedicina)
    }

    private fun confirmarEliminacion(medicamento: RegistroMedicina) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar medicamento")
            .setMessage("¿Eliminar \"${medicamento.nombre}\" de tu lista?")
            .setPositiveButton("Eliminar") { _, _ ->
                dbHelper.eliminarMedicina(medicamento.id)
                // Si tenía un recordatorio programado, se cancela la alarma
                if (medicamento.recordatorioActivo) {
                    RecordatorioScheduler.cancelar(this, medicamento.id)
                }
                Toast.makeText(this, "Medicamento eliminado", Toast.LENGTH_SHORT).show()
                cargarMedicamentos()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun anadirMedicamento() {
        val name = etNombreMedicina.text.toString().trim()
        val dosis = etDosis.text.toString().trim()
        val frec = etFrecuencia.text.toString().trim()
        val recordatorioActivado = swRecordatorio.isChecked

        if (name.isEmpty() || dosis.isEmpty() || frec.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        if (recordatorioActivado && horaSeleccionada == null) {
            Toast.makeText(this, "Elige una hora para el recordatorio", Toast.LENGTH_SHORT).show()
            return
        }

        // INSERT INTO medicina (nombre, dosis, frecuencia, hora_recordatorio, recordatorio_activo) VALUES (?, ?, ?, ?, ?)
        val idInsertado = dbHelper.insertarMedicina(name, dosis, frec, horaSeleccionada, recordatorioActivado)

        if (recordatorioActivado && horaSeleccionada != null) {
            RecordatorioScheduler.programar(this, idInsertado.toInt(), name, dosis, horaSeleccionada!!)
            Toast.makeText(this, "Medicamento añadido, te avisaremos a las $horaSeleccionada", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "Medicamento añadido", Toast.LENGTH_SHORT).show()
        }

        etNombreMedicina.text.clear()
        etDosis.text.clear()
        etFrecuencia.text.clear()
        swRecordatorio.isChecked = false
        horaSeleccionada = null
        btnHoraRecordatorio.text = "Elegir hora"

        cargarMedicamentos()
    }

    private fun cargarGrafico(chart: BarChart) {
        chart.setNoDataText("Aún no hay registros")
        chart.setNoDataTextColor(Color.parseColor("#94A3B8"))

        if (medicamentosActuales.isEmpty()) {
            chart.clear()
            return
        }

        val entries = ArrayList<BarEntry>()
        // Demo data para adherencia pero basada en elementos reales
        for (i in medicamentosActuales.indices) {
            entries.add(BarEntry((i + 1).toFloat(), 100f))
        }

        val dataSet = BarDataSet(entries, "Adherencia (%)")
        dataSet.color = Color.parseColor("#F59E0B")
        dataSet.setDrawValues(false)

        val barData = BarData(dataSet)
        chart.data = barData
        chart.description.isEnabled = false
        chart.xAxis.setDrawGridLines(false)
        chart.axisRight.isEnabled = false
        chart.animateY(1000)
    }
}
