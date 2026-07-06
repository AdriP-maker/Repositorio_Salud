package pa.ac.utp.salud_app

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import java.text.SimpleDateFormat
import java.util.*

class ModuloGlucosa : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper

    private lateinit var tvFechaHoraRegistro: TextView
    private lateinit var etGlucosa: EditText
    private lateinit var etNotas: EditText
    private lateinit var btnGuardar: Button
    private lateinit var tvResumenPrevio: TextView
    private lateinit var lineChartGlucosa: LineChart

    private var tipoSeleccionado: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_modulo_glucosa)

        dbHelper = DatabaseHelper.getInstance(this)

        tvFechaHoraRegistro = findViewById(R.id.tvFechaHoraRegistro)
        etGlucosa           = findViewById(R.id.etGlucosa)
        etNotas             = findViewById(R.id.etNotas)
        btnGuardar          = findViewById(R.id.btnGuardar)
        tvResumenPrevio     = findViewById(R.id.tvResumenPrevio)
        lineChartGlucosa    = findViewById(R.id.lineChartGlucosa)

        actualizarFechaHora()

        val rowAyuno = findViewById<TextView>(R.id.rowAyuno)
        val rowPreComida = findViewById<TextView>(R.id.rowPreComida)
        val rowPostComida = findViewById<TextView>(R.id.rowPostComida)
        val rowCena = findViewById<TextView>(R.id.rowCena)

        val filas = listOf(rowAyuno, rowPreComida, rowPostComida, rowCena)

        fun seleccionarFila(fila: TextView, tipo: String) {
            filas.forEach { it.setTextColor(Color.parseColor("#1E293B")) }
            fila.setTextColor(Color.parseColor("#8B5CF6")) // Acento Morado
            tipoSeleccionado = tipo
        }

        rowAyuno.setOnClickListener { seleccionarFila(rowAyuno, "Ayuno") }
        rowPreComida.setOnClickListener { seleccionarFila(rowPreComida, "Pre-Comida") }
        rowPostComida.setOnClickListener { seleccionarFila(rowPostComida, "Post-Comida") }
        rowCena.setOnClickListener { seleccionarFila(rowCena, "Cena") }

        val btnVerHistorialGlucosa = findViewById<TextView>(R.id.btnVerHistorialGlucosa)
        btnVerHistorialGlucosa.setOnClickListener {
            mostrarBottomSheetHistorial()
        }

        btnGuardar.setOnClickListener { guardarRegistro() }
        
        cargarUltimoRegistro()
        cargarGrafico(lineChartGlucosa)
    }

    private fun actualizarFechaHora() {
        val cal    = Calendar.getInstance()
        val locale = Locale("es", "PA")
        val dia    = SimpleDateFormat("EEEE", locale).format(cal.time)
            .replaceFirstChar { it.uppercase() }
        val fecha  = SimpleDateFormat("d 'de' MMMM", locale).format(cal.time)
        val hora   = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(cal.time)
        tvFechaHoraRegistro.text = "REGISTRO: Hoy - $dia, $fecha | $hora"
    }

    private fun cargarUltimoRegistro() {
        // SELECT * FROM glucosa ORDER BY id ASC (tomamos el último elemento)
        val historial = dbHelper.obtenerHistorialGlucosa()
        if (historial.isNotEmpty()) {
            val last = historial.last()
            tvResumenPrevio.text = "Último: ${last.glucosa} mg/dL (${last.tipo}) a las ${last.fecha}"
        }
    }

    private fun cargarGrafico(chart: LineChart) {
        // SELECT * FROM glucosa ORDER BY id ASC
        val historial = dbHelper.obtenerHistorialGlucosa()

        chart.setNoDataText("Aún no hay registros")
        chart.setNoDataTextColor(Color.parseColor("#94A3B8"))

        if (historial.isEmpty()) {
            chart.clear()
            return
        }

        val entries = ArrayList<Entry>()
        historial.forEachIndexed { i, registro ->
            entries.add(Entry((i + 1).toFloat(), registro.glucosa.toFloat()))
        }
        
        val dataSet = LineDataSet(entries, "Glucosa (mg/dL)")
        dataSet.color = Color.parseColor("#8B5CF6")
        dataSet.setCircleColor(Color.parseColor("#8B5CF6"))
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

    private fun mostrarBottomSheetHistorial() {
        val bottomSheetDialog = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.layout_bottom_sheet_historial, null)
        bottomSheetDialog.setContentView(view)

        val lvHistorial = view.findViewById<android.widget.ListView>(R.id.lvHistorial)

        // SELECT * FROM glucosa ORDER BY id ASC
        val historial = dbHelper.obtenerHistorialGlucosa()

        val listaString = mutableListOf<String>()
        for (registro in historial) {
            val notaTexto = if (!registro.notas.isNullOrBlank()) "\nNotas: ${registro.notas}" else ""
            listaString.add("Momento: ${registro.fecha}\nNivel: ${registro.glucosa} mg/dL (${registro.tipo})$notaTexto")
        }

        if (listaString.isEmpty()) {
            listaString.add("Aún no hay registros.")
        }

        val adapter = android.widget.ArrayAdapter(this, android.R.layout.simple_list_item_1, listaString)
        lvHistorial.adapter = adapter

        // Long-click para borrar (SQL: DELETE FROM glucosa WHERE id=?)
        lvHistorial.setOnItemLongClickListener { _, _, position, _ ->
            if (historial.isNotEmpty() && position < historial.size) {
                val registro = historial[position]
                AlertDialog.Builder(this)
                    .setTitle("Eliminar registro")
                    .setMessage("¿Eliminar el registro de las ${registro.fecha}?")
                    .setPositiveButton("Eliminar") { _, _ ->
                        dbHelper.eliminarGlucosa(registro.id)
                        Toast.makeText(this, "Registro eliminado", Toast.LENGTH_SHORT).show()
                        cargarGrafico(lineChartGlucosa)
                        cargarUltimoRegistro()
                        bottomSheetDialog.dismiss()
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
            true
        }

        bottomSheetDialog.show()
    }

    private fun guardarRegistro() {
        val valorGlucosaStr = etGlucosa.text.toString().trim()
        val notasTexto = etNotas.text.toString().trim()

        if (valorGlucosaStr.isEmpty()) {
            Toast.makeText(this, "Ingresa un valor de glucosa", Toast.LENGTH_SHORT).show()
            return
        }

        val valorGlucosa = valorGlucosaStr.toDoubleOrNull()
        if (valorGlucosa == null || valorGlucosa <= 0 || valorGlucosa > 600) {
            Toast.makeText(this, "Valor de glucosa inválido", Toast.LENGTH_SHORT).show()
            return
        }

        if (tipoSeleccionado.isEmpty()) {
            Toast.makeText(this, "Selecciona un momento (Ej: Ayuno)", Toast.LENGTH_SHORT).show()
            return
        }

        val horaActual = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
        tvResumenPrevio.text = "Último: $valorGlucosa mg/dL ($tipoSeleccionado) a las $horaActual"
        Toast.makeText(this, "Registro Guardado Exitosamente", Toast.LENGTH_SHORT).show()
        
        // INSERT INTO glucosa (fecha, glucosa, tipo, notas) VALUES (?, ?, ?, ?)
        dbHelper.insertarGlucosa(horaActual, valorGlucosa, tipoSeleccionado, notasTexto.ifEmpty { null })

        etGlucosa.setText("")
        etNotas.setText("")
        tipoSeleccionado = ""
        
        cargarGrafico(lineChartGlucosa)
    }
}
