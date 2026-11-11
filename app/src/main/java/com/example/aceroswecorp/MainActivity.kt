package com.example.aceroswecorp

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_control_acceso)
        applySystemBarInsets()

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val editCorreo = findViewById<EditText>(R.id.edit_correo)
        val editContrasena = findViewById<EditText>(R.id.edit_contrasena)

        findViewById<Button>(R.id.button_ingresar).setOnClickListener {
            val correo = editCorreo.text.toString().trim()
            val contrasena = editContrasena.text.toString()

            if (correo.isEmpty() || contrasena.isEmpty()) {
                mostrarError("Ingresa tu correo y tu contraseña.")
            } else if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
                mostrarError("El correo no tiene un formato válido.")
            } else {
                auth.signInWithEmailAndPassword(correo, contrasena)
                    .addOnSuccessListener {
                        abrirMenuSegunRol()
                    }
                    .addOnFailureListener {
                        mostrarError("Correo o contraseña incorrectos.")
                    }
            }
        }

        findViewById<Button>(R.id.button_crear_cuenta).setOnClickListener {
            startActivity(Intent(this, RegistroActivity::class.java))
        }

        findViewById<TextView>(R.id.text_recuperar).setOnClickListener {
            mostrarRecuperarContrasena()
        }
    }

    override fun onStart() {
        super.onStart()
        if (auth.currentUser != null) {
            abrirMenuSegunRol()
        }
    }

    private fun abrirMenuSegunRol() {
        val uid = auth.currentUser?.uid ?: return

        db.collection("perfiles").document(uid).get()
            .addOnSuccessListener { documento ->
                if (!documento.exists()) {
                    auth.signOut()
                    mostrarError("Tu cuenta no tiene un perfil asignado.")
                } else {
                    val rol = documento.getString("rol")
                    val destino = if (rol == "administrador") {
                        MenuAdministradorActivity::class.java
                    } else {
                        MenuPrincipal::class.java
                    }
                    startActivity(Intent(this, destino))
                    finish()
                }
            }
            .addOnFailureListener {
                mostrarError("No se pudo cargar tu perfil. Revisa tu conexión.")
            }
    }

    private fun mostrarRecuperarContrasena() {
        val editCorreo = EditText(this)
        editCorreo.hint = "Correo electrónico"
        editCorreo.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS

        AlertDialog.Builder(this)
            .setTitle("Recuperar contraseña")
            .setMessage("Te enviaremos un correo para crear una nueva contraseña.")
            .setView(editCorreo)
            .setPositiveButton("Enviar") { _, _ ->
                val correo = editCorreo.text.toString().trim()
                if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
                    mostrarError("Ingresa un correo válido.")
                } else {
                    auth.sendPasswordResetEmail(correo)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Revisa tu correo para cambiar la contraseña.", Toast.LENGTH_LONG).show()
                        }
                        .addOnFailureListener {
                            mostrarError("No se pudo enviar el correo de recuperación.")
                        }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarError(mensaje: String) {
        AlertDialog.Builder(this)
            .setTitle("Error")
            .setMessage(mensaje)
            .setPositiveButton("Aceptar", null)
            .show()
    }
}