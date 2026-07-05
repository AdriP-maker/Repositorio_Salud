package pa.ac.utp.salud_app

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import java.util.Locale

data class RegistroPeso(
    val fecha: String,
    val peso: Double,
    val imc: Double
)

class HistorialPesoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_historial_peso)

        val prefs = getSharedPreferences("salud_app_prefs", MODE_PRIVATE)
        val jsonStr = prefs.getString("historial_peso", "[]") ?: "[]"
        val array = JSONArray(jsonStr)
        val listaRegistros = (array.length() - 1 downTo 0).map { i ->
            val obj = array.getJSONObject(i)
            RegistroPeso(
                obj.getString("fecha"),
                obj.getDouble("peso"),
                obj.getDouble("imc")
            )
        }

        val lvHistorial = findViewById<ListView>(R.id.lvHistorial)
        val adapter = PesoAdapter(this, listaRegistros)
        lvHistorial.adapter = adapter
    }
}

class PesoAdapter(
    private val context: Context,
    private val data: List<RegistroPeso>
) : BaseAdapter() {

    override fun getCount(): Int = data.size
    override fun getItem(position: Int): Any = data[position]
    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View = convertView
            ?: LayoutInflater.from(context).inflate(R.layout.item_historial_peso, parent, false)

        val item = data[position]

        val ivIcono  = view.findViewById<ImageView>(R.id.ivIconoIMC)
        val tvFecha  = view.findViewById<TextView>(R.id.tvFecha)
        val tvPesoIMC = view.findViewById<TextView>(R.id.tvPesoIMC)

        tvFecha.text = "Fecha: ${item.fecha}"
        tvPesoIMC.text = String.format(Locale.getDefault(), "Peso: %.1f kg | IMC: %.1f", item.peso, item.imc)

        val nombreIcono = when {
            item.imc < 18.5 -> "bajopeso"
            item.imc < 25.0 -> "normal"
            item.imc < 30.0 -> "sobrepeso"
            item.imc < 35.0 -> "obesidad1"
            item.imc < 40.0 -> "obesidad2"
            else             -> "obesidad3"
        }

        val resId = context.resources.getIdentifier(nombreIcono, "drawable", context.packageName)
        if (resId != 0) {
            ivIcono.setImageResource(resId)
        } else {
            ivIcono.setImageResource(android.R.drawable.ic_menu_info_details)
        }

        return view
    }
}
