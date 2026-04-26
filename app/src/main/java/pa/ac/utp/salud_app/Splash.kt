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
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
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