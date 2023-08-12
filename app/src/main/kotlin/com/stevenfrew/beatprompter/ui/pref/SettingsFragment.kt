package com.stevenfrew.beatprompter.ui.pref

import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.preference.PreferenceFragment
import android.widget.Toast
import com.stevenfrew.beatprompter.EventRouter
import com.stevenfrew.beatprompter.Events
import com.stevenfrew.beatprompter.Preferences
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.storage.FolderInfo
import com.stevenfrew.beatprompter.storage.FolderSelectionListener
import com.stevenfrew.beatprompter.storage.Storage
import com.stevenfrew.beatprompter.storage.StorageType

class SettingsFragment : PreferenceFragment(), FolderSelectionListener, SharedPreferences.OnSharedPreferenceChangeListener {
    override fun onSharedPreferenceChanged(prefs: SharedPreferences, key: String?) {
        if (key == getString(R.string.pref_cloudPath_key))
            onCloudPathChanged(prefs.getString(key, null))
    }

    private var mSettingsHandler: SettingsEventHandler? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mSettingsHandler = SettingsEventHandler(this)
        EventRouter.setSettingsEventHandler(mSettingsHandler)

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences)

        Preferences.registerOnSharedPreferenceChangeListener(this)

        val clearCachePrefName = getString(R.string.pref_clearCache_key)
        val clearCachePref = findPreference(clearCachePrefName)
        clearCachePref?.setOnPreferenceClickListener {
            EventRouter.sendEventToSongList(Events.CLEAR_CACHE)
            true
        }

        val cloudPathPrefName = getString(R.string.pref_cloudPath_key)
        val cloudPathPref = findPreference(cloudPathPrefName)
        cloudPathPref?.setOnPreferenceClickListener {
            EventRouter.sendEventToSettings(Events.SET_CLOUD_PATH)
            true
        }

        val cloudPrefName = getString(R.string.pref_cloudStorageSystem_key)
        val cloudPref = findPreference(cloudPrefName)
        cloudPref?.setOnPreferenceChangeListener { _, value ->
            EventRouter.sendEventToSongList(Events.CLEAR_CACHE)
            Preferences.storageSystem = StorageType.valueOf(value.toString())
            Preferences.cloudPath = null
            Preferences.cloudDisplayPath = null
            (cloudPref as ImageListPreference).forceUpdate()
						if(value==StorageType.Local.toString())
								EventRouter.sendEventToSongList(Events.ENABLE_STORAGE)
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
        val cloudPathPref = findPreference(cloudPathPrefName)

        if (cloudPathPref != null)
            cloudPathPref.summary = if (newValue == null) getString(R.string.no_cloud_folder_currently_set) else displayPath
    }

    internal fun setCloudPath() {
        val cloudType = Preferences.storageSystem
        if (cloudType !== StorageType.Demo) {
            val cs = Storage.getInstance(cloudType, activity)
            cs.selectFolder(activity, this)
        } else
            Toast.makeText(activity, getString(R.string.no_cloud_storage_system_set), Toast.LENGTH_LONG).show()
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

    class SettingsEventHandler internal constructor(private val mFragment: SettingsFragment)
        : Handler() {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                Events.SET_CLOUD_PATH -> mFragment.setCloudPath()
            }
        }
    }
}