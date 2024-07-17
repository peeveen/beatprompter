package com.stevenfrew.beatprompter.ui.pref

import android.os.Bundle
import android.os.Handler
import android.os.Message
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.stevenfrew.beatprompter.events.EventRouter

open class BaseSettingsFragment(private val mPrefsResourceId: Int) :
	PreferenceFragmentCompat() {
	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		EventRouter.setSettingsEventHandler(NoOpSettingsEventHandler(this))

		// Load the preferences from an XML resource
		addPreferencesFromResource(mPrefsResourceId)
	}

	override fun onDestroy() {
		EventRouter.setSettingsEventHandler(null)
		super.onDestroy()
	}

	override fun onDisplayPreferenceDialog(preference: Preference) =
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

	class NoOpSettingsEventHandler internal constructor(private val mFragment: PreferenceFragmentCompat) :
		Handler(), SettingsEventHandler {
		override fun handleMessage(msg: Message) {}
	}
}