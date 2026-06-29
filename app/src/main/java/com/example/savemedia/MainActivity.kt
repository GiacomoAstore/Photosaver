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
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.savemedia.adapters.MediaAdapter
import com.example.savemedia.services.MediaSaveService
import com.example.savemedia.services.ScreenCaptureService
import com.example.savemedia.utils.FileManager
import com.example.savemedia.utils.PermissionHelper

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var fileCountText: TextView
    private lateinit var recentMediaList: RecyclerView
    private lateinit var fileManager: FileManager

    private var screenCaptureService: ScreenCaptureService? = null
    private var isBound = false

    private val REQUEST_MEDIA_PROJECTION = 1001
    private val REQUEST_PERMISSIONS = 1002

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

        fileManager = FileManager(this)

        statusText = findViewById(R.id.statusText)
        fileCountText = findViewById(R.id.fileCountText)
        recentMediaList = findViewById(R.id.recentMediaList)
        recentMediaList.layoutManager = LinearLayoutManager(this)

        val btnStartService = findViewById<Button>(R.id.btnStartService)
        val btnStopService = findViewById<Button>(R.id.btnStopService)
        val btnStartCapture = findViewById<Button>(R.id.btnStartCapture)
        val btnOpenFolder = findViewById<Button>(R.id.btnOpenFolder)

        btnStartService.setOnClickListener {
            if (PermissionHelper.hasPermissions(this)) {
                startForegroundService(Intent(this, MediaSaveService::class.java))
                statusText.text = "Active"
            } else {
                PermissionHelper.requestPermissions(this, REQUEST_PERMISSIONS)
            }
        }

        btnStopService.setOnClickListener {
            stopService(Intent(this, MediaSaveService::class.java))
            statusText.text = "Inactive"
        }

        btnStartCapture.setOnClickListener {
            val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            startActivityForResult(manager.createScreenCaptureIntent(), REQUEST_MEDIA_PROJECTION)
        }

        btnOpenFolder.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.type = "image/*"
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        }

        updateUI()

        Intent(this, ScreenCaptureService::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    private fun updateUI() {
        fileCountText.text = fileManager.getSavedFilesCount().toString()
        // Here we would ideally load the actual SavedMedia list from MediaStore or DB
        // For now, it stays empty or we could add a dummy if needed for verification
        recentMediaList.adapter = MediaAdapter(emptyList())
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_MEDIA_PROJECTION) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                screenCaptureService?.startCapture(resultCode, data)
                Toast.makeText(this, "Capture started", Toast.LENGTH_SHORT).show()
                updateUI()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            unbindService(connection)
            isBound = false
        }
    }
}
