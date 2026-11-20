package com.example.aceroswecorp

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView

class ProductoAdapter(context: Context) : BaseAdapter() {
    private val inflater = LayoutInflater.from(context)
    private var productos: List<Producto> = emptyList()

    fun actualizar(items: List<Producto>) {
        productos = items
        notifyDataSetChanged()
    }

    override fun getCount(): Int = productos.size

    override fun getItem(position: Int): Producto = productos[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: inflater.inflate(R.layout.row_producto, parent, false)
        val producto = getItem(position)
        view.findViewById<TextView>(R.id.product_name).text = producto.nombre
        view.findViewById<TextView>(R.id.product_details).text =
            "${producto.codigo} · ${producto.categoria} · ${producto.stock} ${producto.unidad}"
        view.findViewById<TextView>(R.id.product_price).text = formatearMoneda(producto.precio)
        return view
    }
}

class CotizacionAdapter(context: Context) : BaseAdapter() {
    private val inflater = LayoutInflater.from(context)
    private var cotizaciones: List<Cotizacion> = emptyList()

    fun actualizar(items: List<Cotizacion>) {
        cotizaciones = items
        notifyDataSetChanged()
    }

    override fun getCount(): Int = cotizaciones.size

    override fun getItem(position: Int): Cotizacion = cotizaciones[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: inflater.inflate(R.layout.row_cotizacion, parent, false)
        val cotizacion = getItem(position)
        val folio = cotizacion.id.takeLast(6).uppercase()
        val descripcion = cotizacion.items.joinToString { item ->
            "${item.productoNombre} · ${item.cantidad} ${item.codigoProducto}"
        }.ifBlank { "Sin productos" }
        view.findViewById<TextView>(R.id.quote_title).text =
            "COT-$folio · ${cotizacion.estado.replaceFirstChar(Char::uppercase)}"
        view.findViewById<TextView>(R.id.quote_client).text = cotizacion.cliente
        view.findViewById<TextView>(R.id.quote_items).text = descripcion
        view.findViewById<TextView>(R.id.quote_total).text = formatearMoneda(cotizacion.total)
        return view
    }
}
