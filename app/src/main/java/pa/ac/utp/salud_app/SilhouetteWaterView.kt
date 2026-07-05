package pa.ac.utp.salud_app

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class SilhouetteWaterView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var percentage: Float = 0f
        set(value) {
            field = value.coerceIn(0f, 1f)
            invalidate()
        }

    var gender: String = "Mujer" // "Hombre" o "Mujer"
        set(value) {
            field = value
            invalidate()
        }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#42A5F5") // Agua
        style = Paint.Style.FILL
    }

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E0E0E0") // Gris
        style = Paint.Style.FILL
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()

        val path = Path()
        
        // Forma básica de silueta abstracta dependiendo del género
        if (gender == "Hombre") {
            // Hombros anchos, sin cadera
            path.addRoundRect(w * 0.2f, h * 0.2f, w * 0.8f, h * 0.9f, 20f, 20f, Path.Direction.CW) // Torso
            path.addCircle(w * 0.5f, h * 0.1f, w * 0.15f, Path.Direction.CW) // Cabeza
        } else {
            // Forma de reloj de arena (Mujer)
            path.moveTo(w * 0.3f, h * 0.2f)
            path.lineTo(w * 0.7f, h * 0.2f) // Hombros
            path.lineTo(w * 0.6f, h * 0.5f) // Cintura
            path.lineTo(w * 0.8f, h * 0.9f) // Cadera
            path.lineTo(w * 0.2f, h * 0.9f)
            path.lineTo(w * 0.4f, h * 0.5f)
            path.close()
            path.addCircle(w * 0.5f, h * 0.1f, w * 0.12f, Path.Direction.CW) // Cabeza
        }

        // Dibujar el fondo gris de la silueta
        canvas.drawPath(path, bgPaint)

        // Dibujar el llenado de agua usando un clip
        val waterTop = h - (h * percentage)
        canvas.save()
        canvas.clipRect(0f, waterTop, w, h)
        canvas.drawPath(path, fillPaint)
        canvas.restore()
    }
}
