package com.example.savemedia.ui

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.savemedia.R
import com.example.savemedia.utils.PermissionManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class OnboardingActivity : AppCompatActivity() {

    @Inject lateinit var permissionManager: PermissionManager

    private lateinit var statusStorage: TextView
    private lateinit var statusOverlay: TextView
    private lateinit var statusAccessibility: TextView
    private lateinit var btnContinue: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        statusStorage = findViewById(R.id.statusStorage)
        statusOverlay = findViewById(R.id.statusOverlay)
        statusAccessibility = findViewById(R.id.statusAccessibility)
        btnContinue = findViewById(R.id.btnContinue)

        findViewById<Button>(R.id.btnFixPermissions).setOnClickListener {
            if (!permissionManager.hasStoragePermission()) {
                permissionManager.requestStoragePermission(this, 100)
            } else if (!permissionManager.hasOverlayPermission()) {
                permissionManager.requestOverlayPermission(this)
            } else if (!permissionManager.hasAccessibilityPermission()) {
                permissionManager.requestAccessibilityPermission(this)
            }
        }

        btnContinue.setOnClickListener {
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }

    private fun updateUI() {
        val s = permissionManager.hasStoragePermission()
        val o = permissionManager.hasOverlayPermission()
        val a = permissionManager.hasAccessibilityPermission()

        statusStorage.text = if (s) "✅ Storage Permission" else "❌ Storage Permission"
        statusOverlay.text = if (o) "✅ Overlay Permission" else "❌ Overlay Permission"
        statusAccessibility.text = if (a) "✅ Accessibility Permission" else "❌ Accessibility Permission"

        btnContinue.isEnabled = s && o && a
    }
}
