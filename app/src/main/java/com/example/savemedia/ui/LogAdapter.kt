package com.example.savemedia.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.savemedia.R
import com.example.savemedia.utils.LogEntry
import java.text.SimpleDateFormat
import java.util.*

class LogAdapter(private val logs: List<LogEntry>) : RecyclerView.Adapter<LogAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(android.R.id.text1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_1, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val log = logs[position]
        val time = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(log.timestamp))
        holder.textView.text = "$time | ${log.level} | ${log.component} | ${log.message}"
        val color = when (log.level) {
            com.example.savemedia.utils.LogLevel.ERROR, com.example.savemedia.utils.LogLevel.CRITICAL -> 0xFFFF0000.toInt()
            com.example.savemedia.utils.LogLevel.WARNING -> 0xFFFFA500.toInt()
            else -> 0xFF000000.toInt()
        }
        holder.textView.setTextColor(color)
    }

    override fun getItemCount() = logs.size
}
