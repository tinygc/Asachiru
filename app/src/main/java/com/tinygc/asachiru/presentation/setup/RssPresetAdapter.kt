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
    selectedFeeds: List<RssFeed>,
    private val onItemChecked: (RssFeed, Boolean) -> Unit
) : RecyclerView.Adapter<RssPresetAdapter.ViewHolder>() {

    private var selectedFeedIds: Set<String> = selectedFeeds.map { it.id }.toSet()

    init {
        setHasStableIds(true)
    }

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
        val isChecked = selectedFeedIds.contains(name)
        holder.checkBox.setOnCheckedChangeListener(null)
        holder.checkBox.isChecked = isChecked
        
        // チェック変更リスナー
        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            val feed = RssFeed.fromPreset(name, url)
            onItemChecked(feed, isChecked)
        }
    }

    fun updateSelectedFeeds(feeds: List<RssFeed>) {
        val newIds = feeds.map { it.id }.toSet()
        if (newIds == selectedFeedIds) return

        val changedIds = symmetricDifference(selectedFeedIds, newIds)
        selectedFeedIds = newIds

        if (changedIds.isEmpty()) return

        val indexMap = presets.mapIndexed { index, pair -> pair.first to index }.toMap()
        changedIds.forEach { id ->
            indexMap[id]?.let { notifyItemChanged(it) }
        }
    }

    override fun getItemCount(): Int = presets.size

    override fun getItemId(position: Int): Long {
        return presets[position].first.hashCode().toLong()
    }

    private fun symmetricDifference(first: Set<String>, second: Set<String>): Set<String> {
        if (first == second) return emptySet()
        val result = first.toMutableSet()
        for (item in second) {
            if (!result.add(item)) {
                result.remove(item)
            }
        }
        return result
    }
}
