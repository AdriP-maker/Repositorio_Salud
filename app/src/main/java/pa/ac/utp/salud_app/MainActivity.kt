package pa.ac.utp.salud_app

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Referencia al control usando CardView para evitar el error de casteo
        val cardPeso = findViewById<CardView>(R.id.Peso_Card)
        
        // 1. Click en Seguimiento de Peso
        cardPeso.setOnClickListener {
            // Usando Snackbar para que sea más vistoso
            Snackbar.make(it, "Accediendo a Peso e IMC", Snackbar.LENGTH_SHORT)
                .setAnchorView(it) // Flota sobre la tarjeta
                .setBackgroundTint(getColor(android.R.color.holo_blue_dark)) 
                .setTextColor(getColor(android.R.color.white))
                .show()
        }
    }
}
