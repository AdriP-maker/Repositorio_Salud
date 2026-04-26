package pa.ac.utp.salud_app

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Helper: muestra Snackbar y abre Activity después de 1 segundo
        fun abrirModulo(card: CardView, mensaje: String, destino: Class<*>) {
            card.setOnClickListener { view ->
                Snackbar.make(view, mensaje, Snackbar.LENGTH_SHORT)
                    .setBackgroundTint(getColor(android.R.color.holo_blue_dark))
                    .setTextColor(getColor(android.R.color.white))
                    .show()
                Handler(Looper.getMainLooper()).postDelayed({
                    startActivity(Intent(this, destino))
                }, 1000)
            }
        }

        abrirModulo(
            card     = findViewById(R.id.card_peso),
            mensaje  = "Accediendo a Control de Peso…",
            destino  = ModuloPeso::class.java
        )

        abrirModulo(
            card     = findViewById(R.id.card_presion),
            mensaje  = "Accediendo a Presión Arterial…",
            destino  = ModuloPresion::class.java
        )

        abrirModulo(
            card     = findViewById(R.id.card_glucosa),
            mensaje  = "Accediendo a Glucosa en Sangre…",
            destino  = ModuloGlucosa::class.java
        )

        abrirModulo(
            card     = findViewById(R.id.card_agua),
            mensaje  = "Accediendo a Hidratación Diaria…",
            destino  = ModuloAgua::class.java
        )

        abrirModulo(
            card     = findViewById(R.id.card_medicina),
            mensaje  = "Accediendo a Medicamentos y Dosis…",
            destino  = ModuloMedicina::class.java
        )

        abrirModulo(
            card     = findViewById(R.id.card_actividad),
            mensaje  = "Accediendo a Actividad Física…",
            destino  = ModuloActividad::class.java
        )
    }
}
