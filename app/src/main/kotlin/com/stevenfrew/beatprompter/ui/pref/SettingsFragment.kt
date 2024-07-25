package com.stevenfrew.beatprompter.ui.pref

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Message
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.stevenfrew.beatprompter.Preferences
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.events.EventRouter

class SettingsFragment : PreferenceFragmentCompat(),
	SharedPreferences.OnSharedPreferenceChangeListener {
	override fun onSharedPreferenceChanged(prefs: SharedPreferences, key: String?) {
		if (key == getString(R.string.pref_cloudPath_key))
			onCloudPathChanged(prefs.getString(key, null))
	}

	private var settingsHandler: SettingsEventHandler? = null

	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		settingsHandler = MainSettingsEventHandler()
		EventRouter.setSettingsEventHandler(settingsHandler)

		// Load the preferences from an XML resource
		addPreferencesFromResource(R.xml.preferences)

		val coffeePrefName = getString(R.string.pref_buyMeACoffee_key)
		val coffeePref = findPreference<Preference>(coffeePrefName)
		coffeePref?.setOnPreferenceClickListener {
			val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.buyMeACoffeeUrl)))
			startActivity(browserIntent)
			true
		}

		val darkModePrefName = getString(R.string.pref_darkMode_key)
		val darkModePref = findPreference<Preference>(darkModePrefName)
		darkModePref?.setOnPreferenceClickListener {
			Preferences.darkMode = !Preferences.darkMode
			true
		}
	}

	override fun onResume() {
		val bluetoothPreference = findPreference<Preference>("bluetooth_screen_preference")
		requireContext().also {
			bluetoothPreference?.isEnabled = PermissionsPreference.permissionsGranted(
				it,
				PermissionsSettingsFragment.getBluetoothPermissions(it)
			)
			super.onResume()
		}
	}

	override fun onDestroy() {
		Preferences.unregisterOnSharedPreferenceChangeListener(this)
		EventRouter.setSettingsEventHandler(null)
		super.onDestroy()
	}

	private fun onCloudPathChanged(newValue: Any?) {
		val displayPath = Preferences.cloudDisplayPath

		val cloudPathPrefName = getString(R.string.pref_cloudPath_key)
		val cloudPathPref = findPreference<CloudPathPreference>(cloudPathPrefName)

		if (cloudPathPref != null)
			cloudPathPref.summary =
				if (newValue == null) getString(R.string.no_cloud_folder_currently_set) else displayPath
	}

	class MainSettingsEventHandler internal constructor() :
		Handler(), SettingsEventHandler {
		override fun handleMessage(msg: Message) {}
	}

	companion object {
		const val STEVEN_FREW_NAMESPACE = "http://com.stevenfrew/"
	}
}