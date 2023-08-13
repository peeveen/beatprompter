package com.stevenfrew.beatprompter.ui.pref

import android.os.Bundle
import android.os.Handler
import android.os.Message
import androidx.preference.PreferenceFragmentCompat
import com.stevenfrew.beatprompter.EventRouter

open class BaseSettingsFragment constructor(private val mPrefsResourceId: Int) :
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

	class NoOpSettingsEventHandler internal constructor(private val mFragment: PreferenceFragmentCompat) :
		Handler(), SettingsEventHandler {
		override fun handleMessage(msg: Message) {}
	}
}