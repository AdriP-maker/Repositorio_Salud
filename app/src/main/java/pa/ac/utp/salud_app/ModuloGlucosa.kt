package pa.ac.utp.salud_app

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
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class ModuloGlucosa : AppCompatActivity() {

    private lateinit var etGlucosa: EditText
    private lateinit var etNotas: EditText
    private lateinit var btnGuardar: Button
    private lateinit var tvResumenPrevio: TextView
    private lateinit var lineChartGlucosa: LineChart

    private var tipoSeleccionado: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_modulo_glucosa)

        etGlucosa           = findViewById(R.id.etGlucosa)
        etNotas             = findViewById(R.id.etNotas)
        btnGuardar          = findViewById(R.id.btnGuardar)
        tvResumenPrevio     = findViewById(R.id.tvResumenPrevio)
        lineChartGlucosa    = findViewById(R.id.lineChartGlucosa)
        
        val rowAyuno = findViewById<TextView>(R.id.rowAyuno)
        val rowPreComida = findViewById<TextView>(R.id.rowPreComida)
        val rowPostComida = findViewById<TextView>(R.id.rowPostComida)

        val filas = listOf(rowAyuno, rowPreComida, rowPostComida)

        fun seleccionarFila(fila: TextView, tipo: String) {
            filas.forEach { it.setTextColor(Color.parseColor("#1E293B")) }
            fila.setTextColor(Color.parseColor("#8B5CF6")) // Acento Morado
            tipoSeleccionado = tipo
        }

        rowAyuno.setOnClickListener { seleccionarFila(rowAyuno, "Ayuno") }
        rowPreComida.setOnClickListener { seleccionarFila(rowPreComida, "Pre-Comida") }
        rowPostComida.setOnClickListener { seleccionarFila(rowPostComida, "Post-Comida") }

        val btnVerHistorialGlucosa = findViewById<TextView>(R.id.btnVerHistorialGlucosa)
        btnVerHistorialGlucosa.setOnClickListener {
            mostrarBottomSheetHistorial()
        }

        btnGuardar.setOnClickListener { guardarRegistro() }
        
        cargarUltimoRegistro()
        cargarGrafico(lineChartGlucosa)
    }

    private fun cargarUltimoRegistro() {
        val prefs = getSharedPreferences("salud_app_prefs", MODE_PRIVATE)
        val jsonStr = prefs.getString("historial_glucosa", "[]") ?: "[]"
        val array = JSONArray(jsonStr)
        if (array.length() > 0) {
            val last = array.getJSONObject(array.length() - 1)
            tvResumenPrevio.text = "Último: ${last.getDouble("glucosa")} mg/dL (${last.getString("tipo")}) a las ${last.getString("fecha")}"
        }
    }

    private fun cargarGrafico(chart: LineChart) {
        val prefs = getSharedPreferences("salud_app_prefs", MODE_PRIVATE)
        val jsonStr = prefs.getString("historial_glucosa", "[]") ?: "[]"
        val array = JSONArray(jsonStr)

        chart.setNoDataText("Aún no hay registros")
        chart.setNoDataTextColor(Color.parseColor("#94A3B8"))

        if (array.length() == 0) {
            chart.clear()
            return
        }

        val entries = ArrayList<Entry>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val glucosa = obj.getDouble("glucosa").toFloat()
            entries.add(Entry((i + 1).toFloat(), glucosa))
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
        
        val prefs = getSharedPreferences("salud_app_prefs", MODE_PRIVATE)
        val jsonStr = prefs.getString("historial_glucosa", "[]") ?: "[]"
        val array = JSONArray(jsonStr)

        val listaString = mutableListOf<String>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val fecha = obj.getString("fecha")
            val glucosa = obj.getDouble("glucosa")
            val tipo = obj.getString("tipo")
            listaString.add("Momento: $fecha\nNivel: $glucosa mg/dL ($tipo)")
        }

        if (listaString.isEmpty()) {
            listaString.add("Aún no hay registros.")
        }

        val adapter = android.widget.ArrayAdapter(this, android.R.layout.simple_list_item_1, listaString)
        lvHistorial.adapter = adapter

        bottomSheetDialog.show()
    }

    private fun guardarRegistro() {
        val valorGlucosaStr = etGlucosa.text.toString().trim()

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
        
        // Guardar en SharedPreferences
        val prefs = getSharedPreferences("salud_app_prefs", MODE_PRIVATE)
        val jsonStr = prefs.getString("historial_glucosa", "[]") ?: "[]"
        val array = JSONArray(jsonStr)
        val registro = JSONObject().apply {
            put("fecha", horaActual)
            put("glucosa", valorGlucosa)
            put("tipo", tipoSeleccionado)
        }
        array.put(registro)
        prefs.edit().putString("historial_glucosa", array.toString()).apply()

        etGlucosa.setText("")
        etNotas.setText("")
        tipoSeleccionado = ""
        
        cargarGrafico(lineChartGlucosa)
    }
}
