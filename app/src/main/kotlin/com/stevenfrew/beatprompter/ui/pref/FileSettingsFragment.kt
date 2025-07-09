package com.stevenfrew.beatprompter.ui.pref

import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.stevenfrew.beatprompter.BeatPrompter
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.events.EventRouter
import com.stevenfrew.beatprompter.events.Events
import com.stevenfrew.beatprompter.storage.FolderInfo
import com.stevenfrew.beatprompter.storage.FolderSelectionListener
import com.stevenfrew.beatprompter.storage.Storage
import com.stevenfrew.beatprompter.storage.StorageType
import com.stevenfrew.beatprompter.util.Utils

class FileSettingsFragment : PreferenceFragmentCompat(),
	SharedPreferences.OnSharedPreferenceChangeListener {
	val mGoogleDriveAuthenticator: ActivityResultLauncher<Intent> =
		registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
			if (result.resultCode == Activity.RESULT_OK)
				mOnGoogleDriveAuthenticated?.invoke()
			else
				mOnGoogleDriveAuthenticationFailed?.invoke()
		}

	override fun onSharedPreferenceChanged(prefs: SharedPreferences, key: String?) {
		if (key == getString(R.string.pref_cloudPath_key))
			onCloudPathChanged(prefs.getString(key, null))
		if (key == getString(R.string.pref_cloudStorageSystem_key)) {
			BeatPrompter.preferences.cloudPath = ""
			BeatPrompter.preferences.dropboxAccessToken = ""
			BeatPrompter.preferences.dropboxRefreshToken = ""
			BeatPrompter.preferences.dropboxExpiryTime = 0
			BeatPrompter.preferences.cloudDisplayPath = ""
		}
	}

	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		// Load the preferences from an XML resource
		addPreferencesFromResource(R.xml.filepreferences)

		BeatPrompter.preferences.registerOnSharedPreferenceChangeListener(this)

		val clearCachePrefName = getString(R.string.pref_clearCache_key)
		val clearCachePref = findPreference<Preference>(clearCachePrefName)
		clearCachePref?.setOnPreferenceClickListener {
			EventRouter.sendEventToCache(Events.CLEAR_CACHE, true)
			true
		}

		val cloudPathPrefName = getString(R.string.pref_cloudPath_key)
		val cloudPathPref = findPreference<CloudPathPreference>(cloudPathPrefName)
		cloudPathPref?.setOnPreferenceClickListener {
			setCloudPath()
			cloudPathPref.forceUpdate()
			true
		}

		val cloudPrefName = getString(R.string.pref_cloudStorageSystem_key)
		val cloudPref = findPreference<ImageListPreference>(cloudPrefName)
		cloudPref?.setOnPreferenceChangeListener { _, value ->
			EventRouter.sendEventToCache(Events.CLEAR_CACHE, true)
			BeatPrompter.preferences.storageSystem = StorageType.valueOf(value.toString())
			BeatPrompter.preferences.cloudPath = ""
			BeatPrompter.preferences.cloudDisplayPath = ""
			cloudPref.forceUpdate()
			true
		}
	}

	override fun onDestroy() {
		BeatPrompter.preferences.unregisterOnSharedPreferenceChangeListener(this)
		EventRouter.setSettingsEventHandler(null)
		super.onDestroy()
	}

	private fun onCloudPathChanged(newValue: Any?) {
		val displayPath = BeatPrompter.preferences.cloudDisplayPath

		val cloudPathPrefName = getString(R.string.pref_cloudPath_key)
		val cloudPathPref = findPreference<CloudPathPreference>(cloudPathPrefName)

		if (cloudPathPref != null) {
			cloudPathPref.summary =
				if (newValue == null) getString(R.string.no_cloud_folder_currently_set) else displayPath
			cloudPathPref.forceUpdate()
		}
	}

	private fun setCloudPath() {
		val cloudType = BeatPrompter.preferences.storageSystem
		if (cloudType !== StorageType.Demo) {
			val cs = Storage.getInstance(cloudType, this)
			val progressDialog =
				ProgressDialog(requireContext()).apply {
					setTitle(getString(R.string.accessingCloudStorage, cs.cloudStorageName))
					setMessage(getString(R.string.gettingRootFolder))
					setCancelable(false)
					isIndeterminate = true
					show()
				}
			cs.selectFolder(requireActivity(), object : FolderSelectionListener {
				override fun onFolderSelected(folderInfo: FolderInfo) {
					BeatPrompter.preferences.cloudDisplayPath = folderInfo.displayPath
					BeatPrompter.preferences.cloudPath = folderInfo.id
				}

				override fun onFolderSelectedError(t: Throwable, context: Context) =
					Utils.showExceptionDialog(t, context)

				override fun onFolderSelectionComplete() = progressDialog.dismiss()

				override fun shouldCancel(): Boolean = false
			})
		} else
			Toast.makeText(activity, getString(R.string.no_cloud_storage_system_set), Toast.LENGTH_LONG)
				.show()
	}

	companion object {
		internal var mOnGoogleDriveAuthenticated: (() -> Unit)? = null
		internal var mOnGoogleDriveAuthenticationFailed: (() -> Unit)? = null
	}
}