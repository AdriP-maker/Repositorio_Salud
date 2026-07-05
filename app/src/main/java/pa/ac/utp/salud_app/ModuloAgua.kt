package pa.ac.utp.salud_app

import android.os.Bundle
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ModuloAgua : AppCompatActivity() {

    private var totalWater = 0f
    private val maxWater = 2000f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_modulo_agua)

        val silhouetteView = findViewById<SilhouetteWaterView>(R.id.silhouetteView)
        val tvWaterProgress = findViewById<TextView>(R.id.tvWaterProgress)
        val rgGender = findViewById<RadioGroup>(R.id.rgGender)
        val rbHombre = findViewById<RadioButton>(R.id.rbHombre)
        
        val btnAdd100 = findViewById<Button>(R.id.btnAdd100)
        val btnAdd250 = findViewById<Button>(R.id.btnAdd250)
        val btnAdd500 = findViewById<Button>(R.id.btnAdd500)
        val btnReset = findViewById<Button>(R.id.btnReset)
        
        fun updateUI() {
            tvWaterProgress.text = "${totalWater.toInt()} / ${maxWater.toInt()} ml"
            silhouetteView.percentage = totalWater / maxWater
            
            // Guardar totalWater
            getSharedPreferences("salud_app_prefs", MODE_PRIVATE)
                .edit().putFloat("agua_hoy", totalWater).apply()
        }

        // Cargar datos previos
        val prefs = getSharedPreferences("salud_app_prefs", MODE_PRIVATE)
        totalWater = prefs.getFloat("agua_hoy", 0f)
        val savedGender = prefs.getString("genero_agua", "Mujer")
        
        if (savedGender == "Hombre") {
            rbHombre.isChecked = true
            silhouetteView.gender = "Hombre"
        } else {
            silhouetteView.gender = "Mujer"
        }

        rgGender.setOnCheckedChangeListener { _, checkedId ->
            val genero = if (checkedId == R.id.rbHombre) "Hombre" else "Mujer"
            silhouetteView.gender = genero
            getSharedPreferences("salud_app_prefs", MODE_PRIVATE)
                .edit().putString("genero_agua", genero).apply()
        }

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
        
        btnReset.setOnClickListener {
            totalWater = 0f
            updateUI()
        }
        
        // Inicializar
        updateUI()
    }
}
