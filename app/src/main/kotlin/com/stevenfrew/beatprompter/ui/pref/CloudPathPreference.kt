package com.stevenfrew.beatprompter.ui.pref

import android.content.Context
import android.util.AttributeSet
import android.widget.TextView
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.stevenfrew.beatprompter.BeatPrompter
import com.stevenfrew.beatprompter.Preferences
import com.stevenfrew.beatprompter.R

class CloudPathPreference(context: Context, attrs: AttributeSet) : Preference(context, attrs) {
	override fun onBindViewHolder(view: PreferenceViewHolder) {
		super.onBindViewHolder(view)
		val textView = view.findViewById(android.R.id.summary) as TextView
		val path = Preferences.cloudPath
		var displayPath = Preferences.cloudDisplayPath
		if (path == null)
			displayPath = BeatPrompter.appResources.getString(R.string.no_cloud_folder_currently_set)
		textView.text = displayPath
	}
}