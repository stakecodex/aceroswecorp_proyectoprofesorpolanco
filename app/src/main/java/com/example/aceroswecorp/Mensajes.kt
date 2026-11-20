package com.example.aceroswecorp

import android.content.Context
import android.util.Log
import androidx.appcompat.app.AlertDialog

internal fun Context.mostrarError(mensaje: String, error: Exception? = null) {
    if (error != null) {
        Log.e("AcerosWeCorp", mensaje, error)
    }
    AlertDialog.Builder(this)
        .setTitle("Error")
        .setMessage(mensaje)
        .setPositiveButton("Aceptar", null)
        .show()
}
