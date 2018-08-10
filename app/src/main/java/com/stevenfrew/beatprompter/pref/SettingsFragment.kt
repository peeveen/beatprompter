package com.stevenfrew.beatprompter.pref

import android.content.SharedPreferences
import android.os.Bundle
import android.os.Message
import android.preference.PreferenceFragment
import android.widget.Toast
import com.stevenfrew.beatprompter.BeatPrompterApplication
import com.stevenfrew.beatprompter.EventHandler
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.SongList
import com.stevenfrew.beatprompter.cloud.CloudFolderInfo
import com.stevenfrew.beatprompter.cloud.CloudFolderSelectionListener
import com.stevenfrew.beatprompter.cloud.CloudStorage
import com.stevenfrew.beatprompter.cloud.CloudType

class SettingsFragment : PreferenceFragment(), CloudFolderSelectionListener, SharedPreferences.OnSharedPreferenceChangeListener {
    override fun onSharedPreferenceChanged(prefs: SharedPreferences, key: String?) {
        if (key == getString(R.string.pref_cloudPath_key))
            onCloudPathChanged(prefs.getString(key, null))
    }

    var mSettingsHandler: SettingsEventHandler?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mSettingsHandler = SettingsEventHandler(this)
        EventHandler.setSettingsEventHandler(mSettingsHandler)

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences)

        BeatPrompterApplication.preferences.registerOnSharedPreferenceChangeListener(this)

        val clearCachePrefName = getString(R.string.pref_clearCache_key)
        val clearCachePref = findPreference(clearCachePrefName)
        clearCachePref?.setOnPreferenceClickListener { _ ->
            EventHandler.sendEventToSongList(EventHandler.CLEAR_CACHE)
            true
        }

        val cloudPathPrefName = getString(R.string.pref_cloudPath_key)
        val cloudPathPref = findPreference(cloudPathPrefName)
        cloudPathPref?.setOnPreferenceClickListener { _ ->
            EventHandler.sendEventToSettings(EventHandler.SET_CLOUD_PATH)
            true
        }

        val cloudPrefName = getString(R.string.pref_cloudStorageSystem_key)
        val cloudPref = findPreference(cloudPrefName)
        cloudPref?.setOnPreferenceChangeListener { _, value ->
            EventHandler.sendEventToSongList(EventHandler.CLEAR_CACHE)
            val sharedPref = BeatPrompterApplication.preferences
            val editor = sharedPref.edit()
            editor.putString(getString(R.string.pref_cloudStorageSystem_key), value.toString())
            editor.putString(getString(R.string.pref_cloudPath_key), null)
            editor.putString(getString(R.string.pref_cloudDisplayPath_key), null)
            editor.apply()
            (cloudPref as ImageListPreference).forceUpdate()
            true
        }
    }

    override fun onDestroy() {
        BeatPrompterApplication.preferences.unregisterOnSharedPreferenceChangeListener(this)
        EventHandler.setSettingsEventHandler(null)
        super.onDestroy()
    }

    internal fun onCloudPathChanged(newValue: Any?) {
        val cloudPathPrefName = getString(R.string.pref_cloudPath_key)
        val cloudDisplayPathPrefName = getString(R.string.pref_cloudDisplayPath_key)
        val sharedPrefs = BeatPrompterApplication.preferences
        val displayPath = sharedPrefs.getString(cloudDisplayPathPrefName, null)

        val cloudPathPref = findPreference(cloudPathPrefName)
        if (cloudPathPref != null)
            cloudPathPref.summary = if (newValue == null) getString(R.string.no_cloud_folder_currently_set) else displayPath
    }

    internal fun setCloudPath() {
        val cloudType = SongList.cloud
        if (cloudType !== CloudType.Demo) {
            val cs = CloudStorage.getInstance(cloudType, activity)
            cs.selectFolder(activity, this)
        } else
            Toast.makeText(activity, getString(R.string.no_cloud_storage_system_set), Toast.LENGTH_LONG).show()
    }

    override fun onFolderSelected(folderInfo: CloudFolderInfo) {
        BeatPrompterApplication.preferences
                .edit()
                .putString(getString(R.string.pref_cloudPath_key), folderInfo.mID)
                .putString(getString(R.string.pref_cloudDisplayPath_key), folderInfo.mDisplayPath)
                .apply()
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

    class SettingsEventHandler internal constructor(private val mFragment: SettingsFragment) : EventHandler() {

        override fun handleMessage(msg: Message) {
            when (msg.what) {
                EventHandler.SET_CLOUD_PATH -> mFragment.setCloudPath()
            }
        }

    }
}