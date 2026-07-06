package pa.ac.utp.salud_app

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ModuloAgua : AppCompatActivity() {

    private var totalWater = 0f
    private var maxWater = 2000f
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var sesionManager: SesionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_modulo_agua)

        dbHelper = DatabaseHelper.getInstance(this)
        sesionManager = SesionManager(this)

        val silhouetteView = findViewById<SilhouetteWaterView>(R.id.silhouetteView)
        val tvWaterProgress = findViewById<TextView>(R.id.tvWaterProgress)
        
        val btnAdd100 = findViewById<Button>(R.id.btnAdd100)
        val btnAdd250 = findViewById<Button>(R.id.btnAdd250)
        val btnAdd500 = findViewById<Button>(R.id.btnAdd500)
        val btnReset = findViewById<Button>(R.id.btnReset)
        
        val etCustomWater = findViewById<EditText>(R.id.etCustomWater)
        val btnAddCustom = findViewById<Button>(R.id.btnAddCustom)
        val btnVerHistorialAgua = findViewById<Button>(R.id.btnVerHistorialAgua)

        // Configuración basada en SesionManager
        val peso = sesionManager.obtenerPeso()
        maxWater = peso * 35f // 35 ml por cada kg de peso corporal
        val genero = sesionManager.obtenerGenero()
        silhouetteView.gender = genero

        fun updateUI() {
            tvWaterProgress.text = "${totalWater.toInt()} / ${maxWater.toInt()} ml"
            silhouetteView.percentage = totalWater / maxWater
            guardarAguaHoy()
        }

        // Cargar datos previos de HOY
        cargarAguaHoy()
        
        updateUI()

        btnAdd100.setOnClickListener {
            totalWater += 100f
            if (totalWater > maxWater) totalWater = maxWater
            updateUI()
        }

        btnAdd250.setOnClickListener {
            totalWater += 250f
            if (totalWater > maxWater) totalWater = maxWater
            updateUI()
        }

        btnAdd500.setOnClickListener {
            totalWater += 500f
            if (totalWater > maxWater) totalWater = maxWater
            updateUI()
        }
        
        btnAddCustom.setOnClickListener {
            val amountStr = etCustomWater.text.toString()
            if (amountStr.isNotEmpty()) {
                val amount = amountStr.toFloat()
                totalWater += amount
                if (totalWater > maxWater) totalWater = maxWater
                updateUI()
                etCustomWater.text.clear()
            }
        }

        btnReset.setOnClickListener {
            totalWater = 0f
            updateUI()
        }
        
        btnVerHistorialAgua.setOnClickListener {
            mostrarHistorial()
        }
    }

    private fun getFechaHoy(): String {
        return SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
    }

    private fun cargarAguaHoy() {
        // Obtenemos el historial y buscamos el de hoy.
        // Si existe, cargamos el totalWater.
        val historial = dbHelper.obtenerHistorialAgua()
        val hoy = getFechaHoy()
        val registroHoy = historial.find { it.fecha == hoy }
        
        if (registroHoy != null) {
            totalWater = registroHoy.cantidadTotal
            // Actualizamos la meta por si el usuario cambió de peso desde la última vez que abrió la app hoy
            if (registroHoy.metaDiaria != maxWater) {
                // actualizar meta en db? Podemos dejarlo así o actualizarlo al guardar
            }
        } else {
            totalWater = 0f
        }
    }

    private fun guardarAguaHoy() {
        val historial = dbHelper.obtenerHistorialAgua()
        val hoy = getFechaHoy()
        val registroHoy = historial.find { it.fecha == hoy }

        if (registroHoy != null) {
            // Actualmente DatabaseHelper no tiene actualizarAgua, así que podemos borrar e insertar o crearlo.
            // Lo más fácil es eliminar el de hoy e insertar el nuevo, o simplemente agregar actualizarAgua
            dbHelper.eliminarAgua(registroHoy.id)
        }
        dbHelper.insertarAgua(hoy, totalWater, maxWater)
    }
    
    private fun mostrarHistorial() {
        val historial = dbHelper.obtenerHistorialAgua()
        val listaString = historial.map { 
            "Fecha: ${it.fecha} | Total: ${it.cantidadTotal.toInt()} / ${it.metaDiaria.toInt()} ml"
        }

        val items = if (listaString.isEmpty()) arrayOf("Aún no hay registros.") else listaString.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Historial de Hidratación")
            .setItems(items, null)
            .setPositiveButton("Cerrar", null)
            .show()
    }
}
