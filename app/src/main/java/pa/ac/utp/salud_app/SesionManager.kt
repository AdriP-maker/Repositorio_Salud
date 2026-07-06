package pa.ac.utp.salud_app

import android.content.Context

/**
 * Maneja la "sesión" del usuario usando SharedPreferences.
 * No hay backend/servidor: es un login local pensado para personalizar la
 * experiencia (saludo con el nombre, recordar sesión iniciada, etc.), por eso
 * se queda en SharedPreferences en vez de la base de datos SQLite (que se usa
 * para los registros de salud: peso, glucosa, presión y medicina).
 */
class SesionManager(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "salud_app_prefs"
        private const val KEY_SESION_INICIADA = "sesion_iniciada"
        private const val KEY_NOMBRE_USUARIO = "usuario_nombre"
    }

    fun iniciarSesion(nombre: String) {
        prefs.edit()
            .putBoolean(KEY_SESION_INICIADA, true)
            .putString(KEY_NOMBRE_USUARIO, nombre)
            .apply()
    }

    fun haySesionActiva(): Boolean = prefs.getBoolean(KEY_SESION_INICIADA, false)

    fun obtenerNombreUsuario(): String = prefs.getString(KEY_NOMBRE_USUARIO, "") ?: ""

    /** Cierra sesión pero mantiene el resto de preferencias (agua, pasos, etc.). */
    fun cerrarSesion() {
        prefs.edit()
            .putBoolean(KEY_SESION_INICIADA, false)
            .apply()
    }

    /** Borra TODAS las preferencias (usado por "Borrar todos los datos"). */
    fun borrarTodo() {
        prefs.edit().clear().apply()
    }
}
