package pa.ac.utp.salud_app

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * Clase encargada exclusivamente de:
 *  - Crear la base de datos y sus tablas (onCreate)
 *  - Manejar la conexión (SQLiteOpenHelper se encarga de abrir/cerrar)
 *  - Ejecutar las sentencias SQL (INSERT, SELECT, UPDATE, DELETE) de todos los módulos
 *
 * Todas las Activities de la app deben usar esta clase para leer/escribir datos,
 * en vez de SharedPreferences + JSON como se hacía antes.
 */
class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "salud_app.db"
        private const val DATABASE_VERSION = 3

        // ---------- Nombres de tablas ----------
        const val TABLE_PESO = "peso"
        const val TABLE_GLUCOSA = "glucosa"
        const val TABLE_PRESION = "presion"
        const val TABLE_MEDICINA = "medicina"

        // ---------- Columnas comunes ----------
        const val COL_ID = "id"
        const val COL_FECHA = "fecha"

        // peso
        const val COL_PESO_VALOR = "peso"
        const val COL_PESO_IMC = "imc"
        const val COL_PESO_IDEAL = "peso_ideal"       // extra: 22 * altura^2
        const val COL_PESO_GRASA = "grasa_corporal"   // extra: % grasa estimada (Deurenberg)

        // glucosa
        const val COL_GLUCOSA_VALOR = "glucosa"
        const val COL_GLUCOSA_TIPO = "tipo"
        const val COL_GLUCOSA_NOTAS = "notas" // extra: notas opcionales del registro

        // presion
        const val COL_PRESION_SISTOLICA = "sistolica"
        const val COL_PRESION_DIASTOLICA = "diastolica"
        const val COL_PRESION_PULSO = "pulso" // extra
        const val COL_PRESION_BRAZO = "brazo" // extra

        // medicina
        const val COL_MEDICINA_NOMBRE = "nombre"
        const val COL_MEDICINA_DOSIS = "dosis"
        const val COL_MEDICINA_FRECUENCIA = "frecuencia"
        const val COL_MEDICINA_HORA_RECORDATORIO = "hora_recordatorio" // formato "HH:mm", null si no tiene
        const val COL_MEDICINA_RECORDATORIO_ACTIVO = "recordatorio_activo" // 0 o 1

        @Volatile
        private var instancia: DatabaseHelper? = null

        /** Acceso único (singleton) a la clase, para no abrir múltiples conexiones. */
        fun getInstance(context: Context): DatabaseHelper {
            return instancia ?: synchronized(this) {
                instancia ?: DatabaseHelper(context.applicationContext).also { instancia = it }
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_PESO (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_FECHA TEXT NOT NULL,
                $COL_PESO_VALOR REAL NOT NULL,
                $COL_PESO_IMC REAL NOT NULL,
                $COL_PESO_IDEAL REAL,
                $COL_PESO_GRASA REAL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_GLUCOSA (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_FECHA TEXT NOT NULL,
                $COL_GLUCOSA_VALOR REAL NOT NULL,
                $COL_GLUCOSA_TIPO TEXT NOT NULL,
                $COL_GLUCOSA_NOTAS TEXT
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_PRESION (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_FECHA TEXT NOT NULL,
                $COL_PRESION_SISTOLICA INTEGER NOT NULL,
                $COL_PRESION_DIASTOLICA INTEGER NOT NULL,
                $COL_PRESION_PULSO INTEGER,
                $COL_PRESION_BRAZO TEXT
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_MEDICINA (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_MEDICINA_NOMBRE TEXT NOT NULL,
                $COL_MEDICINA_DOSIS TEXT NOT NULL,
                $COL_MEDICINA_FRECUENCIA TEXT NOT NULL,
                $COL_MEDICINA_HORA_RECORDATORIO TEXT,
                $COL_MEDICINA_RECORDATORIO_ACTIVO INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Migración aditiva: no borramos las tablas para no perder el historial
        // ya guardado por el usuario (peso, glucosa, presión, medicinas).
        if (oldVersion < 2) {
            // Se agregan las columnas del recordatorio de medicamentos
            db.execSQL("ALTER TABLE $TABLE_MEDICINA ADD COLUMN $COL_MEDICINA_HORA_RECORDATORIO TEXT")
            db.execSQL("ALTER TABLE $TABLE_MEDICINA ADD COLUMN $COL_MEDICINA_RECORDATORIO_ACTIVO INTEGER NOT NULL DEFAULT 0")
        }
        if (oldVersion < 3) {
            // Campos "extra" incorporados desde el proyecto de referencia (primeraclase)
            db.execSQL("ALTER TABLE $TABLE_PESO ADD COLUMN $COL_PESO_IDEAL REAL")
            db.execSQL("ALTER TABLE $TABLE_PESO ADD COLUMN $COL_PESO_GRASA REAL")
            db.execSQL("ALTER TABLE $TABLE_GLUCOSA ADD COLUMN $COL_GLUCOSA_NOTAS TEXT")
            db.execSQL("ALTER TABLE $TABLE_PRESION ADD COLUMN $COL_PRESION_PULSO INTEGER")
            db.execSQL("ALTER TABLE $TABLE_PRESION ADD COLUMN $COL_PRESION_BRAZO TEXT")
        }
    }

    /**
     * Borra TODOS los registros de TODAS las tablas (Peso, Glucosa, Presión, Medicina).
     * Se usa desde la pantalla de Perfil, botón "Borrar todos los datos".
     * Nota: las preferencias simples (agua del día, pasos, sesión) viven en
     * SharedPreferences y se limpian aparte, no aquí.
     */
    fun borrarTodosLosDatos() {
        val db = writableDatabase
        // DELETE FROM peso
        db.execSQL("DELETE FROM $TABLE_PESO")
        // DELETE FROM glucosa
        db.execSQL("DELETE FROM $TABLE_GLUCOSA")
        // DELETE FROM presion
        db.execSQL("DELETE FROM $TABLE_PRESION")
        // DELETE FROM medicina
        db.execSQL("DELETE FROM $TABLE_MEDICINA")
        // Reinicia los contadores autoincrementales
        db.execSQL("DELETE FROM sqlite_sequence WHERE name IN ('$TABLE_PESO', '$TABLE_GLUCOSA', '$TABLE_PRESION', '$TABLE_MEDICINA')")
    }

    // ===================================================================================
    // PESO
    // ===================================================================================

    fun insertarPeso(
        fecha: String,
        peso: Double,
        imc: Double,
        pesoIdeal: Double? = null,
        grasaCorporal: Double? = null
    ): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_FECHA, fecha)
            put(COL_PESO_VALOR, peso)
            put(COL_PESO_IMC, imc)
            put(COL_PESO_IDEAL, pesoIdeal)
            put(COL_PESO_GRASA, grasaCorporal)
        }
        // INSERT INTO peso (fecha, peso, imc, peso_ideal, grasa_corporal) VALUES (?, ?, ?, ?, ?)
        return db.insert(TABLE_PESO, null, values)
    }

    fun obtenerHistorialPeso(): List<RegistroPeso> {
        val lista = mutableListOf<RegistroPeso>()
        val db = readableDatabase
        // SELECT * FROM peso ORDER BY id DESC
        val cursor: Cursor = db.rawQuery(
            "SELECT $COL_ID, $COL_FECHA, $COL_PESO_VALOR, $COL_PESO_IMC, $COL_PESO_IDEAL, $COL_PESO_GRASA " +
                "FROM $TABLE_PESO ORDER BY $COL_ID DESC",
            null
        )
        cursor.use {
            while (it.moveToNext()) {
                lista.add(
                    RegistroPeso(
                        id = it.getInt(it.getColumnIndexOrThrow(COL_ID)),
                        fecha = it.getString(it.getColumnIndexOrThrow(COL_FECHA)),
                        peso = it.getDouble(it.getColumnIndexOrThrow(COL_PESO_VALOR)),
                        imc = it.getDouble(it.getColumnIndexOrThrow(COL_PESO_IMC)),
                        pesoIdeal = if (it.isNull(it.getColumnIndexOrThrow(COL_PESO_IDEAL))) null
                            else it.getDouble(it.getColumnIndexOrThrow(COL_PESO_IDEAL)),
                        grasaCorporal = if (it.isNull(it.getColumnIndexOrThrow(COL_PESO_GRASA))) null
                            else it.getDouble(it.getColumnIndexOrThrow(COL_PESO_GRASA))
                    )
                )
            }
        }
        return lista
    }

    fun actualizarPeso(id: Int, fecha: String, peso: Double, imc: Double): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_FECHA, fecha)
            put(COL_PESO_VALOR, peso)
            put(COL_PESO_IMC, imc)
        }
        // UPDATE peso SET fecha=?, peso=?, imc=? WHERE id=?
        return db.update(TABLE_PESO, values, "$COL_ID = ?", arrayOf(id.toString()))
    }

    fun eliminarPeso(id: Int): Int {
        val db = writableDatabase
        // DELETE FROM peso WHERE id=?
        return db.delete(TABLE_PESO, "$COL_ID = ?", arrayOf(id.toString()))
    }

    // ===================================================================================
    // GLUCOSA
    // ===================================================================================

    fun insertarGlucosa(fecha: String, glucosa: Double, tipo: String, notas: String? = null): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_FECHA, fecha)
            put(COL_GLUCOSA_VALOR, glucosa)
            put(COL_GLUCOSA_TIPO, tipo)
            put(COL_GLUCOSA_NOTAS, notas)
        }
        // INSERT INTO glucosa (fecha, glucosa, tipo, notas) VALUES (?, ?, ?, ?)
        return db.insert(TABLE_GLUCOSA, null, values)
    }

    fun obtenerHistorialGlucosa(): List<RegistroGlucosa> {
        val lista = mutableListOf<RegistroGlucosa>()
        val db = readableDatabase
        // SELECT * FROM glucosa ORDER BY id ASC
        val cursor = db.rawQuery(
            "SELECT $COL_ID, $COL_FECHA, $COL_GLUCOSA_VALOR, $COL_GLUCOSA_TIPO, $COL_GLUCOSA_NOTAS " +
                "FROM $TABLE_GLUCOSA ORDER BY $COL_ID ASC",
            null
        )
        cursor.use {
            while (it.moveToNext()) {
                lista.add(
                    RegistroGlucosa(
                        id = it.getInt(it.getColumnIndexOrThrow(COL_ID)),
                        fecha = it.getString(it.getColumnIndexOrThrow(COL_FECHA)),
                        glucosa = it.getDouble(it.getColumnIndexOrThrow(COL_GLUCOSA_VALOR)),
                        tipo = it.getString(it.getColumnIndexOrThrow(COL_GLUCOSA_TIPO)),
                        notas = it.getString(it.getColumnIndexOrThrow(COL_GLUCOSA_NOTAS))
                    )
                )
            }
        }
        return lista
    }

    fun eliminarGlucosa(id: Int): Int {
        val db = writableDatabase
        // DELETE FROM glucosa WHERE id=?
        return db.delete(TABLE_GLUCOSA, "$COL_ID = ?", arrayOf(id.toString()))
    }

    fun actualizarGlucosa(id: Int, fecha: String, glucosa: Double, tipo: String): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_FECHA, fecha)
            put(COL_GLUCOSA_VALOR, glucosa)
            put(COL_GLUCOSA_TIPO, tipo)
        }
        // UPDATE glucosa SET fecha=?, glucosa=?, tipo=? WHERE id=?
        return db.update(TABLE_GLUCOSA, values, "$COL_ID = ?", arrayOf(id.toString()))
    }

    // ===================================================================================
    // PRESION
    // ===================================================================================

    fun insertarPresion(
        fecha: String,
        sistolica: Int,
        diastolica: Int,
        pulso: Int? = null,
        brazo: String? = null
    ): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_FECHA, fecha)
            put(COL_PRESION_SISTOLICA, sistolica)
            put(COL_PRESION_DIASTOLICA, diastolica)
            put(COL_PRESION_PULSO, pulso)
            put(COL_PRESION_BRAZO, brazo)
        }
        // INSERT INTO presion (fecha, sistolica, diastolica, pulso, brazo) VALUES (?, ?, ?, ?, ?)
        return db.insert(TABLE_PRESION, null, values)
    }

    fun obtenerHistorialPresion(): List<RegistroPresion> {
        val lista = mutableListOf<RegistroPresion>()
        val db = readableDatabase
        // SELECT * FROM presion ORDER BY id ASC
        val cursor = db.rawQuery(
            "SELECT $COL_ID, $COL_FECHA, $COL_PRESION_SISTOLICA, $COL_PRESION_DIASTOLICA, $COL_PRESION_PULSO, $COL_PRESION_BRAZO " +
                "FROM $TABLE_PRESION ORDER BY $COL_ID ASC",
            null
        )
        cursor.use {
            while (it.moveToNext()) {
                lista.add(
                    RegistroPresion(
                        id = it.getInt(it.getColumnIndexOrThrow(COL_ID)),
                        fecha = it.getString(it.getColumnIndexOrThrow(COL_FECHA)),
                        sistolica = it.getInt(it.getColumnIndexOrThrow(COL_PRESION_SISTOLICA)),
                        diastolica = it.getInt(it.getColumnIndexOrThrow(COL_PRESION_DIASTOLICA)),
                        pulso = if (it.isNull(it.getColumnIndexOrThrow(COL_PRESION_PULSO))) null
                            else it.getInt(it.getColumnIndexOrThrow(COL_PRESION_PULSO)),
                        brazo = it.getString(it.getColumnIndexOrThrow(COL_PRESION_BRAZO))
                    )
                )
            }
        }
        return lista
    }

    fun eliminarPresion(id: Int): Int {
        val db = writableDatabase
        // DELETE FROM presion WHERE id=?
        return db.delete(TABLE_PRESION, "$COL_ID = ?", arrayOf(id.toString()))
    }

    fun actualizarPresion(id: Int, fecha: String, sistolica: Int, diastolica: Int): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_FECHA, fecha)
            put(COL_PRESION_SISTOLICA, sistolica)
            put(COL_PRESION_DIASTOLICA, diastolica)
        }
        // UPDATE presion SET fecha=?, sistolica=?, diastolica=? WHERE id=?
        return db.update(TABLE_PRESION, values, "$COL_ID = ?", arrayOf(id.toString()))
    }

    // ===================================================================================
    // MEDICINA
    // ===================================================================================

    fun insertarMedicina(
        nombre: String,
        dosis: String,
        frecuencia: String,
        horaRecordatorio: String? = null,
        recordatorioActivo: Boolean = false
    ): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_MEDICINA_NOMBRE, nombre)
            put(COL_MEDICINA_DOSIS, dosis)
            put(COL_MEDICINA_FRECUENCIA, frecuencia)
            put(COL_MEDICINA_HORA_RECORDATORIO, horaRecordatorio)
            put(COL_MEDICINA_RECORDATORIO_ACTIVO, if (recordatorioActivo) 1 else 0)
        }
        // INSERT INTO medicina (nombre, dosis, frecuencia, hora_recordatorio, recordatorio_activo) VALUES (?, ?, ?, ?, ?)
        return db.insert(TABLE_MEDICINA, null, values)
    }

    fun obtenerMedicinas(): List<RegistroMedicina> {
        val lista = mutableListOf<RegistroMedicina>()
        val db = readableDatabase
        // SELECT * FROM medicina ORDER BY id ASC
        val cursor = db.rawQuery(
            "SELECT $COL_ID, $COL_MEDICINA_NOMBRE, $COL_MEDICINA_DOSIS, $COL_MEDICINA_FRECUENCIA, " +
                "$COL_MEDICINA_HORA_RECORDATORIO, $COL_MEDICINA_RECORDATORIO_ACTIVO " +
                "FROM $TABLE_MEDICINA ORDER BY $COL_ID ASC",
            null
        )
        cursor.use {
            while (it.moveToNext()) {
                lista.add(
                    RegistroMedicina(
                        id = it.getInt(it.getColumnIndexOrThrow(COL_ID)),
                        nombre = it.getString(it.getColumnIndexOrThrow(COL_MEDICINA_NOMBRE)),
                        dosis = it.getString(it.getColumnIndexOrThrow(COL_MEDICINA_DOSIS)),
                        frecuencia = it.getString(it.getColumnIndexOrThrow(COL_MEDICINA_FRECUENCIA)),
                        horaRecordatorio = it.getString(it.getColumnIndexOrThrow(COL_MEDICINA_HORA_RECORDATORIO)),
                        recordatorioActivo = it.getInt(it.getColumnIndexOrThrow(COL_MEDICINA_RECORDATORIO_ACTIVO)) == 1
                    )
                )
            }
        }
        return lista
    }

    /** Usado al reiniciar el teléfono para volver a programar las alarmas activas. */
    fun obtenerMedicinasConRecordatorioActivo(): List<RegistroMedicina> {
        return obtenerMedicinas().filter { it.recordatorioActivo && !it.horaRecordatorio.isNullOrEmpty() }
    }

    fun actualizarMedicina(id: Int, nombre: String, dosis: String, frecuencia: String): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_MEDICINA_NOMBRE, nombre)
            put(COL_MEDICINA_DOSIS, dosis)
            put(COL_MEDICINA_FRECUENCIA, frecuencia)
        }
        // UPDATE medicina SET nombre=?, dosis=?, frecuencia=? WHERE id=?
        return db.update(TABLE_MEDICINA, values, "$COL_ID = ?", arrayOf(id.toString()))
    }

    /** Actualiza únicamente la configuración del recordatorio de un medicamento. */
    fun actualizarRecordatorioMedicina(id: Int, horaRecordatorio: String?, recordatorioActivo: Boolean): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_MEDICINA_HORA_RECORDATORIO, horaRecordatorio)
            put(COL_MEDICINA_RECORDATORIO_ACTIVO, if (recordatorioActivo) 1 else 0)
        }
        // UPDATE medicina SET hora_recordatorio=?, recordatorio_activo=? WHERE id=?
        return db.update(TABLE_MEDICINA, values, "$COL_ID = ?", arrayOf(id.toString()))
    }

    fun eliminarMedicina(id: Int): Int {
        val db = writableDatabase
        // DELETE FROM medicina WHERE id=?
        return db.delete(TABLE_MEDICINA, "$COL_ID = ?", arrayOf(id.toString()))
    }

}

// =======================================================================================
// Data classes usadas por DatabaseHelper (incluyen "id" para poder editar/borrar)
// =======================================================================================

data class RegistroPeso(
    val id: Int = 0,
    val fecha: String,
    val peso: Double,
    val imc: Double,
    val pesoIdeal: Double? = null,
    val grasaCorporal: Double? = null
)

data class RegistroGlucosa(
    val id: Int = 0,
    val fecha: String,
    val glucosa: Double,
    val tipo: String,
    val notas: String? = null
)

data class RegistroPresion(
    val id: Int = 0,
    val fecha: String,
    val sistolica: Int,
    val diastolica: Int,
    val pulso: Int? = null,
    val brazo: String? = null
)

data class RegistroMedicina(
    val id: Int = 0,
    val nombre: String,
    val dosis: String,
    val frecuencia: String,
    val horaRecordatorio: String? = null, // "HH:mm" o null si no tiene recordatorio
    val recordatorioActivo: Boolean = false
)
