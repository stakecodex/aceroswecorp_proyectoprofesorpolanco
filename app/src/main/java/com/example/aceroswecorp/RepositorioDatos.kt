package com.example.aceroswecorp

import com.google.android.gms.tasks.Task
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class RepositorioDatos(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    fun observarProductos(
        onResult: (List<Producto>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration = db.collection("productos")
        .orderBy("nombre", Query.Direction.ASCENDING)
        .addSnapshotListener { snapshot, error ->
            if (error != null) {
                onError(error)
            } else {
                onResult(snapshot?.documents.orEmpty().map(::toProducto))
            }
        }

    fun guardarProducto(producto: Producto): Task<String> {
        val collection = db.collection("productos")
        val reference = if (producto.id.isBlank()) collection.document() else collection.document(producto.id)
        val values = mapOf(
            "nombre" to producto.nombre,
            "codigo" to producto.codigo,
            "categoria" to producto.categoria,
            "unidad" to producto.unidad,
            "stock" to producto.stock,
            "precio" to producto.precio,
            "actualizadoEn" to FieldValue.serverTimestamp()
        )
        return reference.set(values).continueWith { task ->
            task.getResult()
            reference.id
        }
    }

    fun eliminarProducto(id: String): Task<Void> =
        db.collection("productos").document(id).delete()

    fun observarCotizaciones(
        vendedorUid: String?,
        onResult: (List<Cotizacion>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        val base = db.collection("cotizaciones")
        val query = if (vendedorUid == null) base else base.whereEqualTo("vendedorUid", vendedorUid)
        return query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                onError(error)
            } else {
                onResult(
                    snapshot?.documents.orEmpty()
                        .map(::toCotizacion)
                        .sortedByDescending { it.creadoEn?.seconds ?: 0L }
                )
            }
        }
    }

    fun crearCotizacion(cotizacion: Cotizacion): Task<String> {
        val reference = db.collection("cotizaciones").document()
        val values = cotizacion.toFirestoreMap().toMutableMap().apply {
            put("creadoEn", FieldValue.serverTimestamp())
            put("actualizadoEn", FieldValue.serverTimestamp())
        }
        return reference.set(values).continueWith { task ->
            task.getResult()
            reference.id
        }
    }

    fun actualizarCotizacion(id: String, values: Map<String, Any>): Task<Void> {
        val safeValues = values.toMutableMap().apply {
            put("actualizadoEn", FieldValue.serverTimestamp())
        }
        return db.collection("cotizaciones").document(id).update(safeValues)
    }

    fun eliminarCotizacion(id: String): Task<Void> =
        db.collection("cotizaciones").document(id).delete()

    fun cargarPerfil(uid: String, onResult: (PerfilUsuario?) -> Unit, onError: (Exception) -> Unit) {
        db.collection("perfiles").document(uid).get()
            .addOnSuccessListener { document ->
                onResult(
                    if (document.exists()) {
                        PerfilUsuario(
                            uid = uid,
                            nombre = document.getString("nombre").orEmpty(),
                            correo = document.getString("correo").orEmpty(),
                            rol = document.getString("rol").orEmpty(),
                            activo = document.getBoolean("activo") == true
                        )
                    } else {
                        null
                    }
                )
            }
            .addOnFailureListener(onError)
    }

    private fun toProducto(document: DocumentSnapshot) = Producto(
        id = document.id,
        nombre = document.getString("nombre").orEmpty(),
        codigo = document.getString("codigo").orEmpty(),
        categoria = document.getString("categoria").orEmpty(),
        unidad = document.getString("unidad") ?: "unidad",
        stock = document.getDouble("stock") ?: document.getLong("stock")?.toDouble() ?: 0.0,
        precio = document.getDouble("precio") ?: document.getLong("precio")?.toDouble() ?: 0.0,
        actualizadoEn = document.getTimestamp("actualizadoEn")
    )

    private fun toCotizacion(document: DocumentSnapshot): Cotizacion {
        val items = (document.get("items") as? List<*>).orEmpty().mapNotNull { raw ->
            val item = raw as? Map<*, *> ?: return@mapNotNull null
            ItemCotizacion(
                productoId = item["productoId"] as? String ?: "",
                productoNombre = item["productoNombre"] as? String ?: "",
                codigoProducto = item["codigoProducto"] as? String ?: "",
                cantidad = (item["cantidad"] as? Number)?.toDouble() ?: 0.0,
                precioUnitario = (item["precioUnitario"] as? Number)?.toDouble() ?: 0.0,
                subtotal = (item["subtotal"] as? Number)?.toDouble() ?: 0.0
            )
        }
        return Cotizacion(
            id = document.id,
            vendedorUid = document.getString("vendedorUid").orEmpty(),
            cliente = document.getString("cliente").orEmpty(),
            items = items,
            notas = document.getString("notas").orEmpty(),
            subtotal = document.getDouble("subtotal") ?: document.getLong("subtotal")?.toDouble() ?: 0.0,
            iva = document.getDouble("iva") ?: 0.19,
            total = document.getDouble("total") ?: document.getLong("total")?.toDouble() ?: 0.0,
            estado = document.getString("estado") ?: "pendiente",
            fotoUrl = document.getString("fotoUrl"),
            latitud = document.getDouble("latitud"),
            longitud = document.getDouble("longitud"),
            creadoEn = document.getTimestamp("creadoEn"),
            actualizadoEn = document.getTimestamp("actualizadoEn")
        )
    }

    private fun Cotizacion.toFirestoreMap(): Map<String, Any> = mapOf(
        "vendedorUid" to vendedorUid,
        "cliente" to cliente,
        "items" to items.map { item ->
            mapOf(
                "productoId" to item.productoId,
                "productoNombre" to item.productoNombre,
                "codigoProducto" to item.codigoProducto,
                "cantidad" to item.cantidad,
                "precioUnitario" to item.precioUnitario,
                "subtotal" to item.subtotal
            )
        },
        "notas" to notas,
        "subtotal" to subtotal,
        "iva" to iva,
        "total" to total,
        "estado" to estado
    )
}
