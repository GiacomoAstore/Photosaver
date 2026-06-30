package com.example.savemedia.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.savemedia.R
import com.example.savemedia.utils.AppLogger
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class DebugActivity : AppCompatActivity() {

    @Inject lateinit var logger: AppLogger

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_debug)

        val recyclerView = findViewById<RecyclerView>(R.id.logRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val logs = logger.getRecentLogs(100)
        recyclerView.adapter = LogAdapter(logs.reversed())
    }
}
