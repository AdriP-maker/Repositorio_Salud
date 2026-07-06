package pa.ac.utp.salud_app

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class HistorialPesoActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var lvHistorial: ListView
    private lateinit var adapter: PesoAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_historial_peso)

        dbHelper = DatabaseHelper.getInstance(this)
        lvHistorial = findViewById(R.id.lvHistorial)

        cargarHistorial()

        // Long-click sobre un registro para poder borrarlo (SQL: DELETE FROM peso WHERE id=?)
        lvHistorial.setOnItemLongClickListener { _, _, position, _ ->
            val registro = adapter.getItem(position) as RegistroPeso
            confirmarEliminacion(registro)
            true
        }
    }

    private fun cargarHistorial() {
        // SELECT * FROM peso ORDER BY id DESC (ya viene ordenado desde DatabaseHelper)
        val listaRegistros = dbHelper.obtenerHistorialPeso()
        adapter = PesoAdapter(this, listaRegistros)
        lvHistorial.adapter = adapter
    }

    private fun confirmarEliminacion(registro: RegistroPeso) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar registro")
            .setMessage("¿Eliminar el registro del ${registro.fecha}?")
            .setPositiveButton("Eliminar") { _, _ ->
                dbHelper.eliminarPeso(registro.id)
                Toast.makeText(this, "Registro eliminado", Toast.LENGTH_SHORT).show()
                cargarHistorial()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}

class PesoAdapter(
    private val context: Context,
    private val data: List<RegistroPeso>
) : BaseAdapter() {

    override fun getCount(): Int = data.size
    override fun getItem(position: Int): Any = data[position]
    override fun getItemId(position: Int): Long = data[position].id.toLong()

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
