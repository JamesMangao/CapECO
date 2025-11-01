package com.example.eco

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.util.UUID

class reward_page : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var storageRef: StorageReference

    private lateinit var profileImageView: ImageView
    private lateinit var uploadImageButton: Button
    private lateinit var prizeButton: Button
    private lateinit var infoImageView: ImageView
    private lateinit var logoutButton: Button
    private lateinit var studName: TextView
    private lateinit var studId: TextView
    private lateinit var totalPointsValue: TextView

    // Request code for runtime permissions
    private val PERMISSION_REQUEST_CODE = 1001

    // Activity Result Launcher for picking images
    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        // Callback is invoked after the user selects a media item or closes the photo picker.
        if (uri != null) {
            Log.d("RewardPage", "Selected URI: $uri")
            uploadImageToFirebaseStorage(uri)
        } else {
            Log.d("RewardPage", "No media selected")
            Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.reward_page)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // --- Initialize Firebase ---
        auth = Firebase.auth
        db = Firebase.firestore
        storageRef = FirebaseStorage.getInstance().reference

        // --- Get UI Element References ---
        profileImageView = findViewById(R.id.userPfp) // Changed from imageView4 to profileImageView
        uploadImageButton = findViewById(R.id.uploadPhotoButton)
        prizeButton = findViewById(R.id.redeemBtn)
        infoImageView = findViewById(R.id.userPfp) // Kept imageView4 for user info details
        logoutButton = findViewById(R.id.LogoutBtn)
        studName = findViewById(R.id.StudName)
        studId = findViewById(R.id.StudId)
        totalPointsValue = findViewById(R.id.NumPoints)

        // --- Set OnClickListener for Prize Button ---
        prizeButton.setOnClickListener {
            // Navigate to Reward page
            // Replace RewardActivity::class.java with your actual Reward page activity name
            startActivity(Intent(this, reward_page::class.java))
        }

        // --- Set OnClickListener for Info ImageView ---
        infoImageView.setOnClickListener {
            // Navigate to User Info page
            // Replace UserInfoPage::class.java with your actual User Info page activity name
            startActivity(Intent(this, user_info_page::class.java))
        }

        // --- Set OnClickListener for Logout Button ---
        logoutButton.setOnClickListener {
            auth.signOut()
            Log.i("RewardPage", "User signed out via Logout button")
            Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show()
            val logoutIntent = Intent(this, welcome_page::class.java) // Or login_page
            startActivity(logoutIntent)
            finish() // Prevents returning to user_page with back button
        }

        // --- Set OnClickListener for Upload Photo Button ---
        uploadImageButton.setOnClickListener {
            checkPermissionsAndPickImage()
        }

        // --- Load User Profile Data (including image) ---
        loadUserProfile()
    }

    // --- Permission Handling and Image Picking ---
    private fun checkPermissionsAndPickImage() {
        val permissionGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }

        if (permissionGranted) {
            launchImagePicker()
        } else {
            // Request permissions
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestPermissions(arrayOf(Manifest.permission.READ_MEDIA_IMAGES), PERMISSION_REQUEST_CODE)
            } else {
                requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), PERMISSION_REQUEST_CODE)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchImagePicker()
            } else {
                Toast.makeText(this, "Permission denied. Cannot select image.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun launchImagePicker() {
        pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
    // --- End of Permission Handling ---

    // --- Firebase Storage Upload Logic ---
    private fun uploadImageToFirebaseStorage(uri: Uri) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show()
            return
        }
        val userId = currentUser.uid
        // Create a unique filename for the image, using UUID to avoid conflicts
        val fileRef = storageRef.child("profile_images/$userId/${UUID.randomUUID()}.jpg")

        Toast.makeText(this, "Uploading image...", Toast.LENGTH_LONG).show()

        fileRef.putFile(uri)
            .addOnSuccessListener { taskSnapshot ->
                // Image uploaded successfully. Now get the download URL.
                taskSnapshot.storage.downloadUrl.addOnSuccessListener { downloadUri ->
                    val imageUrl = downloadUri.toString()
                    Log.d("RewardPage", "Image uploaded successfully. URL: $imageUrl")
                    saveImageUrlToFirestore(imageUrl)
                    // Display the uploaded image immediately
                    displayImage(imageUrl)
                }
            }
            .addOnFailureListener { exception ->
                Log.e("RewardPage", "Image upload failed", exception)
                Toast.makeText(this, "Upload failed: ${exception.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun saveImageUrlToFirestore(url: String) {
        val currentUser = auth.currentUser ?: return
        db.collection("users").document(currentUser.uid)
            .update("profileImageUrl", url) // Add or update the 'profileImageUrl' field for the user
            .addOnSuccessListener {
                Log.d("RewardPage", "Image URL saved to Firestore.")
            }
            .addOnFailureListener { e ->
                Log.e("RewardPage", "Failed to save image URL to Firestore", e)
                Toast.makeText(this, "Failed to save image URL.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun displayImage(url: String) {
        Glide.with(this)
            .load(url)
            .apply(RequestOptions.circleCropTransform()) // Apply circular crop if desired
            .placeholder(R.drawable.ic_default_profile_placeholder) // Your default placeholder image
            .error(R.drawable.ic_default_profile_placeholder)     // Image to show if loading fails
            .into(profileImageView)
    }

    // --- Load User Profile Data (including image) ---
    private fun loadUserProfile() {
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
                        val imageUrl = doc.getString("profileImageUrl") // Get the image URL from Firestore

                        studName.text = "Name: $name"
                        studId.text = "ID Number: $studentId"
                        totalPointsValue.text = points

                        // Load the profile image if URL exists, otherwise show placeholder
                        if (!imageUrl.isNullOrEmpty()) {
                            displayImage(imageUrl)
                        } else {
                            profileImageView.setImageResource(R.drawable.ic_default_profile_placeholder) // Set your placeholder drawable
                        }

                        Log.d("RewardPage", "Loaded profile: name=$name, studentId=$studentId, points=$points")
                        Toast.makeText(this, "Profile loaded: $name ($studentId)", Toast.LENGTH_SHORT).show()
                    } else {
                        // Handle case where user document doesn't exist in Firestore
                        studName.text = "Name: (Not found)"
                        studId.text = "ID Number: (Not found)"
                        totalPointsValue.text = "0"
                        profileImageView.setImageResource(R.drawable.ic_default_profile_placeholder) // Set placeholder
                        Log.w("RewardPage", "User document not found for UID: $uid")
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("RewardPage", "Failed to load profile", e)
                    Toast.makeText(this, "Failed to load profile: ${e.message}", Toast.LENGTH_LONG).show()
                }
        } else {
            // Handle user not signed in scenario (as you had before)
            studName.text = "Name: (not signed in)"
            studId.text = "ID Number: (not signed in)"
            totalPointsValue.text = "0"
            profileImageView.setImageResource(R.drawable.ic_default_profile_placeholder) // Set placeholder
            Log.w("RewardPage", "No FirebaseAuth currentUser")
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Not signed in")
            builder.setMessage("You are not signed in. Would you like to go to the login page?")
            builder.setPositiveButton("Login") { _, _ ->
                startActivity(Intent(this, login_page::class.java))
                finish()
            }
            builder.setNegativeButton("Close") { dialog, _ -> dialog.dismiss() }
            builder.setCancelable(true)
            builder.show()
        }
    }
}