package com.example.aceroswecorp

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class RegistroActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro)
        applySystemBarInsets()

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val editNombre = findViewById<EditText>(R.id.edit_nombre)
        val editCorreo = findViewById<EditText>(R.id.edit_correo)
        val editContrasena = findViewById<EditText>(R.id.edit_contrasena)
        val editConfirmar = findViewById<EditText>(R.id.edit_confirmar)

        findViewById<ImageButton>(R.id.screen_back).setOnClickListener {
            finish()
        }

        findViewById<Button>(R.id.button_registrar).setOnClickListener {
            val nombre = editNombre.text.toString().trim()
            val correo = editCorreo.text.toString().trim()
            val contrasena = editContrasena.text.toString()
            val confirmar = editConfirmar.text.toString()

            if (nombre.length < 3) {
                mostrarError("El nombre debe tener al menos 3 caracteres.")
            } else if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
                mostrarError("Ingresa un correo válido.")
            } else if (contrasena.length < 6) {
                mostrarError("La contraseña debe tener al menos 6 caracteres.")
            } else if (contrasena != confirmar) {
                mostrarError("Las contraseñas no coinciden.")
            } else {
                auth.createUserWithEmailAndPassword(correo, contrasena)
                    .addOnSuccessListener { resultado ->
                        val uid = resultado.user?.uid
                        if (uid != null) {
                            guardarPerfil(uid, nombre, correo)
                        }
                    }
                    .addOnFailureListener { error ->
                        if (error is FirebaseAuthUserCollisionException) {
                            mostrarError("Ese correo ya está registrado.")
                        } else {
                            mostrarError("No se pudo crear la cuenta. Revisa tu conexión.")
                        }
                    }
            }
        }
    }

    private fun guardarPerfil(uid: String, nombre: String, correo: String) {
        val perfil = hashMapOf<String, Any>(
            "uid" to uid,
            "nombre" to nombre,
            "correo" to correo,
            "rol" to "vendedor",
            "activo" to true,
            "creadoEn" to FieldValue.serverTimestamp()
        )

        db.collection("perfiles").document(uid).set(perfil)
            .addOnSuccessListener {
                Toast.makeText(this, "Cuenta creada correctamente.", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, MenuPrincipal::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
            .addOnFailureListener {
                mostrarError("La cuenta se creó, pero no se pudo guardar el perfil.")
            }
    }

    private fun mostrarError(mensaje: String) {
        AlertDialog.Builder(this)
            .setTitle("Error")
            .setMessage(mensaje)
            .setPositiveButton("Aceptar", null)
            .show()
    }
}