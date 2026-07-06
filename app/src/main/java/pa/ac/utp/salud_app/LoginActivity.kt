package pa.ac.utp.salud_app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * Login simple, 100% local (no hay servidor). Su único propósito es guardar
 * el nombre del usuario en SharedPreferences (a través de SesionManager) para
 * poder saludarlo en el menú principal y para el botón "Cerrar sesión" del
 * perfil. No maneja contraseñas ni valida contra ninguna base de datos.
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var sesionManager: SesionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        sesionManager = SesionManager(this)

        val etNombreUsuario = findViewById<EditText>(R.id.etNombreUsuario)
        val btnIniciarSesion = findViewById<Button>(R.id.btnIniciarSesion)

        btnIniciarSesion.setOnClickListener {
            val nombre = etNombreUsuario.text.toString().trim()

            if (nombre.isEmpty()) {
                Toast.makeText(this, "Escribe tu nombre para continuar", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            sesionManager.iniciarSesion(nombre)

            startActivity(Intent(this, MainActivity::class.java))
            finish() // No queremos volver al Login con el botón atrás
        }
    }
}
