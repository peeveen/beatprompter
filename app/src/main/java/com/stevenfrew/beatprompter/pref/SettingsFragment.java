package com.stevenfrew.beatprompter.pref;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Message;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.widget.Toast;

import com.stevenfrew.beatprompter.BeatPrompterApplication;
import com.stevenfrew.beatprompter.EventHandler;
import com.stevenfrew.beatprompter.R;
import com.stevenfrew.beatprompter.SongList;
import com.stevenfrew.beatprompter.cloud.CloudFolderInfo;
import com.stevenfrew.beatprompter.cloud.CloudFolderSelectionListener;
import com.stevenfrew.beatprompter.cloud.CloudStorage;
import com.stevenfrew.beatprompter.cloud.CloudType;

public class SettingsFragment extends PreferenceFragment implements CloudFolderSelectionListener
{
    public SettingsEventHandler mSettingsHandler;

    SharedPreferences.OnSharedPreferenceChangeListener mCloudPathPrefListener = (sharedPreferences, key) -> {
        if(key.equals(getString(R.string.pref_cloudPath_key)))
            onCloudPathChanged(sharedPreferences.getString(key,null));
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSettingsHandler= new SettingsEventHandler(this);
        EventHandler.setSettingsEventHandler(mSettingsHandler);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);

        BeatPrompterApplication.getPreferences().registerOnSharedPreferenceChangeListener(mCloudPathPrefListener);

        String clearCachePrefName=getString(R.string.pref_clearCache_key);
        Preference clearCachePref = findPreference(clearCachePrefName);
        if(clearCachePref!=null) {
            clearCachePref.setOnPreferenceClickListener(preference -> {
                EventHandler.sendEventToSongList(EventHandler.CLEAR_CACHE);
                return true;
            });
        }

        String cloudPathPrefName=getString(R.string.pref_cloudPath_key);
        final Preference cloudPathPref = findPreference(cloudPathPrefName);
        if(cloudPathPref!=null) {
            cloudPathPref.setOnPreferenceClickListener(preference -> {
                EventHandler.sendEventToSettings(EventHandler.SET_CLOUD_PATH);
                return true;
            });
        }

        String cloudPrefName=getString(R.string.pref_cloudStorageSystem_key);
        final Preference cloudPref = findPreference(cloudPrefName);
        if(cloudPref!=null) {
            cloudPref.setOnPreferenceChangeListener((preference, value) -> {
                EventHandler.sendEventToSongList(EventHandler.CLEAR_CACHE);
                SharedPreferences sharedPref= BeatPrompterApplication.getPreferences();
                SharedPreferences.Editor editor=sharedPref.edit();
                editor.putString(getString(R.string.pref_cloudStorageSystem_key),value.toString());
                editor.putString(getString(R.string.pref_cloudPath_key),null);
                editor.putString(getString(R.string.pref_cloudDisplayPath_key),null);
                editor.apply();
                ((ImageListPreference)cloudPref).forceUpdate();
                return true;
            });
        }
    }

    @Override
    public void onDestroy()
    {
        BeatPrompterApplication.getPreferences().unregisterOnSharedPreferenceChangeListener(mCloudPathPrefListener);
        EventHandler.setSettingsEventHandler(null);
        super.onDestroy();
    }

    void onCloudPathChanged(Object newValue)
    {
        String cloudPathPrefName=getString(R.string.pref_cloudPath_key);
        String cloudDisplayPathPrefName=getString(R.string.pref_cloudDisplayPath_key);
        SharedPreferences sharedPrefs=BeatPrompterApplication.getPreferences();
        String displayPath=sharedPrefs.getString(cloudDisplayPathPrefName,null);

        Preference cloudPathPref = findPreference(cloudPathPrefName);
        if(cloudPathPref!=null)
            cloudPathPref.setSummary(newValue==null?getString(R.string.no_cloud_folder_currently_set):displayPath);
    }

    void setCloudPath()
    {
        CloudType cloudType=SongList.getCloud();
        if(cloudType!= CloudType.Demo) {
            CloudStorage cs = CloudStorage.Companion.getInstance(cloudType, getActivity());
            cs.selectFolder(getActivity(), this);
        }
        else
            Toast.makeText(getActivity(),getString(R.string.no_cloud_storage_system_set),Toast.LENGTH_LONG).show();
    }

    @Override
    public void onFolderSelected(CloudFolderInfo folderInfo) {
        BeatPrompterApplication.getPreferences()
                .edit()
                .putString(getString(R.string.pref_cloudPath_key),folderInfo.mID)
                .putString(getString(R.string.pref_cloudDisplayPath_key),folderInfo.mDisplayPath)
                .apply();
    }

    @Override
    public void onFolderSelectedError(Throwable t) {
        Toast.makeText(getActivity(),t.getMessage(),Toast.LENGTH_LONG).show();
   }

    @Override
    public void onAuthenticationRequired() {
        // Don't need to do anything.
    }

    @Override
    public boolean shouldCancel() {
        return false;
    }

    public static class SettingsEventHandler extends EventHandler {
        private SettingsFragment mFragment;

        SettingsEventHandler(SettingsFragment fragment)
        {
            mFragment=fragment;
        }

        public void handleMessage(Message msg)
        {
            switch (msg.what)
            {
                case EventHandler.SET_CLOUD_PATH:
                    mFragment.setCloudPath();
                    break;
            }
        }

    }
}