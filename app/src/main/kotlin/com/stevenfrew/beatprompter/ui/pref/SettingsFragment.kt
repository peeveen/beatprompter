package com.stevenfrew.beatprompter.ui.pref

import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Message
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.stevenfrew.beatprompter.EventRouter
import com.stevenfrew.beatprompter.Preferences
import com.stevenfrew.beatprompter.R

class SettingsFragment : PreferenceFragmentCompat(),
	SharedPreferences.OnSharedPreferenceChangeListener {
	override fun onSharedPreferenceChanged(prefs: SharedPreferences, key: String?) {
		if (key == getString(R.string.pref_cloudPath_key))
			onCloudPathChanged(prefs.getString(key, null))
	}

	private var mSettingsHandler: SettingsEventHandler? = null

	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		mSettingsHandler = MainSettingsEventHandler(this)
		EventRouter.setSettingsEventHandler(mSettingsHandler)

		// Load the preferences from an XML resource
		addPreferencesFromResource(R.xml.preferences)
	}

	override fun onResume() {
		val bluetoothPreference = findPreference<Preference>("bluetooth_screen_preference")
		val context = this.requireContext()
		bluetoothPreference?.isEnabled = PermissionsPreference.permissionsGranted(
			context,
			PermissionsSettingsFragment.getBluetoothPermissions(context)
		)
		super.onResume()
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

	class MainSettingsEventHandler internal constructor(private val mFragment: SettingsFragment) :
		Handler(), SettingsEventHandler {
		override fun handleMessage(msg: Message) {}
	}

	companion object {
		const val StevenFrewNamespace = "http://com.stevenfrew/"
	}
}