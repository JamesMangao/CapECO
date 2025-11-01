package com.example.eco

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class user_page : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val db = Firebase.firestore // Firestore instance

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.user_page)

        // Handle system bars safely
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // --- Initialize Firebase ---
        auth = Firebase.auth
        val currentUser = auth.currentUser

        if (currentUser == null) {
            Toast.makeText(this, "No user logged in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // 🔹 Find TextViews
        val studName = findViewById<TextView>(R.id.StudName)
        val studId = findViewById<TextView>(R.id.StudId)
        val numPoints = findViewById<TextView>(R.id.NumPoints)
        val logoutBtn = findViewById<Button>(R.id.LogoutBtn)

        val uid = currentUser.uid

        // --- Fetch and display user info ---
        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val name = document.getString("name") ?: "User"
                    val idNumber = document.getString("studentId") ?: "N/A"
                    val points = document.getLong("points") ?: 0L


                    studName.text = "Name: $name"
                    studId.text = "ID Number: $idNumber"
                    numPoints.text = points.toString()
                } else {
                    studName.text = "Welcome, ${currentUser.email}"
                    studId.text = "ID Number: Not found"
                    numPoints.text = "0"
                }
            }
            .addOnFailureListener { e ->
                studName.text = "Welcome, ${currentUser.email}"
                studId.text = "Error loading ID"
                numPoints.text = "0"
                Log.e("UserPage", "Failed to fetch user info", e)
            }

        // --- Logout Functionality ---
        logoutBtn.setOnClickListener {
            auth.signOut()
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
