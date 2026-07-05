package pa.ac.utp.salud_app

import android.graphics.Color
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import org.json.JSONArray
import org.json.JSONObject

class ModuloMedicina : AppCompatActivity() {

    private lateinit var etNombreMedicina: EditText
    private lateinit var etDosis: EditText
    private lateinit var etFrecuencia: EditText
    private lateinit var btnAnadirMedicina: Button
    private lateinit var lvMedicamentos: ListView
    private lateinit var barChartMedicina: BarChart

    private val listaMedicamentos = mutableListOf<String>()
    private lateinit var adapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_modulo_medicina)

        etNombreMedicina = findViewById(R.id.etNombreMedicina)
        etDosis = findViewById(R.id.etDosis)
        etFrecuencia = findViewById(R.id.etFrecuencia)
        btnAnadirMedicina = findViewById(R.id.btnAnadirMedicina)
        lvMedicamentos = findViewById(R.id.lvMedicamentos)
        barChartMedicina = findViewById(R.id.barChartMedicina)

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, listaMedicamentos)
        lvMedicamentos.adapter = adapter

        btnAnadirMedicina.setOnClickListener {
            anadirMedicamento()
        }

        cargarMedicamentos()
        cargarGrafico(barChartMedicina)
    }

    private fun cargarMedicamentos() {
        val prefs = getSharedPreferences("salud_app_prefs", MODE_PRIVATE)
        val jsonStr = prefs.getString("lista_medicina", "[]") ?: "[]"
        val array = JSONArray(jsonStr)
        listaMedicamentos.clear()
        
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val name = obj.getString("nombre")
            val dosis = obj.getString("dosis")
            val frec = obj.getString("frecuencia")
            listaMedicamentos.add("$name - $dosis ($frec)")
        }
        
        if (listaMedicamentos.isEmpty()) {
            listaMedicamentos.add("No hay medicamentos registrados.")
        }
        
        adapter.notifyDataSetChanged()
    }

    private fun anadirMedicamento() {
        val name = etNombreMedicina.text.toString().trim()
        val dosis = etDosis.text.toString().trim()
        val frec = etFrecuencia.text.toString().trim()

        if (name.isEmpty() || dosis.isEmpty() || frec.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        val prefs = getSharedPreferences("salud_app_prefs", MODE_PRIVATE)
        val jsonStr = prefs.getString("lista_medicina", "[]") ?: "[]"
        val array = JSONArray(jsonStr)
        val registro = JSONObject().apply {
            put("nombre", name)
            put("dosis", dosis)
            put("frecuencia", frec)
        }
        array.put(registro)
        prefs.edit().putString("lista_medicina", array.toString()).apply()

        etNombreMedicina.text.clear()
        etDosis.text.clear()
        etFrecuencia.text.clear()
        
        cargarMedicamentos()
        Toast.makeText(this, "Medicamento añadido", Toast.LENGTH_SHORT).show()
    }

    private fun cargarGrafico(chart: BarChart) {
        chart.setNoDataText("Aún no hay registros")
        chart.setNoDataTextColor(Color.parseColor("#94A3B8"))
        
        if (listaMedicamentos.isEmpty() || (listaMedicamentos.size == 1 && listaMedicamentos[0] == "No hay medicamentos registrados.")) {
            chart.clear()
            return
        }

        val entries = ArrayList<BarEntry>()
        // Demo data para adherencia pero basada en elementos reales
        for (i in 0 until listaMedicamentos.size) {
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
