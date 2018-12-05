package com.stevenfrew.beatprompter.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.ui.filter.*

class FilterListAdapter(private val values: List<Filter>) : ArrayAdapter<Filter>(BeatPrompterApplication.context, -1, values) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val inflater = BeatPrompterApplication.context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val rowView = convertView ?: inflater.inflate(R.layout.filter_item_selected, parent, false)
        val titleView = rowView.findViewById<TextView>(R.id.filtertitleselected)
        val filter = values[position]
        titleView.text = filter.mName
        return rowView
    }

    override fun getDropDownView(position: Int, convertView: View?,
                                 parent: ViewGroup): View {
        val inflater = BeatPrompterApplication.context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val dropDownView = convertView ?: inflater.inflate(R.layout.filter_list_item, parent, false)
        val titleView = dropDownView.findViewById<TextView>(R.id.filtertitle)
        val filterIcon = dropDownView.findViewById<ImageView>(R.id.filterIcon)
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
        return dropDownView
    }
}