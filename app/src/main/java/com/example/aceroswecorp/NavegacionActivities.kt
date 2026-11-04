package com.example.aceroswecorp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

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

        findViewById<View?>(R.id.screen_back)?.setOnClickListener { finish() }
        bindNavigation(R.id.nav_inicio, pantallaInicio())
        bindNavigation(R.id.nav_clientes, NuevaCotizacionActivity::class.java)
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
}

class ConsultaInventarioActivity : PantallaNavegableActivity() {
    override val layoutId = R.layout.activity_consulta_inventario
}

class MisCotizacionesActivity : PantallaNavegableActivity() {
    override val layoutId = R.layout.activity_mis_cotizaciones
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
