package com.stevenfrew.beatprompter.ui.pref

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.provider.Settings
import androidx.core.app.ActivityCompat
import com.stevenfrew.beatprompter.BuildConfig
import com.stevenfrew.beatprompter.events.EventRouter
import com.stevenfrew.beatprompter.events.Events
import com.stevenfrew.beatprompter.R

class PermissionsSettingsFragment : BaseSettingsFragment(getPermissionPreferences()) {

	private var mSettingsHandler: PermissionsSettingsEventHandler? = null

	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		// Load the preferences from an XML resource
		addPreferencesFromResource(getPermissionPreferences())

		val bluetoothPermissionPrefName = getString(R.string.pref_bluetoothPermission_key)
		val bluetoothPermissionPref = findPreference<PermissionsPreference>(bluetoothPermissionPrefName)
		bluetoothPermissionPref?.setOnPreferenceClickListener {
			EventRouter.sendEventToSettings(Events.ENABLE_BLUETOOTH)
			true
		}
		val storagePermissionPrefName = getString(R.string.pref_localStoragePermission_key)
		val storagePermissionPref = findPreference<PermissionsPreference>(storagePermissionPrefName)
		storagePermissionPref?.setOnPreferenceClickListener {
			EventRouter.sendEventToSettings(Events.ENABLE_STORAGE)
			true
		}
		mSettingsHandler =
			PermissionsSettingsEventHandler(this, arrayOf(bluetoothPermissionPref, storagePermissionPref))
		EventRouter.setSettingsEventHandler(mSettingsHandler)
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

		fun getStoragePermissions(context: Context): Array<out String> {
			return context.resources.getStringArray(
				R.array.storage_permissions
			)
		}
	}

	class PermissionsSettingsEventHandler internal constructor(
		private val mFragment: PermissionsSettingsFragment,
		private val mPermissionPrefs: Array<PermissionsPreference?>
	) :
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

				Events.ENABLE_STORAGE -> {
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
						val uri = Uri.parse("package:${BuildConfig.APPLICATION_ID}")
						mFragment.startActivity(
							Intent(
								Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
								uri
							)
						)
					} else {
						ActivityCompat.requestPermissions(
							mFragment.requireActivity(),
							getStoragePermissions(mFragment.requireContext()),
							1
						)
					}
				}

				Events.PERMISSIONS_UPDATED -> {
					mPermissionPrefs.filterNotNull().forEach {
						it.forceUpdate()
					}
				}
			}
		}
	}
}



