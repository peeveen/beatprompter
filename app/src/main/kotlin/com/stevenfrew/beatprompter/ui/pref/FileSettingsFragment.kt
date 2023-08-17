package com.stevenfrew.beatprompter.ui.pref

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.stevenfrew.beatprompter.EventRouter
import com.stevenfrew.beatprompter.Events
import com.stevenfrew.beatprompter.Preferences
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.storage.FolderInfo
import com.stevenfrew.beatprompter.storage.FolderSelectionListener
import com.stevenfrew.beatprompter.storage.Storage
import com.stevenfrew.beatprompter.storage.StorageType

class FileSettingsFragment : PreferenceFragmentCompat(), FolderSelectionListener,
	SharedPreferences.OnSharedPreferenceChangeListener {
	var mGoogleDriveAuthenticator: ActivityResultLauncher<Intent>? = null

	override fun onSharedPreferenceChanged(prefs: SharedPreferences, key: String?) {
		if (key == getString(R.string.pref_cloudPath_key))
			onCloudPathChanged(prefs.getString(key, null))
	}

	private var mSettingsHandler: FileSettingsEventHandler? = null

	override fun onCreate(savedInstanceState: Bundle?) {
		mGoogleDriveAuthenticator =
			registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
				if (result.resultCode == Activity.RESULT_OK)
					mOnGoogleDriveAuthenticated?.invoke()
				else
					mOnGoogleDriveAuthenticationFailed?.invoke()
			}
		super.onCreate(savedInstanceState)
	}

	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		mSettingsHandler = FileSettingsEventHandler(this)
		EventRouter.setSettingsEventHandler(mSettingsHandler)

		// Load the preferences from an XML resource
		addPreferencesFromResource(R.xml.filepreferences)

		Preferences.registerOnSharedPreferenceChangeListener(this)

		val clearCachePrefName = getString(R.string.pref_clearCache_key)
		val clearCachePref = findPreference<Preference>(clearCachePrefName)
		clearCachePref?.setOnPreferenceClickListener {
			EventRouter.sendEventToSongList(Events.CLEAR_CACHE)
			true
		}

		val cloudPathPrefName = getString(R.string.pref_cloudPath_key)
		val cloudPathPref = findPreference<CloudPathPreference>(cloudPathPrefName)
		cloudPathPref?.setOnPreferenceClickListener {
			EventRouter.sendEventToSettings(Events.SET_CLOUD_PATH)
			true
		}

		val cloudPrefName = getString(R.string.pref_cloudStorageSystem_key)
		val cloudPref = findPreference<ImageListPreference>(cloudPrefName)
		cloudPref?.setOnPreferenceChangeListener { _, value ->
			EventRouter.sendEventToSongList(Events.CLEAR_CACHE)
			Preferences.storageSystem = StorageType.valueOf(value.toString())
			Preferences.cloudPath = null
			Preferences.cloudDisplayPath = null
			cloudPref.forceUpdate()
			true
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

	internal fun setCloudPath() {
		val cloudType = Preferences.storageSystem
		if (cloudType !== StorageType.Demo) {
			val cs = Storage.getInstance(cloudType, this)
			cs.selectFolder(requireActivity(), this)
		} else
			Toast.makeText(activity, getString(R.string.no_cloud_storage_system_set), Toast.LENGTH_LONG)
				.show()
	}

	override fun onFolderSelected(folderInfo: FolderInfo) {
		Preferences.cloudDisplayPath = folderInfo.mDisplayPath
		Preferences.cloudPath = folderInfo.mID
	}

	override fun onFolderSelectedError(t: Throwable) {
		Toast.makeText(activity, t.message, Toast.LENGTH_LONG).show()
	}

	override fun onAuthenticationRequired() {
		// Don't need to do anything.
	}

	override fun shouldCancel(): Boolean {
		return false
	}

	companion object {
		internal var mOnGoogleDriveAuthenticated: (() -> Unit)? = null
		internal var mOnGoogleDriveAuthenticationFailed: (() -> Unit)? = null
	}

	class FileSettingsEventHandler internal constructor(private val mFragment: FileSettingsFragment) :
		Handler(), SettingsEventHandler {
		override fun handleMessage(msg: Message) {
			when (msg.what) {
				Events.SET_CLOUD_PATH -> mFragment.setCloudPath()
			}
		}
	}
}