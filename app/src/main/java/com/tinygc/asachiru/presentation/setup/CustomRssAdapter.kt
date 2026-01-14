package com.tinygc.asachiru.presentation.setup

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.tinygc.asachiru.R
import com.tinygc.asachiru.domain.entity.RssFeed

/**
 * カスタムRSSリストのアダプタ
 */
class CustomRssAdapter(
    private val customFeeds: List<RssFeed>,
    private val onDelete: (String) -> Unit
) : RecyclerView.Adapter<CustomRssAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameTextView: TextView = view.findViewById(R.id.custom_rss_name)
        val urlTextView: TextView = view.findViewById(R.id.custom_rss_url)
        val deleteButton: ImageButton = view.findViewById(R.id.delete_button)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_custom_rss, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val feed = customFeeds[position]
        holder.nameTextView.text = feed.name
        holder.urlTextView.text = feed.url
        
        holder.deleteButton.setOnClickListener {
            onDelete(feed.id)
        }
    }

    override fun getItemCount(): Int = customFeeds.size
}
