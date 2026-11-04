package com.example.aceroswecorp

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowCompat

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        findViewById<View>(R.id.button_save_quote).setOnClickListener {
            Toast.makeText(
                this,
                "No se guardó: Firebase todavía no está configurado.",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}

class ConsultaInventarioActivity : PantallaNavegableActivity() {
    override val layoutId = R.layout.activity_consulta_inventario
}

class MisCotizacionesActivity : PantallaNavegableActivity() {
    override val layoutId = R.layout.activity_mis_cotizaciones

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        findViewById<Button>(R.id.filter_pending).setOnClickListener {
            showQuotes("pending")
        }
        findViewById<Button>(R.id.filter_approved).setOnClickListener {
            showQuotes("approved")
        }
        findViewById<Button>(R.id.filter_rejected).setOnClickListener {
            showQuotes("rejected")
        }
        showQuotes("pending")
    }

    private fun showQuotes(status: String) {
        val quoteGroups = mapOf(
            "pending" to listOf(R.id.quote_pending_42, R.id.quote_pending_39),
            "approved" to listOf(R.id.quote_approved_41),
            "rejected" to listOf(R.id.quote_rejected_40)
        )
        quoteGroups.values.flatten().forEach { quoteId ->
            findViewById<View>(quoteId).visibility =
                if (quoteId in quoteGroups.getValue(status)) View.VISIBLE else View.GONE
        }
        val selectedColor = Color.parseColor("#D95708")
        val defaultColor = Color.parseColor("#1B252E")
        mapOf(
            "pending" to R.id.filter_pending,
            "approved" to R.id.filter_approved,
            "rejected" to R.id.filter_rejected
        ).forEach { (key, buttonId) ->
            findViewById<Button>(buttonId).backgroundTintList =
                android.content.res.ColorStateList.valueOf(
                    if (key == status) selectedColor else defaultColor
                )
        }
    }
}

class HistorialCotizacionesActivity : PantallaNavegableActivity() {
    override val layoutId = R.layout.activity_historial_cotizaciones

    override fun pantallaInicio(): Class<out AppCompatActivity> =
        MenuAdministradorActivity::class.java

    override fun pantallaCotizaciones(): Class<out AppCompatActivity> =
        HistorialCotizacionesActivity::class.java
}

class HistorialInventarioActivity : PantallaNavegableActivity() {
    override val layoutId = R.layout.activity_historial_inventario

    override fun pantallaInicio(): Class<out AppCompatActivity> =
        MenuAdministradorActivity::class.java

    override fun pantallaInventario(): Class<out AppCompatActivity> =
        HistorialInventarioActivity::class.java
}

class VentasDocumentosActivity : PantallaNavegableActivity() {
    override val layoutId = R.layout.activity_ventas_documentos

    override fun pantallaInicio(): Class<out AppCompatActivity> =
        MenuAdministradorActivity::class.java

    override fun pantallaCotizaciones(): Class<out AppCompatActivity> =
        HistorialCotizacionesActivity::class.java

    override fun pantallaInventario(): Class<out AppCompatActivity> =
        HistorialInventarioActivity::class.java
}
