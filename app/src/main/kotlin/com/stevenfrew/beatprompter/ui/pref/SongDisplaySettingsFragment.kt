package com.stevenfrew.beatprompter.ui.pref

import androidx.preference.Preference
import com.stevenfrew.beatprompter.R

class SongDisplaySettingsFragment : BaseSettingsFragment(R.xml.songdisplaypreferences) {

	override fun onDisplayPreferenceDialog(preference: Preference) {
		if (preference is SeekBarPreference) {
			val f: SeekBarPreferenceDialog = SeekBarPreferenceDialog.newInstance(
				preference.key,
				preference.suffix,
				preference.max,
				preference.offset
			)
			f.setTargetFragment(this, 0)
			f.show(parentFragmentManager, "seekBarDialog")
		} else {
			super.onDisplayPreferenceDialog(preference)
		}
	}
}