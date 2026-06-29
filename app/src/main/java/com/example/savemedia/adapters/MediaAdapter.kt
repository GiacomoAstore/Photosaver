package com.example.savemedia.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.savemedia.R
import com.example.savemedia.models.SavedMedia
import java.text.SimpleDateFormat
import java.util.*

class MediaAdapter(private val mediaList: List<SavedMedia>) : RecyclerView.Adapter<MediaAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.mediaThumb)
        val titleView: TextView = view.findViewById(R.id.mediaTitle)
        val dateView: TextView = view.findViewById(R.id.mediaDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_media, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val media = mediaList[position]
        holder.titleView.text = media.appName
        val date = Date(media.timestamp)
        holder.dateView.text = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(date)
        holder.imageView.setImageURI(media.uri)
    }

    override fun getItemCount() = mediaList.size
}
