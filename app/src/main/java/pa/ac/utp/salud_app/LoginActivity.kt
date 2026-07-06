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
        val etEdadLogin = findViewById<EditText>(R.id.etEdadLogin)
        val etPesoLogin = findViewById<EditText>(R.id.etPesoLogin)
        val etAlturaLogin = findViewById<EditText>(R.id.etAlturaLogin)
        val rgGeneroLogin = findViewById<android.widget.RadioGroup>(R.id.rgGeneroLogin)
        val rbHombreLogin = findViewById<android.widget.RadioButton>(R.id.rbHombreLogin)
        val btnIniciarSesion = findViewById<Button>(R.id.btnIniciarSesion)
        
        val swPesoUnitLogin = findViewById<androidx.appcompat.widget.SwitchCompat>(R.id.swPesoUnitLogin)
        val swEstaturaUnitLogin = findViewById<androidx.appcompat.widget.SwitchCompat>(R.id.swEstaturaUnitLogin)

        swPesoUnitLogin.setOnCheckedChangeListener { _, isChecked ->
            etPesoLogin.hint = if (isChecked) "Peso (Lb)" else "Peso (kg)"
        }

        swEstaturaUnitLogin.setOnCheckedChangeListener { _, isChecked ->
            etAlturaLogin.hint = if (isChecked) "Altura (in)" else "Altura (cm)"
        }

        btnIniciarSesion.setOnClickListener {
            val nombre = etNombreUsuario.text.toString().trim()
            val sEdad = etEdadLogin.text.toString()
            val sPeso = etPesoLogin.text.toString()
            val sAltura = etAlturaLogin.text.toString()

            if (nombre.isEmpty() || sEdad.isEmpty() || sPeso.isEmpty() || sAltura.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos para continuar", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val edad: Int
            var peso: Float
            var altura: Float
            try {
                edad = sEdad.toInt()
                peso = sPeso.toFloat()
                altura = sAltura.toFloat()
            } catch (e: NumberFormatException) {
                Toast.makeText(this, "Valores numéricos inválidos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Convertir a Kg y Cm si el usuario introdujo Lbs o Pulgadas
            if (swPesoUnitLogin.isChecked) {
                peso = (peso * 0.453592).toFloat()
            }
            
            if (swEstaturaUnitLogin.isChecked) {
                altura = (altura * 2.54).toFloat()
            }

            val genero = if (rbHombreLogin.isChecked) "Hombre" else "Mujer"

            sesionManager.iniciarSesion(nombre, genero, edad, peso, altura)

            startActivity(Intent(this, MainActivity::class.java))
            finish() // No queremos volver al Login con el botón atrás
        }
    }
}
