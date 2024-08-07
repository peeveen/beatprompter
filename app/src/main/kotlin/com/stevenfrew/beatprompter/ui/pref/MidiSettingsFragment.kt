package com.stevenfrew.beatprompter.ui.pref

import androidx.preference.Preference
import com.stevenfrew.beatprompter.R

class MidiSettingsFragment : BaseSettingsFragment(R.xml.midipreferences) {

	override fun onDisplayPreferenceDialog(preference: Preference) =
		if (preference is MidiChannelPreference) {
			val f: MidiChannelPreferenceDialog =
				MidiChannelPreferenceDialog.newInstance(preference.key, preference.singleSelect)
			f.setTargetFragment(this, 0)
			f.show(parentFragmentManager, "seekBarDialog")
		} else {
			super.onDisplayPreferenceDialog(preference)
		}
}