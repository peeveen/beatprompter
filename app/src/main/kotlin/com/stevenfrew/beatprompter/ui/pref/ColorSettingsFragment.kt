package com.stevenfrew.beatprompter.ui.pref

import androidx.preference.Preference
import com.rarepebble.colorpicker.ColorPreference
import com.stevenfrew.beatprompter.R

class ColorSettingsFragment : BaseSettingsFragment(R.xml.colorpreferences) {
	override fun onDisplayPreferenceDialog(preference: Preference) {
		if (preference is ColorPreference)
			preference.showDialog(this, 0)
		else
			super.onDisplayPreferenceDialog(preference)
	}
}