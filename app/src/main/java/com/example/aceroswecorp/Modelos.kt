package com.example.aceroswecorp

import com.google.firebase.Timestamp

data class PerfilUsuario(
    val uid: String = "",
    val nombre: String = "",
    val correo: String = "",
    val rol: String = "",
    val activo: Boolean = true
)

data class Producto(
    val id: String = "",
    val nombre: String = "",
    val codigo: String = "",
    val categoria: String = "",
    val unidad: String = "unidad",
    val stock: Double = 0.0,
    val precio: Double = 0.0,
    val actualizadoEn: Timestamp? = null
) {
    override fun toString(): String = "$nombre ($codigo) · ${formatearMoneda(precio)}"
}

data class ItemCotizacion(
    val productoId: String = "",
    val productoNombre: String = "",
    val codigoProducto: String = "",
    val cantidad: Double = 0.0,
    val precioUnitario: Double = 0.0,
    val subtotal: Double = 0.0
)

data class Cotizacion(
    val id: String = "",
    val vendedorUid: String = "",
    val cliente: String = "",
    val items: List<ItemCotizacion> = emptyList(),
    val notas: String = "",
    val subtotal: Double = 0.0,
    val iva: Double = 0.19,
    val total: Double = 0.0,
    val estado: String = "pendiente",
    val fotoUrl: String? = null,
    val latitud: Double? = null,
    val longitud: Double? = null,
    val creadoEn: Timestamp? = null,
    val actualizadoEn: Timestamp? = null
)

fun formatearMoneda(valor: Double): String =
    java.text.NumberFormat.getCurrencyInstance(java.util.Locale.forLanguageTag("es-CL"))
        .format(valor)
