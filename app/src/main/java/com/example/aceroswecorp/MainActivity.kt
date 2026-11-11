package com.example.aceroswecorp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_control_acceso)
        applySystemBarInsets()
        findViewById<ImageButton>(R.id.screen_back).setOnClickListener {
            finish()
        }
        findViewById<Button>(R.id.button_enter_sales).setOnClickListener {
            startActivity(Intent(this, MenuPrincipal::class.java))
        }
        findViewById<Button>(R.id.button_enter_admin).setOnClickListener {
            startActivity(Intent(this, MenuAdministradorActivity::class.java))
        }
    }
}