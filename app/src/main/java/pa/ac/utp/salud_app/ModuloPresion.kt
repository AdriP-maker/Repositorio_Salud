package pa.ac.utp.salud_app

import android.graphics.Color
import android.os.Bundle
import android.view.View
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
import java.util.Date
import java.util.Locale

class ModuloPresion : AppCompatActivity() {

    private lateinit var etSistolica: EditText
    private lateinit var etDiastolica: EditText
    private lateinit var etPulso: EditText
    private lateinit var btnSeleccionarFecha: Button
    private lateinit var btnAnalizar: Button
    private lateinit var cardResultado: View
    private lateinit var tvEstadoPresion: TextView
    private lateinit var tvConsejo: TextView
    private lateinit var lineChartPresion: LineChart

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_modulo_presion)

        etSistolica = findViewById(R.id.etSistolica)
        etDiastolica = findViewById(R.id.etDiastolica)
        etPulso = findViewById(R.id.etPulso)
        btnSeleccionarFecha = findViewById(R.id.btnSeleccionarFecha)
        btnAnalizar = findViewById(R.id.btnAnalizar)
        cardResultado = findViewById(R.id.cardResultado)
        tvEstadoPresion = findViewById(R.id.tvEstadoPresion)
        tvConsejo = findViewById(R.id.tvConsejo)
        lineChartPresion = findViewById(R.id.lineChartPresion)

        btnAnalizar.setOnClickListener {
            analizarPresion()
        }

        val btnVerHistorialPresion = findViewById<TextView>(R.id.btnVerHistorialPresion)
        btnVerHistorialPresion.setOnClickListener {
            mostrarBottomSheetHistorial()
        }

        cargarGrafico(lineChartPresion)
    }

    private fun cargarGrafico(chart: LineChart) {
        val prefs = getSharedPreferences("salud_app_prefs", MODE_PRIVATE)
        val jsonStr = prefs.getString("historial_presion", "[]") ?: "[]"
        val array = JSONArray(jsonStr)

        chart.setNoDataText("Aún no hay registros")
        chart.setNoDataTextColor(Color.parseColor("#94A3B8"))

        if (array.length() == 0) {
            chart.clear()
            return
        }

        val entries = ArrayList<Entry>()
        for (i in 0 until array.length()) {
            val registro = array.getJSONObject(i)
            val sistolica = registro.getDouble("sistolica").toFloat()
            entries.add(Entry((i + 1).toFloat(), sistolica))
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

        val (estado, color, consejo) = when {
            sis < 120 && dia < 80 -> Triple("Normal", "#10B981", "¡Excelente! Mantén tus hábitos.")
            sis in 120..129 && dia < 80 -> Triple("Elevada", "#F59E0B", "Atención, cuida tu dieta.")
            sis in 130..139 || dia in 80..89 -> Triple("Hipertensión Nivel 1", "#EF4444", "Consulta a tu médico.")
            sis >= 140 || dia >= 90 -> Triple("Hipertensión Nivel 2", "#B91C1C", "Requiere atención médica.")
            else -> Triple("Indeterminada", "#64748B", "Vuelve a medir.")
        }

        tvEstadoPresion.text = "Estado: $estado"
        tvEstadoPresion.setTextColor(Color.parseColor(color))
        tvConsejo.text = consejo
        cardResultado.visibility = View.VISIBLE

        guardarRegistro(sis, dia)
        cargarGrafico(lineChartPresion)
    }

    private fun mostrarBottomSheetHistorial() {
        val bottomSheetDialog = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.layout_bottom_sheet_historial, null)
        bottomSheetDialog.setContentView(view)

        val lvHistorial = view.findViewById<android.widget.ListView>(R.id.lvHistorial)
        
        val prefs = getSharedPreferences("salud_app_prefs", MODE_PRIVATE)
        val jsonStr = prefs.getString("historial_presion", "[]") ?: "[]"
        val array = JSONArray(jsonStr)

        val listaString = mutableListOf<String>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val fecha = obj.getString("fecha")
            val sistolica = obj.getInt("sistolica")
            val diastolica = obj.getInt("diastolica")
            listaString.add("Fecha: $fecha\nPresión: $sistolica / $diastolica mmHg")
        }

        if (listaString.isEmpty()) {
            listaString.add("Aún no hay registros.")
        }

        val adapter = android.widget.ArrayAdapter(this, android.R.layout.simple_list_item_1, listaString)
        lvHistorial.adapter = adapter

        bottomSheetDialog.show()
    }

    private fun guardarRegistro(sistolica: Int, diastolica: Int) {
        val prefs = getSharedPreferences("salud_app_prefs", MODE_PRIVATE)
        val jsonStr = prefs.getString("historial_presion", "[]") ?: "[]"
        val array = JSONArray(jsonStr)
        val registro = JSONObject().apply {
            put("fecha", SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date()))
            put("sistolica", sistolica)
            put("diastolica", diastolica)
        }
        array.put(registro)
        prefs.edit().putString("historial_presion", array.toString()).apply()
    }
}
