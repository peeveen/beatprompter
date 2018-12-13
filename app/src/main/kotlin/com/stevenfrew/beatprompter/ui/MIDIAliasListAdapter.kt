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
    private val mLayoutId =
            if (Preferences.largePrint)
                R.layout.midi_alias_list_item_large
            else
                R.layout.midi_alias_list_item

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val inflater = BeatPrompter.context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        return (convertView
                ?: inflater.inflate(mLayoutId, parent, false)).also {
            val titleView = it.findViewById<TextView>(R.id.alias_file_name)
            val errorIcon = it.findViewById<ImageView>(R.id.erroricon)
            val maf = values[position]
            if (maf.mErrors.isEmpty())
                errorIcon.visibility = View.GONE
            titleView.text = maf.mAliasSet.name
        }
    }
}