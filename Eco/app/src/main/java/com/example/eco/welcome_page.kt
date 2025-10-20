package com.example.eco

// Firebase profile display was moved to user_page.kt; remove unused Firebase refs here
import android.widget.TextView
import android.widget.Toast
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class welcome_page : AppCompatActivity() {
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.welcome_page)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Note: user profile UI was moved to user_page; the welcome screen only provides navigation now.
        val firstPageButton = findViewById<Button>(R.id.loginBtn)
        firstPageButton.setOnClickListener {
            val Intent = Intent(this, login_page::class.java)
            startActivity(Intent)
        }

        val secondPageButton = findViewById<Button>(R.id.signupBtn)
        secondPageButton.setOnClickListener {
            val Intent = Intent(this, signup_page::class.java)
            startActivity(Intent)
        }

    }
}