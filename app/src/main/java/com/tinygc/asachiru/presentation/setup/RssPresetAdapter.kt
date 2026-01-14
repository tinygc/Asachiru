package com.tinygc.asachiru.presentation.setup

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.recyclerview.widget.RecyclerView
import com.tinygc.asachiru.R
import com.tinygc.asachiru.domain.entity.RssFeed

/**
 * RSSプリセットチェックボックスリストのアダプタ
 */
class RssPresetAdapter(
    private val presets: List<Pair<String, String>>, // (name, url)
    private val selectedFeeds: List<RssFeed>,
    private val onItemChecked: (RssFeed, Boolean) -> Unit
) : RecyclerView.Adapter<RssPresetAdapter.ViewHolder>() {

    class ViewHolder(val checkBox: CheckBox) : RecyclerView.ViewHolder(checkBox)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val checkBox = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_rss_preset_checkbox, parent, false) as CheckBox
        return ViewHolder(checkBox)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (name, url) = presets[position]
        holder.checkBox.text = name
        
        // 選択状態を設定
        val isChecked = selectedFeeds.any { it.id == name }
        holder.checkBox.isChecked = isChecked
        
        // チェック変更リスナー
        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            val feed = RssFeed.fromPreset(name, url)
            onItemChecked(feed, isChecked)
        }
    }

    override fun getItemCount(): Int = presets.size
}
