package com.example.eco

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.textfield.TextInputEditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.ktx.Firebase
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.Timestamp

class authentication_page : AppCompatActivity() {
    private val RESEND_COOLDOWN_SECONDS = 60L
    private val OTP_EXPIRY_SECONDS = 300L // 5 minutes

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.authentication_page)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val auth = Firebase.auth
        val db = Firebase.firestore

        val inputEmail = findViewById<TextInputEditText>(R.id.inputEmail)
        val inputOtp = findViewById<TextInputEditText>(R.id.inputOtp)
        val authBtn = findViewById<Button>(R.id.AuthBtn)
        val resendLabel = findViewById<TextView>(R.id.Resent)

        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "No signed-in user. Please login first.", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, login_page::class.java))
            finish()
            return
        }

        val uid = currentUser.uid

        // Initially send OTP when page loads if desired (optional):
        // val email = inputEmail.text?.toString()?.trim() ?: ""
        // if (email.isNotEmpty()) generateAndStoreOtp(db, uid, email)

        // Resend handler (uses email entered, checks format):
        resendLabel.setOnClickListener {
            val email = inputEmail.text?.toString()?.trim() ?: ""
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            generateAndStoreOtp(db, uid, email)
            Toast.makeText(this, "OTP sent to your email address.", Toast.LENGTH_SHORT).show()
        }

        // Verify OTP on button click
        authBtn.setOnClickListener {
            val entered = inputOtp.text?.toString()?.trim() ?: ""
            if (entered.isEmpty()) {
                Toast.makeText(this, "Please enter the OTP code", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val otpDoc = db.collection("otps").document(uid)
            otpDoc.get().addOnSuccessListener { snap ->
                if (!snap.exists()) {
                    Toast.makeText(this, "No OTP found. Please resend.", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }
                val code = snap.getString("code") ?: ""
                val expires = snap.getTimestamp("expiresAt")
                val now = Timestamp.now()
                if (expires != null && now.seconds > expires.seconds) {
                    Toast.makeText(this, "OTP expired. Please resend.", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }
                if (entered == code) {
                    db.collection("users").document(uid).update("isVerified", true)
                        .addOnSuccessListener {
                            Log.i("AuthOTP", "User $uid marked verified")
                        }
                        .addOnFailureListener { e ->
                            Log.w("AuthOTP", "Update verified failed: ${e.message}")
                        }
                    Toast.makeText(this, "Authentication successful", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, user_page::class.java))
                    finish()
                } else {
                    Toast.makeText(this, "Incorrect OTP", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener { e ->
                Log.e("AuthOTP", "Failed to read OTP: ${e.message}")
                Toast.makeText(this, "Failed to verify OTP: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

        private fun generateAndStoreOtp(db: com.google.firebase.firestore.FirebaseFirestore, uid: String, email: String) {
            val code = (100000..999999).random().toString()
            val now = Timestamp.now()
            val expiresAt = Timestamp(now.seconds + OTP_EXPIRY_SECONDS, 0)
            val resendAt = Timestamp(now.seconds + RESEND_COOLDOWN_SECONDS, 0)
            val data = mapOf(
                "email" to email,                // <-- include real email
                "code" to code,
                "createdAt" to now,
                "expiresAt" to expiresAt,
                "resendAllowedAt" to resendAt
            )
            db.collection("otps").document(uid).set(data)
                .addOnSuccessListener {
                    // The Cloud Function will send the real email when this doc is created.
                    Toast.makeText(this, "OTP generated. Check your email.", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Log.e("AuthOTP", "Failed to store OTP: ${e.message}")
                    Toast.makeText(this, "Failed to generate OTP: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    
        // Remove or keep for debug only. Real sending is handled by Cloud Function.
        private fun sendOtpEmail(email: String, code: String) {
            Log.i("AuthEmail", "Send OTP $code to $email (local debug)")
            Toast.makeText(this, "OTP $code (debug only)", Toast.LENGTH_LONG).show()
        }

}
