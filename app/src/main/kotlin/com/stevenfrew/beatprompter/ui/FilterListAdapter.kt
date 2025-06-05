package com.stevenfrew.beatprompter.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.graphics.bitmaps.Bitmap
import com.stevenfrew.beatprompter.ui.filter.Filter
import com.stevenfrew.beatprompter.ui.filter.FolderFilter
import com.stevenfrew.beatprompter.ui.filter.MidiAliasFilesFilter
import com.stevenfrew.beatprompter.ui.filter.MidiCommandsFilter
import com.stevenfrew.beatprompter.ui.filter.SetListFilter
import com.stevenfrew.beatprompter.ui.filter.TagFilter
import com.stevenfrew.beatprompter.ui.filter.TemporarySetListFilter
import com.stevenfrew.beatprompter.ui.filter.UltimateGuitarFilter
import com.stevenfrew.beatprompter.ui.filter.VariationFilter
import com.stevenfrew.beatprompter.util.Utils

class FilterListAdapter(
	private val values: List<Filter>,
	private val selectedTagFilters: MutableList<TagFilter>,
	private val selectedVariationFilters: MutableList<VariationFilter>,
	context: Context,
	private val imageDictionary: Map<String, Bitmap>,
	private val missingIconBitmap: android.graphics.Bitmap,
	private val onSelectedFiltersChanged: () -> Unit
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

	private fun <T> applySelectionClickListener(
		filter: T,
		filterListItem: View,
		filterSelectedImageView: ImageView,
		selectedFilters: MutableList<T>
	) {
		filterSelectedImageView.visibility =
			if (selectedFilters.contains(filter)) View.VISIBLE else View.GONE
		val selectedIcon =
			if (selectedFilters.contains(filter)) R.drawable.tick else R.drawable.blank_icon
		filterSelectedImageView.setImageResource(selectedIcon)
		filterListItem.setOnClickListener {
			if (selectedFilters.contains(filter))
				selectedFilters.remove(filter)
			else
				selectedFilters.add(filter)
			notifyDataSetChanged()
			onSelectedFiltersChanged()
		}
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
			val imageBitmap = when (filter) {
				is TagFilter -> Utils.getIconBitmap(
					null,
					setOf(filter.name),
					imageDictionary,
					missingIconBitmap
				)

				else -> null
			}
			val iconResource = when (filter) {
				is TagFilter -> R.drawable.tag
				is VariationFilter -> R.drawable.variation
				is TemporarySetListFilter -> R.drawable.pencil
				is SetListFilter -> R.drawable.ic_document
				is MidiAliasFilesFilter -> R.drawable.midi
				is MidiCommandsFilter -> R.drawable.midi
				is FolderFilter -> R.drawable.ic_folder
				is UltimateGuitarFilter -> R.drawable.ic_ultimateguitar
				else -> R.drawable.blank_icon
			}
			when (filter) {
				is TagFilter -> applySelectionClickListener(
					filter,
					it,
					filterSelectedIcon,
					selectedTagFilters
				)

				is VariationFilter -> applySelectionClickListener(
					filter,
					it,
					filterSelectedIcon,
					selectedVariationFilters
				)

				else -> filterSelectedIcon.visibility = View.GONE
			}
			if (imageBitmap == null)
				filterIcon.setImageResource(iconResource)
			else
				filterIcon.setImageBitmap(imageBitmap)
			titleView.text = filter.name
		}
}