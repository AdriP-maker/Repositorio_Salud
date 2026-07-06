package pa.ac.utp.salud_app

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ModuloPeso : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var sesionManager: SesionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_modulo_peso)

        dbHelper = DatabaseHelper.getInstance(this)
        sesionManager = SesionManager(this)

        val etEdad = findViewById<EditText>(R.id.etEdad)
        val etPeso = findViewById<EditText>(R.id.etPeso)
        val etAltura = findViewById<EditText>(R.id.etAltura)
        val swPesoUnit = findViewById<androidx.appcompat.widget.SwitchCompat>(R.id.swPesoUnit)
        val swEstaturaUnit = findViewById<androidx.appcompat.widget.SwitchCompat>(R.id.swEstaturaUnit)
        val btnCalcular = findViewById<Button>(R.id.btnCalcular)
        
        val cardResult = findViewById<View>(R.id.cardResult)
        val tvImcResult = findViewById<TextView>(R.id.tvImcResult)
        val tvImcClasificacion = findViewById<TextView>(R.id.tvImcClasificacion)
        val tvPesoIdeal = findViewById<TextView>(R.id.tvPesoIdeal)
        val tvGrasa = findViewById<TextView>(R.id.tvGrasa)
        val tvConsejoPeso = findViewById<TextView>(R.id.tvConsejoPeso)
        
        val lineChartPeso = findViewById<LineChart>(R.id.lineChartPeso)

        fun updatePesoVisual() {
            val isLbs = swPesoUnit.isChecked
            val pesoKg = sesionManager.obtenerPeso()
            if (isLbs) {
                etPeso.setText(String.format(java.util.Locale.US, "%.1f", pesoKg / 0.453592f))
            } else {
                etPeso.setText(String.format(java.util.Locale.US, "%.1f", pesoKg))
            }
        }

        fun updateAlturaVisual() {
            val isIn = swEstaturaUnit.isChecked
            val alturaCm = sesionManager.obtenerAltura()
            if (isIn) {
                etAltura.setText(String.format(java.util.Locale.US, "%.1f", alturaCm / 2.54f))
            } else {
                etAltura.setText(String.format(java.util.Locale.US, "%.1f", alturaCm))
            }
        }

        // Pre-cargar datos desde SesionManager
        etEdad.setText(sesionManager.obtenerEdad().toString())
        updatePesoVisual()
        updateAlturaVisual()

        swPesoUnit.setOnCheckedChangeListener { _, isChecked ->
            etPeso.hint = if (isChecked) "Peso (Lb)" else "Peso (kg)"
            updatePesoVisual()
        }

        swEstaturaUnit.setOnCheckedChangeListener { _, isChecked ->
            etAltura.hint = if (isChecked) "Altura (in)" else "Altura (cm)"
            updateAlturaVisual()
        }

        val btnVerHistorial = findViewById<TextView>(R.id.btnVerHistorial)

        btnVerHistorial.setOnClickListener {
            mostrarBottomSheetHistorial(lineChartPeso)
        }

        btnCalcular.setOnClickListener { view ->
            val sEdad = etEdad.text.toString()
            val sPeso = etPeso.text.toString()
            val sEstatura = etAltura.text.toString()

            if (sEdad.isEmpty() || sPeso.isEmpty() || sEstatura.isEmpty()) {
                Snackbar.make(view, "Completa todos los campos", Snackbar.LENGTH_SHORT)
                    .setBackgroundTint(Color.RED)
                    .setTextColor(Color.WHITE)
                    .show()
                return@setOnClickListener
            }

            val edad: Int
            val pesoIngresado: Double
            val estaturaIngresada: Double
            try {
                edad = sEdad.toInt()
                pesoIngresado = sPeso.toDouble()
                estaturaIngresada = sEstatura.toDouble()
            } catch (e: NumberFormatException) {
                Snackbar.make(view, "Valores numéricos inválidos", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validaciones de rango (evita cálculos absurdos por errores de tipeo/unidad)
            if (edad !in 1..120) {
                Snackbar.make(view, "Ingresa una edad válida (1-120 años)", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (pesoIngresado <= 0 || pesoIngresado > 660) {
                Snackbar.make(view, "Ingresa un peso válido", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val rangoEstaturaValido = if (swEstaturaUnit.isChecked)
                estaturaIngresada in 12.0..100.0   // pulgadas (~30cm a ~254cm)
            else
                estaturaIngresada in 30.0..250.0   // centímetros
            if (!rangoEstaturaValido) {
                val unidad = if (swEstaturaUnit.isChecked) "pulgadas" else "centímetros"
                Snackbar.make(view, "Ingresa una altura válida en $unidad", Snackbar.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // Convertir a Kg para el IMC y el guardado
            val pesoKg = if (swPesoUnit.isChecked) pesoIngresado * 0.453592 else pesoIngresado

            // Convertir altura a metros: primero a centímetros (si viene en pulgadas), luego /100
            val estaturaCm = if (swEstaturaUnit.isChecked) estaturaIngresada * 2.54 else estaturaIngresada
            val estaturaM = estaturaCm / 100

            val imc = pesoKg / (estaturaM * estaturaM)

            // Extra: peso ideal (fórmula de Devine simplificada, 22 * altura^2)
            val pesoIdeal = 22 * (estaturaM * estaturaM)

            // Extra: % de grasa corporal estimado (fórmula de Deurenberg).
            val genero = sesionManager.obtenerGenero()
            val constanteGenero = if (genero == "Hombre") -16.2 else -5.4
            val grasaCorporal = ((1.20 * imc) + (0.23 * edad) + constanteGenero).coerceIn(3.0, 60.0)

            val categoria = categorizarIMC(imc)
            val diferenciaPeso = pesoKg - pesoIdeal

            tvImcResult.text = String.format("IMC: %.1f", imc)
            tvImcClasificacion.text = categoria
            tvPesoIdeal.text = String.format("Peso ideal: %.1f kg", pesoIdeal)
            tvGrasa.text = String.format("Grasa corporal estimada: %.1f%%", grasaCorporal)
            tvConsejoPeso.text = generarConsejoPeso(categoria, diferenciaPeso)
            
            // Actualizar la sesión global con el nuevo peso para que el Módulo de Agua lo refleje
            sesionManager.actualizarPerfil(
                nombre = sesionManager.obtenerNombreUsuario(),
                genero = genero,
                edad = edad,
                peso = pesoKg.toFloat(),
                altura = estaturaCm.toFloat()
            )

            // Mostrar resultados
            cardResult.visibility = View.VISIBLE

            guardarRegistro(pesoKg, imc, pesoIdeal, grasaCorporal)
            cargarGrafico(lineChartPeso)
        }
        
        cargarGrafico(lineChartPeso)
    }

    private fun cargarGrafico(chart: LineChart) {
        // SELECT * FROM peso (histórico completo, en orden cronológico para el gráfico)
        val historial = dbHelper.obtenerHistorialPeso().asReversed()

        chart.setNoDataText("Aún no hay registros")
        chart.setNoDataTextColor(Color.parseColor("#94A3B8"))

        if (historial.isEmpty()) {
            chart.clear()
            return
        }

        val entries = ArrayList<Entry>()
        historial.forEachIndexed { i, registro ->
            entries.add(Entry((i + 1).toFloat(), registro.peso.toFloat()))
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

    private fun mostrarBottomSheetHistorial(chart: LineChart) {
        val bottomSheetDialog = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.layout_bottom_sheet_historial, null)
        bottomSheetDialog.setContentView(view)

        val lvHistorial = view.findViewById<android.widget.ListView>(R.id.lvHistorial)

        // SELECT * FROM peso ORDER BY id DESC
        val registros = dbHelper.obtenerHistorialPeso()

        val listaString = mutableListOf<String>()
        for (registro in registros) {
            val extra = if (registro.pesoIdeal != null && registro.grasaCorporal != null) {
                " | Ideal: ${String.format("%.1f", registro.pesoIdeal)} kg | Grasa: ${String.format("%.1f", registro.grasaCorporal)}%"
            } else {
                ""
            }
            listaString.add("Fecha: ${registro.fecha}\nPeso: ${registro.peso} kg | IMC: ${String.format("%.1f", registro.imc)}$extra")
        }

        if (listaString.isEmpty()) {
            listaString.add("Aún no hay registros.")
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, listaString)
        lvHistorial.adapter = adapter

        // Long-click para borrar un registro (SQL: DELETE FROM peso WHERE id=?)
        lvHistorial.setOnItemLongClickListener { _, _, position, _ ->
            if (registros.isNotEmpty() && position < registros.size) {
                val registro = registros[position]
                AlertDialog.Builder(this)
                    .setTitle("Eliminar registro")
                    .setMessage("¿Eliminar el registro del ${registro.fecha}?")
                    .setPositiveButton("Eliminar") { _, _ ->
                        dbHelper.eliminarPeso(registro.id)
                        Toast.makeText(this, "Registro eliminado", Toast.LENGTH_SHORT).show()
                        cargarGrafico(chart)
                        bottomSheetDialog.dismiss()
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
            true
        }

        bottomSheetDialog.show()
    }

    private fun guardarRegistro(pesoKg: Double, imc: Double, pesoIdeal: Double, grasaCorporal: Double) {
        val fecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        // INSERT INTO peso (fecha, peso, imc, peso_ideal, grasa_corporal) VALUES (?, ?, ?, ?, ?)
        dbHelper.insertarPeso(fecha, pesoKg, imc, pesoIdeal, grasaCorporal)
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

    /**
     * Genera un consejo corto según la categoría de IMC, comparando el peso actual
     * contra el "peso ideal" calculado (22 * altura²).
     * diferenciaPeso = pesoActual - pesoIdeal (positivo = por encima, negativo = por debajo)
     */
    private fun generarConsejoPeso(categoria: String, diferenciaPeso: Double): String {
        val kgFaltantes = String.format("%.1f", kotlin.math.abs(diferenciaPeso))
        return when (categoria) {
            "Bajo Peso" ->
                "Estás por debajo de tu peso ideal. Te faltarían aprox. $kgFaltantes kg para alcanzarlo."
            "Normal" ->
                "¡Felicidades! Estás dentro de tu rango de peso ideal."
            "Sobrepeso" ->
                "Estás por encima de tu peso ideal. Bajar aprox. $kgFaltantes kg te ayudaría a llegar a un rango saludable."
            "Obesidad I" ->
                "Estás por encima de tu peso ideal (aprox. $kgFaltantes kg de más). Te recomendamos consultar a un profesional de la salud."
            "Obesidad II", "Obesidad III" ->
                "Tu peso está significativamente por encima del ideal (aprox. $kgFaltantes kg de más). Es importante buscar acompañamiento médico."
            else -> "Vuelve a calcular tus datos."
        }
    }
}
