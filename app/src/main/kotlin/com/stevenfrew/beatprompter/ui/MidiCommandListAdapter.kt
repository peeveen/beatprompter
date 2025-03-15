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
import com.stevenfrew.beatprompter.midi.alias.Alias

class MidiCommandListAdapter(private val values: List<Alias>, context: Context) :
	ArrayAdapter<Alias>(context, -1, values) {
	private val layoutId =
		if (BeatPrompter.preferences.largePrint)
			R.layout.midi_alias_list_item_large
		else
			R.layout.midi_alias_list_item
	private val inflater = context
		.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

	override fun getView(position: Int, convertView: View?, parent: ViewGroup): View =
		(convertView ?: inflater.inflate(layoutId, parent, false)).also {
			val titleView = it.findViewById<TextView>(R.id.alias_file_name)
			val errorIcon = it.findViewById<ImageView>(R.id.erroricon)
			val alias = values[position]
			errorIcon.visibility = View.GONE
			titleView.text = alias.commandName ?: alias.name
		}
}