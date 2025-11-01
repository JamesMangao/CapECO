package com.example.eco

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.app.AlertDialog
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.ktx.Firebase
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore

class signup_page : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.signup_page)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val inputId = findViewById<TextInputEditText>(R.id.InputId)
        val inputName = findViewById<TextInputEditText>(R.id.InputName)
        val inputCourse = findViewById<TextInputEditText>(R.id.InputCourse)
        val inputEmail = findViewById<TextInputEditText>(R.id.inputEmail)
        val inputPass = findViewById<TextInputEditText>(R.id.InputPass)
        val signUpBtn = findViewById<Button>(R.id.SignUpBtn)

        val auth = Firebase.auth
        val db = Firebase.firestore

        signUpBtn.setOnClickListener {
            val studentId = inputId.text?.toString()?.trim().orEmpty()
            val name = inputName.text?.toString()?.trim().orEmpty()
            val course = inputCourse.text?.toString()?.trim().orEmpty()
            val email = inputEmail.text?.toString()?.trim().orEmpty()
            val password = inputPass.text?.toString()?.trim().orEmpty()

            if (studentId.isEmpty() || name.isEmpty() || course.isEmpty() || email.isEmpty() || password.length < 6) {
                Toast.makeText(this, "Please fill all fields and use a password with at least 6 characters", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val uid = auth.currentUser?.uid ?: ""
                        val profile = hashMapOf(
                            "studentId" to studentId,
                            "name" to name,
                            "course" to course,
                            "email" to email,
                            "isVerified" to false,
                            "createdAt" to com.google.firebase.Timestamp.now()
                        )

                        db.collection("users").document(uid).set(profile)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Signup successful! Please verify your email.", Toast.LENGTH_LONG).show()
                                startActivity(Intent(this, authentication_page::class.java).apply {
                                    putExtra("USER_EMAIL", email)
                                })
                                finishAffinity()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Signup succeeded but failed to save profile: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                    } else {
                        val ex = task.exception
                        if (ex is FirebaseAuthUserCollisionException) {
                            AlertDialog.Builder(this)
                                .setTitle("Email already in use")
                                .setMessage("An account with this email already exists. Would you like to go to the login page?")
                                .setPositiveButton("Go to Login") { _, _ ->
                                    startActivity(Intent(this, login_page::class.java))
                                    finish()
                                }
                                .setNegativeButton("Cancel", null)
                                .show()
                        } else {
                            Toast.makeText(this, "Signup failed: ${ex?.localizedMessage}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
        }
    }
}
