package com.stevenfrew.beatprompter.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.stevenfrew.beatprompter.BeatPrompter
import com.stevenfrew.beatprompter.Preferences
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.cache.MIDIAliasFile

class MIDIAliasListAdapter(private val values: List<MIDIAliasFile>) : ArrayAdapter<MIDIAliasFile>(BeatPrompter.context, -1, values) {
    private val mLargePrint = Preferences.largePrint

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val inflater = BeatPrompter.context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val rowView = convertView
                ?: inflater.inflate(if (mLargePrint) R.layout.midi_alias_list_item_large else R.layout.midi_alias_list_item, parent, false)
        val titleView = rowView.findViewById<TextView>(R.id.alias_file_name)
        val errorIcon = rowView.findViewById<ImageView>(R.id.erroricon)
        val maf = values[position]
        if (maf.mErrors.isEmpty())
            errorIcon.visibility = View.GONE
        titleView.text = maf.mAliasSet.name
        return rowView
    }
}