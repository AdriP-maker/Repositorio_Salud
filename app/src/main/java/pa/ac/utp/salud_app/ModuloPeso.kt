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

            val sEdad     = etEdad.text.toString()
            val sPeso     = etPeso.text.toString()
            val sEstatura = etEstatura.text.toString()

            // Validación: campos vacíos
            if (sEdad.isEmpty() || sPeso.isEmpty() || sEstatura.isEmpty()) {
                Snackbar.make(view, "Completa todos los campos", Snackbar.LENGTH_SHORT)
                    .setBackgroundTint(getColor(android.R.color.holo_red_dark))
                    .setTextColor(getColor(android.R.color.white))
                    .show()
                return@setOnClickListener
            }

            val edad     = sEdad.toInt()
            var peso     = sPeso.toDouble()
            var estatura = sEstatura.toDouble()

            // Normalización de unidades
            if (swPeso.isChecked)     peso     *= 0.453592   // lb → kg
            if (swEstatura.isChecked) estatura *= 2.54        // in → cm

            // Cálculos
            val estaturaMetros = estatura / 100.0
            val imc       = peso / (estaturaMetros * estaturaMetros)
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
