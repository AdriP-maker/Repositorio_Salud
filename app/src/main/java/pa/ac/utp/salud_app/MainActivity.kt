package pa.ac.utp.salud_app

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatDelegate

class MainActivity : AppCompatActivity() {

    private lateinit var sesionManager: SesionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        setContentView(R.layout.activity_main)

        sesionManager = SesionManager(this)

        // Saludo personalizado con el nombre guardado en SharedPreferences (Login)
        val tvSaludoUsuario = findViewById<TextView>(R.id.tvSaludoUsuario)
        val nombre = sesionManager.obtenerNombreUsuario()
        tvSaludoUsuario.text = if (nombre.isNotEmpty()) "Hola, $nombre 👋" else "Hola 👋"

        // Acceso al perfil (cerrar sesión / borrar todos los datos)
        findViewById<ImageView>(R.id.ivPerfil).setOnClickListener {
            startActivity(Intent(this, PerfilActivity::class.java))
        }

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
