package com.stevenfrew.beatprompter.storage

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.stevenfrew.beatprompter.BeatPrompter
import com.stevenfrew.beatprompter.R

/**
 * Display adapter for browser items.
 */
internal class BrowserItemListAdapter(items: List<ItemInfo>)
    : ArrayAdapter<ItemInfo>(BeatPrompter.context, -1, items) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val inflater = BeatPrompter.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val rowView = convertView ?: inflater.inflate(R.layout.cloud_browser_item, parent, false)
        val textView = rowView.findViewById<TextView>(R.id.file_or_folder_name)
        val imageView = rowView.findViewById<ImageView>(R.id.file_or_folder_icon)
        val cloudItem = this.getItem(position)
        if (cloudItem != null) {
            val isFolder = cloudItem is FolderInfo
            textView.apply {
                text = cloudItem.mName
                isEnabled = isFolder
            }
            imageView.apply {
                isEnabled = isFolder
                imageView.setImageResource(if (isFolder) R.drawable.ic_folder else R.drawable.ic_document)
            }
        }
        return rowView
    }

    override fun isEnabled(position: Int): Boolean {
        return getItem(position) is FolderInfo
    }
}
