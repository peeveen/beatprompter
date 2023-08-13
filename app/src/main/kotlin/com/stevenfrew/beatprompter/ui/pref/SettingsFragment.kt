package com.stevenfrew.beatprompter.ui.pref

import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.widget.Toast
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceScreen
import com.stevenfrew.beatprompter.EventRouter
import com.stevenfrew.beatprompter.Events
import com.stevenfrew.beatprompter.Preferences
import com.stevenfrew.beatprompter.R
import com.stevenfrew.beatprompter.storage.FolderInfo
import com.stevenfrew.beatprompter.storage.FolderSelectionListener
import com.stevenfrew.beatprompter.storage.Storage
import com.stevenfrew.beatprompter.storage.StorageType

class SettingsFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {
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

        Preferences.registerOnSharedPreferenceChangeListener(this)
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
            cloudPathPref.summary = if (newValue == null) getString(R.string.no_cloud_folder_currently_set) else displayPath
    }

    class MainSettingsEventHandler internal constructor(private val mFragment: SettingsFragment)
        : Handler(), SettingsEventHandler {
        override fun handleMessage(msg: Message) { }
    }
}