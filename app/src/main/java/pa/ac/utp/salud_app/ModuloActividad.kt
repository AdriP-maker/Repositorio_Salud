package pa.ac.utp.salud_app

import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.Manifest
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry

class ModuloActividad : AppCompatActivity(), SensorEventListener {

    private var sensorManager: SensorManager? = null
    private var stepSensor: Sensor? = null
    private lateinit var tvSteps: TextView
    private lateinit var tvMetaPasos: TextView
    private lateinit var pbPasos: ProgressBar
    private var isSensorPresent = false

    private val PERMISSION_REQUEST_ACTIVITY_RECOGNITION = 100

    // Opciones predefinidas de meta de pasos (además de "Personalizada")
    private val metasPredefinidas = listOf(5000, 7500, 10000, 12500, 15000, 20000)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_modulo_actividad)

        tvSteps = findViewById(R.id.tvSteps)
        tvMetaPasos = findViewById(R.id.tvMetaPasos)
        pbPasos = findViewById(R.id.pbPasos)
        val barChartSteps = findViewById<BarChart>(R.id.barChartSteps)
        val btnCambiarMeta = findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCambiarMeta)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        if (stepSensor != null) {
            isSensorPresent = true
            verificarPermisosSensor()
        } else {
            Toast.makeText(this, "Sensor de pasos no disponible en este dispositivo", Toast.LENGTH_LONG).show()
            tvSteps.text = "0"
        }

        btnCambiarMeta.setOnClickListener {
            mostrarDialogoMeta()
        }

        actualizarProgreso()
        cargarHistorialPasos(barChartSteps)
    }

    private fun verificarPermisosSensor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.ACTIVITY_RECOGNITION),
                    PERMISSION_REQUEST_ACTIVITY_RECOGNITION)
            }
        }
    }

    // ---------------------------------------------------------------------
    // Meta de pasos (guardada en SharedPreferences, igual que "pasos_hoy")
    // ---------------------------------------------------------------------

    private fun obtenerMetaPasos(): Int {
        val prefs = getSharedPreferences("salud_app_prefs", MODE_PRIVATE)
        return prefs.getInt("meta_pasos", 10000)
    }

    private fun guardarMetaPasos(meta: Int) {
        getSharedPreferences("salud_app_prefs", MODE_PRIVATE)
            .edit().putInt("meta_pasos", meta).apply()
    }

    private fun mostrarDialogoMeta() {
        val opciones = metasPredefinidas.map { "${"%,d".format(it)} pasos" }.toMutableList()
        opciones.add("Personalizada...")

        AlertDialog.Builder(this)
            .setTitle("Elige tu meta diaria")
            .setItems(opciones.toTypedArray()) { dialog, which ->
                if (which < metasPredefinidas.size) {
                    guardarMetaPasos(metasPredefinidas[which])
                    actualizarProgreso()
                    Toast.makeText(this, "Meta actualizada", Toast.LENGTH_SHORT).show()
                } else {
                    mostrarDialogoMetaPersonalizada()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarDialogoMetaPersonalizada() {
        val input = EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            hint = "Ej. 8000"
            setPadding(48, 32, 48, 32)
        }

        AlertDialog.Builder(this)
            .setTitle("Meta personalizada")
            .setMessage("¿Cuántos pasos quieres caminar al día?")
            .setView(input)
            .setPositiveButton("Guardar") { _, _ ->
                val valor = input.text.toString().toIntOrNull()
                if (valor == null || valor <= 0) {
                    Toast.makeText(this, "Ingresa un número válido", Toast.LENGTH_SHORT).show()
                } else {
                    guardarMetaPasos(valor)
                    actualizarProgreso()
                    Toast.makeText(this, "Meta actualizada", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun actualizarProgreso() {
        val prefs = getSharedPreferences("salud_app_prefs", MODE_PRIVATE)
        val pasosHoy = prefs.getInt("pasos_hoy", 0)
        val meta = obtenerMetaPasos()

        tvSteps.text = pasosHoy.toString()
        tvMetaPasos.text = "Meta: ${"%,d".format(meta)} pasos"
        val porcentaje = if (meta > 0) ((pasosHoy * 100) / meta).coerceIn(0, 100) else 0
        pbPasos.progress = porcentaje
    }

    private fun cargarHistorialPasos(chart: BarChart) {
        val prefs = getSharedPreferences("salud_app_prefs", MODE_PRIVATE)
        val pasosHoy = prefs.getInt("pasos_hoy", 0)

        chart.setNoDataText("Aún no hay registros")
        chart.setNoDataTextColor(android.graphics.Color.parseColor("#94A3B8"))

        if (pasosHoy == 0) {
            chart.clear()
            return
        }

        val entries = ArrayList<BarEntry>()
        entries.add(BarEntry(1f, pasosHoy.toFloat()))

        val dataSet = BarDataSet(entries, "Pasos")
        dataSet.color = android.graphics.Color.parseColor("#10B981")
        dataSet.setDrawValues(false)

        val barData = BarData(dataSet)
        chart.data = barData
        chart.description.isEnabled = false
        chart.xAxis.setDrawGridLines(false)
        chart.axisRight.isEnabled = false
        chart.animateY(1000)
    }

    override fun onResume() {
        super.onResume()
        if (isSensorPresent) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED) {
                    sensorManager?.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_UI)
                }
            } else {
                sensorManager?.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_UI)
            }
        }
        actualizarProgreso()
    }

    override fun onPause() {
        super.onPause()
        if (isSensorPresent) {
            sensorManager?.unregisterListener(this)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event != null) {
            val steps = event.values[0].toInt()
            getSharedPreferences("salud_app_prefs", MODE_PRIVATE)
                .edit().putInt("pasos_hoy", steps).apply()
            actualizarProgreso()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No se necesita hacer nada aquí
    }
}
