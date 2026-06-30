package com.example.savemedia

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.os.IBinder
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.savemedia.adapters.MediaAdapter
import com.example.savemedia.services.MediaSaveService
import com.example.savemedia.services.ScreenCaptureService
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

    private lateinit var statusText: TextView
    private lateinit var fileCountText: TextView
    private lateinit var recentMediaList: RecyclerView

    @Inject lateinit var fileManager: FileManager
    @Inject lateinit var logger: AppLogger
    @Inject lateinit var captureBridge: CaptureBridge
    @Inject lateinit var permissionManager: PermissionManager

    private val viewModel: MainViewModel by viewModels()

    private var screenCaptureService: ScreenCaptureService? = null
    private var isBound = false

    private val mediaProjectionLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            captureBridge.resultCode = result.resultCode
            captureBridge.captureIntent = result.data
            Toast.makeText(this, "Auto-capture enabled", Toast.LENGTH_SHORT).show()
        }
    }

    private val permissionLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        if (permissions.values.all { it }) {
            startForegroundService(Intent(this, MediaSaveService::class.java))
            statusText.text = "Active"
            requestMediaProjection()
        }
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as ScreenCaptureService.LocalBinder
            screenCaptureService = binder.getService()
            isBound = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            isBound = false
            screenCaptureService = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (!permissionManager.hasStoragePermission() || !permissionManager.hasOverlayPermission() || !permissionManager.hasAccessibilityPermission()) {
            startActivity(Intent(this, OnboardingActivity::class.java))
        }

        statusText = findViewById(R.id.statusText)
        fileCountText = findViewById(R.id.fileCountText)
        recentMediaList = findViewById(R.id.recentMediaList)
        recentMediaList.layoutManager = LinearLayoutManager(this)

        findViewById<Button>(R.id.btnStartService).setOnClickListener {
            if (permissionManager.hasStoragePermission()) {
                startForegroundService(Intent(this, MediaSaveService::class.java))
                statusText.text = "Active"
                requestMediaProjection()
            } else {
                // For simplicity, we just trigger standard request, but ideally use permissionLauncher
                permissionManager.requestStoragePermission(this, 1002)
            }
        }

        findViewById<Button>(R.id.btnStopService).setOnClickListener {
            stopService(Intent(this, MediaSaveService::class.java))
            statusText.text = "Inactive"
        }

        findViewById<Button>(R.id.btnStartCapture).setOnClickListener {
            if (captureBridge.hasPermission()) {
                // We already have a MediaProjection token — trigger a real capture now
                Toast.makeText(this, "Capturing screen...", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, ScreenCaptureService::class.java).apply {
                    action = ScreenCaptureService.ACTION_START_CAPTURE
                    putExtra(ScreenCaptureService.EXTRA_RESULT_CODE, captureBridge.resultCode)
                    putExtra(ScreenCaptureService.EXTRA_DATA, captureBridge.captureIntent)
                    putExtra(ScreenCaptureService.EXTRA_APP_NAME, "ManualTest")
                }
                startForegroundService(intent)
            } else {
                // No token yet — request MediaProjection permission first
                Toast.makeText(this, "Requesting screen capture permission...", Toast.LENGTH_SHORT).show()
                requestMediaProjection()
            }
        }

        findViewById<Button>(R.id.btnOpenFolder).setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.type = "image/*"
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        }

        findViewById<Button>(R.id.btnOpenDebug).setOnClickListener {
            startActivity(Intent(this, DebugActivity::class.java))
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

        Intent(this, ScreenCaptureService::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    private fun requestMediaProjection() {
        val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjectionLauncher.launch(manager.createScreenCaptureIntent())
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            unbindService(connection)
            isBound = false
        }
    }
}
