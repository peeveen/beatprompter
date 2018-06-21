package com.stevenfrew.beatprompter.pref;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.stevenfrew.beatprompter.BeatPrompterApplication;
import com.stevenfrew.beatprompter.R;
import com.stevenfrew.beatprompter.SongList;
import com.stevenfrew.beatprompter.cloud.CloudFolderInfo;
import com.stevenfrew.beatprompter.cloud.CloudFolderSelectionListener;
import com.stevenfrew.beatprompter.cloud.CloudStorage;

public class SettingsFragment extends PreferenceFragment implements CloudFolderSelectionListener
{
    // TODO: define class for this.
    public Handler mSettingsHandler = new Handler()
    {
        public void handleMessage(Message msg)
        {
            switch (msg.what)
            {
                case BeatPrompterApplication.SET_CLOUD_PATH:
                    setCloudPath();
                    break;
            }
        }
    };

    SharedPreferences.OnSharedPreferenceChangeListener mCloudPathPrefListener = (sharedPreferences, key) -> {
        if(key.equals(getString(R.string.pref_cloudPath_key)))
            onCloudPathChanged(sharedPreferences.getString(key,null));
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ((BeatPrompterApplication)getActivity().getApplicationContext()).setSettingsHandler(mSettingsHandler);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);

        PreferenceManager.getDefaultSharedPreferences(getActivity()).registerOnSharedPreferenceChangeListener(mCloudPathPrefListener);

        String clearCachePrefName=getString(R.string.pref_clearCache_key);
        Preference clearCachePref = findPreference(clearCachePrefName);
        if(clearCachePref!=null) {
            clearCachePref.setOnPreferenceClickListener(preference -> {
                BeatPrompterApplication.mSongListHandler.obtainMessage(BeatPrompterApplication.CLEAR_CACHE).sendToTarget();
                return true;
            });
        }

        String cloudPathPrefName=getString(R.string.pref_cloudPath_key);
        final Preference cloudPathPref = findPreference(cloudPathPrefName);
        if(cloudPathPref!=null) {
            cloudPathPref.setOnPreferenceClickListener(preference -> {
                BeatPrompterApplication.mSettingsHandler.obtainMessage(BeatPrompterApplication.SET_CLOUD_PATH).sendToTarget();
                return true;
            });
        }

        String cloudPrefName=getString(R.string.pref_cloudStorageSystem_key);
        final Preference cloudPref = findPreference(cloudPrefName);
        if(cloudPref!=null) {
            cloudPref.setOnPreferenceChangeListener((preference, value) -> {
                SongList.mSongListInstance.deleteAllFiles();
                SharedPreferences sharedPref= PreferenceManager.getDefaultSharedPreferences(getActivity());
                sharedPref.edit().putString(getString(R.string.pref_cloudStorageSystem_key),value.toString()).apply();
                getPreferenceManager().getSharedPreferences().edit().putString(getString(R.string.pref_cloudPath_key),null).apply();
                getPreferenceManager().getSharedPreferences().edit().putString(getString(R.string.pref_cloudDisplayPath_key),null).apply();
                ((ImageListPreference)cloudPref).forceUpdate();
                return true;
            });
        }

        String powerwashPrefName=getString(R.string.pref_powerwash_key);
        Preference powerwashPref = findPreference(powerwashPrefName);
        if(powerwashPref!=null) {
            powerwashPref.setOnPreferenceClickListener(preference -> {
                SharedPreferences sharedPrefs=PreferenceManager.getDefaultSharedPreferences(getActivity());
                SharedPreferences.Editor editor = sharedPrefs.edit();
                editor.putString(getString(R.string.pref_cloudStorageSystem_key),"demo");
                editor.putString(getString(R.string.pref_cloudPath_key), null);
                editor.putString(getString(R.string.pref_cloudDisplayPath_key), null);
                editor.apply();

                SharedPreferences privatePrefs = getActivity().getSharedPreferences(BeatPrompterApplication.SHARED_PREFERENCES_ID, Context.MODE_PRIVATE);
                editor = privatePrefs.edit();
                editor.putString(getString(R.string.pref_songSource_key), "");
                editor.putBoolean(getString(R.string.pref_firstRun_key), true);
                editor.putString(getString(R.string.pref_dropboxAccessToken_key), null);
                editor.putBoolean(getString(R.string.pref_wasPowerwashed_key), true);
                editor.apply();

                BeatPrompterApplication.mSongListHandler.obtainMessage(BeatPrompterApplication.POWERWASH).sendToTarget();
                onCloudPathChanged(null);
                if(cloudPathPref!=null)
                    ((CloudPathPreference)cloudPathPref).forceUpdate();
                if(cloudPref!=null)
                    ((ImageListPreference)cloudPref).forceUpdate();

                return true;
            });
        }
    }

    @Override
    public void onDestroy()
    {
        PreferenceManager.getDefaultSharedPreferences(getActivity()).unregisterOnSharedPreferenceChangeListener(mCloudPathPrefListener);
        super.onDestroy();
    }

    void onCloudPathChanged(Object newValue)
    {
        String cloudPathPrefName=getString(R.string.pref_cloudPath_key);
        String cloudDisplayPathPrefName=getString(R.string.pref_cloudDisplayPath_key);
        SharedPreferences sharedPrefs=PreferenceManager.getDefaultSharedPreferences(getActivity());
        String displayPath=sharedPrefs.getString(cloudDisplayPathPrefName,null);

        Preference cloudPathPref = findPreference(cloudPathPrefName);
        if(cloudPathPref!=null)
            cloudPathPref.setSummary(newValue==null?getString(R.string.no_cloud_folder_currently_set):displayPath);
    }

    void setCloudPath()
    {
        CloudStorage cs=CloudStorage.getInstance(SongList.mSongListInstance.getCloud(),getActivity());
        if(cs!=null)
            cs.selectFolder(getActivity(),this);
        else
            Toast.makeText(getActivity(),getString(R.string.no_cloud_storage_system_set),Toast.LENGTH_LONG).show();
    }

    @Override
    public void onFolderSelected(CloudFolderInfo folderInfo) {
        getPreferenceManager()
                .getSharedPreferences()
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
}