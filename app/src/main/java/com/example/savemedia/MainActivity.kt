package com.example.savemedia

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.savemedia.adapters.MediaAdapter
import com.example.savemedia.services.OverlayService
import com.example.savemedia.ui.DebugActivity
import com.example.savemedia.ui.MainViewModel
import com.example.savemedia.ui.OnboardingActivity
import com.example.savemedia.utils.AppLogger
import com.example.savemedia.utils.CaptureBridge
import com.example.savemedia.utils.FileManager
import com.example.savemedia.utils.PermissionManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var fileCountText: TextView
    private lateinit var statusText: TextView
    private lateinit var recentMediaList: RecyclerView

    @Inject lateinit var fileManager: FileManager
    @Inject lateinit var logger: AppLogger
    @Inject lateinit var captureBridge: CaptureBridge
    @Inject lateinit var permissionManager: PermissionManager

    private val viewModel: MainViewModel by viewModels()

    private val mediaProjectionLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            captureBridge.resultCode = result.resultCode
            captureBridge.captureIntent = result.data
            Toast.makeText(this, "Cattura automatica ATTIVA", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (!permissionManager.hasStoragePermission() || !permissionManager.hasOverlayPermission() || !permissionManager.hasAccessibilityPermission()) {
            startActivity(Intent(this, OnboardingActivity::class.java))
        }

        fileCountText = findViewById(R.id.fileCountText)
        statusText = findViewById(R.id.statusText)
        recentMediaList = findViewById(R.id.recentMediaList)
        recentMediaList.layoutManager = LinearLayoutManager(this)

        findViewById<Button>(R.id.btnStartService).setOnClickListener {
            startService(Intent(this, OverlayService::class.java))
            updateStatus()
        }

        findViewById<Button>(R.id.btnStopService).setOnClickListener {
            stopService(Intent(this, OverlayService::class.java))
            updateStatus()
        }

        findViewById<Button>(R.id.btnOpenDebug).setOnClickListener {
            startActivity(Intent(this, DebugActivity::class.java))
        }

        findViewById<Button>(R.id.btnStartCapture).setOnClickListener {
            val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjectionLauncher.launch(manager.createScreenCaptureIntent())
        }

        findViewById<Button>(R.id.btnOpenFolder).setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.type = "image/*"
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        }

        lifecycleScope.launch {
            viewModel.state.collectLatest { state ->
                fileCountText.text = state.savedCount.toString()
                recentMediaList.adapter = MediaAdapter(state.recentMedia.map { entity ->
                    com.example.savemedia.models.SavedMedia(
                        uri = android.net.Uri.parse(entity.filePath),
                        fileName = entity.fileName,
                        timestamp = entity.timestamp,
                        appName = entity.sourceApp
                    )
                })
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun updateStatus() {
        val isActive = permissionManager.hasAccessibilityPermission()
        statusText.text = if (isActive) "ACTIVE" else "INACTIVE"
        statusText.setTextColor(if (isActive) android.graphics.Color.GREEN else android.graphics.Color.RED)
    }

    private fun requestMediaProjection() {
        val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjectionLauncher.launch(manager.createScreenCaptureIntent())
    }
}
