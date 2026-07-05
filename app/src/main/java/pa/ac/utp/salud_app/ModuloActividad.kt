package pa.ac.utp.salud_app

import android.content.Context
import android.content.pm.PackageManager
import android.Manifest
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
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
    private var isSensorPresent = false

    private val PERMISSION_REQUEST_ACTIVITY_RECOGNITION = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_modulo_actividad)

        tvSteps = findViewById(R.id.tvSteps)
        val barChartSteps = findViewById<BarChart>(R.id.barChartSteps)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        if (stepSensor != null) {
            isSensorPresent = true
            verificarPermisosSensor()
        } else {
            Toast.makeText(this, "Sensor de pasos no disponible en este dispositivo", Toast.LENGTH_LONG).show()
            tvSteps.text = "0"
        }

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
        val prefs = getSharedPreferences("salud_app_prefs", MODE_PRIVATE)
        tvSteps.text = prefs.getInt("pasos_hoy", 0).toString()
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
            tvSteps.text = steps.toString()
            getSharedPreferences("salud_app_prefs", MODE_PRIVATE)
                .edit().putInt("pasos_hoy", steps).apply()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No se necesita hacer nada aquí
    }
}
