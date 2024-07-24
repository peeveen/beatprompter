package com.stevenfrew.beatprompter.storage

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.stevenfrew.beatprompter.R

/**
 * Display adapter for browser items.
 */
internal class BrowserItemListAdapter(items: List<ItemInfo>, context: Context) :
	ArrayAdapter<ItemInfo>(context, -1, items) {

	override fun getView(position: Int, convertView: View?, parent: ViewGroup): View =
		(context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater).let { inflater ->
			(convertView ?: inflater.inflate(
				R.layout.cloud_browser_item,
				parent,
				false
			)).apply {
				val textView = findViewById<TextView>(R.id.file_or_folder_name)
				val imageView = findViewById<ImageView>(R.id.file_or_folder_icon)
				this@BrowserItemListAdapter.getItem(position)?.also {
					textView.apply {
						text = it.name
						isEnabled = it.isFolder
					}
					imageView.apply {
						isEnabled = it.isFolder
						imageView.setImageResource(it.icon)
					}
				}
			}
		}

	override fun isEnabled(position: Int): Boolean = getItem(position)?.isFolder ?: false

	companion object {
		private val ItemInfo.icon: Int get() = if (this.isFolder) R.drawable.ic_folder else R.drawable.ic_document
	}
}
