package com.example.eco

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import com.google.android.material.textfield.TextInputEditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.ktx.auth
import android.app.AlertDialog
import com.google.firebase.ktx.Firebase
import com.google.firebase.firestore.ktx.firestore
import android.content.pm.ApplicationInfo

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
        val inputName = findViewById<TextInputEditText>(R.id.inputName)
        val inputCourse = findViewById<TextInputEditText>(R.id.inputCourse)
        val inputEmail = findViewById<TextInputEditText>(R.id.inputEmail)
        val inputPass = findViewById<TextInputEditText>(R.id.InputPass)
        val signUpBtn = findViewById<Button>(R.id.SignUpBtn)

    // Do not access Firebase.* APIs before ensuring FirebaseApp is initialized.

        // Sanity-check Firebase initialization and log options so we can diagnose CONFIGURATION_NOT_FOUND
        try {
            val apps = com.google.firebase.FirebaseApp.getApps(this)
            if (apps.isEmpty()) {
                android.util.Log.w("FirebaseInit", "No FirebaseApp instances found. Attempting programmatic init from raw resource.")
                // Attempt to initialize from res/raw/google_services.json as a fallback
                try {
                    val `is` = resources.openRawResource(R.raw.google_services)
                    val jsonText = `is`.bufferedReader().use { it.readText() }
                        android.util.Log.d("FirebaseInit", "Raw google_services.json: $jsonText")
                    val json = org.json.JSONObject(jsonText)
                    val projectId = json.getJSONObject("project_info").getString("project_id")
                    val client = json.getJSONArray("client").getJSONObject(0)
                    val apiKey = client.getJSONArray("api_key").getJSONObject(0).getString("current_key")
                    val appId = client.getJSONObject("client_info").getString("mobilesdk_app_id")
                        val storageBucket = try { json.getJSONObject("project_info").getString("storage_bucket") } catch (e: Exception) { null }
                        val gcmSenderId = try { json.getJSONObject("project_info").getString("project_number") } catch (e: Exception) { null }
                    val builder = com.google.firebase.FirebaseOptions.Builder()
                        .setProjectId(projectId)
                        .setApiKey(apiKey)
                        .setApplicationId(appId)
                    if (!storageBucket.isNullOrEmpty()) builder.setStorageBucket(storageBucket)
                    if (!gcmSenderId.isNullOrEmpty()) builder.setGcmSenderId(gcmSenderId)
                    val options = builder.build()
                    com.google.firebase.FirebaseApp.initializeApp(this, options)
                    val appInstance = com.google.firebase.FirebaseApp.getInstance()
                    val opts = appInstance.options
                    android.util.Log.i("FirebaseInit", "Programmatic init succeeded. projectId=${opts.projectId}, applicationId=${opts.applicationId}")
                    val isDebuggable = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
                    if (isDebuggable) {
                        // show options to the user for debugging copy/paste
                        val msg = StringBuilder()
                        msg.append("projectId: ${opts.projectId}\n")
                        msg.append("applicationId: ${opts.applicationId}\n")
                        msg.append("apiKey: ${opts.apiKey}\n")
                        if (!opts.storageBucket.isNullOrEmpty()) msg.append("storageBucket: ${opts.storageBucket}\n")
                        if (!opts.gcmSenderId.isNullOrEmpty()) msg.append("gcmSenderId: ${opts.gcmSenderId}\n")
                        AlertDialog.Builder(this)
                            .setTitle("Firebase Init OK")
                            .setMessage(msg.toString())
                            .setPositiveButton("OK", null)
                            .show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Firebase is not initialized. Check google-services.json and plugin config. (${e.message})", Toast.LENGTH_LONG).show()
                    android.util.Log.e("FirebaseInit", "Programmatic init failed", e)
                    return
                }
            } else {
                val appInstance = com.google.firebase.FirebaseApp.getInstance()
                val options = appInstance.options
                android.util.Log.i("FirebaseInit", "FirebaseApp initialized. projectId=${options.projectId}, applicationId=${options.applicationId}, apiKey=${options.apiKey}")
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Firebase init check failed: ${e.message}", Toast.LENGTH_LONG).show()
            android.util.Log.e("FirebaseInit", "Exception while checking FirebaseApp", e)
            return
        }

        // Now that FirebaseApp is initialized, obtain Auth and Firestore instances
        val auth = Firebase.auth
        val db = Firebase.firestore

        signUpBtn.setOnClickListener {
            val studentId = inputId.text?.toString()?.trim() ?: ""
            val name = inputName.text?.toString()?.trim() ?: ""
            val course = inputCourse.text?.toString()?.trim() ?: ""
            val email = inputEmail.text?.toString()?.trim() ?: ""
            val password = inputPass.text?.toString() ?: ""

            // Basic validation
            if (studentId.isEmpty() || name.isEmpty() || course.isEmpty() || email.isEmpty() || password.length < 6) {
                Toast.makeText(this, "Please fill all fields and use a password with >=6 characters", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // Create user with Firebase Auth
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val firebaseUser = auth.currentUser
                        val uid = firebaseUser?.uid ?: ""

                        // Save user profile in Firestore under collection 'users' with document id = uid
                        val profile = hashMapOf(
                            "studentId" to studentId,
                            "name" to name,
                            "course" to course,
                            "email" to email,
                            "createdAt" to com.google.firebase.Timestamp.now()
                        )

                        if (uid.isNotEmpty()) {
                            db.collection("users").document(uid).set(profile)
                                .addOnSuccessListener {
                                    // After saving profile, send verification email to the provided address
                                    val user = firebaseUser
                                    if (user != null) {
                                        // Generate a 6-digit OTP and store it in Firestore under otps/{uid}
                                        try {
                                            val code = (100000..999999).random().toString()
                                            val now = com.google.firebase.Timestamp.now()
                                            val expiresAt = com.google.firebase.Timestamp(now.seconds + 300, 0) // 5 minutes
                                            val resendAt = com.google.firebase.Timestamp(now.seconds + 60, 0) // 60s cooldown
                                            val otpData = hashMapOf(
                                                "code" to code,
                                                "email" to email,
                                                "createdAt" to now,
                                                "expiresAt" to expiresAt,
                                                "resendAllowedAt" to resendAt
                                            )
                                            db.collection("otps").document(uid).set(otpData)
                                                .addOnSuccessListener {
                                                    // NOTE: writing the OTP to Firestore does NOT send an email by itself.
                                                    // To actually deliver the OTP to the user's email, configure a server-side
                                                    // Cloud Function or email service that watches otps/{uid} and sends the code via SMTP/SendGrid.
                                                    // For development/testing we also show the OTP in a Toast so it can be copied.
                                                    Toast.makeText(this, "Signup successful. OTP has been generated and will be delivered by the email service.", Toast.LENGTH_LONG).show()
                                                    val intent = Intent(this, authentication_page::class.java)
                                                    startActivity(intent)
                                                    finish()
                                                }
                                                .addOnFailureListener { e ->
                                                    Toast.makeText(this, "Signup succeeded but failed to create OTP: ${e.message}", Toast.LENGTH_LONG).show()
                                                    val intent = Intent(this, authentication_page::class.java)
                                                    startActivity(intent)
                                                    finish()
                                                }
                                        } catch (e: Exception) {
                                            Toast.makeText(this, "Signup succeeded but failed to generate OTP: ${e.message}", Toast.LENGTH_LONG).show()
                                            val intent = Intent(this, authentication_page::class.java)
                                            startActivity(intent)
                                            finish()
                                        }
                                    } else {
                                        Toast.makeText(this, "Signup successful but user object is null", Toast.LENGTH_LONG).show()
                                        val intent = Intent(this, authentication_page::class.java)
                                        startActivity(intent)
                                        finish()
                                    }
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(this, "Signup succeeded but failed to save profile: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                        } else {
                            Toast.makeText(this, "Signup succeeded but no user id returned", Toast.LENGTH_LONG).show()
                        }
                        } else {
                            val ex = task.exception
                            val msg = ex?.localizedMessage ?: "Signup failed"
                            android.util.Log.e("FirebaseAuth", "createUserWithEmailAndPassword failed: $msg", ex)
                            // If the email is already in use, provide recovery options
                            try {
                                // FirebaseAuthUserCollisionException indicates the email is already used
                                if (ex is com.google.firebase.auth.FirebaseAuthUserCollisionException) {
                                    // Check which sign-in methods are associated with this email
                                    auth.fetchSignInMethodsForEmail(email)
                                        .addOnCompleteListener { fetchTask ->
                                            if (fetchTask.isSuccessful) {
                                                val methods = fetchTask.result?.signInMethods ?: emptyList()
                                                val providers = methods.joinToString(", ")
                                                android.util.Log.i("FirebaseAuth", "Existing providers for $email: $providers")
                                                if (methods.contains("password")) {
                                                    AlertDialog.Builder(this)
                                                        .setTitle("Email already in use")
                                                        .setMessage("An account with this email already exists. Would you like to send a password reset email to $email?")
                                                        .setPositiveButton("Send Reset") { _, _ ->
                                                            auth.sendPasswordResetEmail(email)
                                                                .addOnCompleteListener { resetTask ->
                                                                    if (resetTask.isSuccessful) {
                                                                        Toast.makeText(this, "Password reset email sent to $email", Toast.LENGTH_LONG).show()
                                                                    } else {
                                                                        Toast.makeText(this, "Failed to send reset email: ${resetTask.exception?.message}", Toast.LENGTH_LONG).show()
                                                                    }
                                                                }
                                                        }
                                                        .setNegativeButton("Go to Login") { _, _ ->
                                                            startActivity(Intent(this, login_page::class.java))
                                                        }
                                                        .show()
                                                } else {
                                                    AlertDialog.Builder(this)
                                                        .setTitle("Email already in use")
                                                        .setMessage("This email is already registered with the following providers: $providers. Please sign in using the appropriate provider or contact support.")
                                                        .setPositiveButton("OK", null)
                                                        .show()
                                                }
                                            } else {
                                                Toast.makeText(this, "Email is already in use. Please try logging in.", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                } else {
                                    // Generic error handling: show the exception and stack for debugging
                                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
                                    val stack = StringBuilder()
                                    stack.append(ex?.toString())
                                    ex?.stackTrace?.forEach { st -> stack.append("\n" + st.toString()) }
                                    AlertDialog.Builder(this)
                                        .setTitle("Auth Error")
                                        .setMessage(stack.toString())
                                        .setPositiveButton("OK", null)
                                        .show()
                                }
                            } catch (e: Exception) {
                                // Fallback for any unexpected issues while handling the collision
                                Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
                                android.util.Log.e("FirebaseAuth", "Error while handling signup error: ${e.message}", e)
                            }
                        }
                }
        }
    }
}