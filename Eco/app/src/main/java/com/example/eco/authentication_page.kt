package com.example.eco

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class authentication_page : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val db = Firebase.firestore
    private val EMAIL_KEY = "email_for_sign_in"
    private val PREFS_NAME = "SignInPrefs"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.authentication_page)

        auth = Firebase.auth

        val inputEmail = findViewById<TextInputEditText>(R.id.inputEmail)
        val sendLinkButton = findViewById<Button>(R.id.sendLinkButton)
        val statusTextView = findViewById<TextView>(R.id.statusTextView)

        // Receive the email from the signup page and pre-fill the field
        val passedEmail = intent.getStringExtra("USER_EMAIL")
        if (passedEmail != null) {
            inputEmail.setText(passedEmail)
            statusTextView.text = "A verification link will be sent to your email."
        }

        // Check if the app was opened by the sign-in link
        handleSignInIntent(intent)

        sendLinkButton.setOnClickListener {
            val email = inputEmail.text.toString().trim()
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            sendSignInLink(email)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleSignInIntent(intent)
    }

    private fun sendSignInLink(email: String) {
        val actionCodeSettings = com.google.firebase.auth.ActionCodeSettings.newBuilder()
            // This MUST use your authorized domain
            .setUrl("https://ecocycle-474414.firebaseapp.com/verify")
            .setHandleCodeInApp(true)
            .setAndroidPackageName(
                packageName,
                true,
                null)
            .build()

        auth.sendSignInLinkToEmail(email, actionCodeSettings)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("AuthPage", "Email sent.")
                    Toast.makeText(this, "Verification email sent! Check your inbox (and spam folder).", Toast.LENGTH_LONG).show()
                    // Save email locally to complete the sign-in
                    getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().putString(EMAIL_KEY, email).apply()
                } else {
                    Log.w("AuthPage", "sendSignInLinkToEmail:failure", task.exception)
                    Toast.makeText(this, "Failed to send email: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun handleSignInIntent(intent: Intent?) {
        val emailLink = intent?.data?.toString()
        if (emailLink == null || !auth.isSignInWithEmailLink(emailLink)) {
            return // Not a sign-in link, do nothing.
        }

        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val email = prefs.getString(EMAIL_KEY, null)

        if (email == null) {
            Toast.makeText(this, "Error: Could not find email to complete sign-in. Please try again.", Toast.LENGTH_LONG).show()
            return
        }

        auth.signInWithEmailLink(email, emailLink)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("AuthPage", "signInWithEmailLink:success")
                    prefs.edit().remove(EMAIL_KEY).apply()

                    val uid = auth.currentUser?.uid
                    if (uid != null) {
                        db.collection("users").document(uid).update("isVerified", true)
                            .addOnSuccessListener {
                                Log.i("AuthPage", "User $uid marked verified")
                                Toast.makeText(this, "Authentication successful!", Toast.LENGTH_SHORT).show()
                                startActivity(Intent(this, user_page::class.java))
                                finishAffinity()
                            }
                            .addOnFailureListener { e ->
                                Log.w("AuthPage", "Update verified flag failed: ${e.message}")
                                Toast.makeText(this, "Verification succeeded but failed to update status.", Toast.LENGTH_LONG).show()
                            }
                    }
                } else {
                    Log.w("AuthPage", "signInWithEmailLink:failure", task.exception)
                    Toast.makeText(this, "Authentication failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
}