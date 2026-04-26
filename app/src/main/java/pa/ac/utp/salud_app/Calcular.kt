package pa.ac.utp.salud_app

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class Calcular : AppCompatActivity() {
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_calcular)

        val titulo = findViewById<TextView>(R.id.TxtTitulo_1)
        val valor1 = findViewById<EditText>(R.id.TxtValor1)
        val valor2 = findViewById<EditText>(R.id.TxtValor2)
        val boton = findViewById<Button>(R.id.BtCalcular)
        val resultado = findViewById<TextView>(R.id.TxtResultado)

        titulo.text = "Calcular Salud Prueba"

        boton.setOnClickListener {
            // Ejemplo de lógica básica
            val valor1 = valor1.text.toString()
            val valor2 = valor2.text.toString()

            if (valor1.isNotEmpty() && valor2.isNotEmpty()) {
                val valor1 = valor1.toInt()
                val valor2 = valor2.toInt()
                val suma = valor1 + valor2
                resultado.text = suma.toString()
            }


        }
    }
}
