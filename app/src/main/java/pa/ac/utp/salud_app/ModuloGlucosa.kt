package pa.ac.utp.salud_app

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*

class ModuloGlucosa : AppCompatActivity() {

    private lateinit var tvFechaHoraRegistro: TextView
    private lateinit var etGlucosa: EditText
    private lateinit var etNotas: EditText
    private lateinit var headerNotas: LinearLayout
    private lateinit var tvArrowNotas: TextView
    private lateinit var btnGuardarRegistro: Button
    private lateinit var tvResumenPrevio: TextView

    private lateinit var rowAyunas: LinearLayout
    private lateinit var rowAntesAlmuerzo: LinearLayout
    private lateinit var rowDespuesAlmuerzo: LinearLayout
    private lateinit var rowCena: LinearLayout

    private var tipoSeleccionado: String = ""
    private var notasExpandidas: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_modulo_glucosa)

        tvFechaHoraRegistro = findViewById(R.id.tvFechaHoraRegistro)
        etGlucosa           = findViewById(R.id.etGlucosa)
        etNotas             = findViewById(R.id.etNotas)
        headerNotas         = findViewById(R.id.headerNotas)
        tvArrowNotas        = findViewById(R.id.tvArrowNotas)
        btnGuardarRegistro  = findViewById(R.id.btnGuardarRegistro)
        tvResumenPrevio     = findViewById(R.id.tvResumenPrevio)
        rowAyunas           = findViewById(R.id.rowAyunas)
        rowAntesAlmuerzo    = findViewById(R.id.rowAntesAlmuerzo)
        rowDespuesAlmuerzo  = findViewById(R.id.rowDespuesAlmuerzo)
        rowCena             = findViewById(R.id.rowCena)

        actualizarFechaHora()

        rowAyunas.setOnClickListener          { seleccionarTipo(rowAyunas,          "Ayunas")              }
        rowAntesAlmuerzo.setOnClickListener   { seleccionarTipo(rowAntesAlmuerzo,   "Antes de Almuerzo")   }
        rowDespuesAlmuerzo.setOnClickListener { seleccionarTipo(rowDespuesAlmuerzo, "Después de Almuerzo") }
        rowCena.setOnClickListener            { seleccionarTipo(rowCena,            "Cena")                }

        headerNotas.setOnClickListener        { toggleNotas() }
        btnGuardarRegistro.setOnClickListener { guardarRegistro() }
    }

    private fun actualizarFechaHora() {
        val cal    = Calendar.getInstance()
        val locale = Locale.forLanguageTag("es-PA")
        val dia    = SimpleDateFormat("EEEE", locale).format(cal.time)
            .replaceFirstChar { it.uppercase() }
        val fecha  = SimpleDateFormat("d 'de' MMMM", locale).format(cal.time)
        val hora   = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(cal.time)
        
        val recordText = getString(R.string.record_today_loading)
            .replace("Cargando&#8230;", "$dia, $fecha | $hora")
            .replace("Cargando...", "$dia, $fecha | $hora")
            
        tvFechaHoraRegistro.text = recordText
    }

    private fun seleccionarTipo(rowSeleccionada: LinearLayout, tipo: String) {
        val filas = listOf(rowAyunas, rowAntesAlmuerzo, rowDespuesAlmuerzo, rowCena)
        val dp    = resources.displayMetrics.density

        filas.forEach { fila ->
            val bg = GradientDrawable().apply {
                shape        = GradientDrawable.RECTANGLE
                cornerRadius = 8f * dp
                setColor(if (fila === rowSeleccionada) 0xFFD0E4FF.toInt() else 0xFFF0F4F8.toInt())
            }
            fila.background = bg
        }

        tipoSeleccionado = tipo
    }

    private fun toggleNotas() {
        notasExpandidas     = !notasExpandidas
        etNotas.visibility  = if (notasExpandidas) View.VISIBLE else View.GONE
        tvArrowNotas.text   = if (notasExpandidas) getString(R.string.arrow_up) else getString(R.string.arrow_down)
    }

    private fun guardarRegistro() {
        val valorGlucosaStr = etGlucosa.text.toString().trim()
        val notas           = etNotas.text.toString().trim()

        if (valorGlucosaStr.isEmpty()) {
            Toast.makeText(this, "Ingrese un valor de glucosa", Toast.LENGTH_SHORT).show()
            return
        }

        val valorGlucosa = valorGlucosaStr.toDoubleOrNull()
        if (valorGlucosa == null || valorGlucosa <= 0 || valorGlucosa > 600) {
            Toast.makeText(this, "Ingrese un valor de glucosa válido (entre 1 y 600)", Toast.LENGTH_SHORT).show()
            return
        }

        if (tipoSeleccionado.isEmpty()) {
            Toast.makeText(this, "Seleccione el tipo de registro", Toast.LENGTH_SHORT).show()
            return
        }

        val notasTexto = notas.ifEmpty { "Sin notas" }
        val horaActual = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
        
        // Actualizar el resumen previo en la UI
        tvResumenPrevio.text = "Última lectura: $valorGlucosa mg/dL\n($tipoSeleccionado; $horaActual)"

        val mensaje    = "Registro guardado:\n$valorGlucosa mg/dL\nTipo: $tipoSeleccionado\nNotas: $notasTexto"
        Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show()

        // Limpiar campos después de guardar
        etGlucosa.setText("")
        etNotas.setText("")
        tipoSeleccionado = ""
        restablecerColoresFilas()
    }

    private fun restablecerColoresFilas() {
        val filas = listOf(rowAyunas, rowAntesAlmuerzo, rowDespuesAlmuerzo, rowCena)
        val dp    = resources.displayMetrics.density
        filas.forEach { fila ->
            val bg = GradientDrawable().apply {
                shape        = GradientDrawable.RECTANGLE
                cornerRadius = 8f * dp
                setColor(0xFFF0F4F8.toInt())
            }
            fila.background = bg
        }
    }
}
