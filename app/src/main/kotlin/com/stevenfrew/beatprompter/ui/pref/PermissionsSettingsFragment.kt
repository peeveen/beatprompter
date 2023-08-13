package com.stevenfrew.beatprompter.ui.pref

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Message
import androidx.core.app.ActivityCompat
import com.stevenfrew.beatprompter.EventRouter
import com.stevenfrew.beatprompter.Events
import com.stevenfrew.beatprompter.R

class PermissionsSettingsFragment : BaseSettingsFragment(getPermissionPreferences()) {

	private var mSettingsHandler: PermissionsSettingsEventHandler? = null

	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		mSettingsHandler = PermissionsSettingsEventHandler(this)
		EventRouter.setSettingsEventHandler(mSettingsHandler)

		// Load the preferences from an XML resource
		addPreferencesFromResource(getPermissionPreferences())

		val bluetoothPermissionPrefName = getString(R.string.pref_bluetoothPermission_key)
		val bluetoothPermissionPref = findPreference<PermissionsPreference>(bluetoothPermissionPrefName)
		bluetoothPermissionPref?.setOnPreferenceClickListener {
			EventRouter.sendEventToSettings(Events.ENABLE_BLUETOOTH)
			true
		}
	}

	override fun onDestroy() {
		EventRouter.setSettingsEventHandler(null)
		super.onDestroy()
	}

	companion object {
		fun getPermissionPreferences(): Int {
			return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) R.xml.permissionpreferences12 else R.xml.permissionpreferences
		}

		fun getBluetoothPermissions(context: Context): Array<out String> {
			return context.resources.getStringArray(
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) R.array.bluetooth_permissions_12 else R.array.bluetooth_permissions
			)
		}
	}

	class PermissionsSettingsEventHandler internal constructor(private val mFragment: PermissionsSettingsFragment) :
		Handler(), SettingsEventHandler {
		override fun handleMessage(msg: Message) {
			when (msg.what) {
				Events.ENABLE_BLUETOOTH -> {
					ActivityCompat.requestPermissions(
						mFragment.requireActivity(),
						getBluetoothPermissions(mFragment.requireContext()),
						1
					)
				}
			}
		}
	}
}



