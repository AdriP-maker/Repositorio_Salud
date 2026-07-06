package pa.ac.utp.salud_app

import android.app.AlertDialog
import android.app.TimePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.android.material.datepicker.MaterialDatePicker
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ModuloPresion : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper

    private lateinit var etSistolica: EditText
    private lateinit var etDiastolica: EditText
    private lateinit var etPulso: EditText
    private lateinit var rgBrazo: RadioGroup
    private lateinit var btnSeleccionarFecha: Button
    private lateinit var btnSeleccionarHora: Button
    private lateinit var btnAnalizar: Button
    private lateinit var cardResultado: View
    private lateinit var tvEstadoPresion: TextView
    private lateinit var tvConsejo: TextView
    private lateinit var lineChartPresion: LineChart

    // Extra (de primeraclase): permite registrar una medición con fecha/hora distinta a "ahora"
    private var fechaSeleccionada: String? = null
    private var horaSeleccionada: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_modulo_presion)

        dbHelper = DatabaseHelper.getInstance(this)

        etSistolica = findViewById(R.id.etSistolica)
        etDiastolica = findViewById(R.id.etDiastolica)
        etPulso = findViewById(R.id.etPulso)
        rgBrazo = findViewById(R.id.rgBrazo)
        btnSeleccionarFecha = findViewById(R.id.btnSeleccionarFecha)
        btnSeleccionarHora = findViewById(R.id.btnSeleccionarHora)
        btnAnalizar = findViewById(R.id.btnAnalizar)
        cardResultado = findViewById(R.id.cardResultado)
        tvEstadoPresion = findViewById(R.id.tvEstadoPresion)
        tvConsejo = findViewById(R.id.tvConsejo)
        lineChartPresion = findViewById(R.id.lineChartPresion)

        btnSeleccionarFecha.setOnClickListener { mostrarSelectorFecha() }
        btnSeleccionarHora.setOnClickListener { mostrarSelectorHora() }

        btnAnalizar.setOnClickListener {
            analizarPresion()
        }

        val btnVerHistorialPresion = findViewById<TextView>(R.id.btnVerHistorialPresion)
        btnVerHistorialPresion.setOnClickListener {
            mostrarBottomSheetHistorial()
        }

        cargarGrafico(lineChartPresion)
    }

    // ─── Extra: selección manual de fecha (MaterialDatePicker, igual que primeraclase) ────

    private fun mostrarSelectorFecha() {
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Seleccionar fecha de medición")
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            .build()

        picker.addOnPositiveButtonClickListener { seleccion ->
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            fechaSeleccionada = sdf.format(Date(seleccion))
            btnSeleccionarFecha.text = "Fecha: $fechaSeleccionada"
        }

        picker.show(supportFragmentManager, "MATERIAL_DATE_PICKER")
    }

    // ─── Extra: selección manual de hora (TimePickerDialog en formato 12h) ────────────────

    private fun mostrarSelectorHora() {
        val cal = Calendar.getInstance()
        TimePickerDialog(this, { _, hour, minute ->
            horaSeleccionada = formatearHora12(hour, minute)
            btnSeleccionarHora.text = "Hora: $horaSeleccionada"
        }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false).show()
    }

    private fun formatearHora12(hour: Int, minute: Int): String {
        val h12 = when { hour == 0 -> 12; hour > 12 -> hour - 12; else -> hour }
        val amPm = if (hour < 12) "AM" else "PM"
        return String.format("%02d:%02d %s", h12, minute, amPm)
    }

    private fun cargarGrafico(chart: LineChart) {
        // SELECT * FROM presion ORDER BY id ASC
        val historial = dbHelper.obtenerHistorialPresion()

        chart.setNoDataText("Aún no hay registros")
        chart.setNoDataTextColor(Color.parseColor("#94A3B8"))

        if (historial.isEmpty()) {
            chart.clear()
            return
        }

        val entries = ArrayList<Entry>()
        historial.forEachIndexed { i, registro ->
            entries.add(Entry((i + 1).toFloat(), registro.sistolica.toFloat()))
        }

        val dataSet = LineDataSet(entries, "Sistólica (mmHg)")
        dataSet.color = Color.parseColor("#EF4444")
        dataSet.setCircleColor(Color.parseColor("#EF4444"))
        dataSet.lineWidth = 3f
        dataSet.circleRadius = 5f
        dataSet.setDrawValues(false)

        val lineData = LineData(dataSet)
        chart.data = lineData
        chart.description.isEnabled = false
        chart.xAxis.setDrawGridLines(false)
        chart.axisRight.isEnabled = false
        chart.animateX(1000)
    }

    private fun analizarPresion() {
        val sisStr = etSistolica.text.toString()
        val diaStr = etDiastolica.text.toString()
        val pulsoStr = etPulso.text.toString()

        if (sisStr.isEmpty() || diaStr.isEmpty() || pulsoStr.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        val sis = sisStr.toIntOrNull() ?: 0
        val dia = diaStr.toIntOrNull() ?: 0
        val pulso = pulsoStr.toIntOrNull()

        val brazo = when (rgBrazo.checkedRadioButtonId) {
            R.id.rbBrazoDerecho -> "Brazo Derecho"
            else -> "Brazo Izquierdo"
        }

        // Extra (de primeraclase): categoría "Presión Baja" que salud_app no tenía
        val (estado, color, consejo) = when {
            sis < 90 || dia < 60 ->
                Triple("Presión Baja", "#3B82F6", "Manténgase hidratado y evite cambios bruscos de posición.")
            sis < 120 && dia < 80 ->
                Triple("Normal", "#10B981", "¡Excelente! Mantén tus hábitos.")
            sis in 120..129 && dia < 80 ->
                Triple("Elevada", "#F59E0B", "Atención, cuida tu dieta.")
            sis in 130..139 || dia in 80..89 ->
                Triple("Hipertensión Nivel 1", "#EF4444", "Consulta a tu médico.")
            sis >= 140 || dia >= 90 ->
                Triple("Hipertensión Nivel 2", "#B91C1C", "Requiere atención médica.")
            else ->
                Triple("Indeterminada", "#64748B", "Vuelve a medir.")
        }

        tvEstadoPresion.text = "Estado: $estado"
        tvEstadoPresion.setTextColor(Color.parseColor(color))
        tvConsejo.text = consejo
        cardResultado.visibility = View.VISIBLE

        guardarRegistro(sis, dia, pulso, brazo)
        cargarGrafico(lineChartPresion)

        // Se limpia la fecha/hora manual tras guardar para que el siguiente registro
        // vuelva a usar "ahora" por defecto, salvo que el usuario elija otra vez.
        fechaSeleccionada = null
        horaSeleccionada = null
        btnSeleccionarFecha.text = "Fecha: Hoy"
        btnSeleccionarHora.text = "Hora: Ahora"
    }

    private fun mostrarBottomSheetHistorial() {
        val bottomSheetDialog = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.layout_bottom_sheet_historial, null)
        bottomSheetDialog.setContentView(view)

        val lvHistorial = view.findViewById<android.widget.ListView>(R.id.lvHistorial)

        // SELECT * FROM presion ORDER BY id ASC
        val historial = dbHelper.obtenerHistorialPresion()

        val listaString = mutableListOf<String>()
        for (registro in historial) {
            val extra = buildString {
                if (registro.pulso != null) append(" | Pulso: ${registro.pulso} BPM")
                if (!registro.brazo.isNullOrBlank()) append(" | ${registro.brazo}")
            }
            listaString.add("Fecha: ${registro.fecha}\nPresión: ${registro.sistolica} / ${registro.diastolica} mmHg$extra")
        }

        if (listaString.isEmpty()) {
            listaString.add("Aún no hay registros.")
        }

        val adapter = android.widget.ArrayAdapter(this, android.R.layout.simple_list_item_1, listaString)
        lvHistorial.adapter = adapter

        // Long-click para borrar (SQL: DELETE FROM presion WHERE id=?)
        lvHistorial.setOnItemLongClickListener { _, _, position, _ ->
            if (historial.isNotEmpty() && position < historial.size) {
                val registro = historial[position]
                AlertDialog.Builder(this)
                    .setTitle("Eliminar registro")
                    .setMessage("¿Eliminar el registro del ${registro.fecha}?")
                    .setPositiveButton("Eliminar") { _, _ ->
                        dbHelper.eliminarPresion(registro.id)
                        Toast.makeText(this, "Registro eliminado", Toast.LENGTH_SHORT).show()
                        cargarGrafico(lineChartPresion)
                        bottomSheetDialog.dismiss()
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
            true
        }

        bottomSheetDialog.show()
    }

    private fun guardarRegistro(sistolica: Int, diastolica: Int, pulso: Int?, brazo: String) {
        // Si el usuario eligió fecha/hora manual, se usa esa; si no, la actual.
        val fecha = fechaSeleccionada ?: SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        val hora = horaSeleccionada ?: SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
        val fechaCompleta = "$fecha $hora"

        // INSERT INTO presion (fecha, sistolica, diastolica, pulso, brazo) VALUES (?, ?, ?, ?, ?)
        dbHelper.insertarPresion(fechaCompleta, sistolica, diastolica, pulso, brazo)
    }
}
