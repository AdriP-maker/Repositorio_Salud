package pa.ac.utp.salud_app

import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.android.material.datepicker.MaterialDatePicker
import java.text.SimpleDateFormat
import java.util.*

class ModuloPresion : AppCompatActivity() {

    private lateinit var btnFecha: Button
    private lateinit var txtFecha: TextView
    private lateinit var tvHora: TextView
    private lateinit var timePicker: TimePicker
    private lateinit var npSistolica: NumberPicker
    private lateinit var npDiastolica: NumberPicker
    private lateinit var npPulso: NumberPicker
    private lateinit var rgBrazo: RadioGroup
    private lateinit var btnAnalizar: Button
    private lateinit var cardResultado: CardView
    private lateinit var txtResultado: TextView
    private lateinit var tvClasificacion: TextView
    private lateinit var tvConsejo: TextView

    private var fechaSeleccionada: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_modulo_presion)

        btnFecha       = findViewById(R.id.btnFecha)
        txtFecha       = findViewById(R.id.txtFecha)
        tvHora         = findViewById(R.id.tvHora)
        timePicker     = findViewById(R.id.timePicker)
        npSistolica    = findViewById(R.id.npSistolica)
        npDiastolica   = findViewById(R.id.npDiastolica)
        npPulso        = findViewById(R.id.npPulso)
        rgBrazo        = findViewById(R.id.rgBrazo)
        btnAnalizar    = findViewById(R.id.btnAnalizar)
        cardResultado  = findViewById(R.id.cardResultado)
        txtResultado   = findViewById(R.id.txtResultado)
        tvClasificacion = findViewById(R.id.tvClasificacion)
        tvConsejo      = findViewById(R.id.tvConsejo)

        // Configuración TimePicker en formato 12h
        timePicker.setIs24HourView(false)
        timePicker.setOnTimeChangedListener { _, hour, minute ->
            tvHora.text = formatearHora12(hour, minute)
        }

        // Hora inicial en tvHora
        val calNow = Calendar.getInstance()
        tvHora.text = formatearHora12(
            calNow.get(Calendar.HOUR_OF_DAY),
            calNow.get(Calendar.MINUTE)
        )

        // Rangos NumberPicker (clínicamente razonables)
        npSistolica.minValue  = 80;  npSistolica.maxValue  = 200; npSistolica.value  = 120
        npDiastolica.minValue = 40;  npDiastolica.maxValue = 130; npDiastolica.value =  80
        npPulso.minValue      = 40;  npPulso.maxValue      = 180; npPulso.value      =  72

        // Sin ciclo infinito: el picker no regresa al inicio al llegar al extremo
        npSistolica.wrapSelectorWheel  = false
        npDiastolica.wrapSelectorWheel = false
        npPulso.wrapSelectorWheel      = false

        // Forzar color texto oscuro en todos los pickers (reflection, API 24+)
        listOf(npSistolica, npDiastolica, npPulso).forEach { it.forzarColorTexto() }
        // El TimePicker contiene NumberPickers internos; se aplica después de layout
        timePicker.post {
            repeat(timePicker.childCount) { i ->
                val child = timePicker.getChildAt(i)
                if (child is NumberPicker) child.forzarColorTexto()
            }
        }

        btnFecha.setOnClickListener   { mostrarDatePicker() }
        btnAnalizar.setOnClickListener { analizarMedicion() }
    }

    private fun mostrarDatePicker() {
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Seleccionar fecha de medición")
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            .build()

        picker.addOnPositiveButtonClickListener { seleccion ->
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            fechaSeleccionada = sdf.format(Date(seleccion))
            txtFecha.text = "Fecha: $fechaSeleccionada"
            btnFecha.text = "Fecha: $fechaSeleccionada"
        }

        picker.show(supportFragmentManager, "MATERIAL_DATE_PICKER")
    }

    private fun analizarMedicion() {
        if (fechaSeleccionada == null) {
            Toast.makeText(this, "Debe seleccionar una fecha", Toast.LENGTH_SHORT).show()
            return
        }
        if (rgBrazo.checkedRadioButtonId == -1) {
            Toast.makeText(this, "Debe seleccionar el brazo de medición", Toast.LENGTH_SHORT).show()
            return
        }

        val sistolica  = npSistolica.value
        val diastolica = npDiastolica.value
        val pulso      = npPulso.value
        val hora       = formatearHora12(timePicker.hour, timePicker.minute)
        val brazo      = findViewById<RadioButton>(rgBrazo.checkedRadioButtonId).text

        val clasificacion = clasificarPresion(sistolica, diastolica)

        val badgeColor = when (clasificacion) {
            "Presión baja"    -> Color.parseColor("#2196F3")
            "Presión normal"  -> Color.parseColor("#4CAF50")
            "Presión elevada" -> Color.parseColor("#FF9800")
            else              -> Color.parseColor("#F44336")
        }
        val consejo = when (clasificacion) {
            "Presión baja"    -> "ⓘ  Manténgase hidratado y evite cambios bruscos de posición."
            "Presión normal"  -> "ⓘ  Su presión está en un rango saludable. Mantenga un estilo de vida activo."
            "Presión elevada" -> "ⓘ  Reduzca el consumo de sal y realice actividad física moderada."
            else              -> "ⓘ  Consulte a un médico. Evite el estrés y siga las indicaciones médicas."
        }

        txtResultado.text = """
            Fecha: $fechaSeleccionada
            Hora: $hora
            Sistólica: $sistolica mmHg         Pulso: $pulso BPM
            Diastólica: $diastolica mmHg        Brazo: $brazo
        """.trimIndent()

        val bg = GradientDrawable().apply {
            shape        = GradientDrawable.RECTANGLE
            cornerRadius = 10f * resources.displayMetrics.density
            setColor(badgeColor)
        }
        tvClasificacion.text       = clasificacion.uppercase()
        tvClasificacion.setTextColor(Color.WHITE)
        tvClasificacion.background = bg
        tvConsejo.text             = consejo
        cardResultado.visibility   = View.VISIBLE
    }

    private fun clasificarPresion(sistolica: Int, diastolica: Int): String = when {
        sistolica < 90 || diastolica < 60                    -> "Presión baja"
        sistolica in 90..119 && diastolica in 60..79         -> "Presión normal"
        sistolica in 120..129 && diastolica < 80             -> "Presión elevada"
        else                                                  -> "Hipertensión"
    }

    /** Reflection: fuerza color texto oscuro en NumberPicker (no hay API pública hasta API 29). */
    private fun NumberPicker.forzarColorTexto() {
        val textColor = Color.parseColor("#1A2D4A")
        val divColor  = Color.parseColor("#1A4375")
        try {
            val paintField = NumberPicker::class.java.getDeclaredField("mSelectorWheelPaint")
            paintField.isAccessible = true
            (paintField.get(this) as Paint).color = textColor
        } catch (_: Exception) {}
        try {
            val divField = NumberPicker::class.java.getDeclaredField("mSelectionDivider")
            divField.isAccessible = true
            divField.set(this, ColorDrawable(divColor))
        } catch (_: Exception) {}
        invalidate()
    }

    private fun formatearHora12(hour: Int, minute: Int): String {
        val h12  = when { hour == 0 -> 12; hour > 12 -> hour - 12; else -> hour }
        val amPm = if (hour < 12) "AM" else "PM"
        return String.format("%02d:%02d %s", h12, minute, amPm)
    }

}
