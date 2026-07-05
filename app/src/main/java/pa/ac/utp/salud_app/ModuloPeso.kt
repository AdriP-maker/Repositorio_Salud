package pa.ac.utp.salud_app

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.android.material.snackbar.Snackbar
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ModuloPeso : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_modulo_peso)

        val etPeso = findViewById<EditText>(R.id.etPeso)
        val etAltura = findViewById<EditText>(R.id.etAltura)
        val swPesoUnit = findViewById<androidx.appcompat.widget.SwitchCompat>(R.id.swPesoUnit)
        val swEstaturaUnit = findViewById<androidx.appcompat.widget.SwitchCompat>(R.id.swEstaturaUnit)
        val btnCalcular = findViewById<Button>(R.id.btnCalcular)
        
        val cardResult = findViewById<View>(R.id.cardResult)
        val tvImcResult = findViewById<TextView>(R.id.tvImcResult)
        val tvImcClasificacion = findViewById<TextView>(R.id.tvImcClasificacion)
        
        val lineChartPeso = findViewById<LineChart>(R.id.lineChartPeso)

        swPesoUnit.setOnCheckedChangeListener { _, isChecked ->
            etPeso.hint = if (isChecked) "Peso (Lb)" else "Peso (kg)"
            etPeso.text.clear()
        }

        swEstaturaUnit.setOnCheckedChangeListener { _, isChecked ->
            etAltura.hint = if (isChecked) "Altura (in)" else "Altura (m)"
            etAltura.text.clear()
        }

        val btnVerHistorial = findViewById<TextView>(R.id.btnVerHistorial)

        btnVerHistorial.setOnClickListener {
            mostrarBottomSheetHistorial()
        }

        btnCalcular.setOnClickListener { view ->
            val sPeso = etPeso.text.toString()
            val sEstatura = etAltura.text.toString()

            if (sPeso.isEmpty() || sEstatura.isEmpty()) {
                Snackbar.make(view, "Completa todos los campos", Snackbar.LENGTH_SHORT)
                    .setBackgroundTint(Color.RED)
                    .setTextColor(Color.WHITE)
                    .show()
                return@setOnClickListener
            }

            val pesoIngresado: Double
            val estaturaIngresada: Double
            try {
                pesoIngresado = sPeso.toDouble()
                estaturaIngresada = sEstatura.toDouble()
            } catch (e: NumberFormatException) {
                Snackbar.make(view, "Valores numéricos inválidos", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Convertir a Kg y Metros para el IMC y el Guardado
            val pesoKg = if (swPesoUnit.isChecked) pesoIngresado * 0.453592 else pesoIngresado
            val estaturaM = if (swEstaturaUnit.isChecked) estaturaIngresada * 0.0254 else estaturaIngresada

            val imc = pesoKg / (estaturaM * estaturaM)

            tvImcResult.text = String.format("IMC: %.1f", imc)
            tvImcClasificacion.text = categorizarIMC(imc)
            
            // Mostrar resultados
            cardResult.visibility = View.VISIBLE

            guardarRegistro(pesoKg, imc)
            cargarGrafico(lineChartPeso)
        }
        
        cargarGrafico(lineChartPeso)
    }

    private fun cargarGrafico(chart: LineChart) {
        val prefs = getSharedPreferences("salud_app_prefs", MODE_PRIVATE)
        val jsonStr = prefs.getString("historial_peso", "[]") ?: "[]"
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
            val peso = registro.getDouble("peso").toFloat()
            entries.add(Entry((i + 1).toFloat(), peso))
        }
        
        val dataSet = LineDataSet(entries, "Historial de Peso (kg)")
        dataSet.color = Color.parseColor("#1A73E8")
        dataSet.setCircleColor(Color.parseColor("#1A73E8"))
        dataSet.lineWidth = 3f
        dataSet.circleRadius = 5f
        dataSet.setDrawValues(false) // Ocultar números sobre los puntos

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
        val jsonStr = prefs.getString("historial_peso", "[]") ?: "[]"
        val array = JSONArray(jsonStr)

        val listaString = mutableListOf<String>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val fecha = obj.getString("fecha")
            val peso = obj.getDouble("peso")
            val imc = obj.getDouble("imc")
            listaString.add("Fecha: $fecha\nPeso: $peso kg | IMC: ${String.format("%.1f", imc)}")
        }

        if (listaString.isEmpty()) {
            listaString.add("Aún no hay registros.")
        }

        val adapter = android.widget.ArrayAdapter(this, android.R.layout.simple_list_item_1, listaString)
        lvHistorial.adapter = adapter

        bottomSheetDialog.show()
    }

    private fun guardarRegistro(pesoKg: Double, imc: Double) {
        val prefs = getSharedPreferences("salud_app_prefs", MODE_PRIVATE)
        val jsonStr = prefs.getString("historial_peso", "[]") ?: "[]"
        val array = JSONArray(jsonStr)
        val registro = JSONObject().apply {
            put("fecha", SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()))
            put("peso", pesoKg)
            put("imc", imc)
        }
        array.put(registro)
        prefs.edit().putString("historial_peso", array.toString()).apply()
    }

    private fun categorizarIMC(imc: Double): String {
        return when {
            imc < 18.5 -> "Bajo Peso"
            imc < 25.0 -> "Normal"
            imc < 30.0 -> "Sobrepeso"
            imc < 35.0 -> "Obesidad I"
            imc < 40.0 -> "Obesidad II"
            else       -> "Obesidad III"
        }
    }
}
