package com.stevenfrew.beatprompter.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.stevenfrew.beatprompter.BeatPrompter
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.ui.filter.*

class FilterListAdapter(private val values: List<Filter>)
    : ArrayAdapter<Filter>(BeatPrompter.context, -1, values) {
    private val mInflater = BeatPrompter.context
            .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return (convertView
                ?: mInflater.inflate(R.layout.filter_item_selected, parent, false)).also {
            val titleView = it.findViewById<TextView>(R.id.filtertitleselected)
            val filter = values[position]
            titleView.text = filter.mName
        }
    }

    override fun getDropDownView(position: Int, convertView: View?,
                                 parent: ViewGroup): View {
        return (convertView ?: mInflater.inflate(R.layout.filter_list_item, parent, false)).also {
            val titleView = it.findViewById<TextView>(R.id.filtertitle)
            val filterIcon = it.findViewById<ImageView>(R.id.filterIcon)
            val filter = values[position]
            val iconResource = when (filter) {
                is TagFilter -> R.drawable.tag
                is TemporarySetListFilter -> R.drawable.pencil
                is SetListFilter -> R.drawable.ic_document
                is MIDIAliasFilesFilter -> R.drawable.midi
                is FolderFilter -> R.drawable.ic_folder
                else -> R.drawable.blank_icon
            }
            filterIcon.setImageResource(iconResource)
            titleView.text = filter.mName
        }
    }
}