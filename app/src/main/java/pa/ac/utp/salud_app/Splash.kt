package pa.ac.utp.salud_app

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class Splash : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Configuración básica de la pantalla
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash)

        // Ocultar la ActionBar si existe
        supportActionBar?.hide()

        // 2. Lógica del temporizador (Handler)
        // Se coloca arriba para asegurar que empiece a contar de inmediato
        val sesionManager = SesionManager(this)
        Handler(Looper.getMainLooper()).postDelayed({
            // Si ya hay una sesión guardada en SharedPreferences, entramos directo
            // al menú principal; si no, pedimos el nombre en el Login.
            val destino = if (sesionManager.haySesionActiva()) {
                MainActivity::class.java
            } else {
                LoginActivity::class.java
            }
            startActivity(Intent(this, destino))
            finish() // Cierra el Splash para que no se pueda volver con el botón atrás
        }, 3000) // 3 segundos es ideal para un splash

        // 3. Configuración de bordes (Opcional, pero se mantiene para el diseño)
        val mainLayout = findViewById<android.view.View>(R.id.main)
        if (mainLayout != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainLayout) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }
        }
    }
}