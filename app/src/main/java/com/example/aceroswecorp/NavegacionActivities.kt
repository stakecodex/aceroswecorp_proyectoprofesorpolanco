package com.example.aceroswecorp

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.LayoutInflater
import android.widget.Button
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ListView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration

internal fun AppCompatActivity.applySystemBarInsets() {
    WindowCompat.setDecorFitsSystemWindows(window, false)
    val root = findViewById<View>(R.id.screen_root)
    val initialLeft = root.paddingLeft
    val initialTop = root.paddingTop
    val initialRight = root.paddingRight
    val initialBottom = root.paddingBottom

    ViewCompat.setOnApplyWindowInsetsListener(root) { view, windowInsets ->
        val systemBars = windowInsets.getInsets(
            WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
        )
        view.setPadding(
            initialLeft + systemBars.left,
            initialTop + systemBars.top,
            initialRight + systemBars.right,
            initialBottom + systemBars.bottom
        )
        windowInsets
    }
    ViewCompat.requestApplyInsets(root)
}

abstract class PantallaNavegableActivity : AppCompatActivity() {
    protected abstract val layoutId: Int

    protected open fun pantallaInicio(): Class<out AppCompatActivity> =
        MenuPrincipal::class.java

    protected open fun pantallaInventario(): Class<out AppCompatActivity> =
        ConsultaInventarioActivity::class.java

    protected open fun pantallaCotizaciones(): Class<out AppCompatActivity> =
        MisCotizacionesActivity::class.java

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layoutId)
        applySystemBarInsets()

        findViewById<View?>(R.id.screen_back)?.setOnClickListener { finish() }
        bindNavigation(R.id.nav_inicio, pantallaInicio())
        findViewById<View?>(R.id.nav_clientes)?.setOnClickListener {
            Toast.makeText(
                this,
                "La gestión de clientes se habilitará en otro hito.",
                Toast.LENGTH_SHORT
            ).show()
        }
        bindNavigation(R.id.nav_inventario, pantallaInventario())
        bindNavigation(R.id.nav_cotizaciones, pantallaCotizaciones())
        bindNavigation(R.id.nav_reportes, VentasDocumentosActivity::class.java)
        bindNavigation(R.id.nav_ventas, VentasDocumentosActivity::class.java)
        findViewById<View?>(R.id.nav_usuarios)?.setOnClickListener {
            Toast.makeText(
                this,
                "La gestión de usuarios se habilitará en otro hito.",
                Toast.LENGTH_SHORT
            ).show()
        }

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            RepositorioDatos().cargarPerfil(
                uid,
                onResult = { perfil ->
                    if (perfil == null || !perfil.activo) {
                        FirebaseAuth.getInstance().signOut()
                        mostrarError("Tu perfil no está disponible o está desactivado.")
                        volverAlLogin()
                    } else {
                        findViewById<TextView>(R.id.header_subtitle).text =
                            "${perfil.nombre} · ${perfil.rol.replaceFirstChar(Char::uppercase)}"
                    }
                },
                onError = { error ->
                    Toast.makeText(
                        this,
                        "No se pudo cargar el perfil: ${error.localizedMessage ?: "error de conexión"}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            )
        }
    }

    override fun onStart() {
        super.onStart()
        if (FirebaseAuth.getInstance().currentUser == null) {
            volverAlLogin()
        }
    }

    protected fun configurarCerrarSesion() {
        findViewById<View>(R.id.screen_back).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Cerrar sesión")
                .setMessage("¿Quieres cerrar tu sesión?")
                .setPositiveButton("Cerrar sesión") { _, _ ->
                    FirebaseAuth.getInstance().signOut()
                    volverAlLogin()
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }

    private fun volverAlLogin() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    protected fun bindNavigation(viewId: Int, destination: Class<out AppCompatActivity>) {
        findViewById<View?>(viewId)?.setOnClickListener {
            if (destination != javaClass) {
                startActivity(Intent(this, destination))
            }
        }
    }
}

class MenuPrincipal : PantallaNavegableActivity() {
    override val layoutId = R.layout.activity_menu_ventas

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configurarCerrarSesion()
        bindNavigation(R.id.button_new_quote, NuevaCotizacionActivity::class.java)
        bindNavigation(R.id.button_inventory, ConsultaInventarioActivity::class.java)
        bindNavigation(R.id.button_my_quotes, MisCotizacionesActivity::class.java)
        bindNavigation(R.id.button_clients, NuevaCotizacionActivity::class.java)
    }
}

class MenuAdministradorActivity : PantallaNavegableActivity() {
    override val layoutId = R.layout.activity_menu_administrador

    override fun pantallaInicio(): Class<out AppCompatActivity> =
        MenuAdministradorActivity::class.java

    override fun pantallaInventario(): Class<out AppCompatActivity> =
        HistorialInventarioActivity::class.java

    override fun pantallaCotizaciones(): Class<out AppCompatActivity> =
        HistorialCotizacionesActivity::class.java

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configurarCerrarSesion()
        bindNavigation(R.id.admin_quotes, HistorialCotizacionesActivity::class.java)
        bindNavigation(R.id.admin_reports, VentasDocumentosActivity::class.java)
        findViewById<View>(R.id.admin_users).setOnClickListener {
            Toast.makeText(
                this,
                "La gestión de usuarios se habilitará en otro hito.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}

class NuevaCotizacionActivity : PantallaNavegableActivity() {
    override val layoutId = R.layout.activity_nueva_cotizacion

    private val repositorio by lazy { RepositorioDatos() }
    private var productsListener: ListenerRegistration? = null
    private lateinit var productSpinner: Spinner
    private lateinit var productsAdapter: ArrayAdapter<Producto>
    private lateinit var quantityInput: EditText
    private lateinit var totalText: TextView
    private lateinit var finalTotalText: TextView
    private var saving = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        productSpinner = findViewById(R.id.spinner_quote_products)
        quantityInput = findViewById(R.id.input_quote_quantity)
        totalText = findViewById(R.id.quote_product_total)
        finalTotalText = findViewById(R.id.quote_final_total)
        productsAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            mutableListOf<Producto>()
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        productSpinner.adapter = productsAdapter

        productSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = actualizarTotal()
            override fun onItemSelected(
                parent: android.widget.AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) = actualizarTotal()
        }
        quantityInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) =
                actualizarTotal()
            override fun afterTextChanged(s: Editable?) = Unit
        })

        productsListener = repositorio.observarProductos(
            onResult = { items ->
                productsAdapter.clear()
                productsAdapter.addAll(items)
                findViewById<Button>(R.id.button_save_quote).isEnabled = items.isNotEmpty()
                if (items.isEmpty()) {
                    totalText.text = "Agrega productos al inventario antes de cotizar."
                    finalTotalText.text = "TOTAL CON IVA"
                } else {
                    actualizarTotal()
                }
            },
            onError = { error ->
                mostrarError("No se pudo cargar el catálogo de productos.", error)
            }
        )

        findViewById<View>(R.id.button_save_quote).setOnClickListener {
            guardarCotizacion()
        }
    }

    override fun onDestroy() {
        productsListener?.remove()
        super.onDestroy()
    }

    private fun actualizarTotal() {
        val producto = productSpinner.selectedItem as? Producto ?: return
        val cantidad = quantityInput.text.toString().toDoubleOrNull() ?: 0.0
        val subtotal = producto.precio * cantidad
        totalText.text = "${producto.nombre} · ${formatearMoneda(subtotal)} + IVA " +
            "(${formatearMoneda(subtotal * 1.19)})"
        finalTotalText.text = "TOTAL CON IVA · ${formatearMoneda(subtotal * 1.19)}"
    }

    private fun guardarCotizacion() {
        if (saving) return
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        val cliente = findViewById<EditText>(R.id.input_quote_client).text.toString().trim()
        val cantidad = quantityInput.text.toString().toDoubleOrNull()
        val producto = productSpinner.selectedItem as? Producto
        if (uid == null) {
            mostrarError("Tu sesión expiró. Inicia sesión nuevamente.")
            return
        }
        if (cliente.isBlank()) {
            mostrarError("Ingresa el nombre del cliente.")
            return
        }
        if (producto == null || cantidad == null || cantidad <= 0.0) {
            mostrarError("Selecciona un producto e ingresa una cantidad mayor que cero.")
            return
        }

        val subtotal = producto.precio * cantidad
        val item = ItemCotizacion(
            productoId = producto.id,
            productoNombre = producto.nombre,
            codigoProducto = producto.codigo,
            cantidad = cantidad,
            precioUnitario = producto.precio,
            subtotal = subtotal
        )
        val cotizacion = Cotizacion(
            vendedorUid = uid,
            cliente = cliente,
            items = listOf(item),
            notas = findViewById<EditText>(R.id.input_quote_notes).text.toString().trim(),
            subtotal = subtotal,
            iva = 0.19,
            total = subtotal * 1.19
        )
        saving = true
        findViewById<Button>(R.id.button_save_quote).isEnabled = false
        repositorio.crearCotizacion(cotizacion)
            .addOnSuccessListener {
                Toast.makeText(this, "Cotización guardada.", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { error ->
                saving = false
                findViewById<Button>(R.id.button_save_quote).isEnabled = true
                mostrarError("No se pudo guardar la cotización.", error)
            }
    }
}

class ConsultaInventarioActivity : PantallaNavegableActivity() {
    override val layoutId = R.layout.activity_consulta_inventario

    private val repositorio by lazy { RepositorioDatos() }
    private var productos: List<Producto> = emptyList()
    private var productsListener: ListenerRegistration? = null
    private lateinit var adapter: ProductoAdapter
    private var categoriaSeleccionada = "Todas las categorías"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adapter = ProductoAdapter(this)
        val list = findViewById<ListView>(R.id.list_inventory)
        val empty = findViewById<TextView>(R.id.inventory_empty)
        val search = findViewById<EditText>(R.id.input_inventory_search)
        val categorySpinner = findViewById<Spinner>(R.id.spinner_categories)
        list.adapter = adapter
        list.emptyView = empty
        empty.text = "No hay productos para mostrar."

        search.addTextChangedListener(simpleTextWatcher { filtrar(search.text.toString()) })
        categorySpinner.onItemSelectedListener =
            object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
                override fun onItemSelected(
                    parent: android.widget.AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    categoriaSeleccionada = parent?.getItemAtPosition(position)?.toString()
                        ?: "Todas las categorías"
                    filtrar(search.text.toString())
                }
            }
        list.setOnItemClickListener { _, _, position, _ ->
            val producto = adapter.getItem(position)
            AlertDialog.Builder(this)
                .setTitle(producto.nombre)
                .setMessage(
                    "${producto.codigo}\n${producto.stock} ${producto.unidad} disponibles\n" +
                        formatearMoneda(producto.precio)
                )
                .setPositiveButton("Cerrar", null)
                .show()
        }
        productsListener = repositorio.observarProductos(
            onResult = { items ->
                productos = items
                val categories = listOf("Todas las categorías") +
                    items.map { it.categoria }.filter(String::isNotBlank).distinct().sorted()
                val current = categorySpinner.selectedItem?.toString()
                categorySpinner.adapter = ArrayAdapter(
                    this,
                    android.R.layout.simple_spinner_item,
                    categories
                ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
                val index = categories.indexOf(current).takeIf { it >= 0 } ?: 0
                categorySpinner.setSelection(index)
                filtrar(search.text.toString())
                empty.text = if (items.isEmpty()) "El inventario aún no tiene productos." else "No hay resultados."
            },
            onError = { error -> mostrarError("No se pudo cargar el inventario.", error) }
        )
    }

    override fun onDestroy() {
        productsListener?.remove()
        super.onDestroy()
    }

    private fun filtrar(texto: String) {
        val term = texto.trim().lowercase()
        adapter.actualizar(productos.filter { producto ->
            val matchesText = term.isBlank() ||
                producto.nombre.lowercase().contains(term) ||
                producto.codigo.lowercase().contains(term)
            val matchesCategory = categoriaSeleccionada == "Todas las categorías" ||
                producto.categoria == categoriaSeleccionada
            matchesText && matchesCategory
        })
    }
}

class MisCotizacionesActivity : PantallaNavegableActivity() {
    override val layoutId = R.layout.activity_mis_cotizaciones

    private val repositorio by lazy { RepositorioDatos() }
    private var quotesListener: ListenerRegistration? = null
    private var cotizaciones: List<Cotizacion> = emptyList()
    private lateinit var adapter: CotizacionAdapter
    private var estadoSeleccionado = "pendiente"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adapter = CotizacionAdapter(this)
        val list = findViewById<ListView>(R.id.list_quotes)
        val empty = findViewById<TextView>(R.id.quotes_empty)
        list.adapter = adapter
        list.emptyView = empty
        empty.text = "No tienes cotizaciones en este estado."
        findViewById<Button>(R.id.filter_pending).setOnClickListener {
            showQuotes("pendiente")
        }
        findViewById<Button>(R.id.filter_approved).setOnClickListener {
            showQuotes("aprobada")
        }
        findViewById<Button>(R.id.filter_rejected).setOnClickListener {
            showQuotes("rechazada")
        }
        showQuotes("pendiente")
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        quotesListener = repositorio.observarCotizaciones(
            vendedorUid = uid,
            onResult = { items ->
                cotizaciones = items
                filtrarCotizaciones()
            },
            onError = { error -> mostrarError("No se pudieron cargar tus cotizaciones.", error) }
        )
        list.setOnItemClickListener { _, _, position, _ ->
            mostrarEdicionNotas(adapter.getItem(position))
        }
    }

    override fun onDestroy() {
        quotesListener?.remove()
        super.onDestroy()
    }

    private fun showQuotes(status: String) {
        estadoSeleccionado = status
        val selectedColor = Color.parseColor("#D95708")
        val defaultColor = Color.parseColor("#1B252E")
        mapOf(
            "pendiente" to R.id.filter_pending,
            "aprobada" to R.id.filter_approved,
            "rechazada" to R.id.filter_rejected
        ).forEach { (key, buttonId) ->
            findViewById<Button>(buttonId).backgroundTintList =
                android.content.res.ColorStateList.valueOf(
                    if (key == status) selectedColor else defaultColor
                )
        }
        filtrarCotizaciones()
    }

    private fun filtrarCotizaciones() {
        adapter.actualizar(cotizaciones.filter { it.estado.equals(estadoSeleccionado, ignoreCase = true) })
    }

    private fun mostrarEdicionNotas(cotizacion: Cotizacion) {
        val input = EditText(this).apply {
            setText(cotizacion.notas)
            hint = "Notas de la cotización"
            minLines = 3
            gravity = android.view.Gravity.TOP
        }
        AlertDialog.Builder(this)
            .setTitle("Editar notas")
            .setView(input)
            .setPositiveButton("Guardar") { _, _ ->
                repositorio.actualizarCotizacion(
                    cotizacion.id,
                    mapOf("notas" to input.text.toString().trim())
                ).addOnSuccessListener {
                    Toast.makeText(this, "Cotización actualizada.", Toast.LENGTH_SHORT).show()
                }.addOnFailureListener { error ->
                    mostrarError("No se pudieron guardar los cambios.", error)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}

class HistorialCotizacionesActivity : PantallaNavegableActivity() {
    override val layoutId = R.layout.activity_historial_cotizaciones

    private val repositorio by lazy { RepositorioDatos() }
    private var quotesListener: ListenerRegistration? = null
    private var cotizaciones: List<Cotizacion> = emptyList()
    private lateinit var adapter: CotizacionAdapter

    override fun pantallaInicio(): Class<out AppCompatActivity> =
        MenuAdministradorActivity::class.java

    override fun pantallaCotizaciones(): Class<out AppCompatActivity> =
        HistorialCotizacionesActivity::class.java

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adapter = CotizacionAdapter(this)
        val list = findViewById<ListView>(R.id.admin_quotes_list)
        val empty = findViewById<TextView>(R.id.admin_quotes_empty)
        val search = findViewById<EditText>(R.id.input_admin_quotes_search)
        list.adapter = adapter
        list.emptyView = empty
        empty.text = "No hay cotizaciones para mostrar."
        search.addTextChangedListener(simpleTextWatcher { filtrarCotizaciones(search.text.toString()) })
        list.setOnItemClickListener { _, _, position, _ ->
            mostrarOpcionesCotizacion(adapter.getItem(position))
        }
        quotesListener = repositorio.observarCotizaciones(
            vendedorUid = null,
            onResult = { items ->
                cotizaciones = items
                findViewById<TextView>(R.id.admin_quotes_total).text =
                    "${formatearMoneda(items.sumOf(Cotizacion::total))}\nTotal cotizado"
                findViewById<TextView>(R.id.admin_quotes_count).text =
                    "${items.size}\nCotizaciones"
                filtrarCotizaciones(search.text.toString())
            },
            onError = { error -> mostrarError("No se pudo cargar el historial de cotizaciones.", error) }
        )
    }

    override fun onDestroy() {
        quotesListener?.remove()
        super.onDestroy()
    }

    private fun filtrarCotizaciones(texto: String) {
        val term = texto.trim().lowercase()
        adapter.actualizar(cotizaciones.filter {
            term.isBlank() || it.cliente.lowercase().contains(term) ||
                it.id.lowercase().contains(term) || it.vendedorUid.lowercase().contains(term)
        })
    }

    private fun mostrarOpcionesCotizacion(cotizacion: Cotizacion) {
        AlertDialog.Builder(this)
            .setTitle("COT-${cotizacion.id.takeLast(6).uppercase()}")
            .setItems(arrayOf("Cambiar estado", "Eliminar")) { _, choice ->
                if (choice == 0) cambiarEstado(cotizacion) else confirmarEliminar(cotizacion)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun cambiarEstado(cotizacion: Cotizacion) {
        val estados = arrayOf("pendiente", "aprobada", "rechazada")
        val selected = estados.indexOf(cotizacion.estado).coerceAtLeast(0)
        AlertDialog.Builder(this)
            .setTitle("Estado de la cotización")
            .setSingleChoiceItems(estados, selected, null)
            .setPositiveButton("Guardar") { dialog, _ ->
                val choice = (dialog as AlertDialog).listView.checkedItemPosition
                val nuevoEstado = estados.getOrElse(choice) { estados[0] }
                repositorio.actualizarCotizacion(
                    cotizacion.id,
                    mapOf("estado" to nuevoEstado)
                ).addOnFailureListener { error ->
                    mostrarError("No se pudo cambiar el estado.", error)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun confirmarEliminar(cotizacion: Cotizacion) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar cotización")
            .setMessage("¿Eliminar la cotización de ${cotizacion.cliente}? Esta acción no se puede deshacer.")
            .setPositiveButton("Eliminar") { _, _ ->
                repositorio.eliminarCotizacion(cotizacion.id)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Cotización eliminada.", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { error ->
                        mostrarError("No se pudo eliminar la cotización.", error)
                    }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}

class HistorialInventarioActivity : PantallaNavegableActivity() {
    override val layoutId = R.layout.activity_historial_inventario

    private val repositorio by lazy { RepositorioDatos() }
    private var productsListener: ListenerRegistration? = null
    private var productos: List<Producto> = emptyList()
    private lateinit var adapter: ProductoAdapter

    override fun pantallaInicio(): Class<out AppCompatActivity> =
        MenuAdministradorActivity::class.java

    override fun pantallaInventario(): Class<out AppCompatActivity> =
        HistorialInventarioActivity::class.java

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adapter = ProductoAdapter(this)
        val list = findViewById<ListView>(R.id.admin_inventory_list)
        val empty = findViewById<TextView>(R.id.admin_inventory_empty)
        val search = findViewById<EditText>(R.id.input_admin_inventory_search)
        list.adapter = adapter
        list.emptyView = empty
        empty.text = "El inventario aún no tiene productos."
        search.addTextChangedListener(simpleTextWatcher { filtrarProductos(search.text.toString()) })
        findViewById<Button>(R.id.button_add_product).setOnClickListener {
            mostrarFormularioProducto(null)
        }
        list.setOnItemClickListener { _, _, position, _ ->
            mostrarFormularioProducto(adapter.getItem(position))
        }
        list.setOnItemLongClickListener { _, _, position, _ ->
            confirmarEliminarProducto(adapter.getItem(position))
            true
        }
        productsListener = repositorio.observarProductos(
            onResult = { items ->
                productos = items
                filtrarProductos(search.text.toString())
            },
            onError = { error -> mostrarError("No se pudo cargar el inventario.", error) }
        )
    }

    override fun onDestroy() {
        productsListener?.remove()
        super.onDestroy()
    }

    private fun filtrarProductos(texto: String) {
        val term = texto.trim().lowercase()
        adapter.actualizar(productos.filter {
            term.isBlank() || it.nombre.lowercase().contains(term) || it.codigo.lowercase().contains(term)
        })
    }

    private fun mostrarFormularioProducto(existing: Producto?) {
        val form = LayoutInflater.from(this).inflate(R.layout.dialog_producto, null)
        val name = form.findViewById<EditText>(R.id.input_product_name)
        val code = form.findViewById<EditText>(R.id.input_product_code)
        val category = form.findViewById<EditText>(R.id.input_product_category)
        val unit = form.findViewById<EditText>(R.id.input_product_unit)
        val stock = form.findViewById<EditText>(R.id.input_product_stock)
        val price = form.findViewById<EditText>(R.id.input_product_price)
        if (existing != null) {
            name.setText(existing.nombre)
            code.setText(existing.codigo)
            category.setText(existing.categoria)
            unit.setText(existing.unidad)
            stock.setText(existing.stock.toString())
            price.setText(existing.precio.toString())
        } else {
            unit.setText("pieza")
        }
        val dialog = AlertDialog.Builder(this)
            .setTitle(if (existing == null) "Nuevo producto" else "Editar producto")
            .setView(form)
            .setPositiveButton("Guardar", null)
            .setNegativeButton("Cancelar", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val productName = name.text.toString().trim()
                val productCode = code.text.toString().trim()
                val productStock = stock.text.toString().toDoubleOrNull()
                val productPrice = price.text.toString().toDoubleOrNull()
                if (productName.isBlank() || productCode.isBlank() ||
                    category.text.toString().isBlank() || productStock == null ||
                    productStock < 0 || productPrice == null || productPrice < 0
                ) {
                    mostrarError("Completa los campos y usa valores numéricos no negativos para existencia y precio.")
                    return@setOnClickListener
                }
                val product = Producto(
                    id = existing?.id.orEmpty(),
                    nombre = productName,
                    codigo = productCode,
                    categoria = category.text.toString().trim(),
                    unidad = unit.text.toString().trim().ifBlank { "unidad" },
                    stock = productStock,
                    precio = productPrice
                )
                repositorio.guardarProducto(product)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Producto guardado.", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    }
                    .addOnFailureListener { error ->
                        mostrarError("No se pudo guardar el producto.", error)
                    }
            }
        }
        dialog.show()
    }

    private fun confirmarEliminarProducto(producto: Producto) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar producto")
            .setMessage("¿Eliminar ${producto.nombre} del inventario?")
            .setPositiveButton("Eliminar") { _, _ ->
                repositorio.eliminarProducto(producto.id)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Producto eliminado.", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { error ->
                        mostrarError("No se pudo eliminar el producto.", error)
                    }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}

private fun simpleTextWatcher(onTextChanged: () -> Unit): TextWatcher =
    object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) =
            onTextChanged()
        override fun afterTextChanged(s: Editable?) = Unit
    }