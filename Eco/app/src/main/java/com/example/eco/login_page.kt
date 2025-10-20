package com.example.eco

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import com.google.android.material.textfield.TextInputEditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.ktx.Firebase
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore

class login_page : AppCompatActivity() {
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.login_page)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Inputs
        val inputId = findViewById<TextInputEditText>(R.id.InputId)
        val inputPass = findViewById<TextInputEditText>(R.id.InputPass)

        // Firebase instances
        val auth = Firebase.auth
        val db = Firebase.firestore

        val loginBtn = findViewById<Button>(R.id.LoginBtn)
        loginBtn.setOnClickListener {
            val idOrEmailOrName = inputId.text?.toString()?.trim() ?: ""
            val password = inputPass.text?.toString() ?: ""

            if (idOrEmailOrName.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter email/ID/name and password", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            fun signInWithEmail(email: String) {
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()
                            Log.i("LoginPage", "Signed in: ${auth.currentUser?.uid}")
                            startActivity(Intent(this, user_page::class.java))
                            finish()
                        } else {
                            val ex = task.exception
                            val msg = ex?.localizedMessage ?: "Login failed"
                            Log.e("LoginPage", "signInWithEmail failed: $msg", ex)
                            Toast.makeText(this, "Login failed: $msg", Toast.LENGTH_LONG).show()
                        }
                    }
            }

            if (idOrEmailOrName.contains("@")) {
                // Treat as email and login directly
                signInWithEmail(idOrEmailOrName)
            } else {
                // Try by studentId
                db.collection("users").whereEqualTo("studentId", idOrEmailOrName).get()
                    .addOnSuccessListener { idSnapshots ->
                        if (idSnapshots != null && !idSnapshots.isEmpty) {
                            val doc = idSnapshots.documents[0]
                            val email = doc.getString("email")
                            if (!email.isNullOrEmpty()) {
                                signInWithEmail(email)
                            } else {
                                Toast.makeText(this, "No email found for that ID", Toast.LENGTH_LONG).show()
                                Log.w("LoginPage", "user doc has no email for studentId=$idOrEmailOrName")
                            }
                        } else {
                            // Not found by studentId, try by name
                            db.collection("users").whereEqualTo("name", idOrEmailOrName).get()
                                .addOnSuccessListener { nameSnapshots ->
                                    if (nameSnapshots != null && !nameSnapshots.isEmpty) {
                                        val doc = nameSnapshots.documents[0]
                                        val email = doc.getString("email")
                                        if (!email.isNullOrEmpty()) {
                                            signInWithEmail(email)
                                        } else {
                                            Toast.makeText(this, "No email found for that name", Toast.LENGTH_LONG).show()
                                            Log.w("LoginPage", "user doc has no email for name=$idOrEmailOrName")
                                        }
                                    } else {
                                        Toast.makeText(this, "No user found with that ID or name", Toast.LENGTH_LONG).show()
                                        Log.w("LoginPage", "No user document for studentId or name=$idOrEmailOrName")
                                    }
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(this, "Failed to lookup user by name: ${e.message}", Toast.LENGTH_LONG).show()
                                    Log.e("LoginPage", "Firestore lookup by name failed", e)
                                }
                        }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to lookup user by ID: ${e.message}", Toast.LENGTH_LONG).show()
                        Log.e("LoginPage", "Firestore lookup by ID failed", e)
                    }
            }
        }
    }
}
