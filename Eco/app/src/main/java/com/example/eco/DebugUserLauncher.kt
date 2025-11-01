package com.example.eco

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/**
 * Debug-only launcher activity.
 * Immediately starts the real user_page activity and finishes.
 * Exported so it can be started via adb for automated testing of the user page.
 */
class DebugUserLauncher : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Directly launch the user_page activity
        val intent = Intent(this, user_page::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }
}
