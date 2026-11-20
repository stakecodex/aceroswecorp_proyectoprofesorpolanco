package com.example.aceroswecorp

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import org.json.JSONException
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import java.util.concurrent.Executors

class VentasDocumentosActivity : PantallaNavegableActivity() {
    override val layoutId = R.layout.activity_ventas_documentos

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap == null) {
            Toast.makeText(this, "No se tomó ninguna foto.", Toast.LENGTH_SHORT).show()
        } else {
            subirFoto(bitmap)
        }
    }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.any { it }) {
            obtenerUbicacion()
        } else {
            mostrarResultadoUbicacion("Permiso de ubicación denegado.")
        }
    }

    private val networkExecutor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var locationListener: LocationListener? = null
    private var locationTimeout: Runnable? = null

    override fun pantallaInicio(): Class<out AppCompatActivity> =
        MenuAdministradorActivity::class.java

    override fun pantallaCotizaciones(): Class<out AppCompatActivity> =
        HistorialCotizacionesActivity::class.java

    override fun pantallaInventario(): Class<out AppCompatActivity> =
        HistorialInventarioActivity::class.java

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        findViewById<Button>(R.id.button_capture_photo).setOnClickListener {
            cameraLauncher.launch(null)
        }
        findViewById<Button>(R.id.button_get_location).setOnClickListener {
            solicitarUbicacion()
        }
        findViewById<Button>(R.id.button_get_exchange_rate).setOnClickListener {
            consultarTipoCambio()
        }
        findViewById<Button>(R.id.button_three_actions).setOnClickListener {
            mostrarDialogoAcciones()
        }
        findViewById<Button>(R.id.button_open_website).setOnClickListener {
            abrirReferenciaWeb()
        }
    }

    override fun onDestroy() {
        detenerSolicitudUbicacion()
        networkExecutor.shutdownNow()
        super.onDestroy()
    }

    private fun subirFoto(bitmap: Bitmap) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            mostrarError("Inicia sesión antes de guardar una foto.")
            return
        }
        val bytes = ByteArrayOutputStream().use { output ->
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 85, output)) {
                mostrarError("No se pudo preparar la fotografía.")
                return
            }
            output.toByteArray()
        }
        val path = "usuarios/$uid/fotos/${UUID.randomUUID()}.jpg"
        val metadata = StorageMetadata.Builder()
            .setContentType("image/jpeg")
            .build()
        try {
            FirebaseStorage.getInstance().reference.child(path).putBytes(bytes, metadata)
                .addOnSuccessListener {
                    if (!isFinishing && !isDestroyed) {
                        findViewById<TextView>(R.id.camera_upload_result).text =
                            "Foto guardada en Firebase Storage."
                        Toast.makeText(this, "Foto guardada.", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { error ->
                    mostrarError("No se pudo subir la foto a Firebase Storage.", error)
                }
        } catch (error: IllegalStateException) {
            mostrarError("Configura Firebase Storage en el proyecto antes de subir fotos.", error)
        }
    }

    private fun solicitarUbicacion() {
        val fine = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (fine || coarse) {
            obtenerUbicacion()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun obtenerUbicacion() {
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        val hasFinePermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val providers = if (hasFinePermission) {
            listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        } else {
            listOf(LocationManager.NETWORK_PROVIDER)
        }
        val provider = providers.firstOrNull { locationManager.isProviderEnabled(it) }
        if (provider == null) {
            mostrarResultadoUbicacion("Activa la ubicación del dispositivo e inténtalo nuevamente.")
            return
        }

        try {
            val lastLocation = locationManager.getLastKnownLocation(provider)
            if (lastLocation != null &&
                System.currentTimeMillis() - lastLocation.time <= MAX_LOCATION_AGE_MILLIS
            ) {
                mostrarUbicacion(lastLocation)
                return
            }
            mostrarResultadoUbicacion("Buscando ubicación actual...")
            val listener = LocationListener { location ->
                detenerSolicitudUbicacion()
                mostrarUbicacion(location)
            }
            locationListener = listener
            locationManager.requestLocationUpdates(provider, 0L, 0f, listener, Looper.getMainLooper())
            val timeout = Runnable {
                detenerSolicitudUbicacion()
                mostrarResultadoUbicacion("No se encontró una ubicación reciente. Inténtalo al aire libre.")
            }
            locationTimeout = timeout
            mainHandler.postDelayed(timeout, LOCATION_TIMEOUT_MILLIS)
        } catch (error: SecurityException) {
            mostrarResultadoUbicacion("No hay permiso para consultar la ubicación.")
        } catch (error: IllegalArgumentException) {
            mostrarResultadoUbicacion("El proveedor de ubicación dejó de estar disponible.")
        }
    }

    private fun mostrarUbicacion(location: Location) {
        mostrarResultadoUbicacion(
            "Latitud: ${"%.5f".format(location.latitude)} · Longitud: ${"%.5f".format(location.longitude)}"
        )
    }

    private fun mostrarResultadoUbicacion(message: String) {
        findViewById<TextView>(R.id.location_result).text = message
        if (message != "Buscando ubicación actual...") {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun detenerSolicitudUbicacion() {
        locationTimeout?.let(mainHandler::removeCallbacks)
        locationTimeout = null
        val listener = locationListener ?: return
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        val hasPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            try {
                locationManager.removeUpdates(listener)
            } catch (error: SecurityException) {
                mostrarResultadoUbicacion("El permiso de ubicación fue revocado.")
            }
        }
        locationListener = null
    }

    private fun consultarTipoCambio() {
        val result = findViewById<TextView>(R.id.exchange_rate_result)
        result.text = "Consultando tipo de cambio..."
        networkExecutor.execute {
            var connection: HttpURLConnection? = null
            try {
                connection = URL(EXCHANGE_RATE_URL).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = NETWORK_TIMEOUT_MILLIS
                connection.readTimeout = NETWORK_TIMEOUT_MILLIS
                val responseCode = connection.responseCode
                if (responseCode !in 200..299) {
                    throw IOException("El servicio respondió con HTTP $responseCode.")
                }
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                val rate = JSONObject(body).getJSONObject("rates").getDouble("CLP")
                runOnUiThread {
                    if (!isFinishing && !isDestroyed) {
                        result.text = "1 USD = ${formatearMoneda(rate)}"
                    }
                }
            } catch (error: IOException) {
                runOnUiThread {
                    if (!isFinishing && !isDestroyed) {
                        result.text = "No se pudo consultar el tipo de cambio."
                        mostrarError("Revisa tu conexión e inténtalo nuevamente.", error)
                    }
                }
            } catch (error: JSONException) {
                runOnUiThread {
                    if (!isFinishing && !isDestroyed) {
                        result.text = "La respuesta del servicio no tiene el formato esperado."
                        mostrarError("No se pudo interpretar el tipo de cambio.", error)
                    }
                }
            } finally {
                connection?.disconnect()
            }
        }
    }

    private fun mostrarDialogoAcciones() {
        AlertDialog.Builder(this)
            .setTitle("Acción de prueba")
            .setMessage("Elige una acción para continuar.")
            .setPositiveButton("Confirmar") { _, _ ->
                Toast.makeText(this, "Acción confirmada.", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar") { _, _ ->
                Toast.makeText(this, "Acción cancelada.", Toast.LENGTH_SHORT).show()
            }
            .setNeutralButton("Más tarde") { _, _ ->
                Toast.makeText(this, "Acción pospuesta.", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun abrirReferenciaWeb() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(EXCHANGE_RATE_URL))
        try {
            startActivity(intent)
        } catch (error: ActivityNotFoundException) {
            mostrarError("No hay una aplicación disponible para abrir el enlace.", error)
        }
    }

    private companion object {
        const val EXCHANGE_RATE_URL = "https://open.er-api.com/v6/latest/USD"
        const val NETWORK_TIMEOUT_MILLIS = 10_000
        const val LOCATION_TIMEOUT_MILLIS = 15_000L
        const val MAX_LOCATION_AGE_MILLIS = 5 * 60 * 1000L
    }
}
