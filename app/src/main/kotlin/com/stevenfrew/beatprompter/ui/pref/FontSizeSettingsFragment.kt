package com.stevenfrew.beatprompter.ui.pref

import androidx.preference.Preference
import com.stevenfrew.beatprompter.R

class FontSizeSettingsFragment : BaseSettingsFragment(R.xml.fontsizepreferences){
	override fun onDisplayPreferenceDialog(preference: Preference) {
		if (preference is FontSizePreference) {
			val f: FontSizePreferenceDialog = FontSizePreferenceDialog.newInstance(preference.key)
			f.setTargetFragment(this, 0)
			f.show(parentFragmentManager, "fontSizeDialog")
		} else {
			super.onDisplayPreferenceDialog(preference)
		}
	}
}