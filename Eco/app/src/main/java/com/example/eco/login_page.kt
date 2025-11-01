package com.example.eco

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class login_page : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val db = Firebase.firestore // Firestore instance

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.login_page)

        // Handle system bars
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // --- Initialize Firebase instances ---
        auth = Firebase.auth

        // --- Get UI Element References ---
        val inputEmailOrId = findViewById<TextInputEditText>(R.id.InputId) // For student ID or email
        val inputPassEditText = findViewById<TextInputEditText>(R.id.InputPass) // Password field
        val inputPassLayout = findViewById<TextInputLayout>(R.id.InputPassLayout) // Wrapper for toggle visibility
        val loginButton = findViewById<Button>(R.id.LoginBtn)

        // --- Verify TextInputLayout for password toggle ---
        if (inputPassLayout == null) {
            Log.e("LoginPage", "TextInputLayout with ID 'InputPassLayout' not found in XML. Password toggle may not work.")
        }

        // --- Login Button Click Listener ---
        loginButton.setOnClickListener {
            val idOrEmailOrName = inputEmailOrId.text?.toString()?.trim() ?: ""
            val password = inputPassEditText.text?.toString()?.trim() ?: ""

            if (idOrEmailOrName.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter your email/ID/name and password", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // --- Function to sign in with email and password ---
            fun signInWithEmail(email: String, pwd: String) {
                auth.signInWithEmailAndPassword(email, pwd)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()
                            Log.i("LoginPage", "Signed in: ${auth.currentUser?.uid}")

                            // ✅ Proper redirect with clear task flags
                            val intent = Intent(this, user_page::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        } else {
                            val ex = task.exception
                            val msg = ex?.localizedMessage ?: "Login failed"
                            Log.e("LoginPage", "signInWithEmail failed: $msg", ex)
                            Toast.makeText(this, "Login failed: $msg", Toast.LENGTH_LONG).show()
                        }
                    }
            }

            // --- Determine if input is an email or ID/name ---
            if (idOrEmailOrName.contains("@")) {
                // Direct email login
                signInWithEmail(idOrEmailOrName, password)
            } else {
                // Try lookup by studentId first
                db.collection("users").whereEqualTo("studentId", idOrEmailOrName).get()
                    .addOnSuccessListener { idSnapshots ->
                        if (!idSnapshots.isEmpty) {
                            val doc = idSnapshots.documents[0]
                            val email = doc.getString("email")
                            if (!email.isNullOrEmpty()) {
                                signInWithEmail(email, password)
                            } else {
                                Toast.makeText(this, "No email found for that ID", Toast.LENGTH_LONG).show()
                                Log.w("LoginPage", "User document has no email for studentId=$idOrEmailOrName")
                            }
                        } else {
                            // Try lookup by name
                            db.collection("users").whereEqualTo("name", idOrEmailOrName).get()
                                .addOnSuccessListener { nameSnapshots ->
                                    if (!nameSnapshots.isEmpty) {
                                        val doc = nameSnapshots.documents[0]
                                        val email = doc.getString("email")
                                        if (!email.isNullOrEmpty()) {
                                            signInWithEmail(email, password)
                                        } else {
                                            Toast.makeText(this, "No email found for that name", Toast.LENGTH_LONG).show()
                                            Log.w("LoginPage", "User document has no email for name=$idOrEmailOrName")
                                        }
                                    } else {
                                        Toast.makeText(this, "No user found with that ID or name", Toast.LENGTH_LONG).show()
                                        Log.w("LoginPage", "No user document found for studentId or name=$idOrEmailOrName")
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
