package pa.ac.utp.salud_app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class PerfilActivity : AppCompatActivity() {

    private lateinit var sesionManager: SesionManager
    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_perfil)

        sesionManager = SesionManager(this)
        dbHelper = DatabaseHelper.getInstance(this)

        val tvNombreUsuario = findViewById<TextView>(R.id.tvNombreUsuario)
        val etEdadPerfil = findViewById<android.widget.EditText>(R.id.etEdadPerfil)
        val etPesoPerfil = findViewById<android.widget.EditText>(R.id.etPesoPerfil)
        val etAlturaPerfil = findViewById<android.widget.EditText>(R.id.etAlturaPerfil)
        val btnGuardarPerfil = findViewById<Button>(R.id.btnGuardarPerfil)
        
        val btnCerrarSesion = findViewById<Button>(R.id.btnCerrarSesion)
        val btnBorrarDatos = findViewById<Button>(R.id.btnBorrarDatos)

        val nombre = sesionManager.obtenerNombreUsuario()
        tvNombreUsuario.text = if (nombre.isNotEmpty()) nombre else "Usuario"
        
        etEdadPerfil.setText(sesionManager.obtenerEdad().toString())
        etPesoPerfil.setText(sesionManager.obtenerPeso().toString())
        etAlturaPerfil.setText(sesionManager.obtenerAltura().toString())

        btnGuardarPerfil.setOnClickListener {
            try {
                val nuevaEdad = etEdadPerfil.text.toString().toInt()
                val nuevoPeso = etPesoPerfil.text.toString().toFloat()
                val nuevaAltura = etAlturaPerfil.text.toString().toFloat()
                
                sesionManager.actualizarPerfil(
                    nombre = nombre,
                    genero = sesionManager.obtenerGenero(), // Mantenemos el género por ahora o lo agregamos después
                    edad = nuevaEdad,
                    peso = nuevoPeso,
                    altura = nuevaAltura
                )
                Toast.makeText(this, "Datos guardados", Toast.LENGTH_SHORT).show()
            } catch (e: NumberFormatException) {
                Toast.makeText(this, "Por favor revisa los números ingresados", Toast.LENGTH_SHORT).show()
            }
        }

        btnCerrarSesion.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Cerrar sesión")
                .setMessage("¿Seguro que quieres cerrar sesión? Tus registros de salud se conservan.")
                .setPositiveButton("Cerrar sesión") { _, _ ->
                    sesionManager.cerrarSesion()
                    irALogin()
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        btnBorrarDatos.setOnClickListener {
            confirmarBorradoTotal()
        }
    }

    private fun confirmarBorradoTotal() {
        AlertDialog.Builder(this)
            .setTitle("Borrar todos los datos")
            .setMessage(
                "Esta acción es IRREVERSIBLE. Se eliminará todo tu historial de " +
                    "peso, glucosa, presión, medicamentos y tus preferencias " +
                    "(agua, pasos y sesión). ¿Deseas continuar?"
            )
            .setPositiveButton("Sí, borrar todo") { _, _ ->
                borrarTodo()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun borrarTodo() {
        // Borra todas las tablas de la base de datos SQLite
        dbHelper.borrarTodosLosDatos()
        // Borra todas las SharedPreferences (agua, pasos, genero, sesión, etc.)
        sesionManager.borrarTodo()

        Toast.makeText(this, "Todos los datos fueron eliminados", Toast.LENGTH_LONG).show()
        irALogin()
    }

    private fun irALogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
