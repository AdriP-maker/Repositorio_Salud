package pa.ac.utp.salud_app

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.google.android.material.snackbar.Snackbar

class ModuloPeso : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_modulo_peso)

        // --- Referencias a vistas ---
        val etEdad         = findViewById<EditText>(R.id.etEdad)
        val etPeso         = findViewById<EditText>(R.id.etPeso)
        val etEstatura     = findViewById<EditText>(R.id.etEstatura)
        val swPeso         = findViewById<SwitchCompat>(R.id.swPesoUnit)
        val swEstatura     = findViewById<SwitchCompat>(R.id.swEstaturaUnit)
        val btnCalcular    = findViewById<Button>(R.id.btnCalcular)
        val tvIMC          = findViewById<TextView>(R.id.tvIMC)
        val tvPesoIdeal    = findViewById<TextView>(R.id.tvPesoIdeal)
        val tvGrasa        = findViewById<TextView>(R.id.tvGrasa)
        val tvClasificacion = findViewById<TextView>(R.id.tvClasificacion)

        // --- Listeners switches: actualizar hint y limpiar campo ---
        swPeso.setOnCheckedChangeListener { _, isChecked ->
            etPeso.hint = if (isChecked) "Peso (Lb)" else "Peso (Kg)"
            etPeso.text.clear()
        }

        swEstatura.setOnCheckedChangeListener { _, isChecked ->
            etEstatura.hint = if (isChecked) "Estatura (in)" else "Estatura (cm)"
            etEstatura.text.clear()
        }

        // --- Botón Calcular ---
        btnCalcular.setOnClickListener { view ->

            fun error(msg: String) {
                Snackbar.make(view, msg, Snackbar.LENGTH_SHORT)
                    .setBackgroundTint(getColor(android.R.color.holo_red_dark))
                    .setTextColor(getColor(android.R.color.white))
                    .show()
            }

            val sEdad     = etEdad.text.toString()
            val sPeso     = etPeso.text.toString()
            val sEstatura = etEstatura.text.toString()

            // Validación: campos vacíos
            if (sEdad.isEmpty() || sPeso.isEmpty() || sEstatura.isEmpty()) {
                error("Completa todos los campos")
                return@setOnClickListener
            }

            val edad: Int
            val peso: Double
            val estatura: Double
            try {
                edad     = sEdad.toInt()
                peso     = sPeso.toDouble()
                estatura = sEstatura.toDouble()
            } catch (e: NumberFormatException) {
                error("Valores numéricos inválidos")
                return@setOnClickListener
            }

            // Validación de rangos según unidad seleccionada
            if (edad < 1 || edad > 120) {
                error("Edad inválida (1 – 120 años)")
                return@setOnClickListener
            }
            if (swPeso.isChecked) {
                if (peso < 22.0 || peso > 661.0) { error("Peso inválido (22 – 661 lb)"); return@setOnClickListener }
            } else {
                if (peso < 10.0 || peso > 300.0) { error("Peso inválido (10 – 300 kg)"); return@setOnClickListener }
            }
            if (swEstatura.isChecked) {
                if (estatura < 20.0 || estatura > 99.0) { error("Estatura inválida (20 – 99 in)"); return@setOnClickListener }
            } else {
                if (estatura < 50.0 || estatura > 250.0) { error("Estatura inválida (50 – 250 cm)"); return@setOnClickListener }
            }

            var pesoKg     = peso
            var estatCm    = estatura

            // Normalización de unidades
            if (swPeso.isChecked)     pesoKg  *= 0.453592   // lb → kg
            if (swEstatura.isChecked) estatCm *= 2.54        // in → cm

            // Cálculos
            val estaturaMetros = estatCm / 100.0
            val imc       = pesoKg / (estaturaMetros * estaturaMetros)
            val pesoIdeal = 22.0 * (estaturaMetros * estaturaMetros)
            val grasa     = (1.20 * imc) + (0.23 * edad) - 16.2

            // Actualizar UI
            tvIMC.text          = String.format("%.1f", imc)
            tvPesoIdeal.text    = String.format("%.1f kg", pesoIdeal)
            tvGrasa.text        = String.format("%.1f%%", grasa)
            tvClasificacion.text = categorizarIMC(imc)
        }
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
