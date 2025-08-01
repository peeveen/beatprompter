package com.stevenfrew.beatprompter.ui.pref

import android.content.Context
import android.util.AttributeSet
import android.widget.TextView
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.stevenfrew.beatprompter.BeatPrompter
import com.stevenfrew.beatprompter.R

class CloudPathPreference(context: Context, attrs: AttributeSet) : Preference(context, attrs) {
	private var inForceUpdate = false

	override fun onBindViewHolder(view: PreferenceViewHolder) {
		super.onBindViewHolder(view)
		val textView = view.findViewById(android.R.id.summary) as TextView
		val path = BeatPrompter.preferences.cloudPath
		textView.text = if (path.isBlank())
			BeatPrompter.appResources.getString(R.string.no_cloud_folder_currently_set)
		else
			BeatPrompter.preferences.cloudDisplayPath
	}

	fun forceUpdate() {
		if (!inForceUpdate) {
			try {
				inForceUpdate = true
				notifyChanged()
			} finally {
				inForceUpdate = false
			}
		}
	}
}