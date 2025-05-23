package com.stevenfrew.beatprompter.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.ui.filter.Filter
import com.stevenfrew.beatprompter.ui.filter.FolderFilter
import com.stevenfrew.beatprompter.ui.filter.MidiAliasFilesFilter
import com.stevenfrew.beatprompter.ui.filter.MidiCommandsFilter
import com.stevenfrew.beatprompter.ui.filter.SetListFilter
import com.stevenfrew.beatprompter.ui.filter.TagFilter
import com.stevenfrew.beatprompter.ui.filter.TemporarySetListFilter
import com.stevenfrew.beatprompter.ui.filter.UltimateGuitarFilter

class FilterListAdapter(
	private val values: List<Filter>,
	private val selectedTagFilters: MutableList<TagFilter>,
	context: Context,
	private val onSelectedTagsChanged: () -> Unit
) :
	ArrayAdapter<Filter>(context, -1, values) {
	private val inflater = context
		.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

	override fun getView(position: Int, convertView: View?, parent: ViewGroup): View =
		(convertView ?: inflater.inflate(R.layout.filter_item_selected, parent, false)).also {
			val titleView = it.findViewById<TextView>(R.id.filtertitleselected)
			val filter = values[position]
			titleView.text = filter.name
		}

	override fun getDropDownView(
		position: Int,
		convertView: View?,
		parent: ViewGroup
	): View =
		inflater.inflate(R.layout.filter_list_item, parent, false).also {
			val titleView = it.findViewById<TextView>(R.id.filtertitle)
			val filterIcon = it.findViewById<ImageView>(R.id.filterIcon)
			val filterSelectedIcon = it.findViewById<ImageView>(R.id.filterSelectedIcon)
			val filter = values[position]
			val iconResource = when (filter) {
				is TagFilter -> R.drawable.tag
				is TemporarySetListFilter -> R.drawable.pencil
				is SetListFilter -> R.drawable.ic_document
				is MidiAliasFilesFilter -> R.drawable.midi
				is MidiCommandsFilter -> R.drawable.midi
				is FolderFilter -> R.drawable.ic_folder
				is UltimateGuitarFilter -> R.drawable.ic_ultimateguitar
				else -> R.drawable.blank_icon
			}
			if (filter is TagFilter) {
				filterSelectedIcon.visibility = View.VISIBLE
				val selectedIcon =
					if (selectedTagFilters.contains(filter)) R.drawable.tick else R.drawable.blank_icon
				filterSelectedIcon.setImageResource(selectedIcon)
				it.setOnClickListener {
					if (selectedTagFilters.contains(filter))
						selectedTagFilters.remove(filter)
					else
						selectedTagFilters.add(filter)
					notifyDataSetChanged()
					onSelectedTagsChanged()
				}
			} else
				filterSelectedIcon.visibility = View.GONE
			filterIcon.setImageResource(iconResource)
			titleView.text = filter.name
		}
}