package com.example.eco

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import android.util.Log
import com.google.firebase.ktx.Firebase
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class user_page : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.user_page)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val firstPageButton = findViewById<Button>(R.id.PrizeBtn)
        firstPageButton.setOnClickListener {
            val Intent = Intent(this, user_page::class.java)
            startActivity(Intent)
            }

        val secondPageButton = findViewById<Button>(R.id.LogoutBtn)
        secondPageButton.setOnClickListener {
            // Sign out from Firebase and return to welcome page
            val auth = com.google.firebase.ktx.Firebase.auth
            auth.signOut()
            android.util.Log.i("UserPage", "User signed out via Logout button")
            val Intent = Intent(this, welcome_page::class.java)
            startActivity(Intent)
            finish()
        }

    // Profile display: populate Name, ID, and Total Points for signed-in user
    val studName = findViewById<TextView>(R.id.StudName)
    val studId = findViewById<TextView>(R.id.StudId)
    // Big label (TOTAL POINTS) is static; numeric value is in TotalPointsValue
    val totalPointsValue = findViewById<TextView>(R.id.TotalPointsValue)

        val auth = Firebase.auth
        val db = Firebase.firestore

        val currentUser = auth.currentUser
        if (currentUser != null) {
            val uid = currentUser.uid
            db.collection("users").document(uid).get()
                .addOnSuccessListener { doc ->
                    if (doc != null && doc.exists()) {
                        val name = doc.getString("name") ?: "-"
                        val studentId = doc.getString("studentId") ?: "-"
                        val pointsAny = doc.get("totalPoints")
                        val points = when (pointsAny) {
                            is Long -> pointsAny.toString()
                            is Double -> pointsAny.toInt().toString()
                            is Int -> pointsAny.toString()
                            else -> "0"
                        }
                        studName.text = "Name: $name"
                        studId.text = "ID Number: $studentId"
                        totalPointsValue.text = points
                        // Debug feedback
                        Log.d("UserPage", "Loaded profile: name=$name, studentId=$studentId, points=$points")
                        Toast.makeText(this, "Profile loaded: $name ($studentId)", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("UserPage", "Failed to load profile", e)
                    Toast.makeText(this, "Failed to load profile: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
        else {
            // Provide placeholder text so the layout doesn't look empty
            studName.text = "Name: (not signed in)"
            studId.text = "ID Number: (not signed in)"
            totalPointsValue.text = "0"
            Log.w("UserPage", "No FirebaseAuth currentUser")
            // Prompt the user to sign in
            val builder = androidx.appcompat.app.AlertDialog.Builder(this)
            builder.setTitle("Not signed in")
            builder.setMessage("You are not signed in. Would you like to go to the login page?")
            builder.setPositiveButton("Login") { _, _ ->
                val intent = Intent(this, login_page::class.java)
                startActivity(intent)
                finish()
            }
            builder.setNegativeButton("Close") { dialog, _ ->
                dialog.dismiss()
            }
            builder.setCancelable(true)
            builder.show()
        }
    }
}